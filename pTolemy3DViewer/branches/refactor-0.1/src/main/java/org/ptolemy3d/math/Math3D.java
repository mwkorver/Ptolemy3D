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

import static org.ptolemy3d.Ptolemy3DConfiguration.EARTH_RADIUS;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.view.Camera;

/**
 * Math 3D utilities.
 */
public class Math3D
{
	public static final double degToRad = (Math.PI / 180.0);
	public static final double radToDeg = (180.0 / Math.PI);
	public static final double twoPi = (2 * Math.PI);    // value raised above ground.
	public static final double halfPI = (Math.PI / 2.0);

	private static double[][] T = new double[3][3];

	/**
	 * Convert a longitude/latitude to cartesian coordinates
	 * @param lon longitude, unit is degree multiplied by <code>unit.DDBuffer</code> (default is 1000000).
	 * @param lat latitude, unit is degree multiplied by <code>unit.DDBuffer</code> (default is 1000000).
	 * @param dest destination
	 * @see Ptolemy3DUnit#DD
	 */
	public static final void setSphericalCoord(double lon, double lat, double[] dest)
	{
		final Landscape landscape = Ptolemy3D.ptolemy.scene.landscape;
		final Ptolemy3DUnit unit = Ptolemy3D.ptolemy.unit;

		double toRadiansOverDDBuffer = Math3D.degToRad / unit.DD;

		double thx = (lon - landscape.maxLongitude) * toRadiansOverDDBuffer;	//+ or - is equivalent (laxLon is 180°)
		double thy = lat * toRadiansOverDDBuffer;

		double cosX = Math.cos(thx);
		double sinX = Math.sin(thx);
		double cosY = Math.cos(thy);
		double sinY = Math.sin(thy);

		dest[0] = cosY * sinX;
		dest[1] = sinY;
		dest[2] = cosY * cosX;
	}
	
	/**
	 * @param lon longitude in DD
	 * @param lat latitude in DD
	 * @return true if the (lon, lat) point is on the visible side of the globe
	 */
	public static boolean isPointInView(double lon, double lat)
	{
		//Normal direction of the point( lon, lat) on the globe
		double[] point = new double[3];
		setSphericalCoord(lon, lat, point);
		
		return isPointInView(point);
	}
	/**
	 * @param point cartesian coordinate of the point on the globe
	 * @return true if the point is on the visible side of the globe
	 */
	public static boolean isPointInView(double[] point)
	{
		final Camera camera = Ptolemy3D.ptolemy.camera;
		
		//View forward vector
		double[] forward = new double[3];
		camera.getForward(forward);
		
		//Dot product between the vectors (front/back face test)
		double dot = (forward[0] * point[0]) + (forward[1] * point[1]) + (forward[2] * point[2]);
		if (dot <= 0) {
			//Visible if the forward vector is pointing in the other direction than the globe normal
			return true;
		}
		else {
			return false;
		}
	}
	
	//====================================================================================
	
	// picks triangle and slides verticeis 1 back
	public static final boolean pickTri(double[] arr, double[][] R)
	{
		boolean found = false;
		if (rayIntersectTri(arr, R) == 1) {
			found = true;
		}
		Vector3d.copy(T[0], T[1]);
		Vector3d.copy(T[1], T[2]);
		return found;
	}

	public static final void setTriVert(int vix, double x, double y, double z)
	{
		T[vix][0] = x;
		T[vix][1] = y;
		T[vix][2] = z;
	}

