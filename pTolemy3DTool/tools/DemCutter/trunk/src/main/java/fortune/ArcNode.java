/**
 * visual-fortune-algorithm
 * http://github.com/sorbits/visual-fortune-algorithm/tree/master
 *
 * ABOUT
 * This Java applet implements a visualization of Fortunes plane-sweep algorithm
 * for creating a voronoi diagram.
 * Here is a live demo of the applet: http://www.diku.dk/hjemmesider/studerende/duff/Fortune/
 * The applet was created by Benny Kjor Nielsen and Allan Odgaard in spring of 2000
 * following a course in Computational Geometry taught by Pawel Winter at DIKU.
 *
 * LICENSE
 * Permission to copy, use, modify, sell and distribute this software is granted.
 * This software is provided as is without express or implied warranty,
 * and with no claim as to its suitability for any purpose.
 *
 * NOTES
 * The purpose of the source is to visualize the algorithm, it is not a good base
 * for an efficient implementation of the algorithm (it does not run in O(n log n) time).
 * The original source was initially lost and recovered using a Java decompiler
 * so most variable names are nonsensical.
 */
package fortune;

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
