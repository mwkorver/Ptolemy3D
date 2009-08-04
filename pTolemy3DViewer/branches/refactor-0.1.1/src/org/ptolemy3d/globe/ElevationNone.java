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

import org.ptolemy3d.globe.TileArea;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class ElevationNone {
	public final Tile tile;
	
	private final int numTilePolygons;
	private final int polygonSize;
	public final int polySizeLat, polySizeLon;
	public final int numPolyLon, numPolyLat;
	public final int polyLonStart, polyLatStart;
	public final int polyLonEnd, polyLatEnd;
	
	public final int polySizeLonOffsetStart, polySizeLatOffsetStart;
	public final int polySizeLonOffsetEnd, polySizeLatOffsetEnd;
	
	public ElevationNone(TileArea subTile) {
		this.tile = subTile.tile;
		
		numTilePolygons = getNumTilePolygons();
		polygonSize = getPolygonSize();
		
		polySizeLat = polygonSize;
		polySizeLon = polygonSize;
		
		polyLonStart = subTile.ulx;
		polyLatStart = subTile.ulz;
		
		polyLonEnd = subTile.lrx;
		polyLatEnd = subTile.lrz;
		
		polySizeLonOffsetStart = getPolygonSizeLonOffsetStart();
		polySizeLonOffsetEnd = getPolygonSizeLonOffsetEnd();
		
		polySizeLatOffsetStart = getPolygonSizeLatOffsetStart();
		polySizeLatOffsetEnd = getPolygonSizeLatOffsetEnd();

		numPolyLon = getNumPolygonLongitude();
		numPolyLat = getNumPolygonLatitude();
		
		if(true) {
			int endX = polyLonStart + (numPolyLon * polygonSize) + polySizeLonOffsetStart + polySizeLonOffsetEnd;
			int endZ = polyLatStart + (numPolyLat * polygonSize) + polySizeLatOffsetStart + polySizeLatOffsetEnd;
			
			if(endX != polyLonEnd) {
				System.out.println("Lon diff: "+endX+" != "+polyLonEnd+", "+(endX-polyLonEnd)+", "+polygonSize);
			}
			if(endZ != polyLatEnd) {
				System.out.println("Lat diff: "+endZ+" != "+polyLatEnd+", "+(endZ-polyLatEnd)+", "+polygonSize);
			}
		}
	}
	
	/* All function works in DD */
	
	/** @return the number of quad per tile depending on the tile's layer. */
	private final int getNumTilePolygons() {
		return (tile.getLayerID() == 0) ? 16 : 4;
	}
	
	/** @return */
	private final int getPolygonSize() {
		final int size = tile.getTileSize() / numTilePolygons;
		return size;
	}

	/** @return */
	private int getPolygonSizeLonOffsetStart() {
		final int offset = (polyLonStart - tile.getReferenceLeftLongitude()) % polygonSize;
		return -offset;
	}
	/** @return */
	private int getPolygonSizeLonOffsetEnd() {
		final int offset = (tile.getReferenceRightLongitude() - polyLonEnd) % polygonSize;
		return -offset;
	}

	/** @return */
	private int getPolygonSizeLatOffsetStart() {
		final int offset = (polyLatStart - tile.getReferenceUpperLatitude()) % polygonSize;
		return -offset;
	}
	/** @return */
	private int getPolygonSizeLatOffsetEnd() {
		final int offset = (tile.getReferenceLowerLatitude() - polyLatEnd) % polygonSize;
		return -offset;
	}
	
	/** @return */
	private final int getNumPolygonLongitude() {
		final int sizeLon = (polyLonEnd - polySizeLonOffsetEnd) - (polyLonStart + polySizeLonOffsetStart);
		final int nLon = sizeLon / polygonSize;
		return nLon;
	}
	/** @return */
	private final int getNumPolygonLatitude() {
		final int sizeLat = (polyLatEnd - polySizeLatOffsetEnd) - (polyLatStart + polySizeLatOffsetStart);
		final int nLat = sizeLat / polygonSize;
		return nLat;
	}
	
	public double indexOfLongitude(int lon) {
		if(lon < polyLonStart) {
			return -1;
		}
		
		final double index;
		if(numPolyLon == 1) {
			index = (double)(lon - polyLonStart) / (polySizeLon + polySizeLonOffsetStart + polySizeLonOffsetEnd);
		}
		else {
			final int polySizeLonStart = polySizeLon + polySizeLonOffsetStart;
			final int polySizeLonEnd = polySizeLon + polySizeLonOffsetEnd;
			
			if(lon <= polyLonStart + polySizeLonStart) {
				index = (double)(lon - polyLonStart) / polySizeLonStart;
			}
			else if(lon <= polyLonEnd - polySizeLonEnd) {
				index = 1 + ((double)(lon - polySizeLonStart - polyLonStart) / polySizeLon);
			}
			else {
				index = numPolyLon - ((double)(polyLonEnd - lon) / polySizeLonEnd);
			}
		}
		if(index > numPolyLon) {
			return -1;
		}
		return index;
	}
	public double indexOfLatitude(int lat) {
		if(lat < polyLatStart) {
			return -1;
		}
		
		final double index;
		if(numPolyLat == 1) {
			index = (double)(lat - polyLatStart) / (polySizeLat + polySizeLatOffsetStart + polySizeLatOffsetEnd);
		}
		else {
			final int polySizeLatStart = polySizeLat + polySizeLatOffsetStart;
			final int polySizeLatEnd = polySizeLat + polySizeLatOffsetEnd;
			
			if(lat <= polyLatStart + polySizeLatStart) {
				index = (double)(lat - polyLatStart) / polySizeLatStart;
			}
			else if(lat <= polyLatEnd - polySizeLatEnd) {
				index = 1 + ((double)(lat - polySizeLatStart - polyLatStart) / polySizeLat);
			}
			else {
				index = numPolyLat - ((double)(polyLatEnd - lat) / polySizeLatEnd);
			}
		}
		if(index > numPolyLat) {
			return -1;
		}
		return index;

	}
	
	public int getLonAtIndex(int lonID) {
		int lon = polyLonStart;
		for(int i = 1; i <= lonID; i++) {
			lon += polySizeLon;
			if(i == 1) {
				lon += polySizeLonOffsetStart;
			}
			if(i == numPolyLon) {
				lon += polySizeLonOffsetEnd;
			}
		}
		return lon;
	}
	
	public int getLatAtIndex(int latID) {
		int lat = polyLatStart;
		for(int i = 1; i <= latID; i++) {
			lat += polySizeLat;
			if(i == 1) {
				lat += polySizeLatOffsetStart;
			}
			if(i == numPolyLat) {
				lat += polySizeLatOffsetEnd;
			}
		}
		return lat;
	}
	
	public boolean isInRange(int lon, int lat) {
		return (lon >= polyLonStart) && (lon <= polyLonEnd) && (lat >= polyLatStart) && (lat <= polyLatEnd);
	}
}
