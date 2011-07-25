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

class XMLSequence implements CharSequence {
	char[] data;
	int offset;
	int count;

	public XMLSequence() { }

	public void init(char[] data, int start, int end) {
		this.data = data;
		offset = start;
		count = end - start;
	}
	public void init(XMLSequence s) {
		data = s.data;
		offset = s.offset;
		count = s.count;
	}
	void adjust(int start, int end) {
		offset = start;
		count = end - start;
	}
	boolean eq(XMLSequence other) {
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
	/* modifies the sequence in-place, escaping basic entities */
	public XMLSequence unescape() {
		count = unescape(data, offset) - offset;
		return this;
	}

	/* copies the sequence into a char[] + offset, escaping basic entities */
	public int unescape(char[] out, int off) {
		char[] in = data;
		int n = offset;
		int max = n + count;

		while (n < max) {
			char c = in[n++];
			if (c != '&') {
				out[off++] = c;
				continue;
			}
			int e = n;
			while (n < max) {
				if (in[n++] != ';')
					continue;
				switch(in[e]) {
				case 'l': // lt
					out[off++] = '<';
					break;
				case 'g': // gt
					out[off++] = '>';
					break;
				case 'q': // quot
					out[off++] = '"';
					break;
				case 'a': // amp | apos
					if (in[e+1] == 'm')
						out[off++] = '&';
					else if (in[e+1] == 'p')
						out[off++] = '\'';
					break;
				}
				break;
			}
		}
		return off;
	}
	
	public CharSequence copy() {
		XMLSequence s = new XMLSequence();
		s.init(data, offset, offset + count);
		return s;
	}
	public void trim() {
		while (count > 0) {
			char c = data[offset];
			if ((c==' ')||(c=='\r')||(c=='\n')||(c=='\t')) {
				offset++;
				count--;
				continue;
			}
			c = data[offset + count - 1];
			if ((c==' ')||(c=='\r')||(c=='\n')||(c=='\t')) {
				count--;
				continue;
			}
			break;
		}
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
		XMLSequence x = new XMLSequence();
		x.init(data, offset + start, offset + end);
		return x;
	}
	public String toString() {
		return new String(data, offset, count);
	}
}
