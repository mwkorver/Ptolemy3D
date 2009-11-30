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

public class Fortune {
	public EventQueue Events;
	public ArcTree Arcs;
	public int XPos = 0;
	public TriangleMesh trimesh = new TriangleMesh();

	public Rectangle rect;

	int slip_id = 0;
	double slip_val = 0;

	public Fortune(double[][] pts, int XPos, int YPos, int width) {
		checkDegenerate(pts);
		Events = new EventQueue();
		for (int i = 0; i < pts.length; i++) {
			Events.insert(new EventPoint(pts[i][0], pts[i][1], pts[i][2]));
		}
		this.rect = new Rectangle(XPos, YPos, width, width);
		this.XPos = XPos;

	}

	public void checkDegenerate(double[][] pts) {
		if (pts.length > 1) {
			int min = 0, next = min;
			for (int i = 1; i < pts.length; i++) {
				if (pts[i][0] <= pts[min][0]) {
					next = min;
					min = i;
				}
				// else if(pts[i][0] <= pts[min][0]){
				// next = i;
				// }
			}

			if (pts[min][0] == pts[next][0] && min != next) {
				slip_id = min;
				slip_val = pts[min][0];

				pts[min][0]--;
				// System.out.println("Moved point: " + next + " -> " + min);
			}
		}
	}

	public void slipBack(double[][] pts) {
		pts[slip_id][0] = slip_val;
	}

	public void run() {
		Arcs = new ArcTree();
		trimesh.reset();
		while (singlestep())
			;

	}

	public boolean singlestep() {
		if (Events.Events == null || (double) XPos < Events.Events.x)
			XPos++;

		while (Events.Events != null && (double) XPos >= Events.Events.x) {
			EventPoint eventpoint = Events.pop();
			XPos = Math.max(XPos, (int) eventpoint.x);
			eventpoint.action(this);
			Arcs.checkBounds(this, XPos);
		}

		if (XPos > getBounds().width && Events.Events == null)
			Arcs.checkBounds(this, XPos);

		return Events.Events != null || XPos < 1000 + getBounds().width;
	}

	public Rectangle getBounds() {
		return rect;

	}

}