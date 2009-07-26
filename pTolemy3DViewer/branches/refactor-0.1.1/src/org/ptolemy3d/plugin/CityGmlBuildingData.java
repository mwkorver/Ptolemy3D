package org.ptolemy3d.plugin;

import java.util.ArrayList;
import java.util.List;

import org.citygml4j.model.gml.AbstractRing;
import org.citygml4j.model.gml.LinearRing;
import org.citygml4j.model.gml.Polygon;
import org.ptolemy3d.Unit;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.view.Camera;

/**
 * Stores information for one single building object: ID, LOD1, LOD2.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 */
public class CityGmlBuildingData {
	private String id = "";
	private ArrayList<Polygon> lod1Polygonsurfaces = new ArrayList<Polygon>();
	private ArrayList<CityPolygon> lod1polygons = new ArrayList<CityPolygon>();
	private ArrayList<Polygon> lod2PolygonSurfaces = new ArrayList<Polygon>();
	private ArrayList<CityPolygon> lod2polygons = new ArrayList<CityPolygon>();

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the LOD1 surfaces
	 */
	public ArrayList<Polygon> getLod1Surfaces() {
		return lod1Polygonsurfaces;
	}

	public boolean addLod1Surface(Polygon surface) {
		return lod1Polygonsurfaces.add(surface);
	}

	public boolean addLod1Surfaces(List<Polygon> surfaces) {
		return lod1Polygonsurfaces.addAll(surfaces);
	}

	/**
	 * @return the LOD2 surfaces
	 */
	public ArrayList<Polygon> getLod2Surfaces() {
		return lod2PolygonSurfaces;
	}

	public boolean addLod2Surface(Polygon surface) {
		return lod2PolygonSurfaces.add(surface);
	}

	public boolean addLod2Surfaces(List<Polygon> surfaces) {
		return lod2PolygonSurfaces.addAll(surfaces);
	}

	public boolean addLod1Polygon(CityPolygon polygon) {
		return lod1polygons.add(polygon);
	}

	public ArrayList<CityPolygon> getLod1Polygons() {
		return lod1polygons;
	}

	public boolean addLod2Polygon(CityPolygon polygon) {
		return lod2polygons.add(polygon);
	}

	public ArrayList<CityPolygon> getLod2Polygons() {
		return lod2polygons;
	}

	/**
	 * Convert all building surfaces into vertex that can be rendered by the
	 * viewer. Also computes the normals.
	 */
	public void computeVertexAndNormals() {

		// Convert LOD1 surfaces to polygon points
		List<Polygon> listSurfaces = this.getLod1Surfaces();
		for (Polygon polygon : listSurfaces) {
			AbstractRing ring = polygon.getExterior().getRing();
			if (ring instanceof LinearRing) {
				LinearRing linRing = (LinearRing) ring;
				List<Double> posValues = linRing.getPosList().getValue();

				// Store polygon vertices and compute its normals.
				CityPolygon cityPolygon = new CityPolygon(posValues.size());
				for (int j = 0; j < posValues.size(); j += 3) {
					// TODO - Supose the polygon as dimension 3.
					Double lat = posValues.get(j) * Unit.DEGREE_TO_DD_FACTOR;
					Double lon = posValues.get(j + 1) * Unit.DEGREE_TO_DD_FACTOR;
					Double alt = posValues.get(j + 2) * Unit.getCoordSystemRatio();

					Vector3d d = Camera.computeCartesianPoint(lat, lon, alt);

					cityPolygon.addVertex(d.x, d.y, d.z);
				}
				cityPolygon.computeNormals();

				this.addLod1Polygon(cityPolygon);
			}
		}

		// Convert LOD2 surfaces to polygon points
		listSurfaces = this.getLod2Surfaces();
		for (Polygon polygon : listSurfaces) {
			AbstractRing ring = polygon.getExterior().getRing();
			if (ring instanceof LinearRing) {
				LinearRing linRing = (LinearRing) ring;
				List<Double> posValues = linRing.getPosList().getValue();

				// Store polygon vertices and compute its normals.
				CityPolygon cityPolygon = new CityPolygon(posValues.size());
				for (int j = 0; j < posValues.size(); j += 3) {
					// TODO - Supose the polygon as dimension 3.
					Double lat = posValues.get(j) * Unit.DEGREE_TO_DD_FACTOR;
					Double lon = posValues.get(j + 1) * Unit.DEGREE_TO_DD_FACTOR;
					Double alt = posValues.get(j + 2) * Unit.getCoordSystemRatio();

					Vector3d d = Camera.computeCartesianPoint(lon, lat, alt);

					cityPolygon.addVertex(d.x, d.y, d.z);
				}
				cityPolygon.computeNormals();

				this.addLod2Polygon(cityPolygon);
			}
		}
	}
}
