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

import java.io.DataInputStream;
import java.io.IOException;

/**
 * FNT font reader.
 */
public class FntFont
{
	static class FntFontChar {
	    public float left;
	    public float top;
	    public float right;
	    public float bottom;
	    public boolean enabled;
	    public float widthFactor;
	}
	
	/** Font character descriptor */
    protected FntFontChar[] chars = new FntFontChar[256];
	/** Width of the font texture */
    protected int width;
	/** Height of the font texture */
    protected int height;
	/** Font texture datas */
    protected byte[] fontDatas;

	public FntFont(String file) throws IOException
	{
		if (file == null) {
			file = "/org/ptolemy3d/font/verdana36both.fnt";
		}

		int t;
		DataInputStream dis = new DataInputStream(getClass().getResourceAsStream(file));

		int id = readIntLittleEndian(dis);	//Not used
		width = readIntLittleEndian(dis);
		height = readIntLittleEndian(dis);
		t = readIntLittleEndian(dis);

		int defaultHeight = t;	//Not used

		for (int i = 0; i < 256; i++)
		{
			chars[i] = new FntFontChar();
			t = readIntLittleEndian(dis);
			chars[i].top = (float) t / (float) height;

			t = readIntLittleEndian(dis);
			chars[i].left = (float) t / (float) width;

			t = readIntLittleEndian(dis);
			chars[i].bottom = (float) t / (float) height;

			t = readIntLittleEndian(dis);
			chars[i].right = (float) t / (float) width;

			t = readIntLittleEndian(dis);
			float enabled = (float) t / (float) height;
			chars[i].enabled = enabled != 0;

			chars[i].widthFactor = readFloatLittleEndian(dis);

//			IO.println(i +  " top: " + chars[i].top + "bottom: " + chars[i].bottom + "left: " + chars[i].left + "right: " + chars[i].right);
//			IO.println("i: " + i + "wf: " + chars[i].widthFactor);
		}

		byte[] tmpFontData = new byte[width * height];
		for (int i = 0; i < width * height; i++) {
			tmpFontData[i] = dis.readByte();
		}
		//read(bytes, offset, bytes.length-offset)

		fontDatas = new byte[tmpFontData.length * 2];
		for (int i = 0; i < tmpFontData.length; i++) {
			fontDatas[i * 2] = tmpFontData[i];
			fontDatas[i * 2 + 1] = tmpFontData[i];
		}

		dis.close();
	}

	private final int readIntLittleEndian(DataInputStream dis) throws IOException
	{
		return ((dis.readByte() & 0xff)      ) |
			   ((dis.readByte() & 0xff) <<  8) |
			   ((dis.readByte() & 0xff) << 16) |
			   ((dis.readByte() & 0xff) << 24);
	}

	private final float readFloatLittleEndian(DataInputStream dis) throws IOException
	{
		int accum = ((dis.readByte() & 0xff)      ) |
					((dis.readByte() & 0xff) <<  8) |
					((dis.readByte() & 0xff) << 16) |
					((dis.readByte() & 0xff) << 24);
		return Float.intBitsToFloat(accum);
	}
}