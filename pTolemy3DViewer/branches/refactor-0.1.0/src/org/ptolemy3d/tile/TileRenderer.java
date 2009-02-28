package org.ptolemy3d.tile;

public interface TileRenderer {
	public void drawSubsection(Tile tile, int x1, int z1, int x2, int z2)
			throws Exception;
}
