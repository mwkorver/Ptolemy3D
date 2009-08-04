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
import org.ptolemy3d.globe.TileArea;
import org.ptolemy3d.globe.Tile.ITileRenderer;
import org.ptolemy3d.math.CosTable;
import org.ptolemy3d.scene.Landscape;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
class TileRenderer implements ITileRenderer {
	/* All of that are temporary datas that are used in drawSubsection.
	 * They must be restore in each entering */

	protected GL gl;
	protected MapData mapData;

	//From the Tile
	protected TileArea subTile;
	protected int drawLevelID;
	protected int layerID;
	protected Tile leftTile;
	protected Tile aboveTile;
	protected Tile rightTile;
	protected Tile belowTile;
	protected int refLeftLon, refUpLat;
	protected int refRightLon, refLowLat;
	protected int tileSize;
	protected Landscape landscape;
	protected float[] tileColor;
	protected float[] colratios;
	protected double terrainScaler;
	
	protected void fillLocalVariables(TileArea subTile) {
		final Tile tile = subTile.tile;
		
		gl = tile.gl;

		landscape = Ptolemy3D.getScene().getLandscape();
		tileColor = landscape.getTileColor();
		colratios = landscape.getColorRatios();
		terrainScaler = landscape.getTerrainScaler();
		
		this.mapData = tile.mapData;
		this.subTile = subTile;
		
		drawLevelID = tile.drawLevelID;
		layerID = tile.getLayerID();
		tileSize = tile.getTileSize();
		try {
			refLeftLon = tile.getReferenceLeftLongitude();
			refUpLat = tile.getReferenceUpperLatitude();
			refRightLon = tile.getReferenceRightLongitude();
			refLowLat = tile.getReferenceLowerLatitude();
		}
		catch(RuntimeException e) {
			System.out.println(subTile.tile.visible+" "+subTile.active);
			throw e;
		}
	}

	public final void renderSubTile(TileArea subTile) {
		if (subTile.left.size() > 0) {
			leftTile = subTile.left.get(0).tile;
		} else { leftTile = null;}
		if (subTile.above.size() > 0) {
			aboveTile = subTile.above.get(0).tile;
		} else { aboveTile = null;}
		if (subTile.right.size() > 0) {
			rightTile = subTile.right.get(0).tile;
		} else { rightTile = null;}
		if (subTile.below.size() > 0) {
			belowTile = subTile.below.get(0).tile;
		} else { belowTile = null;}
		
		final int x1 = subTile.ulx;
		final int z1 = subTile.ulz;
		final int x2 = subTile.lrx;
		final int z2 = subTile.lrz;
		if ((x1 == x2) || (z1 == z2)) {
			return;
		}

		if (DEBUG) {
			if (!ProfilerUtil.renderTiles) {
				return;
			}
			ProfilerUtil.tileSectionCounter++;
		}
		fillLocalVariables(subTile);

		boolean lookForElevation = (mapData != null) && (landscape.isTerrainEnabled());
		boolean useDem = lookForElevation && (mapData.dem != null);
		boolean useTin = lookForElevation && (mapData.tin != null);

		boolean texture = loadGLState(subTile.tile.texture);
		if (texture) {
			if (useDem) {
				renderSubTile_DemTextured(x1, z1, x2, z2);
			}
			else if (useTin) {
				renderSubTile_Tin(true, x1, z1, x2, z2);
			}
			else {
				renderSubTile_Textured();
			}
		}
		else {
			if (useDem) {
				renderSubTile_Dem(x1, z1, x2, z2);
			}
			else if (useTin) {
				renderSubTile_Tin(false, x1, z1, x2, z2);
			}
			else {
				renderSubTile();
			}
		}
	}

