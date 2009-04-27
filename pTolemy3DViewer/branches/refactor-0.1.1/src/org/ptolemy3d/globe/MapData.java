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
import org.ptolemy3d.manager.Texture;

/**
 * This class holds info for our tile data
 */
public class MapData {
	/** Number of resolution per jp2 tiles. */
	public final static int MAX_NUM_RESOLUTION = 4;

	/** Longitude in DD. */
	public final MapDataKey key;
	/** Jp2 resolutions */
	public final Texture[] wavelets;
	/** Current resolution */
	public int curRes = 0;
	/** TIN Elevation */
	public ElevationTin tin;
	/** DEM Elevation */
	public ElevationDem dem;

	/* JP2 Location */
	/** Server ID of the JP2 Tile. */
	public int dataServerID;
	/** Location ID of the JP2 Tile. */
	public int dataLocID;
	/** Path of the JP2 Tile relative to the location. */
	public String fileBase;

	/** Tells if the map request to the server has been finished */
	public boolean mapRequest = false;
	/** Tells if the elevation request to the server has been finished */
	public boolean elevationRequest = false;

	public boolean hasData = false;	//FIXME Remove
	public boolean inScene;

	/** We must go through manager to create a MapData, to avoid duplication.
	 * @deprecated Use MapDataManager instead */
	public MapData(MapDataKey mapDataKey) {
		key = mapDataKey;
		wavelets = new Texture[MAX_NUM_RESOLUTION];
		
		dataServerID = 0;
		dataLocID = 0;
		fileBase = createFileBaseName();
		
		mapRequest = false;
		hasData = false;

		elevationRequest = false;
		tin = null;
		dem = null;
		
		curRes = 0;
	}

	@Override
	public boolean equals(Object obj) {
		return key.equals(obj);
	}
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	private final String createFileBaseName() {
		final Layer[] levels = Ptolemy3D.getScene().landscape.getLayers();

		String fileBase = String.valueOf(key.mapSize) + "/";
		if (levels != null) {
			final int divider = levels[key.level].getDivider();

			if (divider > 0) {	//as usual
				fileBase += "D" + divider + "/x";
				int div_ = (key.lon / divider);
				fileBase += String.valueOf((div_ > 0) ? String.valueOf(div_) : "n" + (-div_));
				div_ = (key.lat / divider);
				fileBase += "y" + ((div_ > 0) ? String.valueOf(div_) : ("n" + (-div_))) + "/";
			}
			else if (divider < 0) {	//super funky style
				fileBase += key.lon < 0 ? "n" : "p";
				fileBase += key.lat < 0 ? "n" : "p";
				int absx = Math.abs(key.lon);
				int absz = Math.abs(key.lat);
				//pad x and y to 9 digits
				String xStr = padWithZeros(absx, 9);
				String yStr = padWithZeros(absz, 9);
				for (int k = 0; k < 4; k++) {
					fileBase += xStr.charAt(k);
					fileBase += yStr.charAt(k);
					fileBase += "/";
				}
			}
		}
		fileBase += Ptolemy3D.getConfiguration().area + "x" + key.lon + "y" + key.lat;

		return fileBase;
	}

	// converts integer to left-zero padded string, len  chars long.
	private static String padWithZeros(int i, int len) {
		String s = Integer.toString(i);
		if (s.length() > len) {
			return s.substring(0, len);
		}
		else if (s.length() < len) {	// pad on left with zeros
			return "000000000000000000000000000".substring(0, len - s.length()) + s;
		}
		else {
			return s;
		}
	}

	/** @return true if the map has elevation data */
	public boolean hasElevation() {
		return (tin != null) || (dem != null);
	}

	/** */
	public int getLon() { return key.lon; }
	/** */
	public int getLat() { return key.lat; }
	/** */
	public int getLevel() { return key.level; }
	/** */
	public int getMapSize() { return key.mapSize; }
	/** */
	public Texture getWavelet(int res) { return wavelets[res]; }
}