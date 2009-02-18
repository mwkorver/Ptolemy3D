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
	protected final static int ASC_A       = (1 << 0);
	protected final static int ASC_Z       = (1 << 1);
	protected final static int LEFT_ARROW  = (1 << 2);
	protected final static int RIGHT_ARROW = (1 << 3);
	protected final static int UP_ARROW    = (1 << 6);
	protected final static int DOWN_ARROW  = (1 << 7);
	protected final static int WHEEL_TOWARD= (1 << 8);
	protected final static int WHEEL_AWAY  = (1 << 9);
	protected final static int ASC_S       = (1 << 16);		//Idem ASC_A
	protected final static int ASC_X       = (1 << 18);		//Idem ASC_Z
	protected final static int ASC_D       = (1 << 15);
	protected final static int ASC_F       = (1 << 17);
	protected final static int ASC_R       = (1 << 11);
	protected final static int ASC_C       = (1 << 12);
	//protected final static int ASC_H     = (1 << 19);
	//protected final static int ASC_Q     = (1 << 13);

	public final static int OUTPUT_COORDINATES = 0;
	public final static int CENTER_IN = 1;
	public final static int ZOOM_IN = 2;

	/** Ptolemy3D Instance */
	private final Ptolemy3D ptolemy;
	/** Keyboard enabled ? */
	public boolean keyboardEnabled = true;
	/** Key Pressed */
	public int key_state = 0;
	/** Current mouse position */
	private Point mousePoint = null;
	/** Mouse wheel count */
	public int wheelCount = 0;
	/** Mouse moving flag, used to know when mousePressed event have been triggered. */
	private boolean mouseMoveFlag = false;
	/** */
	public int currentFunction = OUTPUT_COORDINATES;

	protected InputListener(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
	}

	public final void asciiKeyPress(int dec)
	{
		ptolemy.cameraController.inAutoPilot = 0;
		if (ptolemy.tileLoader.isSleeping) {
			ptolemy.tileLoaderThread.interrupt();
		}
		switch (dec)
		{
			case 65:
				key_state |= ASC_A;
				break;
			case 90:
				key_state |= ASC_Z;
				break;
			case 37:
				key_state |= LEFT_ARROW;
				break;
			case 39:
				key_state |= RIGHT_ARROW;
				break;
			case 38:
				key_state |= UP_ARROW;
				break;
			case 40:
				key_state |= DOWN_ARROW;
				break;
			case 68:
				key_state |= ASC_D;
				break;
			case 70:
				key_state |= ASC_F;
				break;
			case 83:
				key_state |= ASC_S;
				break;
			case 88:
				key_state |= ASC_X;
				break;
			case 82:
				key_state |= ASC_R;
				break;
			case 67:
				key_state |= ASC_C;
				break;
		}
	}

	public final void asciiKeyRelease(int dec)
	{
		switch (dec)
		{
			case 65:
				key_state &= ~ASC_A;
				break;
			case 90:
				key_state &= ~ASC_Z;
				break;
			case 37:
				key_state &= ~LEFT_ARROW;
				break;
			case 39:
				key_state &= ~RIGHT_ARROW;
				break;
			case 38:
				key_state &= ~UP_ARROW;
				break;
			case 40:
				key_state &= ~DOWN_ARROW;
				break;
			case 68:
				key_state &= ~ASC_D;
				break;
			case 70:
				key_state &= ~ASC_F;
				break;
			case 83:
				key_state &= ~ASC_S;
				break;
			case 88:
				key_state &= ~ASC_X;
				break;
			case 82:
				key_state &= ~ASC_R;
				break;
			case 67:
				key_state &= ~ASC_C;
				break;
		}
		ptolemy.cameraController.updatePosition(true);
	}

	/* ************************* KeyListener ************************* */

	public void keyPressed(KeyEvent e)
	{
		ptolemy.cameraController.inAutoPilot = 0;
		if (ptolemy.tileLoader.isSleeping) {
			ptolemy.tileLoaderThread.interrupt();
		}

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_A:
				key_state |= ASC_A;
				break;
			case KeyEvent.VK_Z:
				key_state |= ASC_Z;
				break;
			case KeyEvent.VK_LEFT:
				key_state |= LEFT_ARROW;
				break;
			case KeyEvent.VK_RIGHT:
				key_state |= RIGHT_ARROW;
				break;
			case KeyEvent.VK_UP:
				key_state |= UP_ARROW;
				break;
			case KeyEvent.VK_DOWN:
				key_state |= DOWN_ARROW;
				break;
			case KeyEvent.VK_D:
				key_state |= ASC_D;
				break;
			case KeyEvent.VK_F:
				key_state |= ASC_F;
				break;
			case KeyEvent.VK_S:
				key_state |= ASC_S;
				break;
			case KeyEvent.VK_X:
				key_state |= ASC_X;
				break;
			case KeyEvent.VK_R:
				key_state |= ASC_R;
				break;
			case KeyEvent.VK_C:
				key_state |= ASC_C;
				break;
//			case KeyEvent.VK_H:
//				ptolemy.scene.hud.drawHud = ptolemy.scene.hud.drawHud ? false : true;
//				break;
			//case KeyEvent.VK_Q:
			//	key_state |=  ASC_Q;
			//	break;
		}
	}

	public void keyReleased(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_A:
				key_state &= ~ASC_A;
				break; // accellerate
			case KeyEvent.VK_Z:
				key_state &= ~ASC_Z;
				break; // reverse
			case KeyEvent.VK_LEFT:
				key_state &= ~LEFT_ARROW;
				break;
			case KeyEvent.VK_RIGHT:
				key_state &= ~RIGHT_ARROW;
				break;
			case KeyEvent.VK_UP:
				key_state &= ~UP_ARROW;
				break;
			case KeyEvent.VK_DOWN:
				key_state &= ~DOWN_ARROW;
				break;
			case KeyEvent.VK_D:
				key_state &= ~ASC_D;
				break;
			case KeyEvent.VK_F:
				key_state &= ~ASC_F;
				break;
			case KeyEvent.VK_S:
				key_state &= ~ASC_S;
				break;
			case KeyEvent.VK_X:
				key_state &= ~ASC_X;
				break;
			case KeyEvent.VK_R:
				key_state &= ~ASC_R;
				break;
			case KeyEvent.VK_C:
				key_state &= ~ASC_C;
				break;
				//case KeyEvent.VK_Q:    key_state &= ~ASC_Q;       break;
			default:
				if(DEBUG) {
					IO.keyReleasedDebug(ptolemy, e.getKeyCode());
				}
				break;
		}

		ptolemy.cameraController.updatePosition(true);
	}

	public void keyTyped(KeyEvent e)
	{
	}

	/* ************************* MouseListener ************************* */

	public void mouseClicked(MouseEvent e)
	{
		switch (currentFunction)
		{
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
		if (mouseMoveFlag == false)
		{  // start drag
			mouseMoveFlag = true;
			mousePoint = e.getPoint();
		}
	}

	public void mouseReleased(MouseEvent e)
	{
		mouseMoveFlag = false;
		ptolemy.cameraController.updatePosition(true);
	}

	public void mouseDragged(MouseEvent e)
	{
		if (mouseMoveFlag == true)
		{
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
				cameraController.lr_rot_velocity += (mousePoint.x - oldMousePoint.x) * -(rot_accel);
				cameraController.ud_rot_velocity += (mousePoint.y - oldMousePoint.y) *  (rot_accel);
			}
		}
	}

	public void mouseMoved(MouseEvent e)
	{

	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		key_state &= ~WHEEL_AWAY;
		key_state &= ~WHEEL_TOWARD;

		wheelCount = e.getWheelRotation() * 10;
		if(wheelCount > 0) {
			key_state |= WHEEL_AWAY;	//Zoom-In
		}
		else if(wheelCount < 0) {
			wheelCount = -wheelCount;
			key_state |= WHEEL_TOWARD;	//Zoom-out
		}
	}
}
