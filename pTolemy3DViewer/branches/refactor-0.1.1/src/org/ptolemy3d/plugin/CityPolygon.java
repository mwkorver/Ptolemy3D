package org.ptolemy3d.plugin;

import java.util.ArrayList;

import org.ptolemy3d.view.Position;

public class CityPolygon {
	private ArrayList<Position> positions = new ArrayList<Position>();

	public boolean addPosition(Position position) {
		return positions.add(position);
	}

	public ArrayList<Position> getPositions() {
		return positions;
	}
}
