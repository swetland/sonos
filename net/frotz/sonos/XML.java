/*
 * Copyright (C) 2011 Brian Swetland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.frotz.sonos;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.PrintStream;

// TODO: &apos; -> '

public class XML {
	XMLSequence seq; /* entire buffer */
	XMLSequence tag; /* most recent tag */
	XMLSequence tmp; /* for content return */
	char[] xml;
	int offset;
	int count;

	Matcher mTag;
	Matcher mEntity;
	Matcher mAttr;
	boolean isOpen;

	CharsetDecoder decoder;

	/* used for io operations */
	CharBuffer buf;

	public XML(int size) {
		decoder = cs.newDecoder();
		seq = new XMLSequence();
		tag = new XMLSequence();
		tmp = new XMLSequence();
		mTag = pTag.matcher(seq);
		mEntity = pEntity.matcher(tmp);
		mAttr = pAttr.matcher(tmp);
		xml = new char[size];
		buf = CharBuffer.wrap(xml);	
	}

	public void init(ByteBuffer in) {
		buf.clear();
		CoderResult cr = decoder.decode(in, buf, true);
		// TODO: error handling
		buf.flip();
		reset();
	}
	public void init(XMLSequence s) {
		buf.clear();
		buf.put(s.data, s.offset, s.count);
		buf.flip();
		reset();
	}
	void reset() {
		seq.init(xml, buf.arrayOffset(), buf.length());
		tag.init(xml, 0, 0);
		tmp.init(xml, 0, 0);
		offset = 0;
		nextTag();
		//System.err.println("XML reset, "+buf.length()+" bytes.");
	}
	public void rewind() {
		offset = 0;
		nextTag();
	}
	
	public XMLSequence getAttr(String name) {
		int off = mTag.start(3);
		int end = off + mTag.end(3);

		tmp.offset = 0;
		tmp.count = end;
		while (mAttr.find(off)) {
			//System.err.println("ANAME: " + mAttr.group(1));
			//System.err.println("ATEXT: " + mAttr.group(2));
			tmp.offset = mAttr.start(1);
			tmp.count = mAttr.end(1) - tmp.offset;
			if (name.contentEquals(tmp)) {
				tmp.offset = mAttr.start(2);
				tmp.count = mAttr.end(2) - tmp.offset;
				return tmp;
			}
			tmp.offset = 0;
			tmp.count = end;
			off = mAttr.end();
		}
		return null;
	}

	/* set sequence to the text between the end of the current tag
	 * and the beginning of the next tag.
	 */
	public XMLSequence getText() {
		char[] data = xml;
		int n;
		tmp.data = data;
		n = tmp.offset = mTag.end();
		try {
			for (;;) {
				if (data[n] == '<')
					break;
				n++;
			}
			tmp.count = n - tmp.offset;
		} catch (ArrayIndexOutOfBoundsException x) {
			tmp.count = 0;
		}
		return tmp;
	}

	public void print(PrintStream out, int max) {
		char[] buf = new char[max];
		print(out, max, 0, buf);
	}
	void print(PrintStream out, int max, int indent, char[] buf) {
		XMLSequence s;
		int n;
		if (!isOpen) {
			out.println("ERROR");
			return;
		}
		for (n = 0; n < indent; n++)
			out.print(" ");
		out.print(str());
		s = getText();
		nextTag();
		if (isOpen) {
			out.print("\n");
			do {
				print(out, max, indent + 2, buf);
			} while (isOpen);
			for (n = 0; n < indent; n++)
				out.print(" ");
			out.println(str());
		} else {
			if (s.count > max) {
				s.count = max;
				n = s.unescape(buf, 0);
				out.println("" + new String(buf, 0, n) + "..." + str());
			} else {
				n = s.unescape(buf, 0);
				out.println("" + new String(buf, 0, n) + str());
			}
		}	
		nextTag();
	}

	public boolean more() {
		return isOpen;
	}

	/* require <tag> and consume it */
	public void open(String name) throws XML.Oops {
		if (!isOpen || !name.contentEquals(tag))
			throw new XML.Oops("expecting <"+name+"> but found " + str());
		nextTag();
	}

	/* require </tag> and consume it */
	public void close(String name) throws XML.Oops {
		if (isOpen || !name.contentEquals(tag))
			throw new XML.Oops("expecting </"+name+"> but found " + str());
		nextTag();
	}

	/* require <tag> text </tag> and return text */
	public XMLSequence read(String name) throws XML.Oops {
		int start = mTag.end(); 
		open(name);
		tmp.adjust(start, mTag.start());
		close(name);
		return tmp;
	}

	/* read the next  <name> value </name>  returns false if no open tag */
	public boolean tryRead(XMLSequence name, XMLSequence value) throws XML.Oops {
		if (!isOpen)
			return false;

		name.data = xml;
		name.offset = tag.offset;
		name.count = tag.count;

		value.data = xml;
		value.offset = mTag.end();

		nextTag();

		value.count = mTag.start() - value.offset;
		close(name);

		return true;
	}
	public void close(XMLSequence name) throws XML.Oops {
		if (isOpen)
			throw new XML.Oops("1expected </"+name+">, found <"+tag+">");
		if (!name.eq(tag))
			throw new XML.Oops("2expected </"+name+">, found </"+tag+">");
		nextTag();
	}

	public boolean tryRead(String name, XMLSequence value) throws XML.Oops {
		if (!isOpen || !name.contentEquals(tag))
			return false;
		value.data = xml;
		value.offset = mTag.end();
		nextTag();
		value.count = mTag.start() - value.offset;
		close(name);
		return true;
	}

	/* eat the current tag and any children */
	public void consume() throws XML.Oops {
		tmp.offset = mTag.start(2);
		tmp.count = mTag.end(2) - tmp.offset;
		nextTag();
		while (isOpen)
			consume();
		close(tmp);
	}

	/* format current begin/end tag as a string. for error messages */
	String str() {
		if (isOpen)
			return "<" + tag + ">";
		else
			return "</" + tag + ">";
	}

	void nextTag() {
		/* can't deal with comments or directives */
		if (!mTag.find(offset)) {
			tag.adjust(0,0);
			return;
		}

		isOpen = (mTag.start(1) == -1);
		tag.adjust(mTag.start(2), mTag.end(2));
		offset = mTag.end();
	}

	/* G1: \  G2: tagname  G3: attributes */
	static Pattern pTag = Pattern.compile("<(/)?([a-zA-Z:_][a-zA-Z0-9:\\-\\._]*)([^>]*)>",Pattern.DOTALL);
	/* G1: name  G2: value */
	static Pattern pAttr = Pattern.compile("\\s*([a-zA-Z:_][a-zA-Z0-9:\\-\\._]*)=\"([^\"]*)\"");
	/* G1: pretext  G2: entity  G3: entity body */
	static Pattern pEntity = Pattern.compile("([^&]*)(&([^;]*);)");

	static Charset cs = Charset.forName("UTF-8");

	static public class Oops extends Exception {
		public Oops(String msg) {
			super(msg);
		}
	}
}
