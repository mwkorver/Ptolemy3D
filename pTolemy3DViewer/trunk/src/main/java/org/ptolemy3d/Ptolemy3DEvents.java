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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.scene.Scene;
import org.ptolemy3d.view.InputListener;

import com.sun.opengl.util.Animator;

/**
 * Events related to the initialization, rendering and destruction of 3D/OpenGL
 * related code.
 */
public class Ptolemy3DEvents implements GLEventListener, Transferable {
	/** Ptolemy3D Instance */
	private final Ptolemy3D ptolemy;

	/** Rendering loop */
	private Animator animator;
	/** OpenGL Context. Null when OpenGL Context is not current. */
	protected GL gl = null;
	private GLAutoDrawable glAutoDrawable = null;

	protected boolean isOffscreenRender = false; // Not implemented yet with
	// JOGL ...

	/** Width on the screen */
	public int screenWidth = 0;
	/** Height on the screen */
	public int screenHeight = 0;
	/** Aspect ration of the frame buffer, . */
	public float screenAspectRatio = 1;
	/**
	 * Width of the frame buffer, can be different to screen in case of an
	 * offscreen rendering.
	 */
	public int drawWidth = 0;
	/**
	 * Height of the frame buffer, can be different to screen in case of an
	 * offscreen rendering.
	 */
	public int drawHeight = 0;
	/** Aspect ration of the frame buffer, . */
	public float drawAspectRatio = 1;

	/** Flag to know when the init has been done. */
	private boolean isInit = false;
	/** Flag to end rendering loop. */
	private boolean renderingLoopOn = true;

	public Ptolemy3DEvents(Ptolemy3D ptolemy) {
		this.ptolemy = ptolemy;
	}

	/** Start rendering loop. */
	private void startRenderingLoop() {
		if (animator == null) {
			IO.print("Animator not yet initialized.");
		}
		if (animator != null && !animator.isAnimating()) {
			renderingLoopOn = true;
			animator.start();
		}
	}

	/**
	 * Stop rendering loop.<BR>
	 * To stop and clean up OpenGL datas, when exiting, use <code>ON</code> flag
	 * and wait while rendering thread is alive.
	 * 
	 * @see #renderingLoopOn
	 * @see #isRenderingThreadAlive()
	 */
	private void stopRenderingLoop() {
		if (animator != null && animator.isAnimating()) {
			renderingLoopOn = false;
			animator.stop();
		}
	}

	/** */
	public boolean isRenderingThreadAlive() {
		if (animator != null && animator.isAnimating()) {
			return true;
		}
		return false;
	}

	/* GLEventListener */

	public void init(GLAutoDrawable glDrawable) {
		glAutoDrawable = glDrawable;
		gl = glDrawable.getGL();

		// Enable V-Sync
		gl.setSwapInterval(1);

		animator = new Animator(glDrawable);

		if (!isInit) {
			// Init flight movement
			ptolemy.cameraController.init();
			// ptolemy.flight.realisticFlightOn("0"); //CHANGE 2 feb 06

			// Init Scene landscane, sky, hud, plugins ...
			Scene scene = ptolemy.scene;
			scene.init(ptolemy);
			scene.initGL(gl);

			// Add listeners
			addListeners();

			isInit = true;

			ptolemy.startTileLoaderThread();
			ptolemy.callJavascript("sc_init", null, null, null);
		}

		startRenderingLoop();

		this.gl = null; /* Don't let someone else use it from now */
	}

	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width,
			int height) {
		glAutoDrawable = glDrawable;
		gl = glDrawable.getGL();

		gl.glViewport(x, y, width, height);

		if (height == 0)
			height = 1;

		// if (isOffscreenRender && (width > 512) || (height > 512)) //FIXME Not
		// implement with JOGL
		// {
		// // put a 512 pix cap on height and width
		// if (Math.max(height, width) == height){
		// double rat = 512.0/height;
		// GL_HEIGHT = 512;
		// GL_WIDTH = (int)(width * rat);
		// } else{
		// double rat = 512.0 / width;
		// GL_WIDTH = 512;
		// GL_HEIGHT = (int)(height * rat);
		// }
		// }
		// else
		{
			drawWidth = width;
			drawHeight = height;
			drawAspectRatio = (float) width / height;
		}

		screenWidth = width;
		screenHeight = height;
		screenAspectRatio = (float) width / height;

		this.gl = null; /* Don't let someone else use it from now */
	}

	public void display(GLAutoDrawable glDrawable) {
		glAutoDrawable = glDrawable;
		gl = glDrawable.getGL();

		if (renderingLoopOn) {
			if (isInit) {
				Scene scene = ptolemy.scene;
				scene.draw(gl);

				// if (isOffscreenRender) { //FIXME Not implement with JOGL
				// repaint();
				// }
			}
		} else {
			destroyGL();
		}

		// gl = null; /* Don't let someone else use it from now*/
	}

	protected final void destroyGL() {
		// Stop rendering loop
		stopRenderingLoop();
		removeListeners();

		// Destroy OpenGL Datas
		try {
			ptolemy.scene.destroyGL(gl);
		} catch (Exception e) {
			IO.printStackRenderer(e);
		}

		renderingLoopOn = false;
		isInit = false;
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
			boolean deviceChanged) {
	}

	/* Listeners */

	protected void removeListeners() {
		if (glAutoDrawable == null) {
			IO.print("Ptolemy3DEvents not yet initialized.");
			return;
		}

		InputListener inputs = ptolemy.cameraController.inputs;
		if (inputs.keyboardEnabled) {
			glAutoDrawable.removeKeyListener(inputs);
		}
		glAutoDrawable.removeMouseListener(inputs);
		glAutoDrawable.removeMouseMotionListener(inputs);
		// if (componentEnabled) {
		// removeComponentListener(inputs);
		// }
	}

	protected void addListeners() {
		if (glAutoDrawable == null) {
			IO.print("Ptolemy3DEvents not yet initialized.");
			return;
		}

		InputListener inputs = ptolemy.cameraController.inputs;
		if (inputs.keyboardEnabled) {
			glAutoDrawable.addKeyListener(inputs);
		}
		glAutoDrawable.addMouseListener(inputs);
		glAutoDrawable.addMouseMotionListener(inputs);
		glAutoDrawable.addMouseWheelListener(inputs);
		// if (componentEnabled) {
		// addComponentListener(inputs);
		// }
	}

	/* Transferable */

	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.imageFlavor };
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return DataFlavor.imageFlavor.equals(flavor);
	}

	public Object getTransferData(DataFlavor flavor)
			throws UnsupportedFlavorException, IOException {
		if (!DataFlavor.imageFlavor.equals(flavor)) {
			throw new UnsupportedFlavorException(flavor);
		}
		// Returns image
		return ptolemy.scene.clipboardImage;
	}
}
