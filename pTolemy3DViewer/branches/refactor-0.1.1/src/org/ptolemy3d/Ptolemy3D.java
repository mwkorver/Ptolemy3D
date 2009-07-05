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

import java.util.logging.Logger;

import netscape.javascript.JSObject;

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.io.FileSystemCache;
import org.ptolemy3d.io.DataFinder;
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
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public final class Ptolemy3D {

	private static final Logger logger = Logger.getLogger(Ptolemy3D.class
			.getName());

	// TODO - canvas variable is only here temporaly to solve a reference
	// problem in Jp2TileLoader. We must remove from here.
	private static Ptolemy3DGLCanvas canvas = null;
	private static Ptolemy3DJavascript javascript = null;
	private static Configuration configuration = null;
	private static Scene scene = new Scene();
	private static TextureManager textureManager = null;
	private static MapDataManager mapDataManager = null;
	private static DataFinder dataFinder = null;
	private static FileSystemCache cache = null;

	// JavaScript function names
	private static String JS_START_FUNCTION = "ptolemyStart";
	private static String JS_STOP_FUNCTION = "ptolemyStop";
	private static String JS_SHUTDOWN_FUNCTION = "ptolemyShutdown";

	public Ptolemy3D() {
	}

	/**
	 * Initialized pTolemy3D system with the specified settings.
	 * 
	 * @param config
	 */
	public static void initialize(Configuration config) {
		configuration = config;
		textureManager = new TextureManager();
		dataFinder = new DataFinder(configuration.getServer(),
				configuration.servers);
		mapDataManager = new MapDataManager();
		cache = new FileSystemCache();
	}

	/**
	 * Registers the canvas to be used by ptolemy.
	 * 
	 * TODO - This method must be temporal. This not allows to have various
	 * canvas render the same scene. For the moment only one canvas is allowed.
	 * In the future we could have a list of canvas registered in the system and
	 * rendering the same scene.
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
	 * Registers a javascript interface to communicate with the browser when
	 * pTolemy is used as applet.
	 * 
	 * @param javascript
	 */
	public static void registerApplet(Ptolemy3DJavascript javascript) {
		Ptolemy3D.javascript = javascript;
	}

	/**
	 * Start rendering threads.
	 */
	public static void start() {
		canvas.startRenderingLoop();

		// Call the start JavaScript function
		callJavascript(JS_START_FUNCTION, null);
	}

	/**
	 * Stop rendering threads.
	 */
	public static void stop() {
		canvas.stopRenderingLoop();

		// Call the stop JavaScript function
		callJavascript(JS_STOP_FUNCTION, null);
	}

	/**
	 * Stop the pTolemy system: request data thread and other resources.
	 */
	public static void shutDown() {
		stop();

		// Call the stop JavaScript function
		callJavascript(JS_SHUTDOWN_FUNCTION, null);

		// TODO - Test if necessary.
		// // Stop and shutDown all the 3D
		// if (canvas != null) {
		// canvas.destroyGL();
		// canvas = null;
		// }
	}

	/**
	 * @return the canvas
	 */
	public static Ptolemy3DGLCanvas getCanvas() {
		return canvas;
	}

	/**
	 * Returns the scene.
	 * 
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
	 * @param aTextureManager
	 *            the textureManager to set
	 */
	public static void setTextureManager(TextureManager aTextureManager) {
		textureManager = aTextureManager;
	}

	/**
	 * @return the map data manager
	 */
	public static MapDataManager getMapDataManager() {
		return mapDataManager;
	}

	/**
	 * @return the map data manager finder
	 */
	public static DataFinder getDataFinder() {
		return dataFinder;
	}

	/**
	 * @param mapDataManager
	 *            the textureManager to set
	 */
	public static void setMapDataManager(MapDataManager aMapDataManager) {
		mapDataManager = aMapDataManager;
	}

	public static FileSystemCache getFileSystemCache() {
		return cache;
	}

	public static Ptolemy3DJavascript getJavascript() {
		return javascript;
	}

	/**
	 * Call a javascript function.
	 * 
	 * @param javascripFunction
	 */
	public static void callJavascript(String javascripFunction,
			String[] plugargs) {
		if (javascript != null) {
			JSObject jsObject = javascript.getJSObject();
			if (jsObject != null) {
				try {
					jsObject.call(javascripFunction, plugargs);
				} catch (Exception ne) {
					IO.printStackRenderer(ne);
				}
			}
		}
	}

	/**
	 * Creates a new instance of the specified class. Useful to create new
	 * plugins instances.
	 * 
	 * @param className
	 *            fully qualified class name
	 * @return New instance or null otherwise
	 */
	public Object createInstance(String className) {

		try {
			return Class.forName(className).newInstance();
		} catch (ClassNotFoundException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		} catch (InstantiationException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			logger.severe(e.getMessage());
			e.printStackTrace();
		}

		return null;
	}
	//
	// /**
	// * Send an error message to javascript.
	// */
	// public final void sendErrorMessage(String msg) {
	// callJavascript("alert", msg, null, null);
	// }
}
