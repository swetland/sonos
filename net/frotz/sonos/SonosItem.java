package net.frotz.sonos;


public class SonosItem {
	public XMLSequence title;
	public XMLSequence album;
	public XMLSequence artist;
	public XMLSequence playURI; /* to enqueue */
	public XMLSequence idURI;   /* for browse/list */

	public void reset() {
		title.adjust(0,0);
		album.adjust(0,0);
		artist.adjust(0,0);
		playURI.adjust(0,0);
		idURI.adjust(0,0);
	}
}
