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
		Sonos sonos = new Sonos("10.0.0.199");

		//sonos.trace_io(true);
		sonos.trace_reply(true);
		sonos.trace_browse(true);

		if (args.length == 0) {
			sonos.getPosition();
			return;
		}

		String cmd = args[0];
		if (cmd.equals("discover")) {
			Discover d = new Discover();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException x) {
			}
			d.done();
			String[] list = d.getList();
			for (int n = 0; n < list.length; n++) {
				Sonos s = new Sonos(list[n]);
				String name = s.getZoneName();
				if (name != null)
					System.out.println(list[n] + " - " + name);
			}
		} else if (cmd.equals("play")) {
			sonos.play();
			sonos.play();
			sonos.play();
		} else if (cmd.equals("save")) {
			sonos.save(args[1],"SQ:3");
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
		} else if (cmd.equals("setxport")) {
			sonos.setTransportURI(args[1]);
		} else if (cmd.equals("getxport")) {
			String x = sonos.getTransportURI();
			System.out.println(x);
		} else if (cmd.equals("remove")) {
			sonos.remove(args[1]);
		} else if (cmd.equals("removeall")) {
			sonos.removeAll();
		} else if (cmd.equals("move")) {
			sonos.move(Integer.parseInt(args[1]),Integer.parseInt(args[2]));
		} else if (cmd.equals("track")) {
			sonos.seekTrack(Integer.parseInt(args[1]));
		} else if (cmd.equals("volume")) {
			if (args.length == 1) {
				int n = sonos.volume();
				System.out.println(n);
			} else {
				sonos.volume(Integer.parseInt(args[1]));
			}
		} else {
			System.err.println("Unknown command '"+cmd+"'");
			System.exit(-1);
		}
	}
	public void updateDone(String id) { }
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
