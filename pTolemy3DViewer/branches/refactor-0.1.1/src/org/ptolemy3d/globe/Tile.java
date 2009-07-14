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

import java.util.List;
import java.util.Vector;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.manager.MapDataManager;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.view.Area;
import org.ptolemy3d.view.Camera;

/**
 * A tile.
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
class Tile {
	/** Tile may be divided in  */
	static class TileBounds {
		protected int ulx, ulz, lrx, lrz;
		protected boolean active;
		
		protected TileBounds() {}
		
		protected TileBounds set(int ulx, int ulz, int lrx, int lrz) {
			final Landscape landscape = Ptolemy3D.getScene().getLandscape();
			
			this.ulx = ulx + landscape.getMaxLongitude();
			this.ulz = ulz;
			this.lrx = lrx + landscape.getMaxLongitude();
			this.lrz = lrz;
			
			this.active = true;
			
			return this;
		}
		
		protected TileBounds set(TileBounds from) {
			this.ulx = from.ulx;
			this.ulz = from.ulz;
			this.lrx = from.lrx;
			this.lrz = from.lrz;
			
			this.active = true;
			
			return this;
		}
	}
	
	private final int tileID;

	/* Tile.processVisibility */
	private int layerID;
	private int tileSize;
	private int tileLat;
	private int tileLon;
	protected boolean visible;
	protected MapData mapData;

	/* Tile.processClipping */
	private final TileBounds bounds;
	private final TileBounds[] subTiles;
	
	/* Tile.display */
	protected GL gl;
	protected int drawLevelID;
	protected boolean texture;
	protected Tile left, above, right, below;

	/* TexTile renderer */
