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
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.xml.bind.JAXBException;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.scene.Plugin;

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
 */
public class CityGmlPlugin implements Plugin {

	private boolean status = true;
	//
	private String wfsServerUrl = "";
	private String fileUrl = null;
	private double altitude = 0;
	//
	private boolean fileLoaded = false;
	private boolean errorLoading = false;
	private CityGmlReader cityReader = null;
	//
	private ArrayList<CityGmlBuildingData> listBuildindData = new ArrayList<CityGmlBuildingData>();
	private boolean vertexInitialized = false;

	public void initGL(DrawContext drawContext) {
		if (!fileLoaded) {

			Thread newThread = new Thread(new Runnable() {

				public void run() {
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
						listBuildindData = cityReader.getBuildingData();
					} catch (IOException ex) {
						Logger.getLogger(CityGmlPlugin.class.getName())
								.warning("Can't load GML file: '" + url + "'");
						Logger.getLogger(CityGmlPlugin.class.getName()).log(
								Level.WARNING, ex.getMessage());
						errorLoading = true;
					} catch (JAXBException ex) {
						Logger.getLogger(CityGmlPlugin.class.getName())
								.warning(
										"Problems loading GML file: '" + url
												+ "'");
						Logger.getLogger(CityGmlPlugin.class.getName()).log(
								Level.WARNING, ex.getMessage());
					} finally {
						fileLoaded = true;
						Logger.getLogger(CityGmlPlugin.class.getName()).info(
								"Finished CityGML loader thread...");
					}
				}
			}, "CityGmlPluginThread");

			newThread.start();
		}
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

	public boolean pick(double[] intersectPoint, double[][] ray) {
		return false;
	}

	public boolean onPick(double[] intersectPoint) {
		return false;
	}

	public void reloadData() {
	}

	/**
	 * Renders the GML.
	 * 
	 * @param drawContext
	 */
	public void draw(DrawContext drawContext) {

		GL gl = drawContext.getGL();

		if (!status) {
			return;
		}

		// If the gml file is not loaded return or there was an error loading
		// it then returns.
		if (!fileLoaded || errorLoading || cityReader == null) {
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
		gl.glDisable(GL.GL_CULL_FACE);		
		gl.glDisable(GL.GL_BLEND);
		gl.glDisable(GL.GL_COLOR_MATERIAL);

		// Enable lights
		gl.glEnable(GL.GL_LIGHTING);
		
		// Set FLAT shade
		gl.glShadeModel(GL.GL_FLAT);		

		// Set color
		float color[] = { 0.0f, 0.0f, 1.0f, 1.0f };
		gl.glColor4fv(color, 0);

		// Enable vertex array
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL.GL_NORMAL_ARRAY);

		// Compute the buildings vertices if they are not computed previously.
		if (!vertexInitialized) {
			for (CityGmlBuildingData buildingData : listBuildindData) {
				// Compute vertex array and normals for every polygon in the
				// building.
				buildingData.computeVertexAndNormals();
			}
			vertexInitialized = true;
		}
		
		// For each buildings decide which LOD must be drawn depending on the
		// altitude parameter.
		for (CityGmlBuildingData buildingData : listBuildindData) {

			ArrayList<CityPolygon> listCityPolygons = null;
			// Decide if show LOD1 or LOD2 depending on the altitude
			double camAltitude = drawContext.getCanvas().getCamera()
					.getPosition().getAltitudeDD();
			if (camAltitude < altitude
					&& buildingData.getLod1Polygons().size() > 0) {
				listCityPolygons = buildingData.getLod1Polygons();
			} else {
				listCityPolygons = buildingData.getLod2Polygons();
			}
			for (CityPolygon cityPolygon : listCityPolygons) {
				DoubleBuffer db = cityPolygon.getVertex();
				db.rewind();
				FloatBuffer fb = cityPolygon.getNormals();
				fb.rewind();
				gl.glVertexPointer(3, GL.GL_DOUBLE, 0, db);
				gl.glNormalPointer(GL.GL_FLOAT, 0, fb);
				gl.glDrawArrays(GL.GL_POLYGON, 0, db.capacity() / 3);
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
