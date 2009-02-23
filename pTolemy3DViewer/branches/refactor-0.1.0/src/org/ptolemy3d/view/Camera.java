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

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix16d;
import org.ptolemy3d.math.Matrix9d;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Sky;

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
 */
public class Camera {

	// Ptolemy instance that camera renders.
	private Ptolemy3DGLCanvas canvas = null;
	private Ptolemy3D ptolemy = null;

	// Camera position.
	private Position position = Position.fromDD(0, 0, 5000);
	// Direction, in radians, defined with a rotation around the vertical axis
	// <i>(axis that goes from the earth center to the longitude/latitude
	// position)</i>.
	private double direction = 0;
	// Tilt defined with a rotation around the side axis.
	private double tilt = 0;
	// Vertical altitude in meters. 0 is the ground reference (ground with no
	// elevation).
	private double vertAlt = 0;
	private double vertAltDD = 0;

	private double cameraY = 0;
	private double cameraX = 0;

	// Cartesian position
	public double[] cameraPos = new double[3];

	public Matrix9d vpMat = new Matrix9d();
	public Matrix9d cameraMat = new Matrix9d();
	public Matrix16d cameraMatInv = Matrix16d.identity();

	// 'Field Of View' in degrees
	public final double fov = 60.0;
	// Perspective matrix
	public Matrix16d perspective = new Matrix16d();
	// Modelview matrix
	public Matrix16d modelview = new Matrix16d();

	/**
	 * Creates a new Camera instance.
	 */
	public Camera(Ptolemy3DGLCanvas canvas) {
		this.canvas = canvas;
		this.ptolemy = canvas.getPtolemy();
	}

	/**
	 * Updates the camera view.
	 * 
	 * @param gl
	 */
	public void update(GL gl) {
		/* update perspective */
		updatePerspective(gl);

		/* Update modelview */
		CameraMovement cameraController = ptolemy.cameraController;
		try {
			cameraController.setCamera(modelview);
		} catch (Exception e) {
			IO.printError(e.getMessage());
			cameraController.stopMovement();
			modelview.identityMatrix();
		}
	}

	/**
	 * Updates the camera perspective.
	 * 
	 * @param gl
	 */
	private final void updatePerspective(GL gl) {
		final Ptolemy3DUnit unit = ptolemy.unit;
		final CameraMovement cameraController = ptolemy.cameraController;
		final Landscape landscape = ptolemy.scene.landscape;
		final Sky sky = ptolemy.scene.sky;
		final int FAR_CLIP = landscape.getFarClip();
		final int FOG_RADIUS = landscape.getFogRadius();
		final double aspectRatio = canvas.getDrawAspectRatio();

		// maximize our precision
		if (sky.fogStateOn) {
			if (vertAlt > sky.horizonAlt) {
				gl.glDisable(GL.GL_FOG);
				perspective.setPerspectiveProjection(fov, aspectRatio,
						((sky.horizonAlt * unit.getCoordSystemRatio()) / 2),
						FAR_CLIP);
				sky.fogStateOn = false;
			} else {
				// minimize our back z clip for precision
				double gamma = 0.5235987756 - tilt;
				if (gamma > Math3D.HALF_PI_VALUE) {
					perspective.setPerspectiveProjection(fov, aspectRatio, 1,
							FOG_RADIUS + 10000);
				} else {
					double maxView = Math.tan(gamma)
							* (position.getAltitudeDD() + cameraController.ground_ht)
							* 2;
					if (maxView >= FOG_RADIUS + 10000) {
						perspective.setPerspectiveProjection(fov, aspectRatio,
								1, FOG_RADIUS + 10000);
					} else {
						perspective.setPerspectiveProjection(fov, aspectRatio,
								1, maxView);
					}
				}
			}
		} else {
			if (vertAlt > sky.horizonAlt) {
				// On resize, force update aspectRatio
				perspective.setPerspectiveProjection(fov, aspectRatio,
						((sky.horizonAlt * unit.getCoordSystemRatio()) / 2),
						FAR_CLIP);
			} else {
				gl.glEnable(GL.GL_FOG);
				perspective.setPerspectiveProjection(fov, aspectRatio, 1,
						FOG_RADIUS + 10000);
				sky.fogStateOn = true;
			}
		}
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
		return direction * Math3D.RADIAN_TO_DEGREE_FACTOR;
	}

	/** @return direction: unit is radians. */
	public final double getDirectionRadians() {
		return direction;
	}

	/** @return pitch (tilt): unit is degrees. */
	public final double getPitchDegrees() {
		return tilt * Math3D.RADIAN_TO_DEGREE_FACTOR;
	}

	/** @return pitch (tilt): unit is radians. */
	public final double getPitchRadians() {
		return tilt;
	}

	/**
	 * @param position
	 *            destination array to write cartesian position in.
	 */
	public final void getCartesianPosition(double[] position) {
		position[0] = cameraPos[0];
		position[1] = cameraPos[1];
		position[2] = cameraPos[2];
	}

	/**
	 * @param left
	 *            destination vector to store left vector
	 */
	public void getLeft(double[] left) {
		left[0] = -cameraMat.m[0][2];
		left[1] = -cameraMat.m[1][2];
		left[2] = -cameraMat.m[2][2];
	}

	/**
	 * @param up
	 *            destination vector to store up vector
	 */
	public void getUp(double[] up) {
		up[0] = cameraMat.m[0][1];
		up[1] = cameraMat.m[1][1];
		up[2] = cameraMat.m[2][1];
	}

	/**
	 * @param forward
	 *            destination vector to store forward vector
	 */
	public void getForward(double[] forward) {
		forward[0] = -cameraMat.m[0][2];
		forward[1] = -cameraMat.m[1][2];
		forward[2] = -cameraMat.m[2][2];
	}

	/** @return the position in the view coordinate system */
	public String toString() {
		return String.format(
				"lon:%f, lat:%f, alt:%f, dir:%f, tilt:%f, x:%f, y:%f, z:%f",
				(float) position.lon, (float) position.lat,
				(float) position.alt, (float) getDirection(), tilt,
				(float) cameraPos[0], (float) cameraPos[1],
				(float) cameraPos[2]);
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
}