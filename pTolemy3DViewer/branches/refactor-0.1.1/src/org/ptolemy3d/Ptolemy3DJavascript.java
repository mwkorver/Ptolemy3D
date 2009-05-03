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

import netscape.javascript.JSObject;

/**
 * Interface between Ptolemy3D and Javascript.
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public interface Ptolemy3DJavascript {
	public Applet getApplet();
	public JSObject getJSObject();
}
