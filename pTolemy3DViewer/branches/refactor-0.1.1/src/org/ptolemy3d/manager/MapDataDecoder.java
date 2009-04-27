/**
 * Ptolemy3D - a Java-based 3D Viewer for GeoWeb applications.
 * Copyright (C) 2008 Mark W. Korver
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ptolemy3d.manager;

import java.util.HashMap;
import java.util.Iterator;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;

/*
 * FIXME
 *  - Handle ressource that fails to download
 *  - Move to multiple download unit
 */

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDataDecoder {
	/** Keep reference of all MapData */
	private final HashMap<MapDataKey, MapDecoderEntry> decoders;
	
	public MapDataDecoder() {
		decoders = new HashMap<MapDataKey, MapDecoderEntry>();
		new DownloadDispatcherThread().start();
		new DecoderThread().start();
	}
	
	public Texture request(MapData mapData, int resolution) {
		final MapDecoderEntry decoder = decoders.get(mapData.key);
		if (decoder == null) {
			decoders.put(mapData.key, new MapDecoderEntry(mapData));
			return null;
		}
		else {
			decoder.onRequest();
			return decoder.getWavelet(resolution);
		}
	}

	/** */
	//FIXME Implemented as a single unit, but to be moved with multiple download units
	class DownloadDispatcherThread extends Thread {
		public DownloadDispatcherThread() {
			super();
			setDaemon(true);
		}
		
		public void run() {
			while (true) {
				if (!download()) {
					//wait();
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) { }
				}
			}
		}
		private boolean download() {
			final MapDecoderEntry entry = findEntryToDownload();
			if (entry != null) {
				return entry.download();
			}
			return false;
		}
		private MapDecoderEntry findEntryToDownload() {
			final Iterator<MapDecoderEntry> i = decoders.values().iterator();
			while (i.hasNext()) {
				final MapDecoderEntry entry = i.next();
				if (!entry.isDownloaded()) {
					return entry;
				}
			}
			return null;
		}
	}
	
	/** */
	class DecoderThread extends Thread {
		public DecoderThread() {
			super();
			setDaemon(true);
		}
		
		public void run() {
			while (true) {
				if (!decode()) {
					//wait();
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) { }
				}
			}
		}
		private boolean decode() {
			for(int resolution = 0; resolution < MapData.MAX_NUM_RESOLUTION; resolution++) {
				final MapDecoderEntry entry = findEntryToDecode(resolution);
				if (entry != null) {
					return entry.decode(resolution);
				}
			}
			return false;
		}
		private MapDecoderEntry findEntryToDecode(int resolution) {
			final Iterator<MapDecoderEntry> i = decoders.values().iterator();
			while (i.hasNext()) {
				final MapDecoderEntry entry = i.next();
				if (!entry.isDecoded(resolution)) {
					return entry;
				}
			}
			return null;
		}
	}
}
