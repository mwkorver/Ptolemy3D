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

import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapData.MapWavelet;

@Deprecated
public class QuadBufferPool
{
	private static final int DATA_STORAGE_MULT = 3;

	protected int full; // a basic full state, goes for any resolution
	private byte[][][] pool;
	private boolean filled[][];

	public final void init(int[] quadtiles, int[] tileWidth)
	{
		full = -1;
		// init pool to hold all of our data.
		pool = new byte[MapData.MAX_NUM_RESOLUTION][][];
		filled = new boolean[MapData.MAX_NUM_RESOLUTION][];

		for (int i = 0; i < MapData.MAX_NUM_RESOLUTION; i++)
		{
			pool[i] = new byte[quadtiles[i] * DATA_STORAGE_MULT][tileWidth[i] * tileWidth[i] * 3];
			filled[i] = new boolean[quadtiles[i] * DATA_STORAGE_MULT];
		}
	}

	public byte[] setArray(int res, MapWavelet[] levels)
	{
		// look for an open array
		for (int i = 0; i < filled[res].length; i++) {
			if (!filled[res][i]) {
				levels[res].dataKey = i;
				filled[res][i] = true;
				return pool[res][i];
			}
		}
		full = res;
		return null;
	}

	public byte[] getArray(int res, int key)
	{
		return pool[res][key];
	}

	public final void free(int res, int key)
	{
		if (res == full) {
			full = -1;
		}
		filled[res][key] = false;
	}

	public void release()
	{
		if(pool != null) {
			for(int i = 0; i < pool.length; i++) {
				for(int j = 0; j < pool[i].length; j++) {
					pool[i][j] = null;
				}
				pool[i] = null;
			}
		}
		pool = null;
		if(filled != null) {
			for(int i = 0; i < filled.length; i++) {
				filled[i] = null;
			}
		}
		filled = null;
	}
}