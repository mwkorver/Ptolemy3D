package org.ptolemy3d.plugin;

import java.util.ArrayList;
import java.util.List;

import org.citygml4j.model.gml.Polygon;

/**
 * Stores information for one single building object: ID, LOD1, LOD2.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class CityGmlBuilding {
	private String id = "";
	private List<Polygon> lod1Polygonsurfaces = new ArrayList<Polygon>();
	private List<Polygon> lod2PolygonSurfaces = new ArrayList<Polygon>();

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
	public List<Polygon> getLod1Surfaces() {
		return lod1Polygonsurfaces;
	}

	public boolean addLod1Surface(List<Polygon> surfaces) {
		return lod1Polygonsurfaces.addAll(surfaces);
	}

	/**
	 * @return the LOD2 surfaces
	 */
	public List<Polygon> getLod2Surfaces() {
		return lod2PolygonSurfaces;
	}

	public boolean addLod2Surface(List<Polygon> surfaces) {
		return lod2PolygonSurfaces.addAll(surfaces);
	}
}
