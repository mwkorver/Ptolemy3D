package org.ptolemy3d.plugin;

import java.util.ArrayList;
import java.util.List;

import org.citygml4j.model.gml.Polygon;

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

	/**
	 * Get positions
	 * 
	 * @param position
	 * @return
	 */
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
}
