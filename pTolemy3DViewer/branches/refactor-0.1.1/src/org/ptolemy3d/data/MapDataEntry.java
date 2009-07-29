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

import java.io.IOException;
import java.net.URL;

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.io.DataFinder;
import org.ptolemy3d.io.Stream;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
abstract class MapDataEntry {
	/** */
	public final MapData mapData;
	
	/** Stream downloaded */
	private Stream stream;
	/** Track the number of file request fails */
	private int streamNotFound;
	/** Track number of file download fail */
	private int downloadFail;
	
	public MapDataEntry(MapData mapData) {
		this.mapData = mapData;
		this.stream = null;
		this.streamNotFound = 0;
		this.downloadFail = 0;
	}
	
	/* Map Download */

	/** @return true if the map has been downloaded */
	public boolean isDownloaded() {
		return (stream != null) && (stream.isDownloaded());
	}
	
	/** @return true if the map has been downloaded */
	public boolean isJustDownloaded() {
		if (stream == null) {
			// Attempt to get from cache
			stream = findDataFromCache(mapData);
		}
		return isDownloaded();
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
	public final boolean download() {
		if(isJustDownloaded()) {
			return true;
		}
		
		if(stream == null) {
			final URL url = findData(mapData);
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
	
	/** @return the Stream if it is donwload */
	public Stream getStream() {
		return stream;
	}
	
	/** @return the URL of the data
	 * @see DataFinder */
	public abstract URL findData(MapData mapData);
	public abstract Stream findDataFromCache(MapData mapData);
	
	/* Data Decoding */
	
	/** @return the next resolution ID to decode */
	public abstract int getNextDecoderUnit();
	/** @return true if decoded */
	public abstract boolean isDecoded(int unit);
	/** @return true if the resolution is or has been decoded */
	public abstract boolean decode();
	/** */
	public abstract void freeDecoder();
	
}
