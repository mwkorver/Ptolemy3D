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

import java.awt.Rectangle;

/**
 * Code from: http://www.diku.dk/hjemmesider/studerende/duff/Fortune/
 */
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