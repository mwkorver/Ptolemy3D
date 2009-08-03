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

import static org.ptolemy3d.Unit.EARTH_RADIUS;

import java.awt.event.MouseEvent;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.Unit;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix16d;
import org.ptolemy3d.math.Matrix9d;
import org.ptolemy3d.math.Quaternion4d;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.scene.Landscape;

/**
 * <H1>Camera Movements from Inputs (Mouse/Keyboard)</H1> <BR>
 * Default mouse and keyboard actions are bind to move in the coordinate system
 * describe in the <a href="Camera.html">Camera documentation</a>.<BR>
 * Default binding is describe on the <a
 * href="http://ptolemy3d.org/wiki/PtolemyViewerControls">Ptolemy3D wiki</a>.<BR>
 * <BR>
 * <B><U>FIXME EXPOSE THE INTERFACE FOR USER/CUSTOM KEY/MOUSE BINDING.</B></U><BR>
 * <BR>
 * <BR>
 * <H1>Camera Movements from Automatic Movement (Trajectory)</H1> <BR>
 * Camera movement can also be controlled automatically with trajectory or a
 * location.<BR>
 * Destination location is expressed on the coordinate system describe in the <a
 * href="Camera.html">Camera documentation</a>.<BR>
 * <BR>
 * For that, the camera movement controller provide few utilities :<BR>
 * <ul>
 * <li><u>flyTo</a>:</u> Go to the desirate position throught a trajectory that
 * start from the actual position to the final position.<BR>
 * The movement on the trajectory can be controlled with ???.</li>
 * <li><u>flyToPosition</a>:</u> Go to the desirate position throught a
 * trajectory that start from the actual position to the final position.<BR>
 * The movement on the trajectory can be controlled with a speed or a time
 * parameter.</li>
 * <li><u>setOrientation</a>:</u> Go instantly to the desirate position.</li>
 * </ul>
 * <BR>
 * 
 * @see Camera
 */
public class CameraMovement {

	private final static short UPDATE_INC = 50;
	// public final static double PITCH = -Math3D.DEGREE_TO_RADIAN_FACTOR * 30;

	// Ptolemy3D Instance
	private Ptolemy3DGLCanvas canvas = null;
	// Inputs
	public InputHandler inputs = null;

	// Tells either or not the view is moving
	public boolean isActive = false;
	// Maximum altitude
	public static int MAXIMUM_ALTITUDE = 250000;

	// Flight mode
	private short flightMode = 1;

	// Some variable modified by InputHandler
	protected double mouse_lr_rot_velocity = 0, mouse_ud_rot_velocity = 0;
	protected double ud_rot_velocity = 0, lr_rot_velocity = 0;

	// Result of picking
	private Vector3d intersectPoint = new Vector3d();
	private Matrix9d rotMat = new Matrix9d();
	private Matrix9d cloneMat = new Matrix9d(); // these are used by outside canvas
	private Matrix9d evtclnMat = new Matrix9d();
	private Quaternion4d quat = null;
	private final double scaler = 0.001;
	private Vector3d a_vec = new Vector3d(); // this is used by the rendering loop
	private Vector3d a = new Vector3d(); // used for autopilot operations
	private Vector3d endpoint = new Vector3d();
	private final Vector3d out = new Vector3d(0, 0, 1);
	private final Vector3d dispout = new Vector3d(0, 0, 1);
	private double mmx = 8000.0f, default_mmx = mmx;
	private int Accel = 160;
	private int accel = 160;
	private double rotAng = 0.03;
	protected double rot_accel = (float) (rotAng / 100);
	protected double ground_ht = 0;
	private double default_rot_accel = rot_accel;
	private double rot_vel_scaler = 1;
	private int velocity = 0;
	private int vert_velocity = 0;
	private int horz_velocity = 0;
	private boolean isPan;
	private short updatecounter = UPDATE_INC;
	private Vector3d[] pickray = new Vector3d[3];
	private boolean followDemOn = false;
	private double relspdmult = 1;
	private double desiredTilt;
	private double desiredTiltIncrement;

	/**
	 * Creates a new CameraMovement instance related with the specified canvas
	 * and input handler.
	 * 
	 * @param canvas
	 * @param input
	 */
	public CameraMovement(Ptolemy3DGLCanvas canvas, InputHandler input) {
		this.canvas = canvas;
		this.inputs = input;

		this.desiredTilt = -1;
		this.desiredTiltIncrement = 1;
		
		for(int i = 0; i < pickray.length; i++) {
			pickray[i] = new Vector3d();
		}
	}

	/**
	 * Initialize the instance.
	 */
	public void init() {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();

		flightMode = 0;// 1;
		accel = (int) mmx;
		rot_accel = rotAng;

		// end 2 feb 06 changes
		MAXIMUM_ALTITUDE = 750000;
		landscape.setFarClip(MAXIMUM_ALTITUDE + (EARTH_RADIUS * 2));

		stopMovement();
	}

