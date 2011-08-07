package net.frotz.sonos;

public interface SonosListener {
	public void updateItem(String parent, int index, SonosItem item);
	public void updateDone(String parent);
}
