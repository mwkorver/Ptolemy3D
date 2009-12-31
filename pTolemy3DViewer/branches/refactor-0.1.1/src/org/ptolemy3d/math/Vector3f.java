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
 * A vector with 3 components with float precision.
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
public final class Vector3f implements Cloneable {
	public float x;
	public float y;
	public float z;
	
	public Vector3f() {
		this(0, 0, 0);
	}
	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Vector3f(Vector3f vec) {
		this(vec.x, vec.y, vec.z);
	}
	
	@Override
	public String toString() {
		return super.toString()+"("+x+", "+y+", "+z+")";
	}
	
	@Override
	protected Vector3f clone() {
		return new Vector3f(x, y, z);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(getClass() == obj.getClass()) {
			final Vector3f vec = (Vector3f)obj;
			return ((vec.x == x) && (vec.y == y) && (vec.z == z));
		}
		else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		long hash;
		hash = 31L        + Float.floatToIntBits(x);
		hash = 31L * hash + Float.floatToIntBits(y);
		hash = 31L * hash + Float.floatToIntBits(z);
		return (int)(hash ^ (hash>>32));
	}
	
	public final boolean isZero() {
		return (x == 0) && (y == 0) && (z == 0);
	}
	
	public final boolean isUnit() {
		return (x == 1) && (y == 1) && (z == 1);
	}
	
	public final float magnitude() {
		return (float)Math.sqrt((x * x) + (y * y) + (z * z));
	}
	
	public final void normalize() {
		final float mag = 1 / magnitude();
		x *= mag;
		y *= mag;
		z *= mag;
	}

	public final void cross(Vector3f vec1, Vector3f vec2) {
		float x = vec1.y * vec2.z - vec1.z * vec2.y;
		float y = vec1.z * vec2.x - vec1.x * vec2.z;
		float z = vec1.x * vec2.y - vec1.y * vec2.x;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public final float dot(Vector3f vec) {
		return (x * vec.x) + (y * vec.y) + (z * vec.z);
	}

	public final Vector3f set(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	public final Vector3f set(Vector3f vec) {
		x = vec.x;
		y = vec.y;
		z = vec.z;
		return this;
	}

	public final Vector3f add(Vector3f vec1, Vector3f vec2) {
		x = vec1.x + vec2.x;
		y = vec1.y + vec2.y;
		z = vec1.z + vec2.z;
		return this;
	}

	public final Vector3f sub(Vector3f vec1, Vector3f vec2) {
		x = vec1.x - vec2.x;
		y = vec1.y - vec2.y;
		z = vec1.z - vec2.z;
		return this;
	}

	public final Vector3f negate() {
		x = -x;
		y = -y;
		z = -z;
		return this;
	}

	public final Vector3f scale(float s) {
		x *= s;
		y *= s;
		z *= s;
		return this;
	}
	
    public float distance(Vector3f vec) {
        float x = (vec.x - this.x);
        float y = (vec.y - this.y);
        float z = (vec.z - this.z);
        return (float)Math.sqrt((x * x) + (y * y) + (z * z));
    }
}
