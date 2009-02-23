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
package org.ptolemy3d.view;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;

/**
 * Position represented by lat/lon/altitude.
 * 
 * @author Jerome Jouvie
 * @author Antonio Santiago
 */
public class Position {
	/** Latitude of the view point. */
	protected double lat;
	/** Longitude of the view point. */
	protected double lon;
	/**
	 * Distance from the view point. May not be vertical (distance not
	 * altitude).
	 */
	protected double alt;

	/**
	 * @param lat
	 *            unit is DD
	 * @param lon
	 *            unit is DD
	 * @param alt
	 */
	private Position(double lat, double lon, double alt) {
		this.lon = lon;
		this.alt = alt;
		this.lat = lat;
	}

	public Position clone() {
		return new Position(lat, lon, alt);
	}

	/** @return longitude in degrees. */
	public final double getLongitudeDegrees() {
		final Unit unit = Ptolemy3D.ptolemy.unit;
		return lon / unit.getDD();
	}

	/** @return longitude in DD. */
	public final double getLongitudeDD() {
		return lon;
	}

	/** @return latitude in degrees. */
	public final double getLatitudeDegrees() {
		final Unit unit = Ptolemy3D.ptolemy.unit;
		return lat / unit.getDD();
	}

	/** @return latitude in DD. */
	public final double getLatitudeDD() {
		return lat;
	}

	/**
	 * @return the altitude in view space (altitude axis may not be vertical),
	 *         zero is the altitude of the ground with no elevation.
	 */
	public final double getAltitude() {
		final Unit unit = Ptolemy3D.ptolemy.unit;
		return alt / unit.getCoordSystemRatio();
	}

	/**
	 * @return the altitude in view space (altitude axis may not be vertical),
	 *         zero is the altitude of the ground with no elevation.
	 */
	public final double getAltitudeDD() {
		return alt;
	}

	public static Position fromDD(double lat, double lon, double alt) {
		return new Position(lat, lon, alt);
	}

	public static Position fromDegrees(double lat, double lon, double alt) {
		final Unit unit = Ptolemy3D.ptolemy.unit;
		return new Position(lat * unit.getDD(), lon * unit.getDD(), alt);
	}

}
