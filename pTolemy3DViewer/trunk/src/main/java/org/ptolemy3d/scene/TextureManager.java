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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.tile.Jp2Tile;

public class TextureManager
{
	/** Instance of Ptolemy3D */
	private final Ptolemy3D ptolemy;

	private final int[][] tileTextureIDs;
	private final int[] tileNumTextures;

	/** Keep track of ALL texture loaded */
	private final HashMap<Integer, Integer> textures;
	private final Vector<Integer> emptyRecycler;
	private final Vector<Integer>[] trash;
	
	public final boolean useSubtexturing;

	public TextureManager(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;

		this.useSubtexturing = ptolemy.configuration.useSubtexturing;

		tileTextureIDs = new int[Jp2Tile.NUM_RESOLUTION][1];
		tileNumTextures = new int[Jp2Tile.NUM_RESOLUTION];
		Arrays.fill(tileNumTextures, 0);
		
		textures = new HashMap<Integer, Integer>();
		emptyRecycler = new Vector<Integer>(20);
		trash = new Vector[Jp2Tile.NUM_RESOLUTION];
		for(int i = 0; i < trash.length; i++) {
			trash[i] = new Vector<Integer>(10);
		}
	}
	
	/** Texture loading
	 * @param resolution tile resolution, else -1 if it is not a tile
	 * @param colorType default 3
	 * @return the texture ID */
	public final int load(GL gl, byte[] imageData, int width, int height, int format, boolean clamp, int resolution)
	{
		if(resolution >= 0) {
			int textureId = get(resolution);
			if (textureId == -1) {
				return loadTexture(gl, imageData, width, height, format, clamp);
			}
			else {
				update(gl, textureId, imageData, width, height, format);
				return textureId;
			}
		}
		else {
			return loadTexture(gl, imageData, width, height, format, clamp);
		}
	}
	private final int loadTexture(GL gl, byte[] imageData, int width, int height, int format, boolean clamp)
	{
		if(imageData == null) {
			return -1;
		}

		int[] buff = new int[1];
		gl.glGenTextures(1, buff, 0);
		final int texId = buff[0];
		
		//assert(gl.glIsTexture(texId[0]));
		gl.glBindTexture(GL.GL_TEXTURE_2D, texId);

		if (clamp) {
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		}

		gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
		gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);

		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR/*GL_NEAREST*/);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);

		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, format, width, height, 0, format, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(imageData));
		
		//Register texture
		final Integer keyAndValue = texId;
		textures.put(keyAndValue, keyAndValue);
		
//		IO.printlnRenderer("Texture creation: "+texId);
		return texId;
	}
	public final void update(GL gl, int textureId, byte[] datas, int width, int height, int format) {
		assert(gl.glIsTexture(textureId));
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, width, height, format, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(datas));
	}
	
	/**
	 * Safely texture unload
	 * @param textureIDs array of texture ID
	 * @param numTextures number of texture to delete
	 */
	public final void unload(GL gl, int[] textureIDs, int numTextures)
	{
		for(int i = 0; i < numTextures; i++) {
			unload(gl, textureIDs[i]);
		}
	}
	/** Safely texture unload */
	public final void unload(GL gl, int textureID)
	{
		unloadImpl(gl, textureID);
		
		//Unregister texture
		final Integer keyAndValue = textureID;
		textures.remove(keyAndValue);
	}
	private final boolean unloadImpl(GL gl, int textureID)
	{
		if(gl.glIsTexture(textureID)) {
//			IO.printlnRenderer("Texture deletion: "+textureID);
			gl.glDeleteTextures(1, new int[]{textureID}, 0);
			return true;
		}
		else {
			return false;
		}
	}

	private final int get(int res)
	{
		final int ptr = tileNumTextures[res];
		if(ptr == 0) {
			return -1;
		}
		
		final int texID = tileTextureIDs[res][ptr - 1];
		tileNumTextures[res]--;
		return texID;
	}
	
	protected final void destroyTrashTextures(GL gl)
	{
		if(ptolemy.tileLoader == null) {
			IO.printError("Can't destroy textures.");
			return;
		}
		ptolemy.tileLoader.getTextureTrash(trash);
		if (trash == null) {
			return;
		}

		for (int i = 0; i < trash.length; i++) {
			final Vector<Integer> vec = trash[i];
			if (vec == null || vec.size() == 0) {
				continue;
			}
			final int num = vec.size();

			// add to recycle bin
			if (useSubtexturing) {
				if ((num + tileNumTextures[i]) > tileTextureIDs[i].length) {
					int[] newRecycler = new int[num + tileNumTextures[i]];
					System.arraycopy(tileTextureIDs[i], 0, newRecycler, 0, tileTextureIDs[i].length);
					tileTextureIDs[i] = newRecycler;
				}
				for(int j = 0; j < num; j++) {
					tileTextureIDs[i][tileNumTextures[i] + j] = vec.get(j);
				}
				tileNumTextures[i] += num;
			}
			else {
				for(int texId : vec) {
					unload(gl, texId);
				}
			}
		}
	}
	/**
	 * make sure to do this on system exit to clear out the remaining textures from memory.
	 */
	protected final void emptyRecyler(GL gl)
	{
		emptyRecycler.clear();
		for (int i = 0; i < tileNumTextures.length; i++) {
			if (tileNumTextures[i] <= 0) {
				continue;
			}
			for (int j = 0; j < tileNumTextures[i]; j++) {
				if (tileTextureIDs[i][j] < 0) {
					continue;
				}
				emptyRecycler.add(tileTextureIDs[i][j]);
			}
			tileNumTextures[i] = 0;
		}

		for(int textureId : emptyRecycler) {
			unload(gl, textureId);
		}
	}
	
	protected void destroyGL(GL gl) {
		//Destroy texture retained here
		destroyTrashTextures(gl);
		emptyRecyler(gl);
		
		//Search for leak
		Iterator<Integer> i = textures.keySet().iterator();
		while(i.hasNext()) {
			int texId = i.next();
			if(unloadImpl(gl, texId)) {
				IO.printlnRenderer("Texture leak (texID="+texId+")");
			}
		}
		
		ptolemy.textureManager = null;
	}
}
