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
package org.ptolemy3d.globe;

import static org.ptolemy3d.debug.Config.DEBUG;
import static org.ptolemy3d.Unit.EARTH_RADIUS;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.globe.Tile.TileRenderer;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;

/**
 * Original version of the tile render.<BR>
 * Keeped to track bug if some appears ...<BR>
 * Please keep it !
 */
class TileDefaultRenderer implements TileRenderer {
	/* All of that are temporary datas that are used in drawSubsection.
	 * They must be restore in each entering */

	private GL gl;
	private MapData jtile;
	private int drawZlevel;
	private boolean texture;
	private Tile leftTile;
	private Tile aboveTile;
	private Tile rightTile;
	private Tile belowTile;
	private int ZLevel;
	private int upLeftX,  upLeftZ;
	private int lowRightX,  lowRightZ;
	private Landscape landscape;
	private float[] tileColor;
	private float[] colratios;
	private double terrainScaler;
	private float meshColor;

	private final void fillTemporaryVariables(Tile tile) {
		gl = tile.gl;
		jtile = tile.mapData;
		drawZlevel = tile.drawZlevel;
		texture = tile.texture;
		leftTile = tile.left;
		aboveTile = tile.above;
		rightTile = tile.right;
		belowTile = tile.below;
		upLeftX = tile.getUpLeftLon();
		upLeftZ = tile.getUpLeftLat();
		lowRightX = tile.getLowRightLon();
		lowRightZ = tile.getLowRightLat();
		ZLevel = tile.getLevelID();

		landscape = Ptolemy3D.getScene().getLandscape();
		tileColor = landscape.getTileColor();
		colratios = landscape.getColorRatios();

		terrainScaler = landscape.getTerrainScaler();
		meshColor = landscape.getMeshColor();
	}

	public void drawSubsection(Tile tile, int x1, int z1, int x2, int z2) {
		if ((x1 == x2) || (z1 == z2)) {
			return;
		}

		if (DEBUG) {
			if (!ProfilerUtil.renderTiles) {
				return;
			}
			ProfilerUtil.tileSectionCounter++;
		}
		fillTemporaryVariables(tile);

		boolean useDem = ((jtile != null) && (jtile.dem != null) && (landscape.isTerrainEnabled())) ? true : false;
		boolean useTin = ((jtile != null) && (jtile.tin != null) && (landscape.isTerrainEnabled())) ? true : false;

		if (useTin) {
			drawSubsection_Tin(x1, z1, x2, z2);
		}
		else if (useDem) {
			drawSubsection_Dem(x1, z1, x2, z2);
		}
		else {
			drawSubsection(x1, z1, x2, z2);
		}
	}

