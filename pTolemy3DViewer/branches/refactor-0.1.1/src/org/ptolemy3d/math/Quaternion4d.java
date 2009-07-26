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
 * A quaternion with double precision.
 */
public class Quaternion4d {
	private double x;
	private double y;
	private double z;
	private double w;

	public Quaternion4d() {}

	public Quaternion4d(double theta, Vector3d vec) {
		set(theta, vec);
	}

	public final void set(double theta, Vector3d vec) {
		x = Math.cos(theta / 2);
		y = vec.x * Math.sin(theta / 2);
		z = vec.y * Math.sin(theta / 2);
		w = vec.z * Math.sin(theta / 2);
		final double mag = Math.sqrt(x * x + y * y + z * z + w * w);
		x /= mag;
		y /= mag;
		z /= mag;
		w /= mag;
	}

	public final void setMatrix(Matrix9d m) {
		m.m[0][0] = x * x + y * y - z * z - w * w;
		m.m[1][0] = 2 * (z * y + x * w);
		m.m[2][0] = 2 * (w * y - x * z);

		m.m[0][1] = 2 * (y * z - x * w);
		m.m[1][1] = x * x - y * y + z * z - w * w;
		m.m[2][1] = 2 * (w * z + x * y);

		m.m[0][2] = 2 * (y * w + x * z);
		m.m[1][2] = 2 * (z * w - x * y);
		m.m[2][2] = x * x - y * y - z * z + w * w;
	}
}