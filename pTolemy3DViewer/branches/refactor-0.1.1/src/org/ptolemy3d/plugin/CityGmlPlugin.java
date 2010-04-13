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
package org.ptolemy3d.plugin;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.xml.bind.JAXBException;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.view.Camera;

/**
 * CityGmlPlugin adds basic support for CityGML format. Allowed CityGML elements
 * it reads are Lod1/2MultiSurface and Lod1/2Solid. <br/>
 * The plugin accepts two parameters:
 * <ul>
 * <li>The 'altitude' parameter is used to decide which LOD to be show. When
 * camera is under the 'altitude' value LOD1 is shown (if any exists), LOD2 is
 * shown otherwise.</li>
 * 
 * <li>The 'wfsServerUrl' is the query URL to a WFS server which must return a
 * valid CityGML document.</li>
 * 
 * </ul>
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class CityGmlPlugin implements Plugin {

	private boolean status = true;
	//
	private String wfsServerUrl = "";
	private String fileUrl = null;
	private double altitude = 0;
	//
	private volatile boolean fileLoading = false;
	private volatile boolean fileLoaded = false;
	private volatile boolean errorLoading = false;
	private CityGmlReader cityReader = null;
	//
	private List<CityGmlBuildingArea> buildingAreas = new ArrayList<CityGmlBuildingArea>();

	public CityGmlPlugin() {
	}
	
	public void initGL(DrawContext drawContext) {
	}

	public void destroyGL(DrawContext drawContext) {
	}

	public void setPluginParameters(String params) {
		String[] parameters = params.split(",");
		try {
			altitude = Double.parseDouble(parameters[0]);
		} catch (NumberFormatException ex) {
			Logger.getLogger(CityGmlPlugin.class.getName()).warning(
					ex.getMessage());
		}
		wfsServerUrl = params.replaceAll(parameters[0] + ",", "");
	}

	public void motionStop(GL gl) {
	}

	public boolean pick(Vector3d intersectPoint, Vector3d[] ray) {
		return false;
	}

	public boolean onPick(Vector3d intersectPoint) {
		return false;
	}

	public void reloadData() {
	}

	/**
	 * Starts a new thread to load CityGML document.
	 */
	private void loadData() {

		Thread newThread = new Thread(new Runnable() {

			public void run() {
				fileLoading = true;

				Logger.getLogger(CityGmlPlugin.class.getName()).info(
						"Started CityGML loader thread...");

				URL url = null;
				try {
					if (fileUrl != null && !fileUrl.equals("")) {
						url = new URL(fileUrl);
					} else {
						url = new URL(wfsServerUrl);
					}

					cityReader = new CityGmlReader();
					cityReader.loadGML(url);
					CityGmlBuildingArea buildingArea = cityReader.getBuildingArea();
					if (buildingArea != null) {
						buildingAreas.add(buildingArea);
					}
				} catch (IOException ex) {
					Logger.getLogger(CityGmlPlugin.class.getName()).warning(
							"Can't load GML file: '" + url + "'");
					Logger.getLogger(CityGmlPlugin.class.getName()).log(
							Level.WARNING, ex.getMessage());
					errorLoading = true;
				} catch (JAXBException ex) {
					Logger.getLogger(CityGmlPlugin.class.getName()).warning(
							"Problems loading GML file: '" + url + "'");
					Logger.getLogger(CityGmlPlugin.class.getName()).log(
							Level.WARNING, ex.getMessage());
				} finally {
					fileLoaded = true;
					fileLoading = false;
					Logger.getLogger(CityGmlPlugin.class.getName()).info(
							"Finished CityGML loader thread...");
				}
			}
		}, "CityGmlPluginThread");

		newThread.start();
	}

	/**
	 * Renders the GML.
	 * 
	 * @param drawContext
	 */
	public void draw(DrawContext drawContext) {

		// Load data
		if (!fileLoading && !fileLoaded && !errorLoading) {
			loadData();
		}

		// If the gml file is not loaded return or there was an error loading
		// it then returns.
		if (!fileLoaded || errorLoading || cityReader == null) {
			return;
		}

		GL gl = drawContext.getGL();
		Camera camera = drawContext.getCanvas().getCamera();

		if (!status) {
			return;
		}

		// Store previous matrices
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();

		// Store attributes that will change
		gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_ENABLE_BIT
				| GL.GL_COLOR_BUFFER_BIT | GL.GL_LIGHTING_BIT);

		// Disable some flags
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_BLEND);
		gl.glDisable(GL.GL_COLOR_MATERIAL);

		// Enable lights
		gl.glEnable(GL.GL_LIGHTING);

		// Set FLAT shade
		gl.glShadeModel(GL.GL_FLAT);

		// Set color
		gl.glColor4f(0.0f, 0.0f, 1.0f, 1.0f);
		
		// Enable vertex array
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL.GL_NORMAL_ARRAY);

		// For each buildings decide which LOD must be drawn depending on the
		// altitude parameter.
		for (CityGmlBuildingArea buildingArea : buildingAreas) {
			if (!buildingArea.isVisible(camera)) {
				continue;
			}
			
			double camAltitude = camera.getPosition().getAltitudeDD();
			
			// Decide if show LOD1 or LOD2 depending on the altitude
			TriangleList cityTriangles = null;
			if (camAltitude < altitude) {
				cityTriangles = buildingArea.getLod1Geometry();
			}
			if (cityTriangles == null) {
				cityTriangles = buildingArea.getLod2Geometry();
			}
			
			if (cityTriangles != null) {
				cityTriangles.render(gl);
			}
		}

		// Disable vertex array
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
		
		// Restore attributes
		gl.glPopAttrib();

		// Restore matrices
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
	}

	/**
	 * @param altitude
	 *            the altitude to set
	 */
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	/**
	 * @return the altitude
	 */
	public double getAltitude() {
		return altitude;
	}

	/**
	 * @param wfsServerUrl
	 *            the wfsServerUrl to set
	 */
	public void setWfsServerUrl(String wfsServerUrl) {
		this.wfsServerUrl = wfsServerUrl;
	}

	/**
	 * @return the wfsServerUrl
	 */
	public String getWfsServerUrl() {
		return wfsServerUrl;
	}

	/**
	 * @param fileUrl
	 *            the fileUrl to set
	 */
	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	/**
	 * @return the fileUrl
	 */
	public String getFileUrl() {
		return fileUrl;
	}

}
