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

import org.ptolemy3d.Unit;

/**
 * Position represented by lat/lon/altitude.
 * 
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class Position {

	// Latitude of the view point.
	private double latitudeDD = 0;
	// Longitude of the view point.
	private double longitudeDD = 0;
	// Distance from the view point. May not be vertical (distance not
	// altitude).
	private double altitude = 0;

	/**
	 * Creates a new Position instance using degrees and meter units
	 * 
	 * @param lat
	 *            units in degrees
	 * @param lon
	 *            units in degrees
	 * @param alt
	 *            units in meters
	 * @return
	 */
	public static final Position fromLatLonAlt(double lat, double lon,
			double alt) {
		return new Position(
				lat * Unit.DEGREE_TO_DD_FACTOR,
				lon * Unit.DEGREE_TO_DD_FACTOR,
				alt / Unit.getCoordSystemRatio());
	}

	/**
	 * Creates a new default instance.
	 */
	public Position() {
	}

	/**
	 * Creates a new instance with specified degrees values.
	 * 
	 * @param lat
	 *            unit is DD
	 * @param lon
	 *            unit is DD
	 * @param alt
	 */
	public Position(double latDD, double lonDD, double altDD) {
		this.latitudeDD = latDD;
		this.longitudeDD = lonDD;
		this.altitude = altDD;
	}

	@Override
	public Position clone() {
		return new Position(latitudeDD, longitudeDD, altitude);
	}

	@Override
	public String toString() {
		return "Lat: " + latitudeDD + ", Lon: " + longitudeDD + ", Alt: "
				+ altitude;
	}

	/**
	 * @return latitude in DD.
	 */
	public double getLatitudeDD() {
		return latitudeDD;
	}

	public double getLatitude() {
		return latitudeDD / Unit.DEGREE_TO_DD_FACTOR;
	}

	public void setLatitude(double lat) {
		setLatitudeDD(lat * Unit.DEGREE_TO_DD_FACTOR);
	}

	public void setLatitudeDD(double latDD) {
		latitudeDD = latDD;
	}

	/**
	 * @return longitude in DD.
	 */
	public double getLongitudeDD() {
		return longitudeDD;
	}

	public double getLongitude() {
		return longitudeDD / Unit.DEGREE_TO_DD_FACTOR;
	}

	public void setLongitude(double lon) {
		setLongitudeDD(lon * Unit.DEGREE_TO_DD_FACTOR);
	}

	public void setLongitudeDD(double lonDD) {
		longitudeDD = lonDD;
	}

	/**
	 * @return the altitude in view space (altitude axis may not be vertical),
	 *         zero is the altitude of the ground with no elevation.
	 */
	public double getAltitudeDD() {
		return altitude;
	}

	public double getAltitude() {
		return altitude / Unit.getCoordSystemRatio();
	}

	public void setAltitude(double alt) {
		altitude = alt * Unit.getCoordSystemRatio();
	}

	public void setAltitudeDD(double altDD) {
		altitude = altDD;
	}
}
