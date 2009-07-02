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

import java.security.InvalidParameterException;

import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDataEntries {
	public final static int NUM_DATAS = 3;
	
	/** */
	public final MapData mapData;
	/** Data order by priority */
	public MapDataEntry[] datas;
	
	/** */
	private long lastRequest;
	
	public MapDataEntries(MapDataKey key) {
		this.mapData = new MapData(key);
		this.datas = new MapDataEntry[] {new Jp2DataEntry(mapData), new TinDataEntry(mapData), new DemDataEntry(mapData)};
	}
	
	public boolean isDownloaded(int dataID) {
		return datas[dataID].isDownloaded();
	}
	public boolean isDownloadFailed(int dataID) {
		return datas[dataID].isDownloadFailed();
	}
	public boolean download(int dataID) {
		return datas[dataID].download();
	}
	public int getNextDecoderUnit(int dataID) {
		return datas[dataID].getNextDecoderUnit();
	}
	public boolean decode(int dataID) {
		return datas[dataID].decode();
	}
	
	public void freeDecoder() {
		for (int dataID = 0; dataID < datas.length; dataID++) {
			datas[dataID].freeDecoder();
		}
	}
	public void resetDownloadFails() {
		for (int dataID = 0; dataID < datas.length; dataID++) {
			datas[dataID].resetDownloadFails();
		}
	}
	
	public final static int getNumDecoderUnit(int dataID) {
		switch (dataID) {
			case 0: return Jp2DataEntry.NUM_DECODERUNIT;
			case 1: return TinDataEntry.NUM_DECODERUNIT;
			case 2: return DemDataEntry.NUM_DECODERUNIT;
		}
		throw new InvalidParameterException();
	}
	public final static int getMaxNumDecoderUnit() {
		int max = 0;
		for (int dataID = 0; dataID < NUM_DATAS; dataID++) {
			final int value = getNumDecoderUnit(dataID);
			if(value > max) {
				max = value;
			}
		}
		return max;
	}
	
	/* Garbage */
	
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
}
