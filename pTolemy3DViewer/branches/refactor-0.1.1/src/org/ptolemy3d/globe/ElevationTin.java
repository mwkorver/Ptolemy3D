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
public class ElevationTin
{
	/** Array of positions */
	public float[][] p;
	/* Array of texture coordinates */
	public int[][] nSt;
	/**  */
	public double x, y;
	/**  */
	public int w;

	public ElevationTin(byte[] datas)
	{
		int[] cursor = { 0 };

		x = ByteReader.readDouble(datas, cursor);
		y = ByteReader.readDouble(datas, cursor);
		w = ByteReader.readInt(datas, cursor);

		int nP = ByteReader.readInt(datas, cursor);
		p = new float[nP][3];
		for (int j = 0; j < nP; j++) {
			for (int k = 0; k < 3; k++) {
				p[j][k] = ByteReader.readFloat(datas, cursor);
			}
		}

		nP = ByteReader.readInt(datas, cursor);
		nSt = new int[nP][];
		for (int j = 0; j < nP; j++) {
			int nV = ByteReader.readInt(datas, cursor);
			nSt[j] = new int[nV];
			for (int k = 0; k < nV; k++) {
				nSt[j][k] = ByteReader.readInt(datas, cursor);
			}
		}
	}
}
