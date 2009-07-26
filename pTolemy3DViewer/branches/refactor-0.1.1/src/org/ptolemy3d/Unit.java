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

import org.ptolemy3d.math.Math3D;

/**
 * Unit class constains a set of static methods to work with the unit system
 * used in Ptolemy3D.
 *
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Unit {
	/** Earth radius from the center to the equator, in meter. */
	private final static float REAL_EARTH_RADIUS = 6378136;
	
	/** Earth radius in the viewer unit system. */
    public final static int EARTH_RADIUS = 500000; // REAL_EARTH_RADIUS;
    private final static float coordSystemRatio = EARTH_RADIUS / REAL_EARTH_RADIUS;
    
    /** Factor to convert degrees to dd */
    public final static int DEGREE_TO_DD_FACTOR = 1000000;
    /** Factor to convert dd to radians */
    public final static double DD_TO_RADIAN = Math3D.DEGREE_TO_RADIAN / DEGREE_TO_DD_FACTOR;

    /**
     * Convert degrees to DD.
     *
     * @param degrees angle in degree to convert
     * @return the angle mesured in DD unit
     * @see #ddToDegrees
     */
    public static final int degreesToDD(double degrees) {
        return (int) (degrees * DEGREE_TO_DD_FACTOR);
    }

    /**
     * Convert DD to degrees.
     *
     * @param dd angle in DD to convert
     * @return the angle mesured in degrees unit
     * @see #degreesToDD
     */
    public static double ddToDegrees(int ddUnit) {
        return (double) ddUnit / DEGREE_TO_DD_FACTOR;
    }
    
    /**
     * Convert degrees to DD.
     *
     * @param degrees angle in degree to convert
     * @return the angle mesured in DD unit
     * @see #ddToDegrees
     */
    public static final double ddToRadians(int dd) {
        return dd * DD_TO_RADIAN;
    }
    
    /**
     * Convert degrees to DD.
     *
     * @param degrees angle in degree to convert
     * @return the angle mesured in DD unit
     * @see #ddToDegrees
     */
    public static final double radiansToDD(int dd) {
        return dd * DD_TO_RADIAN;
    }
    
    public static float getCoordSystemRatio() {
        return coordSystemRatio;
    }
}