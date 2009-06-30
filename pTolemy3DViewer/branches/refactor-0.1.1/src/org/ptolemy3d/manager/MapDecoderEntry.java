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

import java.io.IOException;
import java.net.URL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;
import org.ptolemy3d.io.MapDataFinder;
import org.ptolemy3d.io.Stream;
import org.ptolemy3d.jp2.Decoder;
import org.ptolemy3d.jp2.fast.JJ2000FastDecoder;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDecoderEntry {
	/* Avoid storing tile in memory */
	private final static boolean AUTO_FREE = true;
	
	/** */
	public final MapData mapData;
	
	/** Stream downloaded */
	private Stream stream;
	/** Track the number of file request fails */
	public int streamNotFound;
	/** Track number of file download fail */
	public int downloadFail;
	
	/** Decoder */
	private Decoder decoder;
	/** Number of wavelets to decode */
	private int numWavelets;
	
	/** */
	private long lastRequest;
	
	public MapDecoderEntry(MapDataKey key) {
		this.mapData = new MapData(key);
		
		streamNotFound = 0;
		downloadFail = 0;
		
		numWavelets = -1;
	}
	
	protected void onRequest() {
		lastRequest = getTime();
	}
	
	protected long getLastRequestTime() {
		return lastRequest;
	}
	protected static long getTime() {
		return System.currentTimeMillis();
	}
	protected static long getMaxTime() {
		return 5 * 1000;
	}
	
	/** @return true if the map has been downloaded */
	public boolean isDownloaded() {
		if (stream == null) {
			// Attempt to get from cache
			final MapDataFinder mapDataFinder = Ptolemy3D.getMapDataFinder();
			stream = mapDataFinder.findMapDataTextureFromCache(mapData);
		}
		return (stream != null) && (stream.isDownloaded());
	}
	
	/** @return true if the map has been downloaded */
	public boolean isDownloadFailed() {
		return (streamNotFound > 0) || (downloadFail > 0);
	}
	
	/** Marked as not failed */
	public void resetDownloadFails() {
		streamNotFound = 0;
		downloadFail = 0;
	}
	
	/** @return true if the data has been downloaded */
	public boolean download() {
		if(isDownloaded()) {
			return true;
		}
		
		if(stream == null) {
			final MapDataFinder mapDataFinder = Ptolemy3D.getMapDataFinder();
			final URL url = mapDataFinder.findMapDataTexture(mapData);
			if(url == null) {
				streamNotFound++;
				return false;
			}
			stream = new Stream(url);
			streamNotFound = 0;
		}
		try {
			final boolean downloaded = stream.download();
			if(downloaded) {
				downloadFail = 0;
			}
			return downloaded;
		}
		catch(IOException e) {
			downloadFail++;
			IO.printStackConnection(e);
			return false;
		}
	}
	
	/** @return the next resolution ID to decode */
	public int getNextResolution() {
		return mapData.mapResolution + 1;
	}
	
	/** @return true if the resolution is or has been decoded */
	public boolean decode() {
		if(!isDownloaded()) {
			return false;
		}
		
		final int nextResolution = getNextResolution();
		if((numWavelets >= 0) && (nextResolution >= numWavelets)) {
			return true;
		}
		if(decoder == null) {
			decoder = new JJ2000FastDecoder(stream);	//A lighter / faster version of JJ200 Decoder
//			decoder = new JJ2000Decoder(stream);		//JJ200 Decoder
			numWavelets = decoder.getNumWavelets();
		}
		
		IO.printfParser("Parse wavelet: %s@%d/%d\n", mapData.key, (nextResolution + 1), numWavelets);
		final Texture texture;
		try {
			texture = decoder.parseWavelet(nextResolution);
		}
		catch(OutOfMemoryError e) {
			throw e;
		}
		if(AUTO_FREE) {
			freeDecoder();
		}
		boolean decoded = (texture != null);
		if(decoded) {
			mapData.newTexture = texture;
			mapData.mapResolution = nextResolution;
		}
		else {
			//Invalidate cache
			stream.invalidateCache();
		}
		// Check if the decoded has more map
		if(nextResolution >= numWavelets) {
			IO.printlnParser("Decoding finished ...");
			freeDecoder();
		}
		return decoded;
	}
	
	protected void freeDecoder() {
		decoder = null;
	}
}
