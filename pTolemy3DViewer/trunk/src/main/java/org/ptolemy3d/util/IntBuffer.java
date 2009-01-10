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

public class IntBuffer
{
	private int[] buf;
	private int index = 0;
	private int sizeIncr;

	public IntBuffer()
	{
		this(10, 10);
	}

	public IntBuffer(int size, int sizeIncr)
	{
		this.buf = new int[size];
		this.sizeIncr = sizeIncr;
	}

	public void append(int i)
	{
		if (index >= buf.length) {
			int[] r = new int[buf.length + sizeIncr];
			if(buf.length > 0) {
				System.arraycopy(buf, 0, r, 0, buf.length);
			}
			buf = r;
		}
		buf[index++] = i;
	}

	public void reset()
	{
		index = 0;
	}

	public int size()
	{
		return index;
	}

	public int[] getBuffer()
	{
		if(buf == null || buf.length == 0) {
			return null;
		}
		int[] r = new int[index];
		System.arraycopy(buf, 0, r, 0, index);
		buf = r;
		return buf;
	}

	public int get(int ix)
	{
		return (ix < index) ? buf[ix] : -1;
	}

	public int find(int val)
	{
		for (int i = 0; i < index; i++) {
			if (buf[i] == val) {
				return i;
			}
		}
		return -1;
	}

	public int popBuf()
	{
		index--;
		return buf[index];
	}
}
