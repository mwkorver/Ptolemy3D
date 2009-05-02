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
import java.util.Map.Entry;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class MapDataManager {
	/** Keep reference of all MapData */
	private final HashMap<MapDataKey, MapData> mapDatas;
	/** Map decoder queue */
	private final MapDataDecoder decoderQueue;
	
	public MapDataManager() {
		mapDatas = new HashMap<MapDataKey, MapData>();
		decoderQueue = new MapDataDecoder();
	}
	
	public MapData get(MapDataKey key) {
		MapData mapData = mapDatas.get(key);
		if (mapData == null) {
			mapData = new MapData(key);
			mapDatas.put(mapData.key, mapData);
			IO.printfManager("New MapData: %s\n", mapData.key);
		}
		return mapData;
	}
	
	public void remove(MapData mapData) {
		// Remove from table
		final MapData removed = mapDatas.remove(mapData.key);
		if (removed == mapData) {
			// Unload texture
		}
		else {
			IO.println("ERROR: fail to remove MapData ("+removed+")");
		}
	}

	public final MapData getMapDataHeader(int levelID, int mapSize, int lon, int lat) {
		final MapDataKey exactKey = new MapDataKey(levelID, mapSize, lon, lat);
		final MapData mapData = mapDatas.get(exactKey);
		if (mapData != null) {
			return mapData;
		}
		
		for (int level = levelID - 1; level >= 0; level--) {
			final Iterator<Entry<MapDataKey,MapData>> i = mapDatas.entrySet().iterator();
			while (i.hasNext()) {
				final Entry<MapDataKey,MapData> entry = i.next();
				final MapDataKey key = entry.getKey();
				if ((key.lon <= lon) && ((key.lon + key.mapSize) > lon) &&
					(key.lat >= lat) && ((key.lat - key.mapSize) < lat)) {
					return entry.getValue();
				}
			}
		}
		
		return get(exactKey);
	}
	
	public int getTextureID(GL gl, MapData mapData) {
		//Request map data
		final int resolution = decoderQueue.getCurrentResolution(mapData);
		
		//Texture: acquire or loading
		final TextureManager textureManager = Ptolemy3D.getTextureManager();
		final int textureID;
		if(resolution > mapData.mapResolution) {
			IO.printfRenderer("Loading texture: %s@%d\n", mapData.key, resolution);
			// Load resolution
			final Texture texture = decoderQueue.getCurrentWavelet(mapData, resolution);
			textureID = textureManager.load(gl, mapData, texture, true);
			mapData.mapResolution = resolution;
		}
		else {
			textureID = textureManager.get(mapData);
		}
		
		//Alternate texture if not loaded
		if(textureID == 0) {
			return getAlternateTextureID(gl);
		}
		else {
			return textureID;
		}
	}
	public int getAlternateTextureID(GL gl) {
		return 0;
	}
}
