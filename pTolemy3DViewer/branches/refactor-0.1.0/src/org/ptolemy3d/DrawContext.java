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

    private GLAutoDrawable glDrawable = null;
    private GL gl = null;
    private GLContext glcontext = null;
    private Ptolemy3DGLCanvas canvas = null;
    private Ptolemy3D ptolemy = null;

    /**
     * @return the glDrawable
     */
    public GLAutoDrawable getGlDrawable() {
        return glDrawable;
    }

    /**
     * @param glDrawable the glDrawable to set
     */
    public void setGlDrawable(GLAutoDrawable glDrawable) {
        this.glDrawable = glDrawable;
    }

    /**
     * @return the gl
     */
    public GL getGl() {
        return gl;
    }

    /**
     * @param gl the gl to set
     */
    public void setGl(GL gl) {
        this.gl = gl;
    }

    /**
     * @return the glcontext
     */
    public GLContext getGlcontext() {
        return glcontext;
    }

    /**
     * @param glcontext the glcontext to set
     */
    public void setGlcontext(GLContext glcontext) {
        this.glcontext = glcontext;
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

    /**
     * @return the ptolemy
     */
    public Ptolemy3D getPtolemy() {
        return ptolemy;
    }

    /**
     * @param ptolemy the ptolemy to set
     */
    public void setPtolemy(Ptolemy3D ptolemy) {
        this.ptolemy = ptolemy;
    }
}
