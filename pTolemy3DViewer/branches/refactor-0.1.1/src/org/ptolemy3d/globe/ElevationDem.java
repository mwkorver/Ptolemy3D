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

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
public class ElevationDem {
	/* 2: data are stored as unsigned short */
	private final static int DATA_SIZE = 2;
	
	/** DEM Elevation raw datas */
	public final byte[] demDatas;
	/** Number of elevation point (square) */
	public final int size;

	public ElevationDem(byte[] datas) {
		demDatas = datas;
		size = (int)Math.sqrt((datas.length / 2));
	}
	
	public double getUpLeftHeight(Tile tile) {
		final int left = tile.getRenderingLeftLongitude();
		final int up = tile.getRenderingUpperLatitude();
		return getHeight(tile, left, up);
	}
	public double getBotLeftHeight(Tile tile) {
		final int left = tile.getRenderingLeftLongitude();
		final int lower = tile.getRenderingLowerLatitude();
		return getHeight(tile, left, lower);
	}
	public double getUpRightHeight(Tile tile) {
		final int right = tile.getRenderingRightLongitude();
		final int up = tile.getRenderingUpperLatitude();
		return getHeight(tile, right, up);
	}
	public double getBotRightHeight(Tile tile) {
		final int right = tile.getRenderingRightLongitude();
		final int lower = tile.getRenderingLowerLatitude();
		return getHeight(tile, right, lower);
	}
	
	public double getHeight(Tile tile, int lon, int lat) {
		final int upLeftX = tile.getRenderingLeftLongitude();
		final int upLeftZ = tile.getRenderingUpperLatitude();
		
		final Layer layer = Ptolemy3D.getScene().getLandscape().globe.getLayer(tile.mapData.key.layer);
		final double geomIncr = (double) (size - 1) / layer.getTileSize();
		final int x = (int) ((lon - upLeftX) * geomIncr);
		final int z = (int) ((lat - upLeftZ) * geomIncr);
		return getHeightFromIndex(x, z);
	}
	
	private final double getHeightFromIndex(int lon, int lat) {
		final int index = (lat * size * DATA_SIZE) + (lon * DATA_SIZE);
		final int height = (demDatas[index] << 8) + (demDatas[index + 1] & 0xFF);
		return height;
	}
}
