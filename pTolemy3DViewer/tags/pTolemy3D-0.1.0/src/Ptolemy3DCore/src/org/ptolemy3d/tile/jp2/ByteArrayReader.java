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
package org.ptolemy3d.tile.jp2;

import java.io.IOException;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DConfiguration;
import org.ptolemy3d.io.BasicCommunicator;
import org.ptolemy3d.io.Communicator;

/**
 * A class to do the same thing as DataInputStream,
 * but from an array rather than an input stream.<BR>
 * <BR>
 * Note : we could have used DataInputStream Wrapped around ByteArrayInputStream,
 * but this implementation is lighter...
 */
class ByteArrayReader
{
	private final Jp2Head jp2Head;

	private byte[] ba;
	private int pos = 0;

	public ByteArrayReader(Jp2Head jp2Head)
	{
		this.jp2Head = jp2Head;
	}

	public final void setByteArray(byte[] ba)
	{
		this.pos = 0;
		this.ba = ba;
	}

	public final int readUnsignedShort()
	{
		return read() << 8 | read();
	}

	public final int readInt()
	{
		return read() << 24 | read() << 16 | read() << 8 | read();
	}

	public final int read()
	{
		int b = 0;
		try {
			b = (ba[pos] & 0xff);
			pos++;
		}
		catch (ArrayIndexOutOfBoundsException e) {
			getMoreHeaderData(jp2Head.jp2Header.fileBase);	//Fixme
			b = (ba[pos] & 0xff);
			pos++;
		}
		return b;
	}

	public final short readShort()
	{
		return (short) (read() << 8 | read());
	}

	public final int readUnsignedByte()
	{
		return read();
	}

	public final void readFully(byte[] b, int offset, int length)
	{
		for (int i = offset; i < length + offset; i++) {
			int c = read();
			b[i] = (byte) c;
		}
	}

	public final int skipBytes(int len)
	{
		pos += len;
		return len;
	}

	public final boolean notValidStream()
	{
		if (ba == null) {
			return true;
		}
		else {
			return false;
		}
	}

	public int getPos()
	{
		return pos;
	}

	/*
	 *  this function is called in the case that there is not enought data to cover the header.
	 *
	 */
	 private void getMoreHeaderData(String fileBase)
	 {
		 final Ptolemy3DConfiguration settings = Ptolemy3D.ptolemy.configuration;

		 Communicator myJC = new BasicCommunicator(null);	//Server set in the loop
		 for (int i = 0; i < settings.dataServers.length; i++)
		 {
			 for (int j = 0; j < settings.locations[i].length; j++)
			 {
				 myJC.setServer(settings.dataServers[i]);
				 // grab data from the end position
				 try
				 {
					 myJC.verify();
					 myJC.requestMapData(settings.locations[i][j] + fileBase + ".jp2", pos, pos + 1000);
					 byte[] data = myJC.getData(1001);
					 if (data != null)
					 {
						 // add this data to ba array
						 byte[] newba = new byte[pos + 1001];
						 System.arraycopy(ba, 0, newba, 0, pos);
						 System.arraycopy(data, 0, newba, pos, 1001);
						 this.ba = newba;
						 myJC.endSocket();
						 myJC = null;
						 return;
					 }
				 }
				 catch (IOException e)
				 {
				 }
			 }
		 }
		 myJC = null;
	 }
}
