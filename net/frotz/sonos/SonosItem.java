package net.frotz.sonos;


public class SonosItem {
	public CharSequence title;
	public CharSequence album;
	public CharSequence artist;
	public CharSequence playURI; /* to enqueue */
	public CharSequence idURI;   /* for browse/list */

	public void reset() {
		title = null;
		album = null;
		artist = null;
		playURI = null;
		idURI= null;
	}
}
