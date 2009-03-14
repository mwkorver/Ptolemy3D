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

package org.ptolemy3d.tile;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.tile.jp2.Jp2Head;

/**
 * This class holds info for our tile data
 */
public class Jp2Tile
{
	static class Jp2TileRes {
		public boolean tileGotten = false;
		public boolean imageDataReady;
		public int textureId = -1;
		public int datakey = -1;
	}

	/** Number of resolution per jp2 tiles. */
	public final static int NUM_RESOLUTION = 4;

	/** Longitude in DD. */
	public int lon;
	/** Latitude in DD. */
	public int lat;
	/** Tile level */
	public int level = 0;
	/** Jp2 resolutions */
	protected final Jp2TileRes[] tileRes;
	/** Current resolution */
	public int curRes = 0;

	/** TIN Elevation */
	protected ElevationTin tin = null;
	/** DEM Elevation */
	protected ElevationDem dem = null;

	/* JP2 Location */
	/** Server ID of the JP2 Tile. */
	protected int tileDataServer;
	/** Location ID of the JP2 Tile. */
	protected int tileDataLoc;
	/** Path of the JP2 Tile relative to the location. */
	protected String fileBase;

	protected boolean gotten = false;
	protected boolean hasData = false;
	protected int scale;
	protected int terrainGotten;
	protected boolean inScene;

	// converts integer to left-zero padded string, len  chars long.
	private static String padWithZeros(int i, int len)
	{
		String s = Integer.toString(i);
		if (s.length() > len) {
			return s.substring(0, len);
		}
		else if (s.length() < len) {	// pad on left with zeros
			return "000000000000000000000000000".substring(0, len - s.length()) + s;
		}
		else {
			return s;
		}
	}

	protected Jp2Tile(int lvl, int scale, int x, int z)
	{
		tileDataServer = 0;
		tileDataLoc = 0;

		tileRes = new Jp2TileRes[NUM_RESOLUTION];
		for(int i = 0; i < NUM_RESOLUTION; i++) {
			tileRes[i] = new Jp2TileRes();
		}

		set(lvl, scale, x, z);
	}

	protected final void set(int lvl, int scale, int lon, int lat)
	{
		final Level[] levels = Ptolemy3D.ptolemy.scene.landscape.levels;

		this.curRes = 0;
		this.level = lvl;
		this.scale = scale;
		this.lon = lon;
		this.lat = lat;
		this.tin = null;

		fileBase = String.valueOf(scale) + "/";
		if (levels != null)
		{
			final int divider = levels[lvl].divider;

			if (divider > 0)
			{	//as usual
				fileBase += "D" + divider + "/x";
				int div_ = (lon / divider);
				fileBase += String.valueOf((div_ > 0) ? String.valueOf(div_) : "n" + (-div_));
				div_ = (lat / divider);
				fileBase += "y" + ((div_ > 0) ? String.valueOf(div_) : ("n" + (-div_))) + "/";
			}
			else if (divider < 0)
			{	//super funky style
				fileBase += lon < 0 ? "n" : "p";
				fileBase += lat < 0 ? "n" : "p";
				int absx = Math.abs(lon);
				int absz = Math.abs(lat);
				//pad x and y to 9 digits
				String xStr = padWithZeros(absx, 9);
				String yStr = padWithZeros(absz, 9);
				for (int k = 0; k < 4; k++)
				{
					fileBase += xStr.charAt(k);
					fileBase += yStr.charAt(k);
					fileBase += "/";
				}
			}
		}
		fileBase += Ptolemy3D.ptolemy.configuration.area + "x" + lon + "y" + lat;

		final QuadBufferPool quadBufferPool = Ptolemy3D.ptolemy.tileLoader.quadBufferPool;

		gotten = false;
		hasData = false;
		for (int i = 0; i < NUM_RESOLUTION; i++) {
			Jp2TileRes tileLevel = tileRes[i];
			tileLevel.tileGotten = false;
			tileLevel.textureId = -1;
			tileLevel.imageDataReady = false;
			if (tileLevel.datakey != -1) {
				quadBufferPool.free(i, tileLevel.datakey);
			}
			tileLevel.datakey = -1;
		}
		terrainGotten = 0;
		dem = null;
	}

	protected String fillFile(int res)
	{
		final Jp2TileLoader tileLoader = Ptolemy3D.ptolemy.tileLoader;
		return Ptolemy3D.ptolemy.configuration.backgroundImageUrl + "&lat=" + lat + "&lon=" + lon + "&scale=" + scale + "&w=" + tileLoader.twidth[res] + "&h=" + tileLoader.twidth[res] + "&res=" + res;
	}

	protected boolean setImageData(byte[] data, int[] meta, int res)
	{
		final QuadBufferPool quadBufferPool = Ptolemy3D.ptolemy.tileLoader.quadBufferPool;
		byte[] imageData = quadBufferPool.setArray(res, tileRes);
		if (imageData == null) {
			return false;
		}
		if (imageData.length != (meta[0] * meta[1] * 3)) {
			return false;
			// just copy over array
		}

		if (meta[3] == 3) {
			System.arraycopy(data, 0, imageData, 0, data.length);
		}
		else if (meta[3] == 4) {
			for (int i = 0; i < meta[1]; i++) {
				for (int j = 0; j < meta[0]; j++) {
					System.arraycopy(data, i * meta[0] * 4 + j * 4, imageData, i * meta[0] * 3 + j * 3, 3);
				}
			}
		}
		tileRes[res].imageDataReady = true;
		curRes = res;
		return true;
	}

	protected boolean setImageData(short[][] data, int res, Jp2Head jp2h)
	{
		final Jp2TileLoader tileLoader = Ptolemy3D.ptolemy.tileLoader;
		final QuadBufferPool quadBufferPool = tileLoader.quadBufferPool;
		byte[] imageData = quadBufferPool.setArray(res, tileRes);
		if (imageData == null) {
			return false;
		}

		int width = tileLoader.twidth[res];
		jp2h.setImageData(imageData, data, width);

		tileRes[res].imageDataReady = true;
		curRes = res;
		return true;
	}

	protected byte[] getImageData(int res)
	{
		if (tileRes[res].datakey >= 0) {
			final QuadBufferPool quadBufferPool = Ptolemy3D.ptolemy.tileLoader.quadBufferPool;
			return quadBufferPool.getArray(res, tileRes[res].datakey);
		}
		else {
			IO.println("ERROR: trying to request data that isn't there!");
			return null;
		}
	}

	protected final void dropTo(int res)
	{
		final QuadBufferPool quadBufferPool = Ptolemy3D.ptolemy.tileLoader.quadBufferPool;
		for (int i = res + 1; i < NUM_RESOLUTION; i++) {
			Jp2TileRes tileLevel = tileRes[i];
			tileLevel.tileGotten = false;
			tileLevel.textureId = -1;
			tileLevel.imageDataReady = false;
			if (tileLevel.datakey != -1) {
				quadBufferPool.free(i, tileLevel.datakey);
			}
			tileLevel.datakey = -1;
		}
		if (res < 0) {
			res = 0;
		}
		curRes = res;
	}

	public int getTextureId(int res)
	{
		final Jp2TileRes tile = tileRes[res];
		return tile.textureId;
	}
	
	/** @return the number of wavelets already loaded */
	public int getNumWavelets() {
		int num = 0;
		if(tileRes != null) {
			for(Jp2TileRes res : tileRes) {
				if(res != null && res.textureId > 0) {
					num++;
				}
			}
		}
		return num;
	}
}