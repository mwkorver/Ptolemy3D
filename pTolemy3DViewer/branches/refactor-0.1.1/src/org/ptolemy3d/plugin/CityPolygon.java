package org.ptolemy3d.plugin;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import org.ptolemy3d.math.Math3D;

import com.sun.opengl.util.BufferUtil;

/**
 * Stores a list of Positions that composes the Polygon. Also stores the double
 * values that forms the vertex to render every polygon. The vertex array must
 * be initialized and filled in the rendering loop.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 */
public class CityPolygon {

	private DoubleBuffer vertex = null;
	private FloatBuffer normals = null;

	/**
	 * Creates a new instance with specified vertex size. Note the size must be
	 * the number of vertices x 3, that is for 4 vertices we must specify a size
	 * of 12.
	 * 
	 * @param size
	 */
	public CityPolygon(int size) {
		vertex = BufferUtil.newDoubleBuffer(size);
		normals = BufferUtil.newFloatBuffer(size);
	}

	/**
	 * Compute the normal for every vertex.
	 */
	public void computeNormals() {

		float[] normal = new float[3];
		float[] pf1 = new float[3];
		float[] pf2 = new float[3];
		float[] pf3 = new float[3];
		double[] p = new double[3];

		normals.rewind();
		vertex.rewind();

		int size = vertex.capacity();
		for (int i = 0; i < (size - 1); i += 9) {
			vertex.get(p);
			pf1[0] = (float) p[0];
			pf1[1] = (float) p[1];
			pf1[2] = (float) p[2];

			if (vertex.remaining() == 0) {
				vertex.rewind();
			}
			vertex.get(p);
			pf2[0] = (float) p[0];
			pf2[1] = (float) p[1];
			pf2[2] = (float) p[2];

			if (vertex.remaining() == 0) {
				vertex.rewind();
			}
			vertex.get(p);
			pf3[0] = (float) p[0];
			pf3[1] = (float) p[1];
			pf3[2] = (float) p[2];

			Math3D.computeFaceNormal(pf1, pf2, pf3, normal);
			normals.put(normal);
		}

		normals.rewind();
		vertex.rewind();
	}

	public void addVertex(double v) {
		vertex.put(v);
	}

	public DoubleBuffer getVertex() {
		return vertex;
	}

	public FloatBuffer getNormals() {
		return normals;
	}
}
