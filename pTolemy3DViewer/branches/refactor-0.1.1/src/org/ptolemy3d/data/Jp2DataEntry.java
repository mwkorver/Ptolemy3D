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
package org.ptolemy3d.data;

import java.net.URL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.io.DataFinder;
import org.ptolemy3d.io.Stream;
import org.ptolemy3d.jp2.Decoder;
import org.ptolemy3d.jp2.fast.JJ2000FastDecoder;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class Jp2DataEntry extends MapDataEntry {
	public final static int NUM_DECODERUNIT = MapData.MAX_NUM_RESOLUTION;
	
	/* Avoid storing tile in memory */
	private final static boolean AUTO_FREE = true;
	
	/** Decoder */
	private Decoder decoder;
	/** Number of wavelets to decode */
	private int numWavelets;
	
	public Jp2DataEntry(MapData mapData) {
		super(mapData);
		numWavelets = -1;
	}
	
	@Override
	public URL findData(MapData mapData) {
		final DataFinder dataFinder = Ptolemy3D.getDataFinder();
		return dataFinder.findJp2(mapData);
	}

	@Override
	public Stream findDataFromCache(MapData mapData) {
		final DataFinder dataFinder = Ptolemy3D.getDataFinder();
		return dataFinder.findJp2FromCache(mapData);
	}

	@Override
	public boolean decode() {
		if(!isDownloaded()) {
			return false;
		}
		final Stream stream = getStream();
		
		final int nextResolution = getNextDecoderUnit();
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

	@Override
	public int getNextDecoderUnit() {
		final int next = mapData.mapResolution + 1;
		if(next >= NUM_DECODERUNIT) {
			return -1;
		}
		return next;
	}

	@Override
	public void freeDecoder() {
		decoder = null;
	}

	@Override
	public boolean isDecoded(int unit) {
		return false;
	}
}
