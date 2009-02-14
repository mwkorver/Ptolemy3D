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
package org.ptolemy3d.plugin.util;

import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;

public class VectorClass
{
	public float lineW = 1;
	public float[] color;
	public String regex;
	public Vector<Integer> shapes;

	public VectorClass(String lw, String c, String r)
	{
		shapes = new Vector<Integer>(10);
		color = new float[4];
		Arrays.fill(color, 0);

		try {
			lineW = Float.parseFloat(lw);
			
			StringTokenizer st = new StringTokenizer(c, ":");
			color[0] = (float)Integer.parseInt(st.nextToken()) / 255;
			color[1] = (float)Integer.parseInt(st.nextToken()) / 255;
			color[2] = (float)Integer.parseInt(st.nextToken()) / 255;
			if(st.hasMoreTokens()) {
				color[3] = (float)Integer.parseInt(st.nextToken()) / 255;
			}
			else {
				color[3] = 1;
			}
		} catch (Exception e){}
		
		this.regex = r;
	}
}

