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
import java.security.InvalidParameterException;
import java.util.Arrays;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.view.InputListener.InputConfig.Input;

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
		public Input straightBack            = new Input(KeyEvent.VK_Z, 0, 10);
		public Input straightForwardConstAlt = new Input(KeyEvent.VK_S, 0, 0);
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
		private int[] inputStates;
		public int getStraightForward() {return inputStates[0];} public void decStraightForward() {dec(0);}
		public int getStraightBack() {return inputStates[1];} public void decStraightBack() {dec(1);}
		public int getStraightForwardConstAlt() {return inputStates[2];} public void decStraightForwardConstAlt() {dec(2);}
		public int getStraightBackConstAlt() {return inputStates[3];} public void decStraightBackConstAlt() {dec(3);}
		public int getTurnLeft() {return inputStates[4];} public void decTurnLeft() {dec(4);}
		public int getTurnRight() {return inputStates[5];} public void decTurnRight() {dec(5);}
		public int getTiltUp() {return inputStates[6];} public void decTiltUp() {dec(6);}
		public int getTiltDown() {return inputStates[7];} public void decTiltDown() {dec(7);}
		public int getStrafeLeft() {return inputStates[8];} public void decStrafeLeft() {dec(8);}
		public int getStrafeRight() {return inputStates[9];} public void decStrafeRight() {dec(9);}
		public int getIncreaseAltitude() {return inputStates[10];} public void decIncreaseAltitude() {dec(10);}
		public int getDecreaseAltitude() {return inputStates[11];} public void decDecreaseAltitude() {dec(11);}
		private final void dec(int index) {
			if(inputStates[index] > 0) {
				inputStates[index]--;
			}
		}
		private final static int maxNumInputs = 12;
		
		private InputState() {
			inputStates = new int[maxNumInputs];
			reset();
		}
		public void reset() {
			Arrays.fill(inputStates, 0);
		}
		public boolean hasInput() {
			int mult = 0;
			for(int value : inputStates) {
				mult *= value;
			}
			return mult != 0;
		}
		
		private Input getInput(InputConfig inputConfig, int index) {
			switch(index) {
				case 0:  return inputConfig.straightForward;
				case 1:  return inputConfig.straightBack;
				case 2:  return inputConfig.straightForwardConstAlt;
				case 3:  return inputConfig.straightBackConstAlt;
				case 4:  return inputConfig.turnLeft;
				case 5:  return inputConfig.turnRight;
				case 6:  return inputConfig.tiltUp;
				case 7:  return inputConfig.tiltDown;
				case 8:  return inputConfig.strafeLeft;
				case 9:  return inputConfig.strafeRight;
				case 10: return inputConfig.increaseAltitude;
				case 11: return inputConfig.decreaseAltitude;
			}
			throw new InvalidParameterException();
		}
		
		private void keyPressed(InputConfig inputConfig, int keyCode) {
			for(int i = 0; i < inputStates.length; i++) {
				final Input input = getInput(inputConfig, i);
				if(keyCode == input.key) {
					inputStates[i] = 1000;
				}
			}
		}
		private void keyReleased(InputConfig inputConfig, int keyCode) {
			for(int i = 0; i < inputStates.length; i++) {
				final Input input = getInput(inputConfig, i);
				if(keyCode == input.key) {
					inputStates[i] = 0;
				}
			}
		}
		private void mousePressed(InputConfig inputConfig, int button) {
			for(int i = 0; i < inputStates.length; i++) {
				final Input input = getInput(inputConfig, i);
				if(button == input.button) {
					inputStates[i] = 1;
				}
			}
		}
		private void mouseReleased(InputConfig inputConfig, int button) {
			for(int i = 0; i < inputStates.length; i++) {
				final Input input = getInput(inputConfig, i);
				if(button == input.button) {
					inputStates[i] = 0;
				}
			}
		}
		public void wheel(InputConfig inputConfig, int wheelCount) {
			for(int i = 0; i < inputStates.length; i++) {
				final Input input = getInput(inputConfig, i);
				
				int w = wheelCount * input.wheel;
				if(w > 0) {
					inputStates[i] = w;
					if(i < 12) {
						final int j = ((i&0x1) != 0) ? i&~0x01 : i+1;
						inputStates[j] = 0;
					}
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
		inputState.mousePressed(inputConfig, e.getButton());
		
		ptolemy.cameraController.inAutoPilot = 0;
		if (mouseMoveFlag == false) {  // start drag
			mouseMoveFlag = true;
			mousePoint = e.getPoint();
		}
	}
	public void mouseReleased(MouseEvent e)
	{
		inputState.mouseReleased(inputConfig, e.getButton());
		
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

			if(cameraController.fmode == 0) {
				cameraController.mouse_lr_rot_velocity = (double)(oldMousePoint.x - mousePoint.x) / 150;
				cameraController.mouse_ud_rot_velocity = (double)(mousePoint.y - oldMousePoint.y) / 150;
			}
			else {
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
