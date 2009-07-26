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

import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.globe.Tile.TileArea;
import org.ptolemy3d.math.CosTable;
import org.ptolemy3d.scene.Landscape;

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
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class TileDirectModeRenderer_MathLookUp extends TileDirectModeRenderer {
	@Override
	protected void fillLocalVariables(TileArea subTile) {
		super.fillLocalVariables(subTile);
	}

	@Override
	protected void renderSubTile_DemTextured(int xStart, int zStart, int xEnd, int zEnd) {
		final Layer drawLevel = landscape.globe.getLayer(drawLevelID);
		final byte[] dem = mapData.dem.demDatas;
		final int numRows = mapData.dem.size;

		final int rowWidth = numRows * 2;	// assuming we have a square tile
		final float tex_inc = 1.0f / (numRows - 1);

		final boolean xsinterpolate, xeinterpolate, zsinterpolate, zeinterpolate;
		final double startxcoord, startzcoord, endxcoord, endzcoord;
		final int startx, startz; /*final*/ int endx, endz;
		{
			final double geom_inc = (double) (numRows - 1) / drawLevel.getTileSize();

			startxcoord = ((xStart - refLeftLon) * geom_inc);
			startx = (int) startxcoord;
			xsinterpolate = (startx != startxcoord);

			startzcoord = ((zStart - refUpLat) * geom_inc);
			startz = (int) startzcoord;
			zsinterpolate = (startz != startzcoord);

			endxcoord = ((xEnd - refLeftLon) * geom_inc) + 1;
			endx = (int) endxcoord;
			xeinterpolate = (endxcoord != endx);
			if (xeinterpolate) {
				endx++;
			}

			endzcoord = ((zEnd - refUpLat) * geom_inc);
			endz = (int) endzcoord;
			zeinterpolate = (endzcoord != endz);
			if (zeinterpolate) {
				endz++;
			}
		}

		final double oneOverNrowsX = 1.0 / (endx - startx - 1);
		final double oneOverNrowsZ = 1.0 / (endz - startz);

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawLevelID == layerID);
		if (eqZLevel && (xStart == refLeftLon) && (xEnd == refRightLon) && (zStart == refUpLat) && (zEnd == refLowLat)) {
			final int rowWidthMinusOne = rowWidth - 1;
			final int rowWidthMinusTwo = rowWidth - 2;
			final int i1 = rowWidth * (numRows - 1);

			ul_corner = (dem[     0] << 8) + (dem[     1] & 0xFF);
			ur_corner = (dem[rowWidthMinusTwo] << 8) + (dem[rowWidthMinusOne] & 0xFF);
			ll_corner = (dem[i1] << 8) + (dem[i1 + 1] & 0xFF);
			lr_corner = (dem[i1 + rowWidthMinusTwo] << 8) + (dem[i1 + rowWidthMinusOne] & 0xFF);

			if ((leftTile == null) || (leftTile.mapData == null) || (leftTile.mapData.key.layer != drawLevelID)) {
				left_dem_slope = (ll_corner - ul_corner) * oneOverNrowsZ;
			}
			if ((rightTile == null) || (rightTile.mapData == null) || (rightTile.mapData.key.layer != drawLevelID)) {
				right_dem_slope = (lr_corner - ur_corner) * oneOverNrowsZ;
			}
			if ((aboveTile == null) || (aboveTile.mapData == null) || (aboveTile.mapData.key.layer != drawLevelID)) {
				top_dem_slope = (ur_corner - ul_corner) * oneOverNrowsX;
			}
			if ((belowTile == null) || (belowTile.mapData == null) || (belowTile.mapData.key.layer != drawLevelID)) {
				bottom_dem_slope = (lr_corner - ll_corner) * oneOverNrowsX;
			}
		}

		int dx = (xEnd - xStart) / (endx - startx - 1);
		int dz = (zEnd - zStart) / (endz - startz);

		final double dyScaler = Unit.getCoordSystemRatio() * terrainScaler;

		int lat = zStart;
		double cosZ = CosTable.cos(lat);
		double sinZ = CosTable.sin(lat);

		int pos_d1 = (startz * rowWidth) + (startx * 2);
//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int i = startz; i < endz; i++) //startz can be equal to (endz-1)
		{
			final int lat2 = lat + dz;

			double cos2Z = CosTable.cos(lat2);
			double sin2Z = CosTable.sin(lat2);

			final float tex_z, tex_z2;
			final double t_sz_wt, t_ez_wt, b_sz_wt, b_ez_wt;
			if ((i == startz) && (zsinterpolate)) {
				tex_z = (float) (tex_inc * startzcoord);
				t_ez_wt = startzcoord - startz;
				t_sz_wt = 1.0 - t_ez_wt;
			}
			else {
				tex_z = tex_inc * i;
				t_ez_wt = 0;
				t_sz_wt = 1;
			}
			if ((i == (endz - 1)) && (zeinterpolate)) {
				tex_z2 = (float) (tex_inc * endzcoord);
				b_sz_wt = endz - endzcoord;
				b_ez_wt = 1.0 - b_sz_wt;
			}
			else {
				tex_z2 = tex_inc * (i + 1);
				b_sz_wt = 0;
				b_ez_wt = 1;
			}

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int pos_d1_save = pos_d1;	//int pos_d1 = (i * rowWidth) + (startx * 2);
			int lon = xStart;
			for (int j = startx; j < endx; j++) {
				final int pos_d2 = pos_d1 + rowWidth;

				final float tex_x;
				final double sx_wt,  ex_wt;
				final int pos_r1,  pos_r2;
				if (xsinterpolate && (j == startx)) {
					ex_wt = startxcoord - startx;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((startx + 1) * 2);
					pos_r2 = pos_r1 + rowWidth;

					tex_x = (float) (tex_inc * startxcoord);
				}
				else if (xeinterpolate && (j == (endx - 1))) {
					ex_wt = endx - endxcoord;
					sx_wt = 1.0 - ex_wt;

					pos_r1 = (i * rowWidth) + ((j - 1) * 2);
					pos_r2 = pos_r1 + rowWidth;

					tex_x = (float) (tex_inc * (endxcoord - 1));
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

					final double cosX = CosTable.cos(lon);
					final double sinX = CosTable.sin(lon);

					cx1 = dy1E * cosZ * sinX;
					cx2 = dy2E * cos2Z * sinX;
					cy1 = -dy1E * sinZ;
					cy2 = -dy2E * sin2Z;
					cz1 = dy1E * cosZ * cosX;
					cz2 = dy2E * cos2Z * cosX;
				}

				gl.glTexCoord2f(tex_x, tex_z);
				gl.glVertex3d(cx1, cy1, cz1);

				gl.glTexCoord2f(tex_x, tex_z2);
				gl.glVertex3d(cx2, cy2, cz2);

				pos_d1 += 2;
				lon += dx;
			}
			gl.glEnd();

			lat = lat2;
			cosZ = cos2Z;
			sinZ = sin2Z;
			pos_d1 = pos_d1_save + rowWidth;
		}
//		gl.glEnd();

		if (DEBUG) {
			int numVertices = 2 * (endx - startx) * (endz - startz);
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}

	@Override
	protected void renderSubTile_Dem(int xStart, int zStart, int xEnd, int zEnd) {
		final Layer drawLevel = landscape.globe.getLayer(drawLevelID);
		final byte[] dem = mapData.dem.demDatas;
		final int numRows = mapData.dem.size;
		final boolean useColor = (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM);

		final int rowWidth = numRows * 2;	// assuming we have a square tile

		final boolean xsinterpolate,  xeinterpolate,  zsinterpolate,  zeinterpolate;
		final double startxcoord,  startzcoord,  endxcoord,  endzcoord;
		final int startx,  startz; /*final*/ int endx, endz;
		{
			final double geom_inc = (double) (numRows - 1) / drawLevel.getTileSize();

			startxcoord = ((xStart - refLeftLon) * geom_inc);
			startx = (int) startxcoord;
			xsinterpolate = (startx != startxcoord);

			startzcoord = ((zStart - refUpLat) * geom_inc);
			startz = (int) startzcoord;
			zsinterpolate = (startz != startzcoord);

			endxcoord = ((xEnd - refLeftLon) * geom_inc) + 1;
			endx = (int) endxcoord;
			xeinterpolate = (endxcoord != endx);
			if (xeinterpolate) {
				endx++;
			}

			endzcoord = ((zEnd - refUpLat) * geom_inc);
			endz = (int) endzcoord;
			zeinterpolate = (endzcoord != endz);
			if (zeinterpolate) {
				endz++;
			}
		}

		final double oneOverNrowsX = 1.0 / (endx - startx - 1);
		final double oneOverNrowsZ = 1.0 / (endz - startz);

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawLevelID == layerID);
		if (eqZLevel && (xStart == refLeftLon) && (xEnd == refRightLon) && (zStart == refUpLat) && (zEnd == refLowLat)) {
			final int rowWidthMinusOne = rowWidth - 1;
			final int rowWidthMinusTwo = rowWidth - 2;
			final int i1 = rowWidth * (numRows - 1);

			ul_corner = (dem[     0] << 8) + (dem[     1] & 0xFF);
			ur_corner = (dem[rowWidthMinusTwo] << 8) + (dem[rowWidthMinusOne] & 0xFF);
			ll_corner = (dem[i1] << 8) + (dem[i1 + 1] & 0xFF);
			lr_corner = (dem[i1 + rowWidthMinusTwo] << 8) + (dem[i1 + rowWidthMinusOne] & 0xFF);

			if ((leftTile == null) || (leftTile.mapData == null) || (leftTile.mapData.key.layer != drawLevelID)) {
				left_dem_slope = (ll_corner - ul_corner) * oneOverNrowsZ;
			}
			if ((rightTile == null) || (rightTile.mapData == null) || (rightTile.mapData.key.layer != drawLevelID)) {
				right_dem_slope = (lr_corner - ur_corner) * oneOverNrowsZ;
			}
			if ((aboveTile == null) || (aboveTile.mapData == null) || (aboveTile.mapData.key.layer != drawLevelID)) {
				top_dem_slope = (ur_corner - ul_corner) * oneOverNrowsX;
			}
			if ((belowTile == null) || (belowTile.mapData == null) || (belowTile.mapData.key.layer != drawLevelID)) {
				bottom_dem_slope = (lr_corner - ll_corner) * oneOverNrowsX;
			}
		}

		int dx = (xEnd - xStart) / (endx - startx - 1);
		int dz = (zEnd - zStart) / (endz - startz);

		final double dyScaler = Unit.getCoordSystemRatio() * terrainScaler;

		int lat = zStart;
		double cosZ = CosTable.cos(lat);
		double sinZ = CosTable.sin(lat);

		int pos_d1 = (startz * rowWidth) + (startx * 2);
//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int i = startz; i < endz; i++) //startz can be equal to (endz-1)
		{
			final int lat2 = lat + dz;

			double cos2Z = CosTable.cos(lat2);
			double sin2Z = CosTable.sin(lat2);

			final double t_sz_wt,  t_ez_wt,  b_sz_wt,  b_ez_wt;
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

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int pos_d1_save = pos_d1;	//int pos_d1 = (i * rowWidth) + (startx * 2);
			int lon = xStart;
			for (int j = startx; j < endx; j++) {
				final int pos_d2 = pos_d1 + rowWidth;

				final double sx_wt,  ex_wt;
				final int pos_r1,  pos_r2;
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

					final double cosX = CosTable.cos(lon);
					final double sinX = CosTable.sin(lon);

					cx1 = dy1E * cosZ * sinX;
					cx2 = dy2E * cos2Z * sinX;
					cy1 = -dy1E * sinZ;
					cy2 = -dy2E * sin2Z;
					cz1 = dy1E * cosZ * cosX;
					cz2 = dy2E * cos2Z * cosX;
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
				lon += dx;
			}
			gl.glEnd();

			lat = lat2;
			cosZ = cos2Z;
			sinZ = sin2Z;
			pos_d1 = pos_d1_save + rowWidth;
		}
//		gl.glEnd();

		if (DEBUG) {
			int numVertices = 2 * (endx - startx) * (endz - startz);
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}

	@Override
	protected final void renderSubTile_Textured() {
		// Elevation
		final ElevationNone elevation = new ElevationNone(subTile);
		
		final int nLon = elevation.numPolyLon;
		final int nLat = elevation.numPolyLat;
		
		final int startLon = elevation.polyLonStart;
		final int startLat = elevation.polyLatStart;
		
		final int dLon = elevation.polySizeLon;
		final int dLat = elevation.polySizeLat;
		
		final int lonOffsetStart = elevation.polySizeLonOffsetStart;
		final int lonOffsetEnd = elevation.polySizeLonOffsetEnd;
		final int latOffsetStart = elevation.polySizeLatOffsetStart;
		final int latOffsetEnd = elevation.polySizeLatOffsetEnd;
		
		// Texture coordinates
		final Layer drawLevel = landscape.globe.getLayer(drawLevelID);
		final float oneOverTileWidth = 1.0f / drawLevel.getTileSize();
		
		float uvLonStart = (subTile.ulx - refLeftLon) * oneOverTileWidth;
		float uvLatStart = (subTile.ulz - refUpLat) * oneOverTileWidth;

//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		int lat1 = startLat;
		double cosY1 = CosTable.cos(lat1) * EARTH_RADIUS;
		double sinY1 = -CosTable.sin(lat1) * EARTH_RADIUS;
		float ty1 = uvLatStart;
		for (int j = 0; j < nLat; j++) {
			final int lat2;
			final float ty2; {
				int curDLat = dLat;
				if(j == 0) {
					curDLat += latOffsetStart;
				}
				if(j == nLat-1) {
					curDLat += latOffsetEnd;
				}
				lat2 = lat1 + curDLat;
				ty2 = ty1 + curDLat * oneOverTileWidth;
			}
			
			double cosY2 = CosTable.cos(lat2) * EARTH_RADIUS;
			double sinY2 = -CosTable.sin(lat2) * EARTH_RADIUS;

			int lon = startLon;
			float tx = uvLonStart;

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int i = 0; i <= nLon; i++) {
				double cosX = CosTable.cos(lon);
				double sinX = CosTable.sin(lon);

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

				int curDLont = dLon; {
					if(i == 0) {
						curDLont += lonOffsetStart;
					}
					if(i == nLon-1) {
						curDLont += lonOffsetEnd;
					}
				}
				lon += curDLont;
				tx += curDLont * oneOverTileWidth;
			}
			gl.glEnd();

			lat1 = lat2;
			cosY1 = cosY2;
			sinY1 = sinY2;

			ty1 = ty2;
		}
//		gl.glEnd();

		if(DEBUG) {
			int numVertices = 2*nLat*(nLon+1);
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 8 + 2 * 4);
		}
	}

	@Override
	protected final void renderSubTile() {
		// Elevation
		final ElevationNone elevation = new ElevationNone(subTile);
		
		final int nLon = elevation.numPolyLon;
		final int nLat = elevation.numPolyLat;
		
		final int startLon = elevation.polyLonStart;
		final int startLat = elevation.polyLatStart;
		
		final int dLon = elevation.polySizeLon;
		final int dLat = elevation.polySizeLat;
		
		final int lonOffsetStart = elevation.polySizeLonOffsetStart;
		final int lonOffsetEnd = elevation.polySizeLonOffsetEnd;
		final int latOffsetStart = elevation.polySizeLatOffsetStart;
		final int latOffsetEnd = elevation.polySizeLatOffsetEnd;

		final boolean useColor = (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM);
		if (useColor) {
			gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
		}

//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		int lat1 = startLat;
		double cosY1 = CosTable.cos(lat1) * EARTH_RADIUS;
		double sinY1 = -CosTable.sin(lat1) * EARTH_RADIUS;
		for (int j = 0; j < nLat; j++) {
			int lat2 = lat1 + dLat;
			if(j == 0) {
				lat2 += latOffsetStart;
			}
			if(j == nLat-1) {
				lat2 += latOffsetEnd;
			}
			double cosY2 = CosTable.cos(lat2) * EARTH_RADIUS;
			double sinY2 = -CosTable.sin(lat2) * EARTH_RADIUS;

			int lon = startLon;

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int i = 0; i <= nLon; i++) {
				double cosX = CosTable.cos(lon);
				double sinX = CosTable.sin(lon);

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

				lon += dLon;
				if(i == 0) {
					lon += lonOffsetStart;
				}
				if(i == nLon-1) {
					lon += lonOffsetEnd;
				}
			}
			gl.glEnd();

			lat1 = lat2;
			cosY1 = cosY2;
			sinY1 = sinY2;
		}
//		gl.glEnd();
//		gl.glColor3f(1, 1, 1);	//This should be done somewhere

		if(DEBUG) {
			int numVertices = 2*nLat*(nLon+1);
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (3 * 8);
		}
	}
}
