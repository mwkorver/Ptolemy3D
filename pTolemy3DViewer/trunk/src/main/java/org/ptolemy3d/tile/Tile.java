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

import static org.ptolemy3d.Ptolemy3DConfiguration.EARTH_RADIUS;
import static org.ptolemy3d.debug.Config.DEBUG;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.ProfilerInterface;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.tile.Jp2Tile.Jp2TileRes;

/**
 * A tile.
 */
class Tile
{
	protected static interface ITileRenderer {
		public void drawSubsection(Tile tile, int x1, int z1, int x2, int z2) throws Exception;
	}

	/* Variable updated by setTile */
	protected int tileID;
	protected int levelID;
	protected int upLeftLon, upLeftLat;
	protected int lowRightLon, lowRightLat;
	protected int fmx, fmz;

	/* TexTile renderer */
//	private static final ITileRenderer renderer = new TileDefaultRenderer();				//Original version
//	private static final ITileRenderer renderer = new TileDirectModeRenderer();			//CPU Optimizations
	private static final ITileRenderer renderer = new TileDirectModeRenderer_MathLookUp();//Cos/Sin table lookup
//	private static final ITileRenderer renderer = new TileVARenderer();					//

	/* Variable updated by Landscape */
	/** Tile status (for visibility, ...) */
	public boolean status;
	protected Jp2Tile jp2;

	/* Temporary datas for display */
	protected GL gl;
	protected int drawZlevel;
	protected boolean texture;
	protected Tile left, above, right, below;

	protected Tile(int tileID)
	{
		this.tileID = tileID;
	}

	protected void setTile(Level level, int lon, int lat)
	{
		this.upLeftLon = lon;
		this.upLeftLat = lat;
		this.lowRightLon = lon + level.tileSize;
		this.lowRightLat = lat + level.tileSize;
		this.levelID = level.levelID;

		this.fmx = lon;
		this.fmz = -lat;

		this.status = true;
		this.jp2 = null;
	}

	protected void processVisibility()
	{
//		status = 
//			Math3D.isPointInView(upLeftLon,     upLeftLat) &&
//			Math3D.isPointInView(lowRightLon,   upLeftLat) &&
//			Math3D.isPointInView(upLeftLon,   lowRightLat) &&
//			Math3D.isPointInView(lowRightLon, lowRightLat);
	}

