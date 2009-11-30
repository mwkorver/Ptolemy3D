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

class MyLine {

	MyLine(MyPoint mypoint, MyPoint mypoint1) {
		P1 = mypoint;
		P2 = mypoint1;
	}

	public void set(MyPoint mypoint, MyPoint mypoint1) {
		P1 = mypoint;
		P2 = mypoint1;
	}

	public MyPoint P1, P2;
}
