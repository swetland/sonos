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
	XML result;

	public Sonos(byte[] ip) {
		init(ip);
	}

	void init(byte[] ip) {
		result = new XML(32768);
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
	public void list(String _id, boolean d) {
		int n = 0;
		XML xml;

		rpc.prepare(media,"Browse");
		rpc.simpleTag("ObjectID",_id);
		rpc.simpleTag("BrowseFlag",
			(d ? "BrowseDirectChildren" : "BrowseMetadata"));
		rpc.simpleTag("Filter","");
		rpc.simpleTag("StartingIndex", n);
		rpc.simpleTag("RequestedCount",25);
		rpc.simpleTag("SortCriteria","");
		xml = rpc.invoke();

		try {
			XMLSequence name = new XMLSequence();
			XMLSequence value = new XMLSequence();
			xml.open("u:BrowseResponse");
			XMLSequence tmp = xml.read("Result");	
			tmp.unescape();
			//System.out.println(tmp);
			result.init(tmp);

			if (trace_browse) {
				System.out.println("--------- list -----------");
				result.print(System.out,1024);
				result.rewind();
			}
			System.err.println("Count = " + xml.read("NumberReturned"));
			System.err.println("Total = " + xml.read("TotalMatches"));
			System.err.println("UpdID = " + xml.read("UpdateID"));

			result.open("DIDL-Lite");
			while (result.more()) {
				n++;
				CharSequence id = result.getAttr("id").copy();
				CharSequence title = "";
				CharSequence album = "";
				CharSequence res = "";
				String thing = "item";	
				try { 
					result.open("item");
				} catch (XML.Oops x) {
					result.open("container"); // yuck!
					thing = "container";
				}
				while (result.tryRead(name,value)) {
					if ("dc:title".contentEquals(name)) {
						title = value.unescape().copy();
						continue;
					}
					if ("upnp:album".contentEquals(name)) {
						album = value.unescape().copy();
						continue;
					}
					if ("res".contentEquals(name)) {
						res = value.copy();
						continue;
					}
				}
				if (thing == "item")
					System.err.println("Item:       " + n);
				else
					System.err.println("Item:       " + id);
				System.out.println("  Title:    " + title);
				if (album.length() > 0)
					System.out.println("  Album:    " + album);
				System.out.println("  Resource: " + res);
				result.close(thing);
			}
		} catch (XML.Oops x) {
			System.err.println("OOPS: " + x.getMessage());
			x.printStackTrace();
		}
	}
}