	protected void display(GL gl, boolean texture, Tile rightTile, Tile belowTile, Tile leftTile, Tile aboveTile) throws Exception
	{
		if (!status) {
			return;
		}

		if(DEBUG) {
			ProfilerInterface.tileCounter++;
		}

		final Landscape landscape = Ptolemy3D.ptolemy.scene.landscape;
		final Level[] levels = landscape.levels;
		final Ptolemy3DUnit unit = Ptolemy3D.ptolemy.unit;

		this.gl = gl;
		this.texture = texture;
		this.right = rightTile;
		this.below = belowTile;
		this.left = leftTile;
		this.above = aboveTile;

		if (!texture) {
			final float[] tileColor = landscape.tileColor;
			gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
		}

		// look for tile header to match coords
		int ulx, ulz, lrx, lrz;
		ulx = upLeftLon;
		ulz = upLeftLat;
		lrx = lowRightLon;
		lrz = lowRightLat;
		{
			if (ulx < -landscape.maxLongitude) {
				ulx = -landscape.maxLongitude;
			}
			if (lrx > landscape.maxLongitude) {
				lrx = landscape.maxLongitude;
			}
			if (ulz < -landscape.maxLatitude) {
				ulz = -landscape.maxLatitude;
			}
			if (lrz > landscape.maxLatitude) {
				lrz = landscape.maxLatitude;
			}
		}

		drawZlevel = levelID;
		if ((jp2 != null) && (jp2.level != levelID)) {
			final int tileWidth = levels[jp2.level].tileSize;

			upLeftLon = jp2.lon - unit.meterX;
			upLeftLat = unit.meterZ - jp2.lat;
			lowRightLon = upLeftLon + tileWidth;
			lowRightLat = upLeftLat + tileWidth;
			drawZlevel = jp2.level;
		}
		if(DEBUG) {
			if(ProfilerInterface.forceLevel != -1 && ProfilerInterface.forceLevel != drawZlevel) {
				return;
			}
		}

		if (levelID </*!=*/ (levels.length - 1))
		{
			final int below = levelID + 1;
			final Level levelBelow = levels[below];

			if (levelBelow.visible)
			{
				int c_ulx = levelBelow.upLeftLon;
				int c_ulz = levelBelow.upLeftLat;
				int c_lrx = levelBelow.lowRightLon;
				int c_lrz = levelBelow.lowRightLat;

				if ((c_lrz < landscape.maxLatitude) && (c_lrz > -landscape.maxLatitude))
				{
					if (!((lrx <= c_ulx) || (ulx >= c_lrx) || (lrz <= c_ulz) || (ulz >= c_lrz))) {
						displayClipped(ulx, ulz, lrx, lrz, c_ulx, c_ulz, c_lrx, c_lrz);
						return;
					}

					int maxx = (landscape.maxLongitude / levelBelow.tileSize) * levelBelow.tileSize;
					if (maxx != landscape.maxLongitude) {
						maxx += levelBelow.tileSize;
					}

					if (c_lrx > maxx)
					{
						int t_past = Level.LEVEL_NUMTILE_LON - ((landscape.maxLongitude - c_ulx) / levelBelow.tileSize) - 1;
						c_ulx = -maxx;
						c_lrx = c_ulx + (t_past * levelBelow.tileSize);

						if (!((lrx <= c_ulx) || (ulx >= c_lrx) || (lrz <= c_ulz) || (ulz >= c_lrz))) {
//						if (!((lrx <=   ulx) || (ulx >= c_lrx) || (lrz <= c_ulz) || (ulz >= c_lrz))) {	//Old (replace with above line 08-2008)
							displayClipped(ulx, ulz, lrx, lrz, c_ulx, c_ulz, c_lrx, c_lrz);
							return;
						}
					}
				}
			}
		}
		renderer.drawSubsection(this, ulx, ulz, lrx, lrz);
	}
	private void displayClipped(int ulx, int ulz, int lrx, int lrz, int c_ulx, int c_ulz, int c_lrx, int c_lrz) throws Exception
	{
		boolean ul, ur, ll, lr;

		ul = inBounds(ulx, ulz, c_ulx, c_ulz, c_lrx, c_lrz);
		ur = inBounds(lrx, ulz, c_ulx, c_ulz, c_lrx, c_lrz);
		ll = inBounds(ulx, lrz, c_ulx, c_ulz, c_lrx, c_lrz);
		lr = inBounds(lrx, lrz, c_ulx, c_ulz, c_lrx, c_lrz);

		if (ul && ur && ll && lr) {
			return;
		}
		else if (ul && ur) {
			renderer.drawSubsection(this, ulx, c_lrz, lrx, lrz);
		}
		else if (ur && lr) {
			renderer.drawSubsection(this, ulx, ulz, c_ulx, lrz);
		}
		else if (ll && lr) {
			renderer.drawSubsection(this, ulx, ulz, lrx, c_ulz);
		}
		else if (ul && ll) {
			renderer.drawSubsection(this, c_lrx, ulz, lrx, lrz);
		}
		else if (ul) {
			renderer.drawSubsection(this, c_lrx, ulz, lrx, c_lrz);
			renderer.drawSubsection(this, ulx, c_lrz, lrx, lrz);
		}
		else if (ur) {
			renderer.drawSubsection(this, ulx, ulz, c_ulx, c_lrz);
			renderer.drawSubsection(this, ulx, c_lrz, lrx, lrz);
		}
		else if (ll) {
			renderer.drawSubsection(this, ulx, ulz, lrx, c_ulz);
			renderer.drawSubsection(this, c_lrx, c_ulz, lrx, lrz);
		}
		else if (lr) {
			renderer.drawSubsection(this, ulx, ulz, lrx, c_ulz);
			renderer.drawSubsection(this, ulx, c_ulz, c_ulx, lrz);
		}
		else {
			renderer.drawSubsection(this, ulx, ulz, lrx, c_ulz);
			renderer.drawSubsection(this, ulx, c_lrz, lrx, lrz);
			renderer.drawSubsection(this, ulx, c_ulz, c_ulx, c_lrz);
			renderer.drawSubsection(this, c_lrx, c_ulz, lrx, c_lrz);
		}
	}
	private final boolean inBounds(int x, int z, int c_ulx, int c_ulz, int c_lrx, int c_lrz)
	{
		if ((x >= c_ulx) && (x <= c_lrx) && (z >= c_ulz) && (z <= c_lrz)) {
			return true;
		}
		else {
			return false;
		}
	}

