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

import org.ptolemy3d.debug.ProfilerInterface;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.tile.Tile.ITileRenderer;

//FIXME Optimize for tin

/**
 * TexTileDirectModeRenderer optimized with a pre-computed cos table.<BR>
 * <BR>
 * The precision depends on the table size.<BR>
 * With a 1.9ko table and DDBuffer of 10E6, the precision is 360 and is in degree 3.6E-4.
 * With a 0.95ko table and DDBuffer of 10E6, the precision is 720 and is in degree 7.2E-4.
 * The first  level precision is: 6 553 600
 * The second level precision is:   819 200
 * The third  level precision is:   204 800
 * The fourth level precision is:    12 800
 * @see Landscape#DD
 */
class TileDirectModeRenderer_MathLookUp extends TileDirectModeRenderer implements ITileRenderer
{
	private boolean isInit = false;

	/* Angles values */
	private int angle90, angle180, angle360;
	/* Pre-calculate cos */
	private double[] cosTable;	//Memory usage (in octets): cosTable.length * 8;

//	/* Maximum memory used by the cos table (in octets) */
//	private final static int MAX_MEMORY = 1024 * 1024;	//precision: 720
//	/* Number of level to use pre-computed values */
//	private final static int MAX_PRECOMPUTEDLEVEL = 4;
//	/* Convert input angle to index in the cosTable */
//	private float angleToIndex;
//
//	private void init(Landscape landscape) {
//		final double toRadiansOverDDBuffer = Math3D.degToRadConst / unit.DDBuffer;
//
//		//Angles in unit system
//		angle90  =  90 * unit.DDBuffer;
//		angle180 = 180 * unit.DDBuffer;
//		angle360 = 360 * unit.DDBuffer;
//
//		//Calculate the number of entries in the cosinus table
//		int numEntry = angle90;
//		while(numEntry > MAX_MEMORY / 8 || (angle90 % numEntry != 0)) {
//			numEntry--;
//		}
//
//		//Fill the cos table
//		cosTable = new double[numEntry+1];
//		for(int i=0, index=0, incr = angle90 / numEntry; i <= angle90; i+=incr, index++) {
//			double angle = i * toRadiansOverDDBuffer;
//			cosTable[index] = Math.cos(angle);
//		}
//
//		angleToIndex = (float) numEntry / angle90;		//No error here, it is effectively numEntry (but not numEntry+1)
//
//		isInit = true;
//	}
//
//	private final double lookUpCos(int angle)
//	{
//		if(angle < 0) {
//			angle = -angle;
//		}
//		/* To reduce the table size, we must fit to [0°;90°].
//		 * A little time is consumed here, but it reduced a lot the cos table. */
//		if(angle >= angle180) {
//			angle = angle360-angle;
//		}
//		if(angle >= angle90) {
//			return -cosTable[(int)((angle180-angle) * angleToIndex)];
//		}
//		else {
//			return cosTable[(int)(angle * angleToIndex)];
//		}
//	}

	//Settings: MaxLevels=4, MaxPrecision=3200, MemUsage=215ko
	//Settings: MaxLevels=5, MaxPrecision=1600, MemUsage=430ko

	/* Angle precision */
	private final static int MAX_PRECISION = 1600;
	/* Number of level to use pre-computed values */
	private final static int MAX_PRECOMPUTEDLEVEL = 5;

	private void init(Landscape landscape) {
		final double toRadiansOverDDBuffer = Math3D.degToRad / unit.DD;

		//Angles in unit system
		angle90  =  90 * unit.DD;
		angle180 = 180 * unit.DD;
		angle360 = 360 * unit.DD;

		//Calculate the number of entries in the cosinus table
		int numEntry = angle90 / MAX_PRECISION;

		//Fill the cos table
		cosTable = new double[numEntry+1];
		for(int i=0, index=0, incr = MAX_PRECISION; i <= angle90; i+=incr, index++) {
			double angle = i * toRadiansOverDDBuffer;
			cosTable[index] = Math.cos(angle);
		}

		isInit = true;
	}

