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

import static org.ptolemy3d.debug.Config.enableProfiler;

import java.awt.Font;
import java.util.Locale;
import java.util.Vector;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.Profiler.ProfilerEvent;
import org.ptolemy3d.debug.Profiler.ProfilerEventReport;
import org.ptolemy3d.globe.Globe;
import org.ptolemy3d.globe.Layer;
import org.ptolemy3d.globe.MapDataKey;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Scene;
import org.ptolemy3d.util.FontRenderer;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.Position;

/**
 * By encapsulating any debug related code, the compiler will
 * <pre>
 * if (org.ptolemy3d.debug.Debug.DEBUG) {
 *     //Code to be compiled only on debug build
 * }
 * </pre>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class ProfilerUtil
{
	private static Profiler profiler = null;
	private static Timer counter = null;

	/** Enable Profiler Prints */
	public static boolean printProfiler = true;
	/** Tile counter */
	public static int tileCounter = 0;
	/** Tile counter */
	public static int tileSectionCounter = 0;
	/** Vertex counter */
	public static int vertexCounter = 0;
	/** Bytes downloaded this frame */
	public static int bytesDownloaded = 0;
	/** Total byte downloaded */
	public static int totalBytesDownloaded = 0;
	/** Download speed */
	public static float koPerSec = 0;
	private static int accumDLBytes = 0, accumDLMs = 0;	//Accumulation to display 1s average speed
	private static final int DL_FPS_UPDATE = 500;
	/** Vertex counter */
	public static int vertexMemoryUsage = 0;
	/** Tiles */
	public static boolean renderTiles = true;
	/** Force Level */
	public static int forceLayer = -1;
	/** Freeze visibility */
	public static boolean freezeVisibility = false;
	/** Z-Fight correction */
	public static boolean zFightCorrection = false; // No more needed
	/** Variable used for misc test */
	public static boolean test = false;

	/** Font */
	private static FontRenderer font;

	public static enum ProfilerEventInterface implements ProfilerEvent {
		Frame, Prepare, Render, Sky, Landscape, Plugins, Screenshot;
	}

	private static void init() {
		if(enableProfiler) {
			if(counter == null) {
				counter = new Timer();
			}
			if(profiler == null) {
				profiler = new Profiler(counter);
				for(ProfilerEventInterface event : ProfilerEventInterface.values()) {
					profiler.registerEvent(event);
				}
			}
		}
	}

	private static void update() {
		if(enableProfiler) {
			profiler.start();
		}
	}

	public static void start(ProfilerEventInterface event) {
		if(enableProfiler) {
			init();
			if(event == ProfilerEventInterface.Frame) {
				update();
				tileCounter = 0;
				tileSectionCounter = 0;
				vertexCounter = 0;
				vertexMemoryUsage = 0;
			}
			profiler.onEventStart(event);
		}
	}

	public static void stop(ProfilerEventInterface event) {
		if(enableProfiler) {
			init();
			profiler.onEventEnd(event);
		}
	}

	public static ProfilerEventReport getReport(ProfilerEventInterface event) {
		if(enableProfiler) {
			init();
			return profiler.getEventReport(event);
		}
		return null;
	}

	public static float getFPS() {
		if(enableProfiler) {
			init();
			return counter.getFPS();
		}
		return 0;
	}
	
	public static float getTimePassedMillis() {
		if(enableProfiler) {
			init();
			return counter.getTimePassedMillis();
		}
		return 1000000000;
	}

	private static Vector<String> generateReport() {
		if(printProfiler) {
			Vector<String> report = new Vector<String>(4);
			int count = 0;
			while(true) {
				String line = null;
				switch(count) {
					case 0:
						accumDLBytes += bytesDownloaded;
						accumDLMs += getTimePassedMillis();
						if(accumDLMs > DL_FPS_UPDATE) {
							koPerSec = (accumDLBytes / 1024.f) / (accumDLMs / 1000.f);
							accumDLBytes = 0;
							accumDLMs = 0;
						}
						totalBytesDownloaded += bytesDownloaded;
						bytesDownloaded = 0;
						
						float totalKO = (totalBytesDownloaded / 1024.f);
						final MapDataKey dlMap = Ptolemy3D.getMapDataManager().getDownloadingMap();
						line = String.format("DL speed:%f ko|TotalDL:%f ko|DL map:%s", koPerSec, totalKO, format(dlMap));
						break;
					case 1:
						final MapDataKey decodeMap = Ptolemy3D.getMapDataManager().getDecodingMap();
						line = String.format("Decode map:%s", format(decodeMap));
						break;
					case 2:
						float fps = getFPS();
						ProfilerEventReport renderProf = ProfilerUtil.getReport(ProfilerEventInterface.Frame);
						ProfilerEventReport landscapeProf = ProfilerUtil.getReport(ProfilerEventInterface.Landscape);

						if(renderProf != null && landscapeProf != null) {
							//float landscapPercent = (landscapeProf.duration == 0) ? 0 : (float)renderProf.duration / landscapeProf.duration;
							float landscapFPS = (landscapeProf.duration == 0) ? 0 : (float)1000000
									/ landscapeProf.duration;

							line = String.format("Frame:%dFPS|Landscape:%dFPS", (int)fps, (int)landscapFPS);
						}
						break;
					case 3:
						float memUsage = vertexMemoryUsage / 1024.f;
						line = String.format("Tile:%d|Section:%d|Vtx:%d|VtxUsage:%.2f ko", tileCounter,
								tileSectionCounter, vertexCounter, memUsage);
						break;
					case 4:
						final Globe globe = Ptolemy3D.getScene().getLandscape().globe;
						final int numLayers = globe.getNumLayers();
						//Format active layers in a list: (activeLayer0Id,activeLayer1Id,...)
						String activeLayers = "(";
						boolean first = true;
						for(int i = 0; i < numLayers; i++) {
							final Layer layer = globe.getLayer(i);
							if(layer.isVisible()) {
								if(!first) {
									activeLayers += ",";
								}
								activeLayers += i;
								first = false;
							}
						}
						activeLayers += ")";
						line = String.format("NumLayers:%d|ActiveLayers:%s",
								numLayers, activeLayers);
						break;
					case 5:
						Camera camera = Ptolemy3D.getCanvas().getCamera();
						Position pos = camera.getPosition();
						line = String.format(Locale.US, "(lat,lon,alt): (%f,%f,%f)",
								(float)pos.getLatitudeDD(), (float)pos.getLongitudeDD(), (float)pos.getAltitudeDD());
						break;
					case 6:
						Scene scene = Ptolemy3D.getScene();
						Landscape landscape = scene.getLandscape();

						String landscapeRenderMode;
						switch(landscape.getDisplayMode()) {
							case Landscape.DISPLAY_STANDARD:
								landscapeRenderMode = "Standard";
								break;
							case Landscape.DISPLAY_MESH:
								landscapeRenderMode = "Mesh";
								break;
							case Landscape.DISPLAY_SHADEDDEM:
								landscapeRenderMode = "Elevation";
								break;
							case Landscape.DISPLAY_JP2RES:
								landscapeRenderMode = "JP2 resolution";
								break;
							case Landscape.DISPLAY_TILEID:
								landscapeRenderMode = "Tile id";
								break;
							case Landscape.DISPLAY_LEVELID:
								landscapeRenderMode = "Level id";
								break;
							default:
								return null;
						}
						line = "RenderMode: " + landscapeRenderMode;
						break;
				}
				if(line == null) {
					break;
				}
				report.add(line);
				count++;
			}
			return report;
		}
		return null;
	}
	private static String format(MapDataKey key) {
		if(key == null) {
			return "None";
		}
		else {
			return String.format("(%d, %.2f°, %.2f°)", key.layer, ddToDegree(key.lat), ddToDegree(key.lon));
		}
	}
	private static float ddToDegree(int dd) {
		return (float)Unit.ddToDegrees(dd);
	}

	public static void drawProfiler(GL gl) {
		if(printProfiler) {
			FontRenderer font = getFont(gl);
			font.start();
			drawGUI(font);
			drawDisplayModeLegend(font);
			font.end();
		}
	}

	private static void drawGUI(FontRenderer font) {
		Vector<String> profilerReport = ProfilerUtil.generateReport();
		if(profilerReport != null) {
			font.draw(profilerReport, 10, 15, 14);
		}
	}

	private static void drawDisplayModeLegend(FontRenderer font) {
		Scene scene = Ptolemy3D.getScene();
		Landscape landscape = scene.getLandscape();

		Vector<String> legend = new Vector<String>(11);
		switch(landscape.getDisplayMode()) {
			case Landscape.DISPLAY_STANDARD:
				legend.add("Aerial view of the earth :)");
				break;
			case Landscape.DISPLAY_MESH:
				legend.add("Lines show the globe geometry.");
				break;
			case Landscape.DISPLAY_SHADEDDEM:
				legend.add("Color is point elevation/altitude");
				legend.add("Color is point elevation/altitude");
				legend.add("Color is point elevation/altitude");
				break;
			case Landscape.DISPLAY_JP2RES:
				legend.add("Red:   JP2 Wavalet 0");
				legend.add("Green: JP2 Wavalet 1");
				legend.add("Blue:  JP2 Wavalet 3");
				legend.add("White: JP2 Wavalet 4");
				legend.add("Square with constant color is a tile.");
				break;
			case Landscape.DISPLAY_TILEID:
				legend.add("Square with constant color is a tile.");
				break;
			case Landscape.DISPLAY_LEVELID:
				legend.add("Area with a constant color correspond to a layer id.");
				legend.add("Full Red:    Layer 0");
				legend.add("Full Green:  Layer 1");
				legend.add("Full Blue:   Layer 2");
				legend.add("Red:         Layer 3");
				legend.add("Green:       Layer 4");
				legend.add("Blue:        Layer 5");
				legend.add("Light Red:   Layer 6");
				legend.add("Light Green: Layer 7");
				legend.add("Light Blue:  Layer 8");
				legend.add("...");
				break;
		}

		//int width = ptolemy.events.drawWidth;
		int height = Ptolemy3D.getCanvas().getHeight();
		font.draw(legend, 10, height - 20, -14);
	}

	private static FontRenderer getFont(GL gl) {
		if(font == null) {
			font = new FontRenderer(new Font("Courier New", Font.ITALIC, 14));
			font.initGL();
		}
		return font;
	}

	public static long getTimePassedMS() {
		if(enableProfiler) {
			init();
			return counter.getTimePassedMillis();
		}
		return 0;
	}
}