	/** Tile Picking */
	protected boolean pick(double[] intersectPoint, double[][] ray)
	{
		if ((jp2 == null) || ((jp2 != null) && (jp2.level != levelID))) {
			return false;
		}

		final Landscape landscape = Ptolemy3D.ptolemy.scene.landscape;
		final Ptolemy3DUnit unit = Ptolemy3D.ptolemy.unit;
		final double terrainScaler = landscape.terrainScaler;

		intersectPoint[0] = intersectPoint[1] = intersectPoint[2] = -999;

		int x1 = upLeftLon;
		int z1 = upLeftLat;
		int x2 = lowRightLon;
		int z2 = lowRightLat;

		boolean useDem, useTin;
		{
			boolean b = landscape.terrainEnabled && (jp2 != null);
			useDem = (b && (jp2.dem != null)) ? true : false;
			useTin = (b && (jp2.tin != null)) ? true : false;
		}

		double theta1, dTetaOverN;
		double phi1, dPhiOverN;
		{
			double theta2, phi2;
			double oneOverDDToRad;

			oneOverDDToRad = Math3D.degToRad / unit.DD;

			theta1 = (x1 + landscape.maxLongitude) * oneOverDDToRad;
			theta2 = (x2 + landscape.maxLongitude) * oneOverDDToRad;

			phi1 = z1 * oneOverDDToRad;
			phi2 = z2 * oneOverDDToRad;

			dTetaOverN = (theta2 - theta1);	//To be divieded by n later
			dPhiOverN  = (phi2   - phi1  );
		}

		if (!useTin && !useDem)
		{
			int x_w = (x2 - x1);
			int z_w = (z2 - z1);

			// texture ratios
			int n = (int) (((double) ((x_w > z_w) ? x_w : z_w) / (double) landscape.maxLongitude) * 50);// precision, soon to be dem...
			if (n <= 2) {
				n = 4;
			}

			{
				dTetaOverN /= n;
				dPhiOverN  /= n;
			}

			double t1 = phi1;
			for (int j = 0; j < n; j++)
			{
				final double t2 = t1 + dPhiOverN;

				final double cosT1_E =   Math.cos(t1) * EARTH_RADIUS;
				final double sinT1_E = - Math.sin(t1) * EARTH_RADIUS;
				final double cosT2_E =   Math.cos(t2) * EARTH_RADIUS;
				final double sinT2_E = - Math.sin(t2) * EARTH_RADIUS;

				double t3 = theta1;
				for (int i = 0; i <= n; i++)
				{
					double cx1, cy1, cz1, cx2, cy2, cz2;
					{
						final double cosT3 = Math.cos(t3);
						final double sinT3 = Math.sin(t3);

						cx1 =  cosT1_E * sinT3;
						cx2 =  cosT2_E * sinT3;
						cy1 =  sinT1_E;
						cy2 =  sinT2_E;
						cz1 =  cosT1_E * cosT3;
						cz2 =  cosT2_E * cosT3;
					}

					if (i > 0) {
						Math3D.setTriVert(2, cx1, cy1, cz1);
						if (Math3D.pickTri(intersectPoint, ray)) {
							return true;
						}
						Math3D.setTriVert(2, cx2, cy2, cz2);
						if (Math3D.pickTri(intersectPoint, ray)) {
							return true;
						}
					}
					else {
						Math3D.setTriVert(0, cx1, cy1, cz1);
						Math3D.setTriVert(1, cx2, cy2, cz2);
					}

					t3 += dTetaOverN;
				}
			}

		}
		else if (useTin)
		{
			{
				double d = 1.0 / jp2.tin.w;
				dTetaOverN /= d;
				dPhiOverN  /= d;
			}
			double scaler = unit.coordSystemRatio * terrainScaler;

			for (int i = 0; i < jp2.tin.nSt.length; i++)
			{
				for (int j = 0; j < jp2.tin.nSt[i].length; j++)
				{
					double dx, dz, dy;
					{
						final int v = jp2.tin.nSt[i][j];
						final float[] p = jp2.tin.p[v];

						dx = theta1 + p[0] * dTetaOverN;
						dy =          p[1] * scaler;
						dz = phi1   + p[2] * dPhiOverN;
					}


					double tx, ty, tz;
					{
						double dyE = EARTH_RADIUS + dy;
						double sinZ = Math.sin(dz);
						double cosZ = Math.cos(dz);
						double sinX = Math.sin(dx);
						double cosX = Math.cos(dx);

						tx =   dyE * cosZ * sinX;
						ty = - dyE * sinZ;
						tz =   dyE * cosZ * cosX;
					}

					// pick
					if (j < 2) {
						Math3D.setTriVert(j, tx, ty, tz);
					}
					else {
						Math3D.setTriVert(2, tx, ty, tz);
						if (Math3D.pickTri(intersectPoint, ray)) {
							return true;
						}
					}
				}
			}
		}
		else if (useDem)
		{
			final byte[] dem = jp2.dem.demDatas;

			final int row_width = (int) Math.sqrt((dem.length / 2)) * 2; // assuming we have a square tile
			final int numrows = row_width / 2;

			int startx = 0;
			int startz = 0;
			int endx = numrows - 1;
			int endz = numrows - 1;

			int nrows_x = endx - startx;
			int nrows_z = endz - startz;

			{
				dTetaOverN /= nrows_x;
				dPhiOverN  /= nrows_z;
			}

			double scaler = unit.coordSystemRatio * terrainScaler;

			double dz = phi1;
			int pos_d1 = (startz * row_width) + (startx * 2);
			for (int i = startz; i < endz; i++)
			{
				double d2z = dz + dPhiOverN;

				final double cosZ  = Math.cos(dz);
				final double cos2Z = Math.cos(d2z);
				final double sinZ  = Math.sin(dz);
				final double sin2Z = Math.sin(d2z);

				int pos_d1_save = pos_d1;	//int pos_d1 = (i * rowWidth) + (startx * 2);
				double dx = theta1;
				for (int j = startx; j < endx; j++)
				{
					int pos_d2 = pos_d1 + row_width;

					double dy1 = ((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * scaler;
					double dy2 = ((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * scaler;

					double cx1, cy1, cz1, cx2, cy2, cz2;
					{
						final double dy1E = EARTH_RADIUS + dy1;
						final double dy2E = EARTH_RADIUS + dy2;

						final double cosX = Math.cos(dx);
						final double sinX = Math.sin(dx);

						cx1 =  dy1E * cosZ  * sinX;
						cx2 =  dy2E * cos2Z * sinX;
						cy1 = -dy1E * sinZ;
						cy2 = -dy2E * sin2Z;
						cz1 =  dy1E * cosZ  * cosX;
						cz2 =  dy2E * cos2Z * cosX;
					}

					if (j > startx) {
						Math3D.setTriVert(2, cx1, cy1, cz1);
						if (Math3D.pickTri(intersectPoint, ray)) {
							return true;
						}

						Math3D.setTriVert(2, cx2, cy2, cz2);
						if (Math3D.pickTri(intersectPoint, ray)) {
							return true;
						}
					}
					else {
						Math3D.setTriVert(0, cx1, cy1, cz1);
						Math3D.setTriVert(1, cx2, cy2, cz2);
					}

					pos_d1 += 2;
					dx     += dTetaOverN;
				}

				dz += dPhiOverN;
				pos_d1 = pos_d1_save + row_width;
			}
		}
		else
		{
			Math3D.setTriVert(0, x1, 0, z1);
			Math3D.setTriVert(1, x1, 0, z2);
			Math3D.setTriVert(2, x2, 0, z1);

			if (Math3D.pickTri(intersectPoint, ray)) {
				return true;
			}
			Math3D.setTriVert(2, x2, 0, z2);
			if (Math3D.pickTri(intersectPoint, ray)) {
				return true;
			}
		}
		return false;
	}

	protected final boolean isImageDataReady(int res)
	{
		if(jp2 == null) {
			return false;
		}

		final Jp2TileRes tile = jp2.tileRes[res];
		return tile.imageDataReady;
	}
	/** @return OpenGL texture texels, in the format RGB */
	protected byte[] getCurImageData()
	{
		return jp2.getImageData(jp2.curRes);
	}
	/** @return current resolution */
	protected final int getCurRes()
	{
		return jp2.curRes;
	}
	/** @return current tile width */
	protected final int getWidth()
	{
		final Jp2TileLoader tileLoader = Ptolemy3D.ptolemy.tileLoader;
		return tileLoader.twidth[jp2.curRes];
	}
	/** @return current tile height */
	protected final int getHeight()
	{
		final Jp2TileLoader tileLoader = Ptolemy3D.ptolemy.tileLoader;
		return tileLoader.twidth[jp2.curRes];	//Square textures
	}
	/** @param textureId OpenGL texture ID */
	protected final void setCurTextureId(int textureId)
	{
		final Jp2TileRes tile = jp2.tileRes[jp2.curRes];
		tile.textureId = textureId;
	}
	/** @return OpenGL texture ID */
	protected final int getCurTextureId()
	{
		final Jp2TileRes tile = jp2.tileRes[jp2.curRes];
		return tile.textureId;
	}

	//Getters

	/** Level ID
	 * @return the level id (first is 0)*/
	public int getLevelID() { return levelID; }
	/** Upper left corner longitude
	 * @return the upper left longitude*/
	public int getUpLeftLon() { return upLeftLon; }
	/** Upper left corner latitude
	 * @return the upper left latitude*/
	public int getUpLeftLat() { return upLeftLat; }
	/** Lower right corner longitude
	 * @return the lower right longitude*/
	public int getLowRightLon() { return lowRightLon; }
	/** Lower right corner latitude
	 * @return the lower right latitude*/
	public int getLowRightLat() { return lowRightLat; }
}
