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
package org.ptolemy3d.data;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.ElevationDem;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.io.DataFinder;
import org.ptolemy3d.io.Stream;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class DemDataEntry extends MapDataEntry {
	public final static int NUM_DECODERUNIT = 1;
	
	public DemDataEntry(MapData mapData) {
		super(mapData);
	}

	@Override
	public URL findData(MapData mapData) {
		final DataFinder dataFinder = Ptolemy3D.getDataFinder();
		return dataFinder.findDem(mapData);
	}

	@Override
	public Stream findDataFromCache(MapData mapData) {
		final DataFinder dataFinder = Ptolemy3D.getDataFinder();
		return dataFinder.findDemFromCache(mapData);
	}
	
	@Override
	public boolean decode() {
		if(isDecoded(0)) {
			return true;
		}
		
		try {
			IO.printfParser("Parse DEM: %s\n", mapData.key);
			
			final File file = getStream().createFile();
			final byte[] b = new byte[(int)file.length()];
			final FileChannel channel = new FileInputStream(file).getChannel();
			channel.read(ByteBuffer.wrap(b));
			channel.close();
			
			mapData.dem = new ElevationDem(b);
		}
		catch(Throwable t) {
			IO.printStackConnection(t);
		}
		
		return isDecoded(0);
	}

	@Override
	public void freeDecoder() {}

	@Override
	public int getNextDecoderUnit() {
		if(isDecoded(0)) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean isDecoded(int unit) {
		return mapData.dem != null;
	}
}