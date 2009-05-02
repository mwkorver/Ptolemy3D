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

import org.ptolemy3d.debug.IO;
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
	
	/** @return the current resolution ID, -1 if no resolution decoded.
	 *  @see #getCurrentWavelet(MapData, int) */
	public int getCurrentResolution(MapData mapData) {
		final MapDecoderEntry decoder = getDecoderEntry(mapData);
		return decoder.getCurrentResolution();
	}
	/** @return the current wavelet.
	 *  @see #getCurrentResolution(MapData) */
	public Texture getCurrentWavelet(MapData mapData, int resolution) {
		final MapDecoderEntry decoder = getDecoderEntry(mapData);
		return decoder.getCurrentWavelet();
	}
	private MapDecoderEntry getDecoderEntry(MapData mapData) {
		MapDecoderEntry decoder = decoders.get(mapData.key);
		if (decoder == null) {
			decoder = new MapDecoderEntry(mapData);
			synchronized(decoders) {	//Accessed from multiple threads
				decoders.put(mapData.key, decoder);
				synchronized(downloadDispatcher) {	//Wait / notify events
					try {
						downloadDispatcher.notify();
					} catch(IllegalMonitorStateException e) {
						e.printStackTrace();
					}
				}
			}
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
				final MapDecoderEntry entry = findEntryToDownload();
				if (entry != null) {
					final boolean downloaded = entry.download();
					if(downloaded) {
						synchronized(decoderThread) {	//Wait / notify events
							try {
								decoderThread.notify();
							} catch(Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
				else {
					synchronized(this) {	//Wait / notify events
						try {
							IO.printlnConnection("Download thread waiting ...");
							wait();
							IO.printlnConnection("Download thread wake-up.");
						} catch(IllegalMonitorStateException e) {
							e.printStackTrace();
						} catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		private MapDecoderEntry findEntryToDownload() {
			synchronized(decoders) {	//Accessed from multiple threads
				final Iterator<MapDecoderEntry> i = decoders.values().iterator();
				while (i.hasNext()) {
					final MapDecoderEntry entry = i.next();
					if (!entry.isDownloaded() && !entry.isDownloadFailed()) {
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
				decode();
			}
		}
		private boolean decode() {
			final MapDecoderEntry entry = findEntryToDecode();
			if (entry != null) {
				final boolean decoded = entry.decode();
				return decoded;
			}
			else {
				synchronized(this) {	//Wait / notify events
					try {
						IO.printlnParser("Decoder thread waiting ...");
						wait();
						IO.printlnParser("Decoder thread wake-up.");
					} catch(IllegalMonitorStateException e) {
					} catch(InterruptedException e) {}
				}
				return false;
			}
		}
		private MapDecoderEntry findEntryToDecode() {
			for(int resolution = 0; resolution < MapData.MAX_NUM_RESOLUTION; resolution++) {
				final MapDecoderEntry entry = findEntryToDecode(resolution);
				if (entry != null) {
					return entry;
				}
			}
			return null;
		}
		private MapDecoderEntry findEntryToDecode(int resolution) {
			synchronized(decoders) {	//Accessed from multiple threads
				final Iterator<MapDecoderEntry> i = decoders.values().iterator();
				while (i.hasNext()) {
					final MapDecoderEntry entry = i.next();
					if (entry.isDownloaded() && (entry.getNextResolution() == resolution)) {
						return entry;
					}
				}
			}
			return null;
		}
	}
}
