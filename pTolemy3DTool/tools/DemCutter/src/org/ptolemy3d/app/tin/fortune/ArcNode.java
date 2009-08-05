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

import java.awt.Rectangle;

class ArcNode extends ParabolaPoint {
	public ArcNode(MyPoint mypoint) {
		super(mypoint);
	}

	public void checkCircle(EventQueue eventqueue) {
		if (Prev != null && Next != null) {
			circlePoint = calculateCenter(Next, this, Prev);
			if (circlePoint != null)
				eventqueue.insert(circlePoint);
		}
	}

	public void removeCircle(EventQueue eventqueue) {
		if (circlePoint != null) {
			eventqueue.remove(circlePoint);
			circlePoint = null;
		}
	}

	public void completeTrace(Fortune fortune, MyPoint mypoint) {
		if (startOfTrace != null) {
			fortune.trimesh.insertEdge(new MyLine(this, Next));
			startOfTrace = null;
		}
	}

	public void checkBounds(Fortune fortune, double d) {
		if (Next != null) {
			Next.init(d);
			if (d > Next.x && d > x && startOfTrace != null) {
				try {
					double ad[] = solveQuadratic(a - Next.a, b - Next.b, c
							- Next.c);
					double d1 = ad[0];
					double d2 = d - F(d1);
					Rectangle rectangle = fortune.getBounds();
					if (d2 < startOfTrace.x && d2 < 0.0D || d1 < 0.0D
							|| d2 >= (double) rectangle.width
							|| d1 >= (double) rectangle.height)
						completeTrace(fortune, new MyPoint(d2, d1));
				} catch (Throwable _ex) {
					System.out.println("*** exception");
				}
			}
			Next.checkBounds(fortune, d);
		}
	}

	public void insert(ParabolaPoint parabolapoint, double sline,
			EventQueue eventqueue) throws Throwable {
		boolean split = true;
		if (Next != null) {
			Next.init(sline);
			if (sline > Next.x && sline > x) {
				double xs[] = solveQuadratic(a - Next.a, b - Next.b, c - Next.c);
				if (xs[0] <= parabolapoint.realX() && xs[0] != xs[1])
					split = false;
			} else {
				split = false;
			}
		}

		if (split) {
			removeCircle(eventqueue);

			ArcNode arcnode = new ArcNode(parabolapoint);
			arcnode.Next = new ArcNode(this);
			arcnode.Prev = this;
			arcnode.Next.Next = Next;
			arcnode.Next.Prev = arcnode;

			if (Next != null)
				Next.Prev = arcnode.Next;

			Next = arcnode;

			checkCircle(eventqueue);
			Next.Next.checkCircle(eventqueue);

			Next.Next.startOfTrace = startOfTrace;
			startOfTrace = new MyPoint(sline - F(parabolapoint.y),
					parabolapoint.y);
			Next.startOfTrace = new MyPoint(sline - F(parabolapoint.y),
					parabolapoint.y);
		} else {
			Next.insert(parabolapoint, sline, eventqueue);
		}
	}

	ArcNode Next, Prev;
	CirclePoint circlePoint;
	MyPoint startOfTrace;
}