	protected void renderSubTile_DemTextured(int xStart, int zStart, int xEnd, int zEnd) {
		final Layer drawLevel = landscape.globe.getLayer(drawLevelID);
		final ElevationDem dem = mapData.dem;
		final int numRows = mapData.dem.getNumRows();

		final float tex_inc = 1.0f / (numRows - 1);

		final boolean xsinterpolate, xeinterpolate, zsinterpolate, zeinterpolate;
		final double startxcoord, startzcoord, endxcoord, endzcoord;
		final int nLon, nLat;
		final int startx, startz;
		{
			int endx, endz;
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
			
			nLon = endx - startx;
			nLat = endz - startz;
		}

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawLevelID == layerID);
		if (eqZLevel && (xStart == refLeftLon) && (xEnd == refRightLon) && (zStart == refUpLat) && (zEnd == refLowLat)) {
			ul_corner = dem.getUpLeftCorner();
			ur_corner = dem.getUpRightCorner();
			ll_corner = dem.getLowerLeftCorner();
			lr_corner = dem.getLowerRightCorner();

			final double oneOverNrowsX = 1.0 / (nLon - 1);
			final double oneOverNrowsZ = 1.0 / nLat;

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

		final int dx = (xEnd - xStart) / (nLon - 1);
		final int dz = (zEnd - zStart) / nLat;

		final double dyScaler = Unit.getCoordSystemRatio() * terrainScaler;

		int lat = zStart;
		double cosZ = CosTable.cos(lat);
		double sinZ = CosTable.sin(lat);

//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int latID = 0; latID < nLat; latID++) {
			final int lat2 = lat + dz;

			double cos2Z = CosTable.cos(lat2);
			double sin2Z = CosTable.sin(lat2);

			final float tex_z, tex_z2;
			if (zsinterpolate && (latID == 0)) {
				tex_z = (float) (tex_inc * startzcoord);
			}
			else {
				tex_z = tex_inc * (latID + startz);
			}
			if (zeinterpolate && (latID == (nLat - 1))) {
				tex_z2 = (float) (tex_inc * endzcoord);
			}
			else {
				tex_z2 = tex_inc * (latID + startz + 1);
			}

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int lon = xStart;
			for (int lonID = 0; lonID < nLon; lonID++) {
				final float tex_x;
				if (xsinterpolate && (lonID == 0)) {
					tex_x = (float) (tex_inc * startxcoord);
				}
				else if (xeinterpolate && (lonID == (nLon - 1))) {
					tex_x = (float) (tex_inc * (endxcoord - 1));
				}
				else {
					tex_x = (lonID + startx) * tex_inc;
				}

				double dy1, dy2;
				if (eqZLevel) {
					if ((left_dem_slope != -1) && (lonID == 0)) {
						dy1 = ul_corner + (latID * left_dem_slope);
						dy2 = dy1 + left_dem_slope;
					}
					else if ((right_dem_slope != -1) && (lonID == (nLon - 1))) {
						dy1 = ur_corner + (latID * right_dem_slope);
						dy2 = dy1 + right_dem_slope;
					}
					else if ((top_dem_slope != -1) && (latID == 0)) {
						dy1 = ul_corner + (lonID * top_dem_slope);
						dy2 = dem.getElevation(lon, lat2).height;
					}
					else if ((bottom_dem_slope != -1) && (latID == (nLat - 1))) {
						dy1 = dem.getElevation(lon, lat).height;
						dy2 = ll_corner + (lonID * bottom_dem_slope);
					}
					else {
						dy1 = dem.getElevation(lon, lat).height;
						dy2 = dem.getElevation(lon, lat2).height;
					}
				}
				else {
					dy1 = dem.getElevation(lon, lat).height;
					dy2 = dem.getElevation(lon, lat2).height;
				}

				double cx1, cy1, cz1, cx2, cy2, cz2;
				{
					final double dy1E = (dy1 * dyScaler) + EARTH_RADIUS;
					final double dy2E = (dy2 * dyScaler) + EARTH_RADIUS;

					final double cosX = CosTable.cos(lon);
					final double sinX = CosTable.sin(lon);

					cx1 =  dy1E * cosZ * sinX;
					cx2 =  dy2E * cos2Z * sinX;
					cy1 = -dy1E * sinZ;
					cy2 = -dy2E * sin2Z;
					cz1 =  dy1E * cosZ * cosX;
					cz2 =  dy2E * cos2Z * cosX;
				}

				gl.glTexCoord2f(tex_x, tex_z);
				gl.glVertex3d(cx1, cy1, cz1);

				gl.glTexCoord2f(tex_x, tex_z2);
				gl.glVertex3d(cx2, cy2, cz2);

				lon += dx;
			}
			gl.glEnd();

			lat = lat2;
			cosZ = cos2Z;
			sinZ = sin2Z;
		}
//		gl.glEnd();

		if (DEBUG) {
			int numVertices = 2 * nLon * nLat;
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}

