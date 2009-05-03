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

import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.manager.MapDataManager;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.view.Camera;

/**
 * A resolution level.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Layer {
	/** Number of tiles in longitude direction */
	public final static int LEVEL_NUMTILE_LON = 8;
	/** Number of tiles in latitude direction */
	public final static int LEVEL_NUMTILE_LAT = 8;
	/** Total number of tiles in the level. */
	public final static int LEVEL_NUMTILES = LEVEL_NUMTILE_LON * LEVEL_NUMTILE_LAT;

	/** Level id */
	private final int levelID;

	/** List of tiles */
	private Tile[/* LEVEL_NUMTILES */] tiles;

	/** Upper left corner latitude */
	private int upLeftLon;
	/** Upper left corner longitude */
	private int upLeftLat;
	/** Upper right corner longitude */
	private int lowRightLon;
	/** Upper right corner latitude */
	private int lowRightLat;
	/** Tile's size */
	private final int tileSize;
	/** Layer visibility */
	private boolean visible;
	/** Maximum altitude to render the level */
	private final int maxZoom;
	/** Minimum altitude to render the level */
	private final int minZoom;
	/** Use for server efficiency storage and speed of map datas */
	private final int divider;

	/**
	 * Creates a new instance.
	 *
	 * @param levelId
	 * @param tileSize
	 * @param minZoom
	 * @param maxZoom
	 * @param divider
	 */
	public Layer(int levelId, int tileSize, int minZoom, int maxZoom, int divider) {
		this.levelID = levelId;
		this.minZoom = minZoom;
		this.maxZoom = maxZoom;
		this.tileSize = tileSize;
		this.divider = divider;

		this.upLeftLon = 0;
		this.upLeftLat = 0;
		this.lowRightLon = 0;
		this.lowRightLat = 0;

		visible = false;

		tiles = new Tile[LEVEL_NUMTILES];
		for (int i = 0; i < LEVEL_NUMTILES; i++) {
			tiles[i] = new Tile(i);
		}
	}

	@Override
	public String toString() {
		return super.toString()+"("+levelID+","+tileSize+","+minZoom+","+maxZoom+","+divider+")";
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
		final Landscape landscape = Ptolemy3D.getScene().landscape;
		final int maxLongitude = landscape.getMaxLongitude();
		final int maxLatitude = landscape.getMaxLatitude();

		// Find bounds
		int ySign = -1;
		int lonIncr = (maxLongitude / tileSize) * tileSize;

		int leftMostTile = -((maxLongitude / tileSize) * tileSize);
		if (leftMostTile != maxLongitude) {
			leftMostTile -= tileSize;
		}

		int minLon, maxLon;
		minLon = ((int) (camera.getPosition().getLongitudeDD() / tileSize) * tileSize) - (tileSize * 4);
		if (minLon < leftMostTile) {
			minLon += ((maxLongitude * 2) / tileSize) * tileSize;
		}
		maxLon = minLon + (tileSize * LEVEL_NUMTILE_LON);

		int topTile = (maxLatitude / tileSize) * tileSize;
		if (topTile != maxLatitude) {
			topTile += tileSize;
		}

		int minLat, maxLat;
		boolean wraps = false;
		if ((tileSize * LEVEL_NUMTILE_LAT) > (maxLatitude * 2)) {
			wraps = true;
			minLat = topTile;
		}
		else {
			minLat = ((int) (camera.getPosition().getLatitudeDD() / tileSize) * tileSize) + (tileSize * 4);
			if (minLat > topTile) {
				minLat = topTile - (((minLat - maxLatitude) / tileSize) * tileSize);
				if (minLon > 0) {
					minLon -= lonIncr;
				}
				else {
					minLon += lonIncr;
				}
				ySign *= -1;
			}
		}
		maxLat = minLat + (tileSize * LEVEL_NUMTILE_LAT) * ySign;

		// Level bounds
		upLeftLon = minLon;
		upLeftLat = -minLat;
		lowRightLon = maxLon;
		lowRightLat = -maxLat;

		//Correct tiles
		int k = 0;
		int row = 0;
		int vlat = minLat;
		while (row < LEVEL_NUMTILE_LAT) {
			int col = 0;
			int vlon = minLon;
			while (col < LEVEL_NUMTILE_LON) {
				final int landscapeMaxAlt = landscape.getMaxLongitude();
				if (vlon >= landscapeMaxAlt) {
					vlon = (-landscapeMaxAlt / tileSize) * tileSize;
					if (vlon != landscapeMaxAlt) {
						vlon -= tileSize;
					}
				}

				if ((vlon == minLon) && (col != 0)) {
					for (; col < LEVEL_NUMTILE_LON; col++) {
						tiles[k++].visible = false;
					}
				}
				else {
					tiles[k++].processVisibility(camera, this, vlon, -vlat);
					vlon += tileSize;
					col++;
				}
			}
			vlat += tileSize * ySign;

			if (vlat <= -landscape.getMaxLatitude()) {
				if (!wraps) {
					vlat += tileSize;
					if (minLon > 0) {
						minLon -= lonIncr;
					}
					else {
						minLon += lonIncr;
					}
					ySign *= -1;
				}
				else {
					for (; k < LEVEL_NUMTILE_LON * LEVEL_NUMTILE_LAT; k++) {
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
				ySign *= -1;
			}
			row++;
		}
	}


	/**
	 * Draw Level geometry
	 */
	public void draw(DrawContext drawContext) {
		GL gl = drawContext.getGL();

		if (!visible) {
			return;
		}

		// Render layer tiles
		for (int i = 0; i < LEVEL_NUMTILE_LAT; i++) {
			for (int j = 0; j < LEVEL_NUMTILE_LON; j++) {
				final int tileID = (i * LEVEL_NUMTILE_LON) + j;
				final Tile tile = tiles[tileID];

				// Display
				tile.display(gl,
						(j == (LEVEL_NUMTILE_LON - 1)) ? null : tiles[(i * LEVEL_NUMTILE_LON) + j + 1],
								(i == (LEVEL_NUMTILE_LAT - 1)) ? null : tiles[((i + 1) * LEVEL_NUMTILE_LON) + j],
										(j == 0) ? null : tiles[(i * LEVEL_NUMTILE_LON) + j - 1], (i == 0) ? null : tiles[((i - 1) * LEVEL_NUMTILE_LON) + j]);
			}
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
		final MapData mapData = null;//mapDataManager.getMapDataHeader(levelID, tileSize, (int)lon, (int)lat);
		if (mapData == null) {
			return null;
		}

		final double[] pickArr = {-999, -999, -999};
		if (mapData.dem != null) {
			/*
			 * DEM Elevation
			 */
			final byte[] dem = mapData.dem.demDatas;
			final int numRows = mapData.dem.numRows;
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
			for (int k = 0; (k < mapData.tin.nSt.length); k++) {
				if (mapData.tin.nSt[k] == null) {
					continue;
				}
				for (int j = 2; (j < mapData.tin.nSt[k].length); j++) {
					Math3D.setTriVert(0, mapData.getLon() + (mapData.tin.p[mapData.tin.nSt[k][j - 2]][0]),
							mapData.tin.p[mapData.tin.nSt[k][j - 2]][1], mapData.getLat() - (mapData.tin.p[mapData.tin.nSt[k][j - 2]][2]));
					Math3D.setTriVert(1, mapData.getLon() + (mapData.tin.p[mapData.tin.nSt[k][j - 1]][0]),
							mapData.tin.p[mapData.tin.nSt[k][j - 1]][1], mapData.getLat() - (mapData.tin.p[mapData.tin.nSt[k][j - 1]][2]));
					Math3D.setTriVert(2, mapData.getLon() + (mapData.tin.p[mapData.tin.nSt[k][j]][0]),
							mapData.tin.p[mapData.tin.nSt[k][j]][1], mapData.getLat() - (mapData.tin.p[mapData.tin.nSt[k][j]][2]));
					if (Math3D.rayIntersectTri(pickArr, ray) == 1) {
						pickArr[1] *= Unit.getCoordSystemRatio();
						return pickArr;
					}
				}
			}
		}
		return null;
	}

	// Getters
	/**
	 * Level ID
	 * @return the level id (first is 0)
	 */
	public int getLevelID() {
		return levelID;
	}

	/**
	 * Upper left corner longitude
	 * @return the upper left longitude
	 */
	public int getUpLeftLon() {
		return upLeftLon;
	}

	/**
	 * Upper left corner latitude
	 * @return the upper left latitude
	 */
	public int getUpLeftLat() {
		return upLeftLat;
	}

	/**
	 * Lower right corner longitude
	 * @return the lower right longitude
	 */
	public int getLowRightLon() {
		return lowRightLon;
	}

	/**
	 * Lower right corner latitude
	 * @return the lower right latitude
	 */
	public int getLowRightLat() {
		return lowRightLat;
	}

	/**
	 * Get the Tile at a specified index.
	 * @return the Tile at the specified index
	 */
	public Tile getTiles(int index) {
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
	 * @param tiles the tiles to set
	 */
	public void setTiles(Tile[/* LEVEL_NUMTILES */] tiles) {
		this.tiles = tiles;
	}

	/**
	 * @return the tiles
	 */
	public Tile[/* LEVEL_NUMTILES */] getTiles() {
		return tiles;
	}

	/**
	 * @param upLeftLon the upLeftLon to set
	 */
	public void setUpLeftLon(int upLeftLon) {
		this.upLeftLon = upLeftLon;
	}

	/** @param upLeftLat the upLeftLat to set */
	public void setUpLeftLat(int upLeftLat) {
		this.upLeftLat = upLeftLat;
	}

	/** @param lowRightLon the lowRightLon to set */
	public void setLowRightLon(int lowRightLon) {
		this.lowRightLon = lowRightLon;
	}

	/** @param lowRightLat the lowRightLat to set */
	public void setLowRightLat(int lowRightLat) {
		this.lowRightLat = lowRightLat;
	}

	/** @param visible the visible to set */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/** @return the visible */
	public boolean isVisible() {
		return visible;
	}

	/** @return the maxZoom */
	public int getMaxZoom() {
		return maxZoom;
	}

	/** @return the minZoom */
	public int getMinZoom() {
		return minZoom;
	}

	/** @return the divider */
	public int getDivider() {
		return divider;
	}
}