//	private static final TileRenderer renderer = new TileDefaultRenderer();					//First clean version
//	private static final TileRenderer renderer = new TileDirectModeRenderer();				//CPU Optimizations
	private static final TileRenderer renderer = new TileDirectModeRenderer_MathLookUp();	//Cos/Sin table lookup
	protected interface TileRenderer { public void renderSubTile(Tile tile, TileBounds subTile); }

	protected Tile(int tileID) {
		this.tileID = tileID;
		this.bounds = new TileBounds();
		this.subTiles = new TileBounds[4];
		for(int i = 0; i < subTiles.length; i++) {
			subTiles[i] = new TileBounds();
		}
	}

	public void processVisibility(Camera camera, Layer layer, int lon, int lat) {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();
		
		layerID = layer.getLevelID();
		tileSize = layer.getTileSize();
		
		tileLon = lon;
		tileLat = lat;
		
		mapData = mapDataManager.getIfExist(layerID, lon, -lat);

		final int leftLon = landscape.clampLeftLongitude(tileLon);
		final int rightLon = landscape.clampRightLongitude(tileLon + tileSize);
		final int upLat = landscape.clampUpperLatitude(tileLat);
		final int botLat = landscape.clampLowerLatitude(tileLat + tileSize);
		bounds.set(leftLon, upLat, rightLon, botLat);
		
		// Visibility test
		{
			final double upLeft, botLeft, botRight, upRight;
			if (landscape.isTerrainEnabled()) {
				final double yScaler = Unit.getCoordSystemRatio() * landscape.getTerrainScaler();
				upLeft = getUpLeftHeight() * yScaler;
				botLeft = getBotLeftHeight() * yScaler;
				botRight = getBotRightHeight() * yScaler;
				upRight = getUpRightHeight() * yScaler;
			} else {
				upLeft = 0;
				botLeft = 0;
				botRight = 0;
				upRight = 0;
			}
			
			visible = camera.isTileInView(leftLon, rightLon, -upLat, -botLat, upLeft, botLeft, botRight, upRight);
		}
		
		if (visible) {
			mapData = mapDataManager.request(layerID, lon, -lat);
			drawLevelID = mapData.key.layer;
			
			for(TileBounds subTile : subTiles) {
				subTile.active = false;
			}
		}
	}
	
	public void processClipping(Tile rightTile, Tile belowTile, Tile leftTile, Tile aboveTile) {
		if (!visible) {
			return;
		}
		
		this.right = rightTile;
		this.below = belowTile;
		this.left = leftTile;
		this.above = aboveTile;
		
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final Globe globe = landscape.globe;
		
		if (layerID < (globe.getNumLayers() - 1)) {	//Last layer don't have tile below
			final Layer layerBelow = globe.getLayer(layerID + 1);
			if (layerBelow.isVisible()) {
				for(Area area : layerBelow.getAreas()) {
					if(checkClipWithArea(area)) {
						return;
					}
				}
			}
		}
		
		subTiles[0].set(bounds);
	}
	private boolean checkClipWithArea(Area belowArea) {
		int ulxBelow = belowArea.getMinLongitude();
		int lrxBelow = belowArea.getMaxLongitude();
		int ulzBelow = belowArea.getMinLatitude();
		int lrzBelow = belowArea.getMaxLatitude();
		
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final int maxLon = landscape.getMaxLongitude();
		
        final Layer layerBelow = landscape.globe.getLayer(layerID + 1);
		final int topTileLat = layerBelow.getMaxTileLatitude();
		
		final int ulx = bounds.ulx - landscape.getMaxLongitude();
		final int ulz = bounds.ulz;
		final int lrx = bounds.lrx - landscape.getMaxLongitude();
		final int lrz = bounds.lrz;
        
        if ((lrzBelow <= topTileLat) && (lrzBelow > -topTileLat)) {
			if ((lrx > ulxBelow) && (ulx < lrxBelow) && (lrz > ulzBelow) && (ulz < lrzBelow)) {
				clipWithArea(ulx, ulz, lrx, lrz, ulxBelow, ulzBelow, lrxBelow, lrzBelow);
				return true;
			}

			int maxX = (maxLon / layerBelow.getTileSize()) * layerBelow.getTileSize();
			if (maxX != maxLon) {
				maxX += layerBelow.getTileSize();
			}
			
			if (lrxBelow > maxX) {
				int past = Layer.LEVEL_NUMTILE_LON - ((maxLon - ulxBelow) / layerBelow.getTileSize()) - 1;
				ulxBelow = -maxX;
				lrxBelow = ulxBelow + (past * layerBelow.getTileSize());

				if (!((lrx <= ulxBelow) || (ulx >= lrxBelow) || (lrz <= ulzBelow) || (ulz >= lrzBelow))) {
                    clipWithArea(ulx, ulz, lrx, lrz, ulxBelow, ulzBelow, lrxBelow, lrzBelow);
					return true;
				}
			}
		}
		return false;
	}
	private void clipWithArea(int ulx, int ulz, int lrx, int lrz, int c_ulx, int c_ulz, int c_lrx, int c_lrz) {
		final boolean ul = inBounds(ulx, ulz, c_ulx, c_ulz, c_lrx, c_lrz);
		final boolean ur = inBounds(lrx, ulz, c_ulx, c_ulz, c_lrx, c_lrz);
		final boolean ll = inBounds(ulx, lrz, c_ulx, c_ulz, c_lrx, c_lrz);
		final boolean lr = inBounds(lrx, lrz, c_ulx, c_ulz, c_lrx, c_lrz);

		if (ul && ur && ll && lr) {
			visible = false;
			return;
		}
		
		if (ul && ur) {
			subTiles[0].set(ulx, c_lrz, lrx, lrz);
		}
		else if (ur && lr) {
			subTiles[0].set(ulx, ulz, c_ulx, lrz);
		}
		else if (ll && lr) {
			subTiles[0].set(ulx, ulz, lrx, c_ulz);
		}
		else if (ul && ll) {
			subTiles[0].set(c_lrx, ulz, lrx, lrz);
		}
		else if (ul) {
			subTiles[0].set(c_lrx, ulz, lrx, c_lrz);
			subTiles[1].set(ulx, c_lrz, lrx, lrz);
		}
		else if (ur) {
			subTiles[0].set(ulx, ulz, c_ulx, c_lrz);
			subTiles[1].set(ulx, c_lrz, lrx, lrz);
		}
		else if (ll) {
			subTiles[0].set(ulx, ulz, lrx, c_ulz);
			subTiles[1].set(c_lrx, c_ulz, lrx, lrz);
		}
		else if (lr) {
			subTiles[0].set(ulx, ulz, lrx, c_ulz);
			subTiles[1].set(ulx, c_ulz, c_ulx, lrz);
		}
		else {
			subTiles[0].set(ulx, ulz, lrx, c_ulz);
			subTiles[1].set(ulx, c_lrz, lrx, lrz);
			subTiles[2].set(ulx, c_ulz, c_ulx, c_lrz);
			subTiles[3].set(c_lrx, c_ulz, lrx, c_lrz);
		}
	}
	private final boolean inBounds(int x, int z, int c_ulx, int c_ulz, int c_lrx, int c_lrz) {
		return (x >= c_ulx) && (x <= c_lrx) && (z >= c_ulz) && (z <= c_lrz);
	}
	
	public void display(GL gl) {
		if (!visible) {
			return;
		}

		if (DEBUG) {
			ProfilerUtil.tileCounter++;
			
//			if (ProfilerUtil.forceLayer != -1 && ProfilerUtil.forceLayer != drawLevelID) {	//Select map layer
			if (ProfilerUtil.forceLayer != -1 && ProfilerUtil.forceLayer != layerID) {		//Select mesh layer
				return;
			}
		}


		final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();
		final int textureID = mapDataManager.getTextureID(gl, mapData);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureID);

		this.gl = gl;
		this.texture = (textureID != 0);
		
		if (!texture) {
			final Landscape landscape = Ptolemy3D.getScene().getLandscape();
			final float[] tileColor = landscape.getTileColor();
			gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
		}
		
		for (TileBounds subTile : subTiles) {
			if (subTile.active) {
				renderer.renderSubTile(this, subTile);
			}
		}
	}

	/** Tile Picking */
	protected boolean pick(double[] intersectPoint, double[][] ray) {
		if (mapData == null || mapData.key.layer != layerID) {
			return false;
		}

		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final double terrainScaler = landscape.getTerrainScaler();

		intersectPoint[0] = intersectPoint[1] = intersectPoint[2] = -999;

		int x1 = tileLon;
		int z1 = tileLat;
		int x2 = tileLon + tileSize;
		int z2 = tileLat + tileSize;

		boolean useDem, useTin;
		{
			boolean b = landscape.isTerrainEnabled() && (mapData != null);
			useDem = (b && (mapData.dem != null)) ? true : false;
			useTin = (b && (mapData.tin != null)) ? true : false;
		}

		double theta1, dTetaOverN;
		double phi1, dPhiOverN;
		{
			double theta2, phi2;
			double oneOverDDToRad;

			oneOverDDToRad = Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();

			theta1 = x1 * oneOverDDToRad;
			theta2 = x2 * oneOverDDToRad;

			phi1 = z1 * oneOverDDToRad;
			phi2 = z2 * oneOverDDToRad;

			dTetaOverN = (theta2 - theta1); // To be divieded by n later
			dPhiOverN = (phi2 - phi1);
		}

		if (!useTin && !useDem) {
			int x_w = (x2 - x1);
			int z_w = (z2 - z1);

			// texture ratios
			int n = (int) (((double) ((x_w > z_w) ? x_w : z_w) / (double) landscape.getMaxLongitude()) * 50);// precision, soon to be dem...
			if (n <= 2) {
				n = 4;
			}

			{
				dTetaOverN /= n;
				dPhiOverN /= n;
			}

			double t1 = phi1;
			for (int j = 0; j < n; j++) {
				final double t2 = t1 + dPhiOverN;

				final double cosT1_E =  Math.cos(t1) * EARTH_RADIUS;
				final double sinT1_E = -Math.sin(t1) * EARTH_RADIUS;
				final double cosT2_E =  Math.cos(t2) * EARTH_RADIUS;
				final double sinT2_E = -Math.sin(t2) * EARTH_RADIUS;

				double t3 = theta1;
				for (int i = 0; i <= n; i++) {
					double cx1, cy1, cz1, cx2, cy2, cz2;
					{
						final double cosT3 = Math.cos(t3);
						final double sinT3 = Math.sin(t3);

						cx1 = cosT1_E * sinT3;
						cx2 = cosT2_E * sinT3;
						cy1 = sinT1_E;
						cy2 = sinT2_E;
						cz1 = cosT1_E * cosT3;
						cz2 = cosT2_E * cosT3;
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
		else if (useTin) {
			{
				double d = 1.0 / mapData.tin.w;
				dTetaOverN /= d;
				dPhiOverN /= d;
			}
			double scaler = Unit.getCoordSystemRatio() * terrainScaler;

			for (int i = 0; i < mapData.tin.indices.length; i++) {
				for (int j = 0; j < mapData.tin.indices[i].length; j++) {
					double dx, dz, dy;
					{
						final int v = mapData.tin.indices[i][j];
						final float[] p = mapData.tin.positions[v];

						dx = theta1 + p[0] * dTetaOverN;
						dy = p[1] * scaler;
						dz = phi1 + p[2] * dPhiOverN;
					}

					double tx, ty, tz;
					{
						double dyE = EARTH_RADIUS + dy;
						double sinZ = Math.sin(dz);
						double cosZ = Math.cos(dz);
						double sinX = Math.sin(dx);
						double cosX = Math.cos(dx);

						tx = dyE * cosZ * sinX;
						ty = -dyE * sinZ;
						tz = dyE * cosZ * cosX;
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
		else if (useDem) {
			final byte[] dem = mapData.dem.demDatas;

			final int row_width = (int) Math.sqrt((dem.length / 2)) * 2;
			final int numrows = row_width / 2;

			int startx = 0;
			int startz = 0;
			int endx = numrows - 1;
			int endz = numrows - 1;

			int nrows_x = endx - startx;
			int nrows_z = endz - startz;

			{
				dTetaOverN /= nrows_x;
				dPhiOverN /= nrows_z;
			}

			double scaler = Unit.getCoordSystemRatio() * terrainScaler;

			double dz = phi1;
			int pos_d1 = (startz * row_width) + (startx * 2);
			for (int i = startz; i < endz; i++) {
				double d2z = dz + dPhiOverN;

				final double cosZ = Math.cos(dz);
				final double cos2Z = Math.cos(d2z);
				final double sinZ = Math.sin(dz);
				final double sin2Z = Math.sin(d2z);

				int pos_d1_save = pos_d1; // int pos_d1 = (i * rowWidth) +
				// (startx * 2);
				double dx = theta1;
				for (int j = startx; j < endx; j++) {
					int pos_d2 = pos_d1 + row_width;

					double dy1 = ((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * scaler;
					double dy2 = ((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * scaler;

					double cx1, cy1, cz1, cx2, cy2, cz2;
					{
						final double dy1E = EARTH_RADIUS + dy1;
						final double dy2E = EARTH_RADIUS + dy2;

						final double cosX = Math.cos(dx);
						final double sinX = Math.sin(dx);

						cx1 = dy1E * cosZ * sinX;
						cx2 = dy2E * cos2Z * sinX;
						cy1 = -dy1E * sinZ;
						cy2 = -dy2E * sin2Z;
						cz1 = dy1E * cosZ * cosX;
						cz2 = dy2E * cos2Z * cosX;
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
					dx += dTetaOverN;
				}

				dz += dPhiOverN;
				pos_d1 = pos_d1_save + row_width;
			}
		}
		else {
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

	// Getters

	/**
	 * Tile ID
	 * @return the tile id (first is 0)
	 */
	public int getTileID() {
		return tileID;
	}

	/**
	 * Level ID
	 * @return the level id (first is 0)
	 */
	public int getLayerID() {
		return layerID;
	}

	public int getTileSize() {
		return tileSize;
	}
	
	public TileBounds getBounds() {
		return bounds;
	}
	private List<TileBounds> getSubBounds() {	//Not used for now
		List<TileBounds> boundsList = new Vector<TileBounds>(4);
		for (TileBounds subTile : subTiles) {
			if (subTile.active) {
				boundsList.add(subTile);
			}
		}
		return boundsList;
	}
	
	/**
	 * Upper left corner longitude
	 * @return the upper left longitude
	 */
	protected int getReferenceLeftLongitude() {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		
		final int res;
		if (layerID != mapData.key.layer) {
			res = mapData.getLon();
		}
		else {
			res = tileLon;
		}
		return res + landscape.getMaxLongitude();
	}
	/**
	 * Lower right corner longitude
	 * @return the lower right longitude
	 */
	protected int getReferenceRightLongitude() {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		
		final int res;
		if (layerID != mapData.key.layer) {
			final Globe globe = Ptolemy3D.getScene().getLandscape().globe;
			final int tileSize = globe.getTileSize(mapData.key.layer);
			res = getReferenceLeftLongitude() + tileSize;
		}
		else {
			res = tileLon + tileSize + landscape.getMaxLongitude();
		}
		return res;
	}
	/**
	 * Upper left corner latitude
	 * @return the upper left latitude
	 */
	protected int getReferenceUpperLatitude() {
		final int res;
		if (layerID != mapData.key.layer) {
			res = -mapData.getLat();
		}
		else {
			res = tileLat;
		}
		return res;
	}
	/**
	 * Lower right corner latitude
	 * @return the lower right latitude
	 */
	protected int getReferenceLowerLatitude() {
		final int res;
		if (layerID != mapData.key.layer) {
			final Globe globe = Ptolemy3D.getScene().getLandscape().globe;
			final int tileSize = globe.getTileSize(mapData.key.layer);
			res = getReferenceUpperLatitude() + tileSize;
		}
		else {
			res = tileLat + tileSize;
		}
		return res;
	}
	
	/* Height getters */
	
	public double getUpLeftHeight() {
		if (mapData != null) {
			if (mapData.tin != null) {
				return mapData.tin.getUpLeftHeight(this);
			}
			else if(mapData.dem != null) {
				return mapData.dem.getUpLeftHeight(this);
			}
		}
		return 0;
	}
	public double getBotLeftHeight() {
		if (mapData != null) {
			if (mapData.tin != null) {
				return mapData.tin.getBotLeftHeight(this);
			}
			else if(mapData.dem != null) {
				return mapData.dem.getBotLeftHeight(this);
			}
		}
		return 0;
	}
	public double getUpRightHeight() {
		if (mapData != null) {
			if (mapData.tin != null) {
				return mapData.tin.getUpRightHeight(this);
			}
			else if(mapData.dem != null) {
				return mapData.dem.getUpRightHeight(this);
			}
		}
		return 0;
	}
	public double getBotRightHeight() {
		if (mapData != null) {
			if (mapData.tin != null) {
				return mapData.tin.getBotRightHeight(this);
			}
			else if(mapData.dem != null) {
				return mapData.dem.getBotRightHeight(this);
			}
		}
		return 0;
	}
}
