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
class CirclePoint extends EventPoint {

	CirclePoint(double d, double d1, ArcNode arcnode) {
		super(d, d1, 0);
		arc = arcnode;
		radius = distance(arcnode);
		x += radius;
	}

	public void action(Fortune fortune) {
		ArcNode arcnode = arc.Prev;
		ArcNode arcnode1 = arc.Next;
		MyPoint mypoint = new MyPoint(x - radius, y, 0);
		arc.completeTrace(fortune, mypoint);
		arcnode.completeTrace(fortune, mypoint);
		arcnode.startOfTrace = mypoint;
		arcnode.Next = arcnode1;
		arcnode1.Prev = arcnode;
		if (arcnode.circlePoint != null) {
			fortune.Events.remove(arcnode.circlePoint);
			arcnode.circlePoint = null;
		}
		if (arcnode1.circlePoint != null) {
			fortune.Events.remove(arcnode1.circlePoint);
			arcnode1.circlePoint = null;
		}
		arcnode.checkCircle(fortune.Events);
		arcnode1.checkCircle(fortune.Events);
	}

	double radius;
	ArcNode arc;
}
