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

package org.ptolemy3d.debug;

/**
 * General configuration settings.<BR>
 * By encapsulating any debug related code,
 * the compiler will automatically remove it during
 * the compilation in non debug mode.
 * <pre>
 * if (org.ptolemy3d.debug.Debug.DEBUG) {
 *     //Code to be compiled only on debug build
 * }
 * </pre>
 */
public class Config
{
	/** Debug flag, set this to false to disable all debug related code (and remove from the compiled file) */
	public final static boolean DEBUG = false;	//final static: false compiler will remove all blocks: if(DEBUG) { ... }

	/** Standard output */
	public final static boolean enablePrint           = DEBUG && true;
	/** Standard output for error */
	public final static boolean enablePrintError      = DEBUG && true;
	/** Connection output */
	public final static boolean enablePrintConnection = DEBUG && true;
	/** Javascript output */
	public final static boolean enablePrintJavascript = DEBUG && true;
	/** Renderer output */
	public final static boolean enablePrintRender     = DEBUG && true;
	/** Plugin output */
	public final static boolean enablePrintPlugin     = DEBUG && true;
	/** Debug output */
	public final static boolean enablePrintDebug      = DEBUG && true;

	/** Enable Profiler*/
	public final static boolean enableProfiler        = DEBUG && true;
}