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

import javax.media.opengl.glu.GLU;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.Unit;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix16d;
import org.ptolemy3d.math.Matrix9d;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.plugin.Sky;
import org.ptolemy3d.scene.Landscape;

/**
 * <H1>Camera Coordinate System</H1> <BR>
 * The view position is defined with the position (longitude, latitude,
 * altitude, direction, tilt).<BR>
 * <BR>
 * The longitude and latitude components are angles in the ranges respectively
 * [-180�;180�] and [-90�;90�]. Those two angles define every position on the
 * globe.<BR>
 * <BR>
 * <div align="center"><img
 * src="../../../ViewCoordinateSystem-LongitudeLatitude.jpg" width=256
 * height=256><BR>
 * Longitude / Latitude angles</div><BR>
 * <BR>
 * The view always look towards that position, but from any possible
 * orientation. The view orientation is describe with the two angles (direction,
 * tilt).<BR>
 * <BR>
 * The reference view orientation is as follow: the view up direction is tangent
 * to the longitude line, the view forward is in direction to the globe center.<BR>
 * The view vectors are represented on the next picture:<BR>
 * <div align="center"><img
 * src="../../../ViewCoordinateSystem-RefDirectionPitch.jpg" width=256
 * height=256></div><BR>
 * <BR>
 * The first angle <code>direction</code> defines the rotation around the
 * vertical (upward) direction:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-Direction.jpg"
 * width=256 height=256></div><BR>
 * <BR>
 * The second angle <code>tilt</code> is the rotation around the side vector:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-Tilt.jpg"
 * width=256 height=256></div><BR>
 * <BR>
 * Finally the altitude tell how far we are from the position on the globe,
 * along the view forward vector.<BR>
 * This last picture represents the view vectors and the view position:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-Altitude.jpg"
 * width=256 height=256></div><BR>
 * <BR>
 * <BR>
 * <H1>Camera & Cartesian Coordinate System Relation</H1> <BR>
 * Here are relations between cartesian axis and longitude/latitude:<BR>
 * <ul>
 * <li>X axis: longitude=180�, latitude=0�</li>
 * <li>Y axis: longitude=90�, latitude=any angles</li>
 * <li>Z axis: longitude=90�, latitude=0�</li>
 * </ul>
 * From that, you can find easily those relations :<BR>
 * <ul>
 * <li>-X axis: longitude=0�, latitude=0�</li>
 * <li>-Y axis: longitude=-90�, latitude=any angles</li>
 * <li>-Z axis: longitude=-90�, latitude=0�</li>
 * </ul>
 * <BR>
 * <i><u>Remark:</u> All angles are expressed modulos 360� (180� equals to -180�
 * and 540� ...)</i><BR>
 * <BR>
 * <BR>
 * <H1>Positioning</H1> <BR>
 * View movements are controlled in the coordinate system describe in the
 * previous paragraph.<BR>
 * View movements is controlled from CameraMovement, for more information, see
 * the <a href="CameraMovement.html">CameraMovement documentation</a>.<BR>
 * <BR>
 * Sometimes you'll may find usefull to convert that position into another
 * coordinate system.<BR>
 * You can get the equivalent cartesian coordinates (x,y,z), using the method
 * <code>getCartesianPosition</code>.<BR>
 * Axis system of cartesian coordinate system is shown in the final picture:<BR>
 * <div align="center"><img src="../../../ViewCoordinateSystem-Cartesian.jpg"
 * width=256 height=256></div><BR>
 * <BR>
 * 
 * @see CameraMovement
 * @see #getCartesianPosition
 * 
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Camera {

	private Ptolemy3DGLCanvas canvas = null;

	// Camera position.
	private Position position = new Position(0, 0, 5000);
	// Direction defined with a rotation around the vertical axis <i>(axis that
	// goes from the earth center to the longitude/latitude position)</i>.
	protected double direction;
	// Tilt defined with a rotation around the side axis. */
	protected double tilt;
	// Vertical altitude in meters. 0 is the ground reference (ground with no
	// elevation). */
	protected double vertAlt, vertAltDD;

	private double cameraY = 0;
	private double cameraX = 0;

	// Cartesian position
	public final Vector3d cameraPos = new Vector3d();
	public final Matrix9d vpMat = new Matrix9d();
	public final Matrix9d cameraMat = new Matrix9d();
	public final Matrix16d cameraMatInv = Matrix16d.identity();

	// 'Field Of View' in degrees
	public double fov = 60.0;
	// Perspective matrix
	public final Matrix16d perspective = new Matrix16d();
	// Modelview matrix
	public final Matrix16d modelview = new Matrix16d();
	// Frustum
	public final Frustum frustum = new Frustum();

	/**
	 * Creates a new Camera instance.
	 */
	public Camera(Ptolemy3DGLCanvas canvas) {
		this.canvas = canvas;
	}

	/**
	 * Updates the camera view.
	 */
	public void update() {
		/* update perspective */
		updatePerspective();

		/* Update modelview */
		final CameraMovement cameraController = canvas.getCameraMovement();
		cameraController.setCamera(modelview);

		/* Update frustum */
		frustum.update(perspective, modelview);
	}

	/**
	 * Updates the camera perspective.
	 * 
	 * @param gl
	 */
	private final void updatePerspective() {
		final CameraMovement cameraController = canvas.getCameraMovement();
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final Sky sky = Ptolemy3D.getScene().getSky();
		final int fogRadius = sky.getFogRadius();
		final double aspectRatio = canvas.getAspectRatio();

		float farClip = landscape.getFarClip();
		float nearClip = 1f;

		if (vertAlt > sky.horizonAlt) {
			nearClip = ((sky.horizonAlt * Unit.getCoordSystemRatio()) / 2);
			sky.fogStateOn = false;
		} else {
			if (sky.fogStateOn) {
				// minimize our back z clip for precision
				final double gamma = 0.5235987756 - tilt;
				if (gamma > Math3D.HALF_PI) {
					farClip = fogRadius + 10000;
				} else {
					float maxView = (float) (Math.tan(gamma)
							* (position.getAltitudeDD() + cameraController.ground_ht) * 2);
					if (maxView >= fogRadius + 10000) {
						maxView = fogRadius + 10000;
					}
					farClip = maxView;
				}
			} else {
				farClip = fogRadius + 10000;
				sky.fogStateOn = true;
			}
		}

		perspective.setPerspectiveProjection(fov, aspectRatio, nearClip,
				farClip);
	}

	/**
	 * @return the altitude in world space (vertical altitude), zero is the
	 *         altitude of the ground with no elevation.
	 */
	public final double getVerticalAltitudeMeters() {
		return vertAlt;
	}

	/** @return direction: unit is degrees. */
	public final double getDirectionDegrees() {
		return direction * Math3D.RADIAN_TO_DEGREE;
	}

	/** @return direction: unit is radians. */
	public final double getDirectionRadians() {
		return direction;
	}

	/** @return pitch (tilt): unit is degrees. */
	public final double getPitchDegrees() {
		return tilt * Math3D.RADIAN_TO_DEGREE;
	}

	/** @return pitch (tilt): unit is radians. */
	public final double getPitchRadians() {
		return tilt;
	}

	/**
	 * @param position
	 *            destination array to write cartesian position in.
	 */
	public final void getCartesianPosition(Vector3d position) {
		position.set(cameraPos);
	}

	/**
	 * @param left
	 *            destination vector to store left vector
	 */
	public void getLeft(Vector3d left) {
		left.x = -cameraMat.m[0][2];
		left.y = -cameraMat.m[1][2];
		left.z = -cameraMat.m[2][2];
	}

	/**
	 * @param up
	 *            destination vector to store up vector
	 */
	public void getUp(Vector3d up) {
		up.x = cameraMat.m[0][1];
		up.y = cameraMat.m[1][1];
		up.z = cameraMat.m[2][1];
	}

	/**
	 * @param forward
	 *            destination vector to store forward vector
	 */
	public void getForward(Vector3d forward) {
		forward.x = -cameraMat.m[0][2];
		forward.y = -cameraMat.m[1][2];
		forward.z = -cameraMat.m[2][2];
	}

	/** @return the position in the view coordinate system */
	public String toString() {
		Vector3d forward = new Vector3d();
		Vector3d up = new Vector3d();
		Vector3d left = new Vector3d();
		getForward(forward);
		getUp(up);
		getLeft(left);
		return String.format(
			"lat:%f, lon:%f, alt:%f, dir:%f, tilt:%f, x:%f, y:%f, z:%f, forX: %f, forY: %f, forZ: %f, upX: %f, upY: %f, upZ: %f, leftX: %f, leftY: %f, leftZ: %f",
			(float) position.getLatitudeDD(), (float) position.getLongitudeDD(), (float) position.getAltitudeDD(),
			(float) getDirection(),
			tilt,
			(float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z,
			(float) forward.x, (float) forward.y, (float) forward.z,
			(float) up.x, (float) up.y, (float) up.z,
			(float) left.x, (float) left.y, (float) left.z);
	}

	/**
	 * @return the position
	 */
	public Position getPosition() {
		return position;
	}

	/**
	 * @param direction
	 *            the direction to set
	 */
	public void setDirection(double direction) {
		this.direction = direction;
	}

	/**
	 * @param direction
	 *            the direction in degreesto set
	 */
	public void setDirectionDegrees(double direction) {
		this.direction = direction * Math3D.DEGREE_TO_RADIAN;
	}

	/**
	 * @return the direction
	 */
	public double getDirection() {
		return direction;
	}

	/**
	 * @param tilt
	 *            the tilt to set
	 */
	public void setTilt(double tilt) {
		this.tilt = tilt;
	}

	/**
	 * @param tilt
	 *            the tilt in degreesto set
	 */
	public void setTiltDegrees(double tilt) {
		this.tilt = tilt * Math3D.DEGREE_TO_RADIAN;
	}

	/**
	 * @return the tilt
	 */
	public double getTilt() {
		return tilt;
	}

	/**
	 * @param vertAlt
	 *            the vertAlt to set
	 */
	public void setVertAlt(double vertAlt) {
		this.vertAlt = vertAlt;
	}

	/**
	 * @return the vertAlt
	 */
	public double getVertAlt() {
		return vertAlt;
	}

	/**
	 * @param vertAltDD
	 *            the vertAltDD to set
	 */
	public void setVertAltDD(double vertAltDD) {
		this.vertAltDD = vertAltDD;
	}

	/**
	 * @return the vertAltDD
	 */
	public double getVertAltDD() {
		return vertAltDD;
	}

	/**
	 * @param cameraX
	 *            the cameraX to set
	 */
	public void setCameraX(double cameraX) {
		this.cameraX = cameraX;
	}

	/**
	 * @return the cameraX
	 */
	public double getCameraX() {
		return cameraX;
	}

	/**
	 * @param cameraY
	 *            the cameraY to set
	 */
	public void setCameraY(double cameraY) {
		this.cameraY = cameraY;
	}

	/**
	 * @return the cameraY
	 */
	public double getCameraY() {
		return cameraY;
	}

	/**
	 * @param lon
	 *            longitude in DD
	 * @param lat
	 *            latitude in DD
	 * @return true if the point is the visible side of the globe and in the
	 *         view sight (frustum volume).
	 */
	public boolean isPointInView(double lon, double lat) {
		// Normal direction of the point( lon, lat) on the globe
		Vector3d point = new Vector3d();
		Math3D.setSphericalCoord(lon, lat, Unit.EARTH_RADIUS, point);
		return isCartesianPointInView(point);
	}

	/**
	 * @param point
	 *            cartesian coordinate of the point on the globe
	 * @return true if the point is the visible side of the globe and in the
	 *         view sight (frustum volume).
	 */
	public boolean isCartesianPointInView(Vector3d point) {
		// View forward vector
		Vector3d posToPoint = new Vector3d();
		// Direction from the viewer to the point
		getCartesianPosition(posToPoint);
		posToPoint.sub(point, posToPoint);

		// Dot product between the vectors (front/back face test)
		final double dot = posToPoint.dot(point);
		if (dot <= 0) {
			// Visible if the forward vector is pointing in the other direction
			// than the globe normal
			// And the point is inside the view frustum volume
			return frustum.insideFrustum(point);
		} else {
			return false;
		}
	}

	/**
	 * @param lon
	 *            longitude in DD
	 * @param lat
	 *            latitude in DD
	 * @return true if the (lon, lat) point is on the visible side of the globe
	 */
	public boolean isPointInVisibleSide(double lon, double lat) {
		// Normal direction of the point( lon, lat) on the globe
		Vector3d point = new Vector3d();
		Math3D.setSphericalCoord(lon, lat, Unit.EARTH_RADIUS, point);
		return isCartesianPointInVisibleSide(point);
	}

	/**
	 * @param point
	 *            cartesian coordinate of the point on the globe
	 * @return true if the point is on the visible side of the globe
	 */
	public boolean isCartesianPointInVisibleSide(Vector3d point) {
		// View forward vector
		Vector3d posToPoint = new Vector3d();
		// Direction from the viewer to the point
		getCartesianPosition(posToPoint);
		posToPoint.sub(point, posToPoint);

		// Dot product between the vectors (front/back face test)
		final double dot = posToPoint.dot(point);
		if (dot <= 0) {
			// Visible if the forward vector is pointing in the other direction
			// than the globe normal
			return true;
		} else {
			return false;
		}
	}

	private final Vector3d _point1_ = new Vector3d();
	private final Vector3d _point2_ = new Vector3d();
	private final Vector3d _point3_ = new Vector3d();
	private final Vector3d _point4_ = new Vector3d();
	/**
	 * @param lon
	 *            longitude
	 * @param lat
	 *            latitude
	 * @param tileSize
	 *            tile size (square: size == width == height)
	 * @return true if the tile is on the visible side of the globe and in the
	 *         view sight (frustum volume)
	 */
	public boolean isTileInView(int leftLon, int rightLon, int upLat, int botLat,
			double upLeftHeight, double botLeftHeight, double botRightHeight, double upRightHeight) {
		/*
		 * FIXME At north pose for close view, the visibility goes wrong
		 */
		
		// Tile corners
		Math3D.setSphericalCoord(leftLon, upLat, Unit.EARTH_RADIUS + upLeftHeight, _point1_);
		Math3D.setSphericalCoord(leftLon, botLat, Unit.EARTH_RADIUS + botLeftHeight, _point2_);
		Math3D.setSphericalCoord(rightLon, botLat, Unit.EARTH_RADIUS + botRightHeight, _point3_);
		Math3D.setSphericalCoord(rightLon, upLat, Unit.EARTH_RADIUS + upRightHeight, _point4_);

		if (isCartesianPointInVisibleSide(_point1_) || isCartesianPointInVisibleSide(_point2_)
		 || isCartesianPointInVisibleSide(_point3_) || isCartesianPointInVisibleSide(_point4_)) {
			return frustum.insideFrustum(_point1_, _point2_, _point3_, _point4_);
		} else {
			return false;
		}
	}

	public boolean realPointInView(double lon, double alt, double lat, int MAXANGLE) {
		final Vector3d coord = new Vector3d();
		Math3D.setSphericalCoord(lon, lat, Unit.EARTH_RADIUS + alt, coord);
		double angle = Math3D.angle3dvec(-cameraMat.m[0][2],
				-cameraMat.m[1][2], -cameraMat.m[2][2],
				(cameraPos.x - coord.x), (cameraPos.y - coord.y),
				(cameraPos.z - coord.z), true);

		if (angle <= MAXANGLE) {
			return true;
		} else {
			return false;
		}
	}

	public double getScreenScaler(double tx, double ty, double tz, int fms) {
		double dx, dy, dz;
		dx = cameraPos.x - tx;
		dy = cameraPos.y - ty;
		dz = cameraPos.z - tz;

		double kyori = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
		// adjust distance according to angle
		{
			double d = 1.0 / kyori;
			dx *= d;
			dy *= d;
			dz *= d;
		}
		return (kyori * Math.cos(Math3D.angle3dvec(cameraMat.m[0][2],
				cameraMat.m[1][2], cameraMat.m[2][2], dx, dy, dz, false)))
				/ fms;
	}

	/**
	 * Converts a lat/lot position into a cartesian space situated in the globe
	 * surface.
	 * 
	 * @param latDD
	 * @param lonDD
	 * @return
	 */
	public static Vector3d computeCartesianSurfacePoint(double lonDD, double latDD) {
		// Get the terrain elevation
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final double terrainElev = landscape.groundHeight(lonDD, latDD, 0);

		// Transform from lat/lon to cartesion coordinates.
		final Vector3d point = new Vector3d();
		Math3D.setSphericalCoord(lonDD, latDD, Unit.EARTH_RADIUS + terrainElev, point);
		return point;
	}

	/**
	 * Converts a lat/lot position into a cartesian space situated in the globe
	 * surface.
	 * 
	 * @param latDD
	 * @param lonDD
	 * @return
	 */
	public static Vector3d computeCartesianPoint(double lonDD, double latDD, double altitude) {
		// Transform from lat/lon to cartesion coordinates.
		final Vector3d point = new Vector3d();
		Math3D.setSphericalCoord(lonDD, latDD, Unit.EARTH_RADIUS + altitude, point);
		return point;
	}

	/**
	 * Projects a cartesion point in the camera space and returns the pixel
	 * position in OpenGL coordinate space, that is, (0,0) at bottom-lect.
	 * 
	 * @param cartesian
	 */
	public double[] worldToScreen(DrawContext drawContext, Vector3d cartesian) {

		GLU glu = new GLU();
		double model[] = new double[16];
		double proj[] = new double[16];
		int viewport[] = new int[4];

		Ptolemy3DGLCanvas canvas = drawContext.getCanvas();
		viewport[0] = canvas.getX();
		viewport[1] = canvas.getY();
		viewport[2] = canvas.getWidth();
		viewport[3] = canvas.getHeight();

		Camera camera = canvas.getCamera();
		model = camera.modelview.m;
		proj = camera.perspective.m;

		double screen[] = new double[4];
		glu.gluProject(cartesian.x, cartesian.y, cartesian.z, model, 0,
				proj, 0, viewport, 0, screen, 0);

		return screen;
	}
}