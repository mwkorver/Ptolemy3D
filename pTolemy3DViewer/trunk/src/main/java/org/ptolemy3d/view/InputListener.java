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

import static org.ptolemy3d.debug.Config.DEBUG;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;

/**
 * Key and mouse inputs.
 */
public class InputListener implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener
{
	/** Associate action and keyboard/mouse*/
	public static class InputConfig {
		public static class Input {
			/** KeyEvent.VK_*** */
			public int key = -1;
			/** MouseEvent.BUTTON* */
			public int button = 0;
			/** + */
			public int wheel = 0;
			public Input(int keyCode, int button, int wheelDir) {
				this.button = button;
				this.key = keyCode;
				this.wheel = wheelDir;
			}
		}
		public Input straightForward         = new Input(KeyEvent.VK_A, 0, -10);
		public Input straightForwardConstAlt = new Input(KeyEvent.VK_S, 0, 0);
		public Input straightBack            = new Input(KeyEvent.VK_Z, 0, 10);
		public Input straightBackConstAlt    = new Input(KeyEvent.VK_X, 0, 0);
		public Input strafeLeft              = new Input(KeyEvent.VK_D, 0, 0);
		public Input strafeRight             = new Input(KeyEvent.VK_F, 0, 0);
		public Input increaseAltitude        = new Input(KeyEvent.VK_R, 0, 0);
		public Input decreaseAltitude        = new Input(KeyEvent.VK_C, 0, 0);
		public Input turnLeft                = new Input(KeyEvent.VK_LEFT, 0, 0);
		public Input turnRight               = new Input(KeyEvent.VK_RIGHT, 0, 0);
		public Input tiltUp                  = new Input(KeyEvent.VK_UP, 0, 0);
		public Input tiltDown                = new Input(KeyEvent.VK_DOWN, 0, 0);
	}
	/** Action state */
	public static class InputState {
		public int straightForward;
		public int straightForwardConstAlt;
		public int straightBack;
		public int straightBackConstAlt;
		public int strafeLeft;
		public int strafeRight;
		public int increaseAltitude;
		public int decreaseAltitude;
		public int turnLeft;
		public int turnRight;
		public int tiltUp;
		public int tiltDown;
		
		private InputState() {
			reset();
		}
		public void reset() {
			straightForward         = 0;
			straightForwardConstAlt = 0;
			straightBack            = 0;
			straightBackConstAlt    = 0;
			strafeLeft              = 0;
			strafeRight             = 0;
			increaseAltitude        = 0;
			decreaseAltitude        = 0;
			turnLeft                = 0;
			turnRight               = 0;
			tiltUp                  = 0;
			tiltDown                = 0;
		}
		public boolean hasInput() {
			return 0 != 
				straightForward *
				straightForwardConstAlt *
				straightBack *
				straightBackConstAlt *
				strafeLeft *
				strafeRight *
				increaseAltitude *
				decreaseAltitude *
				turnLeft *
				turnRight *
				tiltUp *
				tiltDown;
		}
		
		private void keyPressed(InputConfig inputConfig, int keyCode) {
			if(keyCode == inputConfig.straightForward.key) {
				straightForward = 1;
			}
			else if(keyCode == inputConfig.straightForwardConstAlt.key) {
				straightForwardConstAlt = 1;
			}
			else if(keyCode == inputConfig.straightBack.key) {
				straightBack = 1;
			}
			else if(keyCode == inputConfig.straightBackConstAlt.key) {
				straightBackConstAlt = 1;
			}
			else if(keyCode == inputConfig.turnLeft.key) {
				turnLeft = 1;
			}
			else if(keyCode == inputConfig.turnRight.key) {
				turnRight = 1;
			}
			else if(keyCode == inputConfig.tiltUp.key) {
				tiltUp = 1;
			}
			else if(keyCode == inputConfig.tiltDown.key) {
				tiltDown = 1;
			}
			else if(keyCode == inputConfig.strafeLeft.key) {
				strafeLeft = 1;
			}
			else if(keyCode == inputConfig.strafeRight.key) {
				strafeRight = 1;
			}
			else if(keyCode == inputConfig.increaseAltitude.key) {
				increaseAltitude = 1;
			}
			else if(keyCode == inputConfig.decreaseAltitude.key) {
				decreaseAltitude = 1;
			}
		}
		private void keyReleased(InputConfig inputConfig, int keyCode) {
			if(keyCode == inputConfig.straightForward.key) {
				straightForward = 0;
			}
			else if(keyCode == inputConfig.straightForwardConstAlt.key) {
				straightForwardConstAlt = 0;
			}
			else if(keyCode == inputConfig.straightBack.key) {
				straightBack = 0;
			}
			else if(keyCode == inputConfig.straightBackConstAlt.key) {
				straightBackConstAlt = 0;
			}
			else if(keyCode == inputConfig.turnLeft.key) {
				turnLeft = 0;
			}
			else if(keyCode == inputConfig.turnRight.key) {
				turnRight = 0;
			}
			else if(keyCode == inputConfig.tiltUp.key) {
				tiltUp = 0;
			}
			else if(keyCode == inputConfig.tiltDown.key) {
				tiltDown = 0;
			}
			else if(keyCode == inputConfig.strafeLeft.key) {
				strafeLeft = 0;
			}
			else if(keyCode == inputConfig.strafeRight.key) {
				strafeRight = 0;
			}
			else if(keyCode == inputConfig.increaseAltitude.key) {
				increaseAltitude = 0;
			}
			else if(keyCode == inputConfig.decreaseAltitude.key) {
				decreaseAltitude = 0;
			}
		}
		public void wheel(InputConfig inputConfig, int wheelCount) {
			if(inputConfig.straightForward.wheel != 0) {
				int w = wheelCount * inputConfig.straightForward.wheel;
				if(w >= 0) {
					straightForward = w;
					straightBack = 0;
				}
			}
			if(inputConfig.straightBack.wheel != 0) {
				int w = wheelCount * inputConfig.straightBack.wheel;
				if(w >= 0) {
					straightBack = w;
					straightForward = 0;
				}
			}
		}
	}

