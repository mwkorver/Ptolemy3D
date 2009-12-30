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
package org.ptolemy3d.math;

import static org.ptolemy3d.Unit.EARTH_RADIUS;

import org.ptolemy3d.Unit;
import org.ptolemy3d.scene.Landscape;

/**
 * Math 3D utilities.
 */
public class Math3D {
	public static final double DEGREE_TO_RADIAN = (Math.PI / 180.0);
	public static final double RADIAN_TO_DEGREE = (180.0 / Math.PI);
	public static final double TWO_PI = (2 * Math.PI);
	public static final double HALF_PI = (Math.PI / 2.0);

	/**
	 * Convert a longitude/latitude to cartesian coordinates
	 *
	 * @param lon
	 *            longitude, unit is degree multiplied by
	 *            <code>unit.DDBuffer</code> (default is 1000000).
	 * @param lat
	 *            latitude, unit is degree multiplied by
	 *            <code>unit.DDBuffer</code> (default is 1000000).
	 * @param radius 
	 * @param dest
	 *            destination
	 * @see Unit#DD
	 */
	public static final void setSphericalCoord(int lon, int lat, double radius, Vector3d dest) {
		lon += Landscape.MAX_LONGITUDE;

		final double cosX, sinX, cosY, sinY;
		cosX = CosTable.cos(lon);
		sinX = CosTable.sin(lon);
		cosY = CosTable.cos(lat);
		sinY = CosTable.sin(lat);

		dest.x = cosY * sinX * radius;
		dest.y = sinY        * radius;
		dest.z = cosY * cosX * radius;
	}

	public static final void setSphericalCoord(double lon, double lat, double radius, Vector3d dest) {
		final double thx, thy;
		thx = (lon + Landscape.MAX_LONGITUDE) * Unit.DD_TO_RADIAN;
		thy = lat * Unit.DD_TO_RADIAN;

		final double cosX, sinX, cosY, sinY;
		cosX = Math.cos(thx);
		sinX = Math.sin(thx);
		cosY = Math.cos(thy);
		sinY = Math.sin(thy);

		dest.x = cosY * sinX * radius;
		dest.y = sinY        * radius;
		dest.z = cosY * cosX * radius;
	}
	
    public static double distance2D(double x1, double x2, double y1, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
	public static float angle3dvec(double x1, double y1, double z1, double x2,
			double y2, double z2, boolean degrees) {
		double mag1 = Math.sqrt((x1 * x1) + (y1 * y1) + (z1 * z1));
		double mag2 = Math.sqrt((x2 * x2) + (y2 * y2) + (z2 * z2));
		double dot = ((x1 * x2) + (y1 * y2) + (z1 * z2)) / (mag1 * mag2);
		if (degrees) {
			return (float) (Math.acos(dot) * Math3D.RADIAN_TO_DEGREE);
		}
		else {
			return (float) Math.acos(dot);
		}
	}

	public static float angle2dvec(double x1, double y1, double x2, double y2, boolean degrees) {
		return angle3dvec(x1, y1, 0, x2, y2, 0, degrees);
	}
	
	public static final void computeFaceNormal(Vector3d a, Vector3d b, Vector3d c, float[] norm) {
		double x = (b.y - a.y) * (c.z - a.z) - (c.y - a.y) * (b.z - a.z);
		double y = (b.z - a.z) * (c.x - a.x) - (c.z - a.z) * (b.x - a.x);
		double z = (b.x - a.x) * (c.y - a.y) - (c.x - a.x) * (b.y - a.y);

		// Normalize
		double mag1 = 1.0 / Math.sqrt((x * x) + (y * y) + (z * z));
		norm[0] = (float)(x * mag1);
		norm[1] = (float)(y * mag1);
		norm[2] = (float)(z * mag1);
	}

	public static final void normalize(float[] norm) {
		float x = norm[0];
		float y = norm[1];
		float z = norm[2];

		// Normalize
		float mag1 = 1.0f / (float)Math.sqrt((x * x) + (y * y) + (z * z));
		norm[0] *= mag1;
		norm[1] *= mag1;
		norm[2] *= mag1;
	}
	
	public static final void setMapCoord(Vector3d in, Vector3d out) {
		Vector3d intvec = new Vector3d(in);
		intvec.normalize();

		double o_lon = angle2dvec(-1, 0, intvec.z, intvec.x, true);
		if (intvec.x >= 0) {
			o_lon = -o_lon;
		}
		out.x = Math.asin(intvec.y) * RADIAN_TO_DEGREE;
		out.y = o_lon;

		double alt = (in.magnitude() - EARTH_RADIUS) / Unit.getCoordSystemRatio();
		if (alt < 0) {
			alt = 0;
		}
		out.z = alt;
	}
}