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


public class SonosItem {
	public XMLSequence title;
	public XMLSequence album;
	public XMLSequence artist;
	public XMLSequence playURI; /* to enqueue */
	public XMLSequence idURI;   /* for browse/list */
	public int flags;

	public void reset() {
		title.adjust(0,0);
		album.adjust(0,0);
		artist.adjust(0,0);
		playURI.adjust(0,0);
		idURI.adjust(0,0);
		flags = 0;
	}
	public static final int SONG = 1;
	public static final int PLAYLIST = 2;
}
