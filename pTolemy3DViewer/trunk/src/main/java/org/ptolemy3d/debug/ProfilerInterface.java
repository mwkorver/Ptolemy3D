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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.Profiler.ProfilerEvent;
import org.ptolemy3d.font.FntFontRenderer;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Sky;
import org.ptolemy3d.view.Camera;

/**
 * By encapsulating any debug related code, the compiler will
 * <pre>
 * if (org.ptolemy3d.debug.Debug.DEBUG) {
 *     //Code to be compiled only on debug build
 * }
 * </pre>
 */
public class ProfilerInterface
{
	private static Profiler profiler = null;
	private static Timer counter = null;
	/** Enable Profiler Prints */
	public static boolean enablePrintProfiler = false;
	/** Tile counter */
	public static int tileCounter = 0;
	/** Tile counter */
	public static int tileSectionCounter = 0;
	/** Vertex counter */
	public static int vertexCounter = 0;
	/** Vertex counter */
	public static int vertexMemoryUsage = 0;
	/** Tiles */
	public static boolean renderTiles = true;
	/** Force Level */
	public static int forceLevel = -1;
	/** Freeze CorrectTile */
	public static boolean freezeCorrectLevels = false;
	/** Freeze Visiblity */
	public static boolean freezeVisibility  = false;

	public static enum ProfilerEventInterface implements ProfilerEvent {
		Frame, Prepare, Render, Sky, Landscape, Plugins, Screenshot;
	}

	private static void init()
	{
		if (enableProfiler) {
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

	private static void update()
	{
		if (enableProfiler) {
			profiler.start();
		}
	}

	public static void start(ProfilerEventInterface event)
	{
		if (enableProfiler) {
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

	public static void stop(ProfilerEventInterface event)
	{
		if (enableProfiler) {
			init();
			profiler.onEventEnd(event);
		}
	}

	public static ProfilerEventReport getReport(ProfilerEventInterface event)
	{
		if (enableProfiler) {
			init();
			return profiler.getEventReport(event);
		}
		return null;
	}


	public static float getFPS()
	{
		if (enableProfiler) {
			init();
			return counter.getFPS();
		}
		return 0;
	}
	public static String generateReport(int line)
	{
		if (enablePrintProfiler) {
			switch(line) {
				case 0:
					int fps = (int)getFPS();
					ProfilerEventReport renderProf    = ProfilerInterface.getReport(ProfilerEventInterface.Frame);
					ProfilerEventReport landscapeProf = ProfilerInterface.getReport(ProfilerEventInterface.Landscape);

					if(renderProf != null && landscapeProf != null) {
//						float landscapPercent = (landscapeProf.duration == 0) ? 0 : (float)renderProf.duration / landscapeProf.duration;
						float landscapFPS     = (landscapeProf.duration == 0) ? 0 : (float)1000000             / landscapeProf.duration;

						return String.format("Frame:%dFPS|Landscape:%.1fFPS",
								fps, landscapFPS);
					}
					break;
				case 1:
					float memUsage = vertexMemoryUsage / 1024.f;
					return String.format("Tile:%d|Section:%d|Vtx:%d|VtxUsage:%.2f ko",
							tileCounter, tileSectionCounter, vertexCounter, memUsage);
				case 2:
					return String.format("CorrectFreeze:%s|VisibilityFreeze:%s",
							freezeCorrectLevels, freezeVisibility);
				case 3:
					String landscapeRenderMode;
					switch(Ptolemy3D.ptolemy.scene.landscape.displayMode) {
						case Landscape.DISPLAY_STANDARD:  landscapeRenderMode = "Standard"; break;
						case Landscape.DISPLAY_MESH:      landscapeRenderMode = "Mesh"; break;
						case Landscape.DISPLAY_SHADEDDEM: landscapeRenderMode = "Elevation"; break;
						case Landscape.DISPLAY_JP2RES:    landscapeRenderMode = "JP2 resolution"; break;
						case Landscape.DISPLAY_TILEID:    landscapeRenderMode = "Tile id"; break;
						case Landscape.DISPLAY_LEVELID:   landscapeRenderMode = "Level id"; break;
						default: return null;
					}
					return "RenderMode: "+landscapeRenderMode;
			}
		}
		return null;
	}
	public static void drawProfiler(Ptolemy3D ptolemy, GL gl)
	{
		if(enablePrintProfiler) {
			if(ptolemy.fontTextRenderer == null) {
				return;
			}
			FntFontRenderer fontText = ptolemy.fontTextRenderer;

			final Sky sky = ptolemy.scene.sky;
			final Camera camera = ptolemy.camera;
			final Ptolemy3DUnit unit = ptolemy.unit;

			int sclsize = 50;
			double logoScaler = (camera.getVerticalAltitudeMeters() > sky.horizonAlt) ? ((sky.horizonAlt * unit.coordSystemRatio) / 2) : 5;
			double aspect = ptolemy.events.drawAspectRatio;
			double pixratiox = (((Math.tan(Math3D.degToRad * 60.0) * sclsize) * aspect) / ptolemy.events.drawWidth);

			double indentx = pixratiox;

			double ulx, uly, urx;

			ulx = -((Math.tan((60.0 * Math3D.degToRad) / 2) * sclsize) * aspect) + indentx;
			urx = (((Math.tan((60.0 * Math3D.degToRad) / 2) * sclsize) * aspect));
			uly = -(Math.tan(Math3D.degToRad * 30.0) * sclsize) + 4;	//+ 4 to move up

			/* Keep text at constant size, when window is resized */
			float scalerX = 1, scalerY = 1;
			Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
			scalerX = (float) scrnsize.width / (float)ptolemy.events.drawWidth;
			scalerY = (float) scrnsize.height / (float)ptolemy.events.drawHeight;

			gl.glDisable(GL.GL_DEPTH_TEST);
			gl.glEnable(GL.GL_BLEND);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

			gl.glPushMatrix();
			gl.glLoadIdentity();
			gl.glTranslated(0, 0, -logoScaler);

			logoScaler /= sclsize;

			gl.glScaled(logoScaler, logoScaler, logoScaler);

			// bg box
			Color color = ptolemy.configuration.hudBackgroundColor;
			gl.glColor4b((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha());
			gl.glBegin(GL.GL_QUADS);
				gl.glTexCoord2f(0, 0);
				gl.glVertex3d(ulx, uly + (1.157 * scalerY), 0);
				gl.glTexCoord2f(0, 1);
				gl.glVertex3d(ulx, uly, 0);
				gl.glTexCoord2f(1, 1);
				gl.glVertex3d(urx, uly, 0);
				gl.glTexCoord2f(1, 0);
				gl.glVertex3d(urx, uly + (1.157 * scalerY), 0);
			gl.glEnd();
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

			//Profiler report
			float str_length = 0;

			fontText.bindTexture(gl);
			String profilerReport;
			int line = 0;
			while((profilerReport = ProfilerInterface.generateReport(line)) != null) {
				str_length += fontText.drawLeftToRight(gl, profilerReport, ulx, uly+0.09);

				uly += 1.157 * scalerY; line++;
			}

			gl.glPopMatrix();

			gl.glEnable(GL.GL_DEPTH_TEST);
			gl.glDisable(GL.GL_BLEND);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);
		}
	}
	public static long getTimePassed()
	{
		if (enableProfiler) {
			init();
			return counter.getTimePassedMillis();
		}
		return 0;
	}
}
