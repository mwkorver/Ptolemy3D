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

import static org.ptolemy3d.debug.Config.DEBUG;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.Globe;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class MapDataManager {
	/** Map decoder queue */
	private final MapDataQueue decoderQueue;
	
	public MapDataManager() {
		decoderQueue = new MapDataQueue();
		decoderQueue.start();
	}
	
	public MapDataKey getDownloadingMap() {
		return decoderQueue.getDownloadingMap();
	}
	public MapDataKey getDecodingMap() {
		return decoderQueue.getDecodingMap();
	}
	
	/** Request a <code>MapData</code> for the given (<code>layer</code>,<code>lon</code>,<code>lat</code>) and return the closer. */
	public final MapData request(int layer, int lon, int lat) {
		return get(layer, lon, lat, true);
	}
	/** Get if exist the closer <code>MapData</code> for the given (<code>layer</code>,<code>lon</code>,<code>lat</code>) */
	public final MapData getIfExist(int layer, int lon, int lat) {
		return get(layer, lon, lat, false);
	}
	/** Request a <code>MapData</code> for the given (<code>layer</code>,<code>lon</code>,<code>lat</code>) */
	private final MapData get(int layer, int lon, int lat, boolean request) {
		//Request exact layer
		final MapDataKey exactKey = new MapDataKey(layer, lon, lat);
		final MapDataEntries exactEntry = decoderQueue.getIfExist(exactKey, request);
		if((exactEntry != null) && (exactEntry.mapData.hasTexture())) {
			return exactEntry.mapData;
		}
		
		//Request closest layer
		MapDataKey prevKey = exactKey;
		final Globe globe = Ptolemy3D.getScene().getLandscape().globe;
		for (int i = layer - 1; i > 0; i--) {
			final MapDataKey closeKey = globe.getCloserMap(i, lon, lat);
			if(DEBUG) {	//Keep this code to be sure we got the good key
				final int tileSize = Ptolemy3D.getScene().getLandscape().globe.getLayer(i).getTileSize();
				if((closeKey.lon <= lon) && ((closeKey.lon + tileSize) > lon) &&
				   (closeKey.lat >= lat) && ((closeKey.lat - tileSize) < lat)) {
					//OK
				}
				else {
					throw new RuntimeException();
				}
			}
			
			final MapDataEntries entry = decoderQueue.getIfExist(closeKey, request);
			if(entry != null) {
				final MapData curMapData = entry.mapData;
				if(curMapData.hasTexture()) {
					if(request) {
						//Request level below
						decoderQueue.get(prevKey);
					}
					return curMapData;
				}
			}
			
			prevKey = closeKey;
		}
		
		if(request) {
			//Request layer below
			decoderQueue.get(prevKey);
			
			//Request first layer
			final MapDataKey firstKey = globe.getCloserMap(0, lon, lat);
			final MapDataEntries firstEntry = decoderQueue.get(firstKey);
			return firstEntry.mapData;
		}
		
		return null;
	}
	
	/** @return the texture ID for the <code>MapData</code>. */
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
			return getAlternateTextureID(gl, mapData.key);
		}
		else {
			return textureID;
		}
	}
	/** @return alternate texture for the <code>key</code> */
	public int getAlternateTextureID(GL gl, MapDataKey key) {
		return 0;
	}
	
	public void freeUnused() {
		decoderQueue.freeUnused();
	}
}
