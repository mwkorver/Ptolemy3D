/**
 * Ptolemy3D - a Java-based 3D Viewer for GeoWeb applications.
 * Copyright (C) 2008 Mark W. Korver
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ptolemy3d.globe;

import static org.ptolemy3d.debug.Config.DEBUG;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.ProfilerUtil;
import org.ptolemy3d.manager.MapDataManager;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;

/**
 * A tile.
 */
public class Tile {
    /* Variable updated by setTile */
    private final int tileID;
	private int levelID;
    private int upLeftLat;
    private int upLeftLon;
    private int lowRightLat;
    private int lowRightLon;
    private int fmz;
    private int fmx;

	/* TexTile renderer */
//	private static final TileRenderer renderer = new TileDefaultRenderer();					//First clean version
//	private static final TileRenderer renderer = new TileDirectModeRenderer();				//CPU Optimizations
	private static final TileRenderer renderer = new TileDirectModeRenderer_MathLookUp();	//Cos/Sin table lookup
	protected interface TileRenderer { public void drawSubsection(Tile tile, MapData mapData, int x1, int z1, int x2, int z2) throws Exception; }
	
    /* Variable updated by Landscape */
    /** Tile status (for visibility, ...) */
    public boolean status;
    public MapData mapData;

    /* Temporary datas for display */
    protected GL gl;
    protected int drawZlevel;
    protected boolean texture;
    protected Tile left, above, right, below;

	protected Tile(int tileID) {
		this.tileID = tileID;
	}

    public void setTile(Layer level, int lon, int lat) {
        this.upLeftLon = lon;
        this.upLeftLat = lat;
        this.lowRightLon = lon + level.getTileSize();
        this.lowRightLat = lat + level.getTileSize();
        this.levelID = level.getLevelID();

        this.fmx = lon;
        this.fmz = -lat;

        this.status = true;
        
        final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();
    	this.mapData = mapDataManager.getMapDataHeader(level.getLevelID(), level.getTileSize(), fmx, fmz);
    }

    protected void processVisibility() {
        // status =
        // Math3D.isPointInView(upLeftLon, upLeftLat) &&
        // Math3D.isPointInView(lowRightLon, upLeftLat) &&
        // Math3D.isPointInView(upLeftLon, lowRightLat) &&
        // Math3D.isPointInView(lowRightLon, lowRightLat);
    }

    protected void display(GL gl, Tile rightTile,
                           Tile belowTile, Tile leftTile, Tile aboveTile) throws Exception {
        if (!status) {
            return;
        }

        if (DEBUG) {
            ProfilerUtil.tileCounter++;
        }

    	final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();
    	int textureID = mapDataManager.getTextureID(gl, mapData);
        gl.glBindTexture(GL.GL_TEXTURE_2D, textureID);
    	
        final Landscape landscape = Ptolemy3D.getScene().landscape;
        final Layer[] levels = landscape.getLayers();

        this.gl = gl;
        this.texture = (textureID != 0);
        this.right = rightTile;
        this.below = belowTile;
        this.left = leftTile;
        this.above = aboveTile;

        if (!texture) {
            final float[] tileColor = landscape.getTileColor();
            gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
        }

        // look for tile header to match coords
        int ulx, ulz, lrx, lrz;
        ulx = upLeftLon;
        ulz = upLeftLat;
        lrx = lowRightLon;
        lrz = lowRightLat;
        {
            if (ulx < -landscape.getMaxLongitude()) {
                ulx = -landscape.getMaxLongitude();
            }
            if (lrx > landscape.getMaxLongitude()) {
                lrx = landscape.getMaxLongitude();
            }
            if (ulz < -landscape.getMaxLatitude()) {
                ulz = -landscape.getMaxLatitude();
            }
            if (lrz > landscape.getMaxLatitude()) {
                lrz = landscape.getMaxLatitude();
            }
        }

        drawZlevel = levelID;
        if (mapData.key.level != levelID) {
            final int tileWidth = levels[mapData.key.level].getTileSize();

            upLeftLon = mapData.getLon() - Unit.getMeterX();
            upLeftLat = Unit.getMeterZ() - mapData.getLat();
            lowRightLon = upLeftLon + tileWidth;
            lowRightLat = upLeftLat + tileWidth;
            drawZlevel = mapData.key.level;
        }
        if (DEBUG) {
            if (ProfilerUtil.forceLevel != -1 && ProfilerUtil.forceLevel != drawZlevel) {
                return;
            }
        }

        if (levelID < /* != */ (levels.length - 1)) {
            final int below = levelID + 1;
            final Layer levelBelow = levels[below];

            if (levelBelow.isVisible()) {
                int c_ulx = levelBelow.getUpLeftLon();
                int c_ulz = levelBelow.getUpLeftLat();
                int c_lrx = levelBelow.getLowRightLon();
                int c_lrz = levelBelow.getLowRightLat();

                if ((c_lrz < landscape.getMaxLatitude()) && (c_lrz > -landscape.getMaxLatitude())) {
                    if (!((lrx <= c_ulx) || (ulx >= c_lrx) || (lrz <= c_ulz) || (ulz >= c_lrz))) {
                        displayClipped(mapData, ulx, ulz, lrx, lrz, c_ulx, c_ulz, c_lrx,
                                       c_lrz);
                        return;
                    }

                    int maxx = (landscape.getMaxLongitude() / levelBelow.getTileSize()) * levelBelow.getTileSize();
                    if (maxx != landscape.getMaxLongitude()) {
                        maxx += levelBelow.getTileSize();
                    }

                    if (c_lrx > maxx) {
                        int t_past = Layer.LEVEL_NUMTILE_LON - ((landscape.getMaxLongitude() - c_ulx) / levelBelow.getTileSize()) - 1;
                        c_ulx = -maxx;
                        c_lrx = c_ulx + (t_past * levelBelow.getTileSize());

                        if (!((lrx <= c_ulx) || (ulx >= c_lrx) || (lrz <= c_ulz) || (ulz >= c_lrz))) {
                            // if (!((lrx <= ulx) || (ulx >= c_lrx) || (lrz <=
                            // c_ulz) || (ulz >= c_lrz))) { //Old (replace with
                            // above line 08-2008)
                            displayClipped(mapData, ulx, ulz, lrx, lrz, c_ulx, c_ulz,
                                           c_lrx, c_lrz);
                            return;
                        }
                    }
                }
            }
        }
        renderer.drawSubsection(this, mapData, ulx, ulz, lrx, lrz);
    }

