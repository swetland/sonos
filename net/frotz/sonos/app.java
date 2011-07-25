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

public class app implements SonosListener {
	public static void main(String args[]) {
		app a = new app();
		Sonos sonos = new Sonos(new byte[] { 10, 0, 0, (byte) 199});
		String cmd = args[0];

		//sonos.trace_io(true);
		//sonos.trace_reply(true);
		//sonos.trace_browse(true);

		if (cmd.equals("play")) {
			sonos.play();
			sonos.play();
			sonos.play();
		} else if (cmd.equals("pause")) {
			sonos.pause();
		} else if (cmd.equals("next")) {
			sonos.next();
		} else if (cmd.equals("prev")) {
			sonos.prev();
		} else if (cmd.equals("list")) {
			sonos.browse(args[1],a);
		} else if (cmd.equals("add")) {
			sonos.add(args[1]);
		} else if (cmd.equals("remove")) {
			sonos.remove(args[1]);
		} else if (cmd.equals("removeall")) {
			sonos.removeAll();
		} else if (cmd.equals("move")) {
			sonos.move(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
		} else if (cmd.equals("track")) {
			sonos.seekTrack(Integer.parseInt(args[1]));
		} else if (cmd.equals("volume")) {
			if (args.length == 1)
				sonos.volume();
			else
				sonos.volume(Integer.parseInt(args[1]));
		} else {
			System.err.println("Unknown command '"+cmd+"'");
			System.exit(-1);
		}
	}
	public void updateItem(String id, int idx, SonosItem item) {
		System.out.println("("+idx+")\t    id: " + item.idURI);
		System.out.println("\t   res: " + item.playURI);
		System.out.println("\t title: " + item.title);
		if (item.album != null)
			System.out.println("\t album: " + item.album);
		if (item.artist != null)
			System.out.println("\tartist: " + item.artist);
	}
}
