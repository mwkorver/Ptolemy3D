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

import java.security.InvalidParameterException;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.scene.Landscape;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class ElevationDem {
	/** Elevation grid */
	private final short[][] elevation;
	/** Reference longitude or the upper left corner */
	private final int refLongitude;
	/** Reference latitude or the upper left corner */
	private final int refLatitude;
	/** */
	private final double geomIncr;	//Only used to avoid / computed each time, remove that ?

	public ElevationDem(MapDataKey mapKey, byte[] datas) {
		final int numRows = (int)Math.sqrt((datas.length / 2));
		if((numRows * numRows * 2) != datas.length) {
			throw new InvalidParameterException("Corrupted DEM data");
		}
		
		final int tileSize = Ptolemy3D.getScene().getLandscape().globe.getLayer(mapKey.layer).getTileSize();
		geomIncr = (double) (numRows - 1) / tileSize;
		
		// Convert to rendering unit
		refLongitude = mapKey.lon + Landscape.MAX_LONGITUDE;
		refLatitude = -mapKey.lat;
		
		// Decode DEM data
		this.elevation = new short[numRows][numRows];
		for(int lat = 0; lat < numRows; lat++) {
			for(int lon = 0; lon < numRows; lon++) {
				final int index = ((lat * numRows) + lon) * 2;
				final int height = ((datas[index] & 0xFF) << 8) | (datas[index + 1] & 0xFF);
				elevation[lon][lat] = (short)(height&0xFFFF);
			}
		}
	}
	
	public int getNumRows() {
		return elevation.length;
	}
	
	/**
	 * @param lon longitude in DD
	 * @param lat latitude in DD
	 * @return the elevation (can be cast to int)
	 */
	public ElevationValue getElevation(int lon, int lat) {
		final double lonID = (lon - refLongitude) * geomIncr;
		final double latID = (lat - refLatitude) * geomIncr;
		
		final int lonInfID = (int)lonID;
		final int latInfID = (int)latID;
		
		final boolean interpolateLon = (lonInfID != lonID);
		final boolean interpolateLat = (latInfID != latID);
		
		final double height;
		if(interpolateLon && interpolateLat) {
			final double interpLon = lonID - lonInfID;
			final double interpLat = latID - latInfID;
			
			final double h00 = getHeightFromIndex(lonInfID,   latInfID  ) * (1 - interpLon);
			final double h10 = getHeightFromIndex(lonInfID+1, latInfID  ) * interpLon;
			final double h01 = getHeightFromIndex(lonInfID,   latInfID+1) * (1 - interpLon);
			final double h11 = getHeightFromIndex(lonInfID+1, latInfID+1) * interpLon;
			
			height = (h00 + h10) * (1 - interpLat) + (h01 + h11) * interpLat;
		}
		else if(interpolateLon) {
			final double interpLon = lonID - lonInfID;
			
			final double h00 = getHeightFromIndex(lonInfID,   latInfID) * (1 - interpLon);
			final double h10 = getHeightFromIndex(lonInfID+1, latInfID) * interpLon;
			
			height = h00 + h10;
		}
		else if(interpolateLat) {
			final double interpLat = latID - latInfID;
			
			final double h00 = getHeightFromIndex(lonInfID,   latInfID  );
			final double h01 = getHeightFromIndex(lonInfID,   latInfID+1);
			
			height = h00 * (1 - interpLat) + h01 * interpLat;
		}
		else {
			height = getHeightFromIndex(lonInfID, latInfID);
		}
		return new ElevationValue(height, ElevationValue.TYPE_DEM, interpolateLon || interpolateLat);
	}
	
	/** @return true if (lon, lat) is a point inside the DEM area range */
	public boolean isInRange(int lon, int lat) {
		if((lon < refLongitude) || (lat < refLatitude)) {
			return false;
		}
		
		final double lonID = (lon - refLongitude) * geomIncr;
		final double latID = (lat - refLatitude) * geomIncr;
		final int numRows = getNumRows();
		return (lonID < numRows) && (latID < numRows);
	}
	
	/** @return ul corner, can be cast to int */
	public final int getUpLeftCorner() {
		return getHeightFromIndex(0, 0);
	}
	/** @return ur corner, can be cast to int */
	public final int getUpRightCorner() {
		return getHeightFromIndex(getNumRows() - 1, 0);
	}
	/** @return ll corner, can be cast to int */
	public final int getLowerLeftCorner() {
		return getHeightFromIndex(0, getNumRows() - 1);
	}
	/** @return ul corner, can be cast to int */
	public final int getLowerRightCorner() {
		return getHeightFromIndex(getNumRows() - 1, getNumRows() - 1);
	}

	/**
	 * @param lonID index of the point in the longitude direction of the grid. Should be in range [0;getNumRows()-1]
	 * @param latID index of the point in the longitude direction of the grid. Should be in range [0;getNumRows()-1]
	 * @return the elevation
	 * @see #getNumRows()
	 */
	public final int getHeightFromIndex(int lonID, int latID) {
		return elevation[lonID][latID]&0xFFFF;
	}
}