    private void displayClipped(MapData mapData, int ulx, int ulz, int lrx, int lrz, int c_ulx,
                                int c_ulz, int c_lrx, int c_lrz) throws Exception {
        boolean ul, ur, ll, lr;

        ul = inBounds(ulx, ulz, c_ulx, c_ulz, c_lrx, c_lrz);
        ur = inBounds(lrx, ulz, c_ulx, c_ulz, c_lrx, c_lrz);
        ll = inBounds(ulx, lrz, c_ulx, c_ulz, c_lrx, c_lrz);
        lr = inBounds(lrx, lrz, c_ulx, c_ulz, c_lrx, c_lrz);

        if (ul && ur && ll && lr) {
            return;
        }
        else if (ul && ur) {
            renderer.drawSubsection(this, mapData, ulx, c_lrz, lrx, lrz);
        }
        else if (ur && lr) {
            renderer.drawSubsection(this, mapData, ulx, ulz, c_ulx, lrz);
        }
        else if (ll && lr) {
            renderer.drawSubsection(this, mapData, ulx, ulz, lrx, c_ulz);
        }
        else if (ul && ll) {
            renderer.drawSubsection(this, mapData, c_lrx, ulz, lrx, lrz);
        }
        else if (ul) {
            renderer.drawSubsection(this, mapData, c_lrx, ulz, lrx, c_lrz);
            renderer.drawSubsection(this, mapData, ulx, c_lrz, lrx, lrz);
        }
        else if (ur) {
            renderer.drawSubsection(this, mapData, ulx, ulz, c_ulx, c_lrz);
            renderer.drawSubsection(this, mapData, ulx, c_lrz, lrx, lrz);
        }
        else if (ll) {
            renderer.drawSubsection(this, mapData, ulx, ulz, lrx, c_ulz);
            renderer.drawSubsection(this, mapData, c_lrx, c_ulz, lrx, lrz);
        }
        else if (lr) {
            renderer.drawSubsection(this, mapData, ulx, ulz, lrx, c_ulz);
            renderer.drawSubsection(this, mapData, ulx, c_ulz, c_ulx, lrz);
        }
        else {
            renderer.drawSubsection(this, mapData, ulx, ulz, lrx, c_ulz);
            renderer.drawSubsection(this, mapData, ulx, c_lrz, lrx, lrz);
            renderer.drawSubsection(this, mapData, ulx, c_ulz, c_ulx, c_lrz);
            renderer.drawSubsection(this, mapData, c_lrx, c_ulz, lrx, c_lrz);
        }
    }

    private final boolean inBounds(int x, int z, int c_ulx, int c_ulz,
                                   int c_lrx, int c_lrz) {
        if ((x >= c_ulx) && (x <= c_lrx) && (z >= c_ulz) && (z <= c_lrz)) {
            return true;
        }
        else {
            return false;
        }
    }

