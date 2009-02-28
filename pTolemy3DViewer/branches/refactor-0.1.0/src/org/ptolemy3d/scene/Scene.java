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
package org.ptolemy3d.scene;

import static org.ptolemy3d.debug.Config.DEBUG;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.debug.ProfilerInterface;
import org.ptolemy3d.debug.ProfilerInterface.ProfilerEventInterface;
import org.ptolemy3d.view.Camera;

/**
 * <H1>Overview</H1> <BR>
 * The scene is a manager to initialize, render and destruct of:<BR>
 * <ul>
 * <li><a href="Landscape.html">Landcscape</a>: manage landscape visibility,
 * rendering ...<BR>
 * For more information, see the related documentation.</li>
 * <li><a href="Sky.html">Sky</a>: manage sky, atmosphere and stars.</li>
 * <li><a href="Plugins.html">Plugins</a>: manage ptolemy plugins<BR>
 * For more information, see the related documentation.</li>
 * </ul>
 * <BR>
 * Among other thing, it contains the 3D rendering loop.<BR>
 * <BR>
 */
public class Scene implements Transferable {

    /** Landscape */
    public final Landscape landscape;
    /** Sky */
    public final Sky sky;
    /** Plugins */
    public final Plugins plugins;
    /** Light */
    public final Light light;
    /**
     * Do a screenshot of the next frame in <code>clipboardImage</code>. @see
     * #clipboardImage
     */
    public boolean screenshot = false;
    /** Screenshot image */
    public Image clipboardImage;

    /**
     * Creates a new Scene instance.
     */
    public Scene() {
        this.landscape = new Landscape();
        this.sky = new Sky();
        this.plugins = new Plugins();
        this.light = new Light();
    }

    /** Initialize OpenGL data of the scene. */
    public void initGL(DrawContext drawContext) {
        landscape.initGL(drawContext);
        sky.initGL(drawContext);
        plugins.initGL(drawContext);
        light.initGL(drawContext);
    }

    /** Destroy OpenGL data of the scene. */
    public void destroyGL(DrawContext drawContext) {
        landscape.destroyGL(drawContext);
        sky.destroyGL(drawContext);
        plugins.destroyGL(drawContext);
        light.destroyGL(drawContext);

        // Destroy Ptolemy3D OpenGL datas
        if (Ptolemy3D.getFontTextRenderer() != null) {
            Ptolemy3D.getFontTextRenderer().destroyGL(drawContext.getGl());
            Ptolemy3D.setFontTextRenderer(null);
        }
        if (Ptolemy3D.getFontNumericRenderer() != null) {
            Ptolemy3D.getFontNumericRenderer().destroyGL(drawContext.getGl());
            Ptolemy3D.setFontNumericRenderer(null);
        }

        Ptolemy3D.getTextureManager().destroyGL(drawContext.getGl());
    }

    /** Draw the scene with OpenGL. */
    public void draw(DrawContext drawContext) {

        GL gl = drawContext.getGl();

        if (Ptolemy3D.getTileLoader() == null) {
            // Just clear out the screen
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            return;
        }

        Camera camera = drawContext.getCanvas().getCamera();

        if (DEBUG) {
            ProfilerInterface.start(ProfilerEventInterface.Frame);
        }
        if (DEBUG) {
            ProfilerInterface.start(ProfilerEventInterface.Prepare);
        }

        /* Camera */
        camera.update(gl);

        /* Prepare frame */
        landscape.prepareFrame(drawContext);
        plugins.prepareFrame(drawContext);
        if (DEBUG) {
            ProfilerInterface.stop(ProfilerEventInterface.Prepare);
        }

        /* Standard OpenGL Init: Clear frame buffer color and z */
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        /* Lighting */
        light.draw(drawContext); // FIXME Strange here, must be after camera ?

        /* Perspective matrix */
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadMatrixd(camera.perspective.m, 0);
        /* Modelview matrix */
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadMatrixd(camera.modelview.m, 0);

        /* Rendering */
        if (DEBUG) {
            ProfilerInterface.start(ProfilerEventInterface.Sky);
        }
        sky.draw(drawContext);
        if (DEBUG) {
            ProfilerInterface.stop(ProfilerEventInterface.Sky);
        }

        if (DEBUG) {
            ProfilerInterface.start(ProfilerEventInterface.Landscape);
        }
        landscape.draw(drawContext);
        if (DEBUG) {
            ProfilerInterface.stop(ProfilerEventInterface.Landscape);
        }

        if (DEBUG) {
            ProfilerInterface.start(ProfilerEventInterface.Plugins);
        }
        plugins.draw(drawContext);
        if (DEBUG) {
            ProfilerInterface.stop(ProfilerEventInterface.Plugins);
        }

        /* Screenshot */
        if (DEBUG) {
            ProfilerInterface.start(ProfilerEventInterface.Screenshot);
        }
        if (screenshot) {
            getSceneImage(drawContext);
        }
        if (DEBUG) {
            ProfilerInterface.stop(ProfilerEventInterface.Screenshot);
        }

        if (DEBUG) {
            ProfilerInterface.stop(ProfilerEventInterface.Frame);
        }
        if (DEBUG) {
            ProfilerInterface.drawProfiler(drawContext);
        }
    }

    protected void getSceneImage(DrawContext drawContext) {
        GL gl = drawContext.getGl();
        Ptolemy3DGLCanvas canvas = drawContext.getCanvas();

        final int GL_WIDTH = canvas.getDrawWidth();
        final int GL_HEIGHT = canvas.getDrawHeight();

        byte[] pixels = new byte[(GL_WIDTH + 1) * (GL_HEIGHT + 1) * 3];
        gl.glReadPixels(0, 0, GL_WIDTH, GL_HEIGHT, GL.GL_RGB,
                        GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(pixels));

        try {
            clipboardImage = Toolkit.getDefaultToolkit().createImage(
                    new SceneProducer(pixels, GL_WIDTH, GL_HEIGHT));
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(this,
                                                                         null);
        }
        catch (Exception e) {
            // ptolemy
            // .sendErrorMessage("Security Exception : Unable to access clipboard.");
        }
    }

    /* Transferable */
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
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
        return clipboardImage;
    }
}