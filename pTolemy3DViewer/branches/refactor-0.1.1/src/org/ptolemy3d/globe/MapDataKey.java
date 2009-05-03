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

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class MapDataKey {
	/** Tile level */
	public final int layer;
	/** Longitude in DD. */
	public final int lon;
	/** Latitude in DD. */
	public final int lat;
	/* Hash code generated */
	private final int hashCode;
	
	/**
	 * @param layer level
	 * @param lon longitude
	 * @param lat latitude
	 */
	public MapDataKey(int layer, int lon, int lat) {
		this.layer = layer;
		this.lon = lon;
		this.lat = lat;
		this.hashCode = generateHashCode();
	}
	
	private int generateHashCode() {
		long hash;
		hash = 31L        + layer;
		hash = 31L * hash + lon;
		hash = 31L * hash + lat;
		return (int)(hash ^ (hash>>32));
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		else if(obj instanceof MapDataKey) {
			final MapDataKey that = (MapDataKey)obj;
			return (that.lon == this.lon) &&
				   (that.lat == this.lat) &&
				   (that.layer == this.layer);
		}
		else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return super.toString() + "("+layer+", "+lon+", "+lat+")";
	}
}
