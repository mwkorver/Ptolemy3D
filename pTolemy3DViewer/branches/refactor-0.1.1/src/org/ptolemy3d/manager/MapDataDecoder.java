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
import java.util.Vector;
import java.util.Map.Entry;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.Config;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.view.Camera;

/*
 * TODO
 *  - Download dispatcher to mutlitple download unit (one unit by server)
 *  - Memory manager
 */

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDataDecoder {
	private final static int timeOut = 10 * 1000;
	
	/** Keep reference of all MapData */
	private final HashMap<Integer, HashMap<MapDataKey, MapDataEntries>> decoders;
	private final DownloadDispatcherThread downloadDispatcher;
	private final DecoderThread decoderThread;
	
	public MapDataDecoder() {
		decoders = new HashMap<Integer, HashMap<MapDataKey, MapDataEntries>>();
		downloadDispatcher = new DownloadDispatcherThread();
		decoderThread = new DecoderThread();
	}
	
	public void start() {
		//Let's start now, if nothing is to be done,
		//thread will be in wait state and will wake-up when entry will be added
		downloadDispatcher.start();
		decoderThread.start();
	}
	
	public MapDataKey getDownloadingMap() {
		return downloadDispatcher.mapDataKey;
	}
	public MapDataKey getDecodingMap() {
		return decoderThread.mapDataKey;
	}
	
	/** @return the <code>MapDecoderEntry</code> for the <code>key</code>, null if it did not exists */
	public MapDataEntries getIfExist(MapDataKey key) {
		final HashMap<MapDataKey, MapDataEntries> map = getHashMap(key.layer);
		final MapDataEntries decoder = map.get(key);
		if(decoder != null) {
			decoder.onRequest();
		}
		return decoder;
	}
	/** @return the <code>MapDecoderEntry</code> for the <code>key</code>.<BR>
	 *  A new <code>MapDecoderEntry</code> is created if it did not exists for the given <code>key</code>. */
	public MapDataEntries get(MapDataKey key) {
		final HashMap<MapDataKey, MapDataEntries> map = getHashMap(key.layer);
		MapDataEntries decoder = map.get(key);
		if (decoder == null) {
			decoder = new MapDataEntries(key);
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
	protected final HashMap<MapDataKey, MapDataEntries> getHashMap(int layer) {
		HashMap<MapDataKey, MapDataEntries> hashMap = decoders.get(layer);
		if (hashMap == null) {
			hashMap = new HashMap<MapDataKey, MapDataEntries>();
			decoders.put(layer, hashMap);
		}
		return hashMap;
	}

	/** */
	class DownloadDispatcherThread extends Thread {
		protected MapDataKey mapDataKey = null;
		
		public DownloadDispatcherThread() {
			super("DownloadDispatcher");
			setDaemon(true);
		}
		
		public void run() {
			while (true) {
				boolean found = false;
				for(int dataID = 0; dataID < MapDataEntries.NUM_DATAS; dataID++) {
					final MapDataEntries entry = findCloserEntryToDownload(dataID);
					if (entry != null) {
						mapDataKey = entry.mapData.key;
						final boolean downloaded = entry.download(dataID);
						mapDataKey = null;
						if(downloaded) {
							notifyDecoder();
						}
						
						found = true;
						break;
					}
				}
				
				if (!found) {
					notifyDecoder();
					
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
		
		private MapDataEntries findCloserEntryToDownload(int dataID) {
			if(Ptolemy3D.getCanvas() == null) {
				return null;
			}
			
			double dist = Double.MAX_VALUE;
			MapDataEntries closer = null;
			
			final Landscape landscape = Ptolemy3D.getScene().getLandscape();
			final Camera camera = Ptolemy3D.getCanvas().getCamera();
			final int numLayers = landscape.globe.getNumLayers();
			for(int layer = 0; layer < numLayers; layer++) {
				if(closer != null) {
					return closer;
				}
				
				final HashMap<MapDataKey, MapDataEntries> map = getHashMap(layer);
				synchronized(map) {	//Accessed from multiple threads
					final Iterator<MapDataEntries> j = map.values().iterator();
					while (j.hasNext()) {
						final MapDataEntries entry = j.next();
						if (!entry.isDownloaded(dataID) && !entry.isDownloadFailed(dataID)) {
							final double entryDist = landscape.globe.getMapDistanceFromCamera(entry.mapData.key, camera);
							if(entryDist < dist) {
								closer = entry;
								dist = entryDist;
							}
						}
					}
				}
			}
			return closer;
		}
		
		private void resetDownloadStates() {
			final int numLevels = Ptolemy3D.getScene().getLandscape().globe.getNumLayers();
			for(int layer = 0; layer < numLevels; layer++) {
				final HashMap<MapDataKey, MapDataEntries> map = getHashMap(layer);
				synchronized(map) {	//Accessed from multiple threads
					final Iterator<MapDataEntries> j = map.values().iterator();
					while (j.hasNext()) {
						final MapDataEntries entry = j.next();
						entry.resetDownloadFails();
					}
				}
			}
		}
	}
	
	/** */
	class DecoderThread extends Thread {
		protected MapDataKey mapDataKey = null;
		
		public DecoderThread() {
			super("Decoder");
			setDaemon(true);
			setPriority(Thread.MIN_PRIORITY);
		}
		
		public void run() {
			while (true) {
				if (!findCloserEntryToDecode()) {
					synchronized(this) {	//Wait / notify events
						try {
							IO.printlnParser("Decoder thread waiting ...");
							wait(timeOut);
							IO.printlnParser("Decoder thread wake-up.");
						} catch(IllegalMonitorStateException e) {
						} catch(InterruptedException e) {}
					}
				}
			}
		}
		private boolean findCloserEntryToDecode() {
			if(Config.DEBUG && ProfilerUtil.freezeDecoding) {
				return false;
			}
			if(Ptolemy3D.getCanvas() == null) {
				return false;
			}
			
			final int numLayers = Ptolemy3D.getScene().getLandscape().globe.getNumLayers();
			final int maxDecoderUnit = MapDataEntries.getMaxNumDecoderUnit();
			
			double closerDist = Double.MAX_VALUE;
			MapDataEntries closerEntry = null;
			int closerDataID = 0;
			
			final Landscape landscape = Ptolemy3D.getScene().getLandscape();
			final Camera camera = Ptolemy3D.getCanvas().getCamera();
search:		for(int layer = 0; layer < numLayers; layer++) {
				for(int decoderUnit = 0; decoderUnit < maxDecoderUnit; decoderUnit++) {
					for(int dataID = 0; dataID < MapDataEntries.NUM_DATAS; dataID++) {
						if(decoderUnit >= MapDataEntries.getNumDecoderUnit(dataID)) {
							continue;
						}
						final HashMap<MapDataKey, MapDataEntries> map = getHashMap(layer);
						synchronized(map) {	//Accessed from multiple threads
							final Iterator<MapDataEntries> j = map.values().iterator();
							while (j.hasNext()) {
								final MapDataEntries entry = j.next();
								if(entry.isDownloaded(dataID) && (entry.getNextDecoderUnit(dataID) == decoderUnit)) {
									final double dist = landscape.globe.getMapDistanceFromCamera(entry.mapData.key, camera);
									if(dist < closerDist) {
										closerEntry = entry;
										closerDataID = dataID;
										closerDist = dist;
									}
								}
							}
						}
						if(closerEntry != null) {
							break search;
						}
					}
				}
			}
			
			if (closerEntry != null) {
				try {
					mapDataKey = closerEntry.mapData.key;
					closerEntry.decode(closerDataID);
					mapDataKey = null;
				}
				catch(OutOfMemoryError e) {
					closerEntry.freeDecoder();
					garbageCollect();
					IO.printStackParser(e);
				}
				
				return true;
			}
			return false;
		}
	}
	
	protected void garbageCollect() {
		final Iterator<HashMap<MapDataKey, MapDataEntries>> i = decoders.values().iterator();
		while (i.hasNext()) {
			final HashMap<MapDataKey, MapDataEntries> map = i.next();
			synchronized(map) {	//Accessed from multiple threads
				final Iterator<MapDataEntries> j = map.values().iterator();
				while (j.hasNext()) {
					final MapDataEntries entry = j.next();
					entry.freeDecoder();
				}
			}
		}
		System.gc();
	}
	
	public void freeUnused() {
		final long t = MapDataEntries.getTime();
		final long dt = MapDataEntries.getMaxTime();
		
		final Vector<MapDataKey> keys = new Vector<MapDataKey>();
		
		final Iterator<HashMap<MapDataKey, MapDataEntries>> i = decoders.values().iterator();
		while (i.hasNext()) {
			final HashMap<MapDataKey, MapDataEntries> map = i.next();
			synchronized(map) {	//Accessed from multiple threads
				final Iterator<Entry<MapDataKey,MapDataEntries>> j = map.entrySet().iterator();
				while (j.hasNext()) {
					final Entry<MapDataKey,MapDataEntries> entry = j.next();
					final MapDataEntries mapEntry = entry.getValue();
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
		final MapDataEntries entry = getHashMap(key.layer).remove(key);
		if(entry != null) {
			IO.printfManager("Remove decoder: %s\n", key);
			return entry.mapData;
		}
		else {
			return null;
		}
	}
}
