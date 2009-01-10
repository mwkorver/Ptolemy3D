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
import org.ptolemy3d.tile.Tile.ITileRenderer;

class TileDirectModeRenderer implements ITileRenderer
{
	/* All of that are temporary datas that are used in drawSubsection.
	 * They must be restore in each entering */
	protected GL gl;
	protected Jp2Tile jtile;

	//From the Tile
	protected int drawZlevel;
	protected Tile leftTile;
	protected Tile aboveTile;
	protected Tile rightTile;
	protected Tile belowTile;
	protected int ZLevel;
	protected int upLeftX ,  upLeftZ;
	protected int lowRightX, lowRightZ;

	protected Ptolemy3DUnit unit;
	protected Landscape landscape;
	protected float[] tileColor;
	protected float[] colratios;
	protected double terrainScaler;

	protected void fillLocalVariables(Tile tile)
	{
		gl = tile.gl;
		jtile = tile.jp2;

		drawZlevel = tile.drawZlevel;
		leftTile = tile.left;
		aboveTile = tile.above;
		rightTile = tile.right;
		belowTile = tile.below;
		ZLevel = tile.levelID;
		upLeftX = tile.upLeftLon;
		upLeftZ = tile.upLeftLat;
		lowRightX = tile.lowRightLon;
		lowRightZ = tile.lowRightLat;

		unit = Ptolemy3D.ptolemy.unit;
		landscape = Ptolemy3D.ptolemy.scene.landscape;
		tileColor = landscape.tileColor;
		colratios = landscape.colorRatios;
		terrainScaler = landscape.terrainScaler;
	}

	public void drawSubsection(Tile tile, int x1, int z1, int x2, int z2)
	{
		if ((x1 == x2) || (z1 == z2)) {
			return;
		}

		if(DEBUG) {
			if(!ProfilerInterface.renderTiles) {
				return;
			}
			ProfilerInterface.tileSectionCounter++;
		}
		fillLocalVariables(tile);

		boolean lookForElevation = (jtile != null) && (landscape.terrainEnabled);
		boolean useDem = lookForElevation && (jtile.dem != null);
		boolean useTin = lookForElevation && (jtile.tin != null);

		boolean texture = loadGLState(tile.texture);
		if(texture) {
			if(useDem) {
				drawDemSubsection_Textured(x1, z1, x2, z2);
			}
			else if(useTin) {
				drawTinSubsection(true, x1, z1, x2, z2);
			}
			else {
				drawSubsection_Textured(x1, z1, x2, z2);
			}
		}
		else {
			if(useDem) {
				drawDemSubsection(x1, z1, x2, z2);
			}
			else if(useTin) {
				drawTinSubsection(false, x1, z1, x2, z2);
			}
			else {
				drawSubsection(x1, z1, x2, z2);
			}
		}
	}

	protected void drawDemSubsection_Textured(int x1, int z1, int x2, int z2)
	{
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

			startxcoord = ((x1 - upLeftX) * geom_inc);
			startx = (int) startxcoord;
			xsinterpolate = (startx != startxcoord);

			startzcoord = ((z1 - upLeftZ) * geom_inc);
			startz = (int) startzcoord;
			zsinterpolate = (startz != startzcoord);

			endxcoord = ((x2 - upLeftX) * geom_inc) + 1;
			endx = (int) endxcoord;
			xeinterpolate = (endxcoord != endx);
			if (xeinterpolate) { endx++; }

			endzcoord = ((z2 - upLeftZ) * geom_inc);
			endz = (int) endzcoord;
			zeinterpolate = (endzcoord != endz);
			if (zeinterpolate) { endz++; }
		}

