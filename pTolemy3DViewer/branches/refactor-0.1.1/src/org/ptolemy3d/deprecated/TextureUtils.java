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

package org.ptolemy3d.deprecated;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.manager.Texture;

@Deprecated
class TextureUtils {
	private TextureUtils() {}

	@Deprecated
	public final static int colorTypeToFormat(int colorType) {
		int format;
		switch(colorType) {
			case 4: format = GL.GL_RGBA; break;
			default:
			case 3: format = GL.GL_RGB; break;
			case 1: format = GL.GL_LUMINANCE_ALPHA; break;
		}
		return format;
	}
	
	@Deprecated
	public final static Texture setWebImage(String url, float[] meta, int transparency, String svr) {
		if (svr == null) {
			svr = Ptolemy3D.getConfiguration().server;
		}
		if ((meta == null) || (meta.length < 4)) {
			meta = new float[4];
		}
		
		BufferedImage input = null;
		try {
			input = ImageIO.read(new URL("http://" + svr + url.substring(0, url.lastIndexOf("/") + 1) + url.substring(url.lastIndexOf("/") + 1, url.length()).replaceAll(" ", "%20")));
		} catch (Exception e) {
			IO.printStackRenderer(e);
			input = null;
		}
		if (input == null) {
			return null;
		}
		
		int src_height = input.getHeight();
		int src_width = input.getWidth();

		float rasterWidth = src_width;
		int ct = 1;
		while (rasterWidth > 2) {
			rasterWidth /= 2;
			ct++;
		}
		rasterWidth = (float) Math.pow(2, ct);
		meta[0] = src_width / rasterWidth;
		float rasterHeight = src_height;
		ct = 1;
		while (rasterHeight > 2) {
			rasterHeight /= 2;
			ct++;
		}
		rasterHeight = (float) Math.pow(2, ct);
		meta[1] = src_height / rasterHeight;

		final int numChannels = 4;
		byte[] img = new byte[(int) (rasterWidth * rasterHeight) * numChannels];
		int[] dat = input.getRGB(0, 0, src_width, src_height, null, 0, src_width);
		int raster_offset = 0;
		int offset = 0;

		int a, b;
		for(a = 0; a < rasterHeight; a++) {
			for(b = 0; b < rasterWidth; b++) {
				offset = (a * (int)rasterWidth * 4) + (b * 4);
				raster_offset = (a * src_width) + (b);
				if((src_width > b) && (src_height > a)) {
					img[offset] = (byte) (dat[raster_offset] >> 16);
					img[offset + 1] = (byte) (dat[raster_offset] >> 8);
					img[offset + 2] = (byte) (dat[raster_offset] >> 0);
					img[offset + 3] = (byte) transparency;
				}
			}
		}
		return new Texture(img, (int)rasterWidth, (int)rasterHeight);
	}
}