	private final void drawSubsection_Tin(int x1, int z1, int x2, int z2) {
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1, dy;

		// corners clockwise, from ul
		if ((leftTile == null) || (leftTile.mapData.key.layer != drawZlevel)) {
			left_dem_slope = (double) (jtile.tin.p[3][1] - jtile.tin.p[0][1]) / jtile.tin.w;
		}
		if ((rightTile == null) || (rightTile.mapData.key.layer != drawZlevel)) {
			right_dem_slope = (double) (jtile.tin.p[2][1] - jtile.tin.p[1][1]) / jtile.tin.w;
		}
		if ((aboveTile == null) || (aboveTile.mapData.key.layer != drawZlevel)) {
			top_dem_slope = (double) (jtile.tin.p[1][1] - jtile.tin.p[0][1]) / jtile.tin.w;
		}
		if ((belowTile == null) || (belowTile.mapData.key.layer != drawZlevel)) {
			bottom_dem_slope = (double) (jtile.tin.p[2][1] - jtile.tin.p[3][1]) / jtile.tin.w;
		}

		double theta1 = 0, theta2 = 0;
		double phi1 = 0, phi2 = 0;
		{
			theta1 = (x1 + landscape.getMaxLongitude()) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();  // startx
			theta2 = (x2 + landscape.getMaxLongitude()) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor(); // endx

			phi1 = (z1) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor(); //starty
			phi2 = (z2) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();  //endy
		}

		double dx, dz;
		double tx, ty, tz;

		for (int i = 0; i < jtile.tin.nSt.length; i++) {
			setGLBegin();
			for (int j = 0; j < jtile.tin.nSt[i].length; j++) {
				int v;

				v = jtile.tin.nSt[i][j];
				dy = jtile.tin.p[v][1];

				if ((left_dem_slope != -1) && (jtile.tin.p[v][0] == 0)) {
					dy = jtile.tin.p[0][1] + (jtile.tin.p[v][2] * left_dem_slope);
				}
				if ((right_dem_slope != -1) && (jtile.tin.p[v][0] == jtile.tin.w)) {
					dy = jtile.tin.p[1][1] + (jtile.tin.p[v][2] * right_dem_slope);
				}
				if ((top_dem_slope != -1) && (jtile.tin.p[v][2] == 0)) {
					dy = jtile.tin.p[0][1] + (jtile.tin.p[v][0] * top_dem_slope);
				}
				if ((bottom_dem_slope != -1) && (jtile.tin.p[v][2] == jtile.tin.w)) {
					dy = jtile.tin.p[3][1] + (jtile.tin.p[v][0] * bottom_dem_slope);
				}
				dy *= Unit.getCoordSystemRatio() * terrainScaler;

				{
					dx = theta1 + jtile.tin.p[v][0] * (theta2 - theta1) / jtile.tin.w;
					dz = phi1 + jtile.tin.p[v][2] * (phi2 - phi1) / jtile.tin.w;

					tx =  (EARTH_RADIUS + dy) * Math.cos(dz) * Math.sin(dx);
					ty = -(EARTH_RADIUS + dy) * Math.sin(dz);
					tz =  (EARTH_RADIUS + dy) * Math.cos(dz) * Math.cos(dx);
				}

				if (texture) {
					gl.glTexCoord2f((jtile.tin.p[v][0] / jtile.tin.w), (jtile.tin.p[v][2] / jtile.tin.w));
				}
				else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
					setColor((float) dy);
				}
				gl.glVertex3d(tx, ty, tz);
			}
			setGLEnd();

			if(DEBUG) {
				int numVertices = jtile.tin.nSt[i].length;
				ProfilerUtil.vertexCounter += numVertices;
				if (texture) {
					ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
				}
				else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
					ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 4 + 3 * 8);
				}
				else {
					ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 8);
				}
			}
		}
	}

	private final void drawSubsection_Dem(int x1, int z1, int x2, int z2) {
		final Layer drawLevel = Ptolemy3D.getScene().getLandscape().globe.getLayer(drawZlevel);
		final byte[] dem = jtile.dem.demDatas;

		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;

		int numrows = jtile.dem.numRows;
		int row_width = numrows * 2; // assuming we have a square tile
		double geom_inc = (double) drawLevel.getTileSize() / (numrows - 1);
		float tex_inc = 1.0f / (numrows - 1);
		double dx, dz, d2z, dy1, dy2;
		float tex_x, tex_z, tex_z2;
		double ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double sx_wt = 0, ex_wt = 0, t_sz_wt = 0, t_ez_wt = 0, b_sz_wt = 0, b_ez_wt = 0;
		int pos_d1 = 0, pos_r1 = 0, pos_d2 = 0, pos_r2 = 0;
		double cx1, cy1, cz1, cx2, cy2, cz2;

		boolean xsinterpolate = false, xeinterpolate = false, zsinterpolate = false, zeinterpolate = false;

		double startxcoord = ((x1 - upLeftX) / geom_inc);
		int startx = (int) startxcoord;
		if (startx != startxcoord) {
			xsinterpolate = true;
		}
		double startzcoord = ((z1 - upLeftZ) / geom_inc);
		int startz = (int) startzcoord;
		if (startz != startzcoord) {
			zsinterpolate = true;
		}
		double endxcoord = ((x2 - upLeftX) / geom_inc) + 1;
		int endx = (int) endxcoord;
		if (endxcoord != endx) {
			endx++;
			xeinterpolate = true;
		}
		double endzcoord = ((z2 - upLeftZ) / geom_inc);
		int endz = (int) endzcoord;
		if (endzcoord != endz) {
			endz++;
			zeinterpolate = true;
		}

		int nrows_x = endx - startx - 1;
		int nrows_z = endz - startz;

		if ((drawZlevel == ZLevel) && (x1 == upLeftX) && (x2 == lowRightX) && (z1 == upLeftZ) && (z2 == lowRightZ)) {
			ul_corner = ((dem[0] << 8) + (dem[1] & 0xFF));
			ur_corner = ((dem[(row_width - 2)] << 8) + (dem[(row_width - 1)] & 0xFF));
			ll_corner = ((dem[row_width * (numrows - 1)] << 8) + (dem[(row_width * (numrows - 1)) + 1] & 0xFF));
			lr_corner = ((dem[row_width * (numrows - 1) + (row_width - 2)] << 8) + (dem[(row_width * (numrows - 1)) + (row_width - 1)] & 0xFF));

			if ((leftTile == null) || (leftTile.mapData.key.layer != drawZlevel)) {
				left_dem_slope = (ll_corner - ul_corner) / (nrows_z);
			}
			if ((rightTile == null) || (rightTile.mapData.key.layer != drawZlevel)) {
				right_dem_slope = (lr_corner - ur_corner) / (nrows_z);
			}
			if ((aboveTile == null) || (aboveTile.mapData.key.layer != drawZlevel)) {
				top_dem_slope = (ur_corner - ul_corner) / (nrows_x);
			}
			if ((belowTile == null) || (belowTile.mapData.key.layer != drawZlevel)) {
				bottom_dem_slope = (lr_corner - ll_corner) / (nrows_x);
			}
		}

		double theta1 = 0, theta2 = 0;
		double phi1 = 0, phi2 = 0;
		{
			theta1 = (x1 + landscape.getMaxLongitude()) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();  // startx
			theta2 = (x2 + landscape.getMaxLongitude()) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor(); // endx

			phi1 = (z1) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor(); //starty
			phi2 = (z2) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();  //endy
		}

		for (int i = startz; i < endz; i++) {
			dz = phi1 + (i - startz) * (phi2 - phi1) / nrows_z;
			d2z = phi1 + ((i - startz) + 1) * (phi2 - phi1) / nrows_z;

			tex_z = (tex_inc * i);
			tex_z2 = (tex_inc * (i + 1));

			if ((i == startz) && (zsinterpolate)) {
				tex_z += (tex_inc * (startzcoord - startz));
				t_ez_wt = (startzcoord - startz);
				t_sz_wt = 1.0 - t_ez_wt;
			}
			else {
				t_ez_wt = 0;
				t_sz_wt = 1;
			}

			if ((i == (endz - 1)) && (zeinterpolate)) {
				tex_z2 -= (tex_inc * (endz - endzcoord));
				b_sz_wt = (endz - endzcoord);
				b_ez_wt = 1.0 - b_sz_wt;
			}
			else {
				b_sz_wt = 0;
				b_ez_wt = 1;
			}

			setGLBegin();
			for (int j = startx; j < endx; j++) {
				dx = theta1 + (j - startx) * (theta2 - theta1) / (nrows_x);

				tex_x = j * tex_inc;
				pos_d1 = ((i * row_width) + (j * 2));
				pos_d2 = (((i + 1) * row_width) + (j * 2));

				if ((j == startx) && (xsinterpolate)) {
					tex_x += (tex_inc * (startxcoord - startx));
					ex_wt = (startxcoord - startx);
					sx_wt = 1.0 - ex_wt;
					pos_r1 = ((i * row_width) + ((j + 1) * 2));
					pos_r2 = (((i + 1) * row_width) + ((j + 1) * 2));
				}
				else if ((j == (endx - 1)) && (xeinterpolate)) {
					tex_x -= (tex_inc * (endx - endxcoord));
					ex_wt = (endx - endxcoord);
					sx_wt = 1.0 - ex_wt;
					pos_r1 = ((i * row_width) + ((j - 1) * 2));
					pos_r2 = (((i + 1) * row_width) + ((j - 1) * 2));
				}
				else {
					ex_wt = 0.0;
					sx_wt = 1;
					pos_r1 = pos_d1;
					pos_r2 = pos_d2;
				}

				if (drawZlevel == ZLevel) {
					if ((j == startx) && (left_dem_slope != -1)) {
						dy1 = ul_corner + (i * left_dem_slope);
						dy2 = ul_corner + ((i + 1) * left_dem_slope);
					}
					else if ((j == (endx - 1)) && (right_dem_slope != -1)) {
						dy1 = ur_corner + (i * right_dem_slope);
						dy2 = ur_corner + ((i + 1) * right_dem_slope);
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
					dy1 = (((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * sx_wt * t_sz_wt) + (((dem[pos_r1] << 8) + (dem[pos_r1 + 1] & 0xFF)) * ex_wt * t_sz_wt) + (((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * sx_wt * t_ez_wt) + (((dem[pos_r2] << 8) + (dem[pos_r2 + 1] & 0xFF)) * ex_wt * t_ez_wt);
					dy2 = (((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * sx_wt * b_sz_wt) + (((dem[pos_r1] << 8) + (dem[pos_r1 + 1] & 0xFF)) * ex_wt * b_sz_wt) + (((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * sx_wt * b_ez_wt) + (((dem[pos_r2] << 8) + (dem[pos_r2 + 1] & 0xFF)) * ex_wt * b_ez_wt);
				}

				dy1 *= Unit.getCoordSystemRatio() * terrainScaler;
				dy2 *= Unit.getCoordSystemRatio() * terrainScaler;

				{
					cx1 = (EARTH_RADIUS + dy1) * Math.cos(dz) * Math.sin(dx);
					cx2 = (EARTH_RADIUS + dy2) * Math.cos(d2z) * Math.sin(dx);
					cy1 = -(EARTH_RADIUS + dy1) * Math.sin(dz);
					cy2 = -(EARTH_RADIUS + dy2) * Math.sin(d2z);
					cz1 = (EARTH_RADIUS + dy1) * Math.cos(dz) * Math.cos(dx);
					cz2 = (EARTH_RADIUS + dy2) * Math.cos(d2z) * Math.cos(dx);
				}


				if (texture) {
					gl.glTexCoord2f(tex_x, tex_z);
				}
				else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
					setColor((float) dy1);
				}
				gl.glVertex3d(cx1, cy1, cz1);

				if (texture) {
					gl.glTexCoord2f(tex_x, tex_z2);
				}
				else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
					setColor((float) dy2);
				}
				gl.glVertex3d(cx2, cy2, cz2);

				if (texture) {
					gl.glTexCoord2f(tex_x, tex_z2);
				}
				else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
					setColor((float) dy2);
				}
			}
			setGLEnd();
		}

		if(DEBUG) {
			int numVertices = 2 * (endx-startx) * (endz-startz);
			ProfilerUtil.vertexCounter += numVertices;
			if (texture) {
				ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
			}
			else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
				ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 4 + 3 * 8);
			}
			else {
				ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 8);
			}
		}
	}

	private final void drawSubsection(int x1, int z1, int x2, int z2) {
		final Layer drawLevel = Ptolemy3D.getScene().getLandscape().globe.getLayer(drawZlevel);

		double t1, t2, t3;
		float tx, ty1, ty2;
		int x_w = (x2 - x1);
		int z_w = (z2 - z1);

		// texture ratios
		float tx_start = ((float) (x1 - upLeftX) / drawLevel.getTileSize());
		float tz_start = ((float) (z1 - upLeftZ) / drawLevel.getTileSize());

		double cx1, cy1, cz1, cx2, cy2, cz2;

		// precision, soon to be dem...
		int n = (int) (((double) ((x_w > z_w) ? x_w : z_w) / (double) landscape.getMaxLongitude()) * 50);
		if (n <= 2) {
			n = 4;
		}

		float tx_w = ((float) x_w / drawLevel.getTileSize() / n);
		float tz_w = ((float) z_w / drawLevel.getTileSize() / (n));

		if (!texture) {
			gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
		}

		double theta1 = 0, theta2 = 0;
		double phi1 = 0, phi2 = 0;
		{
			theta1 = (x1 + landscape.getMaxLongitude()) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();  // startx
			theta2 = (x2 + landscape.getMaxLongitude()) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor(); // endx

			phi1 = (z1) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor(); //starty
			phi2 = (z2) * Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();  //endy
		}

		for (int j = 0; j < n; j++) {
			t1 = phi1 + j * (phi2 - phi1) / n;
			t2 = phi1 + (j + 1) * (phi2 - phi1) / n;

			ty1 = (tz_start + (j * tz_w));      //j/(n/2);
			ty2 = (tz_start + ((j + 1) * tz_w));

			setGLBegin();
			for (int i = 0; i <= n; i++) {

				t3 = (theta1 + i * (theta2 - theta1) / n);
				tx = tx_start + i * tx_w; //(float)i/n;

				cx1 = EARTH_RADIUS * Math.cos(t1) * Math.sin(t3);
				cx2 = EARTH_RADIUS * Math.cos(t2) * Math.sin(t3);
				cy1 = -EARTH_RADIUS * Math.sin(t1);
				cy2 = -EARTH_RADIUS * Math.sin(t2);
				cz1 = EARTH_RADIUS * Math.cos(t1) * Math.cos(t3);
				cz2 = EARTH_RADIUS * Math.cos(t2) * Math.cos(t3);

				if (texture) {
					gl.glTexCoord2f(tx, ty1);
				}
				gl.glVertex3d(cx1, cy1, cz1);

				if (texture) {
					gl.glTexCoord2f(tx, ty2);
				}
				gl.glVertex3d(cx2, cy2, cz2);
			}
			setGLEnd();
		}

		if(DEBUG) {
			int numVertices = 2*n*n;
			ProfilerUtil.vertexCounter += numVertices;
			if (texture) {
				ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
			}
			else {
				ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 8);
			}
		}
	}

	private final void setColor(float y) {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final float COORDSYS_RATIO = Unit.getCoordSystemRatio();

		if (y > landscape.getMaxColorHeight()) {
			y = landscape.getMaxColorHeight();
		}
		y /= COORDSYS_RATIO;

		gl.glColor3f(tileColor[0] + y * colratios[0],
				tileColor[1] + y * colratios[1],
				tileColor[2] + y * colratios[2]);
	}

	private final void setGLBegin() {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();

		switch (landscape.getDisplayMode()) {
			case Landscape.DISPLAY_MESH:
				texture = false;
				gl.glDisable(GL.GL_TEXTURE_2D);
				gl.glColor3f(meshColor, meshColor, meshColor);
				gl.glLineWidth(1);
				gl.glBegin(GL.GL_LINE_STRIP);
				break;
			case Landscape.DISPLAY_SHADEDDEM:
				gl.glShadeModel(GL.GL_SMOOTH);
				gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
				texture = false;
				gl.glDisable(GL.GL_TEXTURE_2D);
			default:
				gl.glBegin(GL.GL_TRIANGLE_STRIP);
			break;
		}
	}

	private final void setGLEnd() {
		gl.glEnd();

		Landscape landscape = Ptolemy3D.getScene().getLandscape();
		switch (landscape.getDisplayMode()) {
			case Landscape.DISPLAY_MESH:
				gl.glEnable(GL.GL_TEXTURE_2D);
				break;
			case Landscape.DISPLAY_SHADEDDEM:
				gl.glShadeModel(GL.GL_FLAT);
				gl.glEnable(GL.GL_TEXTURE_2D);
				break;
		}
	}
}
