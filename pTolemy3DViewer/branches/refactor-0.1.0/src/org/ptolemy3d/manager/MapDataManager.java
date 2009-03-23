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

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.exceptions.Ptolemy3DException;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapData.MapDataKey;
import org.ptolemy3d.globe.MapData.MapWavelet;
import org.ptolemy3d.jp2.Jp2Head;

/**
 * @author Jérôme JOUVIE
 */
public class MapDataManager {
	/** Map to store MapData - This ensure a unique instance of MapData objects */
//	private final HashMap<MapDataKey, MapData> maps;
	
	public MapDataManager() {
//		maps = new HashMap<MapDataKey, MapData>();
	}
	
	public MapData get(int level, int mapSize, int lon, int lat) {
//		final MapDataKey key = new MapDataKey(level, mapSize, lon, lat);
		MapData mapData = /*maps.get(key);
		if (mapData == null) {
			mapData = */new MapData(level, mapSize, lon, lat);
//			maps.put(mapData.key, mapData);
//		}
		return mapData;
	}
	
	public void remove(MapData mapData) {
		// Free textures
		dropTo(mapData, -1);
		
		// Remove from table
//		MapData removed = maps.remove(mapData.key);
//		if (removed != mapData) {
//			IO.println("ERROR: fail to remove MapData ("+removed+")");
//		}
	}

	public final void dropTo(MapData mapData, int res) {
		final Jp2TileLoader tileLoader = Ptolemy3D.getTileLoader();
		for (int i = res + 1; i < mapData.wavelets.length; i++) {
			MapWavelet wavelet = mapData.wavelets[i];
			wavelet.tileGotten = false;
			wavelet.imageDataReady = false;
			wavelet.textureID = -1;
			if (wavelet.dataKey != -1) {
				tileLoader.quadBufferPool.free(i, wavelet.dataKey);
				wavelet.dataKey = -1;
			}
		}
		if (res < 0) {
			res = 0;
		}
		mapData.curRes = res;
	}
	
	public final void moveToTrash(MapData mapData) {
		final Jp2TileLoader tileLoader = Ptolemy3D.getTileLoader();
		for (int i = 0; i < mapData.wavelets.length; i++) {
			MapWavelet wavelet = mapData.wavelets[i];
			wavelet.tileGotten = false;
			wavelet.imageDataReady = false;
 			if (wavelet.textureID != -1) {
 				tileLoader.addToTrash(wavelet.textureID, wavelet.dataKey);
				wavelet.textureID = -1;
			}
			wavelet.dataKey = -1;
		}
		mapData.curRes = 0;
	}
	
	public boolean setImageData(MapData mapData, byte[] data, int[] meta, int res) {
		final Jp2TileLoader tileLoader = Ptolemy3D.getTileLoader();
		byte[] imageData = tileLoader.quadBufferPool.setArray(res, mapData.wavelets);
		if (imageData == null) {
			return false;
		}
		if (imageData.length != (meta[0] * meta[1] * 3)) {
			return false;
			// just copy over array
		}

		if (meta[3] == 3) {
			System.arraycopy(data, 0, imageData, 0, data.length);
		}
		else if (meta[3] == 4) {
			for (int i = 0; i < meta[1]; i++) {
				for (int j = 0; j < meta[0]; j++) {
					System.arraycopy(data, i * meta[0] * 4 + j * 4, imageData, i * meta[0] * 3 + j * 3, 3);
				}
			}
		}
		mapData.getWavelet(res).imageDataReady = true;
		mapData.curRes = res;
		return true;
	}

	public boolean setImageData(MapData mapData, short[][] data, int res, Jp2Head jp2h) {
		final Jp2TileLoader tileLoader = Ptolemy3D.getTileLoader();
		byte[] imageData = tileLoader.quadBufferPool.setArray(res, mapData.wavelets);
		if (imageData == null) {
			return false;
		}

		int width = tileLoader.tileSize[res];
		jp2h.setImageData(imageData, data, width);

		mapData.getWavelet(res).imageDataReady = true;
		mapData.curRes = res;
		return true;
	}
	
	public byte[] getImageData(MapData mapData, int res) {
		final Jp2TileLoader tileLoader = Ptolemy3D.getTileLoader();
		final MapWavelet wavelet = mapData.getWavelet(res);
		if (wavelet.dataKey >= 0) {
			return tileLoader.quadBufferPool.getArray(res, wavelet.dataKey);
		}
		else {
			IO.println("ERROR: trying to request data that isn't there!");
			return null;
		}
	}
}
