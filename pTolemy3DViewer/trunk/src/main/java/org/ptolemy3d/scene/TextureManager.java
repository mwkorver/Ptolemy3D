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

package org.ptolemy3d.scene;

import java.util.Arrays;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.tile.Jp2Tile;
import org.ptolemy3d.util.IntBuffer;
import org.ptolemy3d.util.TextureLoaderGL;

class TextureManager
{
	/** Instance of Ptolemy3D */
	private final Ptolemy3D ptolemy;

	private int[][] recycler;
	private int[] recyclerPtr;

	public final boolean useSubtexturing;

	public TextureManager(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;

		this.useSubtexturing = ptolemy.configuration.useSubtexturing;

		recycler = new int[Jp2Tile.NUM_RESOLUTION][1];
		recyclerPtr = new int[Jp2Tile.NUM_RESOLUTION];
		Arrays.fill(recyclerPtr, 0);
	}

	protected final boolean hasTexture(int res)
	{
		return recyclerPtr[res] != 0;
	}
	protected final int get(int res)
	{
		int i = recycler[res][recyclerPtr[res] - 1];
		recyclerPtr[res]--;
		return i;
	}

	protected final void destroyTrashTextures(GL gl)
	{
		int[][] trashBin = ptolemy.tileLoader.getTextureTrash();
		if (trashBin == null) {
			return;
		}

		for (int i = 0; i < Jp2Tile.NUM_RESOLUTION; i++) {
			if (trashBin[i] == null || trashBin[i].length == 0) {
				continue;
			}

			// add to recycle bin
			if (useSubtexturing) {
				if ((trashBin[i].length + recyclerPtr[i]) > recycler[i].length) {
					int[] newbin = new int[(trashBin[i].length + recyclerPtr[i])];
					System.arraycopy(recycler[i], 0, newbin, 0, recycler[i].length);
					recycler[i] = null; //clear current array from memory.
					recycler[i] = newbin;
				}
				System.arraycopy(trashBin[i], 0, recycler[i], recyclerPtr[i], trashBin[i].length);
				recyclerPtr[i] += trashBin[i].length;
			}
			else {
				IO.printlnRenderer("Deleting texture: " + i);
				TextureLoaderGL.deleteTextures(gl, trashBin[i], trashBin[i].length);
			}
		}
	}
	/**
	 * make sure to do this on system exit to clear out the remaining textures from memory.
	 */
	protected final void emptyRecyler(GL gl)
	{
		IntBuffer ib = new IntBuffer(20, 10);

		for (int i = 0; i < Jp2Tile.NUM_RESOLUTION; i++) {
			if (recyclerPtr[i] > 0) {
				for (int y = 0; y < recyclerPtr[i]; y++) {
					if (recycler[i][y] >= 0) {
						ib.append(recycler[i][y]);
					}
				}
				recyclerPtr[i] = 0;
			}
		}

		int[] dbuff = ib.getBuffer();
		if(dbuff != null && dbuff.length > 0) {
			TextureLoaderGL.deleteTextures(gl, dbuff, dbuff.length);
		}
	}
}
