package org.ptolemy3d.plugin;

import java.nio.DoubleBuffer;
import java.util.ArrayList;

import org.ptolemy3d.view.Position;

import com.sun.opengl.util.BufferUtil;

/**
 * Stores a list of Positions that composes the Polygon. Also stores the double
 * values that forms the vertex to render every polygon. The vertex array must
 * be initialized and filled in the rendering loop.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 */
public class CityPolygon {
	private ArrayList<Position> positions = new ArrayList<Position>();
	private DoubleBuffer vertex = null;

	public boolean addPosition(Position position) {
		return positions.add(position);
	}

	public ArrayList<Position> getPositions() {
		return positions;
	}

	public void initVertexArray(int size) {
		vertex = BufferUtil.newDoubleBuffer(size);
	}

	public void addVertex(double v) {
		vertex.put(v);
	}

	public DoubleBuffer getVertex() {
		return vertex;
	}
}
