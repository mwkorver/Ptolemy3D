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

package org.ptolemy3d.data;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.MapDataKey;

/**
 * Texture manager<br>
 * <br>
 * Manage {@code MapData} image loading and automatic garbage for proper GPU memory use.<br>
 * Texture garbage is based and how much memory is used, with the garbage threashold
 * {@code TextureManager#TEXTURE_GARBAGE_TRESHOLD}, and last time the texture was used
 * {@code TextureManager#TEXTURE_GARBAGE_TIME}.<br>
 * When a texture has been garbaged, it be will automatically reload by the {@link MapDataManager}
 * at rendering request.<br>
 * <br>
 * All the other texture are not automatically garbaged, and must be deleted when they are<br>
 * no more needed (ie by the plugins, ...).<br>
 * <br>
 * @see MapData
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class TextureManager {
	static class HwTextureID {
		private final int id;
		int resolution;
		int lastUse;
		int memoryUsage;
		public HwTextureID(int id, int resolution) {
			this.id = id;
			this.resolution = resolution;
			this.memoryUsage = 0;
			getID();
		}
		final int getID() {
			lastUse = TextureManager.frameID;
			return id;
		}
	}
	private static int frameID = 0;
	/** Texture garbage threshold when memory usage is higher than (in mo) */
	public static int TEXTURE_GARBAGE_TRESHOLD = 20;
	/** Texture garbage time in number of frames */
	public static int TEXTURE_GARBAGE_TIME = 196;
	
	/** Keep track of ALL texture loaded */
	private final Map<Integer, Integer> textures;
	/** Keep track of ALL texture loaded for MapData */
	private final Map<MapDataKey, HwTextureID> mapDataTextures;
	/** HW memory usage of map data textures (in octets) */
	private int mapDataMemoryUsage, lastMapDataMemoryUsage;
	/** Anisotropic texture filter */
	private float maxTextureAnisotropy;

	public TextureManager() {
		textures = new HashMap<Integer, Integer>();
		mapDataTextures = new ConcurrentHashMap<MapDataKey, HwTextureID>();
		mapDataMemoryUsage = 0;
		lastMapDataMemoryUsage = 0;
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
	protected final int load(GL gl, MapData mapData, Texture texture, int resolution, boolean clamp) {
		HwTextureID texID = mapDataTextures.get(mapData.key);
		if (texID == null) {
			final int textureID = loadTexture(gl, texture, clamp);
			if(textureID == 0) {
				return 0;
			}
			texID = new HwTextureID(textureID, resolution);
			mapDataTextures.put(mapData.key, texID);
		}
		else {
			mapDataMemoryUsage -= texID.memoryUsage;
			texID.resolution = resolution;
			update(gl, texID.id, texture);
		}
		texID.memoryUsage = texture.width * texture.height * 3;
		mapDataMemoryUsage += texID.memoryUsage;
		return texID.getID();
	}
	private final int loadTexture(GL gl, Texture texture, boolean clamp) {
		if(texture.pixels == null) {
			return -1;
		}

		int[] buff = new int[1];
		gl.glGenTextures(1, buff, 0);
		final int texID = buff[0];
		
		//assert(gl.glIsTexture(texId[0]));
		gl.glBindTexture(GL.GL_TEXTURE_2D, texID);

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
		
		return texID;
	}
	private final void update(GL gl, int textureId, Texture texture) {
		assert(gl.glIsTexture(textureId));
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureId);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, texture.getFormat(), texture.width, texture.height, 0, texture.getFormat(), GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(texture.pixels));
	}

	/** */
	public final void unload(GL gl, MapDataKey mapData) {
		final HwTextureID hwTexID = mapDataTextures.remove(mapData);
		if(hwTexID != null) {
			mapDataMemoryUsage -= hwTexID.memoryUsage;
			unload(gl, hwTexID.id);
		}
	}
	
	public int getTextureID(MapData mapData) {
		HwTextureID texID = mapDataTextures.get(mapData.key);
		if (texID == null) {
			if(mapData.newTexture == null) {
				mapData.mapResolution = -1;
			}
			return 0;
		}
		else {
			return texID.getID();
		}
	}
	public int getTextureResolution(MapData mapData) {
		HwTextureID texID = mapDataTextures.get(mapData.key);
		if (texID == null) {
			return -1;
		}
		else {
			return texID.resolution;
		}
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
	
	/** Called each frames */
	public void freeUnusedTextures(GL gl) {
		frameID++;
		
		if(mapDataMemoryUsage > (TEXTURE_GARBAGE_TRESHOLD * 1024 * 1024)) {
			for(Entry<MapDataKey, HwTextureID> entry : mapDataTextures.entrySet()) {
				if(mapDataMemoryUsage > (TEXTURE_GARBAGE_TRESHOLD * 1024 * 1024)) {
					final MapDataKey mapDataKey = entry.getKey();
					final HwTextureID hwTexID = entry.getValue();
					if((frameID - hwTexID.lastUse) > TEXTURE_GARBAGE_TIME) {
						unload(gl, mapDataKey);
					}
				}
			}
		}
		
		if(lastMapDataMemoryUsage != mapDataMemoryUsage) {
			IO.printfRenderer("Texture usage (mo): %f\n", mapDataMemoryUsage / 1024.f / 1024.f);
		}
		lastMapDataMemoryUsage = mapDataMemoryUsage;
	}
}
