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

/* not thread-safe, not reentrant */
public class Sonos {
	boolean trace_browse;
	SoapRPC.Endpoint xport;
	SoapRPC.Endpoint media;
	SoapRPC.Endpoint render;
	SoapRPC rpc;
	XMLSequence name, value;
	SonosItem item;

	public Sonos(byte[] ip) {
		init(ip);
	}

	void init(byte[] ip) {
		name = new XMLSequence();
		value = new XMLSequence();
		item = new SonosItem();

		rpc = new SoapRPC(ip, 1400);

		xport = new SoapRPC.Endpoint(
			"AVTransport:1",
			"/MediaRenderer/AVTransport/Control");
		media = new SoapRPC.Endpoint(
			"ContentDirectory:1",
			"/MediaServer/ContentDirectory/Control");
		render = new SoapRPC.Endpoint(
			"RenderingControl:1",
			"/MediaRenderer/RenderingControl/Control");
	}

	public void trace_io(boolean x) { rpc.trace_io = x; }
	public void trace_reply(boolean x) { rpc.trace_reply = x; }
	public void trace_browse(boolean x) { trace_browse = x; }

	/* volume controls */
	public void volume() {
		rpc.prepare(render,"GetVolume");
		rpc.simpleTag("InstanceID",0);
		rpc.simpleTag("Channel", "Master"); // Master | LF | RF
		rpc.invoke();
	}
	public void volume(int vol) { // 0-100
		if ((vol < 0) || (vol > 100))
			return;
		rpc.prepare(render,"SetVolume");
		rpc.simpleTag("InstanceID",0);
		rpc.simpleTag("Channel","Master");
		rpc.simpleTag("DesiredVolume",vol);
		rpc.invoke();
	}

	/* transport controls */
	public void play() {
		rpc.prepare(xport,"Play");
		rpc.simpleTag("InstanceID",0);
		rpc.simpleTag("Speed",1);
		rpc.invoke();
	}
	public void pause() {
		rpc.prepare(xport,"Pause");
		rpc.simpleTag("InstanceID",0);
		rpc.invoke();
	}
	public void stop() {
		rpc.prepare(xport,"Stop");
		rpc.simpleTag("InstanceID",0);
		rpc.invoke();
	}
	public void next() {
		rpc.prepare(xport,"Next");
		rpc.simpleTag("InstanceID",0);
		rpc.invoke();
	}
	public void prev() {
		rpc.prepare(xport,"Previous");
		rpc.simpleTag("InstanceID",0);
		rpc.invoke();
	}
	public void seekTrack(int nr) {
		if (nr < 1)
			return;	
		rpc.prepare(xport,"Seek");
		rpc.simpleTag("InstanceID",0);
		rpc.simpleTag("Unit","TRACK_NR");
		rpc.simpleTag("Target",nr);
		rpc.invoke();
		// does not start playing if not already in playback mode
	}

	/* queue management */
	public void add(String uri) {
		rpc.prepare(xport,"AddURIToQueue");
		rpc.simpleTag("InstanceID",0);
		rpc.simpleTag("EnqueuedURI",uri);
		rpc.simpleTag("EnqueuedURIMetaData","");
		rpc.simpleTag("DesiredFirstTrackNumberEnqueued",0);
		rpc.simpleTag("EnqueueAsNext",0); // 0 = append, 1+ = insert
		rpc.invoke();
	}
	public void remove(String id) {
		rpc.prepare(xport,"RemoveTrackFromQueue");
		rpc.simpleTag("InstanceID",0);
		rpc.simpleTag("ObjectID",id);
		rpc.invoke();
	}
	public void removeAll() {
		rpc.prepare(xport,"RemoveAllTracksFromQueue");
		rpc.simpleTag("InstanceID",0);
		rpc.invoke();
	}
	public void move(int from, int to) {
		if ((from < 1) || (to < 1))
			return;
		rpc.prepare(xport,"ReorderTracksInQueue");
		rpc.simpleTag("InstanceID",0);
		rpc.simpleTag("StartingIndex",from);
		rpc.simpleTag("NumberOfTracks",1);
		rpc.simpleTag("InsertBefore",to);
		rpc.invoke();
	}

	/* content service calls */
	public void browse(String _id, SonosListener cb) {
		int total, count, updateid;
		int n = 0;
		XML xml;

		do {
			rpc.prepare(media,"Browse");
			rpc.simpleTag("ObjectID",_id);
			rpc.simpleTag("BrowseFlag","BrowseDirectChildren"); // BrowseMetadata
			rpc.simpleTag("Filter","");
			rpc.simpleTag("StartingIndex", n);
			rpc.simpleTag("RequestedCount",100);
			rpc.simpleTag("SortCriteria","");

			xml = rpc.invoke();
			try {
				xml.open("u:BrowseResponse");
				value.init(xml.read("Result"));

				// Eww, toString()? really? surely there's
				// a non-allocating Int parser somewhere
				// in the bloat that is java standard libraries?
				count = Integer.parseInt(xml.read("NumberReturned").toString());
				total = Integer.parseInt(xml.read("TotalMatches").toString());
				updateid = Integer.parseInt(xml.read("UpdateID").toString());

				/* descend in to the contained results */
				value.unescape();
				xml.init(value);
				n += processBrowseResults(xml,_id,cb);
			} catch (Exception x) {
				System.err.println("OOPS " + x);
				x.printStackTrace();
				break;
			}
		} while (n < total);
	}
	int processBrowseResults(XML result, String _id, SonosListener cb) throws XML.Oops {
		SonosItem item = this.item;
		int n = 0;
		if (trace_browse) {
			System.out.println("--------- list -----------");
			result.print(System.out,1024);
			result.rewind();
		}
		result.open("DIDL-Lite");
		while (result.more()) {
			String thing;
			n++;
			item.reset();
			item.idURI = result.getAttr("id").copy();
			try { 
				result.open("item");
				thing = "item";	
			} catch (XML.Oops x) {
				result.open("container"); // yuck!
				thing = "container";
			}
			while (result.tryRead(name,value)) {
				if ("dc:title".contentEquals(name)) {
					item.title = value.unescape().copy();
					continue;
				}
				if ("dc:creator".contentEquals(name)) {
					item.artist = value.unescape().copy();
					continue;
				}
				if ("upnp:album".contentEquals(name)) {
					item.album = value.unescape().copy();
					continue;
				}
				if ("res".contentEquals(name)) {
					item.playURI = value.unescape().copy();
					continue;
				}
			}
			cb.updateItem(_id, n, item);
			result.close(thing);
		}
		return n;
	}
}
