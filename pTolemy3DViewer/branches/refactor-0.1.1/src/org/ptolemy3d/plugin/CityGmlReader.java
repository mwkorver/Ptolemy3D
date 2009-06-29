package org.ptolemy3d.plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.citygml4j.CityGMLContext;
import org.citygml4j.factory.CityGMLFactory;
import org.citygml4j.model.citygml.CityGMLClass;
import org.citygml4j.model.citygml.building.Building;
import org.citygml4j.model.citygml.core.CityModel;
import org.citygml4j.model.citygml.core.CityObject;
import org.citygml4j.model.citygml.core.CityObjectMember;
import org.citygml4j.model.gml.AbstractRing;
import org.citygml4j.model.gml.AbstractSolid;
import org.citygml4j.model.gml.AbstractSurface;
import org.citygml4j.model.gml.CompositeSolid;
import org.citygml4j.model.gml.CompositeSurface;
import org.citygml4j.model.gml.GMLClass;
import org.citygml4j.model.gml.LinearRing;
import org.citygml4j.model.gml.MultiSurface;
import org.citygml4j.model.gml.MultiSurfaceProperty;
import org.citygml4j.model.gml.Polygon;
import org.citygml4j.model.gml.Solid;
import org.citygml4j.model.gml.SolidProperty;
import org.citygml4j.model.gml.SurfaceProperty;
import org.ptolemy3d.Unit;
import org.ptolemy3d.view.Position;

/**
 * Reads the content of a CityGML document.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 * 
 */
public class CityGmlReader {

	private ArrayList<CityGmlBuildingData> listBuildindData = new ArrayList<CityGmlBuildingData>();

	public ArrayList<CityGmlBuildingData> getBuildingData() {
		return listBuildindData;
	}

	/**
	 * Load GML file.
	 * 
	 * @param url
	 * @throws JAXBException
	 */
	public void loadGML(URL url) throws JAXBException {

		// Set up citygml4j context
		CityGMLContext ctx = new CityGMLContext();
		CityGMLFactory citygml = ctx.createCityGMLFactory();

		// Initialize JAXBContext environment for reading CityGML datasets
		JAXBContext jaxbCtx = ctx.createJAXBContext();
		Unmarshaller um = jaxbCtx.createUnmarshaller();

		// Read the CityGML dataset and unmarshal it into a JAXBElement instance
		JAXBElement<?> featureElem = (JAXBElement<?>) um.unmarshal(url);

		// Map the JAXBElement class to the citygml4j object model
		CityModel cityModel = (CityModel) citygml.jaxb2cityGML(featureElem);

		// Read all Building elements in the model
		for (CityObjectMember cityObjectMember : cityModel
				.getCityObjectMember()) {
			CityObject cityObject = cityObjectMember.getCityObject();

			// If object is a building
			if (cityObject.getCityGMLClass() == CityGMLClass.BUILDING) {
				Building building = (Building) cityObject;

				// Read the building.
				CityGmlBuildingData buildingData = loadBuilding(building);
				listBuildindData.add(buildingData);
			}
		}

		// Convert all building surfaces into positions to be represented by
		// pTolemy
		for (CityGmlBuildingData buildingData : listBuildindData) {

			// Convert LOD1 surfaces to polygon points
			List<Polygon> listSurfaces = buildingData.getLod1Surfaces();
			for (Polygon polygon : listSurfaces) {
				AbstractRing ring = polygon.getExterior().getRing();
				if (ring instanceof LinearRing) {
					LinearRing linRing = (LinearRing) ring;
					List<Double> posValues = linRing.getPosList().getValue();

					CityPolygon cityPolygon = new CityPolygon();
					for (int j = 0; j < posValues.size(); j++) {
						// TODO - Supose the polygon as dimension 3.
						Double d1 = posValues.get(j++);
						Double d2 = posValues.get(j++);
						Double d3 = posValues.get(j);
						cityPolygon.addPosition(new Position(d1
								* Unit.getDDFactor(), d2 * Unit.getDDFactor(),
								d3 * Unit.getCoordSystemRatio()));
					}
					buildingData.addLod1Polygon(cityPolygon);
				}
			}

			// Convert LOD2 surfaces to polygon points
			listSurfaces = buildingData.getLod2Surfaces();
			for (Polygon polygon : listSurfaces) {
				AbstractRing ring = polygon.getExterior().getRing();
				if (ring instanceof LinearRing) {
					LinearRing linRing = (LinearRing) ring;
					List<Double> posValues = linRing.getPosList().getValue();

					CityPolygon cityPolygon = new CityPolygon();
					for (int j = 0; j < posValues.size(); j++) {
						// TODO - Supose the polygon as dimension 3.
						Double d1 = posValues.get(j++);
						Double d2 = posValues.get(j++);
						Double d3 = posValues.get(j);
						Position position = new Position(d1
								* Unit.getDDFactor(), d2 * Unit.getDDFactor(),
								d3 * Unit.getCoordSystemRatio());
						cityPolygon.addPosition(position);
					}
					buildingData.addLod2Polygon(cityPolygon);
				}
			}
		}
	}