	protected final void setCamera(Matrix16d modelView) {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final Camera camera = canvas.getCamera();
		final Position camPos = camera.getPosition();

		double vpPos_1_bak = camPos.getAltitudeDD(), tilt_bak = camera.tilt;

		Matrix9d vpMat_bak = new Matrix9d();
		camera.vpMat.copyTo(vpMat_bak);

		isActive = (velocity != 0) || (vert_velocity != 0)
				|| (ud_rot_velocity != 0) || (lr_rot_velocity != 0)
				|| (horz_velocity != 0);

		if (inAutoPilot > 0) {
			switch (inAutoPilot) {
			case 1:
				moveAutopilot();
				break;
			case 2:
				movePanorama();
				break;
			}
		} else {

			int vh_accel = accel;
			double vh_mmx = mmx;

			if (flightMode == 0) {
				// alter velocity and accel according to current altitude.
				mmx = accel = (int) ((2 * (Math.tan(0.5235987756) * (camera
						.getPosition().getAltitudeDD() + 1))) * 25 * relspdmult);
				horz_velocity = vert_velocity = velocity = 0;
				lr_rot_velocity = mouse_lr_rot_velocity;
				ud_rot_velocity = mouse_ud_rot_velocity;
				mouse_lr_rot_velocity = mouse_ud_rot_velocity = 0;
				vh_accel = accel;
				vh_mmx = mmx;
			} else {
				vh_accel = 160;
				vh_mmx = 8000;

				if ((inputs.inputState.getStraightForward() <= 0)
						&& (inputs.inputState.getStraightBack() <= 0)) {
					if (velocity > 0) {
						velocity -= accel;
					} else if (velocity < 0) {
						velocity += accel;
					}
				}
				if ((inputs.inputState.getStrafeLeft() <= 0)
						&& (inputs.inputState.getStrafeRight() <= 0)) {
					if (vert_velocity > 0) {
						vert_velocity -= vh_accel;
					} else if (vert_velocity < 0) {
						vert_velocity += vh_accel;
					}
				}
				if ((inputs.inputState.getIncreaseAltitude() <= 0)
						&& (inputs.inputState.getDecreaseAltitude() <= 0)) {
					if (horz_velocity > 0) {
						horz_velocity -= vh_accel;
					} else if (horz_velocity < 0) {
						horz_velocity += vh_accel;
					}
				}
				if ((inputs.inputState.getTurnLeft() <= 0)
						&& (inputs.inputState.getTurnRight() <= 0)) {
					if (lr_rot_velocity > 0) {
						lr_rot_velocity -= rot_accel;
					} else if (lr_rot_velocity < 0) {
						lr_rot_velocity += rot_accel;
					}
					if (Math.abs(lr_rot_velocity) <= rot_accel) {
						lr_rot_velocity = 0;
					}
				}
				if ((inputs.inputState.getTiltUp() <= 0)
						&& (inputs.inputState.getTiltDown() <= 0)) {
					if (ud_rot_velocity > 0) {
						ud_rot_velocity -= rot_accel;
					} else if (ud_rot_velocity < 0) {
						ud_rot_velocity += rot_accel;
					}
					if (Math.abs(ud_rot_velocity) <= rot_accel) {
						ud_rot_velocity = 0;
					}
				}
			}

			
			a_vec.set(0, 0, 0);

			/* Acceleration due to keys being down */
			if (((inputs.inputState.getStraightForward() | inputs.inputState
					.getStraightForwardConstAlt())) > 0
					&& ((inputs.inputState.getStraightBack() | inputs.inputState
							.getStraightBackConstAlt())) <= 0
					&& (-velocity < mmx)) {
				inputs.inputState.decStraightForward();
				inputs.inputState.decStraightForwardConstAlt();
				velocity -= (velocity > 0) ? accel + accel : accel;
				isPan = ((inputs.inputState.getStraightForwardConstAlt()) > 0) ? false
						: true;
			} else if (((inputs.inputState.getStraightForward() | inputs.inputState
					.getStraightForwardConstAlt())) <= 0
					&& ((inputs.inputState.getStraightBack() | inputs.inputState
							.getStraightBackConstAlt())) > 0
					&& (velocity < mmx)) {
				inputs.inputState.decStraightBack();
				inputs.inputState.decStraightBackConstAlt();
				velocity += (velocity < 0) ? accel + accel : accel;
				isPan = ((inputs.inputState.getStraightBackConstAlt()) > 0) ? false
						: true;
			}

			if ((inputs.inputState.getStrafeLeft() > 0)
					&& (inputs.inputState.getStrafeRight() <= 0)
					&& (-vert_velocity < vh_mmx)) {
				inputs.inputState.decStrafeLeft();
				vert_velocity -= (vert_velocity > 0) ? (vh_accel + vh_accel)
						: vh_accel;
			} else if ((inputs.inputState.getStrafeLeft() <= 0)
					&& (inputs.inputState.getStrafeRight() > 0)
					&& (vert_velocity < vh_mmx)) {
				inputs.inputState.decStrafeRight();
				vert_velocity += (vert_velocity < 0) ? vh_accel + vh_accel
						: vh_accel;
			}
			if ((inputs.inputState.getDecreaseAltitude() > 0)
					&& (inputs.inputState.getIncreaseAltitude() <= 0)
					&& (-horz_velocity < vh_mmx)) {
				inputs.inputState.decDecreaseAltitude();
				horz_velocity -= (horz_velocity > 0) ? vh_accel + vh_accel
						: vh_accel;
			} else if ((inputs.inputState.getDecreaseAltitude() <= 0)
					&& (inputs.inputState.getIncreaseAltitude() > 0)
					&& (horz_velocity < vh_mmx)) {
				inputs.inputState.decIncreaseAltitude();
				horz_velocity += (horz_velocity < 0) ? vh_accel + vh_accel
						: vh_accel;
			}
			if ((inputs.inputState.getDecreaseAltitude() > 0)
					|| (inputs.inputState.getIncreaseAltitude() > 0)) {
				inputs.inputState.decDecreaseAltitude();
				desiredTilt = -1;// cancel setPitchDegrees order
			}
			a_vec.z = velocity * scaler;
			a_vec.x = vert_velocity * scaler;

			/* TURN LEFT AND RIGHT */
			if ((inputs.inputState.getTurnLeft()) > 0
					&& (inputs.inputState.getTurnRight()) <= 0
					&& (-lr_rot_velocity < rotAng)) {
				inputs.inputState.decTurnLeft();
				lr_rot_velocity -= (lr_rot_velocity > 0) ? rot_accel
						+ rot_accel : rot_accel;// * ((JS.unit.UNITS <= 1) ? -1
				// : 1);
			} else if ((inputs.inputState.getTurnLeft()) <= 0
					&& (inputs.inputState.getTurnRight()) > 0
					&& (lr_rot_velocity < rotAng)) {
				inputs.inputState.decTurnRight();
				lr_rot_velocity += (lr_rot_velocity < 0) ? rot_accel
						+ rot_accel : rot_accel;// * ((JSUNITS <= 1) ? -1 : 1);

				/* YAW and PITCH */
			}
			if ((inputs.inputState.getTiltDown()) > 0
					&& (inputs.inputState.getTiltUp()) <= 0
					&& (-ud_rot_velocity < rotAng)) {
				inputs.inputState.decTiltDown();
				ud_rot_velocity -= (ud_rot_velocity > 0) ? rot_accel
						+ rot_accel : rot_accel;
			} else if ((inputs.inputState.getTiltDown()) <= 0
					&& (inputs.inputState.getTiltUp()) > 0
					&& (ud_rot_velocity < rotAng)) {
				inputs.inputState.decTiltUp();
				ud_rot_velocity += (ud_rot_velocity < 0) ? rot_accel
						+ rot_accel : rot_accel;
			}

			/* Addition to existing orientation */
			// if (((lr_rot_velocity != 0.0) || (ud_rot_velocity != 0.0) ||
			// (vert_velocity != 0)) && (false))
			// {
			// camera.vpMat.invert(cloneMat);
			// if (lr_rot_velocity != 0.0)
			// {
			// direction -= lr_rot_velocity;
			// if (direction < 0) {
			// direction += Math3D.twoPi;
			// }
			// if (direction > (Math3D.twoPi)) {
			// direction -= Math3D.twoPi;
			// }
			// rotMat.rotY(lr_rot_velocity);
			// camera.vpMat.copyTo(cloneMat);
			// Matrix3x3d.multiply(cloneMat, rotMat, camera.vpMat);
			// }
			// if (((pitch - ud_rot_velocity) > -((Math3D.halfPI) - 0.005f)) &&
			// ((pitch - ud_rot_velocity) < (Math3D.halfPI)))
			// {
			// pitch -= ud_rot_velocity;
			// if (ud_rot_velocity != 0.0) {
			// rotMat.rotX(ud_rot_velocity);
			// camera.vpMat.copyTo(cloneMat);
			// Matrix3x3d.multiply(rotMat, cloneMat, camera.vpMat);
			// }
			// }
			// else
			// {
			// ud_rot_velocity = 0;
			// /* Rotation of distance vector */
			// }
			// camera.vpMat.invert(cloneMat);
			// }
			if (isPan) {
				rotMat.rotY(camera.direction);
				rotMat.transform(a_vec);
			} else {
				camera.vpMat.transform(a_vec);
			}
			a_vec.y += (horz_velocity * scaler);

			{
				camPos.setAltitudeDD(
						camPos.getAltitudeDD() + velocity
								* scaler); // distance away from surface...

				double min_rot = 0.000002;

				if (camPos.getAltitudeDD() < 1) {
					camPos.setAltitudeDD(1);
				}
				if (camPos.getAltitudeDD() > MAXIMUM_ALTITUDE) {
					camPos.setAltitudeDD(MAXIMUM_ALTITUDE);
				}
				double rot_scaler = 0.000005;
				double rot_y = (lr_rot_velocity * rot_vel_scaler / 10) * Math3D.DEGREE_TO_RADIAN;
				double rot_x = -(ud_rot_velocity * rot_vel_scaler / 10) * Math3D.DEGREE_TO_RADIAN;

				if (flightMode == 0) {
					final double GL_WIDTH = canvas.getWidth();
					final double GL_HEIGHT = canvas.getHeight();

					// this is to cancel out the relative acceleration.
					horz_velocity /= (2 * (0.57735026919189393373967470078005 *
							camera.getPosition().getAltitudeDD()));
					vert_velocity /= (2 * (0.57735026919189393373967470078005 *
							camera.getPosition().getAltitudeDD()));
					rot_y = (((0.57735026919189393373967470078005 * camera
							.getPosition().getAltitudeDD()) / GL_WIDTH)
							* lr_rot_velocity / 10)
							* Math.PI / 180 * relspdmult;
					if ((Math.abs(rot_y) < min_rot) && (rot_y != 0)) {
						rot_y = min_rot * ((rot_y > 0) ? 1 : -1);
					}
					rot_x = -(((0.57735026919189393373967470078005 * camera
							.getPosition().getAltitudeDD()) / GL_HEIGHT)
							* ud_rot_velocity / 10)
							* Math.PI / 180 * relspdmult;
					if ((Math.abs(rot_x) < min_rot) && (rot_x != 0)) {
						rot_x = min_rot * ((rot_x > 0) ? 1 : -1);
					}
					rot_scaler = 0.001;
				}

				if (desiredTilt == -1) {
					camera.tilt += horz_velocity * rot_scaler;
				} else {
					double tiltDiff = desiredTilt - camera.tilt;
					if (Math.abs(tiltDiff) < desiredTiltIncrement) {
						camera.tilt = desiredTilt;
					} else {
						camera.tilt += (desiredTilt > camera.tilt ? desiredTiltIncrement : -desiredTiltIncrement);
					}
				}

				if (camera.getTilt() > 0) {
					camera.setTilt(0);
				} else if (camera.getTilt() < -Math3D.HALF_PI) {
					camera.setTilt(-Math3D.HALF_PI);
				}
				rotMat.rotY(rot_y);
				camera.vpMat.copyTo(cloneMat);
				Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

				rotMat.rotX(rot_x);
				camera.vpMat.copyTo(cloneMat);
				Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

				rotMat.rotZ(vert_velocity * rot_scaler);
				camera.vpMat.copyTo(cloneMat);
				Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);
			}
		} // end of !autopilot condition

