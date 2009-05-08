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
package org.ptolemy3d.util;

/**
 * Utility to read in a byte array.
 */
public class ByteReader {
	private final byte[] bytes;
	private int position;
	
	public ByteReader(byte[] data) {
		this.bytes = data;
		this.position = 0;
	}
	
	public final int readInt() {
		return (bytes[position++] & 0xff) << 24 |
			   (bytes[position++] & 0xff) << 16 |
			   (bytes[position++] & 0xff) << 8 |
			   (bytes[position++] & 0xff) << 0;
	}

	public final long readLong() {
		return (long) (bytes[position++] & 0xff) << 56 |
			   (long) (bytes[position++] & 0xff) << 48 |
			   (long) (bytes[position++] & 0xff) << 40 |
			   (long) (bytes[position++] & 0xff) << 32 |
			   (long) (bytes[position++] & 0xff) << 24 |
			   (long) (bytes[position++] & 0xff) << 16 |
			   (long) (bytes[position++] & 0xff) <<  8 |
			   (long) (bytes[position++] & 0xff) <<  0;
	}

	public final float readFloat() {
		return Float.intBitsToFloat(readInt());
	}

	public final double readDouble() {
		return Double.longBitsToDouble(readLong());
	}
}
