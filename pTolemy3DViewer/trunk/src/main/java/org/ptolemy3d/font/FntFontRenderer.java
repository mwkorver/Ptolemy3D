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

package org.ptolemy3d.font;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;

/**
 * FNT Font Renderer<BR>
 * Text and Numeric utilities.
 */
public class FntFontRenderer
{
	private final static double numberStringW = 0;

	/** Ptolemy3D Instance */
	private final Ptolemy3D ptolemy;
	/** Font */
	private final FntFont font;
	/** Font */
	private int[] texID;

	public FntFontRenderer(Ptolemy3D ptolemy, FntFont font)
	{
		this.ptolemy = ptolemy;
		this.font = font;
		this.texID = null;
	}

	public void initGL(GL gl)
	{
		if(texID != null) {
			destroyGL(gl);
		}
		texID = new int[1];

		gl.glGenTextures(1, texID, 0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, texID[0]);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_LUMINANCE_ALPHA, font.width, font.height, 0, GL.GL_LUMINANCE_ALPHA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(font.fontDatas));
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP);
	}

	public void destroyGL(GL gl)
	{
		if(texID != null) {
			gl.glDeleteTextures(texID.length, texID, 0);
			texID = null;
		}
	}

	public void bindTexture(GL gl)
	{
		if (texID == null) {
			initGL(gl);
		}
		if (texID != null) {
			gl.glBindTexture(GL.GL_TEXTURE_2D, texID[0]);
		}
	}

	/**
	 * Draw string, from Left to Right, at the coordinate (x, y)
	 * @return string width
	 */
	public float drawLeftToRight(GL gl, String s, double x, double y)
	{
		return drawLeftToRight(gl, s, x, y, 1.25f, 1.45f);
	}
	public float drawLeftToRight(GL gl, String s, double x, double y, float h, float w)
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		float scalerX = (float) screenSize.width / (float)ptolemy.events.drawWidth;
		float scalerY = (float) screenSize.height / (float)ptolemy.events.drawHeight;
		
		h *= scalerY;
		float wf_tot = 0.0f,wf=0.0f;
		int length = s.length();
		
		for (int i = 0; i < length; i++)
		{
			int n_ascii = s.charAt(i);
			FntFontChar fontChar = font.chars[n_ascii];
			
			wf = w * fontChar.widthFactor * scalerX;
			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			
			gl.glTexCoord2f(fontChar.left, fontChar.top);
			gl.glVertex3d(x + wf_tot, y + h, 0);

			gl.glTexCoord2f(fontChar.left, fontChar.bottom);
			gl.glVertex3d(x + wf_tot, y, 0);

			gl.glTexCoord2f(fontChar.right, fontChar.top);
			gl.glVertex3d(wf + x + wf_tot, y + h,0);

			gl.glTexCoord2f(fontChar.right, fontChar.bottom);
			gl.glVertex3d(wf + x + wf_tot, y, 0);
			gl.glEnd();
			
			wf_tot += wf;
		}
		return wf_tot;
	}

	/**
	 * Draw letters, from Right to Left, at the coordinate (x, y)
	 * @return string width
	 */
	public float drawRightToLeft(GL gl, String s, double x, double y)
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		float scalerX = (float) screenSize.width / (float)ptolemy.events.drawWidth;
		float scalerY = (float) screenSize.height / (float)ptolemy.events.drawHeight;

		float h = 1.25f * scalerY;
		float wf_tot = 0.0f, wf = 0.0f;
		int length = s.length();

		for (int i = length - 1; i >= 0; i--)
		{
			int n_ascii = s.charAt(i);
			FntFontChar fontChar = font.chars[n_ascii];

			wf = 1.45f * fontChar.widthFactor * scalerX;
			wf_tot -= wf;

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			gl.glTexCoord2f(fontChar.left, fontChar.top);
			gl.glVertex3d(x + wf_tot, y + h, 0);

			gl.glTexCoord2f(fontChar.left, fontChar.bottom);
			gl.glVertex3d(x + wf_tot, y, 0);

			gl.glTexCoord2f(fontChar.right, fontChar.top);
			gl.glVertex3d(wf + x + wf_tot, y + h, 0);

			gl.glTexCoord2f(fontChar.right, fontChar.bottom);
			gl.glVertex3d(wf + x + wf_tot, y, 0);
			gl.glEnd();

		}
		return wf_tot;
	}

	/** Draw integer value (right to left) */
	public float drawRightToLeft(GL gl, int integerNumber, double x, double y)
	{
		String val_str = Integer.toString(integerNumber);

		return drawRightToLeft(gl, val_str, x, y); //returns ending position of string
	}
	/** Draw XY Coordinates */
	public float drawLeftToRight(GL gl, int integerNumber, int decplace, double x, double y)
	{
		String val_str = Integer.toString(integerNumber);
		int str_length = val_str.length();
		if (str_length - decplace > 0) {
			val_str = val_str.substring(0, str_length - decplace) + "." + val_str.substring(str_length - decplace, str_length);
		}
		return drawLeftToRight(gl, val_str, x, y); //returns ending position of string
	}

	/** Draw numeric value <code>val</code> at the coordinate (x, y, z) and the size (w, h). */
	public float drawLeftToRight(GL gl, double doubleValue, int decplace, double x, double y, double z, double h, double w)
	{
		String val_str = String.valueOf(doubleValue);
		int dec_pt = val_str.indexOf(".");
		if (dec_pt + decplace < val_str.length()) {
			val_str = val_str.substring(0, val_str.indexOf(".") + decplace + 1);
		}
		return drawLeftToRight(gl, val_str, x, y);
	}

	/** Draw the number <code>n</code> at the coordinate (x, y, z) and the size (w, h).<BR>
	 * FIXME This code seem to be very strange ... may be does not work ? */
	public void drawNumber(GL gl, int n, double x, double y, double z, double h, double w)
	{
		double rat = (numberStringW / 256.0) / 10.5;	//Always 0  (anyway 256 and 10.5 constant should not be hard coded here ...)
		float startx = (float) ( n      * rat);			//Always 0 !
		float endx   = (float) ((n + 1) * rat);			//Always 0 !!

		gl.glBegin(GL.GL_TRIANGLE_STRIP);

		gl.glTexCoord2f(startx, 1);
		gl.glVertex3d(x, y, z);

		gl.glTexCoord2f(endx, 1);
		gl.glVertex3d(x + w, y, z);

		gl.glTexCoord2f(startx, 0);
		gl.glVertex3d(x, y + h, z);

		gl.glTexCoord2f(endx, 0);
		gl.glVertex3d(x + w, y + h, z);

		gl.glEnd();
	}
}
