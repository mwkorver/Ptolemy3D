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
package org.ptolemy3d;

/**
 * Unit class constains a set of static methods to work with the unit system
 * used in Ptolemy3D.
 *
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Unit {

    // Globe radius
    public static final int EARTH_RADIUS = 500000; // 6378136;
    public static final int DEFAULT_DD_FACTOR = 1000000;
    private static int ddFactor = DEFAULT_DD_FACTOR;
    private static int meterX = 0;	//FIXME Always 0, remove that ?
    private static int meterZ = 0;	//FIXME Always 0, remove that ?
    private static float coordSystemRatio = (float) EARTH_RADIUS / 6378136;

    /**
     * Convert degrees to DD.
     *
     * @param degrees angle in degree to convert
     * @return the angle mesured in DD unit
     * @see #ddToDegrees
     */
    public static int degreesToDD(double degrees) {
        return (int) (degrees * ddFactor);
    }

    /**
     * Convert DD to degrees.
     *
     * @param dd angle in DD to convert
     * @return the angle mesured in degrees unit
     * @see #degreesToDD
     */
    public static double ddToDegrees(int ddUnit) {
        return (double) ddUnit / ddFactor;
    }

    public static int getMeterX() {
        return meterX;
    }

    public static void setMeterX(int mx) {
        meterX = mx;
    }

    public static int getMeterZ() {
        return meterZ;
    }

    public static void setMeterZ(int mz) {
        meterZ = mz;
    }

    public static float getCoordSystemRatio() {
        return coordSystemRatio;
    }

    public static int getDDFactor() {
        return ddFactor;
    }

    public static void setDDFactor(int ddf) {
        ddFactor = ddf;
    }
}