	public static int rayIntersectTri(double[] dest, double[][] tris)
	{
		double[] u, v, n;             // triangle vectors
		double[] dir, w0, w;          // ray vectors
		double r, a, b;             // params to calc ray-plane intersect

		u = new double[3];
		v = new double[3];
		n = new double[3];
		dir = new double[3];
		w0 = new double[3];
		w = new double[3];

		// get triangle edge vectors and plane normal
		Vector3d.sub(u, T[1], T[0]);
		Vector3d.sub(v, T[2], T[0]);

		Vector3d.cross(n, u, v);

		if ((n[0] == 0) && (n[1] == 0) && (n[2] == 0)) {            // triangle is degenerate
			return -1;                 // do not deal with this case
		}
		Vector3d.sub(dir, tris[1], tris[0]);
		Vector3d.sub(w0, tris[0], T[0]);

		a = Vector3d.dot(n, w0);
		b = Vector3d.dot(n, dir);

		if (Math.abs(b) < 0.00000001) {     // ray is parallel to triangle plane
			if (a == 0) {                // ray lies in triangle plane
				return 2;
			}
			else {
				return 0;             // ray disjoint from plane
			}
		}

		// get intersect point of ray with triangle plane
		r = -a / b;
		if (r < 0.0) {                   // ray goes away from triangle
			return 0;                  // => no intersect
			// for a segment, also test if (r > 1.0) => no intersect
		}
		double xt = tris[0][0] + (r * dir[0]), yt = tris[0][1] + (r * dir[1]), zt = tris[0][2] + (r * dir[2]);

		// is I inside T?
		double uu, uv, vv, wu, wv, D;
		uu = Vector3d.dot(u, u);
		uv = Vector3d.dot(u, v);
		vv = Vector3d.dot(v, v);

		w[0] = xt - T[0][0];
		w[1] = yt - T[0][1];
		w[2] = zt - T[0][2];

		wu = Vector3d.dot(w, u);
		wv = Vector3d.dot(w, v);
		D = uv * uv - uu * vv;

		// get and test parametric coords
		double s, t;
		s = (uv * wv - vv * wu) / D;
		if ((s < 0.0) || (s > 1.0)) {       // I is outside T
			return 0;
		}
		t = (uv * wu - uu * wv) / D;
		if ((t < 0.0) || ((s + t) > 1.0)) {  // I is outside T
			return 0;
		}
		if ((dest[0] == -999) && (dest[1] == -999) && (dest[2] == -999)) {
			dest[0] = xt;
			dest[1] = yt;
			dest[2] = zt;
		}
		else if (distance3D(tris[0][0], xt, tris[0][1], yt, tris[0][2], zt) < distance3D(tris[0][0], dest[0], tris[0][1], dest[1], tris[0][2], dest[2])) {
			dest[0] = xt;
			dest[1] = yt;
			dest[2] = zt;
		}
		else {
			return 0;
			//IntPt[0]=xt;IntPt[1]=yt;IntPt[2]=zt;
		}
		return 1;                      // I is in T
	}

	public static double distance3D(double x1, double x2, double y1, double y2, double z1, double z2)
	{
		double x = (x2 - x1);
		double y = (y2 - y1);
		double z = (z2 - z1);
		return Math.sqrt(x * x + y * y + z * z);
	}

//	public static boolean lineThroughPoint(short[][] inPoints,float xpt,float ypt,int N)
//	{
//		double rise,run,length,nrise,nrun;
//		for(int i=0; i < N-1; i++) {
//			rise = inPoints[1][i+1]-inPoints[1][i];
//			run = inPoints[0][i+1]-inPoints[0][i];
//			length = Math.sqrt(rise*rise + run*run);
//
//			nrise = rise/(length*2);//set the normal at 0.5
//			nrun = run/(length*2);
//
//			int numpts = (int)(length*2);
//			for(int j=0 ; j <= numpts; j++) {
//				if(distance2D(inPoints[0][i]+(nrun*j)  , xpt ,inPoints[1][i]+(nrise*j),ypt  ) < 5 )
//					return true;
//			}
//		}
//		return false;
//	}

	public static double distance2D(double x1, double x2, double y1, double y2)
	{
		double dx = x2 - x1;
		double dy = y2 - y1;
		return Math.sqrt(dx*dx + dy*dy);
	}

	public static int posByteToInt(byte[] b)
	{
		int val = 0;
		for (int j=0, i=b.length-1; i >= 0; i--, j++) {
			val += (b[i] - 48) * Math.pow(10, j);
		}
		return val;
	}

	public static double dot(double a1, double a2, double a3, double b1, double b2, double b3)
	{
		return (a1 * b1) + (a2 * b2) + (a3 * b3);
	}

	public static float angle3dvec(double x1, double y1, double z1, double x2, double y2, double z2, boolean degrees)
	{
		double mag1 = Math.sqrt((x1 * x1) + (y1 * y1) + (z1 * z1));
		double mag2 = Math.sqrt((x2 * x2) + (y2 * y2) + (z2 * z2));
		double dot = ((x1 * x2) + (y1 * y2) + (z1 * z2)) / (mag1 * mag2);
		if (degrees) {
			return (float) (Math.acos(dot) * Math3D.radToDeg);
		}
		else {
			return (float) Math.acos(dot);
		}
	}

	public static float angle2dvec(double x1, double y1, double x2, double y2, boolean degrees)
	{
		return angle3dvec(x1, y1, 0, x2, y2, 0, degrees);
	}

