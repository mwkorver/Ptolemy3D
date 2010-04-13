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

import java.util.ArrayList;
import org.ptolemy3d.scene.Landscape;

/**
 * A tile rectangular area.
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class TileArea {
	protected int ulx, ulz, lrx, lrz;
	protected boolean edge;
	protected boolean active;
	
	protected Tile tile;
	protected final ArrayList<TileArea> left, above, right, below;
	
	protected TileArea(Tile tile) {
		this.tile = tile;
		left = new ArrayList<TileArea>(4);
		above = new ArrayList<TileArea>(4);
		right = new ArrayList<TileArea>(4);
		below = new ArrayList<TileArea>(4);
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
		if(left != null && left.visible) {
			for(TileArea area : left.getAreas()) {
				addLeft(area);
			}
		}
		if(above != null && above.visible) {
			for(TileArea area : above.getAreas()) {
				addAbove(area);
			}
		}
		if(right != null && right.visible) {
			for(TileArea area : right.getAreas()) {
				addRight(area);
			}
		}
		if(below != null && below.visible) {
			for(TileArea area : below.getAreas()) {
				addBelow(area);
			}
		}
		edge = this.above.isEmpty() || this.below.isEmpty() || this.right.isEmpty() || this.left.isEmpty();
	}
	protected final void addAbove(TileArea above) {
		if(above.active) {
			this.above.add(above);
			above.below.add(this);
		}
	}
	protected final void addBelow(TileArea below) {
		if(below.active) {
			this.below.add(below);
			below.above.add(this);
		}
	}
	protected final void addRight(TileArea right) {
		if(right.active) {
			this.right.add(right);
			right.left.add(this);
		}
	}
	protected final void addLeft(TileArea left) {
		if(left.active) {
			this.left.add(left);
			left.right.add(this);
		}
	}
	
	/** @return true if (lon, lat) is a point inside the DEM area range */
	public boolean isInRange(int lon, int lat) {
		return (lon >= ulx) && (lon <= lrx) && (lat >= ulz) && (lat <= lrz);
	}
	
	/** @return the height of  */
	public double getElevation(int lon, int lat, boolean smoothed) {
		final ElevationNone elevation = new ElevationNone(this);
		
		final int startLon = elevation.polyLonStart;
		final int startLat = elevation.polyLatStart;
		
		final int endLon = elevation.polyLonEnd;
		final int endLat = elevation.polyLatEnd;
		
		final ElevationValue heightContext = getElevationValue(lon, lat);
		if(smoothed) {
			final TileArea aboveTile = getNeightboorTile(above, lon, lat);
			final TileArea leftTile = getNeightboorTile(left, lon, lat);
			final TileArea rightTile = getNeightboorTile(right, lon, lat);
			final TileArea belowTile = getNeightboorTile(below, lon, lat);
			
			if((lon == startLon) && (leftTile != null)) {
				heightContext.blend(leftTile.getElevationValue(lon, lat));
				if((lat == startLat) && (aboveTile != null)) {
					heightContext.blend(aboveTile.getElevationValue(lon, lat));
				}
				else if((lat == endLat) && (belowTile != null)) {
					heightContext.blend(belowTile.getElevationValue(lon, lat));
				}
			}
			else if((lon == endLon) && (rightTile != null)) {
				heightContext.blend(rightTile.getElevationValue(lon, lat));
				if((lat == startLat) && (aboveTile != null)) {
					heightContext.blend(aboveTile.getElevationValue(lon, lat));
				}
				else if((lat == endLat) && (belowTile != null)) {
					heightContext.blend(belowTile.getElevationValue(lon, lat));
				}
			}
			else if((lat == startLat) && (aboveTile != null)) {
				heightContext.blend(aboveTile.getElevationValue(lon, lat));
			}
			else if((lat == endLat) && (belowTile != null)) {
				heightContext.blend(belowTile.getElevationValue(lon, lat));
			}
		}
		return heightContext.height;
	}
	private TileArea getNeightboorTile(ArrayList<TileArea> areas, int lon, int lat) {
		for(TileArea area : areas) {
			if(area.tile.visible && area.isInRange(lon, lat)) {
				return area;
			}
		}
		return null;
	}
	
	private ElevationValue getElevationValue(int lon, int lat) {
		if(tile.mapData != null && tile.mapData.dem != null) {
			final ElevationDem dem = tile.mapData.dem;
			return dem.getElevation(lon, lat);
		}
		
		final ElevationNone elevation = new ElevationNone(this);
		final double lonID = elevation.indexOfLongitude(lon);
		final double latID = elevation.indexOfLatitude(lat);
		if(lonID == -1 || latID == -1) {
			throw new IllegalArgumentException();
		}
		
		final int lonInfID = (int)lonID;
		final int latInfID = (int)latID;
		
		final boolean interpolateLon = (lonInfID != lonID);
		final boolean interpolateLat = (latInfID != latID);
		
		final ElevationValue heightContext;
		if(interpolateLon && interpolateLat) {
			final double interpLon = lonID - lonInfID;
			final double interpLat = latID - latInfID;
			
			final int lon1 = elevation.getLonAtIndex(lonInfID);
			final int lon2 = elevation.getLonAtIndex(lonInfID+1);
			final int lat1 = elevation.getLatAtIndex(latInfID);
			final int lat2 = elevation.getLatAtIndex(latInfID+1);
			
			final double h00 = getElevationValue(lon1, lat1).height * (1 - interpLon);
			final double h10 = getElevationValue(lon2, lat1).height * interpLon;
			final double h01 = getElevationValue(lon1, lat2).height * (1 - interpLon);
			final double h11 = getElevationValue(lon2, lat2).height * interpLon;
			
			final double height = (h00 + h10) * (1 - interpLat) + (h01 + h11) * interpLat;
			heightContext = new ElevationValue(height, ElevationValue.TYPE_DEFAULT, true);
		}
		else if(interpolateLon) {
			final double interpLon = lonID - lonInfID;
			
			final int lon1 = elevation.getLonAtIndex(lonInfID);
			final int lon2 = elevation.getLonAtIndex(lonInfID+1);
			final int lat1 = elevation.getLatAtIndex(latInfID);
			
			final double h00 = getElevationValue(lon1, lat1).height * (1 - interpLon);
			final double h10 = getElevationValue(lon2, lat1).height * interpLon;
			
			final double height = h00 + h10;
			heightContext = new ElevationValue(height, ElevationValue.TYPE_DEFAULT, true);
		}
		else if(interpolateLat) {
			final double interpLat = latID - latInfID;
			
			final int lon1 = elevation.getLonAtIndex(lonInfID);
			final int lat1 = elevation.getLatAtIndex(latInfID);
			final int lat2 = elevation.getLatAtIndex(latInfID+1);
			
			final double h00 = getElevationValue(lon1, lat1).height;
			final double h01 = getElevationValue(lon1, lat2).height;
			
			final double height = h00 * (1 - interpLat) + h01 * interpLat;
			heightContext = new ElevationValue(height, ElevationValue.TYPE_DEFAULT, true);
		}
		else {
			final boolean startLon = (lon == elevation.polyLonStart);
			final boolean startLat = (lat == elevation.polyLatStart);
			
			final boolean endLon = (lon == elevation.polyLonEnd);
			final boolean endLat = (lat == elevation.polyLatEnd);
			
			ElevationValue aboveDEM, belowDEM, leftDEM, rightDEM;
			if(startLon && ((leftDEM = getNeighboorElevationWithCornerCheck(left, startLat || endLat, false, lon, lat)) != null)) {
				heightContext = leftDEM;
				if(startLat && ((aboveDEM = getNeighboorElevationWithCornerCheck(above, true, true, lon, lat)) != null)) {
					heightContext.blend(aboveDEM);
				}
				else if(endLat && ((belowDEM = getNeighboorElevationWithCornerCheck(below, true, true, lon, lat)) != null)) {
					heightContext.blend(belowDEM);
				}
			}
			else if(endLon && ((rightDEM = getNeighboorElevationWithCornerCheck(right, startLat || endLat, false, lon, lat)) != null)) {
				heightContext = rightDEM;
				if(startLat && ((aboveDEM = getNeighboorElevationWithCornerCheck(above, true, true, lon, lat)) != null)) {
					heightContext.blend(aboveDEM);
				}
				else if(endLat && ((belowDEM = getNeighboorElevationWithCornerCheck(below, true, true, lon, lat)) != null)) {
					heightContext.blend(belowDEM);
				}
			}
			else if(startLat && ((aboveDEM = getNeighboorElevation(above, lon, lat)) != null)) {
				heightContext = aboveDEM;
			}
			else if(endLat && ((belowDEM = getNeighboorElevation(below, lon, lat)) != null)) {
				heightContext = belowDEM;
			}
			else {
				heightContext = new ElevationValue(0, ElevationValue.TYPE_DEFAULT, false);
			}
		}
		return heightContext;
	}
	private ElevationValue getNeighboorElevation(ArrayList<TileArea> areas, int lon, int lat) {
		return getNeighboorElevationWithCornerCheck(areas, false, false, lon, lat);
	}
	private ElevationValue getNeighboorElevationWithCornerCheck(ArrayList<TileArea> areas, boolean corner, boolean isAboveOrBelow, int lon, int lat) {
		ElevationValue elevationValue;
		
		elevationValue = getNeighboorElevationWithCornerCheck_(areas, lon, lat);
		if(elevationValue != null) {
			return elevationValue;
		}
		
		if(corner) {
			/*
			 * This code is here because the actual neightboor information is not present
			 * for corner to corner tile neightboor.
			 * This is requiered for proper junction, so 
			 */
			for(TileArea _ : areas) {
				if(_.tile.visible) {
					if(isAboveOrBelow) {
						elevationValue = getNeighboorElevationWithCornerCheck_(_.left, lon, lat);
						if(elevationValue != null) {
							return elevationValue;
						}
						
						elevationValue = getNeighboorElevationWithCornerCheck_(_.right, lon, lat);
						if(elevationValue != null) {
							return elevationValue;
						}
					}
					else {
						elevationValue = getNeighboorElevationWithCornerCheck_(_.above, lon, lat);
						if(elevationValue != null) {
							return elevationValue;
						}
						
						elevationValue = getNeighboorElevationWithCornerCheck_(_.below, lon, lat);
						if(elevationValue != null) {
							return elevationValue;
						}
					}
				}
			}
		}
		return null;
	}
	private final ElevationValue getNeighboorElevationWithCornerCheck_(ArrayList<TileArea> areas, int lon, int lat) {
		for (TileArea area : areas) {
			if (   area.active
				&& area.tile.visible
				&& area.tile.mapData != null
				&& area.tile.mapData.dem != null
				&& area.isInRange(lon, lat)
			) {
				final ElevationDem dem = area.tile.mapData.dem;
				if(dem.isInRange(lon, lat)) {
					return dem.getElevation(lon, lat);
				}
			}
		}
		return null;
	}
}
