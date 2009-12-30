package org.ptolemy3d.plugin;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.sun.opengl.util.BufferUtil;

/**
 * Stores a list of Positions that composes the Polygon. Also stores the double
 * values that forms the vertex to render every polygon. The vertex array must
 * be initialized and filled in the rendering loop.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class CityTriangleList {

	private final IntBuffer indices;
	private final DoubleBuffer positions;
	private final FloatBuffer normals;

	/**
	 * Creates a new instance with specified vertex size. Note the size must be
	 * the number of vertices x 3, that is for 4 vertices we must specify a size
	 * of 12.
	 * 
	 * @param size
	 */
	public CityTriangleList(int numTriangles, int numVertices) {
		indices = BufferUtil.newIntBuffer(numTriangles * 3);     //3 vertex index (v1,v2,v3)
		positions = BufferUtil.newDoubleBuffer(numVertices * 3); //3 components (x,y,z)
		normals = BufferUtil.newFloatBuffer(numVertices * 3);    //3 components (x,y,z)
	}

	public void addIndex(int i) {
		indices.put(i);
	}

	public void addPosition(double x, double y, double z) {
		positions.put(x).put(y).put(z);
	}

	public void addNormal(float[] xyz) {
		normals.put(xyz);
	}
	
	public DoubleBuffer getPositions() {
		return positions;
	}

	public IntBuffer getIndices() {
		return indices;
	}

	public FloatBuffer getNormals() {
		return normals;
	}
}
