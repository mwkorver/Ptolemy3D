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

import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;
import org.ptolemy3d.jp2.Jp2Decoder;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class MapDecoderQueue {
	static class MapDecoderEntry {
		public final MapData mapData;
		Jp2Decoder decoder;
		Texture texture;
		int lastRequest;
		public MapDecoderEntry(MapData mapData) {
			this.mapData = mapData;
		}
	}
	
	/** Keep reference of all MapData */
	private final HashMap<MapDataKey, MapDecoderEntry> decoders;
	
	public MapDecoderQueue() {
		decoders = new HashMap<MapDataKey, MapDecoderEntry>();
	}
	
	public Texture request(MapData mapData, int resolution) {
		final MapDecoderEntry decoder = decoders.get(mapData.key);
		if (decoder == null) {
			decoders.put(mapData.key, new MapDecoderEntry(mapData));
			return null;
		}
		else {
//			decoder.lastRequest = ;
			return decoder.texture;
		}
	}
	
}
