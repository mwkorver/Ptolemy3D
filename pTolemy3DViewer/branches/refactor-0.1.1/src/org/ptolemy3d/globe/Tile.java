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
import org.ptolemy3d.data.MapDataManager;
import org.ptolemy3d.debug.Config;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.math.CosTable;
import org.ptolemy3d.math.Picking;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.view.Camera;


/**
 * A tile.
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Contributors
 */
class Tile {
	/** Tile may be divided in  */
	static class TileArea {
		protected int ulx, ulz, lrx, lrz;
		protected boolean edge;
		protected boolean active;
		
		protected Tile tile;
		protected final List<TileArea> left, above, right, below;
		
		protected TileArea(Tile tile) {
			this.tile = tile;
			left = new Vector<TileArea>(4);
			above = new Vector<TileArea>(4);
			right = new Vector<TileArea>(4);
			below = new Vector<TileArea>(4);
		}
		
		protected final void setBounds(int ulx, int ulz, int lrx, int lrz) {
			this.ulx = ulx + Landscape.MAX_LONGITUDE;
			this.ulz = ulz;
			this.lrx = lrx + Landscape.MAX_LONGITUDE;
			this.lrz = lrz;
			onBoundsSet();
		}
		protected final void setBounds(TileArea from) {
			this.ulx = from.ulx;
			this.ulz = from.ulz;
			this.lrx = from.lrx;
			this.lrz = from.lrz;
			onBoundsSet();
		}
		private final void onBoundsSet() {
			this.active = true;
			
			this.left.clear();
			this.above.clear();
			this.right.clear();
			this.below.clear();
		}
		
