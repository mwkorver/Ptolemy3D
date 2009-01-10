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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import org.ptolemy3d.debug.ProfilerInterface;
import org.ptolemy3d.math.Math3D;

class TileVARenderer extends TileDirectModeRenderer
{
	//Memory used = 64 * MAX_NUM_VERTEX = 64ko
	private final static int MAX_NUM_VERTEX = 1000;
	private FloatBuffer uvBuffer = ByteBuffer.allocateDirect(MAX_NUM_VERTEX * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
	private DoubleBuffer vertexBuffer = ByteBuffer.allocateDirect(MAX_NUM_VERTEX * 3 * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
	private float[] uvArray = new float[2 * MAX_NUM_VERTEX];
	private double[] vertexArray = new double[3 * MAX_NUM_VERTEX];
	private int uvArrayPointer, vertexArrayPointer, vertexCount;

	protected final void drawDemSubsection_Textured(int x1, int z1, int x2, int z2)
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
		double cosZ, sinZ;
		double cos2Z = Math.cos(dz);
		double sin2Z = Math.sin(dz);

		int pos_d1 = (startz * rowWidth) + (startx * 2);
		for (int i = startz; i < endz; i++)
		{
			final double d2z = dz + dPhiOverN;

			cosZ  = cos2Z;
			sinZ  = sin2Z;
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

				dy1 *= dyScaler;
				dy2 *= dyScaler;

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

				uvArray[uvArrayPointer] = tex_x; uvArrayPointer++;
				uvArray[uvArrayPointer] = tex_z; uvArrayPointer++;
				vertexArray[vertexArrayPointer] = cx1; vertexArrayPointer++;
				vertexArray[vertexArrayPointer] = cy1; vertexArrayPointer++;
				vertexArray[vertexArrayPointer] = cz1; vertexArrayPointer++;

				uvArray[uvArrayPointer] = tex_x; uvArrayPointer++;
				uvArray[uvArrayPointer] = tex_z2; uvArrayPointer++;
				vertexArray[vertexArrayPointer] = cx2; vertexArrayPointer++;
				vertexArray[vertexArrayPointer] = cy2; vertexArrayPointer++;
				vertexArray[vertexArrayPointer] = cz2; vertexArrayPointer++;

				pos_d1 += 2;
				dx     += dTetaOverN;
			}

			dz = d2z;
			pos_d1 = pos_d1_save + rowWidth;
		}

		/**
		 * Vertex Array Renderer
		 * =====================
		 * Important, the strip jonction is ok ONLY IF BACK FACE IS CULLED !!!
		 * Back face culling is enable by default in Landscape.
		 */
		vertexCount = 2 * (endx - startx) * (endz - startz);
		vertexBuffer.put(vertexArray, 0, vertexArrayPointer).rewind(); vertexArrayPointer = 0;
		uvBuffer.put(uvArray, 0, uvArrayPointer).rewind(); uvArrayPointer = 0;
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, uvBuffer);
		gl.glVertexPointer(3, GL.GL_DOUBLE, 0, vertexBuffer);
		gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, vertexCount);
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
		vertexCount = 0;

		if(DEBUG) {
			int numVertices = 2 * (endx-startx) * (endz-startz);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}

	protected final void drawSubsection_Textured(int x1, int z1, int x2, int z2)
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

		double theta1, dTetaOverN;
		double phi1, dPhiOverN;
		{
			double theta2, phi2;
			double oneOverDDToRad, oneOverN;

			oneOverDDToRad = Math3D.degToRad / unit.DD;
			oneOverN = 1.0 / n;

			theta1 = (x1 + landscape.maxLongitude) * oneOverDDToRad;
			theta2 = (x2 + landscape.maxLongitude) * oneOverDDToRad;

			phi1 = (z1) * oneOverDDToRad;
			phi2 = (z2) * oneOverDDToRad;

			dTetaOverN = (theta2 - theta1) * oneOverN;
			dPhiOverN  = (phi2   - phi1)   * oneOverN;
		}

		float tx_w, tz_w;
		{
			float oneOverTileWidthOverN = oneOverTileWidth / n;
			tx_w = x_w * oneOverTileWidthOverN;
			tz_w = z_w * oneOverTileWidthOverN;
		}

		double t1 = phi1;
		double cosT2_E, sinT2_E;
		double cosT1_E =   Math.cos(t1) * EARTH_RADIUS;
		double sinT1_E = - Math.sin(t1) * EARTH_RADIUS;

		float ty1 = tz_start;
		for (int j = 0; j < n; j++)
		{
			final double t2 = t1 + dPhiOverN;
			final float ty2 = ty1 + tz_w;

			cosT2_E =   Math.cos(t2) * EARTH_RADIUS;
			sinT2_E = - Math.sin(t2) * EARTH_RADIUS;

			double t3 = theta1;
			float tx  = tx_start;
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

				uvArray[uvArrayPointer] = tx; uvArrayPointer++;
				uvArray[uvArrayPointer] = ty1; uvArrayPointer++;
				vertexArray[vertexArrayPointer] = cx1; vertexArrayPointer++;
				vertexArray[vertexArrayPointer] = cy1; vertexArrayPointer++;
				vertexArray[vertexArrayPointer] = cz1; vertexArrayPointer++;

				uvArray[uvArrayPointer] = tx; uvArrayPointer++;
				uvArray[uvArrayPointer] = ty2; uvArrayPointer++;
				vertexArray[vertexArrayPointer] = cx2; vertexArrayPointer++;
				vertexArray[vertexArrayPointer] = cy2; vertexArrayPointer++;
				vertexArray[vertexArrayPointer] = cz2; vertexArrayPointer++;

				t3 += dTetaOverN;
				tx += tx_w;
			}

			t1  = t2;
			ty1 = ty2;
			cosT1_E = cosT2_E;
			sinT1_E = sinT2_E;
		}

		/**
		 * Vertex Array Renderer
		 * =====================
		 * Important, the strip jonction is ok ONLY IF BACK FACE IS CULLED !!!
		 * Back face culling is enable by default in Landscape.
		 */
		vertexCount = 2*n*(n+1);
		vertexBuffer.put(vertexArray, 0, vertexArrayPointer).rewind(); vertexArrayPointer = 0;
		uvBuffer.put(uvArray, 0, uvArrayPointer).rewind(); uvArrayPointer = 0;
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, uvBuffer);
		gl.glVertexPointer(3, GL.GL_DOUBLE, 0, vertexBuffer);
		gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, vertexCount);
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
		vertexCount = 0;

		if(DEBUG) {
			int numVertices = 2*n*(n+1);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}
	protected final void drawSubsection(int x1, int z1, int x2, int z2)
	{
		int x_w = (x2 - x1);
		int z_w = (z2 - z1);

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

		gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);

		double t1 = phi1;
		double cosT1_E, sinT1_E;
		double cosT2_E =   Math.cos(t1) * EARTH_RADIUS;
		double sinT2_E = - Math.sin(t1) * EARTH_RADIUS;

		for (int j = 0; j < n; j++)
		{
			final double t2 = t1 + dPhiOverN;

			cosT1_E = cosT2_E;
			sinT1_E = sinT2_E;
			cosT2_E =   Math.cos(t2) * EARTH_RADIUS;
			sinT2_E = - Math.sin(t2) * EARTH_RADIUS;

			double t3 = theta1;

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
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
			gl.glEnd();

			t1 = t2;
		}
//		gl.glColor3f(1, 1, 1);	//This should be done somewhere

		if(DEBUG) {
			int numVertices = 2*n*(n+1);
			ProfilerInterface.vertexCounter += numVertices;
			ProfilerInterface.vertexMemoryUsage += numVertices * (3 * 8);
		}
	}
}