	protected void renderSubTile_Dem(int xStart, int zStart, int xEnd, int zEnd) {
		final Layer drawLevel = landscape.globe.getLayer(drawLevelID);
		final ElevationDem dem = mapData.dem;
		final int numRows = mapData.dem.getNumRows();
		final boolean useColor = (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM);

		final double startxcoord, startzcoord, endxcoord, endzcoord;
		final int nLon, nLat;
		{
			int startx, startz, endx, endz;
			final double geom_inc = (double) (numRows - 1) / drawLevel.getTileSize();

			startxcoord = ((xStart - refLeftLon) * geom_inc);
			startx = (int) startxcoord;

			startzcoord = ((zStart - refUpLat) * geom_inc);
			startz = (int) startzcoord;

			endxcoord = ((xEnd - refLeftLon) * geom_inc) + 1;
			endx = (int) endxcoord;
			if (endxcoord != endx) {
				endx++;
			}

			endzcoord = ((zEnd - refUpLat) * geom_inc);
			endz = (int) endzcoord;
			if (endzcoord != endz) {
				endz++;
			}
			
			nLon = endx - startx;
			nLat = endz - startz;
		}

		int ul_corner = 0, ur_corner = 0, ll_corner = 0, lr_corner = 0;
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;
		final boolean eqZLevel = (drawLevelID == layerID);
		if (eqZLevel && (xStart == refLeftLon) && (xEnd == refRightLon) && (zStart == refUpLat) && (zEnd == refLowLat)) {
			ul_corner = dem.getUpLeftCorner();
			ur_corner = dem.getUpRightCorner();
			ll_corner = dem.getLowerLeftCorner();
			lr_corner = dem.getLowerRightCorner();

			final double oneOverNrowsX = 1.0 / (nLon - 1);
			final double oneOverNrowsZ = 1.0 / nLat;

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

		int dx = (xEnd - xStart) / (nLon - 1);
		int dz = (zEnd - zStart) / nLat;

		final double dyScaler = Unit.getCoordSystemRatio() * terrainScaler;

		int lat = zStart;
		double cosZ = CosTable.cos(lat);
		double sinZ = CosTable.sin(lat);

//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		for (int latID = 0; latID < nLat; latID++) {
			final int lat2 = lat + dz;

			double cos2Z = CosTable.cos(lat2);
			double sin2Z = CosTable.sin(lat2);

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int lon = xStart;
			for (int lonID = 0; lonID < nLon; lonID++) {
				double dy1, dy2;
				if (eqZLevel) {
					if ((left_dem_slope != -1) && (lonID == 0)) {
						dy1 = ul_corner + (latID * left_dem_slope);
						dy2 = dy1 + left_dem_slope;
					}
					else if ((right_dem_slope != -1) && (lonID == (nLon - 1))) {
						dy1 = ur_corner + (latID * right_dem_slope);
						dy2 = dy1 + right_dem_slope;
					}
					else if ((top_dem_slope != -1) && (latID == 0)) {
						dy1 = ul_corner + (lonID * top_dem_slope);
						dy2 = dem.getElevation(lon, lat2).height;
					}
					else if ((bottom_dem_slope != -1) && (latID == (nLat - 1))) {
						dy1 = dem.getElevation(lon, lat).height;
						dy2 = ll_corner + (lonID * bottom_dem_slope);
					}
					else {
						dy1 = dem.getElevation(lon, lat).height;
						dy2 = dem.getElevation(lon, lat2).height;
					}
				}
				else {
					dy1 = dem.getElevation(lon, lat).height;
					dy2 = dem.getElevation(lon, lat2).height;
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

				if (useColor) {
					setColor((float) dy1);
				}
				gl.glVertex3d(cx1, cy1, cz1);

				if (useColor) {
					setColor((float) dy2);
				}
				gl.glVertex3d(cx2, cy2, cz2);

				lon += dx;
			}
			gl.glEnd();

			lat = lat2;
			cosZ = cos2Z;
			sinZ = sin2Z;
		}
//		gl.glEnd();

		if (DEBUG) {
			int numVertices = 2 * nLon * nLat;
			ProfilerUtil.vertexCounter += numVertices;
			ProfilerUtil.vertexMemoryUsage += numVertices * (2 * 4 + 3 * 8);
		}
	}

	protected void renderSubTile_Textured() {
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

	protected void renderSubTile() {
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

	protected void renderSubTile_Tin(boolean texture, int x1, int z1, int x2, int z2) {
		double left_dem_slope = -1, right_dem_slope = -1, top_dem_slope = -1, bottom_dem_slope = -1;

		// corners clockwise, from ul
		if ((leftTile == null) || (rightTile.mapData != null && leftTile.mapData.key.layer != drawLevelID)) {
			left_dem_slope = (double) (mapData.tin.positions[3][1] - mapData.tin.positions[0][1]) / mapData.tin.w;
		}
		if ((rightTile == null) || (rightTile.mapData != null && rightTile.mapData.key.layer != drawLevelID)) {
			right_dem_slope = (double) (mapData.tin.positions[2][1] - mapData.tin.positions[1][1]) / mapData.tin.w;
		}
		if ((aboveTile == null) || (aboveTile.mapData != null && aboveTile.mapData.key.layer != drawLevelID)) {
			top_dem_slope = (double) (mapData.tin.positions[1][1] - mapData.tin.positions[0][1]) / mapData.tin.w;
		}
		if ((belowTile == null) || (belowTile.mapData != null && belowTile.mapData.key.layer != drawLevelID)) {
			bottom_dem_slope = (double) (mapData.tin.positions[2][1] - mapData.tin.positions[3][1]) / mapData.tin.w;
		}

		double theta1, dThetaOverW;
		double phi1, dPhiOverW;
		{
			double phi2, theta2;
			
			theta1 = x1 * Unit.DD_TO_RADIAN; // startx
			theta2 = x2 * Unit.DD_TO_RADIAN; // endx

			phi1 = z1 * Unit.DD_TO_RADIAN; // starty
			phi2 = z2 * Unit.DD_TO_RADIAN; // endy

			double oneOverW = 1.0 / mapData.tin.w;
			dThetaOverW = (theta2 - theta1) * oneOverW;
			dPhiOverW = (phi2 - phi1) * oneOverW;
		}

		float oneOverW = 1.0f / mapData.tin.w;
		for (int i = 0; i < mapData.tin.indices.length; i++) {
			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int j = 0; j < mapData.tin.indices[i].length; j++) {
				int v = mapData.tin.indices[i][j];

				double dx, dy, dz;
				{
					final float[] p = mapData.tin.positions[v];
					final float tinW = mapData.tin.w;

					dy = p[1];
					if ((left_dem_slope != -1) && (p[0] == 0)) {
						dy = mapData.tin.positions[0][1] + (p[2] * left_dem_slope);
					}
					if ((right_dem_slope != -1) && (p[0] == tinW)) {
						dy = mapData.tin.positions[1][1] + (p[2] * right_dem_slope);
					}
					if ((top_dem_slope != -1) && (p[2] == 0)) {
						dy = mapData.tin.positions[0][1] + (p[0] * top_dem_slope);
					}
					if ((bottom_dem_slope != -1) && (p[2] == tinW)) {
						dy = mapData.tin.positions[3][1] + (p[0] * bottom_dem_slope);
					}
					dy *= terrainScaler;

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

					tx =  dyE * cosZ * sinX;
					ty = -dyE * sinZ;
					tz =  dyE * cosZ * cosX;
				}

				if (texture) {
					gl.glTexCoord2f((mapData.tin.positions[v][0] * oneOverW), (mapData.tin.positions[v][2] * oneOverW));
				}
				else if (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM) {
					setColor((float) dy);
				}
				gl.glVertex3d(tx, ty, tz);
			}
			gl.glEnd();

			if (DEBUG) {
				int numVertices = mapData.tin.indices[i].length;
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

	protected final void setColor(float y) {
		if (y > landscape.getMaxColorHeight()) {
			y = landscape.getMaxColorHeight();
		}

		gl.glColor3f(tileColor[0] + y * colratios[0],
				tileColor[1] + y * colratios[1],
				tileColor[2] + y * colratios[2]);
	}

	protected final void setJP2ResolutionColor() {
		if (DEBUG) {
			int tileRes = (mapData == null) ? -1 : mapData.mapResolution;
			switch (tileRes) {
				case 0:
					gl.glColor3f(1.0f, 0.0f, 0.0f);
					break;
				case 1:
					gl.glColor3f(0.0f, 1.0f, 0.0f);
					break;
				case 2:
					gl.glColor3f(0.0f, 0.0f, 1.0f);
					break;
				case 3:
					gl.glColor3f(1.0f, 1.0f, 1.0f);
					break;
				default:
					gl.glColor3f(0.5f, 0.5f, 0.5f);
				break;
			}
		}
	}

	protected final void setLevelIDColor() {
		if (DEBUG) {
			int levelID = (mapData == null) ? 0 : mapData.getLevel();
			float scaler = 1 - (levelID / 3) / 3.0f;
			switch (1 + levelID % 3) {
				default:
					gl.glColor3f(0.25f, 0.25f, 0.25f);
				break;
				case 1:
					gl.glColor3f(scaler, 0.0f, 0.0f);
					break;
				case 2:
					gl.glColor3f(0.0f, scaler, 0.0f);
					break;
				case 3:
					gl.glColor3f(0.0f, 0.0f, scaler);
					break;
			}
		}
	}

	protected final void setTileIDColor() {
		if (DEBUG) {
			final float C0 = 0.0f;
			final float C1 = 1.0f;
			final float C2 = 0.8f;
			final float C3 = 0.6f;
			final float C4 = 0.4f;

			int tileID = ProfilerUtil.tileCounter;

			float scaler = 1 - (tileID / 27) / 8.0f;
			tileID = tileID % 27;
			switch (tileID) {
				case 0:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C0);
					break;
				case 1:
					gl.glColor3f(scaler * C1, scaler * C2, scaler * C0);
					break;
				case 2:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C2);
					break;
				case 3:
					gl.glColor3f(scaler * C2, scaler * C0, scaler * C1);
					break;
				case 4:
					gl.glColor3f(scaler * C0, scaler * C0, scaler * C1);
					break;
				case 5:
					gl.glColor3f(scaler * C0, scaler * C2, scaler * C1);
					break;
				case 6:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C2);
					break;
				case 7:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;
				case 8:
					gl.glColor3f(scaler * C2, scaler * C1, scaler * C0);
					break;

				case 9:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C0);
					break;
				case 10:
					gl.glColor3f(scaler * C1, scaler * C3, scaler * C0);
					break;
				case 11:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C3);
					break;
				case 12:
					gl.glColor3f(scaler * C3, scaler * C0, scaler * C1);
					break;
				case 13:
					gl.glColor3f(scaler * C0, scaler * C0, scaler * C1);
					break;
				case 14:
					gl.glColor3f(scaler * C0, scaler * C3, scaler * C1);
					break;
				case 15:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C3);
					break;
				case 16:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;
				case 17:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;

				case 18:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C0);
					break;
				case 19:
					gl.glColor3f(scaler * C1, scaler * C4, scaler * C0);
					break;
				case 20:
					gl.glColor3f(scaler * C1, scaler * C0, scaler * C4);
					break;
				case 21:
					gl.glColor3f(scaler * C4, scaler * C0, scaler * C1);
					break;
				case 22:
					gl.glColor3f(scaler * C0, scaler * C0, scaler * C1);
					break;
				case 23:
					gl.glColor3f(scaler * C0, scaler * C4, scaler * C1);
					break;
				case 24:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C4);
					break;
				case 25:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;
				case 26:
					gl.glColor3f(scaler * C0, scaler * C1, scaler * C0);
					break;
			}
		}
	}

	protected final boolean loadGLState(boolean texture) {
		final byte displayMode = landscape.getDisplayMode();
		if (displayMode == Landscape.DISPLAY_JP2RES) {
			setJP2ResolutionColor();
			return false;
		}
		else if (displayMode == Landscape.DISPLAY_TILEID || displayMode == Landscape.DISPLAY_TILENEIGHBOUR) {
			setTileIDColor();
			return false;
		}
		else if (displayMode == Landscape.DISPLAY_LEVELID) {
			setLevelIDColor();
			return false;
		}
		else if (displayMode == Landscape.DISPLAY_MESH || displayMode == Landscape.DISPLAY_SHADEDDEM) {
			return false;
		}
		if (texture) {
			gl.glEnable(GL.GL_TEXTURE_2D);
			return true;
		}
		else {
			gl.glDisable(GL.GL_TEXTURE_2D);
			return false;
		}
	}
}