	public static final void computeFaceNormal(float[] a, float[] b, float[] c, float[] norm)
	{
		norm[0] = (b[1] - a[1]) * (c[2] - a[2]) - (c[1] - a[1]) * (b[2] - a[2]);
		norm[1] = (b[2] - a[2]) * (c[0] - a[0]) - (c[2] - a[2]) * (b[0] - a[0]);
		norm[2] = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);

		//Normalize
		double mag1 = Math.sqrt((norm[0] * norm[0]) + (norm[1] * norm[1]) + (norm[2] * norm[2]));
		mag1 = 1 / mag1;
		norm[0] *= mag1;
		norm[1] *= mag1;
		norm[2] *= mag1;
	}

	public static final void setExtents(int[][] poly, int[] extents)
	{
		extents[0] = extents[2] = poly[0][0];
		extents[1] = extents[3] = poly[0][1];

		for (int i = 1; i < poly.length; i++) {
			if (poly[i][1] < extents[1]) {
				extents[1] = poly[i][1];
			}
			if (poly[i][1] > extents[3]) {
				extents[3] = poly[i][1];
			}
			if (poly[i][0] < extents[0]) {
				extents[0] = poly[i][0];
			}
			if (poly[i][0] > extents[2]) {
				extents[2] = poly[i][0];
			}
		}
	}

	/** @return the max value in the array a[...][ix] */
	public static float max(float[][] a, int ix)
	{
		float v = a[0][ix];
		for (int i = 1; i < a.length; i++) {
			if (a[i][ix] > v) {
				v = a[i][ix];
			}
		}
		return v;
	}
	/** @return the min value in the array a[...][ix] */
	public static float min(float[][] a, int ix)
	{
		float v = a[0][ix];
		for (int i = 1; i < a.length; i++) {
			if (a[i][ix] < v) {
				v = a[i][ix];
			}
		}
		return v;
	}

	
	public static final void setMapCoord(double[] in, double[] out)
	{
		final Ptolemy3DUnit unit = Ptolemy3D.ptolemy.unit;

		double[] intvec = new double[3];
		Vector3d.copy(intvec, in);
		Vector3d.normalize(intvec);
		
		double o_lon = angle2dvec(-1,0,intvec[2],intvec[0],true);
		if(intvec[0] >= 0) {
			o_lon = -o_lon;
		}
		out[0] = Math.asin(intvec[1]) * radToDeg;
		out[1] = o_lon;
		
		double alt = (Math.sqrt(in[0]*in[0]) + (in[1]*in[1]) + (in[2]*in[2])-EARTH_RADIUS) / unit.coordSystemRatio;
		if (alt < 0) {
			alt = 0;
		}
		out[2] = alt;
	}

	public static int nextTwoPow(int n)
	{
		float val = n;
		int ct = 1;
		while (val > 2) {
			val /= 2;
			ct++;
		}
		return (int) Math.pow(2, ct);
	}

	public static boolean realPointInView(double lon, double alt, double lat, int MAXANGLE)
	{
		final Camera camera = Ptolemy3D.ptolemy.camera;
		double angle;
		{
			double[] coord = new double[3];
			setSphericalCoord(lon, lat, coord);
			lon = coord[0] * (EARTH_RADIUS + alt);
			alt = coord[1] * (EARTH_RADIUS + alt);
			lat = coord[2] * (EARTH_RADIUS + alt);
			angle = Math3D.angle3dvec(-camera.cameraMat.m[0][2], -camera.cameraMat.m[1][2], -camera.cameraMat.m[2][2],
					(camera.cameraPos[0] - lon), (camera.cameraPos[1] - alt), (camera.cameraPos[2] - lat),
					true);
		}

		if (angle <= MAXANGLE) {
			return true;
		}
		else {
			return false;
		}
	}

	public static double getScreenScaler(double tx, double ty, double tz, int fms)
	{
		final Camera camera = Ptolemy3D.ptolemy.camera;

		double dx, dy, dz;
		dx = camera.cameraPos[0] - tx;
		dy = camera.cameraPos[1] - ty;
		dz = camera.cameraPos[2] - tz;

		double kyori = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		//adjust distance according to angle
		{
			double d = 1.0 / kyori;
			dx *= d;
			dy *= d;
			dz *= d;
		}
		return (kyori * Math.cos(Math3D.angle3dvec(
				camera.cameraMat.m[0][2], camera.cameraMat.m[1][2], camera.cameraMat.m[2][2],
				dx, dy, dz, false))) / fms;
	}
}