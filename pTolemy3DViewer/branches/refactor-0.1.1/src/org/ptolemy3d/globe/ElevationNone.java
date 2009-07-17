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

import org.ptolemy3d.globe.Tile.TileArea;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class ElevationNone {
	public final Tile tile;
	public final TileArea subTile;
	
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
		this.subTile = subTile;
		
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
		final int offset = (subTile.ulx - tile.getReferenceLeftLongitude()) % polygonSize;
		return -offset;
	}
	/** @return */
	private int getPolygonSizeLonOffsetEnd() {
		final int offset = (tile.getReferenceRightLongitude() - subTile.lrx) % polygonSize;
		return -offset;
	}

	/** @return */
	private int getPolygonSizeLatOffsetStart() {
		final int offset = (subTile.ulz - tile.getReferenceUpperLatitude()) % polygonSize;
		return -offset;
	}
	/** @return */
	private int getPolygonSizeLatOffsetEnd() {
		final int offset = (tile.getReferenceLowerLatitude() - subTile.lrz) % polygonSize;
		return -offset;
	}
	
	/** @return */
	private final int getNumPolygonLongitude() {
		final int dFirst = getPolygonSizeLonOffsetStart();
		final int dLast = getPolygonSizeLonOffsetEnd();
		
		int sizeLon = (subTile.lrx - dLast) - (subTile.ulx + dFirst);
		final int nLon = sizeLon / polygonSize;
		return nLon;
	}
	/** @return */
	private final int getNumPolygonLatitude() {
		final int dFirst = getPolygonSizeLatOffsetStart();
		final int dLast = getPolygonSizeLatOffsetEnd();
		
		int sizeLat = (subTile.lrz - dLast) - (subTile.ulz + dFirst);
		final int nLat = sizeLat / polygonSize;
		return nLat;
	}
	
	public double getHeight(Tile tile, int lon, int lat) {
		return 0;
	}
}
