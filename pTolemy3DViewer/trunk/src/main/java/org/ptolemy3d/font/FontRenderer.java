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

import java.awt.Font;
import java.util.Vector;

import org.ptolemy3d.Ptolemy3D;

import com.sun.opengl.util.j2d.TextRenderer;

/**
 * AWT Font renderer.
 */
public class FontRenderer
{
	private final Ptolemy3D ptolemy;
	private final Font font;
	private TextRenderer renderer = null;

	public FontRenderer(Ptolemy3D ptolemy, Font font)
	{
		this.ptolemy = ptolemy;
		this.font = font;
	}

	public void initGL()
	{
		renderer = new TextRenderer(font);
	}

	public void destroyGL()
	{
		renderer.dispose();
		renderer = null;
	}

	public void start()
	{
		if (renderer == null) {
			initGL();
		}
		renderer.beginRendering(ptolemy.events.drawWidth, ptolemy.events.drawHeight);
	}
	public void draw(Vector<String> strings, int x, int y, int lineIncr)
	{
		int offset = 0;
		for (String s : strings) {
			renderer.draw(s, x, y + offset);
			offset += lineIncr;
		}
	}
	/**
	 * @param s string to display
	 * @param x position on the x axis
	 * @param y position on the y axis
	 */
	public void draw(String s, int x, int y)
	{
		renderer.draw(s, x, y);
	}
	public void end()
	{
		renderer.endRendering();
	}
}