		final double oneOverNrowsX = 1.0 / (endx - startx - 1);
		final double oneOverNrowsZ = 1.0 / (endz - startz);

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawZlevel == ZLevel);
		if (eqZLevel && (x1 == upLeftX) && (x2 == lowRightX) && (z1 == upLeftZ) && (z2 == lowRightZ)) {
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

			dTetaOverN = (theta2 - theta1) * oneOverNrowsX;
			dPhiOverN  = (phi2   - phi1  ) * oneOverNrowsZ;
		}

		final double dyScaler = unit.coordSystemRatio * terrainScaler;

		double dz = phi1;
		double cosZ = Math.cos(dz);
		double sinZ = Math.sin(dz);
		double cos2Z, sin2Z;

		int pos_d1 = (startz * rowWidth) + (startx * 2);
		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int i = startz; i < endz; i++)	//startz can be equal to (endz-1)
		{
			final double d2z = dz + dPhiOverN;

			cos2Z = Math.cos(d2z);
			sin2Z = Math.sin(d2z);

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
			double dx = theta1;
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

					final double cosX = Math.cos(dx);
					final double sinX = Math.sin(dx);

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
				dx     += dTetaOverN;
			}
			//gl.glEnd();

			dz = d2z; cosZ = cos2Z; sinZ = sin2Z;
			pos_d1 = pos_d1_save + rowWidth;
		}
		gl.glEnd();

		if(DEBUG) {
			int numVertices = 2 * (endx-startx) * (endz-startz);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}
	protected void drawDemSubsection(int x1, int z1, int x2, int z2)
	{
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

			startxcoord = ((x1 - upLeftX) * geom_inc);
			startx = (int) startxcoord;
			xsinterpolate = (startx != startxcoord);

			startzcoord = ((z1 - upLeftZ) * geom_inc);
			startz = (int) startzcoord;
			zsinterpolate = (startz != startzcoord);

			endxcoord = ((x2 - upLeftX) * geom_inc) + 1;
			endx = (int) endxcoord;
			xeinterpolate = (endxcoord != endx);
			if (xeinterpolate) { endx++; }

			endzcoord = ((z2 - upLeftZ) * geom_inc);
			endz = (int) endzcoord;
			zeinterpolate = (endzcoord != endz);
			if (zeinterpolate) { endz++; }
		}

		final double oneOverNrowsX = 1.0 / (endx - startx - 1);
		final double oneOverNrowsZ = 1.0 / (endz - startz);

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawZlevel == ZLevel);
		if (eqZLevel && (x1 == upLeftX) && (x2 == lowRightX) && (z1 == upLeftZ) && (z2 == lowRightZ)) {
			final int rowWidthMinusOne = rowWidth - 1;
			final int rowWidthMinusTwo = rowWidth - 2;
			final int i1 = rowWidth * (numRows - 1);

			ul_corner = (dem[0                  ] <<8) + (dem[   1               ] & 0xFF);
			ur_corner = (dem[   rowWidthMinusTwo] <<8) + (dem[   rowWidthMinusOne] & 0xFF);
			ll_corner = (dem[i1                 ] <<8) + (dem[i1+1               ] & 0xFF);
			lr_corner = (dem[i1+rowWidthMinusTwo] <<8) + (dem[i1+rowWidthMinusOne] & 0xFF);

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

			dTetaOverN = (theta2 - theta1) * oneOverNrowsX;
			dPhiOverN  = (phi2   - phi1  ) * oneOverNrowsZ;
		}

		final double dyScaler = unit.coordSystemRatio * terrainScaler;

		double dz = phi1;
		double cosZ = Math.cos(dz);
		double sinZ = Math.sin(dz);
		double cos2Z, sin2Z;

		int pos_d1 = (startz * rowWidth) + (startx * 2);
		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int i = startz; i < endz; i++)
		{
			final double d2z = dz + dPhiOverN;

			cos2Z = Math.cos(d2z);
			sin2Z = Math.sin(d2z);

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
			double dx = theta1;
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

					final double cosX = Math.cos(dx);
					final double sinX = Math.sin(dx);

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
				dx     += dTetaOverN;
			}
			//gl.glEnd();

			dz = d2z; cosZ = cos2Z; sinZ = sin2Z;
			pos_d1 = pos_d1_save + rowWidth;
		}
		gl.glEnd();

		if(DEBUG) {
			int numVertices = 2 * (endx-startx) * (endz-startz);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * ((useColor ? 3 * 4 : 0) + 3 * 8);
		}
	}

	protected void drawSubsection_Textured(int x1, int z1, int x2, int z2)
	{
		final Level drawLevel = landscape.levels[drawZlevel];
		final float oneOverTileWidth = 1.0f / drawLevel.tileSize;

		int x_w = (x2 - x1);
		int z_w = (z2 - z1);

		// texture ratios
		float tx_start = (x1 - upLeftX) * oneOverTileWidth;
		float tz_start = (z1 - upLeftZ) * oneOverTileWidth;

		// precision
		int n = (int) (((double) ((x_w > z_w) ? x_w : z_w) / (double) landscape.maxLongitude) * 50);
		if (n <= 2) {
			n = 4;
		}

		double oneOverN = 1.0 / n;

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

			dTetaOverN = (theta2 - theta1) * oneOverN;
			dPhiOverN  = (phi2   - phi1)   * oneOverN;
		}

		float tx_w, tz_w;
		{
			float oneOverTileWidthOverN = oneOverTileWidth * (float)oneOverN;
			tx_w = x_w * oneOverTileWidthOverN;
			tz_w = z_w * oneOverTileWidthOverN;
		}

		double t1 = phi1;
		double cosT2_E, sinT2_E;
		double cosT1_E =   Math.cos(t1) * EARTH_RADIUS;
		double sinT1_E = - Math.sin(t1) * EARTH_RADIUS;

		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		float ty1 = tz_start;
		for (int j = 0; j < n; j++)
		{
			final double t2 = t1 + dPhiOverN;
			final float ty2 = ty1 + tz_w;

			cosT2_E =   Math.cos(t2) * EARTH_RADIUS;
			sinT2_E = - Math.sin(t2) * EARTH_RADIUS;

			//gl.glBegin(GL.GL_TRIANGLE_STRIP);
			double t3 = theta1;
			float tx  = tx_start;
			for (int i = 0; i <= n; i++)
			{
				double cx1, cy1, cz1, cx2, cy2, cz2;
				{
					final double cosT3 = Math.cos(t3);
					final double sinT3 = Math.sin(t3);

					cx1 =  cosT1_E * sinT3;
					cy1 =  sinT1_E;
					cz1 =  cosT1_E * cosT3;

					cx2 =  cosT2_E * sinT3;
					cy2 =  sinT2_E;
					cz2 =  cosT2_E * cosT3;
				}

				gl.glTexCoord2f(tx, ty1);
				gl.glVertex3d(cx1, cy1, cz1);

				gl.glTexCoord2f(tx, ty2);
				gl.glVertex3d(cx2, cy2, cz2);

				t3 += dTetaOverN;
				tx += tx_w;
			}
			//gl.glEnd();

			t1  = t2;
			ty1 = ty2;
			cosT1_E = cosT2_E;
			sinT1_E = sinT2_E;
		}
		gl.glEnd();

		if(DEBUG) {
			int numVertices = 2*n*(n+1);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}
	protected void drawSubsection(int x1, int z1, int x2, int z2)
	{
		final int x_w = (x2 - x1);
		final int z_w = (z2 - z1);

		// precision
		int n = (int) (((double) ((x_w > z_w) ? x_w : z_w) / (double) landscape.maxLongitude) * 50);
		if (n <= 2) {
			n = 4;
		}

		double theta1, dTetaOverN;
		double phi1, dPhiOverN;
		{
			double theta2, phi2;
			double oneOverDDToRad, oneOverN;

			oneOverDDToRad = Math3D.degToRad / unit.DD;
			oneOverN = 1.0 / n;

			theta1 = (x1 + landscape.maxLongitude) * oneOverDDToRad;  // startx
			theta2 = (x2 + landscape.maxLongitude) * oneOverDDToRad; // endx

			phi1 = (z1) * oneOverDDToRad; //starty
			phi2 = (z2) * oneOverDDToRad;  //endy

			dTetaOverN = (theta2 - theta1) * oneOverN;
			dPhiOverN  = (phi2   - phi1)   * oneOverN;
		}

		final boolean useColor = (landscape.displayMode == Landscape.DISPLAY_SHADEDDEM);
		if(useColor) gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);

		double t1 = phi1;
		double cosT1_E, sinT1_E;
		double cosT2_E =   Math.cos(t1) * EARTH_RADIUS;
		double sinT2_E = - Math.sin(t1) * EARTH_RADIUS;

		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int j = 0; j < n; j++)
		{
			final double t2 = t1 + dPhiOverN;

			cosT1_E = cosT2_E;
			sinT1_E = sinT2_E;
			cosT2_E =   Math.cos(t2) * EARTH_RADIUS;
			sinT2_E = - Math.sin(t2) * EARTH_RADIUS;

			double t3 = theta1;

			//gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int i = 0; i <= n; i++)
			{
				final double cosT3 = Math.cos(t3);
				final double sinT3 = Math.sin(t3);

				double cx1, cy1, cz1, cx2, cy2, cz2;
				cx1 =  cosT1_E * sinT3;
				cx2 =  cosT2_E * sinT3;
				cy1 =  sinT1_E;
				cy2 =  sinT2_E;
				cz1 =  cosT1_E * cosT3;
				cz2 =  cosT2_E * cosT3;

				gl.glVertex3d(cx1, cy1, cz1);
				gl.glVertex3d(cx2, cy2, cz2);

				t3 += dTetaOverN;
			}
			//gl.glEnd();

			t1 = t2;
		}
		gl.glEnd();

		if(DEBUG) {
			int numVertices = 2*n*(n+1);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * (3 * 8);
		}
	}

	protected void drawTinSubsection(boolean texture, int x1, int z1, int x2, int z2)
	{
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;

		// corners clockwise, from ul
		if ((leftTile == null) || (leftTile.jp2 == null) || (leftTile.jp2.level != drawZlevel)) {
			left_dem_slope = (double) (jtile.tin.p[3][1] - jtile.tin.p[0][1]) / jtile.tin.w;
		}
		if ((rightTile == null) || (rightTile.jp2 == null) || (rightTile.jp2.level != drawZlevel)) {
			right_dem_slope = (double) (jtile.tin.p[2][1] - jtile.tin.p[1][1]) / jtile.tin.w;
		}
		if ((aboveTile == null) || (aboveTile.jp2 == null) || (aboveTile.jp2.level != drawZlevel)) {
			top_dem_slope = (double) (jtile.tin.p[1][1] - jtile.tin.p[0][1]) / jtile.tin.w;
		}
		if ((belowTile == null) || (belowTile.jp2 == null) || (belowTile.jp2.level != drawZlevel)) {
			bottom_dem_slope = (double) (jtile.tin.p[2][1] - jtile.tin.p[3][1]) / jtile.tin.w;
		}

		double theta1, dThetaOverW;
		double phi1, dPhiOverW;
		{
			double phi2, theta2;

			theta1 = (x1 + landscape.maxLongitude) * Math3D.degToRad / unit.DD;  // startx
			theta2 = (x2 + landscape.maxLongitude) * Math3D.degToRad / unit.DD; // endx

			phi1 = (z1) * Math3D.degToRad / unit.DD; //starty
			phi2 = (z2) * Math3D.degToRad / unit.DD;  //endy

			double oneOverW = 1.0 / jtile.tin.w;
			dThetaOverW = (theta2 - theta1) * oneOverW;
			dPhiOverW   = (phi2 - phi1) * oneOverW;
		}

		float oneOverW = 1.0f / jtile.tin.w;
		for (int i = 0; i < jtile.tin.nSt.length; i++)
		{
			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int j = 0; j < jtile.tin.nSt[i].length; j++)
			{
				int v = jtile.tin.nSt[i][j];

				double dx, dy, dz;
				{
					final float[] p = jtile.tin.p[v];
					final float tinW = jtile.tin.w;

					dy = p[1];
					if ((left_dem_slope != -1) && (p[0] == 0)) {
						dy = jtile.tin.p[0][1] + (p[2] * left_dem_slope);
					}
					if ((right_dem_slope != -1) && (p[0] == tinW)) {
						dy = jtile.tin.p[1][1] + (p[2] * right_dem_slope);
					}
					if ((top_dem_slope != -1) && (p[2] == 0)) {
						dy = jtile.tin.p[0][1] + (p[0] * top_dem_slope);
					}
					if ((bottom_dem_slope != -1) && (p[2] == tinW)) {
						dy = jtile.tin.p[3][1] + (p[0] * bottom_dem_slope);
					}
					dy *= unit.coordSystemRatio * terrainScaler;

					dx = theta1 + p[0] * dThetaOverW;
					dz = phi1 + p[2] * dPhiOverW;
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

				if (texture) {
					gl.glTexCoord2f((jtile.tin.p[v][0] * oneOverW), (jtile.tin.p[v][2] * oneOverW));
				}
				else if (landscape.displayMode == Landscape.DISPLAY_SHADEDDEM) {
					setColor((float) dy);
				}
				gl.glVertex3d(tx, ty, tz);
			}
			gl.glEnd();

			if(DEBUG) {
				int numVertices = jtile.tin.nSt[i].length;
				ProfilerInterface.vertexCounter += numVertices;
				if (texture) {
					ProfilerInterface.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
				}
				else if (landscape.displayMode == Landscape.DISPLAY_SHADEDDEM) {
					ProfilerInterface.vertexMemoryUsage += numVertices * (3 * 4 + 3 * 8);
				}
				else {
					ProfilerInterface.vertexMemoryUsage += numVertices * (3 * 8);
				}
			}
		}
	}

	protected final void setColor(float y)
	{
		final float COORDSYS_RATIO = Ptolemy3D.ptolemy.unit.coordSystemRatio;

		if (y > landscape.maxColorHeight) {
			y = landscape.maxColorHeight;
		}
		y /= COORDSYS_RATIO;

		gl.glColor3f(tileColor[0] + y * colratios[0],
					 tileColor[1] + y * colratios[1],
					 tileColor[2] + y * colratios[2]);
	}
	protected final void setJP2ResolutionColor()
	{
		if(DEBUG) {
			int tileRes = (jtile == null) ? -1 : jtile.curRes;
			switch(tileRes) {
				case 0:  gl.glColor3f(1.0f, 0.0f, 0.0f); break;
				case 1:  gl.glColor3f(0.0f, 1.0f, 0.0f); break;
				case 2:  gl.glColor3f(0.0f, 0.0f, 1.0f); break;
				case 3:  gl.glColor3f(1.0f, 1.0f, 1.0f); break;
				default: gl.glColor3f(0.5f, 0.5f, 0.5f); break;
			}
		}
	}
	protected final void setLevelIDColor()
	{
		if(DEBUG) {
			int levelID = (jtile == null) ? 0 : jtile.level;
			float scaler = 1 - (levelID / 3) / 3.0f;
			switch(1 + levelID % 3) {
				default: gl.glColor3f(0.25f, 0.25f, 0.25f); break;
				case 1:  gl.glColor3f(scaler, 0.0f, 0.0f); break;
				case 2:  gl.glColor3f(0.0f, scaler, 0.0f); break;
				case 3:  gl.glColor3f(0.0f, 0.0f, scaler); break;
			}
		}
	}
	protected final void setTileIDColor()
	{
		if(DEBUG) {
			final float C0 = 0.0f;
			final float C1 = 1.0f;
			final float C2 = 0.8f;
			final float C3 = 0.6f;
			final float C4 = 0.4f;

			int tileID = ProfilerInterface.tileCounter;

			float scaler = 1 - (tileID / 27) / 8.0f;
			tileID = tileID % 27;
			switch(tileID) {
				case 0:  gl.glColor3f(scaler*C1, scaler*C0, scaler*C0); break;
				case 1:  gl.glColor3f(scaler*C1, scaler*C2, scaler*C0); break;
				case 2:  gl.glColor3f(scaler*C1, scaler*C0, scaler*C2); break;
				case 3:  gl.glColor3f(scaler*C2, scaler*C0, scaler*C1); break;
				case 4:  gl.glColor3f(scaler*C0, scaler*C0, scaler*C1); break;
				case 5:  gl.glColor3f(scaler*C0, scaler*C2, scaler*C1); break;
				case 6:  gl.glColor3f(scaler*C0, scaler*C1, scaler*C2); break;
				case 7:  gl.glColor3f(scaler*C0, scaler*C1, scaler*C0); break;
				case 8:  gl.glColor3f(scaler*C2, scaler*C1, scaler*C0); break;

				case 9:  gl.glColor3f(scaler*C1, scaler*C0, scaler*C0); break;
				case 10: gl.glColor3f(scaler*C1, scaler*C3, scaler*C0); break;
				case 11: gl.glColor3f(scaler*C1, scaler*C0, scaler*C3); break;
				case 12: gl.glColor3f(scaler*C3, scaler*C0, scaler*C1); break;
				case 13: gl.glColor3f(scaler*C0, scaler*C0, scaler*C1); break;
				case 14: gl.glColor3f(scaler*C0, scaler*C3, scaler*C1); break;
				case 15: gl.glColor3f(scaler*C0, scaler*C1, scaler*C3); break;
				case 16: gl.glColor3f(scaler*C0, scaler*C1, scaler*C0); break;
				case 17: gl.glColor3f(scaler*C0, scaler*C1, scaler*C0); break;

				case 18: gl.glColor3f(scaler*C1, scaler*C0, scaler*C0); break;
				case 19: gl.glColor3f(scaler*C1, scaler*C4, scaler*C0); break;
				case 20: gl.glColor3f(scaler*C1, scaler*C0, scaler*C4); break;
				case 21: gl.glColor3f(scaler*C4, scaler*C0, scaler*C1); break;
				case 22: gl.glColor3f(scaler*C0, scaler*C0, scaler*C1); break;
				case 23: gl.glColor3f(scaler*C0, scaler*C4, scaler*C1); break;
				case 24: gl.glColor3f(scaler*C0, scaler*C1, scaler*C4); break;
				case 25: gl.glColor3f(scaler*C0, scaler*C1, scaler*C0); break;
				case 26: gl.glColor3f(scaler*C0, scaler*C1, scaler*C0); break;
			}
		}
	}
	protected final boolean loadGLState(boolean texture)
	{
		if (landscape.displayMode == Landscape.DISPLAY_JP2RES) {
			setJP2ResolutionColor();
			return false;
		}
		else if (landscape.displayMode == Landscape.DISPLAY_TILEID) {
			setTileIDColor();
			return false;
		}
		else if (landscape.displayMode == Landscape.DISPLAY_LEVELID) {
			setLevelIDColor();
			return false;
		}
		else if (landscape.displayMode == Landscape.DISPLAY_MESH ||
			landscape.displayMode == Landscape.DISPLAY_SHADEDDEM) {
			return false;
		}
		if(texture) {
			gl.glEnable(GL.GL_TEXTURE_2D);
			return true;
		}
		else {
			gl.glDisable(GL.GL_TEXTURE_2D);
			return false;
		}
	}
}
