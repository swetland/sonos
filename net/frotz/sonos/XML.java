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

// TODO: &apos; -> '

public class XML {
	XML.Sequence seq; /* entire buffer */
	XML.Sequence tag; /* most recent tag */
	XML.Sequence tmp; /* for content return */
	char[] xml;
	int offset;
	int count;

	Matcher mTag;
	Matcher mEntity;
	Matcher mAttr;
	boolean isOpen;

	/* used for io operations */
	CharBuffer buf;

	public XML(int size) {
		seq = new XML.Sequence();
		tag = new XML.Sequence();
		tmp = new XML.Sequence();
		mTag = pTag.matcher(seq);
		mEntity = pEntity.matcher(tmp);
		mAttr = pAttr.matcher(tmp);
		xml = new char[size];
		buf = CharBuffer.wrap(xml);	
	}

	public void init(ByteBuffer in) {
		buf.clear();
		CoderResult cr = decoder.decode(in, buf, true);
		System.err.println("cr = " + cr);
		buf.flip();
		reset();
	}
	public void init(XML.Sequence s) {
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
		System.err.println("XML reset, "+buf.length()+" bytes.");
	}
	public XML.Sequence unescape(XML.Sequence s) {
		int n = s.offset;
		int max = n + s.count;
		char data[] = s.data;
		int out = n;

		while (n < max) {
			char c = data[n++];
			if (c != '&') {
				data[out++] = c;
				continue;
			}
			int e = n;
			while (n < max) {
				if (data[n++] != ';')
					continue;
				switch(data[e]) {
				case 'l': // lt
					data[out++] = '<';
					break;
				case 'g': // gt
					data[out++] = '>';
					break;
				case 'q': // quot
					data[out++] = '"';
					break;
				case 'a': // amp | apos
					if (data[e+1] == 'm')
						data[out++] = '&';
					else if (data[e+1] == 'p')
						data[out++] = '\'';
					break;
				}
				break;
			}
		}
		s.count = out - s.offset;
		return s;
	}
		
	public XML.Sequence getAttr(String name) {
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
	public XML.Sequence read(String name) throws XML.Oops {
		int start = mTag.end(); 
		open(name);
		tmp.adjust(start, mTag.start());
		close(name);
		return tmp;
	}

	/* read the next  <name> value </name>  returns false if no open tag */
	public boolean tryRead(XML.Sequence name, XML.Sequence value) throws XML.Oops {
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
	public void close(XML.Sequence name) throws XML.Oops {
		if (isOpen)
			throw new XML.Oops("1expected </"+name+">, found <"+tag+">");
		if (!name.eq(tag))
			throw new XML.Oops("2expected </"+name+">, found </"+tag+">");
		nextTag();
	}

	public boolean tryRead(String name, XML.Sequence value) throws XML.Oops {
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
	static CharsetDecoder decoder = cs.newDecoder();

	static public class Oops extends Exception {
		public Oops(String msg) {
			super(msg);
		}
	}

	static class Sequence implements CharSequence {
		private char[] data;
		private int offset;
		private int count;

		public Sequence() {
		}

		void init(char[] data, int start, int end) {
			this.data = data;
			offset = start;
			count = end - start;
		}
		void adjust(int start, int end) {
			offset = start;
			count = end - start;
		}
		boolean eq(Sequence other) {
			int count = this.count;
			if (count != other.count)
				return false;
			char[] a = this.data;
			int ao = this.offset;
			char[] b = other.data;
			int bo = other.offset;
			while (count-- > 0)
				if (a[ao++] != b[bo++])
					return false;
			return true;
		}
		CharSequence copy() {
			Sequence s = new Sequence();
			s.init(data, offset, offset + count);
			return s;
		}

		/* CharSequence interface */
		public int length() {
			return count;
		}
		public char charAt(int index) {
			//System.err.print("["+data[offset+index]+"]");
			return data[offset + index];
		}
		public CharSequence subSequence(int start, int end) {
			//System.err.println("[subSequence("+start+","+end+")]");
			Sequence x = new Sequence();
			x.init(data, offset + start, offset + end);
			return x;
		}
		public String toString() {
			return new String(data, offset, count);
		}
	}
}
