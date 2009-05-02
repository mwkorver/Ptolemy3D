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
 *  - Implemented as a single unit, but to be moved with multiple download units
 *  - Download retry
 */

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDataDecoder {
	/** Keep reference of all MapData */
	private final HashMap<MapDataKey, MapDecoderEntry> decoders;
	private final DownloadDispatcherThread downloadDispatcher;
	private final DecoderThread decoderThread;
	
	public MapDataDecoder() {
		decoders = new HashMap<MapDataKey, MapDecoderEntry>();
		downloadDispatcher = new DownloadDispatcherThread();
		decoderThread = new DecoderThread();
		
		//Let's start now, if nothing is to be done,
		//thread will be in wait state and will wake-up when entry will be added
		downloadDispatcher.start();
		decoderThread.start();
	}
	
	public int getCurrentResolution(MapData mapData) {
		final MapDecoderEntry decoder = getDecoderEntry(mapData);
		return decoder.getCurrentResolution();
	}
	public Texture request(MapData mapData, int resolution) {
		final MapDecoderEntry decoder = getDecoderEntry(mapData);
		return decoder.getWavelet(resolution);
	}
	private MapDecoderEntry getDecoderEntry(MapData mapData) {
		MapDecoderEntry decoder = decoders.get(mapData.key);
		if (decoder == null) {
			decoder = new MapDecoderEntry(mapData);
			synchronized(decoders) {	//Accessed from multiple threads
				decoders.put(mapData.key, decoder);
			}
			try {
				downloadDispatcher.notify();
			} catch(Exception e) {}
		}
		decoder.onRequest();
		return decoder;
	}

	/** */
	class DownloadDispatcherThread extends Thread {
		public DownloadDispatcherThread() {
			super("DownloadDispatcher");
			setDaemon(true);
		}
		
		public void run() {
			while (true) {
				if(download()) {
					try {
						decoderThread.notify();
					} catch(Exception e) {}
				}
			}
		}
		private boolean download() {
			final MapDecoderEntry entry = findEntryToDownload();
			if (entry != null) {
				return entry.download();
			}
			try {
				wait();
			} catch(Exception e) { }
			return false;
		}
		private MapDecoderEntry findEntryToDownload() {
			synchronized(decoders) {	//Accessed from multiple threads
				final Iterator<MapDecoderEntry> i = decoders.values().iterator();
				while (i.hasNext()) {
					final MapDecoderEntry entry = i.next();
					if (!entry.isDownloaded() && !entry.isFailed()) {
						return entry;
					}
				}
			}
			return null;
		}
	}
	
	/** */
	class DecoderThread extends Thread {
		public DecoderThread() {
			super("Decoder");
			setDaemon(true);
			setPriority(Thread.MIN_PRIORITY);
		}
		
		public void run() {
			while (true) {
				if (!decode()) {
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
			try {
				wait();
			} catch(Exception e) { }
			return false;
		}
		private MapDecoderEntry findEntryToDecode(int resolution) {
			synchronized(decoders) {	//Accessed from multiple threads
				final Iterator<MapDecoderEntry> i = decoders.values().iterator();
				while (i.hasNext()) {
					final MapDecoderEntry entry = i.next();
					if (entry.getNextResolution() == resolution) {
						return entry;
					}
				}
			}
			return null;
		}
	}
}
