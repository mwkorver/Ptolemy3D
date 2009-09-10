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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Texture {
	/** Width of the wavelet */
	public final int width;
	/** Height of the wavelet */
	public final int height;
	/** Map data (pixels) */
	public byte[] pixels;
	
	public Texture(byte[] pixels, int width, int height) {
		this.pixels = pixels;
		this.width = width;
		this.height = height;
	}
	
	/** @return the number of color channels */
	public int getNumChannels() {
		return pixels.length / (width * height);
	}
	
	/** @return OpenGL format */
	public int getFormat() {
		final int numChannels = getNumChannels();
		switch (numChannels) {
			case 1: return GL.GL_LUMINANCE;
			case 2: return GL.GL_LUMINANCE_ALPHA;
			case 3: return GL.GL_RGB;
			case 4: return GL.GL_RGBA;
		}
		throw new IllegalArgumentException("Illegal numChannels: "+numChannels);
	}
	
	/** Free texture data (pixels array) - <B>Do not free the OpenGL texture.</B> */
	public void freeData() {
		pixels = null;
	}
	
	public final static Texture load(InputStream is) {
		final BufferedImage buffer;
		try {
			buffer = ImageIO.read(is);
		}
		catch(IOException e) {
			return null;
		}
		
		boolean hasAlpha = buffer.getColorModel().hasAlpha();
		
		int width = buffer.getWidth();
		int height = buffer.getHeight();
		int numChannels = hasAlpha ? 4 : 3;
		byte[] pixels = new byte[width*height*numChannels];

		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int i = (height-1-y)*width+x;
				int color = buffer.getRGB(x, y);
				pixels[numChannels*i  ] = (byte) ((color >> 16) & 0xFF);
				pixels[numChannels*i+1] = (byte) ((color >>  8) & 0xFF);
				pixels[numChannels*i+2] = (byte) ((color      ) & 0xFF);
				if(hasAlpha) {
					pixels[numChannels*i+3] = (byte) ((color >> 24) & 0xFF);
				}
			}
		}
		
		return new Texture(pixels, width, height);
	}
}