	public final static int OUTPUT_COORDINATES = 0;
	public final static int CENTER_IN = 1;
	public final static int ZOOM_IN = 2;

	/** Ptolemy3D Instance */
	private final Ptolemy3D ptolemy;
	/** Keyboard enabled ? */
	public boolean keyboardEnabled = true;
	/** Key/Mouse configuration */
	public final InputConfig inputConfig = new InputConfig();
	/** Key/Mouse states */
	public final InputState inputState = new InputState();
	/** Current mouse position */
	private Point mousePoint = null;
	/** Mouse moving flag, used to know when mousePressed event have been triggered. */
	private boolean mouseMoveFlag = false;
	/** */
	public int currentFunction = OUTPUT_COORDINATES;

	protected InputListener(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
	}

	@SuppressWarnings("deprecation")
	public final void asciiKeyPress(int keyCode)
	{
		keyPressed(new KeyEvent(null, 0, 0, 0, keyCode));
	}
	@SuppressWarnings("deprecation")
	public final void asciiKeyRelease(int keyCode)
	{
		keyReleased(new KeyEvent(null, 0, 0, 0, keyCode));
	}

	/* ************************* KeyListener ************************* */

	public void keyPressed(KeyEvent e)
	{
		ptolemy.cameraController.inAutoPilot = 0;
		if (ptolemy.tileLoader.isSleeping) {
			ptolemy.tileLoaderThread.interrupt();
		}

		inputState.keyPressed(inputConfig, e.getKeyCode());
	}
	public void keyReleased(KeyEvent e)
	{
		inputState.keyReleased(inputConfig, e.getKeyCode());
		ptolemy.cameraController.updatePosition(true);
		
		if(DEBUG) {
			IO.keyReleasedDebug(ptolemy, e.getKeyCode());
		}
	}
	public void keyTyped(KeyEvent e)
	{
	}

	/* ************************* MouseListener ************************* */

	public void mouseClicked(MouseEvent e)
	{
		switch (currentFunction) {
			case OUTPUT_COORDINATES:
				ptolemy.cameraController.outputCoordinates(e);
				break;
			case ZOOM_IN:
				ptolemy.cameraController.zoomToSelected(e, true);
				break;
			case CENTER_IN:
				ptolemy.cameraController.zoomToSelected(e, false);
				break;
		}
	}
	public void mouseEntered(MouseEvent e)
	{
	}
	public void mouseExited(MouseEvent e)
	{
	}
	public void mousePressed(MouseEvent e)
	{
		ptolemy.cameraController.inAutoPilot = 0;
		if (mouseMoveFlag == false) {  // start drag
			mouseMoveFlag = true;
			mousePoint = e.getPoint();
		}
	}
	public void mouseReleased(MouseEvent e)
	{
		mouseMoveFlag = false;
		ptolemy.cameraController.updatePosition(true);
	}
	
	/* ************************* MouseMotionListener ************************* */
	
	public void mouseDragged(MouseEvent e)
	{
		if (mouseMoveFlag == true) {
			if (ptolemy.tileLoader.isSleeping) {
				ptolemy.tileLoaderThread.interrupt();
			}

			Point oldMousePoint = mousePoint;
			mousePoint = e.getPoint();

			final CameraMovement cameraController = ptolemy.cameraController;

			if (cameraController.fmode == 0)
			{
				cameraController.mouse_lr_rot_velocity = (double) (oldMousePoint.x - mousePoint.x) / 150;
				cameraController.mouse_ud_rot_velocity = (double) (mousePoint.y - oldMousePoint.y) / 150;
			}
			else
			{
				final double rot_accel = cameraController.rot_accel;
				cameraController.lr_rot_velocity += (oldMousePoint.x - mousePoint.x) * rot_accel;
				cameraController.ud_rot_velocity += (mousePoint.y - oldMousePoint.y) * rot_accel;
			}
		}
	}
	public void mouseMoved(MouseEvent e)
	{

	}

	/* ************************* MouseWheelListener ************************* */
	
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int wheelCount = e.getWheelRotation();
		inputState.wheel(inputConfig, wheelCount);
	}
}
