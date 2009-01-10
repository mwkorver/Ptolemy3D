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
 * A vector with 3 components with double precision.
 */
public final class Vector3d
{
	private Vector3d() {};

	public static final void normalize(double[] n)
	{
		double mag = Math.sqrt((n[0] * n[0]) + (n[1] * n[1]) + (n[2] * n[2]));
		mag = 1 / mag;
		n[0] *= mag;
		n[1] *= mag;
		n[2] *= mag;
	}

	public static final void cross(double[] dest, double[] v1, double[] v2)
	{
		dest[0] = v1[1] * v2[2] - v1[2] * v2[1];
		dest[1] = v1[2] * v2[0] - v1[0] * v2[2];
		dest[2] = v1[0] * v2[1] - v1[1] * v2[0];
	}

	public static final double dot(double[] a, double[] b)
	{
		return (a[0] * b[0]) + (a[1] * b[1]) + (a[2] * b[2]);
	}

	public static final void copy(double[] dest, double[] src)
	{
		dest[0] = src[0];
		dest[1] = src[1];
		dest[2] = src[2];
	}

	public static final void sub(double[] dest, double[] v1, double[] v2)
	{
		dest[0] = v1[0] - v2[0];
		dest[1] = v1[1] - v2[1];
		dest[2] = v1[2] - v2[2];
	}

	public static final void negate(double[] n)
	{
		n[0] = -n[0];
		n[1] = -n[1];
		n[2] = -n[2];
	}

	public static final void scale(double[] n, double s)
	{
		n[0] *= s;
		n[1] *= s;
		n[2] *= s;
	}
}