		protected final void setNeighbour(Tile right, Tile below, Tile left, Tile above) {
			//FIXME getArea
			if(left != null && left.visible) {
				this.left.add(left.getArea());
			}
			if(above != null && above.visible) {
				this.above.add(above.getArea());
			}
			if(right != null && right.visible) {
				this.right.add(right.getArea());
			}
			if(below != null && below.visible) {
				this.below.add(below.getArea());
			}
			edge = (left == null) || (above == null) || (right == null) || (below == null);
		}
		protected final void addAbove(TileArea above) {
			if(above != null && above.active) {
				this.above.add(above);
				above.above.add(this);
			}
		}
		protected final void addBelow(TileArea below) {
			if(below != null && below.active) {
				this.below.add(below);
				below.below.add(this);
			}
		}
		protected final void addRight(TileArea right) {
			if(right != null && right.active) {
				this.right.add(right);
				right.right.add(this);
			}
		}
		protected final void addLeft(TileArea left) {
			if(left != null && left.active) {
				this.left.add(left);
				left.above.add(this);
			}
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
	private final TileArea bounds;
	private final TileArea[] subTiles;
	
	/* Tile.display */
	protected GL gl;
	protected int drawLevelID;
	protected boolean texture;

	/* TexTile renderer */
//	private static final TileRenderer renderer = new TileDefaultRenderer();					//First clean version
//	private static final TileRenderer renderer = new TileDirectModeRenderer();				//CPU Optimizations
	private static final TileRenderer renderer = new TileDirectModeRenderer_MathLookUp();	//Cos/Sin table lookup
	protected interface TileRenderer { public void renderSubTile(TileArea subTile); }

	protected Tile(int tileID) {
		this.tileID = tileID;
		this.bounds = new TileArea(this);
		this.subTiles = new TileArea[4];
		for(int i = 0; i < subTiles.length; i++) {
			subTiles[i] = new TileArea(this);
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
		bounds.setBounds(leftLon, upLat, rightLon, botLat);
		
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
			
			for(TileArea subTile : subTiles) {
				subTile.active = false;
			}
		}
	}
	
	public void processClipping(Tile right, Tile below, Tile left, Tile above) {
		if (!visible) {
			return;
		}
		
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final Globe globe = landscape.globe;
		
		int neightboor = 0;
		if (layerID < (globe.getNumLayers() - 1)) {	//Last layer don't have tile below
			final Layer layerBelow = globe.getLayer(layerID + 1);
			if (layerBelow.isVisible()) {
				for(Area area : layerBelow.getAreas()) {
					final int clipped = checkClipWithArea(right, below, left, above, area);
					if(clipped == 1) {
						return;
					}
					neightboor += clipped;
				}
			}
		}
		
		subTiles[0].setBounds(bounds);
		subTiles[0].setNeighbour(right, below, left, above);
		subTiles[0].edge = neightboor != 0;
	}
	
	/**
	 * @return 
	 *  1: clipped<BR>
	 *  0: not clipped not adge<BR>
	 * -1: on area is an edge<BR>
	 */
	private int checkClipWithArea(Tile right, Tile below, Tile left, Tile above, Area belowArea) {
		int ulxBelow = belowArea.getMinLongitude();
		int lrxBelow = belowArea.getMaxLongitude();
		int ulzBelow = belowArea.getMinLatitude();
		int lrzBelow = belowArea.getMaxLatitude();
		
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
        final Layer layerBelow = landscape.globe.getLayer(layerID + 1);
		final int topTileLat = layerBelow.getMaxTileLatitude();
		
		final int ulx = bounds.ulx - Landscape.MAX_LONGITUDE;
		final int ulz = bounds.ulz;
		final int lrx = bounds.lrx - Landscape.MAX_LONGITUDE;
		final int lrz = bounds.lrz;
        
		if(Config.DEBUG) {
			//In debug, verify that the is always true
	        if ((lrzBelow <= topTileLat) && (lrzBelow > -topTileLat)) {
	        	//Normal value
	        }
	        else {
	        	throw new RuntimeException("Unexpected state");
	        }
		}
		
		/*if ((lrzBelow <= topTileLat) && (lrzBelow > -topTileLat)) */{
			if ((lrx > ulxBelow) && (ulx < lrxBelow) && (lrz > ulzBelow) && (ulz < lrzBelow)) {
				clipWithArea(right, below, left, above, ulx, ulz, lrx, lrz, ulxBelow, ulzBelow, lrxBelow, lrzBelow);
				return 1;
			}

			final int belowTileSize = layerBelow.getTileSize();
			
			int maxX = (Landscape.MAX_LONGITUDE / belowTileSize) * belowTileSize;
			if (maxX != Landscape.MAX_LONGITUDE) {
				maxX += belowTileSize;
			}
			
			if (lrxBelow > maxX) {
				int past = Layer.NUMTILE_LON - ((Landscape.MAX_LONGITUDE - ulxBelow) / belowTileSize) - 1;
				ulxBelow = -maxX;
				lrxBelow = ulxBelow + (past * belowTileSize);

				if ((lrx > ulxBelow) && (ulx < lrxBelow) && (lrz > ulzBelow) && (ulz < lrzBelow)) {
	                clipWithArea(right, below, left, above, ulx, ulz, lrx, lrz, ulxBelow, ulzBelow, lrxBelow, lrzBelow);
					return 1;
				}
			}
		}
        
        //Test if this tile is neighboor to lower tile
		final boolean neightboor =
			((lrxBelow == ulz || ulxBelow == lrz) && ((ulx <= ulxBelow && ulxBelow < lrx) || (ulx < lrxBelow && lrxBelow <= lrx))) ||
			((ulxBelow == lrx || lrxBelow == ulx) && ((ulz <= ulzBelow && ulzBelow < lrz) || (ulz < lrzBelow && lrzBelow <= lrz)));
		return neightboor ? -1 : 0;
	}
	/**
	 * Clip two rectangular in (longitude,latitude) coordinate system.<BR>
	 * The two areas are defined by (ulx, ulz, lrx, lrz) and (clip_ulx, clip_ulz, clip_lrx, clip_lrz)<BR>
	 * <BR>
	 * The results can be 0 to 4 rectangular tiles<BR>
	 */
	private void clipWithArea(Tile right, Tile below, Tile left, Tile above,
			int ulx, int ulz, int lrx, int lrz,
			int clip_ulx, int clip_ulz, int clip_lrx, int clip_lrz) {
		final boolean ul = inBounds(ulx, ulz, clip_ulx, clip_ulz, clip_lrx, clip_lrz);
		final boolean ur = inBounds(lrx, ulz, clip_ulx, clip_ulz, clip_lrx, clip_lrz);
		final boolean ll = inBounds(ulx, lrz, clip_ulx, clip_ulz, clip_lrx, clip_lrz);
		final boolean lr = inBounds(lrx, lrz, clip_ulx, clip_ulz, clip_lrx, clip_lrz);

		if (ul && ur && ll && lr) {
			visible = false;
			return;
		}
		
		if (ul && ur) {
			subTiles[0].setBounds(ulx, clip_lrz, lrx, lrz);
			subTiles[0].setNeighbour(right, below, left, null);
		}
		else if (ur && lr) {
			subTiles[0].setBounds(ulx, ulz, clip_ulx, lrz);
			subTiles[0].setNeighbour(null, below, left, above);
		}
		else if (ll && lr) {
			subTiles[0].setBounds(ulx, ulz, lrx, clip_ulz);
			subTiles[0].setNeighbour(right, null, left, above);
		}
		else if (ul && ll) {
			subTiles[0].setBounds(clip_lrx, ulz, lrx, lrz);
			subTiles[0].setNeighbour(right, below, null, above);
		}
		else if (ul) {
			subTiles[0].setBounds(clip_lrx, ulz, lrx, clip_lrz);
			subTiles[0].setNeighbour(right, null, null, above);
			
			subTiles[1].setBounds(ulx, clip_lrz, lrx, lrz);
			subTiles[1].setNeighbour(right, below, left, null);
		}
		else if (ur) {
			subTiles[0].setBounds(ulx, ulz, clip_ulx, clip_lrz);
			subTiles[0].setNeighbour(null, null, left, above);
			
			subTiles[1].setBounds(ulx, clip_lrz, lrx, lrz);
			subTiles[1].setNeighbour(right, below, left, null);
		}
		else if (ll) {
			subTiles[0].setBounds(ulx, ulz, lrx, clip_ulz);
			subTiles[0].setNeighbour(right, null, left, above);
			
			subTiles[1].setBounds(clip_lrx, clip_ulz, lrx, lrz);
			subTiles[1].setNeighbour(right, below, null, null);
		}
		else if (lr) {
			subTiles[0].setBounds(ulx, ulz, lrx, clip_ulz);
			subTiles[0].setNeighbour(right, null, left, above);
			
			subTiles[1].setBounds(ulx, clip_ulz, clip_ulx, lrz);
			subTiles[1].setNeighbour(null, below, left, null);
		}
		else {
			subTiles[0].setBounds(ulx, ulz, lrx, clip_ulz);
			subTiles[0].setNeighbour(right, null, left, above);
			
			subTiles[1].setBounds(ulx, clip_lrz, lrx, lrz);
			subTiles[1].setNeighbour(right, below, left, null);
			
			subTiles[2].setBounds(ulx, clip_ulz, clip_ulx, clip_lrz);
			subTiles[2].setNeighbour(null, null, left, null);
			
			subTiles[3].setBounds(clip_lrx, clip_ulz, lrx, clip_lrz);
			subTiles[3].setNeighbour(right, null, null, null);
		}
	}
	private final boolean inBounds(int longitude, int latitude, int ulx, int ulz, int lrx, int lrz) {
		return (longitude >= ulx) && (longitude <= lrx) && (latitude >= ulz) && (latitude <= lrz);
	}
	
	protected void linkTiles(Layer layerBelow) {
		if (!visible) {
			return;
		}
		for (TileArea area : getAreas()) {
			if (area.edge) {
				for(int i = 0; i < Layer.NUMTILES; i++) {
					final Tile tile = layerBelow.getTile(i);
					if(tile.visible && tile.isEdge()) {
						tile.linkTiles(area);
					}
				}
			}
		}
	}
	private void linkTiles(TileArea area) {
		if(Config.DEBUG) {
			if(!visible) {
				throw new RuntimeException();
			}
		}
		for(TileArea belowArea : getAreas()) {
			final boolean lonInRange = (area.ulx <= belowArea.ulx && belowArea.ulx < area.lrx) || (area.ulx < belowArea.lrx && belowArea.lrx <= area.lrx);
			final boolean latInRange = (area.ulz <= belowArea.ulz && belowArea.ulz < area.lrz) || (area.ulz < belowArea.lrz && belowArea.lrz <= area.lrz);

			if (lonInRange && belowArea.lrz == area.ulz) {
				area.addAbove(belowArea);
			}
			else if (lonInRange && belowArea.ulz == area.lrz) {
				area.addBelow(belowArea);
			}
			else if (latInRange && belowArea.ulx == area.lrx) {
				area.addRight(belowArea);
			}
			else if (latInRange && belowArea.lrx == area.ulx) {
				area.addLeft(belowArea);
			}
		}
	}
	private boolean isEdge() {
		return getArea().edge || getAreas().size() > 0;
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
		
		for (TileArea subTile : subTiles) {
			if (subTile.active) {
				if(DEBUG) {
					final Landscape landscape = Ptolemy3D.getScene().getLandscape();
					if(landscape.getDisplayMode() == Landscape.DISPLAY_TILENEIGHBOUR) {
						if(ProfilerUtil.tileSelected == ProfilerUtil.tileCounter) {
							/*
							 * FIXME can be along the same level but visibility change later
							 * this explain the visibile check
							 * SHOULD BE WELL HANDLED AND INVISIBLE TILE SHOULD BE REMOVED FROM THE LIST !!
							 */
							gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
							for(TileArea t : subTile.above) {
								if(t.tile.visible) {
									t.tile.gl = gl;
									renderer.renderSubTile(t);
								}
							}
							for(TileArea t : subTile.below) {
								if(t.tile.visible) {
									t.tile.gl = gl;
									renderer.renderSubTile(t);
								}
							}
							for(TileArea t : subTile.right) {
								if(t.tile.visible) {
									t.tile.gl = gl;
									renderer.renderSubTile(t);
								}
							}
							for(TileArea t : subTile.left) {
								if(t.tile.visible) {
									t.tile.gl = gl;
									renderer.renderSubTile(t);
								}
							}
						}
					}

				}
				renderer.renderSubTile(subTile);
				
				if(DEBUG) {
					final Landscape landscape = Ptolemy3D.getScene().getLandscape();
					if(landscape.getDisplayMode() == Landscape.DISPLAY_TILENEIGHBOUR) {
						if(ProfilerUtil.tileSelected == ProfilerUtil.tileCounter) {
							gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
						}
					}
				}
			}
		}
	}

	/** Tile Picking */
	protected boolean pick(Vector3d intersectPoint, Vector3d[] ray) {
		if (mapData == null || mapData.key.layer != layerID) {
			return false;
		}

		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final double terrainScaler = landscape.getTerrainScaler();

		intersectPoint.set(-999, -999, -999);

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

		int theta1 = x1;
		int theta2 = x2;

		int phi1 = z1;
		int phi2 = z2;

		int dTetaOverN = (theta2 - theta1);
		int dPhiOverN = (phi2 - phi1);

		if (!useTin && !useDem) {
			int x_w = (x2 - x1);
			int z_w = (z2 - z1);

			// texture ratios
			int n = (int) (((double) ((x_w > z_w) ? x_w : z_w) / (double)Landscape.MAX_LONGITUDE) * 50);// precision, soon to be dem...
			if (n <= 2) {
				n = 4;
			}

			{
				dTetaOverN /= n;
				dPhiOverN /= n;
			}
			
			final Picking picking = new Picking();

			int t1 = phi1;
			for (int j = 0; j < n; j++) {
				final int t2 = t1 + dPhiOverN;

				final double cosT1_E =  CosTable.cos(t1) * EARTH_RADIUS;
				final double sinT1_E = -CosTable.sin(t1) * EARTH_RADIUS;
				final double cosT2_E =  CosTable.cos(t2) * EARTH_RADIUS;
				final double sinT2_E = -CosTable.sin(t2) * EARTH_RADIUS;

				int t3 = theta1;
				for (int i = 0; i <= n; i++) {
					double cx1, cy1, cz1, cx2, cy2, cz2;
					{
						final double cosT3 = CosTable.cos(t3);
						final double sinT3 = CosTable.sin(t3);

						cx1 = cosT1_E * sinT3;
						cx2 = cosT2_E * sinT3;
						cy1 = sinT1_E;
						cy2 = sinT2_E;
						cz1 = cosT1_E * cosT3;
						cz2 = cosT2_E * cosT3;
					}

					if (i > 0) {
						picking.setTriangle(2, cx1, cy1, cz1);
						if (picking.pickTri(intersectPoint, ray)) {
							return true;
						}
						picking.setTriangle(2, cx2, cy2, cz2);
						if (picking.pickTri(intersectPoint, ray)) {
							return true;
						}
					}
					else {
						picking.setTriangle(0, cx1, cy1, cz1);
						picking.setTriangle(1, cx2, cy2, cz2);
					}

					t3 += dTetaOverN;
				}
			}

		}
		else if (useTin) {
			theta1 *= Unit.DD_TO_RADIAN;
			theta2 *= Unit.DD_TO_RADIAN;

			phi1 *= Unit.DD_TO_RADIAN;
			phi2 *= Unit.DD_TO_RADIAN;

			dTetaOverN *= Unit.DD_TO_RADIAN;
			dPhiOverN *= Unit.DD_TO_RADIAN;
			
			{
				double d = 1.0 / mapData.tin.w;
				dTetaOverN /= d;
				dPhiOverN /= d;
			}
			double scaler = Unit.getCoordSystemRatio() * terrainScaler;

			final Picking picking = new Picking();
			
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

						tx =  dyE * cosZ * sinX;
						ty = -dyE * sinZ;
						tz =  dyE * cosZ * cosX;
					}

					// pick
					if (j < 2) {
						picking.setTriangle(j, tx, ty, tz);
					}
					else {
						picking.setTriangle(2, tx, ty, tz);
						if (picking.pickTri(intersectPoint, ray)) {
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

			int numRowsX = endx - startx;
			int numRowZ = endz - startz;

			dTetaOverN /= numRowsX;
			dPhiOverN /= numRowZ;

			double scaler = Unit.getCoordSystemRatio() * terrainScaler;

			final Picking picking = new Picking();
			
			int dz = phi1;
			int pos_d1 = (startz * row_width) + (startx * 2);
			for (int i = startz; i < endz; i++) {
				int d2z = dz + dPhiOverN;

				final double cosZ  = CosTable.cos(dz);
				final double cos2Z = CosTable.cos(d2z);
				final double sinZ  = CosTable.sin(dz);
				final double sin2Z = CosTable.sin(d2z);

				int pos_d1_save = pos_d1; // int pos_d1 = (i * rowWidth) +
				// (startx * 2);
				int dx = theta1;
				for (int j = startx; j < endx; j++) {
					int pos_d2 = pos_d1 + row_width;

					double dy1 = ((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * scaler;
					double dy2 = ((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * scaler;

					double cx1, cy1, cz1, cx2, cy2, cz2;
					{
						final double dy1E = EARTH_RADIUS + dy1;
						final double dy2E = EARTH_RADIUS + dy2;

						final double cosX = CosTable.cos(dx);
						final double sinX = CosTable.sin(dx);

						cx1 = dy1E * cosZ * sinX;
						cx2 = dy2E * cos2Z * sinX;
						cy1 = -dy1E * sinZ;
						cy2 = -dy2E * sin2Z;
						cz1 = dy1E * cosZ * cosX;
						cz2 = dy2E * cos2Z * cosX;
					}

					if (j > startx) {
						picking.setTriangle(2, cx1, cy1, cz1);
						if (picking.pickTri(intersectPoint, ray)) {
							return true;
						}

						picking.setTriangle(2, cx2, cy2, cz2);
						if (picking.pickTri(intersectPoint, ray)) {
							return true;
						}
					}
					else {
						picking.setTriangle(0, cx1, cy1, cz1);
						picking.setTriangle(1, cx2, cy2, cz2);
					}

					pos_d1 += 2;
					dx += dTetaOverN;
				}

				dz += dPhiOverN;
				pos_d1 = pos_d1_save + row_width;
			}
		}
		else {
			final Picking picking = new Picking();
			
			picking.setTriangle(0, x1, 0, z1);
			picking.setTriangle(1, x1, 0, z2);
			picking.setTriangle(2, x2, 0, z1);

			if (picking.pickTri(intersectPoint, ray)) {
				return true;
			}
			picking.setTriangle(2, x2, 0, z2);
			if (picking.pickTri(intersectPoint, ray)) {
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
	
	public TileArea getArea() {
		return bounds;
	}
	public List<TileArea> getAreas() {	//Not used for now
		if(!visible) {
			throw new RuntimeException();
		}
		
		List<TileArea> boundsList = new Vector<TileArea>(4);
		for (TileArea subTile : subTiles) {
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
		final int res;
		if (layerID != mapData.key.layer) {
			res = mapData.getLon();
		}
		else {
			res = tileLon;
		}
		return res + Landscape.MAX_LONGITUDE;
	}
	/**
	 * Lower right corner longitude
	 * @return the lower right longitude
	 */
	protected int getReferenceRightLongitude() {
		final int res;
		if (layerID != mapData.key.layer) {
			final Globe globe = Ptolemy3D.getScene().getLandscape().globe;
			final int tileSize = globe.getTileSize(mapData.key.layer);
			res = getReferenceLeftLongitude() + tileSize;
		}
		else {
			res = tileLon + tileSize + Landscape.MAX_LONGITUDE;
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
