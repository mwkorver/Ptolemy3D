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
 * Tile area represented by min/max latitude/longitude.<BR>
 * <BR>
 * As design, an Area is immutable. This means its fields are final and cannot be modified.<BR>
 * A new object should be created.
 * 
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class Area {

	// Latitude bounds
	private final int minLatitude, maxLatitude;
	// Longitude bounds
	private final int minLongitude, maxLongitude;
	
	/**
	 * @param minLatitude min latitude in DD
	 * @param maxLatitude max latitude in DD
	 * @param minLongitude min longitude in DD
	 * @param maxLongitude max longitude in DD
	 */
	public Area(int minLatitude, int maxLatitude, int minLongitude, int maxLongitude) {
		this.minLatitude = minLatitude;
		this.maxLatitude = maxLatitude;
		this.minLongitude = minLongitude;
		this.maxLongitude = maxLongitude;
	}

	@Override
	public Area clone() {
		return this;
	}
	
	@Override
	public int hashCode() {
		long hash;
		hash = 31L        + minLatitude;
		hash = 31L * hash + maxLatitude;
		hash = 31L * hash + minLongitude;
		hash = 31L * hash + maxLongitude;
		return (int)(hash ^ (hash>>32));
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		else if(obj instanceof Area) {
			final Area that = (Area)obj;
			return (that.minLatitude == this.minLatitude) &&
				   (that.maxLatitude == this.maxLatitude) &&
				   (that.minLongitude == this.minLongitude) &&
				   (that.maxLongitude == this.maxLongitude);
		}
		else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return "MinLat: " + minLatitude + ", MaxLat: " + maxLatitude + ", MinLon: " + minLongitude + ", MaxLon: " + maxLongitude;
	}

	/** @return min latitude in DD */
	public int getMinLatitude() {
		return minLatitude;
	}

	/** @return max latitude in DD */
	public int getMaxLatitude() {
		return maxLatitude;
	}

	/** @return min longitude in DD */
	public int getMinLongitude() {
		return minLongitude;
	}

	/** @return max longitude in DD */
	public int getMaxLongitude() {
		return maxLongitude;
	}
}
