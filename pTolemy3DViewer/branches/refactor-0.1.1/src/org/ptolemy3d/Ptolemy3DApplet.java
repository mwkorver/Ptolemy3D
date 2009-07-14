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

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import netscape.javascript.JSObject;

/**
 * Ptolemy3D applet. It has methods to get required reference to the ptolemy
 * system, scene, canvas, etc. Also it provides a helpful way to create new
 * object, useful to create new plugins instances.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Ptolemy3DApplet extends Applet implements Ptolemy3DJavascript {
	private static final long serialVersionUID = 1L;

	// JavaScript function names
	private static String JS_START_FUNCTION = "ptolemyStart";
	private static String JS_STOP_FUNCTION = "ptolemyStop";
	private static String JS_SHUTDOWN_FUNCTION = "ptolemyShutdown";

	// Dummy instance to allow return references to browser.
	private Ptolemy3D ptolemy = new Ptolemy3D();
	private Unit unit = new Unit();

	// Canvas reference
	private Ptolemy3DGLCanvas canvas = null;
	private JSObject jsObject = null;
	private static final Logger logger = Logger.getLogger(Ptolemy3DApplet.class
			.getName());

	/**
	 * Initialize the pTolemy applet.
	 */
	@Override
	public void init() {

		// Policy.setPolicy(new Policy() {
		// public void refresh() {
		// }
		//
		// public PermissionCollection getPermissions(CodeSource cs) {
		// Permissions perms = new Permissions();
		// perms.add(new AllPermission());
		// return (perms);
		// }
		// });

		if (jsObject == null) {
			JSObject obj;
			try {
				obj = JSObject.getWindow(this);
			} catch (RuntimeException e) {
				obj = null;
			}
			this.jsObject = obj;
		}

		if (canvas == null) {
			// Register applet
			Ptolemy3D.registerApplet(this);

			// Initialize ptolemy system. When configuration doesn't use any XML
			// config file it is suppose to be executed as applet.
			Configuration config = new Configuration();
			Ptolemy3D.initialize(config);

			// Create the canvas and register it into Ptolemy3D system.
			canvas = new Ptolemy3DGLCanvas();
			Ptolemy3D.registerCanvas(canvas);

			this.setLayout(new BorderLayout());
			this.add(canvas, BorderLayout.CENTER);
			this.addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					requestFocus();
				}
			});
		}
	}

	/**
	 * Applet start. Calls JavaScript 'ptolemyStart' function once the Ptolemy3D
	 * system is started.
	 */
	@Override
	public void start() {
		Ptolemy3D.start();
		requestFocus();

		// Call the start JavaScript function
		Ptolemy3D.callJavascript(JS_START_FUNCTION, null);
	}

	/**
	 * Applet stop. Calls JavaScript 'ptolemyStop' function once the Ptolemy3D
	 * system is stopped.
	 */
	@Override
	public void stop() {
		Ptolemy3D.stop();

		// Call the stop JavaScript function
		Ptolemy3D.callJavascript(JS_STOP_FUNCTION, null);
	}

	/**
	 * Applet destroy. Calls JavaScript 'ptolemyShutdown' function once the
	 * Ptolemy3D system is stopped.
	 */
	@Override
	public void destroy() {
		Ptolemy3D.shutDown();

		// Call the stop JavaScript function
		Ptolemy3D.callJavascript(JS_SHUTDOWN_FUNCTION, null);
	}

	/**
	 * Get a reference to the applet itself.
	 */
	public Applet getApplet() {
		return this;
	}

	/**
	 * Get a reference to the JSObject that uses the applet to comunicate with
	 * the browser.
	 */
	public JSObject getJSObject() {
		return jsObject;
	}

	/**
	 * Get a Ptolemy3D singleton instance reference, it allows access to the
	 * scene and so on.
	 * 
	 * @return
	 */
	public Ptolemy3D getPtolemy() {
		return ptolemy;
	}

	/**
	 * Get the Unit singleton instance reference.
	 * 
	 * @return
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * Get a reference to the Ptolemy3DGLCanvas used in the applet.
	 * 
	 * @return
	 */
	public Ptolemy3DGLCanvas getCanvas() {
		return canvas;
	}
}