	private final double lookUpCos(int angle)
	{
		if(angle < 0) {
			angle = -angle;
		}
		/* To reduce the table size, we must fit to [0°;90°].
		 * A little time is consumed here, but it reduced a lot the cos table. */
		if(angle >= angle180) {
			angle = angle360-angle;
		}
		if(angle >= angle90) {
//			if(DEBUG) {
//				if(((angle180-angle) % MAX_PRECISION) != 0) {
//					IO.printfRenderer("Loose of precision (%d)\n", angle180-angle);
//				}
//			}
			return -cosTable[(angle180-angle) / MAX_PRECISION];
		}
		else {
//			if(DEBUG) {
//				if((angle % MAX_PRECISION) != 0) {
//					IO.printfRenderer("Loose of precision (%d)\n", angle);
//				}
//			}
			return cosTable[angle / MAX_PRECISION];
		}
	}

	//************************************************************

//	/**
//	 * Convert a longitude/latitude into a (x,y,z) position.
//	 * Note: Be awared that lon have previously be added with 180°.
//	 * @param lon longitude
//	 * @param lat latitude
//	 * @param dst destination coordinates
//	 */
//	public final void setSphericalCoord(int lon, int lat, double[] dst)
//	{
//		final double toRadiansOverDDBuffer = Math3D.degToRadConst / unit.DDBuffer;
//		double thx = lon * toRadiansOverDDBuffer;
//		double thy = lat * toRadiansOverDDBuffer;
//
//		double cosX = Math.cos(thx);
//		double sinX = Math.sin(thx);
//		double cosY = Math.cos(thy);
//		double sinY = - Math.sin(thy);
//
//		dst[0] = cosY * sinX * EARTH_RADIUS;
//		dst[1] = sinY        * EARTH_RADIUS;
//		dst[2] = cosY * cosX * EARTH_RADIUS;
//	}

	//************************************************************

	public TileDirectModeRenderer_MathLookUp(){}

	protected void fillLocalVariables(Tile tile)
	{
		super.fillLocalVariables(tile);

		if(!isInit) {
			init(landscape);
		}
	}

	protected void drawDemSubsection_Textured(int xStart, int zStart, int xEnd, int zEnd)
	{
		if(drawZlevel >= MAX_PRECOMPUTEDLEVEL) {
			super.drawDemSubsection_Textured(xStart, zStart, xEnd, zEnd);
			return;
		}

		final Level drawLevel = landscape.levels[drawZlevel];
		final byte[] dem = jtile.dem.demDatas;
		final int numRows = jtile.dem.numRows;

		final int rowWidth = numRows * 2;	// assuming we have a square tile
		final float tex_inc = 1.0f / (numRows - 1);

		final boolean xsinterpolate, xeinterpolate, zsinterpolate, zeinterpolate;
		final double startxcoord, startzcoord, endxcoord, endzcoord;
		final int startx, startz; /*final*/ int endx, endz;
		{
			final double geom_inc = (double) (numRows - 1) / drawLevel.tileSize;

			startxcoord = ((xStart - upLeftX) * geom_inc);
			startx = (int) startxcoord;
			xsinterpolate = (startx != startxcoord);

			startzcoord = ((zStart - upLeftZ) * geom_inc);
			startz = (int) startzcoord;
			zsinterpolate = (startz != startzcoord);

			endxcoord = ((xEnd - upLeftX) * geom_inc) + 1;
			endx = (int) endxcoord;
			xeinterpolate = (endxcoord != endx);
			if (xeinterpolate) { endx++; }

			endzcoord = ((zEnd - upLeftZ) * geom_inc);
			endz = (int) endzcoord;
			zeinterpolate = (endzcoord != endz);
			if (zeinterpolate) { endz++; }
		}

		final double oneOverNrowsX = 1.0 / (endx - startx - 1);
		final double oneOverNrowsZ = 1.0 / (endz - startz);

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawZlevel == ZLevel);
		if (eqZLevel && (xStart == upLeftX) && (xEnd == lowRightX) && (zStart == upLeftZ) && (zEnd == lowRightZ)) {
			final int rowWidthMinusOne = rowWidth - 1;
			final int rowWidthMinusTwo = rowWidth - 2;
			final int i1 = rowWidth * (numRows - 1);

			ul_corner = (dem[     0               ] << 8) + (dem[     1               ] & 0xFF);
			ur_corner = (dem[     rowWidthMinusTwo] << 8) + (dem[     rowWidthMinusOne] & 0xFF);
			ll_corner = (dem[i1                   ] << 8) + (dem[i1 + 1               ] & 0xFF);
			lr_corner = (dem[i1 + rowWidthMinusTwo] << 8) + (dem[i1 + rowWidthMinusOne] & 0xFF);

			if ((leftTile == null) || (leftTile.jp2 == null) || (leftTile.jp2.level != drawZlevel)) {
				left_dem_slope   = (ll_corner - ul_corner) * oneOverNrowsZ;
			}
			if ((rightTile == null) || (rightTile.jp2 == null) || (rightTile.jp2.level != drawZlevel)) {
				right_dem_slope  = (lr_corner - ur_corner) * oneOverNrowsZ;
			}
			if ((aboveTile == null) || (aboveTile.jp2 == null) || (aboveTile.jp2.level != drawZlevel)) {
				top_dem_slope    = (ur_corner - ul_corner) * oneOverNrowsX;
			}
			if ((belowTile == null) || (belowTile.jp2 == null) || (belowTile.jp2.level != drawZlevel)) {
				bottom_dem_slope = (lr_corner - ll_corner) * oneOverNrowsX;
			}
		}

