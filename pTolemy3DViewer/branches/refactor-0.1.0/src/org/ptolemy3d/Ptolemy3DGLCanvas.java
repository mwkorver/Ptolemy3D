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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;

import org.ptolemy3d.debug.IO;
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
 * Ptolemy3DGLCanvas maintains a Camera and a CameraMovement object to control
 * the view.
 * </p>
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class Ptolemy3DGLCanvas extends GLCanvas implements GLEventListener {

    // Camera that renders ptolemy instance.
    private Camera camera = null;
    private CameraMovement cameraMovement = null;
    private InputHandler input = null;
    private DrawContext drawContext = new DrawContext();
    // Rendering loop
    private Animator animator = null;
    private GL gl = null;
    private GLAutoDrawable glAutoDrawable = null;

    // Width on the screen
    private int screenWidth = 0;
    // Height on the screen
    private int screenHeight = 0;
    // Aspect ratio of the frame buffer.
    private float screenAspectRatio = 1;
    // Width of the frame buffer, can be different to screen in case of an
    // offscreen rendering.
    private int drawWidth = 0;
    // Height of the frame buffer, can be different to screen in case of an
    // offscreen rendering.
    private int drawHeight = 0;
    // Aspect ration of the frame buffer.
    private float drawAspectRatio = 1;

    // Flag to know when the init has been done.
    private boolean isInit = false;
    // Flag to end rendering loop.
    private boolean renderingLoopOn = true;

    /**
     * Creates a new instance.
     *
     * @param ptolemy
     */
    public Ptolemy3DGLCanvas() {
        this.camera = new Camera(this);
        this.input = new InputHandler(this);
        this.cameraMovement = new CameraMovement(this, this.input);

        // Register itself as listener.
        this.addGLEventListener(this);

        // Create the animator and starts the rendering process.
        animator = new Animator(this);
        startRenderingLoop();
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

    /** Start rendering loop. */
    public void startRenderingLoop() {
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
    public void stopRenderingLoop() {
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

    /**
     * Initialize the common attributes stored in the DrawContext instance
     * and useful in the rendering process.
     */
    private void initializeDrawContext(GLAutoDrawable glDrawable) {
        drawContext.setGlDrawable(glDrawable);
        drawContext.setGl(glDrawable.getGL());
        drawContext.setGlcontext(GLContext.getCurrent());
        drawContext.setCanvas(this);
    }

    /* GLEventListener */
    public void init(GLAutoDrawable glDrawable) {

        initializeDrawContext(glDrawable);

        glAutoDrawable = glDrawable;
        gl = glDrawable.getGL();

        // Enable V-Sync
        gl.setSwapInterval(1);

        if (!isInit) {
            // Init flight movement
            cameraMovement.init();

            // Init Scene landscane, sky, hud, plugins ...
            Ptolemy3D.getScene().initGL(drawContext);

            // Add listeners
            addListeners();

            isInit = true;

            Ptolemy3D.startTileLoaderThread();
        // ptolemy.callJavascript("sc_init", null, null, null);
        }
    }

    public void display(GLAutoDrawable glDrawable) {

        initializeDrawContext(glDrawable);

        glAutoDrawable = glDrawable;
        gl = glDrawable.getGL();

        if (renderingLoopOn) {
            if (isInit) {
                Ptolemy3D.getScene().draw(drawContext);

            // if (isOffscreenRender) { //FIXME Not implement with JOGL
            // repaint();
            // }
            }
        }
        else {
            destroyGL();
        }
    }

    public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {

        initializeDrawContext(glDrawable);

        glAutoDrawable = glDrawable;
        gl = glDrawable.getGL();

        gl.glViewport(x, y, width, height);

        if (height == 0) {
            height = 1;
        }

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
        setScreenAspectRatio((float) width / height);

        this.gl = null; /* Don't let someone else use it from now */
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    // TODO - Must be called to free Canvas resources.
    public void destroyGL() {
        // Stop rendering loop
//        stopRenderingLoop();
        removeListeners();

        // Destroy OpenGL Datas
        runInContext(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Ptolemy3D.getScene().destroyGL(drawContext);
            }
        });

        renderingLoopOn = false;
        isInit = false;
    }

    private void runInContext(ActionListener action) {
        final GLContext glContext = glAutoDrawable.getContext();
        int result = glContext.makeCurrent();
        if (result != GLContext.CONTEXT_NOT_CURRENT) {
            try {
                action.actionPerformed(null);
            }
            catch (Exception e) {
            }
            catch (Error e) {
            }

            glContext.release();

            if (result == GLContext.CONTEXT_CURRENT_NEW) {
                glContext.destroy();
            }
        }
    }


    /* Listeners */
    protected void removeListeners() {
        if (glAutoDrawable == null) {
            IO.print("Ptolemy3DGLCanvas not yet initialized.");
            return;
        }

        InputHandler inputs = cameraMovement.inputs;
        if (inputs.keyboardEnabled) {
            glAutoDrawable.removeKeyListener(inputs);
        }
        glAutoDrawable.removeMouseListener(inputs);
        glAutoDrawable.removeMouseMotionListener(inputs);
    }

    protected void addListeners() {
        if (glAutoDrawable == null) {
            IO.print("Ptolemy3DGLCanvas not yet initialized.");
            return;
        }

        InputHandler inputs = cameraMovement.inputs;
        if (inputs.keyboardEnabled) {
            glAutoDrawable.addKeyListener(inputs);
        }
        glAutoDrawable.addMouseListener(inputs);
        glAutoDrawable.addMouseMotionListener(inputs);
        glAutoDrawable.addMouseWheelListener(inputs);
    }

    /**
     * @param screenWidth
     *            the screenWidth to set
     */
    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    /**
     * @return the screenWidth
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    /**
     * @param screenHeight
     *            the screenHeight to set
     */
    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    /**
     * @return the screenHeight
     */
    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * @param screenAspectRatio
     *            the screenAspectRatio to set
     */
    public void setScreenAspectRatio(float screenAspectRatio) {
        this.screenAspectRatio = screenAspectRatio;
    }

    /**
     * @return the screenAspectRatio
     */
    public float getScreenAspectRatio() {
        return screenAspectRatio;
    }

    /**
     * @param drawWidth
     *            the drawWidth to set
     */
    public void setDrawWidth(int drawWidth) {
        this.drawWidth = drawWidth;
    }

    /**
     * @return the drawWidth
     */
    public int getDrawWidth() {
        return drawWidth;
    }

    /**
     * @param drawHeight
     *            the drawHeight to set
     */
    public void setDrawHeight(int drawHeight) {
        this.drawHeight = drawHeight;
    }

    /**
     * @return the drawHeight
     */
    public int getDrawHeight() {
        return drawHeight;
    }

    /**
     * @param drawAspectRatio
     *            the drawAspectRatio to set
     */
    public void setDrawAspectRatio(float drawAspectRatio) {
        this.drawAspectRatio = drawAspectRatio;
    }

    /**
     * @return the drawAspectRatio
     */
    public float getDrawAspectRatio() {
        return drawAspectRatio;
    }
}
