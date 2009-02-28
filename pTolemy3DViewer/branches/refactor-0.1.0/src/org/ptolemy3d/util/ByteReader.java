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
public class ByteReader
{
	public final static int readInt(byte[] b, int[] cur)
	{
		return (b[cur[0]++] & 0xff) << 24 |
			   (b[cur[0]++] & 0xff) << 16 |
			   (b[cur[0]++] & 0xff) << 8 |
			   (b[cur[0]++] & 0xff) << 0;
	}

	public final static long readLong(byte[] b, int[] cur)
	{
		return (long) (b[cur[0]++] & 0xff) << 56 |
			   (long) (b[cur[0]++] & 0xff) << 48 |
			   (long) (b[cur[0]++] & 0xff) << 40 |
			   (long) (b[cur[0]++] & 0xff) << 32 |
			   (long) (b[cur[0]++] & 0xff) << 24 |
			   (long) (b[cur[0]++] & 0xff) << 16 |
			   (long) (b[cur[0]++] & 0xff) << 8 |
			   (long) (b[cur[0]++] & 0xff) << 0;
	}

	public final static float readFloat(byte[] b, int[] cur)
	{
		return Float.intBitsToFloat(readInt(b, cur));
	}

	public final static double readDouble(byte[] b, int[] cur)
	{
		return Double.longBitsToDouble(readLong(b, cur));
	}
}
