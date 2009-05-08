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
 *
 */
public class ElevationTin {
	/** Array of positions */
	public float[][] positions;
	/** Array of texture coordinates */
	public int[][] texCoords;
	/**  */
	public double x, y;
	/**  */
	public int w;

	public ElevationTin(byte[] datas) {
		final ByteReader reader = new ByteReader(datas);

		x = reader.readDouble();
		y = reader.readDouble();
		w = reader.readInt();

		int numVertices = reader.readInt();
		positions = new float[numVertices][3];
		for (int j = 0; j < numVertices; j++) {
			for (int k = 0; k < 3; k++) {
				positions[j][k] = reader.readFloat();
			}
		}

		numVertices = reader.readInt();
		texCoords = new int[numVertices][];
		for (int j = 0; j < numVertices; j++) {
			int nV = reader.readInt();
			texCoords[j] = new int[nV];
			for (int k = 0; k < nV; k++) {
				texCoords[j][k] = reader.readInt();
			}
		}
	}
}
