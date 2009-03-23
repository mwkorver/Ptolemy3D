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
package org.ptolemy3d.jp2;

import java.io.IOException;

import org.ptolemy3d.io.Communicator;

/**
 * Internal to JP2 Decoder
 */
public class Jp2Block
{
	public String fileBase;

    // arrays to hold Tile header data
    protected int[][] firstPackOff;

    /*
     * CODEBLOCK INFO:: tile | component | resolution | orientation(subband) | y | x | layer
     * length of each code block
     */
    protected short[][][][][][][] CB_len;	// val cannot exceed 4096, short will do
    protected int[][][][][][][] CB_off;	// offset to each codeblock in stream
    protected boolean[][][][][][] cbInc;	// for tag tree inclusion
    protected int[][][][][][][] cbTpLyr; 	// trunc points per layer
    protected int[][][][][][] msbSk;		// trunc points per layer

    //starting point in stream from this progression level
    protected int[][] progPackStart;
	//ending point in stream from this progression level
    protected int[][] progPackEnd;

	public Jp2Block(){}

	public void requestMapData(Communicator JC, String file, int res) throws IOException
	{
		int start = progPackStart[0][res];
		int end = progPackEnd[0][res];
		JC.requestMapData(file, start, end);
	}
	public byte[] getData(Communicator JC, int res) throws IOException
	{
		int start = progPackStart[0][res];
		int end = progPackEnd[0][res];
		return JC.getData(end - start + 1);
	}
}