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

import static org.ptolemy3d.debug.Config.DEBUG;

import java.awt.event.KeyEvent;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Scene;
import org.ptolemy3d.view.Camera;

/**
 * Provide access standard ouput and debugging keys.<BR>
 * <BR>
 * <BR>Debug Keyboard:<pre>
 * F1 : Show/Hide Landscape
 * F2 : Show/Hide Landscape Tiles
 * F3 : Show/Hide Profiler Hud
 * F4 :
 * F5 : Enable/Disable Wireframe rendering for Landscape.
 * F6 :
 * F7 : Test
 * F8 : Camera position
 * F9 : Force level ++
 * F10: Force level --
 * F11: Freeze visibility
 * F12: Ignore layer visibility range
 * </pre>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class IO {
	/* Output for standard informations */
	public final static void print(String format) {
		if (Config.enablePrint) {
			System.out.print(format);
		}
	}
	public final static void println(String format) {
		if (Config.enablePrint) {
			System.out.println(format);
		}
	}
	public final static void printf(String format, Object... args) {
		if (Config.enablePrint) {
			System.out.printf(format, args);
		}
	}
	public final static void printStack(Throwable e) {
		if (Config.enablePrint) {
			e.printStackTrace();
		}
	}

	/* Output for error informations */
	public final static void printError(String format) {
		if (Config.enablePrintError) {
			System.err.print(format);
		}
	}
	public final static void printlnError(String format) {
		if (Config.enablePrintError) {
			System.err.println(format);
		}
	}
	public final static void printfError(String format, Object... args) {
		if (Config.enablePrintError) {
			System.err.printf(format, args);
		}
	}
	public final static void printStackError(Throwable e) {
		if (Config.enablePrintError) {
			e.printStackTrace();
		}
	}

	/* Output for error informations */
	public final static void printManager(String format) {
		if (Config.enablePrintManager) {
			print(format);
		}
	}
	public final static void printlnManager(String format) {
		if (Config.enablePrintManager) {
			println(format);
		}
	}
	public final static void printfManager(String format, Object... args) {
		if (Config.enablePrintManager) {
			printf(format, args);
		}
	}
	public final static void printStackManager(Throwable e) {
		if (Config.enablePrintManager) {
			e.printStackTrace();
		}
	}
	
	/* Output for renderer informations */
	public final static void printRenderer(String format) {
		if (Config.enablePrintRender) {
			print(format);
		}
	}
	public final static void printlnRenderer(String format) {
		if (Config.enablePrintRender) {
			println(format);
		}
	}
	public final static void printfRenderer(String format, Object... args) {
		if (Config.enablePrintRender) {
			printf(format, args);
		}
	}
	public final static void printStackRenderer(Throwable e) {
		if (Config.enablePrintRender) {
			e.printStackTrace();
		}
	}
	
	/* Output for parser informations */
	public final static void printParser(String format) {
		if (Config.enablePrintParser) {
			print(format);
		}
	}
	public final static void printlnParser(String format) {
		if (Config.enablePrintParser) {
			println(format);
		}
	}
	public final static void printfParser(String format, Object... args) {
		if (Config.enablePrintParser) {
			printf(format, args);
		}
	}
	public final static void printStackParser(Throwable e) {
		if (Config.enablePrintParser) {
			e.printStackTrace();
		}
	}
	
	/* Output for plugin informations */
	public final static void printStackPlugin(Throwable e) {
		if (Config.enablePrintPlugin) {
			e.printStackTrace();
		}
	}

	/* Output for connection informations */
	public final static void printConnection(String format) {
		if (Config.enablePrintConnection) {
			print(format);
		}
	}
	public final static void printlnConnection(String format) {
		if (Config.enablePrintConnection) {
			println(format);
		}
	}
	public final static void printfConnection(String format, Object... args) {
		if (Config.enablePrintConnection) {
			printf(format, args);
		}
	}
	public final static void printStackConnection(Throwable e) {
		if (Config.enablePrintConnection) {
			e.printStackTrace();
		}
	}
	
	/* Output for javascript informations */
	public final static void printStackJavascript(Throwable e) {
		if (Config.enablePrintJavascript) {
			e.printStackTrace();
		}
	}


	/* Output for debug informations */
	public final static void printDebug(String text) {
		if (Config.enablePrintDebug) {
			print(text);
		}
	}
	public final static void printlnDebug(String text) {
		if (Config.enablePrintDebug) {
			println(text);
		}
	}
	public final static void printfDebug(String format, Object... args) {
		if (Config.enablePrintDebug) {
			printf(format, args);
		}
	}

	/* Debug keyboard */
	public final static void keyReleasedDebug(Ptolemy3DGLCanvas canvas, int keyCode) {
		if (DEBUG) {
			Scene scene = Ptolemy3D.getScene();
			Landscape landscape = scene.getLandscape();

			switch (keyCode) {
				case KeyEvent.VK_F1:
					//Hide/Show landscape
					scene.getLandscape().drawLandscape = !scene.getLandscape().drawLandscape;
					break;
				case KeyEvent.VK_F2:
					ProfilerUtil.renderTiles = !ProfilerUtil.renderTiles;
					break;
				case KeyEvent.VK_F3:
					ProfilerUtil.printProfiler = !ProfilerUtil.printProfiler;
					break;
				case KeyEvent.VK_F5:
					switch (landscape.getDisplayMode()) {
						case Landscape.DISPLAY_STANDARD:
							landscape.setDisplayMode(Landscape.DISPLAY_MESH);
							break;
						case Landscape.DISPLAY_MESH:
							landscape.setDisplayMode(Landscape.DISPLAY_SHADEDDEM);
							break;
						case Landscape.DISPLAY_SHADEDDEM:
							landscape.setDisplayMode(Landscape.DISPLAY_JP2RES);
							break;
						case Landscape.DISPLAY_JP2RES:
							landscape.setDisplayMode(Landscape.DISPLAY_TILEID);
							break;
						case Landscape.DISPLAY_TILEID:
							landscape.setDisplayMode(Landscape.DISPLAY_LEVELID);
							break;
						case Landscape.DISPLAY_LEVELID:
							landscape.setDisplayMode(Landscape.DISPLAY_STANDARD);
							break;
					}
					break;
				case KeyEvent.VK_F7:
					ProfilerUtil.test = !ProfilerUtil.test;
					println("Test: "+ProfilerUtil.test);
					break;
				case KeyEvent.VK_F8:
					Camera camera = canvas.getCamera();
					println(camera.toString());
					break;
				case KeyEvent.VK_F9:
					ProfilerUtil.forceLevel++;
					println("Force level: "+ProfilerUtil.forceLevel);
					break;
				case KeyEvent.VK_F10:
					ProfilerUtil.forceLevel = -1;
					println("Force level: "+ProfilerUtil.forceLevel);
					break;
				case KeyEvent.VK_F11:
					ProfilerUtil.freezeVisibility = !ProfilerUtil.freezeVisibility;
					println("Freeze visibility: "+ProfilerUtil.freezeVisibility);
					break;
				case KeyEvent.VK_F12:
					ProfilerUtil.ignoreVisiblityRange = !ProfilerUtil.ignoreVisiblityRange;
					println("Visibility range: "+ProfilerUtil.ignoreVisiblityRange);
					break;
			}
		}
	}
}
