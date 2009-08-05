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
package org.ptolemy3d.app.tin.fortune;

import java.awt.Point;

class MyPoint {
	public int index;

	public MyPoint(double d, double d1, double ix) {
		x = d;
		y = d1;
		index = (int) ix;
	}

	public MyPoint(double d, double d1) {
		x = d;
		y = d1;
		index = 0;
	}

	public MyPoint(MyPoint mypoint) {
		x = mypoint.x;
		y = mypoint.y;
		index = mypoint.index;
	}

	public MyPoint(Point point) {
		x = point.x;
		y = point.y;
		index = 0;
	}

	public double distance(MyPoint mypoint) {
		double d = mypoint.x - x;
		double d1 = mypoint.y - y;
		return Math.sqrt(d * d + d1 * d1);
	}

	public double x, y;
}
