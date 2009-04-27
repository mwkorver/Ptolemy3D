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

/**
 * Position represented by lat/lon/altitude.
 * 
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class Position {

    // Latitude of the view point.
    private double latitudeDD;
    // Longitude of the view point.
    private double longitudeDD;
    // Distance from the view point. May not be vertical (distance not altitude).
    private double altitude;

    /**
     * @param lat
     *            unit is DD
     * @param lon
     *            unit is DD
     * @param alt
     */
    public Position(double lat, double lon, double alt) {
        this.latitudeDD = lat;
        this.longitudeDD = lon;
        this.altitude = alt;
    }

    public Position copy() {
        return new Position(latitudeDD, longitudeDD, altitude);
    }

    /**
     * @return latitude in DD.
     */
    public double getLatitudeDD() {
        return latitudeDD;
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

    public void setAltitudeDD(double altDD) {
        altitude = altDD;
    }
}