    /** Tile Picking */
    protected boolean pick(double[] intersectPoint, double[][] ray) {
        if (mapData.key.level != levelID) {
            return false;
        }

        final Landscape landscape = Ptolemy3D.getScene().landscape;
        final double terrainScaler = landscape.getTerrainScaler();

        intersectPoint[0] = intersectPoint[1] = intersectPoint[2] = -999;

        int x1 = upLeftLon;
        int z1 = upLeftLat;
        int x2 = lowRightLon;
        int z2 = lowRightLat;

        boolean useDem, useTin;
        {
            boolean b = landscape.isTerrainEnabled() && (mapData != null);
            useDem = (b && (mapData.dem != null)) ? true : false;
            useTin = (b && (mapData.tin != null)) ? true : false;
        }

        double theta1, dTetaOverN;
        double phi1, dPhiOverN;
        {
            double theta2, phi2;
            double oneOverDDToRad;

            oneOverDDToRad = Math3D.DEGREE_TO_RADIAN / Unit.getDDFactor();

            theta1 = (x1 + landscape.getMaxLongitude()) * oneOverDDToRad;
            theta2 = (x2 + landscape.getMaxLongitude()) * oneOverDDToRad;

            phi1 = z1 * oneOverDDToRad;
            phi2 = z2 * oneOverDDToRad;

            dTetaOverN = (theta2 - theta1); // To be divieded by n later
            dPhiOverN = (phi2 - phi1);
        }

        if (!useTin && !useDem) {
            int x_w = (x2 - x1);
            int z_w = (z2 - z1);

            // texture ratios
            int n = (int) (((double) ((x_w > z_w) ? x_w : z_w) / (double) landscape.getMaxLongitude()) * 50);// precision, soon to be dem...
            if (n <= 2) {
                n = 4;
            }

            {
                dTetaOverN /= n;
                dPhiOverN /= n;
            }

            double t1 = phi1;
            for (int j = 0; j < n; j++) {
                final double t2 = t1 + dPhiOverN;

                final double cosT1_E = Math.cos(t1) * Unit.EARTH_RADIUS;
                final double sinT1_E = -Math.sin(t1) * Unit.EARTH_RADIUS;
                final double cosT2_E = Math.cos(t2) * Unit.EARTH_RADIUS;
                final double sinT2_E = -Math.sin(t2) * Unit.EARTH_RADIUS;

                double t3 = theta1;
                for (int i = 0; i <= n; i++) {
                    double cx1, cy1, cz1, cx2, cy2, cz2;
                    {
                        final double cosT3 = Math.cos(t3);
                        final double sinT3 = Math.sin(t3);

                        cx1 = cosT1_E * sinT3;
                        cx2 = cosT2_E * sinT3;
                        cy1 = sinT1_E;
                        cy2 = sinT2_E;
                        cz1 = cosT1_E * cosT3;
                        cz2 = cosT2_E * cosT3;
                    }

                    if (i > 0) {
                        Math3D.setTriVert(2, cx1, cy1, cz1);
                        if (Math3D.pickTri(intersectPoint, ray)) {
                            return true;
                        }
                        Math3D.setTriVert(2, cx2, cy2, cz2);
                        if (Math3D.pickTri(intersectPoint, ray)) {
                            return true;
                        }
                    }
                    else {
                        Math3D.setTriVert(0, cx1, cy1, cz1);
                        Math3D.setTriVert(1, cx2, cy2, cz2);
                    }

                    t3 += dTetaOverN;
                }
            }

        }
        else if (useTin) {
            {
                double d = 1.0 / mapData.tin.w;
                dTetaOverN /= d;
                dPhiOverN /= d;
            }
            double scaler = Unit.getCoordSystemRatio() * terrainScaler;

            for (int i = 0; i < mapData.tin.nSt.length; i++) {
                for (int j = 0; j < mapData.tin.nSt[i].length; j++) {
                    double dx, dz, dy;
                    {
                        final int v = mapData.tin.nSt[i][j];
                        final float[] p = mapData.tin.p[v];

                        dx = theta1 + p[0] * dTetaOverN;
                        dy = p[1] * scaler;
                        dz = phi1 + p[2] * dPhiOverN;
                    }

                    double tx, ty, tz;
                    {
                        double dyE = Unit.EARTH_RADIUS + dy;
                        double sinZ = Math.sin(dz);
                        double cosZ = Math.cos(dz);
                        double sinX = Math.sin(dx);
                        double cosX = Math.cos(dx);

                        tx = dyE * cosZ * sinX;
                        ty = -dyE * sinZ;
                        tz = dyE * cosZ * cosX;
                    }

                    // pick
                    if (j < 2) {
                        Math3D.setTriVert(j, tx, ty, tz);
                    }
                    else {
                        Math3D.setTriVert(2, tx, ty, tz);
                        if (Math3D.pickTri(intersectPoint, ray)) {
                            return true;
                        }
                    }
                }
            }
        }
        else if (useDem) {
            final byte[] dem = mapData.dem.demDatas;

            final int row_width = (int) Math.sqrt((dem.length / 2)) * 2; // assuming
            // we
            // have
            // a
            // square
            // tile
            final int numrows = row_width / 2;

            int startx = 0;
            int startz = 0;
            int endx = numrows - 1;
            int endz = numrows - 1;

            int nrows_x = endx - startx;
            int nrows_z = endz - startz;

            {
                dTetaOverN /= nrows_x;
                dPhiOverN /= nrows_z;
            }

            double scaler = Unit.getCoordSystemRatio() * terrainScaler;

            double dz = phi1;
            int pos_d1 = (startz * row_width) + (startx * 2);
            for (int i = startz; i < endz; i++) {
                double d2z = dz + dPhiOverN;

                final double cosZ = Math.cos(dz);
                final double cos2Z = Math.cos(d2z);
                final double sinZ = Math.sin(dz);
                final double sin2Z = Math.sin(d2z);

                int pos_d1_save = pos_d1; // int pos_d1 = (i * rowWidth) +
                // (startx * 2);
                double dx = theta1;
                for (int j = startx; j < endx; j++) {
                    int pos_d2 = pos_d1 + row_width;

                    double dy1 = ((dem[pos_d1] << 8) + (dem[pos_d1 + 1] & 0xFF)) * scaler;
                    double dy2 = ((dem[pos_d2] << 8) + (dem[pos_d2 + 1] & 0xFF)) * scaler;

                    double cx1, cy1, cz1, cx2, cy2, cz2;
                    {
                        final double dy1E = Unit.EARTH_RADIUS + dy1;
                        final double dy2E = Unit.EARTH_RADIUS + dy2;

                        final double cosX = Math.cos(dx);
                        final double sinX = Math.sin(dx);

                        cx1 = dy1E * cosZ * sinX;
                        cx2 = dy2E * cos2Z * sinX;
                        cy1 = -dy1E * sinZ;
                        cy2 = -dy2E * sin2Z;
                        cz1 = dy1E * cosZ * cosX;
                        cz2 = dy2E * cos2Z * cosX;
                    }

                    if (j > startx) {
                        Math3D.setTriVert(2, cx1, cy1, cz1);
                        if (Math3D.pickTri(intersectPoint, ray)) {
                            return true;
                        }

                        Math3D.setTriVert(2, cx2, cy2, cz2);
                        if (Math3D.pickTri(intersectPoint, ray)) {
                            return true;
                        }
                    }
                    else {
                        Math3D.setTriVert(0, cx1, cy1, cz1);
                        Math3D.setTriVert(1, cx2, cy2, cz2);
                    }

                    pos_d1 += 2;
                    dx += dTetaOverN;
                }

                dz += dPhiOverN;
                pos_d1 = pos_d1_save + row_width;
            }
        }
        else {
            Math3D.setTriVert(0, x1, 0, z1);
            Math3D.setTriVert(1, x1, 0, z2);
            Math3D.setTriVert(2, x2, 0, z1);

            if (Math3D.pickTri(intersectPoint, ray)) {
                return true;
            }
            Math3D.setTriVert(2, x2, 0, z2);
            if (Math3D.pickTri(intersectPoint, ray)) {
                return true;
            }
        }
        return false;
    }

    // Getters
    
    /**
     * Tile ID
     * @return the tile id (first is 0)
     */
    public int getTileID() {
        return tileID;
    }
    
    /**
     * Level ID
     * @return the level id (first is 0)
     */
    public int getLevelID() {
        return levelID;
    }

    /**
     * Upper left corner longitude
     * @return the upper left longitude
     */
    public int getUpLeftLon() {
        return upLeftLon;
    }

    /**
     * Upper left corner latitude
     * @return the upper left latitude
     */
    public int getUpLeftLat() {
        return upLeftLat;
    }

    /**
     * Lower right corner longitude
     * @return the lower right longitude
     */
    public int getLowRightLon() {
        return lowRightLon;
    }

    /**
     * Lower right corner latitude
     * @return the lower right latitude
     */
    public int getLowRightLat() {
        return lowRightLat;
    }

    /** @return the fmx */
    public int getFmx() {
        return fmx;
    }

    /** @return the fmz */
    public int getFmz() {
        return fmz;
    }
}