		ground_ht = landscape.groundHeight(camPos.getLongitudeDD(), camPos.getLatitudeDD(), 0);
		setCameraMatrix(followDemOn);

		{
			if (!checkAlt() && (inAutoPilot == 0)) {
				vpMat_bak.copyTo(camera.vpMat);
				camera.setTilt(tilt_bak);
				if (vpPos_1_bak > camPos.getAltitudeDD()) // always allow to zoom out
				{
					camPos.setAltitudeDD(vpPos_1_bak);
				}
				setCameraMatrix(followDemOn); // reset the camera matrix
			}

			modelView.identityMatrix();
			modelView.translate(0, 0, -camPos.getAltitudeDD());
			modelView.rotateX(camera.getTilt());
			modelView.translate(0, 0, -EARTH_RADIUS
					- ((followDemOn) ? ground_ht : 0));
			modelView.multiply(camera.vpMat);

			/*** set coordinate information using current matrices ****/
			camPos.setLatitudeDD(Math.asin(camera.vpMat.m[1][2]) * Math3D.RADIAN_TO_DEGREE);
			camPos.setLongitudeDD(Math3D.angle2dvec(-1, 0, camera.vpMat.m[2][2], camera.vpMat.m[0][2], true));
			if (camera.vpMat.m[0][2] >= 0) {
				camPos.setLongitudeDD(-camPos.getLongitudeDD());
				// set camera.direction, use yup angle of current matrix with
				// yup of a north facing one.
				// north facing y up vector
			}
			double yup_x = (Math.sin((Math3D.DEGREE_TO_RADIAN * camera
					.getPosition().getLongitudeDD())) * Math.sin((camera
					.getPosition().getLatitudeDD() * Math3D.DEGREE_TO_RADIAN)));
			double yup_y = Math.cos((Math3D.DEGREE_TO_RADIAN * camera
					.getPosition().getLatitudeDD()));
			double yup_z = (Math.cos((Math3D.DEGREE_TO_RADIAN * camera
					.getPosition().getLongitudeDD())) * Math.sin((camera
					.getPosition().getLatitudeDD() * Math3D.DEGREE_TO_RADIAN)));

			//TODO Strange, antonio can you look at that ?
			camPos.setLongitudeDD(camPos.getLongitudeDD() * Unit.DEGREE_TO_DD_FACTOR);
			camPos.setLatitudeDD(camPos.getLatitudeDD() * Unit.DEGREE_TO_DD_FACTOR);

			camera.setDirection(Math3D.angle3dvec(yup_x, yup_y, yup_z,
					camera.vpMat.m[0][1], camera.vpMat.m[1][1],
					camera.vpMat.m[2][1], false));
			if ((camPos.getLongitudeDD() > (90 * Unit.DEGREE_TO_DD_FACTOR))
					|| (camPos.getLongitudeDD() < (-90 * Unit.DEGREE_TO_DD_FACTOR))) {
				if (((yup_x * camera.vpMat.m[1][1]) - (yup_y * camera.vpMat.m[0][1])) < 0) { // rotate left
					camera.setDirection(Math3D.TWO_PI - camera.getDirection());
				}
			} else {
				if (((yup_x * camera.vpMat.m[1][1]) - (yup_y * camera.vpMat.m[0][1])) > 0) { // rotate left
					camera.setDirection(Math3D.TWO_PI - camera.getDirection());
				}
			}
		}

