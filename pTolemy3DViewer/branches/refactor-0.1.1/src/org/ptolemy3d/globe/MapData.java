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
package org.ptolemy3d.globe;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.data.Texture;
import org.ptolemy3d.data.TextureManager;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class MapData {
	/** Number of resolution per jp2 tiles. */
	public final static int MAX_NUM_RESOLUTION = 4;

	/** Longitude in DD. */
	public final MapDataKey key;
	/** Current texture resolution (wavelet ID) */
	public transient int mapResolution;
	public transient Texture newTexture;
	/** TIN Elevation */
	public ElevationTin tin;
	/** DEM Elevation */
	public ElevationDem dem;

	/** We must go through manager to create a MapData, to avoid duplication. */
	public MapData(MapDataKey mapDataKey) {
		key = mapDataKey;
		
		// Texture may have been already partially or entirely loaded in GPU
		final TextureManager textureManager = Ptolemy3D.getTextureManager();
		mapResolution = textureManager.getTextureResolution(this);
		
		tin = null;
		dem = null;
	}

	@Override
	public boolean equals(Object obj) {
		return key.equals(obj);
	}
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	@Override
	public String toString() {
		return super.toString()+"["+key.toString()+"]";
	}
	
	/** @return true if the map has texture */
	public boolean hasTexture() {
		return (mapResolution >= 0);
	}

	/** @return true if the map has elevation data */
	public boolean hasElevation() {
		return (tin != null) || (dem != null);
	}

	/** */
	public int getLevel() { return key.layer; }
	/** */
	public int getLon() { return key.lon; }
	/** */
	public int getLat() { return key.lat; }
}