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
package org.ptolemy3d.globe;

import static org.ptolemy3d.debug.Config.DEBUG;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.CameraMovement;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Globe {
	private final static boolean FORCE_BYPASS_MIN_VISIBILITY_RANGE = true;
	// Limit the number of layers active for the camera altitude
	private final static int MAX_DIFFERENT_LAYER = 3;

	private Layer[] layers;

	public void processVisibility(DrawContext drawContext) {
		final Camera camera = drawContext.getCanvas().getCamera();
		boolean hasVisLayer = false;

		int numInRange = 0; // Track the number of levels
		for (int i = layers.length - 1; i >= 0; i--) {
			final Layer layer = layers[i];
			
			if (numInRange >= MAX_DIFFERENT_LAYER) {	//FIXME
				for (int j = i; j >= 0; j--) {
					final Layer prevLayer = layers[j];
                    prevLayer.setVisible(false);
				}
				break;
			}

			// Altitude check: in layer range
			final double altMeters = camera.getVerticalAltitudeMeters();
			boolean inRange = 
				(altMeters <= layer.getMaxZoom()) && (altMeters >= layer.getMinZoom() || FORCE_BYPASS_MIN_VISIBILITY_RANGE);
			if (DEBUG && ProfilerUtil.ignoreVisiblityRange) {
				inRange = true;
			}
			if (inRange) {
				numInRange++; // Track the number of level in the altitude range
			}

			if (!inRange && (hasVisLayer || (altMeters > layer.getMaxZoom()))) {
				layer.setVisible(false);
				continue;
			}

			hasVisLayer = true;
			layer.setVisible(true);

			// Process visibility for visible layers
			layer.processVisibility(drawContext);
		}
	}

	public void draw(DrawContext drawContext) {
		for (int i = layers.length - 1; i >= 0; i--) {
			final Layer layer = layers[i];
			layer.draw(drawContext);
		}
	}

	/** Landscape Picking: Pick tiles */
	public final synchronized boolean pick(double[] intersectPoint, double[][] ray) {
		for (int i = layers.length - 1; i >= 0; i--) {
			final Layer layer = layers[i];
			if (layer.pick(intersectPoint, ray)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Retrieve the ground height.
	 * @param lon longitude in DD
	 * @param lat latitude in DD
	 * @param minLevel minimum level
	 * @return the ground height, 0 is the reference value (no elevation).
	 */
	public double groundHeight(double lon, double lat, int minLevel) {
		double[][] ray = {
				{lon, CameraMovement.MAXIMUM_ALTITUDE, lat},
				{lon, CameraMovement.MAXIMUM_ALTITUDE - 1, lat}
		};

		for (int i = layers.length - 1; (i >= minLevel); i--) {
			final Layer layer = layers[i];

			try {
				double[] pickArr = layer.groundHeight(lon, lat, ray);
				if (pickArr != null) {
					return pickArr[1];
				}
			}
			catch (RuntimeException e) {
				IO.printStackRenderer(e);
			}
		}
		return 0;
	}
	
	/** */
	public MapDataKey getCloserTile(int layerID, int lon, int lat) {
		final int tileSize = getLayer(layerID).getTileSize();
		
		final int refLon = (lon / tileSize) * tileSize;
		final int refLat = (lat / tileSize) * tileSize;
		
		final int closeLon;
		if((lon >= refLon) && (lon < refLon+tileSize)) {
			closeLon = refLon;
		}
		else {
			closeLon = refLon - tileSize;
		}
		
		final int closeLat;
		if((lat <= refLat) && (lat > refLat-tileSize)) {
			closeLat = refLat;
		}
		else {
			closeLat = refLat + tileSize;
		}
		
		return new MapDataKey(layerID, closeLon, closeLat);
	}

	public int getTileSize(int layerID) {
		return layers[layerID].getTileSize();
	}

	/** @param layers the levels to set */
	public void setLevels(Layer[] layers) {
		this.layers = layers;
	}
	/** @return the levels */
	public int getNumLayers() {
		return layers.length;
	}
	/** @return the levels */
	public Layer getLayer(int layerID) {
		return layers[layerID];
	}
}
