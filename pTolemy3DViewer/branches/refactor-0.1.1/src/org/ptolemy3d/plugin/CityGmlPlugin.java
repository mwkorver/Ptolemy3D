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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.xml.bind.JAXBException;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.Position;

/**
 * CityGmlPlugin adds basic support for CityGML format. Allowed CityGML elements
 * it reads are Lod1MultiSurface and Lod1Solid, etc
 * 
 * The 'wfsServerUrl' is the query URL to a WFS server which must return a valid
 * CityGML document.
 * 
 * The 'altitude' parameter is used to decide which LOD to be show. When camera
 * is under the 'altitude' value LOD1 is shown, LOD2 is shown otherwise.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 */
public class CityGmlPlugin implements Plugin {

	private static final String NAME = "CITY_GML_PLUGIN_";
	private int index = -1;
	private boolean status = true;
	//
	private String wfsServerUrl = "";
	private double altitude = 0;
	//
	private boolean fileLoaded = false;
	private boolean errorLoading = false;
	private CityGmlReader cityReader = null;
	//
	private ArrayList<CityGmlBuildingData> listBuildindData = new ArrayList<CityGmlBuildingData>();
	private boolean isVertexArrayInitialized = false;

	public void initGL(DrawContext drawContext) {
		if (!fileLoaded) {

			Thread newThread = new Thread(new Runnable() {

				public void run() {
					IO.printlnDebug("Started CityGML loader thread...");

					URL url = null;
					try {
						// String server = Ptolemy3D.getConfiguration()
						// .getServer();
						// url = new URL("http://" + server + wfsServerUrl);
						String s = "http://localhost:8080/openbd/CFML/pseudoWFS/wfs.cfm?SERVICE=wfs&REQUEST=GetFeature&bbox=-180,-90,180,90";
						url = new URL(wfsServerUrl);

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
						IO.printlnDebug("Finished CityGML loader thread.");
					}
				}
			}, "CityGmlPluginThread");

			newThread.start();
		}
	}

	public void destroyGL(DrawContext drawContext) {
	}

	public void setPluginIndex(int index) {
		this.index = index;
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

	public String pluginAction(String commandname, String command_params) {
		if (commandname.equalsIgnoreCase("status")) {
			status = (Integer.parseInt(command_params) == 1) ? true : false;
		} else if (commandname.equalsIgnoreCase("getLayerName")) {
			return NAME + index;
		}
		return null;
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
		Camera camera = drawContext.getCanvas().getCamera();

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
				| GL.GL_COLOR_BUFFER_BIT);

		gl.glDisable(GL.GL_CULL_FACE);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		gl.glColor4f(0.7f, 0.5f, 0.5f, 0.9f);

		// Initialize the vertex array of all buildings
		if (!isVertexArrayInitialized) {
			for (CityGmlBuildingData buildingData : listBuildindData) {
				ArrayList<CityPolygon> listCityPolygonsLod1 = buildingData
						.getLod1Polygons();
				ArrayList<CityPolygon> listCityPolygonsLod2 = buildingData
						.getLod2Polygons();

				// For each polygon initiale its vertex array.
				for (CityPolygon cityPolygon : listCityPolygonsLod1) {
					cityPolygon.initVertexArray(cityPolygon.getPositions()
							.size() * 3);
					for (Position position : cityPolygon.getPositions()) {
						double[] d = Camera.computeCartesianPoint(position
								.getLongitudeDD(), position.getLatitudeDD(),
								position.getAltitudeDD());
						cityPolygon.addVertex(d[0]);
						cityPolygon.addVertex(d[1]);
						cityPolygon.addVertex(d[2]);
					}
				}
				for (CityPolygon cityPolygon : listCityPolygonsLod2) {
					cityPolygon.initVertexArray(cityPolygon.getPositions()
							.size() * 3);
					for (Position position : cityPolygon.getPositions()) {
						double[] d = Camera.computeCartesianPoint(position
								.getLongitudeDD(), position.getLatitudeDD(),
								position.getAltitudeDD());
						cityPolygon.addVertex(d[0]);
						cityPolygon.addVertex(d[1]);
						cityPolygon.addVertex(d[2]);
					}
				}
			}
			isVertexArrayInitialized = true;
		}

		// Enable vertex array
		gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

		// For each buildings decide which LOD and draws it
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
				gl.glVertexPointer(3, GL.GL_DOUBLE, 0, db);
				gl.glDrawArrays(GL.GL_POLYGON, 0, db.capacity());
			}
		}

		// Disable vertex array
		gl.glDisableClientState(GL.GL_VERTEX_ARRAY);

		// Restore attributes
		gl.glPopAttrib();

		// Restore matrices
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
	}

}
