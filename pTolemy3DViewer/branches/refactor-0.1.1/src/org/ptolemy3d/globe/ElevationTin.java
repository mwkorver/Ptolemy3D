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

import org.ptolemy3d.util.ByteReader;

/**
 * @author Contributors
 */
public class ElevationTin {
	/** Array of positions */
	public float[/*numVertices*/][/*3*/] positions;
	/** Array of texture coordinates */
	public int[][] indices;
	/**  */
	public double x, y;
	/**  */
	public int w;

	public ElevationTin(byte[] datas) {
		final ByteReader reader = new ByteReader(datas);

		x = reader.readDouble();
		y = reader.readDouble();
		w = reader.readInt();

		final int numPositions = reader.readInt();
		positions = new float[numPositions][3];
		for (int j = 0; j < numPositions; j++) {
			for (int k = 0; k < 3; k++) {
				positions[j][k] = reader.readFloat();
			}
		}

		final int numIndices = reader.readInt();
		indices = new int[numIndices][];
		for (int j = 0; j < numIndices; j++) {
			int nV = reader.readInt();
			indices[j] = new int[nV];
			for (int k = 0; k < nV; k++) {
				indices[j][k] = reader.readInt();
			}
		}
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
	
	/*
	 * FIXME Code not tested from here
	 */
	
	public double getHeight(Tile tile, int lon, int lat) {
		final int left = tile.getRenderingLeftLongitude();
		final int upper = tile.getRenderingUpperLatitude();
		final int right = tile.getRenderingRightLongitude();
		final int lower = tile.getRenderingLowerLatitude();
		
		final int x = w * (lon - left) / (right - left);
		final int z = w * (lat - upper) / (lower - upper);
		return getHeightFromIndex(x, z);
	}
	
	private final double getHeightFromIndex(int lonValue, int latValue) {
		int index = find(lonValue, latValue);
		if(index < 0) {
			index = 0;
		}
		final float height = positions[index][1];
		return height;
	}
	
	/*
	 * @param lonValue between 0 to w
	 * @param latValue between 0 to w
	 */
	private final int find(int lonValue, int latValue) {
		for (int i = 0; i < positions.length; i++) {
			float[] pos = positions[i];
			if(pos[0] == lonValue && pos[2] == latValue) {
				return i;
			}
		}
		return -1;
	}
}
