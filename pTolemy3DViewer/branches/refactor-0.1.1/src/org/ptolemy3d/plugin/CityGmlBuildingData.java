package org.ptolemy3d.plugin;

import java.util.ArrayList;
import java.util.List;

import org.citygml4j.model.gml.AbstractRing;
import org.citygml4j.model.gml.LinearRing;
import org.citygml4j.model.gml.Polygon;
import org.ptolemy3d.Unit;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.view.Camera;

/**
 * Stores information for one single building object: ID, LOD1, LOD2.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class CityGmlBuildingData {
	private String id = "";
	private List<Polygon> lod1Polygonsurfaces = new ArrayList<Polygon>();
	private CityTriangleList lod1Triangles = null;
	private List<Polygon> lod2PolygonSurfaces = new ArrayList<Polygon>();
	private CityTriangleList lod2Triangles = null;

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

	public boolean addLod1Surfaces(List<Polygon> surfaces) {
		return lod1Polygonsurfaces.addAll(surfaces);
	}

	/**
	 * @return the LOD2 surfaces
	 */
	public List<Polygon> getLod2Surfaces() {
		return lod2PolygonSurfaces;
	}

	public boolean addLod2Surfaces(List<Polygon> surfaces) {
		return lod2PolygonSurfaces.addAll(surfaces);
	}

	public CityTriangleList getLod1Triangles() {
		return lod1Triangles;
	}

	public CityTriangleList getLod2Triangles() {
		return lod2Triangles;
	}

	/**
	 * Convert all building surfaces into vertex that can be rendered by the
	 * viewer. Also computes the normals.
	 */
	public void computeVertexAndNormals() {
		// Convert LOD1 surfaces to polygon points
		{
			List<Polygon> listSurfaces = getLod1Surfaces();
			CityTriangleList polys = computeVertexAndNormals(listSurfaces);
			if (polys != null) {
				lod1Triangles = polys;
			}
			listSurfaces.clear();
		}

		// Convert LOD2 surfaces to polygon points
		{
			List<Polygon> listSurfaces = getLod2Surfaces();
			CityTriangleList polys = computeVertexAndNormals(listSurfaces);
			if (polys != null) {
				lod2Triangles = polys;
			}
			listSurfaces.clear();
		}
	}
	
	private CityTriangleList computeVertexAndNormals(List<Polygon> listSurfaces) {
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
		CityTriangleList cityTriangles = new CityTriangleList(countTris, countVerts);
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

					positions[j] = d;
				}
				
				// Triangle indices
				final int[][] indices = concavePolygonToTriangles(positions);

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
	
	/*
	 * Alternative algorithm (to avoid the copyright underneath) :
	 * http://www.geometrictools.com/Documentation/TriangulationByEarClipping.pdf
	 */
	
	/*
	 * http://www.gamedev.net/reference/articles/article425.asp
	 * 
	 * poly_tri.c
	 *
	 * Program to take a polygon definition and convert it into triangles
	 * that may then be rendered by the standard triangle rendering
	 * algorithms.  This assumes all transformations have been performed
	 * already and cuts them up into optimal triangles based on their
	 * screen-space representation.
	 *
	 *      Copyright (c) 1988, Evans & Sutherland Computer Corporation
	 *
	 * Permission to use all or part of this program without fee is
	 * granted provided that it is not used or distributed for direct
	 * commercial gain, the above copyright notice appears, and
	 * notice is given that use is by permission of Evans & Sutherland
	 * Computer Corporation.
	 *
	 *      Written by Reid Judd and Scott R. Nelson at
	 *      Evans & Sutherland Computer Corporation (January, 1988)
	 *
	 * To use this program, either write your own "draw_triangle" routine
	 * that can draw triangles from the definitions below, or modify the
	 * code to call your own triangle or polygon rendering code.  Call
	 * "draw_poly" from your main program.
	 */
	public int[][] concavePolygonToTriangles(Vector3d[] v) {
		int n = v.length;
		
		/* Pointers to vertices still left */
		int[] vp = new int[n];
		for (int i = 0; i < n; i++) {
			vp[i] = i;
		}

		int[][] tris = new int[(n - 2)][3];
		int triID = 0;
		
		/* Slice off clean triangles until nothing remains */
		int count = n;
		while (count > 3) {
			double min_dist = Float.MAX_VALUE; /* Minimum distance so far */
			int min_vert = 0;                  /* Vertex with minimum distance */
			
			int prev, cur, next;               /* Three points currently being considered */
			for (cur = 0; cur < count; cur++) {
				prev = cur - 1;
				next = cur + 1;
				if (cur == 0) {                /* Wrap around on the ends */
					prev = count - 1;
				} else if (cur == count - 1) {
					next = 0;
				}
				/* Pick out shortest distance that forms a good triangle */
				double dist;
				if ((determinant(vp[prev], vp[cur], vp[next], v) == COUNTER_CLOCKWISE)
					/* Same orientation as polygon */
					&& no_interior(vp[prev], vp[cur], vp[next], v, vp, count, COUNTER_CLOCKWISE)
					/* No points inside */
					&& ((dist = distance2(v[vp[prev]].x, v[vp[prev]].y, v[vp[next]].x, v[vp[next]].y ))
								< min_dist)) {
					min_dist = dist;
					min_vert = cur;
				}
			}

			prev = min_vert - 1;
			next = min_vert + 1;
			
			if (min_vert == 0) {
				/* Wrap around on the ends */
				prev = count - 1;
			}
			else if (min_vert == count - 1) {
				next = 0;
			}

			/* Output this triangle */
			tris[triID][0] = vp[prev];
			tris[triID][1] = vp[min_vert];
			tris[triID][2] = vp[next];
			triID++;
			
			/* Remove the triangle from the polygon */
			count -= 1;
			for (int i = min_vert; i < count; i++) {
				vp[i] = vp[i+1];
			}
		}

		/* Output the final triangle */
		tris[triID][0] = vp[0];
		tris[triID][1] = vp[1];
		tris[triID][2] = vp[2];
		triID++;
		
		return tris;
	}
	final static int COUNTER_CLOCKWISE = 0;
	final static int CLOCKWISE = 1;
	/*
	 * @param p1, p2, p3 The vertices to consider
	 * @param v The vertex list
	 */
	private int determinant(int p1, int p2, int p3, Vector3d[] v) {
	    double x1, x2, x3, y1, y2, y3;
	    x1 = v[p1].x;
	    y1 = v[p1].y;
	    x2 = v[p2].x;
	    y2 = v[p2].y;
	    x3 = v[p3].x;
	    y3 = v[p3].y;
	
	    double determ = (x2 - x1) * (y3 - y1) - (x3 - x1) * (y2 - y1);
	    if (determ >= 0.0) {
	        return COUNTER_CLOCKWISE;
	    }
	    else {
	        return CLOCKWISE;
	    }
	}
	private double distance2(double x1, double y1, double x2, double y2) {
		final double xd = x1 - x2;
		final double yd = y1 - y2;
		return xd * xd + yd * yd;
	}
	/*
	 * @param p1, p2, p3 The vertices to consider
	 * @param v The vertex list
	 * @param vp The vertex pointers (which are left)
	 * @param n Number of vertices
	 * @param poly_or Polygon orientation
	 */
	private boolean no_interior(int p1, int p2, int p3, Vector3d[] v,int vp[], int n, int poly_or) {
	    for (int i = 0; i < n; i++) {
	    	int  p = vp[i];         /* The point to test */
	        if ((p == p1) || (p == p2) || (p == p3))
	            continue;           /* Don't bother checking against yourself */
	        if (   (determinant( p2, p1, p, v ) == poly_or)
	            || (determinant( p1, p3, p, v ) == poly_or)
	            || (determinant( p3, p2, p, v ) == poly_or) ) {
	            continue;           /* This point is outside */
	        } else {
	            return false;       /* The point is inside */
	        }
	    }
	    return true;                /* No points inside this triangle */
	}
}
