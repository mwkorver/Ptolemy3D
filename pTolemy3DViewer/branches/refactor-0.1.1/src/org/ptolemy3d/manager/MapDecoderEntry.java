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

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.io.MapDataFinder;
import org.ptolemy3d.io.Stream;
import org.ptolemy3d.jp2.Jp2Decoder;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDecoderEntry {
	/** */
	public final MapData mapData;
	/** */
	public int resolution;
	
	/** */
	private Stream stream;
	/** */
	private Jp2Decoder decoder;
	/** */
	private long lastRequest;
	
	public MapDecoderEntry(MapData mapData) {
		this.mapData = mapData;
	}
	
	protected void onRequest() {
		lastRequest = System.currentTimeMillis();
	}
	
	/** @return true if the map has been downloaded */
	public boolean isDownloaded() {
		return (stream != null) && (stream.isDownloaded());
	}
	
	/** @return true if the data has been downloaded */
	public boolean download() {
		if(isDownloaded()) {
			return true;
		}
		
		if(stream == null) {
			final MapDataFinder mapDataFinder = Ptolemy3D.getMapDataFinder();
			stream = new Stream(mapDataFinder.findMapData(mapData));
		}
		try {
			return stream.download();
		}
		catch(IOException e) {
			IO.printStackConnection(e);
			//FIXME handle download fail
			return false;
		}
	}
	
	/** @return true if the resolution has already been decoded */
	public boolean isDecoded(int resolution) {
		return (decoder != null) && (decoder.hasWavelet(resolution));
	}
	
	/** @return true if the resolution is or has been decoded */
	public boolean decode(int resolution) {
		if(!isDownloaded()) {
			return false;
		}
		if(isDecoded(resolution)) {
			return true;
		}
		if(decoder == null) {
			decoder = new Jp2Decoder(stream);
		}
		return (decoder.parseWavelet(resolution) != null);
	}
	
	/** @return the resolution, null if not yet decoded. */
	public Texture getWavelet(int resolution) {
		if(decoder != null) {
			return decoder.getWavelet(resolution);
		}
		return null;
	}
}
