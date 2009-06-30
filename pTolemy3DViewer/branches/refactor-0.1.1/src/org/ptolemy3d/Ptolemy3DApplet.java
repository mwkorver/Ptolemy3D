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

import netscape.javascript.JSObject;


/**
 * Ptolemy3D appletf.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Ptolemy3DApplet extends Applet implements Ptolemy3DJavascript {
	private static final long serialVersionUID = 1L;

	private Ptolemy3DGLCanvas canvas = null;
	private JSObject jsObject = null;

	/**
	 * Initialize the pTolemy applet.
	 */
	@Override
	public void init() {
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

	@Override
	public void start() {
		Ptolemy3D.start();
		requestFocus();
	}

	@Override
	public void stop() {
		Ptolemy3D.stop();
	}

	@Override
	public void destroy() {
		Ptolemy3D.shutDown();
	}

	public Applet getApplet() {
		return this;
	}

	public JSObject getJSObject() {
		return jsObject;
	}
}
