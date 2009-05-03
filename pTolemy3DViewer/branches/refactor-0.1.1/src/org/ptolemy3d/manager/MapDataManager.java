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

/*
 * FIXME
 *  - getMapDataHeader: from level 0 to max
 */

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class MapDataManager {
	/** Map decoder queue */
	private final MapDataDecoder decoderQueue;
	
	public MapDataManager() {
		decoderQueue = new MapDataDecoder();
	}
	
	private MapData request(MapDataKey key) {
		return decoderQueue.getDecoder(key).mapData;
	}
	
	public void freeUnused() {
		decoderQueue.freeUnused();
	}
	
	public final MapData getMapDataHeader(int levelID, int lon, int lat) {
		final MapDataKey exactKey = new MapDataKey(levelID, lon, lat);
		final MapData mapData = request(exactKey);
		if(mapData.hasTexture()) {
			return mapData;
		}
		
		for (int l = levelID - 1; l >= 0; l--) {
			final int mapSize = Ptolemy3D.getScene().landscape.globe.getTileSize(l);
			final HashMap<MapDataKey,MapDecoderEntry> map = decoderQueue.getHashMap(l);
			synchronized(map) {
				final Iterator<Entry<MapDataKey,MapDecoderEntry>> i = map.entrySet().iterator();
				while (i.hasNext()) {
					final Entry<MapDataKey,MapDecoderEntry> entry = i.next();
					final MapDataKey key = entry.getKey();
					if((key.lon <= lon) && ((key.lon + mapSize) > lon) &&
					   (key.lat >= lat) && ((key.lat - mapSize) < lat)) {
						final MapDecoderEntry decoderEntry = entry.getValue();
						decoderEntry.onRequest();
						
						final MapData curMapData = decoderEntry.mapData;
						if(curMapData.hasTexture()) {
							return curMapData;
						}
					}
				}
			}
		}
		
		return mapData;
	}
	
	/** Acquire texture ID */
	public int getTextureID(GL gl, MapData mapData) {
		//Acquire texture ID, and update if necessary
		final int textureID;
		final TextureManager textureManager = Ptolemy3D.getTextureManager();
		if(mapData.newTexture != null) {
			final Texture texture = mapData.newTexture;
			mapData.newTexture = null;
			textureID = textureManager.load(gl, mapData, texture, true);
			IO.printfRenderer("Loading texture: %s@%d\n", mapData.key, mapData.mapResolution);
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
