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
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
public final class Vector3d implements Cloneable {
	public double x;
	public double y;
	public double z;
	
	public Vector3d() {
		this(0, 0, 0);
	}
	public Vector3d(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Vector3d(Vector3d vec) {
		this(vec.x, vec.y, vec.z);
	}
	
	@Override
	public String toString() {
		return super.toString()+"("+x+", "+y+", "+z+")";
	}
	
	@Override
	protected Vector3d clone() {
		return new Vector3d(x, y, z);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(getClass() == obj.getClass()) {
			final Vector3d vec = (Vector3d)obj;
			return ((vec.x == x) && (vec.y == y) && (vec.z == z));
		}
		else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		long hash;
		hash = 31L        + Float.floatToIntBits((float)x);
		hash = 31L * hash + Float.floatToIntBits((float)y);
		hash = 31L * hash + Float.floatToIntBits((float)z);
		return (int)(hash ^ (hash>>32));
	}
	
	public final boolean isZero() {
		return (x == 0) && (y == 0) && (z == 0);
	}
	
	public final boolean isUnit() {
		return (x == 1) && (y == 1) && (z == 1);
	}
	
	public final double magnitude() {
		return Math.sqrt((x * x) + (y * y) + (z * z));
	}
	
	public final void normalize() {
		final double mag = 1 / magnitude();
		x *= mag;
		y *= mag;
		z *= mag;
	}

	public final void cross(Vector3d vec1, Vector3d vec2) {
		double x = vec1.y * vec2.z - vec1.z * vec2.y;
		double y = vec1.z * vec2.x - vec1.x * vec2.z;
		double z = vec1.x * vec2.y - vec1.y * vec2.x;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final double dot(Vector3d vec) {
		return (x * vec.x) + (y * vec.y) + (z * vec.z);
	}

	public final Vector3d set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	public final Vector3d set(Vector3d vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
		return this;
	}

	public final Vector3d add(Vector3d vec1, Vector3d vec2) {
		x = vec1.x + vec2.x;
		y = vec1.y + vec2.y;
		z = vec1.z + vec2.z;
		return this;
	}

	public final Vector3d sub(Vector3d vec1, Vector3d vec2) {
		x = vec1.x - vec2.x;
		y = vec1.y - vec2.y;
		z = vec1.z - vec2.z;
		return this;
	}

	public final Vector3d negate() {
		x = -x;
		y = -y;
		z = -z;
		return this;
	}

	public final Vector3d scale(double s) {
		x *= s;
		y *= s;
		z *= s;
		return this;
	}
	
    public double distance(Vector3d vec) {
        double x = (vec.x - this.x);
        double y = (vec.y - this.y);
        double z = (vec.z - this.z);
        return Math.sqrt((x * x) + (y * y) + (z * z));
    }
}
