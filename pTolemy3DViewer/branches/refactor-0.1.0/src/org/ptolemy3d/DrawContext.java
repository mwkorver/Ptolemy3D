package org.ptolemy3d;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;

/**
 * DrawContext stores attributes commonly needed in the rendering process.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class DrawContext {

    private GLAutoDrawable gLDrawable = null;
    private GL gL = null;
    private GLContext gLContext = null;
    private Ptolemy3DGLCanvas canvas = null;

    /**
     * @return the glDrawable
     */
    public GLAutoDrawable getGLDrawable() {
        return gLDrawable;
    }

    /**
     * @param glDrawable the glDrawable to set
     */
    public void setGLDrawable(GLAutoDrawable glDrawable) {
        this.gLDrawable = glDrawable;
    }

    /**
     * @return the gl
     */
    public GL getGL() {
        return gL;
    }

    /**
     * @param gl the gl to set
     */
    public void setGL(GL gl) {
        this.gL = gl;
    }

    /**
     * @return the glcontext
     */
    public GLContext getGLContext() {
        return gLContext;
    }

    /**
     * @param glcontext the glcontext to set
     */
    public void setGLContext(GLContext glcontext) {
        this.gLContext = glcontext;
    }

    /**
     * @return the canvas
     */
    public Ptolemy3DGLCanvas getCanvas() {
        return canvas;
    }

    /**
     * @param canvas the canvas to set
     */
    public void setCanvas(Ptolemy3DGLCanvas canvas) {
        this.canvas = canvas;
    }
}
