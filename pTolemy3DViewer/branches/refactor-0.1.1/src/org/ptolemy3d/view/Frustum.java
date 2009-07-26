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
package org.ptolemy3d.view;

import org.ptolemy3d.math.Matrix16d;
import org.ptolemy3d.math.Vector3d;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Frustum {
	private final static int RIGHT_PLANE  = 0;
	private final static int LEFT_PLANE   = 1;
	private final static int BOTTOM_PLANE = 2;
	private final static int TOP_PLANE    = 3;
	private final static int FAR_PLANE    = 4;
	private final static int NEAR_PLANE   = 5;
	
	static class Plane {
		final Vector3d n = new Vector3d();
		double d = 0;
		public double dot(Vector3d point) {
			return n.dot(point) + d;
		}
	}

	/** Frustum planes */
	private final Plane[] sides;
	/** proj x modelview matrix */
	private final Matrix16d clip;

	public Frustum() {
		sides = new Plane[6];
		clip = new Matrix16d();
		for(int i = 0; i < sides.length; i++) {
			sides[i] = new Plane();
		}
	}
	
	protected void update(Matrix16d proj, Matrix16d view) {
		fromProjectionModelview(clip, proj, view);
		
		setPlane(sides[RIGHT_PLANE],
				clip.m[ 3] - clip.m[ 0],
				clip.m[ 7] - clip.m[ 4],
				clip.m[11] - clip.m[ 8],
				clip.m[15] - clip.m[12]);
		setPlane(sides[LEFT_PLANE],
				clip.m[ 3] + clip.m[ 0],
				clip.m[ 7] + clip.m[ 4],
				clip.m[11] + clip.m[ 8],
				clip.m[15] + clip.m[12]);
		setPlane(sides[BOTTOM_PLANE],
				clip.m[ 3] + clip.m[ 1],
				clip.m[ 7] + clip.m[ 5],
				clip.m[11] + clip.m[ 9],
				clip.m[15] + clip.m[13]);
		setPlane(sides[TOP_PLANE],
				clip.m[ 3] - clip.m[ 1],
				clip.m[ 7] - clip.m[ 5],
				clip.m[11] - clip.m[ 9],
				clip.m[15] - clip.m[13]);
		setPlane(sides[FAR_PLANE],
				clip.m[ 3] - clip.m[ 2],
				clip.m[ 7] - clip.m[ 6],
				clip.m[11] - clip.m[10],
				clip.m[15] - clip.m[14]);
		setPlane(sides[NEAR_PLANE],
				clip.m[ 3] + clip.m[ 2],
				clip.m[ 7] + clip.m[ 6],
				clip.m[11] + clip.m[10],
				clip.m[15] + clip.m[14]);
	}
	public final void fromProjectionModelview(Matrix16d dst, Matrix16d proj, Matrix16d view) {
		double m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15;

		m0  =   view.m[ 0] * proj.m[ 0];
		m1  =   view.m[ 1] * proj.m[ 5];
		m2  =   view.m[ 2] * proj.m[10];
		m3  = - view.m[ 2];

		m4  =   view.m[ 4] * proj.m[ 0];
		m5  =   view.m[ 5] * proj.m[ 5];
		m6  =   view.m[ 6] * proj.m[10];
		m7  = - view.m[ 6];

		m8  =   view.m[ 8] * proj.m[ 0];
		m9  =   view.m[ 9] * proj.m[ 5];
		m10 =   view.m[10] * proj.m[10];
		m11 = - view.m[10];

		m12 =   view.m[12] * proj.m[ 0];
		m13 =   view.m[13] * proj.m[ 5];
		m14 =   view.m[14] * proj.m[10] + proj.m[14];
		m15 = - view.m[14];

		dst.m[ 0] = m0;
		dst.m[ 1] = m1;
		dst.m[ 2] = m2;
		dst.m[ 3] = m3;

		dst.m[ 4] = m4;
		dst.m[ 5] = m5;
		dst.m[ 6] = m6;
		dst.m[ 7] = m7;

		dst.m[ 8] = m8;
		dst.m[ 9] = m9;
		dst.m[10] = m10;
		dst.m[11] = m11;

		dst.m[12] = m12;
		dst.m[13] = m13;
		dst.m[14] = m14;
		dst.m[15] = m15;
	}
	private void setPlane(Plane plane, double a, double b, double c, double d) {
		double magnitude = Math.sqrt(a * a + b * b + c * c);
		if(magnitude == 0) {
			magnitude = 1;
		}
		else {
			magnitude = 1 / magnitude;
		}

		plane.n.x = a * magnitude;
		plane.n.y = b * magnitude;
		plane.n.z = c * magnitude;
		plane.d = d * magnitude;
	}
	
	public boolean insideFrustum(Vector3d point) {
		for(Plane plane : sides) {
			final double dist = plane.dot(point);
			if(dist <= 0) {
				return false;
			}
		}
		return true;
	}
	public boolean insideFrustum(Vector3d point0, Vector3d point1, Vector3d point2, Vector3d point3) {
		for(Plane plane : sides) {
			double dist;
			dist = plane.dot(point0);
			if(dist > 0) {
				continue;	//Inside
			}
			dist = plane.dot(point1);
			if(dist > 0) {
				continue;	//Inside
			}
			dist = plane.dot(point2);
			if(dist > 0) {
				continue;	//Inside
			}
			dist = plane.dot(point3);
			if(dist > 0) {
				continue;	//Inside
			}

			return false;	//All point are outside the plane
		}
		return true;
	}
}