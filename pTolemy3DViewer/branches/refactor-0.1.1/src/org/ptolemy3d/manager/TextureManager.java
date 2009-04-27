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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class TextureManager {
	/** Keep track of ALL texture loaded */
	private final HashMap<Integer, Integer> textures;
	/** Keep track of ALL texture loaded for MapData */
	private final HashMap<MapDataKey, Integer> mapDataTextures;
	/** Anisotropic texture filter */
	private float maxTextureAnisotropy;

	public TextureManager() {
		textures = new HashMap<Integer, Integer>();
		mapDataTextures = new HashMap<MapDataKey, Integer>();
		maxTextureAnisotropy = -1.0f;
	}
	
	private final float getMaxAnisotropy(GL gl) {
        //Check for the extension GL_EXT_texture_filter_anisotropic
		if (maxTextureAnisotropy == -1.0f) {
			final String extensions = gl.glGetString(GL.GL_EXTENSIONS);
			if(extensions.indexOf("GL_EXT_texture_filter_anisotropic") >= 0) {
				float[] buff = new float[1];
				gl.glGetFloatv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, buff, 0);
				maxTextureAnisotropy = buff[0];
			}
		}
		return maxTextureAnisotropy;
	}
	
	/** Texture loading
	 * @param resolution tile resolution, else -1 if it is not a tile
	 * @param colorType default 3
	 * @return the texture ID */
	public final int load(GL gl, Texture texture, boolean clamp) {
		final int textureID = loadTexture(gl, texture, clamp);
		//Register texture
		final Integer keyAndValue = textureID;
		textures.put(keyAndValue, keyAndValue);
		return textureID;
	}
	protected final int load(GL gl, MapData mapData, Texture texture, boolean clamp) {
		Integer texID = mapDataTextures.get(mapData.key);
		if (texID == null) {
			final int textureID = loadTexture(gl, texture, clamp);
			mapDataTextures.put(mapData.key, textureID);
			return textureID;
		}
		else {
			update(gl, texID, texture);
			return texID;
		}
	}
	private final int loadTexture(GL gl, Texture texture, boolean clamp) {
		if(texture.pixels == null) {
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

		// Good filter for better quality
		if (getMaxAnisotropy(gl) > 1.0f) {
			gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, getMaxAnisotropy(gl));
		}
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);

		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, texture.getFormat(), texture.width, texture.height, 0, texture.getFormat(), GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(texture.pixels));
//		IO.printlnRenderer("Texture creation: "+texId);
		
		return texId;
	}
	public final void update(GL gl, int textureId, Texture texture) {
		assert(gl.glIsTexture(textureId));
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, texture.getFormat(), texture.width, texture.height, 0, texture.getFormat(), GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(texture.pixels));
	}
	
	public int get(MapData mapData) {
//		System.out.println(mapData.key);
//		System.out.println(mapDataTextures);
//		return mapDataTextures.get(mapData.key);
		return 0;
	}
	
	/**
	 * Safely texture unload
	 * @param textureIDs array of texture ID
	 * @param numTextures number of texture to delete
	 */
	public final void unload(GL gl, int[] textureIDs, int numTextures) {
		for(int i = 0; i < numTextures; i++) {
			unload(gl, textureIDs[i]);
		}
	}
	/** Safely texture unload */
	public final void unload(GL gl, int textureID) {
		unloadImpl(gl, textureID);
		
		//Unregister texture
		final Integer keyAndValue = textureID;
		textures.remove(keyAndValue);
	}
	private final boolean unloadImpl(GL gl, int textureID) {
		if(gl.glIsTexture(textureID)) {
//			IO.printlnRenderer("Texture deletion: "+textureID);
			gl.glDeleteTextures(1, new int[]{textureID}, 0);
			return true;
		}
		else {
			return false;
		}
	}
	
	protected void destroyGL(GL gl) {
		//Search for leak
		Iterator<Integer> i = textures.keySet().iterator();
		while(i.hasNext()) {
			int texId = i.next();
			if(unloadImpl(gl, texId)) {
				IO.printlnRenderer("Texture leak (texID="+texId+")");
			}
		}
		
		Ptolemy3D.setTextureManager(null);
	}
	
	public void freeUnusedTextures(GL gl) {
		
	}
}
