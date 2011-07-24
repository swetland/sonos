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

public class Sonos {
	boolean trace_browse;
	SoapRPC.Endpoint xport;
	SoapRPC.Endpoint media;
	SoapRPC.Endpoint render;
	SoapRPC rpc;

	public Sonos(byte[] ip) {
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

	public void volume() {
		rpc.call(render,"GetVolume",
			"<InstanceID>0</InstanceID>"+
			"<Channel>Master</Channel>" // Master | LF | RF
			);
	}
	public void volume(int vol) { // 0-100
		if ((vol < 0) || (vol > 100))
			return;
		rpc.call(render,"SetVolume",
			"<InstanceID>0</InstanceID>"+
			"<Channel>Master</Channel>"+
			"<DesiredVolume>"+vol+"</DesiredVolume>"
			);
	}
	public void play() {
		rpc.call(xport,"Play","<InstanceID>0</InstanceID><Speed>1</Speed>");
	}
	public void pause() {
		rpc.call(xport,"Pause","<InstanceID>0</InstanceID>");
	}
	public void stop() {
		rpc.call(xport,"Stop","<InstanceID>0</InstanceID>");
	}
	public void next() {
		rpc.call(xport,"Next","<InstanceID>0</InstanceID>");
	}
	public void seekTrack(String nr) {
		rpc.call(xport,"Seek","<InstanceID>0</InstanceID><Unit>TRACK_NR</Unit><Target>"+nr+"</Target>");
		// does not start playing if not already in playback mode
	}
	public void prev() {
		rpc.call(xport,"Previous","<InstanceID>0</InstanceID>");
	}
	public void remove(String id) {
		rpc.call(xport,"RemoveTrackFromQueue","<InstanceID>0</InstanceID><ObjectID>"+id+"</ObjectID>");
	}
	public void removeAll() {
		rpc.call(xport,"RemoveAllTracksFromQueue","<InstanceID>0</InstanceID>");
	}
	public void add(String uri) {
		rpc.call(xport,"AddURIToQueue",
			"<InstanceID>0</InstanceID>"+
			"<EnqueuedURI>"+uri+"</EnqueuedURI>"+  // from <res> x-file-cifs... etc
			"<EnqueuedURIMetaData></EnqueuedURIMetaData>"+
			"<DesiredFirstTrackNumberEnqueued>0</DesiredFirstTrackNumberEnqueued>"+
			"<EnqueueAsNext>0</EnqueueAsNext>" // 0 = append, 1-n = insert
		);
	}
	public void move(String from, String to) {
		rpc.call(xport,"ReorderTracksInQueue",
			"<InstanceID>0</InstanceID>"+
			"<StartingIndex>"+from+"</StartingIndex>"+
			"<NumberOfTracks>1</NumberOfTracks>"+
			"<InsertBefore>"+to+"</InsertBefore>"
			);
	}
	public void list(String _id, boolean d) {
		int n = 0;
		XML result = new XML(32768);
		XML xml = rpc.call(media,"Browse",
			"<ObjectID>"+_id+"</ObjectID>"+
			(!d ? "<BrowseFlag>BrowseMetadata</BrowseFlag>" : 
			"<BrowseFlag>BrowseDirectChildren</BrowseFlag>" )+
			"<Filter></Filter>"+
			"<StartingIndex>" + n + "</StartingIndex>"+
			"<RequestedCount>25</RequestedCount>"+
			"<SortCriteria></SortCriteria>"
			);
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
