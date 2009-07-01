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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.ElevationDem;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;
import org.ptolemy3d.io.DataFinder;
import org.ptolemy3d.io.Stream;
import org.ptolemy3d.jp2.Decoder;
import org.ptolemy3d.jp2.fast.JJ2000FastDecoder;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDataEntry {
	static class DownloadData {
		/** Stream downloaded */
		private Stream stream;
		/** Track the number of file request fails */
		public int streamNotFound;
		/** Track number of file download fail */
		public int downloadFail;
		
		public DownloadData() {
			stream = null;
			resetDownloadFails();
		}
		
		public boolean isDownloaded() {
			return (stream  != null) && (stream.isDownloaded());
		}
		public boolean isDownloadFailed() {
			return (streamNotFound > 0) || (downloadFail > 0);
		}
		
		public void resetDownloadFails() {
			streamNotFound = 0;
			downloadFail = 0;
		}
	}
	
	/* Avoid storing tile in memory */
	private final static boolean AUTO_FREE = true;
	
	/** */
	public final MapData mapData;
	
	/** Stream downloaded */
	private final DownloadData mapStream;
	/** DEM downloaded */
	private final DownloadData demStream;
	
	/** Decoder */
	private Decoder decoder;
	/** Number of wavelets to decode */
	private int numWavelets;
	
	/** */
	private long lastRequest;
	
	public MapDataEntry(MapDataKey key) {
		this.mapData = new MapData(key);
		
		mapStream = new DownloadData();
		demStream = new DownloadData();
		
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
	
	/** Marked as not failed */
	public void resetDownloadFails() {
		mapStream.resetDownloadFails();
		demStream.resetDownloadFails();
	}
	
	/* Map Download */

	/** @return true if the map has been downloaded */
	public boolean isMapDownloaded() {
		if (mapStream.stream == null) {
			// Attempt to get from cache
			final DataFinder dataFinder = Ptolemy3D.getDataFinder();
			mapStream.stream = dataFinder.findJp2FromCache(mapData);
		}
		return mapStream.isDownloaded();
	}
	
	/** @return true if the map has been downloaded */
	public boolean isMapDownloadFailed() {
		return mapStream.isDownloadFailed();
	}
	
	/** @return true if the data has been downloaded */
	public boolean downloadMap() {
		if(isMapDownloaded()) {
			return true;
		}
		
		if(mapStream.stream == null) {
			final DataFinder dataFinder = Ptolemy3D.getDataFinder();
			final URL url = dataFinder.findJp2(mapData);
			if(url == null) {
				mapStream.streamNotFound++;
				return false;
			}
			mapStream.stream = new Stream(url);
			mapStream.streamNotFound = 0;
		}
		try {
			final boolean downloaded = mapStream.stream.download();
			if(downloaded) {
				mapStream.downloadFail = 0;
			}
			return downloaded;
		}
		catch(IOException e) {
			mapStream.downloadFail++;
			IO.printStackConnection(e);
			return false;
		}
	}
	
	/* Map Decoding */
	
	/** @return the next resolution ID to decode */
	public int getNextResolution() {
		return mapData.mapResolution + 1;
	}
	
	/** @return true if the resolution is or has been decoded */
	public boolean decode() {
		if(!isMapDownloaded()) {
			return false;
		}
		
		final int nextResolution = getNextResolution();
		if((numWavelets >= 0) && (nextResolution >= numWavelets)) {
			return true;
		}
		if(decoder == null) {
			decoder = new JJ2000FastDecoder(mapStream.stream);	//A lighter / faster version of JJ200 Decoder
//			decoder = new JJ2000Decoder(mapStream.stream);		//JJ200 Decoder
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
			mapStream.stream.invalidateCache();
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
	
	/* DEM Downloading */
	
	/** @return true if the map has been downloaded */
	public boolean isDemDownloaded() {
		if (demStream.stream == null) {
			// Attempt to get from cache
			final DataFinder dataFinder = Ptolemy3D.getDataFinder();
			demStream.stream = dataFinder.findDemFromCache(mapData);
		}
		final boolean downloaded = demStream.isDownloaded();
		if (downloaded) {
			decodeDem();
		}
		return downloaded;
	}
	
	/** @return true if the map has been downloaded */
	public boolean isDemDownloadFailed() {
		return demStream.isDownloadFailed();
	}
	
	/** @return true if the data has been downloaded */
	public boolean downloadDem() {
		if(isDemDownloaded()) {
			return true;
		}
		
		if(demStream.stream == null) {
			final DataFinder dataFinder = Ptolemy3D.getDataFinder();
			final URL url = dataFinder.findDemData(mapData);
			if(url == null) {
				demStream.streamNotFound++;
				return false;
			}
			demStream.stream = new Stream(url);
			demStream.streamNotFound = 0;
		}
		try {
			final boolean downloaded = demStream.stream.download();
			if(downloaded) {
				demStream.downloadFail = 0;
			}
			return downloaded;
		}
		catch(IOException e) {
			demStream.downloadFail++;
			IO.printStackConnection(e);
			return false;
		}
	}
	
	public void decodeDem() {
		if (demStream.stream != null && mapData.dem == null) {
			try {
				File file = demStream.stream.createFile();
				int length = (int)file.length();
				byte[] b = new byte[length];
				new FileInputStream(file).read(b);
				mapData.dem = new ElevationDem(b);
			}
			catch(Throwable t) {
				t.printStackTrace();
			}
		}
	}
}
