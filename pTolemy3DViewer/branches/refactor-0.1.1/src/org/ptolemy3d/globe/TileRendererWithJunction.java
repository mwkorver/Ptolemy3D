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
import org.ptolemy3d.debug.Config;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.math.CosTable;
import org.ptolemy3d.scene.Landscape;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
class TileRendererWithJunction extends TileRenderer {
	@Override
	protected void renderSubTile_Dem(int xStart, int zStart, int xEnd, int zEnd) {
		if(!Config.DEBUG || !landscape.isTerrainEnabled() || !ProfilerUtil.test) {
			super.renderSubTile_Dem(xStart, zStart, xEnd, zEnd);
			return;
		}
		
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

			final boolean edgeLat = (latID == 0);
			final boolean edgeLat2 = (latID == nLat - 1);
			
			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			int lon = xStart;
			for (int lonID = 0; lonID < nLon; lonID++) {
				final boolean edgeLon = (lonID == 0) || (lonID == nLon - 1);
				
				double dy1, dy2;
				dy1 = subTile.getElevation(lon, lat, edgeLon || edgeLat);
				dy2 = subTile.getElevation(lon, lat2, edgeLon || edgeLat2);
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
						//dy2 = ;
					}
					else if ((bottom_dem_slope != -1) && (latID == (nLat - 1))) {
						//dy1 = ;
						dy2 = ll_corner + (lonID * bottom_dem_slope);
					}
				}

				dy1 *= dyScaler;
				dy2 *= dyScaler;

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
	
	@Override
	protected void renderSubTile_Textured() {
		if(!Config.DEBUG || !landscape.isTerrainEnabled() || !ProfilerUtil.test) {
			super.renderSubTile_Textured();
			return;
		}
		
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
		
		final double dyScaler = Unit.getCoordSystemRatio() * terrainScaler;
		
//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		int lat1 = startLat;
		double cosY1 = CosTable.cos(lat1);
		double sinY1 = -CosTable.sin(lat1);
		float ty1 = uvLatStart;
		for (int latID = 0; latID < nLat; latID++) {
			final int lat2;
			final float ty2; {
				int curDLat = dLat;
				if(latID == 0) {
					curDLat += latOffsetStart;
				}
				if(latID == nLat-1) {
					curDLat += latOffsetEnd;
				}
				lat2 = lat1 + curDLat;
				ty2 = ty1 + curDLat * oneOverTileWidth;
			}
			
			double cosY2 = CosTable.cos(lat2);
			double sinY2 = -CosTable.sin(lat2);

			final boolean edgeLat = (latID == 0);
			final boolean edgeLat2 = (latID == nLat - 1);
			
			int lon = startLon;
			float tx = uvLonStart;

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int lonID = 0; lonID <= nLon; lonID++) {
				double cosX = CosTable.cos(lon);
				double sinX = CosTable.sin(lon);
				
				final boolean edgeLon = (lonID == 0) || (lonID == nLon);
				double dy1 = subTile.getElevation(lon, lat1, edgeLon || edgeLat) * dyScaler;
				double dy2 = subTile.getElevation(lon, lat2, edgeLon || edgeLat2) * dyScaler;
				
				dy1 += EARTH_RADIUS;
				dy2 += EARTH_RADIUS;
				
				double pt1X, pt1Y, pt1Z;
				pt1X = cosY1 * sinX * dy1;
				pt1Y = sinY1        * dy1;
				pt1Z = cosY1 * cosX * dy1;

				double pt2X, pt2Y, pt2Z;
				pt2X = cosY2 * sinX * dy2;
				pt2Y = sinY2        * dy2;
				pt2Z = cosY2 * cosX * dy2;

				gl.glTexCoord2f(tx, ty1);
				gl.glVertex3d(pt1X, pt1Y, pt1Z);

				gl.glTexCoord2f(tx, ty2);
				gl.glVertex3d(pt2X, pt2Y, pt2Z);

				int curDLont = dLon; {
					if(lonID == 0) {
						curDLont += lonOffsetStart;
					}
					if(lonID == nLon-1) {
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
	protected void renderSubTile() {
		if(!Config.DEBUG || !landscape.isTerrainEnabled() || !ProfilerUtil.test) {
			super.renderSubTile();
			return;
		}
		
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

		final double dyScaler = Unit.getCoordSystemRatio() * terrainScaler;
		final boolean useColor = (landscape.getDisplayMode() == Landscape.DISPLAY_SHADEDDEM);
		if (useColor) {
			gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
		}

//		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		int lat1 = startLat;
		double cosY1 = CosTable.cos(lat1);
		double sinY1 = -CosTable.sin(lat1);
		for (int latID = 0; latID < nLat; latID++) {
			int lat2 = lat1 + dLat;
			if(latID == 0) {
				lat2 += latOffsetStart;
			}
			if(latID == nLat-1) {
				lat2 += latOffsetEnd;
			}
			double cosY2 = CosTable.cos(lat2);
			double sinY2 = -CosTable.sin(lat2);

			final boolean edgeLat = (latID == 0);
			final boolean edgeLat2 = (latID == nLat - 1);
			
			int lon = startLon;
			gl.glBegin(GL.GL_TRIANGLE_STRIP);
			for (int lonID = 0; lonID <= nLon; lonID++) {
				double cosX = CosTable.cos(lon);
				double sinX = CosTable.sin(lon);
				
				final boolean edgeLon = (lonID == 0) || (lonID == nLon);
				double dy1 = subTile.getElevation(lon, lat1, edgeLon || edgeLat) * dyScaler;
				double dy2 = subTile.getElevation(lon, lat2, edgeLon || edgeLat2) * dyScaler;
				
				dy1 += EARTH_RADIUS;
				dy2 += EARTH_RADIUS;

				double pt1X, pt1Y, pt1Z;
				pt1X = cosY1 * sinX * dy1;
				pt1Y = sinY1        * dy1;
				pt1Z = cosY1 * cosX * dy1;

				double pt2X, pt2Y, pt2Z;
				pt2X = cosY2 * sinX * dy2;
				pt2Y = sinY2        * dy2;
				pt2Z = cosY2 * cosX * dy2;
				
				gl.glVertex3d(pt1X, pt1Y, pt1Z);
				gl.glVertex3d(pt2X, pt2Y, pt2Z);

				lon += dLon;
				if(lonID == 0) {
					lon += lonOffsetStart;
				}
				if(lonID == nLon-1) {
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
