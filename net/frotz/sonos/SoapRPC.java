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

import java.net.InetAddress;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class SoapRPC {
	public boolean trace;
	InetAddress addr;
	int port;
	ByteBuffer reply;
	XML xml;

	public SoapRPC(byte[] host, int port) {
		try {
			addr = InetAddress.getByAddress(host);
		} catch (Exception x) {
		}
		this.port = port;

		reply = ByteBuffer.wrap(new byte[32768]);
		xml = new XML(32768);
	}
	void call(byte[] data) {
		try {
			reply.clear();
			byte[] buf = reply.array();
			Socket s = new Socket(addr,port);
			OutputStream out = s.getOutputStream();
			InputStream in = s.getInputStream();
			out.write(data);
			int off = 0;
			for (;;) {
				int r = in.read(buf, off, buf.length - off);
				if (r <= 0) break;
				off += r;
			}
			reply.limit(off);
			s.close();
			if (trace) {
				System.out.println("--------- reply -----------");
				System.out.println(new String(buf, 0, off));
			}
		} catch (Exception x) {
			System.out.println("OOPS: " + x.getMessage());
			x.printStackTrace();
		}
	}
	public XML call(Endpoint ept, String method, String payload) {
		StringBuilder msg = new StringBuilder();
		msg.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\"><s:Body>");
		msg.append("<u:");
		msg.append(method);
		msg.append(" xmlns:u=\"urn:schemas-upnp-org:service:");
		msg.append(ept.service);
		msg.append("\">");
		msg.append(payload);
		msg.append("</u:");
		msg.append(method);
		msg.append("></s:Body></s:Envelope>\n");

		StringBuilder sb = new StringBuilder();
		sb.append("POST ");
		sb.append(ept.path);
		sb.append(" HTTP/1.0\r\n");
		sb.append("CONNECTION: close\r\n");
		sb.append("Content-Type: text/xml; charset=\"utf-8\"\r\n");
		sb.append("Content-Length: ");
		sb.append(msg.length());
		sb.append("\r\n");
		sb.append("SOAPACTION: \"urn:schemas-upnp-org:service:");
		sb.append(ept.service);
		sb.append("#");
		sb.append(method);
		sb.append("\"\r\n\r\n");
		sb.append(msg);

		if (trace) {
			System.out.println("--------- message -----------");
			System.out.println(sb);
		}

		byte[] data = sb.toString().getBytes();

		call(data);

		xml.init(reply);
		try {
			xml.open("s:Envelope");
			xml.open("s:Body");
			return xml;
		} catch (XML.Oops x) {
			return null;
		}
	}
	public static class Endpoint {
		String service,path;
		public Endpoint(String service, String path) {
			this.service = service;
			this.path = path;
		}
	}
}
