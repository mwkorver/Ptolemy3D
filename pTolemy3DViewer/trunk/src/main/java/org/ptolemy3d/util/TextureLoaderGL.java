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

package org.ptolemy3d.util;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.math.Math3D;

/**
 * Utilities to load texture on the GPU.
 */
public class TextureLoaderGL
{
	/**
	 * @param id
	 * @param color_type number of components (4: rgba, else: rgb)
	 * @param width
	 * @param height
	 * @param imgdat
	 * @param clamp true to clamp in s,t directions
	 */
	public final static void setGLTexture(GL gl, int[] id, int color_type, int width, int height, byte[] imgdat, boolean clamp)
	{
		if(imgdat == null) {
			id[0] = -1;
			return;
		}

		gl.glGenTextures(1, id, 0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, id[0]);

		if (clamp) {
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		}

		//gl.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);	//GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);

		int cty;
		switch(color_type) {
			case 4: cty = GL.GL_RGBA; break;
			default:
			case 3: cty = GL.GL_RGB; break;
			case 1: cty = GL.GL_LUMINANCE_ALPHA; break;
		}
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, cty, width, height, 0, cty, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(imgdat));
	}

	public final static void loadRaster(GL gl, float[] pms, int[] tex_id, byte[] data, byte[] img, short transparency, int[] tran_color)
	{
		if (tex_id[0] != -1) {
			gl.glDeleteTextures(1, tex_id, 0);
		}

		int[] meta = new int[10];
		byte[] idata = PngDecoder.decode(data, meta);

		int raster_width = Math3D.nextTwoPow(meta[0]);
		float raster_height = Math3D.nextTwoPow(meta[1]);

		if ((pms != null) && (pms.length >= 4)) {
			pms[0] = (float) meta[0] / raster_width;
			pms[1] = meta[1] / raster_height;
			pms[2] = meta[0];
			pms[3] = meta[1];
		}

		if (img == null) {
			img = new byte[(int) (raster_width * raster_height) * 4];
		}
		int raster_offset = 0;
		int offset = 0;

		int a, b;
		int cty = meta[3];
		for (a = 0; a < raster_height; a++)
		{
			for (b = 0; b < raster_width; b++)
			{
				offset = (a * raster_width * 4) + (b * 4);
				raster_offset = (a * meta[0] * cty) + (b * cty);
				if ((meta[0] > b) && (meta[1] > a))
				{
					img[offset] = idata[raster_offset];
					img[offset + 1] = idata[raster_offset + 1];
					img[offset + 2] = idata[raster_offset + 2];
					if ((tran_color != null) && ((img[offset] & 0xff) == tran_color[0]) && ((img[offset + 1] & 0xff) == tran_color[1]) && ((img[offset + 2] & 0xff) == tran_color[2]))
					{
						img[offset + 3] = (byte) 0;
					}
					else
					{
						img[offset + 3] = (byte) transparency;
					}
				}
				else
				{
					img[offset + 3] = (byte) 0;
				}
			}
		}

		TextureLoaderGL.setGLTexture(gl, tex_id, 4, raster_width, (int) raster_height, img, false);
	}

	public final static void setWebImage(GL gl, String image_url, int[] tex_id, float[] meta, int transparency, String svr)
	{
		if (svr == null)
		{
			svr = Ptolemy3D.ptolemy.configuration.server;
		}
		if ((meta == null) || (meta.length < 4))
		{
			meta = new float[4];
		}
		BufferedImage input = null;
		try
		{
			input = ImageIO.read(new URL("http://" + svr + image_url.substring(0, image_url.lastIndexOf("/") + 1) + image_url.substring(image_url.lastIndexOf("/") + 1, image_url.length()).replaceAll(" ", "%20")));
		}
		catch (Exception e)
		{
			IO.printStackRenderer(e);
		}

		if (input != null)
		{

			int src_height = input.getHeight();
			int src_width = input.getWidth();


			float raster_width = src_width;
			int ct = 1;
			while (raster_width > 2)
			{
				raster_width /= 2;
				ct++;
			}
			raster_width = (float) Math.pow(2, ct);
			meta[0] = src_width / raster_width;
			float raster_height = src_height;
			ct = 1;
			while (raster_height > 2)
			{
				raster_height /= 2;
				ct++;
			}
			raster_height = (float) Math.pow(2, ct);
			meta[1] = src_height / raster_height;

			byte[] img = new byte[(int) (raster_width * raster_height) * 4];
			int[] dat = input.getRGB(0, 0, src_width, src_height, null, 0, src_width);
			int raster_offset = 0;
			int offset = 0;

			int a, b;
			//int cty = input.getColorModel().getNumComponents();

			for (a = 0; a < raster_height; a++)
			{
				for (b = 0; b < raster_width; b++)
				{
					offset = (a * (int) raster_width * 4) + (b * 4);
					raster_offset = (a * src_width) + (b);
					if ((src_width > b) && (src_height > a))
					{
						img[offset] = (byte) (dat[raster_offset] >> 16);
						img[offset + 1] = (byte) (dat[raster_offset] >> 8);
						img[offset + 2] = (byte) (dat[raster_offset] >> 0);
						img[offset + 3] = (byte) transparency;
					}
				}
			}

			TextureLoaderGL.setGLTexture(gl, tex_id, 4, (int) raster_width, (int) raster_height, img, false);
		}
	}
}
