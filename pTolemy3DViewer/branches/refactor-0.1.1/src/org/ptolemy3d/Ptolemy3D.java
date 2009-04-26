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
import org.ptolemy3d.manager.Jp2TileLoader;
import org.ptolemy3d.manager.MapDataManager;
import org.ptolemy3d.manager.TextureManager;
import org.ptolemy3d.scene.Scene;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.CameraMovement;

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
 *
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome Jouvie
 */
public final class Ptolemy3D {
    // TODO - canvas variable is only here temporaly to solve a reference problem in Jp2TileLoader. We must remove from here.
    private static Ptolemy3DGLCanvas canvas = null;
    private static Configuration configuration = null;
    private static Scene scene = new Scene();
    private static TextureManager textureManager = null;
    private static MapDataManager mapDataManager = null;
    private static Jp2TileLoader tileLoader = null;
    private static Thread tileLoaderThread = null;

    private Ptolemy3D() {
        System.out.println("Ptolemy created");
    }

    /**
     * Initialized pTolemy3D system with the specified settings.
     *
     * @param config
     */
    public static void initialize(Configuration config) {
    	configuration = config;
    	textureManager = new TextureManager();
    	mapDataManager = new MapDataManager();
    }

    /**
     * Registers the canvas to be used by ptolemy.
     *
     * TODO - This method must be temporal. This not allows to have various canvas
     * render the same scene.
     *
     * @param cnv
     */
    public static void registerCanvas(Ptolemy3DGLCanvas cnv) {
    	canvas = cnv;

        // If there is a configuration the apply them to the cameraMovement.
        if (configuration != null) {
        	canvas.getCameraMovement().setOrientation(
        			configuration.initialCameraPosition,
        			configuration.initialCameraDirection,
        			configuration.initialCameraPitch);
        	canvas.getCameraMovement().setFollowDem(configuration.follorDEM);
        }
    }
    
    /**
     * Start required threads.
     */
    public static void start() {
    	startTileLoaderThread();
    	canvas.startRenderingLoop();
    }

    /**
     * Stop the pTolemy system: request data thread and other resources.
     */
    public static void shutDown() {
        // Stop the tile loading first
    	stopTileLoaderThread();

//		// Stop and shutDown all the 3D
//		if (canvas != null) {
//			canvas.destroyGL();
//			canvas = null;
//		}
    }
    
    /**
     * @return the canvas
     */
    public static Ptolemy3DGLCanvas getCanvas() {
        return canvas;
    }

    /**
     * Returns the scene.
     * @return
     */
    public static Scene getScene() {
        return scene;
    }

    /**
     * @return the configuration
     */
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * @return the textureManager
     */
    public static TextureManager getTextureManager() {
        return textureManager;
    }

    /**
     * @param aTextureManager the textureManager to set
     */
    public static void setTextureManager(TextureManager aTextureManager) {
        textureManager = aTextureManager;
    }
    
    /**
     * @return the textureManager
     */
    public static MapDataManager getMapDataManager() {
        return mapDataManager;
    }

    /**
     * @param mapDataManager the textureManager to set
     */
    public static void setMapDataManager(MapDataManager aMapDataManager) {
        mapDataManager = aMapDataManager;
    }

    /**
     * @return the tileLoader
     */
    public static Jp2TileLoader getTileLoader() {
        return tileLoader;
    }

    /**
     * @param aTileLoader the tileLoader to set
     */
    public static void setTileLoader(Jp2TileLoader aTileLoader) {
        tileLoader = aTileLoader;
    }

    /**
     * @return the tileLoaderThread
     */
    public static Thread getTileLoaderThread() {
        return tileLoaderThread;
    }
    
    /**
     * Start tile download thread.
     */
    private static void startTileLoaderThread() {
        if (tileLoader == null) {
            tileLoader = new Jp2TileLoader();
        }
        if (tileLoaderThread == null) {
            tileLoaderThread = new Thread(tileLoader, "TileLoaderThread");
        }
        tileLoaderThread.start();
    }

    /** 
     * Stop tile download thread.
     */
    private static void stopTileLoaderThread() {
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
    
//     /**
//	* Call a javascript function
//	* @param javascripFunction
//	*/
//	// Plugins args
//	private String[] plugargs = new String[3];
//	public void callJavascript(String javascripFunction, String param0, String param1, String param2) {
//		plugargs[0] = param0;
//		plugargs[1] = param1;
//		plugargs[2] = param2;
//
//		if(javascript != null) {
//			JSObject jsObject = javascript.getJSObject();
//			if(jsObject != null) {
//				try {
//					jsObject.call(javascripFunction, plugargs);
//				}
//				catch(Exception ne) {
//					IO.printStackRenderer(ne);
//				}
//			}
//		}
//	}
//
//	/**
//	* Send an error message to javascript.
//	*/
//	public final void sendErrorMessage(String msg) {
//		callJavascript("alert", msg, null, null);
//	}
}
