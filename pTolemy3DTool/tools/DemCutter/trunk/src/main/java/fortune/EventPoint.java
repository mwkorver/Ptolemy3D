/**
 * visual-fortune-algorithm
 * http://github.com/sorbits/visual-fortune-algorithm/tree/master
 * 
 * ABOUT
 * This Java applet implements a visualization of Fortune’s plane-sweep algorithm
 * for creating a voronoi diagram.
 * Here is a live demo of the applet: http://www.diku.dk/hjemmesider/studerende/duff/Fortune/
 * The applet was created by Benny Kjær Nielsen and Allan Odgaard in spring of 2000
 * following a course in Computational Geometry taught by Pawel Winter at DIKU.
 * 
 * LICENSE
 * Permission to copy, use, modify, sell and distribute this software is granted.
 * This software is provided “as is” without express or implied warranty,
 * and with no claim as to its suitability for any purpose.
 * 
 * NOTES
 * The purpose of the source is to visualize the algorithm, it is not a good base
 * for an efficient implementation of the algorithm (it does not run in O(n log n) time).
 * The original source was initially lost and recovered using a Java decompiler
 * so most variable names are nonsensical.
 */
package fortune;

class EventPoint extends MyPoint {

	EventPoint(MyPoint mypoint) {
		super(mypoint);
	}

	EventPoint(double d, double d1, double ix) {
		super(d, d1, ix);
	}

	public void action(Fortune fortune) {
		fortune.Arcs.insert(this, fortune.XPos, fortune.Events);
	}

	EventPoint Prev, Next;
}
