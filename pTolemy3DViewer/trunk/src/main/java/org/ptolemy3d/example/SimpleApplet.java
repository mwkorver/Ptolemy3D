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
package org.ptolemy3d.example;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.media.opengl.GLCanvas;

import netscape.javascript.JSObject;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DJavascript;

/**
 * Ptolemy3D in an Applet
 */
public class SimpleApplet extends Applet implements Ptolemy3DJavascript
{
	private static final long serialVersionUID = 1L;

	private Ptolemy3D ptolemy3d;
	private GLCanvas component3d;
	private JSObject jsObject;

	@Override
	public void init()
	{
		if(jsObject == null) {
			JSObject obj;
			try {
				obj = JSObject.getWindow(this);
			} catch(RuntimeException e) {
				obj = null;
			}
			this.jsObject = obj;
		}

		if(ptolemy3d == null) {
			ptolemy3d = new Ptolemy3D();
			ptolemy3d.javascript = this;
			ptolemy3d.init(null);

			this.component3d = new GLCanvas();
			this.component3d.addGLEventListener(ptolemy3d.events);
			this.component3d.setSize(getSize());

			this.setLayout(new BorderLayout());
			this.add(this.component3d, BorderLayout.CENTER);
			this.addMouseListener(new MouseAdapter() {
			    public void mouseEntered(MouseEvent e) {
			    	requestFocus();
			    }
			});
		}
	}

	@Override
	public void start()
	{
		if(ptolemy3d == null) {
			init();
		}

		requestFocus();
	}

	@Override
	public void stop()
	{
		getInputContext().removeNotify(component3d);

		//Destroy Ptolemy3D engine
		ptolemy3d.destroy();
		ptolemy3d = null;

		//Remove 3D component
		removeAll();
		component3d = null;
	}

	@Override
	public void destroy()
	{

	}

	/* Ptolemy3DJavascript interface */

	public Applet getApplet()
	{
		return this;
	}

	public JSObject getJSObject()
	{
		return jsObject;
	}
}