	/**
	 * Reads a Building element. Read Lod1MultiSurface, Lod1Solid
	 * 
	 * @param building
	 */
	private CityGmlBuildingData loadBuilding(Building building) {

		String id = building.getId();
		if (id == null || id.equals("")) {
			Logger.getLogger(CityGmlPlugin.class.getName()).warning(
					"Ignored building, no ID found.");
			return null;
		}

		CityGmlBuildingData buildingData = new CityGmlBuildingData();
		buildingData.setId(id);

		// Get possible Lod1MultiSurface content
		MultiSurfaceProperty multiSurfaceProperty = building
				.getLod1MultiSurface();
		if (multiSurfaceProperty != null
				&& multiSurfaceProperty.getMultiSurface() != null) {
			MultiSurface multiSurface = multiSurfaceProperty.getMultiSurface();
			if (multiSurface != null) {
				List<Polygon> listSurfaces = loadMultiSurface(multiSurface);
				buildingData.addLod1Surfaces(listSurfaces);
			}
		}
		// Get possible Lod1Solid content
		SolidProperty splod1 = building.getLod1Solid();
		if (splod1 != null && splod1.getSolid() != null) {
			AbstractSolid abstractSolid = splod1.getSolid();
			List<Polygon> listSurfaces = loadAbstractSolid(abstractSolid);
			buildingData.addLod1Surfaces(listSurfaces);
		}

		// Get possible Lod2MultiSurface content
		MultiSurfaceProperty multi2SurfaceProperty = building
				.getLod2MultiSurface();
		if (multi2SurfaceProperty != null
				&& multi2SurfaceProperty.getMultiSurface() != null) {
			MultiSurface multiSurface = multi2SurfaceProperty.getMultiSurface();
			if (multiSurface != null) {
				List<Polygon> listSurfaces = loadMultiSurface(multiSurface);
				buildingData.addLod2Surfaces(listSurfaces);
			}
		}
		// Get possible Lod2Solid content
		SolidProperty splod2 = building.getLod2Solid();
		if (splod2 != null && splod2.getSolid() != null) {
			AbstractSolid abstractSolid = splod2.getSolid();
			List<Polygon> listSurfaces = loadAbstractSolid(abstractSolid);
			buildingData.addLod2Surfaces(listSurfaces);
		}

		return buildingData;
	}

	/**
	 * Read a MultiSurface element and returns a List with all Surfaces
	 * (recursive read).
	 * 
	 * @param multiSurface
	 * @return
	 */
	private List<Polygon> loadMultiSurface(MultiSurface multiSurface) {

		List<Polygon> listSurfaces = new ArrayList<Polygon>();

		List<SurfaceProperty> listSurfaceProperty = multiSurface
				.getSurfaceMember();
		for (SurfaceProperty surfaceProperty : listSurfaceProperty) {
			AbstractSurface abstractSurface = surfaceProperty.getSurface();
			List<Polygon> surfaces = loadAbstractSurface(abstractSurface);
			listSurfaces.addAll(surfaces);
		}

		return listSurfaces;
	}

	/**
	 * Read a CompositeSurface element and returns a List with all Surfaces
	 * (recursive read).
	 * 
	 * @param compositeSurface
	 * @return
	 */
	private List<Polygon> loadCompositeSurface(CompositeSurface compositeSurface) {

		List<Polygon> listSurfaces = new ArrayList<Polygon>();

		List<SurfaceProperty> listSurfaceProperty = compositeSurface
				.getSurfaceMember();
		for (SurfaceProperty surfaceProperty : listSurfaceProperty) {
			AbstractSurface abstractSurface = surfaceProperty.getSurface();
			List<Polygon> surfaces = loadAbstractSurface(abstractSurface);
			listSurfaces.addAll(surfaces);
		}

		return listSurfaces;
	}

	/**
	 * Reads an AbstractSurface taking care if it is a Surface or a
	 * MultiSurface.
	 * 
	 * @param abstractSurface
	 * @return
	 */
	private List<Polygon> loadAbstractSurface(AbstractSurface abstractSurface) {

		List<Polygon> listSurfaces = new ArrayList<Polygon>();

		if (abstractSurface.getGMLClass() == GMLClass.POLYGON) {
			listSurfaces.add((Polygon) abstractSurface);
		}
		if (abstractSurface.getGMLClass() == GMLClass.MULTISURFACE) {
			List<Polygon> surfaces = loadMultiSurface((MultiSurface) abstractSurface);
			listSurfaces.addAll(surfaces);
		}
		if (abstractSurface.getGMLClass() == GMLClass.COMPOSITESURFACE) {
			List<Polygon> surfaces = loadCompositeSurface((CompositeSurface) abstractSurface);
			listSurfaces.addAll(surfaces);
		}

		return listSurfaces;
	}

	/**
	 * Reads an AbstractSolid taking care if it is a SOLID or COMPOSITESOLID
	 * 
	 * @param abstractSolid
	 * @return
	 */
	private List<Polygon> loadAbstractSolid(AbstractSolid abstractSolid) {

		List<Polygon> listSurfaces = new ArrayList<Polygon>();

		if (abstractSolid.getGMLClass() == GMLClass.SOLID) {
			Solid solid = (Solid) abstractSolid;
			SurfaceProperty surfaceProperty = solid.getExterior();

			if (surfaceProperty != null) {
				AbstractSurface abstractSurface = surfaceProperty.getSurface();
				if (abstractSurface != null) {
					List<Polygon> surfaces = loadAbstractSurface(abstractSurface);
					listSurfaces.addAll(surfaces);
				}
			}
		}

		if (abstractSolid.getGMLClass() == GMLClass.COMPOSITESOLID) {
			CompositeSolid compositeSolid = (CompositeSolid) abstractSolid;
			List<Polygon> surfaces = loadAbstractSolid(compositeSolid);
			listSurfaces.addAll(surfaces);
		}

		return listSurfaces;
	}
}
