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

import static org.ptolemy3d.debug.Config.DEBUG;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;

/*
 * FIXME
 *  - Download implemented as a single unit, but to be moved with multiple download units
 *  - Download retry
 *  - Memory manager
 *  - synchronize getCurrentResolution / getAndFreeCurrentWavelet
 */

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDataDecoder {
	private final static int timeOut = 10 * 1000;
	
	/** Keep reference of all MapData */
	private final HashMap<Integer, HashMap<MapDataKey, MapDecoderEntry>> decoders;
	private final DownloadDispatcherThread downloadDispatcher;
	private final DecoderThread decoderThread;
	
	public MapDataDecoder() {
		decoders = new HashMap<Integer, HashMap<MapDataKey, MapDecoderEntry>>();
		downloadDispatcher = new DownloadDispatcherThread();
		decoderThread = new DecoderThread();
		
		//Let's start now, if nothing is to be done,
		//thread will be in wait state and will wake-up when entry will be added
		downloadDispatcher.start();
		decoderThread.start();
	}
	
	/** @return the <code>MapDecoderEntry</code> for the <code>key</code>, null if it did not exists */
	public MapDecoderEntry getIfExist(MapDataKey key) {
		final HashMap<MapDataKey, MapDecoderEntry> map = getHashMap(key.layer);
		final MapDecoderEntry decoder = map.get(key);
		if(decoder != null) {
			decoder.onRequest();
		}
		return decoder;
	}
	/** @return the <code>MapDecoderEntry</code> for the <code>key</code>.<BR>
	 *  A new <code>MapDecoderEntry</code> is created if it did not exists for the given <code>key</code>. */
	public MapDecoderEntry get(MapDataKey key) {
		final HashMap<MapDataKey, MapDecoderEntry> map = getHashMap(key.layer);
		MapDecoderEntry decoder = map.get(key);
		if (decoder == null) {
			decoder = new MapDecoderEntry(key);
			IO.printfManager("New MapData: %s\n", key);

			synchronized(map) {	//Accessed from multiple threads
				map.put(key, decoder);
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
	protected final HashMap<MapDataKey, MapDecoderEntry> getHashMap(int layer) {
		HashMap<MapDataKey, MapDecoderEntry> hashMap = decoders.get(layer);
		if (hashMap == null) {
			hashMap = new HashMap<MapDataKey, MapDecoderEntry>();
			decoders.put(layer, hashMap);
		}
		return hashMap;
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
						notifyDecoder();
					}
				}
				else {
					synchronized(this) {	//Wait / notify events
						try {
							IO.printlnConnection("Download thread waiting ...");
							wait(timeOut);
							IO.printlnConnection("Download thread wake-up.");
						} catch(IllegalMonitorStateException e) {
							e.printStackTrace();
						} catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
					resetDownloadStates();
					notifyDecoder();
				}
			}
		}
		private void notifyDecoder() {
			synchronized(decoderThread) {	//Wait / notify events
				try {
					decoderThread.notify();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
		private MapDecoderEntry findEntryToDownload() {
			final int numLevels = Ptolemy3D.getScene().landscape.globe.getNumLayers();
			for(int layer = 0; layer < numLevels; layer++) {
				final HashMap<MapDataKey, MapDecoderEntry> map = getHashMap(layer);
				synchronized(map) {	//Accessed from multiple threads
					final Iterator<MapDecoderEntry> j = map.values().iterator();
					while (j.hasNext()) {
						final MapDecoderEntry entry = j.next();
						if (!entry.isDownloaded() && !entry.isDownloadFailed()) {
							return entry;
						}
					}
				}
			}
			return null;
		}
		private void resetDownloadStates() {
			final int numLevels = Ptolemy3D.getScene().landscape.globe.getNumLayers();
			for(int layer = 0; layer < numLevels; layer++) {
				final HashMap<MapDataKey, MapDecoderEntry> map = getHashMap(layer);
				synchronized(map) {	//Accessed from multiple threads
					final Iterator<MapDecoderEntry> j = map.values().iterator();
					while (j.hasNext()) {
						final MapDecoderEntry entry = j.next();
						entry.resetDownloadFails();
					}
				}
			}
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
				try {
					final boolean decoded = entry.decode();
					return decoded;
				}
				catch(OutOfMemoryError e) {
					entry.freeDecoder();
					garbageCollect();
					IO.printStackParser(e);
					return false;
				}
			}
			else {
				synchronized(this) {	//Wait / notify events
					try {
						IO.printlnParser("Decoder thread waiting ...");
						wait(timeOut);
						IO.printlnParser("Decoder thread wake-up.");
					} catch(IllegalMonitorStateException e) {
					} catch(InterruptedException e) {}
				}
				if (DEBUG) {
					dumpMap();
				}
				return false;
			}
		}
		private MapDecoderEntry findEntryToDecode() {
			final int numLayers = Ptolemy3D.getScene().landscape.globe.getNumLayers();
			for(int layer = 0; layer < numLayers; layer++) {
				for(int resolution = 0; resolution < MapData.MAX_NUM_RESOLUTION; resolution++) {
					final MapDecoderEntry entry = findEntryToDecode(layer, resolution);
					if (entry != null) {
						return entry;
					}
				}
			}
			return null;
		}
		private MapDecoderEntry findEntryToDecode(int layer, int resolution) {
			final HashMap<MapDataKey, MapDecoderEntry> map = getHashMap(layer);
			synchronized(map) {	//Accessed from multiple threads
				final Iterator<MapDecoderEntry> j = map.values().iterator();
				while (j.hasNext()) {
					final MapDecoderEntry entry = j.next();
					if(entry.isDownloaded() &&
					  (entry.getNextResolution() == resolution)) {
						return entry;
					}
				}
			}
			return null;
		}
		
		private void dumpMap() {
			final int numLayers = Ptolemy3D.getScene().landscape.globe.getNumLayers();
			for(int layer = 0; layer < numLayers; layer++) {
				dumpMap(layer);
			}
		}
		private void dumpMap(int layer) {
			final HashMap<MapDataKey, MapDecoderEntry> map = getHashMap(layer);
			synchronized(map) {	//Accessed from multiple threads
				final Iterator<MapDecoderEntry> j = map.values().iterator();
				while (j.hasNext()) {
					final MapDecoderEntry entry = j.next();
					IO.printfManager("Layer: %d, Res: %d, DL: %s\n",
							entry.mapData.key.layer, entry.mapData.mapResolution, entry.isDownloaded());
				}
			}
		}
	}
	
	protected void garbageCollect() {
		final Iterator<HashMap<MapDataKey, MapDecoderEntry>> i = decoders.values().iterator();
		while (i.hasNext()) {
			final HashMap<MapDataKey, MapDecoderEntry> map = i.next();
			synchronized(map) {	//Accessed from multiple threads
				final Iterator<MapDecoderEntry> j = map.values().iterator();
				while (j.hasNext()) {
					final MapDecoderEntry entry = j.next();
					entry.freeDecoder();
				}
			}
		}
		System.gc();
	}
	
	public void freeUnused() {
		final long t = MapDecoderEntry.getTime();
		final long dt = MapDecoderEntry.getMaxTime();
		
		final Vector<MapDataKey> keys = new Vector<MapDataKey>();
		
		final Iterator<HashMap<MapDataKey, MapDecoderEntry>> i = decoders.values().iterator();
		while (i.hasNext()) {
			final HashMap<MapDataKey, MapDecoderEntry> map = i.next();
			synchronized(map) {	//Accessed from multiple threads
				final Iterator<Entry<MapDataKey,MapDecoderEntry>> j = map.entrySet().iterator();
				while (j.hasNext()) {
					final Entry<MapDataKey,MapDecoderEntry> entry = j.next();
					final MapDecoderEntry mapEntry = entry.getValue();
					if((t - mapEntry.getLastRequestTime()) > dt) {
						keys.add(entry.getKey());
					}
				}
				
				for(MapDataKey key : keys) {
					remove(key);
				}
				keys.clear();
			}
		}
	}
	public MapData remove(MapDataKey key) {
		final MapDecoderEntry entry = getHashMap(key.layer).remove(key);
		if(entry != null) {
			IO.printfManager("Remove decoder: %s\n", key);
			return entry.mapData;
		}
		else {
			return null;
		}
	}
}
