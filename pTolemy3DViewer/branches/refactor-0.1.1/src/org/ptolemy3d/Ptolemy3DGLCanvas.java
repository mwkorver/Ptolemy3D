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

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;

import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.CameraMovement;
import org.ptolemy3d.view.InputHandler;

import com.sun.opengl.util.Animator;

/**
 * <p>
 * Ptolemy3DGLCanvas is the heavyweigh component responsible to render a
 * Ptolemy3D instance.
 * </p>
 * <p>
 * Ptolemy3DGLCanvas maintains a Camera, CameraMovement and InputHandler object
 * to control the view.
 * </p>
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class Ptolemy3DGLCanvas extends GLCanvas implements GLEventListener {
	private static final long serialVersionUID = 1L;

	// Camera that renders ptolemy instance.
	private final Camera camera;
	private final CameraMovement cameraMovement;
	private final InputHandler input;
	private final DrawContext drawContext;
	// Rendering loop
	private final Animator animator;

	/**
	 * Creates a new instance.
	 */
	public Ptolemy3DGLCanvas() {
		this(null);
	}
	public Ptolemy3DGLCanvas(GLCapabilities glCapabilities) {
		super(glCapabilities);

		this.drawContext = new DrawContext();
		this.camera = new Camera(this);
		this.input = new InputHandler(this);
		this.cameraMovement = new CameraMovement(this, this.input);

		// Register itself as listener.
		this.addGLEventListener(this);

		// Add listeners
		this.addKeyListener(this.input);
		this.addMouseListener(this.input);
		this.addMouseMotionListener(this.input);
		this.addMouseWheelListener(this.input);

		// Create the animator and starts the rendering process.
		this.animator = new Animator(this);
	}

	/**
	 * Returns the Camera instance associated with this canvas object.
	 * 
	 * @return
	 */
	public Camera getCamera() {
		return camera;
	}

	/**
	 * Returns the Camera instance associated with this canvas object.
	 * 
	 * @return
	 */
	public CameraMovement getCameraMovement() {
		return cameraMovement;
	}

	public InputHandler getInput() {
		return input;
	}

	/**
	 * Start rendering loop.
	 */
	public void startRenderingLoop() {
		if (animator != null && !animator.isAnimating()) {
			animator.start();
		}
	}

	/**
	 * Stop rendering loop.
	 */
	public void stopRenderingLoop() {
		if (animator != null && animator.isAnimating()) {
			animator.stop();
		}
	}

	/**
	 * Initialize the common attributes stored in the DrawContext instance and
	 * useful in the rendering process.
	 */
	private void initializeDrawContext(GLAutoDrawable glDrawable) {
		drawContext.setGLDrawable(glDrawable);
		drawContext.setGL(glDrawable.getGL());
		drawContext.setGLContext(GLContext.getCurrent());
		drawContext.setCanvas(this);
	}

	/* GLEventListener */
	public void init(GLAutoDrawable glDrawable) {
		initializeDrawContext(glDrawable);

		// TODO - Necessary
		// Enable V-Sync
		// glDrawable.getGL().setSwapInterval(1);

		// Init flight movement
		cameraMovement.init();

		// Init Scene landscane, sky, hud, plugins ...
		Ptolemy3D.getScene().initGL(drawContext);
	}

	public void display(GLAutoDrawable glDrawable) {
		initializeDrawContext(glDrawable);

		Ptolemy3D.getScene().draw(drawContext);
	}

	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width,
			int height) {
	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
			boolean deviceChanged) {
	}

	/**
	 * @return the drawAspectRatio
	 */
	public float getAspectRatio() {
		int width = getWidth();
		int height = getHeight();
		if (height == 0) {
			height = 1;
		}

		return (float) width / height;
	}

	// TODO - Decide if necessary.
	// protected final void destroyGL() {
	// // Stop rendering loop
	// stopRenderingLoop();
	//
	// // Destroy OpenGL Datas
	// runInContext(new ActionListener() {
	// public void actionPerformed(ActionEvent e) {
	// Ptolemy3D.getScene().destroyGL(drawContext);
	// Ptolemy3D.getTextureManager().destroyGL(drawContext);
	// }
	// });
	// }
	// private void runInContext(ActionListener action) {
	// final GLContext glContext = drawContext.getGLContext();
	// if (glContext != null) {
	// int result = glContext.makeCurrent();
	// if(result != GLContext.CONTEXT_NOT_CURRENT) {
	// try {
	// action.actionPerformed(null);
	// } catch(Exception e) { } catch(Error e) { }
	//
	// glContext.release();
	// if(result == GLContext.CONTEXT_CURRENT_NEW) {
	// glContext.destroy();
	// }
	// }
	// }
	// }
}
