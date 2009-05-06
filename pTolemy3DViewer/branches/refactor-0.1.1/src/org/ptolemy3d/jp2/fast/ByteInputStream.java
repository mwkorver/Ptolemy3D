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
package org.ptolemy3d.jp2.fast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

class ByteInputStream {
	private final InputStream is;
	private int position;

	public ByteInputStream(InputStream is) {
		this.is = new BufferedInputStream(is);
		this.position = 0;
	}

	public final int readUnsignedByte() throws IOException {
		return read();
	}

	public final short readShort() throws IOException {
		return (short)(read() << 8 | read());
	}

	public final int readUnsignedShort() throws IOException {
		return read() << 8 | read();
	}

	public final int readInt() throws IOException {
		return read() << 24 | read() << 16 | read() << 8 | read();
	}

	public final int read() throws IOException {
		final int read = is.read();
		position++;
		return read;
	}

	public final int read(byte[] b, int offset, int length) throws IOException {
		int remaining = length;
		while (remaining > 0) {
			final int realRead = is.read(b, offset, remaining);
			position += realRead;
			offset += realRead;
			remaining -= realRead;
		}
		return length - remaining;
	}

	public final long skip(long numBytes) throws IOException {
		long remaining = numBytes;
		while(remaining > 0) {
			final long skipped = is.skip(remaining);
			position += skipped;
			remaining -= skipped;
		}
		return numBytes - remaining;
	}
	
	public int getPosition() {
		return position;
	}
	
	public void close() throws IOException {
		is.close();
	}
}
