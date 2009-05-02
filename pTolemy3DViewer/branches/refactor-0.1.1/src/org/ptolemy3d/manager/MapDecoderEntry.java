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
import org.ptolemy3d.io.MapDataFinder;
import org.ptolemy3d.io.Stream;
import org.ptolemy3d.jp2.Decoder;
import org.ptolemy3d.jp2.Jp2Decoder;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDecoderEntry {
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
	/** Decoder texture wavelet */
	private Texture wavelet;
	/** Resolution decoded */
	private int nextResolution;
	
	/** */
	private long lastRequest;
	
	public MapDecoderEntry(MapData mapData) {
		this.mapData = mapData;
		
		streamNotFound = 0;
		downloadFail = 0;
		nextResolution = 0;
		
		numWavelets = -1;
	}
	
	protected void onRequest() {
		lastRequest = System.currentTimeMillis();
	}
	
	/** @return true if the map has been downloaded */
	public boolean isDownloaded() {
		return (stream != null) && (stream.isDownloaded());
	}
	
	/** @return true if the map has been downloaded */
	public boolean isDownloadFailed() {
		return (streamNotFound > 0) || (downloadFail > 0);
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
	
	/** @return the current resolution ID, -1 if no resolution decoded.
	 *  @see #getCurrentWavelet() */
	public int getCurrentResolution() {
		return nextResolution - 1;
	}
	/** @return the current wavelet.
	 *  @see #getCurrentResolution() */
	public Texture getCurrentWavelet() {
		return wavelet;
	}
	
	/** @return the next resolution ID to decode */
	public int getNextResolution() {
		return nextResolution;
	}
	
	/** @return true if the resolution is or has been decoded */
	public boolean decode() {
		if(!isDownloaded()) {
			return false;
		}
		if((numWavelets >= 0) && (nextResolution >= numWavelets)) {
			return true;
		}
		if(decoder == null) {
			decoder = new Jp2Decoder(stream);
			numWavelets = decoder.getNumWavelets();
		}
		
		final Texture texture = decoder.parseWavelet(nextResolution);
		boolean decoded = (texture != null);
		if(decoded) {
			nextResolution++;
			wavelet = texture;
		}
		// Check if the decoded has more map
		if(nextResolution >= numWavelets) {
			IO.printlnParser("Decoding finished ...");
			decoder = null;
		}
		return decoded;
	}
}
