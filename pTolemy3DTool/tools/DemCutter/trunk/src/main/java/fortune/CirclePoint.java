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