		// check for any bad numbers, if any unacceptable values set back to 0.
		{
			if (Double.isNaN(camPos.getLongitudeDD())) {
				camPos.setLongitudeDD(0);
			}
			if (Double.isNaN(camPos.getAltitudeDD())) {
				camPos.setAltitudeDD(5000);
			}
			if (Double.isNaN(camPos.getLatitudeDD())) {
				camPos.setLatitudeDD(0);
			}
			if (Double.isNaN(camera.getDirection())) {
				camera.setDirection(0);
			}
			if (Double.isNaN(camera.getTilt())) {
				camera.setTilt(0);
			}
		}

		updatePosition(false);
	}

	private final void setCameraMatrix(boolean addGroundHeight) {
		final Camera camera = canvas.getCamera();

		double groundH = addGroundHeight ? ground_ht : 0;

		// set cameraPos
		rotMat.rotX(-camera.getTilt());
		Matrix9d.multiply(camera.vpMat, rotMat, camera.cameraMat);

		camera.cameraPos.x = (camera.vpMat.m[0][2] * (EARTH_RADIUS + groundH)) + (camera.cameraMat.m[0][2] *
					camera.getPosition().getAltitudeDD());
		camera.cameraPos.y = (camera.vpMat.m[1][2] * (EARTH_RADIUS + groundH)) + (camera.cameraMat.m[1][2] *
					camera.getPosition().getAltitudeDD());
		camera.cameraPos.z = (camera.vpMat.m[2][2] * (EARTH_RADIUS + groundH)) + (camera.cameraMat.m[2][2] *
					camera.getPosition().getAltitudeDD());

		camera.setVertAltDD(camera.cameraPos.magnitude() - EARTH_RADIUS);
		camera.setVertAlt(camera.getVertAltDD() / Unit.getCoordSystemRatio());

		camera.cameraMatInv.copyFrom(camera.cameraMat);
		camera.cameraMatInv.invert3x3();
	}

	private final boolean checkAlt() {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final Camera camera = canvas.getCamera();

		{
			double mag = camera.getVertAltDD() + EARTH_RADIUS;
			camera.setCameraY(Math.asin(camera.cameraPos.y / mag) * Math3D.RADIAN_TO_DEGREE * Unit.DEGREE_TO_DD_FACTOR);
			camera.setCameraX(Math3D.angle2dvec(-1, 0, camera.cameraPos.z / mag, camera.cameraPos.x / mag, true) * Unit.DEGREE_TO_DD_FACTOR);
			if ((camera.cameraPos.x / mag) >= 0) {
				camera.setCameraX(camera.getCameraX() * (-1));
			}
			double calt = landscape.groundHeight(camera.getCameraX(), camera
					.getCameraY(), 0);
			if ((mag - EARTH_RADIUS) < (calt + 25 * Unit.getCoordSystemRatio())) {
				return false;
			}
		}
		return true;
	}

	protected synchronized final void updatePosition(boolean force) {
		if (inputs.inputState.hasInput()) {
			force = false; // if user is still moving around no need to force.
		}
		if (!inputs.inputState.hasInput() && (inAutoPilot == 0) && (!force)
				&& (updatecounter != (UPDATE_INC + 1))) {
			return; // user is not moving, no need to call an update.
		}
		updatecounter++;
		if ((updatecounter > UPDATE_INC) || (force)) {
			// if(ptolemy.javascript != null && ptolemy.javascript.getJSObject()
			// != null) {
			// try {
			// final String[] jsArgs = new String[6];
			// jsArgs[0] = String.valueOf(camera.getCameraX());
			// jsArgs[1] = String.valueOf(camera.getCameraY());
			// jsArgs[2] = String.valueOf(camera.getDirection() *
			// Math3D.RADIAN_TO_DEGREE_FACTOR);
			// jsArgs[3] = String.valueOf(camera.getVerticalAltitudeMeters());
			// jsArgs[4] =
			// String.valueOf(camera.getPosition().getLongitudeDD());
			// jsArgs[5] = String.valueOf(camera.getPosition().getLatitudeDD());
			// ptolemy.javascript.getJSObject().call("updatePosition", jsArgs);
			// }
			// catch(Exception e) {}
			// }
			updatecounter = 0;
		}
	}

	private final void displayCoords() {
		Math3D.setMapCoord(intersectPoint, dispout);
		//Ptolemy3D.callJavascript("plotPosition", new String[]{ String.valueOf(dispout.x), String.valueOf(dispout.y), String.valueOf(dispout.z)});
	}

	public Position getPosition() {
		final Camera camera = canvas.getCamera();
		return camera.getPosition();
	}

	public double getPitch() {
		final Camera camera = canvas.getCamera();
		return camera.getPitchDegrees();
	}

	public double getDirection() {
		final Camera camera = canvas.getCamera();
		return camera.getDirectionDegrees();
	}

	/**
	 * Sets camera position
	 * 
	 * @param lat
	 *            degrees
	 * @param lon
	 *            degrees
	 * @param alt
	 *            meters
	 */
	public void setPosition(double lat, double lon, double alt) {
		Position p = Position.fromLatLonAlt(lat, lon, alt);
		this.setPosition(p);
	}

	/**
	 * Sets the camera position.
	 * 
	 * @param position
	 */
	public void setPosition(Position position) {
		final Camera camera = canvas.getCamera();

		double dir = camera.getDirectionDegrees();
		double pit = camera.getPitchDegrees();

		this.setOrientation(position, dir, pit);
	}

	/**
	 * Sets the camera direction
	 * 
	 * @param dir
	 *            degrees
	 */
	public void setDirection(double dir) {
		final Camera camera = canvas.getCamera();
		setOrientation(camera.getPosition(), dir, camera.getPitchDegrees());
	}

	/**
	 * Set the camera position
	 * 
	 * @param position
	 * @param dir
	 *            direction in degrees
	 * @param pit
	 *            pitch in degrees (from 0 to -90)
	 */
	public void setOrientation(Position position, double dir, double pit) {
		final Camera camera = canvas.getCamera();

		double lon = position.getLongitudeDD();
		double alt = position.getAltitudeDD();
		double lat = position.getLatitudeDD();

		stopMovement();
		inAutoPilot = 0;

		try {
			camera.setDirection(dir * Math3D.DEGREE_TO_RADIAN);

			camera.vpMat.identity();
			rotMat.rotY(((lon / Unit.DEGREE_TO_DD_FACTOR) + 180)
					* Math3D.DEGREE_TO_RADIAN);
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);

			rotMat.rotX((-lat / Unit.DEGREE_TO_DD_FACTOR) * Math3D.DEGREE_TO_RADIAN);
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);

			rotMat.rotZ(camera.getDirection());
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);

			camera.setTilt(pit * Math3D.DEGREE_TO_RADIAN);

			camera.getPosition().setLongitudeDD(lon);
			camera.getPosition().setLatitudeDD(lat);
			camera.getPosition()
					.setAltitudeDD(alt * Unit.getCoordSystemRatio());
		} catch (NumberFormatException e) {
		}

		updatecounter = UPDATE_INC + 1;
	}

	/**
	 * @see #setRealisticFlight(int)
	 */
	public void setVelocity(double nv) {
		if ((nv % accel) == 0) {
			if ((nv >= 100) && (nv <= 640000)) {
				mmx = default_mmx = nv;
			}
		}
	}

	/**
	 * @see #setRealisticFlight(int)
	 */
	public void setAccelleration(int na) {
		if ((mmx % na) == 0) {
			accel = na;
			Accel = na;
		}
	}

	/**
	 * Switch between two fly movement behavior. Key behavior and acceleration
	 * is dependant of the mode choosed. Default value is 0.
	 */
	public void setRealisticFlight(boolean v) {
		if (!v) {
			flightMode = (short) 0;
			accel = (int) mmx;
			rot_accel = rotAng;
		} else {
			flightMode = (short) 1;
			accel = Accel;
			mmx = default_mmx;
			rot_accel = default_rot_accel;
		}
	}

	public int getRealisticFlight() {
		return flightMode;
	}

	/**
	 * View rotation scaler.<BR>
	 * Default value is 1.
	 */
	public void setAngularSpeed(double v) {
		rot_vel_scaler = v / rotAng;
	}

	/** @return the rotation scaler. */
	public double getAngularSpeed() {
		return rot_vel_scaler * rotAng;
	}

	/**
	 * Change the pitch of the camera.
	 * 
	 * @param newPitch
	 *            New pitch specifyed in degrees
	 */
	public void setPitch(double newPitch) {
		this.setPitch(newPitch, desiredTiltIncrement);
	}

	/**
	 * Change the pitch of the camera.
	 * 
	 * @param newPitch
	 *            New pitch specifyed in degrees
	 * @param newPitchIncrement
	 *            New pitch increment in degrees.
	 */
	public void setPitch(double newPitch, double newPitchIncrement) {
		desiredTilt = newPitch * Math3D.DEGREE_TO_RADIAN;
		if (desiredTilt > 0) {
			desiredTilt = 0;
		}
		if (desiredTilt < -Math3D.HALF_PI) {
			desiredTilt = -Math3D.HALF_PI;
		}

		desiredTiltIncrement = newPitchIncrement;
		if (desiredTiltIncrement < 1) {
			desiredTiltIncrement = 1;
		} else if (desiredTiltIncrement > 45) {
			desiredTiltIncrement = 45;
		}
		desiredTiltIncrement *= Math3D.DEGREE_TO_RADIAN;
	}

	public void stopMovement() {
		inputs.inputState.reset();
		velocity = 0;
		vert_velocity = 0;
		ud_rot_velocity = 0;
		lr_rot_velocity = 0;
		horz_velocity = 0;
	}

	/************************** AUTOPILOT ********************************/
	private double angle;
	private boolean isRight = true, isUp = true;
	private double setang;
	private double uangle = 0.0;
	private double halfway;
	public int inAutoPilot = 0;
	private double cruise_alt;
	private double max_rate, current_speed, dist_trav, brake_dist,
			current_lr_rot, rot_lr_trav, rot_lr_brake, current_ud_rot,
			rot_ud_trav, rot_ud_brake;
	private double ud_accel;
	private double total_distance;
	private double auto_x = 0, auto_y = 0, auto_z = 0;
	private double start_y, stopangle;

	private void initAutopilot() {
		setFollowDem(false);
		current_speed = dist_trav = brake_dist = current_lr_rot = rot_lr_trav = rot_lr_brake = current_ud_rot = rot_ud_trav = rot_ud_brake = 0.0;
		stopMovement();
	}

	public boolean getFollowDem() {
		return followDemOn;
	}

	public void setFollowDem(boolean v) {
		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final Camera camera = canvas.getCamera();

		boolean prev = followDemOn;
		followDemOn = v;

		if (prev && !followDemOn) {
			double ht = landscape.groundHeight(camera.getPosition()
					.getLongitudeDD(), camera.getPosition().getLatitudeDD(), 0);

			camera.getPosition().setAltitudeDD(
					camera.getPosition().getAltitudeDD()
							+ (ht / Math.cos(-camera.getTilt())));

			rotMat.rotX(-Math.atan((ht * Math.tan(-camera.getTilt()))
					/ EARTH_RADIUS));
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);
		} else if (!prev && followDemOn) {
			double ht = landscape.groundHeight(camera.getPosition()
					.getLongitudeDD(), camera.getPosition().getLatitudeDD(), 0);

			camera.getPosition().setAltitudeDD(
					camera.getPosition().getAltitudeDD()
							- (ht / Math.cos(-camera.getTilt())));

			rotMat.rotX(Math.atan((ht * Math.tan(-camera.getTilt()))
					/ EARTH_RADIUS));
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);
		}
	}

	/**
	 * Time flight
	 */
	public void flyToPositionTime(Position latLonAlt, double s_direction,
			double s_tilt, double flight_arc, double cruise_tilt, double time) {
		flyToPositionDD(latLonAlt, s_direction, s_tilt, flight_arc, cruise_tilt);
		max_rate = 0.2;
		ud_accel = angle / (time * 1000);

		inAutoPilot = 1;
	}

	/**
	 * Speed flight
	 */
	public void flyToPositionSpeed(Position latLonAlt, double s_direction,
			double s_tilt, double flight_arc, double cruise_tilt, double speed) {
		flyToPositionDD(latLonAlt, s_direction, s_tilt, flight_arc, cruise_tilt);
		// 1 km is approx 2.0 * Math.atan(/EARTH_RADIUS)
		max_rate = (2.0 * Math.atan(500.0 / EARTH_RADIUS) * speed * (0.05))
				* scaler;
		ud_accel = max_rate / 20;

		inAutoPilot = 1;
	}

	/* flyTo Implementation */
	private void flyToPositionDD(Position position, double s_direction,
			double s_tilt, double flight_arc, double cruise_tilt) {
		final Camera camera = canvas.getCamera();

		double lon = position.getLongitudeDD();
		double alt = position.getAltitudeDD();
		double lat = position.getLatitudeDD();

		initAutopilot();
		start_y = camera.getPosition().getAltitudeDD();

		if (Math3D.distance2D(camera.getPosition().getLongitudeDD(), lon,
				camera.getPosition().getLatitudeDD(), lat) < 10) {
			lon += 7;
			lat += 7;
		}

		auto_x = lon;
		auto_y = alt * Unit.getCoordSystemRatio();
		auto_z = lat;

		if ((auto_x >= 180000000) || (auto_x <= -180000000)
				|| (auto_z <= -90000000) || (auto_z >= 90000000)) {
			inAutoPilot = 0;
			return;
		}

		Matrix9d tmpMat = new Matrix9d();

		stopangle = s_tilt * Math3D.DEGREE_TO_RADIAN;

		setCrossVert();

		if ((angle = Math3D.angle3dvec(0, 0, 1, endpoint.x, endpoint.y, endpoint.z, false)) == 0) {
			camera.getPosition().setAltitudeDD(auto_y);
		}
		if (quat == null) {
			quat = new Quaternion4d(); // predict y rotate
		}
		quat.set(angle, a);
		quat.setMatrix(rotMat);

		camera.vpMat.copyTo(tmpMat);
		tmpMat.copyTo(evtclnMat);
		Matrix9d.multiply(evtclnMat, rotMat, tmpMat);

		Vector3d yup = new Vector3d(0, 1, 0);
		Vector3d zout = new Vector3d(tmpMat.m[0][2], tmpMat.m[1][2], tmpMat.m[2][2]);

		endpoint.cross(yup, zout);
		endpoint.normalize();

		// find the angle
		uangle = Math3D.angle3dvec(endpoint.x, endpoint.y, endpoint.z,
				tmpMat.m[0][0], tmpMat.m[1][0], tmpMat.m[2][0], false);

		if (Double.isNaN(uangle) || Double.isNaN(angle)) {
			inAutoPilot = 0;
			return;
		}

		if (tmpMat.m[1][0] < 0) {
			uangle = Math3D.TWO_PI - uangle;
		}
		s_direction *= Math3D.DEGREE_TO_RADIAN;

		// set camera.direction to look
		if (uangle < s_direction) {
			uangle = s_direction - uangle;
			isRight = true;
		} else if (uangle > s_direction) {
			uangle = uangle - s_direction;
			isRight = false;
		} else {
			uangle = 0;
		}
		// fix to see if its closer to go the other way.
		if (uangle > Math.PI) {
			uangle = Math3D.TWO_PI - uangle;
			isRight = (isRight) ? false : true;
		}

		total_distance = Math.abs(camera.getTilt() - stopangle);

		isUp = (camera.getTilt() < stopangle) ? true : false;

		halfway = angle / 2.0;

		if (flight_arc > 50) {
			flight_arc = 50;
		}
		if (flight_arc < 1) {
			flight_arc = 1;
		}
		cruise_alt = (((EARTH_RADIUS * angle) * Math.atan(flight_arc
				* Math3D.DEGREE_TO_RADIAN)) + auto_y);
		if (cruise_alt > 7000000) {
			cruise_alt = 7000000;
		}
		// stopdirection = 0;
	}

	private void moveAutopilot() {
		if ((rot_lr_trav == angle) && (rot_ud_trav == uangle)
				&& (dist_trav == total_distance)) {
			stopAuto();
			return;
		}

		final Camera camera = canvas.getCamera();
		// this block controls spin of the earth to our coordinates
		if (rot_lr_trav != angle) {
			boolean stop = false;
			double adjust = 0;
			if ((current_lr_rot < rotAng) && (rot_lr_trav < halfway)) { // accel
				current_lr_rot += ud_accel;
				rot_lr_brake += current_lr_rot;
			} else { // decel
				if ((angle - rot_lr_trav) <= rot_lr_brake) {
					current_lr_rot -= ud_accel;
				} else if ((angle - rot_lr_trav - current_lr_rot) <= rot_lr_brake) {
					adjust = ((angle - rot_lr_trav) - rot_lr_brake)
							- current_lr_rot + 0.000000001;
				}
			}

			if (current_lr_rot > 0) {
				if ((rot_lr_trav + current_lr_rot + adjust) > angle) {
					current_lr_rot = angle - rot_lr_trav;
					adjust = 0;
					stop = true;
				}
			} else {
				current_lr_rot = angle - rot_lr_trav;
				adjust = 0;
				stop = true;
			}

			if (stop) {
				rot_lr_trav = angle;
			} else {
				rot_lr_trav += adjust + current_lr_rot;
			}
			quat.set(current_lr_rot + adjust, a);
			quat.setMatrix(rotMat);
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

			// control our y pos
			if (rot_lr_trav < halfway) {
				camera
						.getPosition()
						.setAltitudeDD(
								((Math.sin(((Math.PI * rot_lr_trav) / angle)) * (cruise_alt - start_y)) + start_y));
			} else {
				camera
						.getPosition()
						.setAltitudeDD(
								((Math.sin(((Math.PI * rot_lr_trav) / angle)) * (cruise_alt - auto_y)) + auto_y));
			}
			if (camera.getPosition().getAltitudeDD() > MAXIMUM_ALTITUDE) {
				camera.getPosition().setAltitudeDD(MAXIMUM_ALTITUDE);
			}
		}

		// this block controls our z axis rotation, rotation to a north facing
		// position
		if (rot_ud_trav != uangle) {
			if ((current_ud_rot < rotAng) && (rot_ud_trav < (uangle / 2))) {
				current_ud_rot += default_rot_accel;
				rot_ud_brake += current_ud_rot;
			} else if ((uangle - rot_ud_trav) <= rot_ud_brake) {
				current_ud_rot -= default_rot_accel;
			}
			if ((rot_ud_trav + current_ud_rot < uangle) && (current_ud_rot > 0)) {
				rot_ud_trav += current_ud_rot;
			} else {
				current_ud_rot = uangle - rot_ud_trav;
				rot_ud_trav = uangle;
			}

			rotMat.rotZ(((isRight) ? current_ud_rot : -current_ud_rot));
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);
			// reset twist axis
			setCrossVert();
		}

		// and this is camera.tilt.
		if (camera.getTilt() != stopangle) {
			if ((current_speed < rotAng) && (dist_trav < (total_distance / 2))) {
				current_speed += default_rot_accel;
				brake_dist += current_speed;
			} else if ((total_distance - dist_trav) <= brake_dist) {
				current_speed -= default_rot_accel;
			}
			if ((dist_trav + current_speed < total_distance)
					&& (current_speed > 0)) {
				dist_trav += current_speed;
			} else {
				current_speed = total_distance - dist_trav;
				dist_trav = total_distance;
				camera.setTilt(stopangle);
			}
			camera.setTilt(camera.getTilt()
					+ (current_speed * ((isUp) ? 1 : -1)));
		}
	}

	/**
	 * Fly to specifying a target in meters and degrees.
	 * 
	 * @param lat
	 * @param lon
	 * @param alt
	 */
	public void flyTo(double lat, double lon, double alt) {
		Position position = Position.fromLatLonAlt(lat, lon, alt);
		flyTo(position, -1, 0, 0, 0, 0);
	}

	/**
	 * Fly to specifying a target in meters and degrees.
	 * 
	 * @param lat
	 * @param lon
	 * @param alt
	 */
	public void flyTo(double lat, double lon, double alt, int speed,
			int ld_angle, int fly_angle, int type, int arc_angle) {

		Position position = Position.fromLatLonAlt(lat, lon, alt);
		flyTo(position, speed, ld_angle, fly_angle, type, arc_angle);
	}

	/**
	 * @param type
	 *            0=LOOP, 1=LINE
	 */
	public void flyTo(Position latLonAlt, int speed, int ld_angle,
			int fly_angle, int type, int arc_angle) {
		final Camera camera = canvas.getCamera();

		initAutopilot();

		start_y = camera.getPosition().getAltitudeDD();

		flyToDD(latLonAlt, speed, ld_angle, fly_angle, type, arc_angle);

		inAutoPilot = 1;
	}

	/* flyTo implementation */
	private void flyToDD(Position position, int speed, int ld_angle,
			int fly_angle, int type, int arc_angle) {
		final Camera camera = canvas.getCamera();

		double lon = position.getLongitudeDD();
		double alt = position.getAltitudeDD();
		double lat = position.getLatitudeDD();

		if (Math3D.distance2D(camera.getPosition().getLongitudeDD(), lon,
				camera.getPosition().getLatitudeDD(), lat) < 10) {
			lon += 7;
			lat += 7;
		}

		auto_x = lon;
		auto_y = alt * Unit.getCoordSystemRatio();
		auto_z = lat;

		if ((auto_x >= 180000000) || (auto_x <= -180000000)
				|| (auto_z <= -90000000) || (auto_z >= 90000000)) {
			inAutoPilot = 0;
			return;
		}

		Matrix9d tmpMat = new Matrix9d();

		setCrossVert();

		if ((angle = Math3D.angle3dvec(0, 0, 1, endpoint.x, endpoint.y, endpoint.z, false)) == 0) {
			camera.getPosition().setAltitudeDD(auto_y);
		}
		if (quat == null) {
			quat = new Quaternion4d(); // predict y rotate
		}
		quat.set(angle, a);
		quat.setMatrix(rotMat);

		camera.vpMat.copyTo(tmpMat);
		tmpMat.copyTo(evtclnMat);
		Matrix9d.multiply(evtclnMat, rotMat, tmpMat);

		Vector3d yup = new Vector3d(0, 1, 0);
		Vector3d zout = new Vector3d(tmpMat.m[0][2], tmpMat.m[1][2], tmpMat.m[2][2]);

		endpoint.cross(yup, zout);
		endpoint.normalize();

		uangle = Math3D.angle3dvec(endpoint.x, endpoint.y, endpoint.z,
				tmpMat.m[0][0], tmpMat.m[1][0], tmpMat.m[2][0], false);
		if (Double.isNaN(uangle) || Double.isNaN(angle)) {
			inAutoPilot = 0;
			return;
		}
		isRight = true;

		if (tmpMat.m[1][0] > 0) {
			isRight = false;
		}
		stopangle = ld_angle * Math3D.DEGREE_TO_RADIAN;
		total_distance = Math.abs(camera.getTilt() - stopangle);
		isUp = (camera.getTilt() < stopangle) ? true : false;

		ud_accel = angle / 10000;
		max_rate = 0.2;

		halfway = angle / 2.0;
		cruise_alt = auto_y
				+ ((type == 0) ? (EARTH_RADIUS * angle)
						* (Math.atan(arc_angle * Math3D.DEGREE_TO_RADIAN)) : 0);
		if (cruise_alt > 7000000) {
			cruise_alt = 7000000;
		}
		// stopdirection = 0;
	}

	private void stopAuto() {
		updatePosition(true);
		inAutoPilot = 0;
		try {
			// ptolemy.javascript.getJSObject().call("onAutoPilotStop", null);
		} catch (Exception e) {
		}
	}

	private void setCrossVert() {
		final Camera camera = canvas.getCamera();

		Matrix9d pointerMat = new Matrix9d();

		Math3D.setSphericalCoord(auto_x, auto_z, 1, endpoint);

		camera.vpMat.copyTo(pointerMat);
		pointerMat.invert(evtclnMat);
		pointerMat.transform(endpoint);
		endpoint.normalize();

		a.cross(out, endpoint);
		a.normalize();
	}

	/****************************** functions for panorama ************************************/
	public void panorama(Position latLonAlt, int rtimes, double rspeed,
			double rpitch) {
		final Camera camera = canvas.getCamera();
		initAutopilot();

		setang = rpitch;
		total_distance = latLonAlt.getAltitudeDD();
		rot_lr_trav = (rtimes == 0) ? Integer.MAX_VALUE : rtimes; // use a var
		// from
		// autopilot...

		current_lr_rot = rspeed * Math.abs(rspeed) * 0.001;

		if (rspeed > 0) {
			rot_lr_trav--;
		}
		setFollowDem(false);
		setOrientation(latLonAlt, 0, rpitch);

		{
			setCameraMatrix(false);
			halfway = (camera.cameraPos.magnitude() - EARTH_RADIUS);
			cruise_alt = Math.atan((halfway * Math.tan(setang * Math3D.DEGREE_TO_RADIAN)) / EARTH_RADIUS);
			// move forward to center camera over point.
			rotMat.rotX(cruise_alt);
			camera.vpMat.copyTo(evtclnMat);
			Matrix9d.multiply(evtclnMat, rotMat, camera.vpMat);
		}
		inAutoPilot = 2;
	}

	private void movePanorama() {
		if (rot_lr_trav < 0) {
			stopAuto();
			return;
		}

		final Camera camera = canvas.getCamera();
		camera.setDirection(camera.getDirection() + current_lr_rot);

		if (camera.getDirection() < 0) {
			camera.setDirection(camera.getDirection() + Math3D.TWO_PI);
			rot_lr_trav--;
		}
		if (camera.getDirection() > (Math3D.TWO_PI)) {
			camera.setDirection(camera.getDirection() - Math3D.TWO_PI);
			rot_lr_trav--;
		}

		{
			// move back
			rotMat.rotX(-cruise_alt);
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

			rotMat.rotZ(current_lr_rot);
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);

			// move forward
			rotMat.rotX(cruise_alt);
			camera.vpMat.copyTo(cloneMat);
			Matrix9d.multiply(cloneMat, rotMat, camera.vpMat);
		}
	}

	/*****************************************************************************************/
	protected synchronized void outputCoordinates(MouseEvent e) {
		setRays(e);
		if (!Ptolemy3D.getScene().getPlugins().pick(intersectPoint, pickray)) {
			if (Ptolemy3D.getScene().getLandscape().globe.pick(intersectPoint, pickray)) {
				if (!Ptolemy3D.getScene().getPlugins().onPick(intersectPoint)) {
					displayCoords();
				}
			}
		}
	}

	protected void zoomToSelected(MouseEvent e, boolean zoomin) {
		setRays(e);
		if (!Ptolemy3D.getScene().getLandscape().globe.pick(intersectPoint, pickray)) {
			return;
		}

		final Landscape landscape = Ptolemy3D.getScene().getLandscape();
		final Camera camera = canvas.getCamera();

		if ((intersectPoint.x != -999) || (intersectPoint.y != -999) || (intersectPoint.z != -999)) {
			double tx, tz, ty;
			{
				Vector3d intvec = new Vector3d(intersectPoint.x, intersectPoint.y, intersectPoint.z);
				intvec.normalize();
				double o_lon = Math3D.angle2dvec(-1, 0, intvec.z, intvec.x, true);
				if (intvec.x >= 0) {
					o_lon = -o_lon;
				}
				tz = Math.asin(intvec.y) * Math3D.RADIAN_TO_DEGREE * Unit.DEGREE_TO_DD_FACTOR;
				tx = o_lon * Unit.DEGREE_TO_DD_FACTOR;
				if (Double.isNaN(tx) || Double.isNaN(tz)) {
					return;
				}
				ty = camera.getPosition().getAltitudeDD() / Unit.getCoordSystemRatio();
				if (zoomin) {
					ty /= 4;
					double efloor = landscape.groundHeight(tx, tz, 0) / Unit.getCoordSystemRatio() + 50;
					// floor of 100 meters
					if (ty < efloor) {
						ty = efloor;
					}
				}
				flyTo(new Position(tz, tx, ty), -1, 0, 0, 0, 0);
				ud_accel = angle / 1000;
				max_rate = 2;
			}
		}

	}

	private void setRays(MouseEvent e) {
		final Camera camera = canvas.getCamera();
		final int width = canvas.getWidth();
		final int height = canvas.getHeight();

		// number of pixels from view center to top/right edge of viewport
		double pixelCenterX = width / 2.0, pixelCenterY = height / 2.0;
		// world distance from view center to top edge of viewport (on near viewplane)
		double worldFOVDistY = Math.tan(Math3D.DEGREE_TO_RADIAN * (camera.fov / 2.0));
		// distance from view center to right edge of viewport (on near viewplane)
		double worldFOVDistX = worldFOVDistY * (width / (double) height);

		// horiz distance from view center to clicked point (on near viewplane)
		double rightDist = ((e.getX() - pixelCenterX) / pixelCenterX) * worldFOVDistX;
		// vert distance from view center to clicked point (on near viewplane)
		double upDist = ((pixelCenterY - e.getY()) / pixelCenterY) * worldFOVDistY;

		pickray[0].x = camera.cameraMat.m[0][0] * rightDist +
			camera.cameraMat.m[0][1] * upDist - camera.cameraMat.m[0][2];
		pickray[0].y = camera.cameraMat.m[1][0] * rightDist +
			camera.cameraMat.m[1][1] * upDist - camera.cameraMat.m[1][2];
		pickray[0].z = camera.cameraMat.m[2][0] * rightDist +
			camera.cameraMat.m[2][1] * upDist - camera.cameraMat.m[2][2];

		pickray[1].add(camera.cameraPos, pickray[0]);

		pickray[0].set(camera.cameraPos);
	}

	public synchronized void setRelativeMotionSpeedMultiplier(double rsm) {
		if (rsm > 0) {
			relspdmult = rsm;
		}
	}
}