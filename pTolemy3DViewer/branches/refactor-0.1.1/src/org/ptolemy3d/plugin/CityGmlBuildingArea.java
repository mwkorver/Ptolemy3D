package org.ptolemy3d.plugin;

import java.util.ArrayList;
import java.util.List;

import org.citygml4j.model.gml.AbstractRing;
import org.citygml4j.model.gml.LinearRing;
import org.citygml4j.model.gml.Polygon;
import org.ptolemy3d.Unit;
import org.ptolemy3d.math.ConcavePolygon;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.view.Camera;

/**
 * Stores information for an area of building.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class CityGmlBuildingArea {
	/** List of building */
	private List<CityGmlBuilding> buildings = new ArrayList<CityGmlBuilding>();
	/** Triangle list for rendering LOD1 */
	private TriangleList lod1Triangles = null;
	/** Triangle list for rendering LOD2 */
	private TriangleList lod2Triangles = null;
	/** Box englobing the buildings */
	private double minLat, maxLat, minLon, maxLon, minAlt, maxAlt;

	public CityGmlBuildingArea() {
		minLat =  Double.MAX_VALUE;
		maxLat = -Double.MAX_VALUE;
		minLon =  Double.MAX_VALUE;
		maxLon = -Double.MAX_VALUE;
		minAlt =  Double.MAX_VALUE;
		maxAlt = -Double.MAX_VALUE;
	}

	public void addBuilding(CityGmlBuilding building) {
		buildings.add(building);
	}

	public TriangleList getLod1Geometry() {
		// Convert LOD1 surfaces to polygon points
		if (lod1Triangles == null) {
			List<Polygon> polygons = new ArrayList<Polygon>();
			for(CityGmlBuilding building : buildings) {
				polygons.addAll(building.getLod1Surfaces());
			}
			TriangleList polys = computeVertexAndNormals(polygons);
			if (polys != null) {
				lod1Triangles = polys;
			}
			polygons.clear();
		}
		return lod1Triangles;
	}

	public TriangleList getLod2Geometry() {
		// Convert LOD2 surfaces to polygon points
		if (lod2Triangles == null) {
			List<Polygon> polygons = new ArrayList<Polygon>();
			for(CityGmlBuilding building : buildings) {
				polygons.addAll(building.getLod2Surfaces());
			}
			TriangleList polys = computeVertexAndNormals(polygons);
			if (polys != null) {
				lod2Triangles = polys;
			}
			polygons.clear();
		}
		return lod2Triangles;
	}
	
	/**
	 * Convert all building surfaces into vertex that can be rendered by the
	 * viewer. Also computes the normals.
	 */
	private TriangleList computeVertexAndNormals(List<Polygon> listSurfaces) {
		// TODO - Suppose the polygon vertex as dimension 3.
		final int VERTEX_DIMENSION = 3;
		
		int countTris = 0;
		int countVerts = 0;
		for (Polygon polygon : listSurfaces) {
			AbstractRing ring = polygon.getExterior().getRing();
			if (ring instanceof LinearRing) {
				LinearRing linRing = (LinearRing) ring;
				List<Double> posValues = linRing.getPosList().getValue();
				
				int numVertices = posValues.size() / VERTEX_DIMENSION;
				int numTriangles = numVertices - 2;
				
				countVerts += numVertices;
				countTris += numTriangles;
			}
		}
		if (countTris == 0) {
			return null;
		}
		
		int curIndex = 0;
		TriangleList cityTriangles = new TriangleList(countTris, countVerts);
		for (Polygon polygon : listSurfaces) {
			AbstractRing ring = polygon.getExterior().getRing();
			if (ring instanceof LinearRing) {
				LinearRing linRing = (LinearRing) ring;
				List<Double> posValues = linRing.getPosList().getValue();
				
				int numVertices = posValues.size() / VERTEX_DIMENSION;
				int numTriangles = numVertices - 2;

				// Vertices positions
				Vector3d[] positions = new Vector3d[numVertices];
				for (int j = 0; j < numVertices; j++) {
					int k = (VERTEX_DIMENSION * j);
					
					Double lat = posValues.get(k) * Unit.DEGREE_TO_DD_FACTOR;
					Double lon = posValues.get(k + 1) * Unit.DEGREE_TO_DD_FACTOR;
					Double alt = posValues.get(k + 2) * Unit.getCoordSystemRatio();

					Vector3d d = Camera.computeCartesianPoint(lon, lat, alt);
					
					minLat = Math.min(minLat, lat);
					maxLat = Math.max(maxLat, lat);
					minLon = Math.min(minLon, lon);
					maxLon = Math.max(maxLon, lon);
					minAlt = Math.min(minAlt, alt);
					maxAlt = Math.max(maxAlt, alt);

					positions[j] = d;
				}
				
				// Triangle indices
				final int[][] indices = ConcavePolygon.concavePolygonToTriangles(positions);

				// Vertices normals
				float[][] normals = computeNormals(positions, indices);
				
				// Store indices
				for(int[] tri : indices) {
					int swap = tri[1];
					tri[1] = tri[2];
					tri[2] = swap;
					
					cityTriangles.addIndex(curIndex + tri[0]);
					cityTriangles.addIndex(curIndex + tri[1]);
					cityTriangles.addIndex(curIndex + tri[2]);
				}
				
				// Store vertices positions and normals
				for (int j = 0; j < numVertices; j++) {
					Vector3d v = positions[j];
					float[] n = normals[j];
					cityTriangles.addPosition(v.x, v.y, v.z);
					cityTriangles.addNormal(n);
					curIndex++;
				}
			}
		}
		
		//System.out.println("Tirs/Verts: "+countTris+"/"+countVerts);
		//System.out.println("Area: "+minLat+","+maxLat+","+minLon+","+maxLon+","+minAlt+","+maxAlt);
		
		return cityTriangles;
	}
	
	private float[][] computeNormals(Vector3d[] v, int[][] indices) {
		float[] faceNormal = new float[3];
		float[][] normals = new float[v.length][3];
		
		for(int[] tri : indices) {
			int vtx0 = tri[0];
			int vtx1 = tri[1];
			int vtx2 = tri[2];
			
			Math3D.computeFaceNormal(v[vtx0], v[vtx1], v[vtx2], faceNormal);
			
			set(normals[vtx0], faceNormal);
			set(normals[vtx1], faceNormal);
			set(normals[vtx2], faceNormal);
		}
		return normals;
	}
	public static void set(float[] dest, float[] src) {
		dest[0] = src[0];
		dest[1] = src[1];
		dest[2] = src[2];
	}
}
