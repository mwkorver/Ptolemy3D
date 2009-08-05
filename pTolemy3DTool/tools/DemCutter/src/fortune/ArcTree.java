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
package fortune;

/**
 * Code from: http://www.diku.dk/hjemmesider/studerende/duff/Fortune/
 */
class ArcTree {
	public void insert(MyPoint mypoint, double d, EventQueue eventqueue) {
		if (Arcs == null) {
			Arcs = new ArcNode(mypoint);
			return;
		}
		try {
			ParabolaPoint parabolapoint = new ParabolaPoint(mypoint);
			parabolapoint.init(d);
			Arcs.init(d);
			Arcs.insert(parabolapoint, d, eventqueue);
			return;
		} catch (Throwable _ex) {
			System.out.println("*** error: No parabola intersection during ArcTree.insert()");
		}
	}

	public void checkBounds(Fortune fortune, double d) {
		if (Arcs != null) {
			Arcs.init(d);
			Arcs.checkBounds(fortune, d);
		}
	}

	ArcNode Arcs;
}
