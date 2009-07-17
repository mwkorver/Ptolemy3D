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

import java.util.List;
import java.util.Vector;

import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.manager.MapDataManager;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.view.Area;
import org.ptolemy3d.view.Camera;

/**
 * A resolution level.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
public class Layer {
	/** Number of tiles in longitude direction */
	public final static int NUMTILE_LON = 8;
	/** Number of tiles in latitude direction */
	public final static int NUMTILE_LAT = 8;
	/** Total number of tiles in the level. */
	public final static int NUMTILES = NUMTILE_LON * NUMTILE_LAT;

	/** Level id */
	private final int layerID;

	/** List of tiles */
	private Tile[/* LEVEL_NUMTILES */] tiles;

	/** Center longitude */
	private int centerLat;
	/** Center latitude */
	private int centerLon;
	/** Tile's size */
	private final int tileSize;
	/** Layer visibility */
	private boolean visible;
	/** Maximum altitude to render the level */
	private final int maxZoom;
	/** Use for server efficiency storage and speed of map datas */
	private final int divider;
	
	/** Layer areas */
	private final List<Area> areas = new Vector<Area>(2);
	
	/**
	 * Creates a new instance.
	 *
	 * @param levelId
	 * @param tileSize
	 * @param maxVisibility
	 * @param divider
	 */
	public Layer(int levelId, int tileSize, int maxVisibility, int divider) {
		this.layerID = levelId;
		this.maxZoom = maxVisibility;
		this.tileSize = tileSize;
		this.divider = divider;

		this.centerLat = 0;
		this.centerLon = 0;

		visible = false;

		tiles = new Tile[NUMTILES];
		for (int i = 0; i < NUMTILES; i++) {
			tiles[i] = new Tile(i);
		}
	}

	@Override
	public String toString() {
		return super.toString()+"("+layerID+","+tileSize+","+maxZoom+","+divider+")";
	}
	
	/**
	 * Take as input the camera position and assign tiles for each layers.<BR>
	 * <BR>
	 * The center of all tiles is the camera (lon,lat). Surrounding this position are a
	 * square of 8 x 8 tiles. So, (minLon, maxLon) = (lon - 4tile, lon + 4 tile). And the
	 * same with (minLat, maxLat).<BR>
	 * <BR>
	 * The first layer will have 64 tile covering all the earth area. The second layer will have
	 * 64 tiles covering 1/4 of the earth area. Etc ...
	 * <BR>
	 * This is how has been designed the rendering of the earth on the original version of viewer.<BR>
	 * This model can be discussed as lot of parameters are not taken into account (tilt, direction ...).
	 */
	protected void processVisibility(DrawContext drawContext) {
		final Camera camera = drawContext.getCanvas().getCamera();
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final int maxLongitude = landscape.getMaxLongitude();
		final int maxLatitude = landscape.getMaxLatitude();

		// Find bounds
		int latSign = -1;
		int lonIncr = (maxLongitude / tileSize) * tileSize;

		int leftMostTile = -((maxLongitude / tileSize) * tileSize);
		if (leftMostTile != maxLongitude) {
			leftMostTile -= tileSize;
		}

		int minLon;
		minLon = ((int) (camera.getPosition().getLongitudeDD() / tileSize) * tileSize) - (tileSize * (NUMTILE_LON/2));
		if (minLon < leftMostTile) {
			minLon += ((maxLongitude * 2) / tileSize) * tileSize;
		}

		int topTile = (maxLatitude / tileSize) * tileSize;
		if (topTile != maxLatitude) {
			topTile += tileSize;
		}

		int minLat;
		boolean wraps = false;
		if ((tileSize * NUMTILE_LAT) > (maxLatitude * 2)) {
			wraps = true;
			minLat = topTile;
		}
		else {
			minLat = ((int) (camera.getPosition().getLatitudeDD() / tileSize) * tileSize) + (tileSize * (NUMTILE_LAT/2));
			if (minLat > topTile) {
				minLat = topTile - (((minLat - maxLatitude) / tileSize) * tileSize);
				
				if (minLon > 0) {
					minLon -= lonIncr;
				}
				else {
					minLon += lonIncr;
				}
				
				latSign *= -1;
			}
		}
		
		// Level bounds
		centerLon = minLon + (tileSize * (NUMTILE_LON/2));
		centerLat = -(minLat + (tileSize * (NUMTILE_LAT/2)) * latSign);
		
		/*
		 * TODO Clean up needed here !
		 */
		
		// Create tiles
		int k = 0;
		int row = 0;
		int vlat = minLat;
		while (row < NUMTILE_LAT) {
			int col = 0;
			int vlon = minLon;
			while (col < NUMTILE_LON) {
				if (vlon >= maxLongitude) {
					vlon = (-maxLongitude / tileSize) * tileSize;
					if (vlon != maxLongitude) {
						vlon -= tileSize;
					}
				}

				if ((vlon == minLon) && (col != 0)) {
					for (; col < NUMTILE_LON; col++) {
						tiles[k++].visible = false;
					}
				}
				else {
					tiles[k++].processVisibility(camera, this, vlon, -vlat);
					vlon += tileSize;
					col++;
				}
			}
			vlat += tileSize * latSign;

			if (vlat <= -maxLatitude) {
				if (!wraps) {
					vlat += tileSize;
					if (minLon > 0) {
						minLon -= lonIncr;
					}
					else {
						minLon += lonIncr;
					}
					latSign *= -1;
				}
				else {
					for (; k < NUMTILE_LON * NUMTILE_LAT; k++) {
						tiles[k].visible = false;
					}
					break;
				}
			}

			if (vlat > topTile) {
				vlat = topTile;
				if (minLon > 0) {
					minLon -= lonIncr;
				}
				else {
					minLon += lonIncr;
				}
				latSign *= -1;
			}
			row++;
		}
		
		updateAreas();
	}
	/* */
	private void updateAreas() {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final int maxLat = landscape.getMaxLatitude();
		final int topTileLat = getMaxTileLatitude();
		final int halfLon = getHalfGlobeLongitude();
		
		final int latSign = (centerLat < maxLat) && (centerLat > -maxLat) ? -1 : 1;
		
		final int b_ulx_, b_lrx_, b_ulz_, b_lrz_;
		b_ulx_ = centerLon - (tileSize * 4);
		b_lrx_ = centerLon + (tileSize * 4);
		b_ulz_ = centerLat + latSign * (tileSize * 4);
		b_lrz_ = centerLat - latSign * (tileSize * 4);
		
		final boolean centerInBounds = (centerLat <= topTileLat) && (centerLat >= -topTileLat);
		final boolean inBounds = (b_lrz_ <= topTileLat) && (b_lrz_ >= -topTileLat);
		if (centerInBounds && inBounds) {
			/*synchronized(areas) */{
				areas.clear();
				areas.add(new Area(b_ulz_, b_lrz_, b_ulx_, b_lrx_));
			}
		}
		else {
			final boolean northPole = b_ulz_ < 0 && b_lrz_ < 0;
			
			final int b_ulx, b_lrx, b_ulz, b_lrz;
			final int b_ulx2, b_lrx2, b_ulz2, b_lrz2;
			if(northPole) {
				final boolean negZSide = b_ulx_ > 0 && b_lrx_ > 0;
				if(negZSide) {
					//One Half
					b_ulx = b_ulx_ - halfLon;
					b_lrx = b_lrx_ - halfLon;
					b_ulz = -topTileLat;
					b_lrz = -topTileLat - (b_lrz_ + topTileLat) - tileSize;
					//Other half
					b_ulx2 = b_ulx_;
					b_lrx2 = b_lrx_;
					b_ulz2 = -topTileLat;
					b_lrz2 = -topTileLat + (b_ulz_ + topTileLat) + tileSize;
				}
				else {
					//One Half
					b_ulx = b_ulx_;
					b_lrx = b_lrx_;
					b_ulz = -topTileLat;
					b_lrz = -topTileLat + (b_ulz_ + topTileLat) + tileSize;
					//Other half
					b_ulx2 = b_ulx_ + halfLon;
					b_lrx2 = b_lrx_ + halfLon;
					b_ulz2 = -topTileLat;
					b_lrz2 = -topTileLat - (b_lrz_ + topTileLat) - tileSize;
				}
			}
			else {
				final boolean posZSide = b_ulx_ > 0 && b_lrx_ > 0;
				if(posZSide) {
					//One Half
					b_ulx = b_ulx_;
					b_lrx = b_lrx_;
					b_ulz = topTileLat + (b_ulz_ - topTileLat);
					b_lrz = topTileLat;
					//Other half
					b_ulx2 = b_ulx_ - halfLon;
					b_lrx2 = b_lrx_ - halfLon;
					b_ulz2 = topTileLat - (b_lrz_ - topTileLat);
					b_lrz2 = topTileLat;
				}
				else {
					//One Half
					b_ulx = b_ulx_ + halfLon;
					b_lrx = b_lrx_ + halfLon;
					b_lrz = topTileLat;
					b_ulz = topTileLat - (b_lrz_ - topTileLat);
					//Other half
					b_ulx2 = b_ulx_;
					b_lrx2 = b_lrx_;
					b_ulz2 = topTileLat + (b_ulz_ - topTileLat);
					b_lrz2 = topTileLat;
				}
			}
			
			/*synchronized(areas) */{
				areas.clear();
				areas.add(new Area(b_ulz, b_lrz, b_ulx, b_lrx));
				areas.add(new Area(b_ulz2, b_lrz2, b_ulx2, b_lrx2));
			}
		}
	}
	
	protected void processClipping() {
		if(!isVisible()) {
			return;
		}
		
		for (int i = 0; i < NUMTILE_LAT; i++) {
			for (int j = 0; j < NUMTILE_LON; j++) {
				final int tileID = (i * NUMTILE_LON) + j;
				final Tile tile = tiles[tileID];

				final Tile right = (j == (NUMTILE_LON - 1)) ? null : tiles[(i * NUMTILE_LON) + j + 1];
				final Tile below = (i == (NUMTILE_LAT - 1)) ? null : tiles[((i + 1) * NUMTILE_LON) + j];
				final Tile left = (j == 0) ? null : tiles[(i * NUMTILE_LON) + j - 1];
				final Tile above = (i == 0) ? null : tiles[((i - 1) * NUMTILE_LON) + j];
				
				// Display
				tile.processClipping(right, below, left, above);
			}
		}
	}
	
	protected void linkTiles(Layer layerBelow) {
		if(!isVisible() || !layerBelow.isVisible()) {
			return;
		}
		for (int i = 0; i < NUMTILES; i++) {
			final Tile tile = tiles[i];
			tile.linkTiles(layerBelow);
		}
	}
	
	/**
	 * Draw Level geometry
	 */
	public void draw(DrawContext drawContext) {
		if (!visible) {
			return;
		}

		// Render layer tiles
		final GL gl = drawContext.getGL();
		for (Tile tile : tiles) {
			tile.display(gl);
		}
	}

	/** Level Picking */
	public boolean pick(double[] intersectPoint, double[][] ray) {
		// Start with highest res tiles
		for (int i = 0; i < tiles.length; i++) {
			if (tiles[i].pick(intersectPoint, ray)) {
				return true;
			}
		}
		return false;
	}

	/** @return true if found */
	public double[] groundHeight(final double lon, final double lat, final double[][] ray) {
		if (!visible) {
			return null;
		}

		final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();
		final MapData mapData = null;//mapDataManager.request(levelID, (int)lon, (int)lat);	//FIXME
		if (mapData == null) {
			return null;
		}

		final double[] pickArr = {-999, -999, -999};
		if (mapData.dem != null) {
			/*
			 * DEM Elevation
			 */
			final byte[] dem = mapData.dem.demDatas;
			final int numRows = mapData.dem.size;
			final int rowWidth = numRows * 2; // assuming we have a square tile

			float dw = (float) (numRows - 1) / tileSize;
			int xpos = (int) ((lon - mapData.getLon()) * (dw));
			int ypos = (int) ((mapData.getLat() - lat) * (dw));
			float sw = (float) tileSize / (numRows - 1);
			double gminx = mapData.getLon() + xpos * sw;
			double gmaxx = mapData.getLon() + (xpos + 1) * sw;
			double gminy = mapData.getLat() - ypos * sw;
			double gmaxy = mapData.getLat() - (ypos + 1) * sw;

			if (lon <= gminx) {
				gminx = lon - 1;
			}
			if (lon >= gmaxx) {
				gminx = lon + 1;
			}
			if (lat <= gminy) {
				gminy = lat - 1;
			}
			if (lat >= gmaxy) {
				gmaxy = lat + 1;
			}

			Math3D.setTriVert(
					0,
					gminx,
					(dem[(ypos * rowWidth) + (xpos * 2)] << 8) + (dem[((ypos * rowWidth) + (xpos * 2)) + 1] & 0xFF),
					gminy);
			Math3D.setTriVert(1, gmaxx, (dem[(ypos * rowWidth) + ((xpos + 1) * 2)] << 8) + (dem[((ypos * rowWidth) + ((xpos + 1) * 2)) + 1] & 0xFF),
					gminy);
			Math3D.setTriVert(2, gminx, (dem[((ypos + 1) * rowWidth) + (xpos * 2)] << 8) + (dem[(((ypos + 1) * rowWidth) + (xpos * 2)) + 1] & 0xFF),
					gmaxy);
			if (Math3D.rayIntersectTri(pickArr, ray) != 1) {
				// to avoid overlap for ray through tri...
				Math3D.setTriVert(
						0,
						gmaxx,
						(dem[(ypos * rowWidth) + ((xpos + 1) * 2)] << 8) + (dem[((ypos * rowWidth) + ((xpos + 1) * 2)) + 1] & 0xFF),
						gminy);
				Math3D.setTriVert(
						1,
						gminx,
						(dem[((ypos + 1) * rowWidth) + (xpos * 2)] << 8) + (dem[(((ypos + 1) * rowWidth) + (xpos * 2)) + 1] & 0xFF),
						gmaxy);
				Math3D.setTriVert(
						2,
						gmaxx,
						(dem[((ypos + 1) * rowWidth) + ((xpos + 1) * 2)] << 8) + (dem[(((ypos + 1) * rowWidth) + ((xpos + 1) * 2)) + 1] & 0xFF),
						gmaxy);
				if (Math3D.rayIntersectTri(pickArr, ray) != 1) {
					pickArr[1] = 0;
				}
			}

			pickArr[1] *= Unit.getCoordSystemRatio();
			return pickArr; // FIXME Must be just over ?
		}
		else if (mapData.tin != null) {
			/*
			 * TIN Elevation
			 */
			for (int k = 0; (k < mapData.tin.indices.length); k++) {
				if (mapData.tin.indices[k] == null) {
					continue;
				}
				for (int j = 2; (j < mapData.tin.indices[k].length); j++) {
					Math3D.setTriVert(0, mapData.getLon() + (mapData.tin.positions[mapData.tin.indices[k][j - 2]][0]),
							mapData.tin.positions[mapData.tin.indices[k][j - 2]][1], mapData.getLat() - (mapData.tin.positions[mapData.tin.indices[k][j - 2]][2]));
					Math3D.setTriVert(1, mapData.getLon() + (mapData.tin.positions[mapData.tin.indices[k][j - 1]][0]),
							mapData.tin.positions[mapData.tin.indices[k][j - 1]][1], mapData.getLat() - (mapData.tin.positions[mapData.tin.indices[k][j - 1]][2]));
					Math3D.setTriVert(2, mapData.getLon() + (mapData.tin.positions[mapData.tin.indices[k][j]][0]),
							mapData.tin.positions[mapData.tin.indices[k][j]][1], mapData.getLat() - (mapData.tin.positions[mapData.tin.indices[k][j]][2]));
					if (Math3D.rayIntersectTri(pickArr, ray) == 1) {
						pickArr[1] *= Unit.getCoordSystemRatio();
						return pickArr;
					}
				}
			}
		}
		return null;
	}
	
	public int getHalfGlobeLongitude() {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final int maxLon = landscape.getMaxLongitude();
		final int halfLon = (maxLon / tileSize) * tileSize;
		return halfLon;
		
	}
	
	public int getMaxTileLatitude() {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final int maxLat = landscape.getMaxLatitude();
		
		int maxTileLat = (maxLat / tileSize) * tileSize;
		if (maxTileLat != maxLat) {
			maxTileLat += tileSize;
		}
		return maxTileLat;
	}
	
	// Getters
	/**
	 * Level ID
	 * @return the level id (first is 0)
	 */
	public int getLevelID() {
		return layerID;
	}

	/**
	 * Get the Tile at a specified index.
	 * @return the Tile at the specified index
	 */
	public Tile getTile(int index) {
		return tiles[index];
	}

	/**
	 * Tile size.
	 * @return tile width and height
	 */
	public int getTileSize() {
		return tileSize;
	}
	
	/**
	 * @return the tiles
	 */
	public Tile[/* LEVEL_NUMTILES */] getTiles() {
		return tiles;
	}

	/** @return the visible */
	public boolean isVisible() {
		return visible;
	}

	/** @return the maxZoom */
	public int getMaxZoom() {
		return maxZoom;
	}

	/** @return the divider */
	public int getDivider() {
		return divider;
	}

	public int getCenterLat() {
		return centerLat;
	}

	public int getCenterLon() {
		return centerLon;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public List<Area> getAreas() {
		return areas;
	}
}
