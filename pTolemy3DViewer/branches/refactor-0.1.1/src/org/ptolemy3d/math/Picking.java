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
package org.ptolemy3d.math;

/**
 * Picking / Ray Tracing
 */
public class Picking {
	private final Vector3d[] T;
	
	public Picking() {
		T = new Vector3d[] {new Vector3d(), new Vector3d(), new Vector3d()};
	}
	
	public final void setTriangle(int triangleID, double x, double y, double z) {
		T[triangleID].set(x, y, z);
	}

	/** picks triangle and slides verticeis 1 back */
	public final boolean pickTri(Vector3d arr, Vector3d[] R) {
		boolean found = false;
		if (rayIntersectTri(arr, R) == 1) {
			found = true;
		}
		T[0].set(T[1]);
		T[1].set(T[2]);
		return found;
	}

	public int rayIntersectTri(Vector3d dest, Vector3d[] tris) {
		Vector3d u, v, n; // triangle vectors
		Vector3d dir, w0, w; // ray vectors
		double r, a, b; // params to calc ray-plane intersect

		u = new Vector3d();
		v = new Vector3d();
		n = new Vector3d();
		dir = new Vector3d();
		w0 = new Vector3d();
		w = new Vector3d();

		// get triangle edge vectors and plane normal
		u.sub(T[1], T[0]);
		v.sub(T[2], T[0]);

		n.cross(u, v);

		if (n.isZero()) { // triangle is degenerate
			return -1; // do not deal with this case
		}
		dir.sub(tris[1], tris[0]);
		w0.sub(tris[0], T[0]);

		a = n.dot(w0);
		b = n.dot(dir);

		if (Math.abs(b) < 0.00000001) { // ray is parallel to triangle plane
			if (a == 0) { // ray lies in triangle plane
				return 2;
			}
			else {
				return 0; // ray disjoint from plane
			}
		}

		// get intersect point of ray with triangle plane
		r = -a / b;
		if (r < 0.0) { // ray goes away from triangle
			return 0; // => no intersect
			// for a segment, also test if (r > 1.0) => no intersect
		}
		//xyzt = tris[0] + (r * dir);
		Vector3d xyzt = new Vector3d();
		xyzt.set(dir).scale(r).add(xyzt, tris[0]);

		// is I inside T?
		double uu, uv, vv, wu, wv, D;
		uu = u.dot(u);
		uv = u.dot(v);
		vv = v.dot(v);

		w.sub(xyzt, T[0]);

		wu = w.dot(u);
		wv = w.dot(v);
		D = uv * uv - uu * vv;

		// get and test parametric coords
		double s, t;
		s = (uv * wv - vv * wu) / D;
		if ((s < 0.0) || (s > 1.0)) { // I is outside T
			return 0;
		}
		t = (uv * wu - uu * wv) / D;
		if ((t < 0.0) || ((s + t) > 1.0)) { // I is outside T
			return 0;
		}
		if ((dest.x == -999) && (dest.y == -999) && (dest.z == -999)) {
			dest.set(xyzt);
		}
		else if (tris[0].distance(xyzt) < tris[0].distance(dest)) {
			dest.set(xyzt);
		}
		else {
			return 0;
			// IntPt[0]=xt;IntPt[1]=yt;IntPt[2]=zt;
		}
		return 1; // I is in T
	}
}
