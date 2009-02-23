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

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.font.FntFontRenderer;
import org.ptolemy3d.scene.Scene;
import org.ptolemy3d.scene.TextureManager;
import org.ptolemy3d.tile.Jp2TileLoader;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.CameraMovement;
import org.w3c.dom.Element;

/**
 * <H1>Overview</H1> <BR>
 * Ptolemy3D is the core class object. It is accessed for initializing the
 * download and rendering engines, and accessing specific datas.<BR>
 * <BR>
 * <BR>
 * <H1>Initialization</H1> <BR>
 * The initialization is explained in more details on the document: <a
 * href="../../Ptolemy3D-Programming-HowToStart.htm"
 * >Ptolemy3D-Programming-HowToStart.htm</a>.<BR>
 * <BR>
 * A simple initialization will be very similar to:
 * 
 * <pre>
 * Ptolemy3D ptolemy = new Ptolemy3D();
 * ptolemy.init(Ptolemy3DSettings.buildXMLDocument(xmlFile)); //null for using applet parameters
 * 
 * //Adding Ptolemy3D inside a GLCanvas
 * GLCanvas canvas = new GLCanvas();
 * canvas.addGLEventListener(ptolemy.events);
 * canvas.setSize(new Dimension(width, height));
 * 
 * //Adding Ptolemy3D inside your application
 * application.add(canvas); //application can be an applet, a frame, a panel or any other container
 * </pre>
 * 
 * And that's it !<BR>
 * <BR>
 * <BR>
 * <H1>How Ptolemy3D works ? How to access data ?</H1> <BR>
 * Ptolemy3D can be accessed to retrieve any data related to:<BR>
 * <ul>
 * <li><a href="Ptolemy3DConfiguration.html">Ptolemy3DConfiguration</a>:
 * Configuration of Ptolemy3D.</li>
 * <li><a href="view/Camera.html">Camera</a>: View positioning.<BR>
 * Look at that documentation for a description of the view coordinates system,
 * and to access the view position values.</li>
 * <li><a href="view/CameraMovement.html">CameraMovement</a>: View movement.<BR>
 * CameraMovement controls the view displacement from mouse/keyboard inputs or
 * automatic movements. Look at that documentation for a more specific
 * description.</li>
 * <li><a href="scene/Scene.html">Scene</a>: 3D scene manager.<BR>
 * This includes the documentation for landscape, plugins, sky ...</li>
 * </ul>
 * <BR>
 * 
 * @see Ptolemy3DConfiguration
 * @see Camera
 * @see CameraMovement
 * @see Scene
 */
public class Ptolemy3D {

    private static Ptolemy3D instance = null;
    // Unit System
    public Unit unit;
    // Texture Manager
    public TextureManager textureManager;
    // Tile Loader: download tile datas (image, geometry)
    public Jp2TileLoader tileLoader;
    // Tile Loader Thread: Thread in which tile loader is running
    public Thread tileLoaderThread;

    // Default Font Text Renderer.<BR> Can be null if not used/initialized by
    // any of the core or plugins components of Ptolemy3D.
    // FIXME protect: don't let the plugins override that.
    public FntFontRenderer fontTextRenderer;

    // Default Font Numeric Renderer.<BR> In most cases, it will be the same as
    // <code>fontTextRenderer</code>.
    // FIXME protect: don't let the plugins override that.
    public FntFontRenderer fontNumericRenderer;

    // Plugins args
    private String[] plugargs = new String[3];

    /** 
     * Creates a new instance.
     */
    private Ptolemy3D() {
        this.textureManager = new TextureManager(this);
    }

    /**
     * Returns the ptolemy instance.
     * @return
     */
    public static Ptolemy3D getInstance() {
        if (instance == null) {
            instance = new Ptolemy3D();
        }
        return instance;
    }

    /** 
     * Start tile download thread.
     */
    protected void startTileLoaderThread() {
        if (tileLoader == null) {
            tileLoader = new Jp2TileLoader(this);
        }
        if (tileLoaderThread == null) {
            tileLoaderThread = new Thread(tileLoader, "TileLoaderThread");
        }
        tileLoaderThread.start();
    }

    /** 
     * Stop tile download thread.
     */
    protected void stopTileLoaderThread() {
        if (tileLoaderThread != null) {
            tileLoader.on = false;
            try {
                tileLoaderThread.interrupt();
            }
            catch (Exception e) {
                IO.printStack(e);
            }
        }
    }

    public void destroy() {
        // Stop the tile loading first
        stopTileLoaderThread();

//		// Stop and destroy all the 3D
//		if (canvas != null) {
//			canvas.destroyGL();
//		}
    }

    // /**
    // * Call a javascript function
    // * @param javascripFunction
    // */
    // public void callJavascript(String javascripFunction, String param0,
    // String param1, String param2)
    // {
    // plugargs[0] = param0;
    // plugargs[1] = param1;
    // plugargs[2] = param2;
    //
    // if (javascript != null) {
    // JSObject jsObject = javascript.getJSObject();
    // if (jsObject != null) {
    // try {
    // jsObject.call(javascripFunction, plugargs);
    // } catch (Exception ne) {
    // IO.printStackRenderer(ne);
    // }
    // }
    // }
    // }
    // /**
    // * Send an error message to javascript.
    // */
    // public final void sendErrorMessage(String msg)
    // {
    // callJavascript("alert", msg, null, null);
    // }
}