		xStart += landscape.maxLongitude;
		xEnd   += landscape.maxLongitude;

		int dx = (xEnd - xStart) / (endx - startx - 1);
		int dz = (zEnd - zStart) / (endz - startz);

		final double dyScaler = unit.coordSystemRatio * terrainScaler;

		int lat = zStart;
		double cosZ = lookUpCos(lat);
		double sinZ = lookUpCos(angle90-lat);

		int pos_d1 = (startz * rowWidth) + (startx * 2);
		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int i = startz; i < endz; i++)	//startz can be equal to (endz-1)
		{
			final int lat2 = lat + dz;

			double cos2Z = lookUpCos(lat2);
			double sin2Z = lookUpCos(angle90-lat2);

			final float tex_z, tex_z2;
			final double t_sz_wt, t_ez_wt, b_sz_wt, b_ez_wt;
			if ((i == startz) && (zsinterpolate)) {
				tex_z = (float)(tex_inc * startzcoord);
				t_ez_wt = startzcoord - startz;
				t_sz_wt = 1.0 - t_ez_wt;
			}
			else {
				tex_z = tex_inc * i;
				t_ez_wt = 0;
				t_sz_wt = 1;
			}
			if ((i == (endz - 1)) && (zeinterpolate)) {
				tex_z2 = (float)(tex_inc * endzcoord);
				b_sz_wt = endz - endzcoord;
				b_ez_wt = 1.0 - b_sz_wt;
			}
			else {
				tex_z2 = tex_inc * (i + 1);
				b_sz_wt = 0;
				b_ez_wt = 1;
			}

			//gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int pos_d1_save = pos_d1;	//int pos_d1 = (i * rowWidth) + (startx * 2);
			int lon = xStart;
			for (int j = startx; j < endx; j++)
			{
				final int pos_d2 = pos_d1 + rowWidth;

				final float tex_x;
				final double sx_wt, ex_wt;
				final int pos_r1, pos_r2;
				if (xsinterpolate && (j == startx)) {
					ex_wt = startxcoord - startx;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((startx + 1) * 2);
					pos_r2 = pos_r1 + rowWidth;

					tex_x = (float)(tex_inc * startxcoord);
				}
				else if (xeinterpolate && (j == (endx - 1))) {
					ex_wt = endx - endxcoord;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((j - 1) * 2);
					pos_r2 = pos_r1 + rowWidth;

					tex_x = (float)(tex_inc * (endxcoord - 1));
				}
				else {
					ex_wt = 0.0;
					sx_wt = 1;

					pos_r1 = pos_d1;
					pos_r2 = pos_d2;

					tex_x = j * tex_inc;
				}

				double dy1, dy2;
				if (eqZLevel) {
					if ((j == startx) && (left_dem_slope != -1)) {
						dy1 = ul_corner + (i * left_dem_slope);
						dy2 = dy1 + left_dem_slope;
					}
					else if ((j == (endx - 1)) && (right_dem_slope != -1)) {
						dy1 = ur_corner + (i * right_dem_slope);
						dy2 = dy1 + right_dem_slope;
					}
					else if ((i == startz) && (top_dem_slope != -1)) {
						dy1 = ul_corner + (j * top_dem_slope);
						dy2 = (dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF);
					}
					else if ((i == (endz - 1)) && (bottom_dem_slope != -1)) {
						dy1 = (dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF);
						dy2 = ll_corner + (j * bottom_dem_slope);
					}
					else {
						dy1 = (dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF);
						dy2 = (dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF);
					}
				}
				else {
					double f1 = ((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * sx_wt;
					double f2 = ((dem[pos_r1] << 8) + (dem[pos_r1 + 1] & 0xFF)) * ex_wt;
					double f3 = ((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * sx_wt;
					double f4 = ((dem[pos_r2] << 8) + (dem[pos_r2 + 1] & 0xFF)) * ex_wt;

					dy1 = (f1 * t_sz_wt) + (f2 * t_sz_wt) + (f3 * t_ez_wt) + (f4 * t_ez_wt);
					dy2 = (f1 * b_sz_wt) + (f2 * b_sz_wt) + (f3 * b_ez_wt) + (f4 * b_ez_wt);
				}

				double cx1, cy1, cz1, cx2, cy2, cz2;
				{
					final double dy1E = (dy1 * dyScaler) + EARTH_RADIUS;
					final double dy2E = (dy2 * dyScaler) + EARTH_RADIUS;

					final double cosX = lookUpCos(lon);
					final double sinX = lookUpCos(angle90-lon);

					cx1 =  dy1E * cosZ  * sinX;
					cx2 =  dy2E * cos2Z * sinX;
					cy1 = -dy1E * sinZ;
					cy2 = -dy2E * sin2Z;
					cz1 =  dy1E * cosZ  * cosX;
					cz2 =  dy2E * cos2Z * cosX;
				}

				gl.glTexCoord2f(tex_x, tex_z);
				gl.glVertex3d(cx1, cy1, cz1);

				gl.glTexCoord2f(tex_x, tex_z2);
				gl.glVertex3d(cx2, cy2, cz2);

				pos_d1 += 2;
				lon    += dx;
			}
			//gl.glEnd();

			lat = lat2; cosZ = cos2Z; sinZ = sin2Z;
			pos_d1 = pos_d1_save + rowWidth;
		}
		gl.glEnd();

		if(DEBUG) {
			int numVertices = 2 * (endx-startx) * (endz-startz);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}
	protected void drawDemSubsection(int xStart, int zStart, int xEnd, int zEnd)
	{
		if(drawZlevel >= MAX_PRECOMPUTEDLEVEL) {
			super.drawDemSubsection(xStart, zStart, xEnd, zEnd);
			return;
		}

		final Level drawLevel = landscape.levels[drawZlevel];
		final byte[] dem = jtile.dem.demDatas;
		final int numRows = jtile.dem.numRows;
		final boolean useColor = (landscape.displayMode == Landscape.DISPLAY_SHADEDDEM);

		final int rowWidth = numRows * 2;	// assuming we have a square tile

		final boolean xsinterpolate, xeinterpolate, zsinterpolate, zeinterpolate;
		final double startxcoord, startzcoord, endxcoord, endzcoord;
		final int startx, startz; /*final*/ int endx, endz;
		{
			final double geom_inc = (double) (numRows - 1) / drawLevel.tileSize;

			startxcoord = ((xStart - upLeftX) * geom_inc);
			startx = (int) startxcoord;
			xsinterpolate = (startx != startxcoord);

			startzcoord = ((zStart - upLeftZ) * geom_inc);
			startz = (int) startzcoord;
			zsinterpolate = (startz != startzcoord);

			endxcoord = ((xEnd - upLeftX) * geom_inc) + 1;
			endx = (int) endxcoord;
			xeinterpolate = (endxcoord != endx);
			if (xeinterpolate) { endx++; }

			endzcoord = ((zEnd - upLeftZ) * geom_inc);
			endz = (int) endzcoord;
			zeinterpolate = (endzcoord != endz);
			if (zeinterpolate) { endz++; }
		}

		final double oneOverNrowsX = 1.0 / (endx - startx - 1);
		final double oneOverNrowsZ = 1.0 / (endz - startz);

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawZlevel == ZLevel);
		if (eqZLevel && (xStart == upLeftX) && (xEnd == lowRightX) && (zStart == upLeftZ) && (zEnd == lowRightZ)) {
			final int rowWidthMinusOne = rowWidth - 1;
			final int rowWidthMinusTwo = rowWidth - 2;
			final int i1 = rowWidth * (numRows - 1);

			ul_corner = (dem[     0               ] << 8) + (dem[     1               ] & 0xFF);
			ur_corner = (dem[     rowWidthMinusTwo] << 8) + (dem[     rowWidthMinusOne] & 0xFF);
			ll_corner = (dem[i1                   ] << 8) + (dem[i1 + 1               ] & 0xFF);
			lr_corner = (dem[i1 + rowWidthMinusTwo] << 8) + (dem[i1 + rowWidthMinusOne] & 0xFF);

			if ((leftTile == null) || (leftTile.jp2 == null) || (leftTile.jp2.level != drawZlevel)) {
				left_dem_slope   = (ll_corner - ul_corner) * oneOverNrowsZ;
			}
			if ((rightTile == null) || (rightTile.jp2 == null) || (rightTile.jp2.level != drawZlevel)) {
				right_dem_slope  = (lr_corner - ur_corner) * oneOverNrowsZ;
			}
			if ((aboveTile == null) || (aboveTile.jp2 == null) || (aboveTile.jp2.level != drawZlevel)) {
				top_dem_slope    = (ur_corner - ul_corner) * oneOverNrowsX;
			}
			if ((belowTile == null) || (belowTile.jp2 == null) || (belowTile.jp2.level != drawZlevel)) {
				bottom_dem_slope = (lr_corner - ll_corner) * oneOverNrowsX;
			}
		}

		xStart += landscape.maxLongitude;
		xEnd   += landscape.maxLongitude;

		int dx = (xEnd - xStart) / (endx - startx - 1);
		int dz = (zEnd - zStart) / (endz - startz);

		final double dyScaler = unit.coordSystemRatio * terrainScaler;

		int lat = zStart;
		double cosZ = lookUpCos(lat);
		double sinZ = lookUpCos(angle90-lat);

		int pos_d1 = (startz * rowWidth) + (startx * 2);
		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int i = startz; i < endz; i++)	//startz can be equal to (endz-1)
		{
			final int lat2 = lat + dz;

			double cos2Z = lookUpCos(lat2);
			double sin2Z = lookUpCos(angle90-lat2);

			final double t_sz_wt, t_ez_wt, b_sz_wt, b_ez_wt;
			if ((i == startz) && (zsinterpolate)) {
				t_ez_wt = startzcoord - startz;
				t_sz_wt = 1.0 - t_ez_wt;
			}
			else {
				t_ez_wt = 0;
				t_sz_wt = 1;
			}
			if ((i == (endz - 1)) && (zeinterpolate)) {
				b_sz_wt = endz - endzcoord;
				b_ez_wt = 1.0 - b_sz_wt;
			}
			else {
				b_sz_wt = 0;
				b_ez_wt = 1;
			}

			//gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int pos_d1_save = pos_d1;	//int pos_d1 = (i * rowWidth) + (startx * 2);
			int lon = xStart;
			for (int j = startx; j < endx; j++)
			{
				final int pos_d2 = pos_d1 + rowWidth;

				final double sx_wt, ex_wt;
				final int pos_r1, pos_r2;
				if (xsinterpolate && (j == startx)) {
					ex_wt = startxcoord - startx;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((startx + 1) * 2);
					pos_r2 = pos_r1 + rowWidth;
				}
				else if (xeinterpolate && (j == (endx - 1))) {
					ex_wt = endx - endxcoord;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((j - 1) * 2);
					pos_r2 = pos_r1 + rowWidth;
				}
				else {
					ex_wt = 0.0;
					sx_wt = 1;

					pos_r1 = pos_d1;
					pos_r2 = pos_d2;
				}

				double dy1, dy2;
				if (eqZLevel) {
					if ((j == startx) && (left_dem_slope != -1)) {
						dy1 = ul_corner + (i * left_dem_slope);
						dy2 = dy1 + left_dem_slope;
					}
					else if ((j == (endx - 1)) && (right_dem_slope != -1)) {
						dy1 = ur_corner + (i * right_dem_slope);
						dy2 = dy1 + right_dem_slope;
					}
					else if ((i == startz) && (top_dem_slope != -1)) {
						dy1 = ul_corner + (j * top_dem_slope);
						dy2 = (dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF);
					}
					else if ((i == (endz - 1)) && (bottom_dem_slope != -1)) {
						dy1 = (dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF);
						dy2 = ll_corner + (j * bottom_dem_slope);
					}
					else {
						dy1 = (dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF);
						dy2 = (dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF);
					}
				}
				else {
					double f1 = ((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * sx_wt;
					double f2 = ((dem[pos_r1] << 8) + (dem[pos_r1 + 1] & 0xFF)) * ex_wt;
					double f3 = ((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * sx_wt;
					double f4 = ((dem[pos_r2] << 8) + (dem[pos_r2 + 1] & 0xFF)) * ex_wt;

					dy1 = (f1 * t_sz_wt) + (f2 * t_sz_wt) + (f3 * t_ez_wt) + (f4 * t_ez_wt);
					dy2 = (f1 * b_sz_wt) + (f2 * b_sz_wt) + (f3 * b_ez_wt) + (f4 * b_ez_wt);
				}

				dy1 *= dyScaler;	//dy1 used later
				dy2 *= dyScaler;	//dy2 used later

				double cx1, cy1, cz1, cx2, cy2, cz2;
				{
					final double dy1E = dy1 + EARTH_RADIUS;
					final double dy2E = dy2 + EARTH_RADIUS;

					final double cosX = lookUpCos(lon);
					final double sinX = lookUpCos(angle90-lon);

					cx1 =  dy1E * cosZ  * sinX;
					cx2 =  dy2E * cos2Z * sinX;
					cy1 = -dy1E * sinZ;
					cy2 = -dy2E * sin2Z;
					cz1 =  dy1E * cosZ  * cosX;
					cz2 =  dy2E * cos2Z * cosX;
				}

				if (useColor) {
					setColor((float) dy1);
				}
				gl.glVertex3d(cx1, cy1, cz1);

				if (useColor) {
					setColor((float) dy2);
				}
				gl.glVertex3d(cx2, cy2, cz2);

				pos_d1 += 2;
				lon    += dx;
			}
			//gl.glEnd();

			lat = lat2; cosZ = cos2Z; sinZ = sin2Z;
			pos_d1 = pos_d1_save + rowWidth;
		}
		gl.glEnd();

		if(DEBUG) {
			int numVertices = 2 * (endx-startx) * (endz-startz);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}

	protected final void drawSubsection_Textured(int xStart, int zStart, int xEnd, int zEnd)
	{
		if(drawZlevel >= MAX_PRECOMPUTEDLEVEL) {
			super.drawSubsection_Textured(xStart, zStart, xEnd, zEnd);
			return;
		}

		final Level drawLevel = landscape.levels[drawZlevel];

		int sizeX = (xEnd - xStart);
		int sizeZ = (zEnd - zStart);

		// Precision
		int n = (int) (((double) 50 * ((sizeX > sizeZ) ? sizeX : sizeZ) / landscape.maxLongitude));
		if (n <= 2) {
			n = 4;
		}

		// Texture coordinates
		float txStart, tzStart;
		final float txIncr, tzIncr;
		{
			final float oneOverTileWidth = 1.0f / drawLevel.tileSize;
			final float oneOverTileWidthOverN = oneOverTileWidth / n;

			txStart = (xStart - upLeftX) * oneOverTileWidth;
			tzStart = (zStart - upLeftZ) * oneOverTileWidth;

			txIncr = sizeX * oneOverTileWidthOverN;
			tzIncr = sizeZ * oneOverTileWidthOverN;
		}

		// Vertex position
		xStart += landscape.maxLongitude;
		xEnd   += landscape.maxLongitude;
		final int xIncr = sizeX / n;
		final int zIncr = sizeZ / n;

		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		int lat1 = zStart;
		double cosY1 = lookUpCos(lat1) * EARTH_RADIUS;
		double sinY1 = -lookUpCos(angle90-lat1) * EARTH_RADIUS;
		float ty1 = tzStart;
		for (int j = 0; j < n; j++)
		{
			final int lat2 = lat1 + zIncr;
			double cosY2 = lookUpCos(lat2) * EARTH_RADIUS;
			double sinY2 = -lookUpCos(angle90-lat2) * EARTH_RADIUS;

			final float ty2 = ty1 + tzIncr;

			int lon = xStart;
			float tx = txStart;

			//gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int i = 0; i <= n; i++)
			{
				//setSphericalCoord(lon, lat1, dst1);
				//setSphericalCoord(lon, lat2, dst2);

				double cosX = lookUpCos(lon);
				double sinX = lookUpCos(angle90-lon);

				double pt1X, pt1Y, pt1Z;
				pt1X = cosY1 * sinX;
				pt1Y = sinY1;
				pt1Z = cosY1 * cosX;

				double pt2X, pt2Y, pt2Z;
				pt2X = cosY2 * sinX;
				pt2Y = sinY2;
				pt2Z = cosY2 * cosX;

				gl.glTexCoord2f(tx, ty1);
				gl.glVertex3d(pt1X, pt1Y, pt1Z);

				gl.glTexCoord2f(tx, ty2);
				gl.glVertex3d(pt2X, pt2Y, pt2Z);

				lon += xIncr;
				tx += txIncr;
			}
			//gl.glEnd();

			lat1 = lat2;
			cosY1 = cosY2;
			sinY1 = sinY2;

			ty1 += tzIncr;
		}
		gl.glEnd();

		if(DEBUG) ProfilerInterface.vertexCounter += 2*n*(n+1);
	}
	protected final void drawSubsection(int xStart, int zStart, int xEnd, int zEnd)
	{
		if(drawZlevel >= MAX_PRECOMPUTEDLEVEL) {
			super.drawSubsection(xStart, zStart, xEnd, zEnd);
			return;
		}

		int sizeX = (xEnd - xStart);
		int sizeZ = (zEnd - zStart);

		// Precision
		int n = (int) (((double) 50 * ((sizeX > sizeZ) ? sizeX : sizeZ) / landscape.maxLongitude));
		if (n <= 2) {
			n = 4;
		}

		// Vertex position
		xStart += landscape.maxLongitude;
		xEnd   += landscape.maxLongitude;
		final int xIncr = sizeX / n;
		final int zIncr = sizeZ / n;

		final boolean useColor = (landscape.displayMode == Landscape.DISPLAY_SHADEDDEM);
		if(useColor) gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);

		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		int lat1 = zStart;
		double cosY1 = lookUpCos(lat1) * EARTH_RADIUS;
		double sinY1 = -lookUpCos(angle90-lat1) * EARTH_RADIUS;
		for (int j = 0; j < n; j++)
		{
			final int lat2 = lat1 + zIncr;
			double cosY2 = lookUpCos(lat2) * EARTH_RADIUS;
			double sinY2 = -lookUpCos(angle90-lat2) * EARTH_RADIUS;

			int lon = xStart;

			//gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int i = 0; i <= n; i++)
			{
				//setSphericalCoord(lon, lat1, dst1);
				//setSphericalCoord(lon, lat2, dst2);

				double cosX = lookUpCos(lon);
				double sinX = lookUpCos(angle90-lon);

				double pt1X, pt1Y, pt1Z;
				pt1X = cosY1 * sinX;
				pt1Y = sinY1;
				pt1Z = cosY1 * cosX;

				double pt2X, pt2Y, pt2Z;
				pt2X = cosY2 * sinX;
				pt2Y = sinY2;
				pt2Z = cosY2 * cosX;

				gl.glVertex3d(pt1X, pt1Y, pt1Z);

				gl.glVertex3d(pt2X, pt2Y, pt2Z);

				lon += xIncr;
			}
			//gl.glEnd();

			lat1 = lat2;
			cosY1 = cosY2;
			sinY1 = sinY2;
		}
		gl.glEnd();
//		gl.glColor3f(1, 1, 1);	//This should be done somewhere

		if(DEBUG) ProfilerInterface.vertexCounter += 2*n*(n+1);
	}
}
