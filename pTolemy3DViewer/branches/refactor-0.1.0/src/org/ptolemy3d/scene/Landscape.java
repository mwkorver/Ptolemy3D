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
package org.ptolemy3d.scene;

import java.awt.Color;
import static org.ptolemy3d.debug.Config.DEBUG;
import static org.ptolemy3d.tile.Level.LEVEL_NUMTILE_LON;
import static org.ptolemy3d.tile.Level.LEVEL_NUMTILE_LAT;

import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.debug.ProfilerInterface;
import org.ptolemy3d.tile.Jp2TileLoader;
import org.ptolemy3d.tile.Level;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.CameraMovement;

/**
 * <H1>Overview</H1> <BR>
 * Manage landscape:<BR>
 * <ul>
 * <li>Initialize landscape</li>
 * <li>Load/Unload incoming map in the graphic card.</li>
 * <li>Process landscape visibility and level of details.</li>
 * <li>Render landscape.</li>
 * </ul>
 * <BR>
 * <BR>
 * <H1>Geometry: Level and Tile ?</H1> <BR>
 * Landscape contains the description of the landscape geometry.<BR>
 * <BR>
 * To render accuratly and efficiently the landscape from any view position (far
 * or very close), the landscape geometry has multiple definiton, called
 * <b>level</b>.<BR>
 * Levels goes from the less detailled (for far view) to very detailled geometry
 * (for close or on 'ground' view).<BR>
 * With that design, the landscape can be rendered with multiple level of
 * details for efficiently rendering.<BR>
 * <BR>
 * Each level is divided in tiles, to cull invisible part of the globe
 * accordingly to the view.<BR>
 * A tile is a rectangular piece of the globe. The properties of a tile is:<BR>
 * <ul>
 * <li>Start longitute and latitude</li>
 * <li>End longitute and latitude</li>
 * <li>Image map (texture): image have 4 resolutions and is the globe view at
 * the tile geometry area.</li>
 * <li>Elevation geometry: represent the actual elevation/altitude of the globe
 * ground at the tile geometry area.</li>
 * </ul>
 * <BR>
 * See the <a href=../tile/Level.html>Level documentation</a>, for more
 * information.<BR>
 * See the <a href=../tile/Tile.html>Tile documentation</a>, for more
 * information.<BR>
 * <BR>
 * <BR>
 * <H2>Elevation</H2> <BR>
 * The landscape have a default geometry representing a spherical globe.<BR>
 * Like explain previously, the landscape have additional geometry details to
 * represent the actual globe geometry in the best way possible.<BR>
 * This additional geometry details is represented in the form as ground
 * elevation at each (longitude, latitude) location.<BR>
 * <BR>
 * To obtain the elevation of the ground at a specific location (longitude,
 * latitude), use the method <code>groundHeight</code>.<BR>
 * <BR>
 * <BR>
 * <H2>A descritive example</H2> <BR>
 * Let's see an example describing few of the notions introduced previously.<BR>
 * All screenshots are taken from the same view position, but each with
 * different render mode to illustrate a specific notion.<BR>
 * <BR>
 * From the next screenshot, is a view of the earth from a given location. We
 * will see how this image has been generated.<BR>
 * <div align="center"><img src="../../../screenshot-satelite.png" width=640
 * height=456><BR>
 * <i>Default render mode</i></div><BR>
 * <BR>
 * The next screenshot illustrate the notion of level.<BR>
 * Like introduced previously, a level represents the globe with different
 * details (level of details).<BR>
 * <BR>
 * The first level is 0, and go the number_of_level minus one). The most
 * detailled level is the last level.<BR>
 * A level id is represented in the screenshot with a single color. The area in
 * full green is the level id 1, in full blue is the level id 2 and slight dark
 * red is the level 3.<BR>
 * <BR>
 * In the area close to the view (in red), we can deduce from the next
 * screenshot it is the area the more details. Similarly, the area far to the
 * view (in green) is the area the less details.<BR>
 * This behavior is allow to render very detailled area where needed, and
 * reasonable render speed rendering other area with lower details.<BR>
 * <BR>
 * <div align="center"><img src="../../../screenshot-levelid.png" width=640
 * height=456><BR>
 * <i>Level render mode</i></div><BR>
 * <BR>
 * The next step, after associating level id to area, is to sub-divide level
 * into smaller area called tile.<BR>
 * We'll see that step with the next screenshot.<BR>
 * <BR>
 * A level is sub-divided in a block of 8x8 tiles. This makes 64 tiles per
 * level.<BR>
 * Each tile is represented with rectangular area with a single color.<BR>
 * <BR>
 * You can see from the next screenshot, the red area represented in red on the
 * screenshot above is effectively divided in 8 rows and 8 column (count the
 * smallest rectangles).<BR>
 * <BR>
 * <div align="center"><img src="../../../screenshot-tileid.png" width=639
 * height=455><BR>
 * <i>Tile render mode</i></div><BR>
 * <BR>
 * Now we've applied the visibility and level of detail to the landscape, the
 * geometry for each tile is grab and rendered.<BR>
 * You can see in the next screenshot the landscape mesh.<BR>
 * <BR>
 * You can notive the elevation which will be shown in a more representative
 * screenshot next.<BR>
 * <BR>
 * <div align="center"><img src="../../../screenshot-mesh.png" width=640
 * height=455><BR>
 * <i>Mesh render mode</i></div><BR>
 * <BR>
 * In the next picture the landscape elevation is represented with color. Color
 * goes from the grey (lowest point) to yellow (highest point).<BR>
 * You can notice the elevation is more detailled near to the view than area on
 * the background, which we can suspect with the more detailled mesh.<BR>
 * <BR>
 * <div align="center"><img src="../../../screenshot-elevation.png" width=639
 * height=456><BR>
 * <i>Elevation render mode</i></div><BR>
 * <BR>
 * A tile has also level of detail for image data. This is used to adjust load
 * balance during the request of map image data to the map server.<BR>
 * A map image has 4 resolutions. The first resolution is the less detailled, to
 * the last resolution with best details.<BR>
 * <BR>
 * This last screenshot represents the resolution used to render the ground
 * image.<BR>
 * <BR>
 * The red is the first resolution, the green the second resolution, the blue
 * the third one and finally white for the last resolution.<BR>
 * <BR>
 * <div align="center"><img src="../../../screenshot-jp2.png" width=640
 * height=456><BR>
 * <i>JP2 render mode</i></div><BR>
 * <BR>
 * <BR>
 * 
 * @see #groundHeight(double, double, int)
 * 
 * @author Jerome Jouvie
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class Landscape {
    // Standard: geometry fill & textured

    public final static byte DISPLAY_STANDARD = (byte) 0;
    // Render geometry in line model
    public final static byte DISPLAY_MESH = (byte) 1;
    // Color is the terrain elevation
    public final static byte DISPLAY_SHADEDDEM = (byte) 2;
    // Tile color is the jp2 current resolution
    public final static byte DISPLAY_JP2RES = (byte) 3;
    // Tile color is the tile id
    public final static byte DISPLAY_TILEID = (byte) 4;
    // Tile color is the level id
    public final static byte DISPLAY_LEVELID = (byte) 5;
    private final static int LANDSCAPE_MAXLONGITUDE_ANGLE = 180;
    private final static int LANDSCAPE_MAXLATITUDE_ANGLE = 90;
    private final static int MAX_LAYERSTATUS = 3;
    private float meshColor = 0.85f;
    private float[] topColor = {0.47843137f, 0.34509804f, 0.117647f};
    private float[] tileColor = {0.2196078f, 0.4627450980f, 0.235294f};
    private float[] colorRatios;

    // Draw landscape
    public boolean drawLandscape = true;
    // Max longitude in DD
    private int maxLongitude;
    // Max latitude in DD
    private int maxLatitude;

    // Distance of far clip
    private int farClip = 1100000;

    // Elevation enabled
    private boolean terrainEnabled = true;
    // Elevation scaling
    private double terrainScaler = 1;
    private short maxColorHeight = 1000;

    // Landscape display mode: DISPLAY_STANDARD, DISPLAY_MESH, DISPLAY_SHADEDDEM
    private byte displayMode;

    // Landscape levels (resolution levels)
    private Level[] levels;

    // Fog radius
    private int fogRadius;
    // Fog color
    private float fogColor[] = {214.0f / 255, 235.0f / 255, 248.0f / 255, 1.0f};

    /**
     * Creates a new instance.
     *
     * @param ptolemy
     */
    public Landscape() {

        this.displayMode = DISPLAY_STANDARD;
        this.terrainEnabled = true;

        initialize();
    }

    // private final void setLevel(int lvl)
    // {
    // int squares = 1;
    // int square_pos = 0;
    // // use pitch and lr rotation to decide which tiles to get.
    //
    // // convert to degrees
    // double pitch = CameraMovement.PITCH * Math3D.RADIAN_TO_DEGREE_FACTOR;
    // double direction = ptolemy.camera.getDirectionDegrees();
    //
    // if (pitch > -30) {
    // squares = 16;
    // }
    // else {
    // int pinc = ((75 - 30) / ((LEVEL_NUMTILE_LON / 2) - 1));
    // int tang = -75;
    // for (int j = 0; j < (LEVEL_NUMTILE_LON / 2) - 1; j++) {
    // if ((pitch > tang) && (pitch <= (tang + pinc))) {
    // squares = 8 * (j + 1);
    // if (squares > 16) {
    // squares = 16; // a fix until i figure this mess out
    // }
    // break;
    // }
    // tang += pinc;
    // }
    // }
    // // dimension defines how big our outer square is.
    // square_pos = Math.round((float) ((squares * direction) / 360));
    //
    //
    // int lrmove, udmove;
    // // find how many tiles down/up we need to go.
    // if (squares < 8) {
    // lrmove = 0;
    // udmove = 0;
    // }
    // else if (square_pos < (squares / 4)) {
    // // how many tiles to the left?
    // lrmove = -square_pos;
    // if (lrmove < (-(squares / 8) + 1)) {
    // lrmove = (-(squares / 8) + 1);
    // // how many tiles up?
    // }
    // udmove = -((squares / 4) - square_pos);
    // if (udmove < (-(squares / 8) + 1)) {
    // udmove = (-(squares / 8) + 1);
    // }
    // }
    // else if (square_pos < (squares / 2)) {
    // // ll quad
    // lrmove = -((squares / 2) - square_pos);
    // if (lrmove < (-(squares / 8) + 1)) {
    // lrmove = (-(squares / 8) + 1);
    // }
    // udmove = square_pos - (squares / 4);
    // if (udmove > (squares / 8)) {
    // udmove = (squares / 8) - 1;
    // }
    // }
    // else if (square_pos < ((squares / 2) + (squares / 4))) {
    // // lr quad
    // lrmove = square_pos - (squares / 2);
    // if (lrmove > (squares / 8)) {
    // lrmove = (squares / 8) - 1;
    // }
    // udmove = (squares - (squares / 4)) - square_pos;
    // if (udmove > (squares / 8)) {
    // udmove = (squares / 8) - 1;
    // }
    // }
    // else {
    // // ur quad
    // lrmove = squares - square_pos;
    // if (lrmove > (squares / 8)) {
    // lrmove = (squares / 8) - 1;
    // }
    // udmove = -(square_pos - (squares - (squares / 4)));
    // if (udmove < (-(squares / 8) + 1)) {
    // udmove = (-(squares / 8) + 1);
    // }
    // }
    //
    // //use zero meter x and y to compute which tile should be directly under
    // us
    //
    // final Camera camera = ptolemy.camera;
    // final Level level = levels[lvl];
    // final int tileWidth = level.getTileWidth();
    //
    // int tileover = (int) (camera.getLongitudeDD() / tileWidth);
    // int tiledown = (int) (camera.getLatitudeDD() / tileWidth);
    //
    // level.upLeftLon = (tileover - LEVEL_NUMTILE_LON / 2) * tileWidth +
    // (lrmove * tileWidth);
    // level.upLeftLat = (tiledown - LEVEL_NUMTILE_LAT / 2) * tileWidth +
    // (udmove * tileWidth);
    //
    // level.lowRightLon = level.upLeftLon + (LEVEL_NUMTILE_LON * tileWidth);
    // level.lowRightLat = level.upLeftLat + (LEVEL_NUMTILE_LAT * tileWidth);
    // }
    /** Landscape Initialization */
    private void initialize() {
        maxLatitude = Unit.degreesToDD(LANDSCAPE_MAXLATITUDE_ANGLE);
        maxLongitude = Unit.degreesToDD(LANDSCAPE_MAXLONGITUDE_ANGLE);

        fogRadius = 20000; // this is the max
        fogColor[0] = 164.f / 255;
        fogColor[1] = 193.f / 255;
        fogColor[2] = 232.f / 255;

        colorRatios = new float[]{
                    (topColor[0] - tileColor[0]) / maxColorHeight,
                    (topColor[1] - tileColor[1]) / maxColorHeight,
                    (topColor[2] - tileColor[2]) / maxColorHeight};

    // setLevel(0);
    }

    /**
     * Init landscape OpenGL States.
     */
    protected void initGL(DrawContext drawContext) {

        GL gl = drawContext.getGl();

        gl.glEnable(GL.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glShadeModel(GL.GL_FLAT);

        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);

        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glEnable(GL.GL_FOG);
        gl.glFogi(GL.GL_FOG_MODE, GL.GL_LINEAR);
        gl.glFogfv(GL.GL_FOG_COLOR, fogColor, 0);
        gl.glFogf(GL.GL_FOG_DENSITY, 0.4f);
        gl.glFogf(GL.GL_FOG_START, 4000.0f);
        gl.glFogf(GL.GL_FOG_END, fogRadius);

        gl.glPolygonOffset(1.0f, 3.0f);
        gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);

        // TODO - Must be get from settings
        final float[] clearColor = Color.BLUE.getRGBComponents(null); //ptolemy.configuration.backgroundColor;
        gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], 1.0f);
    }

    /** Landscape Prepare */
    public void prepareFrame(DrawContext drawContext) {

        GL gl = drawContext.getGl();

        // Destroy unused textures
        Ptolemy3D.getTextureManager().destroyTrashTextures(gl);

        // Correct Tiles
        if (DEBUG) {
            if (!ProfilerInterface.freezeCorrectLevels) {
                correctLevels(drawContext);
            }
        }
        else {
            correctLevels(drawContext);
        }

        processVisibility(drawContext);
    }

    // TODO - Document what this method does.
    // vpPos turns to lon , zoom , lat
    private final void correctLevels(DrawContext drawContext) {
        GL gl = drawContext.getGl();
        final Camera camera = drawContext.getCanvas().getCamera();

        for (int p = 0; p < levels.length; p++) {
            final Level level = levels[p];
            final int tileWidth = level.getTileSize();

            int ysgn = -1;
            // TODO - What???
            int lon_move = (maxLongitude / tileWidth) * tileWidth;

            int minlon, maxlon, minlat, maxlat;

            // TODO - What???
            int leftmosttile = -((maxLongitude / tileWidth) * tileWidth);
            if (leftmosttile != maxLongitude) {
                leftmosttile -= tileWidth;
            }

            // TODO - What???
            minlon = ((int) (camera.getPosition().getLongitudeDD() / tileWidth) * tileWidth) - (tileWidth * 4);
            if (minlon < leftmosttile) {
                minlon += ((maxLongitude * 2) / tileWidth) * tileWidth;
            }
            maxlon = minlon + (tileWidth * LEVEL_NUMTILE_LON);

            int toptile = (maxLatitude / tileWidth) * tileWidth;
            if (toptile != maxLatitude) {
                toptile += tileWidth;
            }
            boolean wraps = false;
            if ((tileWidth * LEVEL_NUMTILE_LAT) > (maxLatitude * 2)) {
                wraps = true;
                minlat = toptile;
            }
            else {
                minlat = ((int) (camera.getPosition().getLatitudeDD() / tileWidth) * tileWidth) + (tileWidth * 4);
                if (minlat > toptile) {
                    minlat = toptile - (((minlat - maxLatitude) / tileWidth) * tileWidth);
                    if (minlon > 0) {
                        minlon -= lon_move;
                    }
                    else {
                        minlon += lon_move;
                    }
                    ysgn *= -1;
                }
            }
            maxlat = minlat + (tileWidth * LEVEL_NUMTILE_LAT) * ysgn;

            level.correctTiles(toptile, minlat, maxlat, minlon, maxlon,
                               lon_move, ysgn, wraps);
        }
    }

    private void processVisibility(DrawContext drawContext) {
        GL gl = drawContext.getGl();
        if (!drawLandscape || Ptolemy3D.getTileLoader() == null) {
            return;
        }
        if (DEBUG && ProfilerInterface.freezeVisibility) {
            return;
        }

        final Camera camera = drawContext.getCanvas().getCamera();
        boolean hasVisLayer = false;

        int numStatus = 0; // Track the number of levels
        for (int p = levels.length - 1; p >= 0; p--) {
            if (numStatus >= MAX_LAYERSTATUS) {
                // Too many levels are in the altitude range
                for (int hh = p; hh >= 0; hh--) {
                    final Level level = levels[hh];
                    level.setVisible(false);
                    level.setStatus(false);
                }
                break;
            }

            final Level level = levels[p];

            // Altitude check: in layer range
            level.setStatus(((camera.getVerticalAltitudeMeters() > level.getMaxZoom()) || (camera.getVerticalAltitudeMeters() <= level.getMinZoom())) ? false
                    : true);
            if (level.isStatus()) {
                numStatus++; // Track the number of level in the altitude range
            }

            if (!level.isStatus() && (hasVisLayer || (camera.getVerticalAltitudeMeters() > level.getMaxZoom()))) {
                level.setVisible(false);
                continue;
            }

            // Image datas available ?
            final Jp2TileLoader tileLoader = Ptolemy3D.getTileLoader();
            if (!tileLoader.levelHasImageData(p)) {
                level.setVisible(false);
                continue;
            }

            hasVisLayer = true; // Now at least one level has image data
            level.setVisible(true);

            // Acquire tile image data
            tileLoader.acquireTile(level, p == 0);
            level.processVisibility();
        }
    }

    /** Landscape Rendering */
    protected void draw(DrawContext drawContext) {
        GL gl = drawContext.getGl();

        if (!drawLandscape) {
            return;
        }

        if (displayMode == DISPLAY_MESH) {
            gl.glColor3f(meshColor, meshColor, meshColor);
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
            gl.glDisable(GL.GL_TEXTURE_2D);
        }
        else if (displayMode == DISPLAY_SHADEDDEM) {
            gl.glColor3f(tileColor[0], tileColor[1], tileColor[2]);
            gl.glShadeModel(GL.GL_SMOOTH);
            gl.glDisable(GL.GL_TEXTURE_2D);
        }
        else if (displayMode == DISPLAY_JP2RES || displayMode == DISPLAY_TILEID || displayMode == DISPLAY_LEVELID) {
            gl.glDisable(GL.GL_TEXTURE_2D);
        }

        for (int p = levels.length - 1; p >= 0; p--) {
            final Level level = levels[p];
            level.draw(drawContext);
        }

        if (displayMode == DISPLAY_MESH) {
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
        }
        else if (displayMode == DISPLAY_SHADEDDEM) {
            gl.glShadeModel(GL.GL_FLAT);
        }
        gl.glColor3f(1, 1, 1);
        gl.glEnable(GL.GL_TEXTURE_2D);
    }

    // TODO - Remove. The return line was moved into the Level.draw method
//    public final int load(byte[] datas, int width, int height, int res) {
//        return Ptolemy3D.getTextureManager().load(gl, datas, width, height, GL.GL_RGB,
//                                                true, res);
//    }
    /** Landscape Picking: Pick tiles */
    public final synchronized boolean pick(double[] intersectPoint,
                                           double[][] ray) {
        for (int p = levels.length - 1; p >= 0; p--) {
            final Level level = levels[p];
            if (level.pick(intersectPoint, ray)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieve the ground height.
     *
     * @param lon
     *            longitude in DD
     * @param lat
     *            latitude in DD
     * @param minLevel
     *            minimum level
     * @return the ground height, 0 is the reference value (no elevation).
     */
    public double groundHeight(double lon, double lat, int minLevel) {
        if (!terrainEnabled) {
            return 0;
        }

        double[][] ray = {
            {lon, CameraMovement.MAXIMUM_ALTITUDE, lat},
            {lon, CameraMovement.MAXIMUM_ALTITUDE - 1, lat}
        };

        for (int i = levels.length - 1; (i >= minLevel); i--) {
            final Level level = levels[i];

            try {
                double[] pickArr = level.groundHeight(lon, lat, ray);
                if (pickArr != null) {
                    return pickArr[1] * terrainScaler;
                }
            }
            catch (RuntimeException e) {
                IO.printStackRenderer(e);
            }
        }
        return 0;
    }

    /** Landscape Destruction */
    protected void destroyGL(DrawContext drawContext) {
    }

    /**
     * @param meshColor
     *            the meshColor to set
     */
    public void setMeshColor(float meshColor) {
        this.meshColor = meshColor;
    }

    /**
     * @return the meshColor
     */
    public float getMeshColor() {
        return meshColor;
    }

    /**
     * @param tileColor
     *            the tileColor to set
     */
    public void setTileColor(float[] tileColor) {
        this.tileColor = tileColor;
    }

    /**
     * @return the tileColor
     */
    public float[] getTileColor() {
        return tileColor;
    }

    /**
     * @param colorRatios
     *            the colorRatios to set
     */
    public void setColorRatios(float[] colorRatios) {
        this.colorRatios = colorRatios;
    }

    /**
     * @return the colorRatios
     */
    public float[] getColorRatios() {
        return colorRatios;
    }

    /**
     * @param maxLongitude
     *            the maxLongitude to set
     */
    public void setMaxLongitude(int maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    /**
     * @return the maxLongitude
     */
    public int getMaxLongitude() {
        return maxLongitude;
    }

    /**
     * @param maxLatitude
     *            the maxLatitude to set
     */
    public void setMaxLatitude(int maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    /**
     * @return the maxLatitude
     */
    public int getMaxLatitude() {
        return maxLatitude;
    }

    /**
     * @param farClip
     *            the farClip to set
     */
    public void setFarClip(int farClip) {
        this.farClip = farClip;
    }

    /**
     * @return the farClip
     */
    public int getFarClip() {
        return farClip;
    }

    /**
     * @param terrainEnabled
     *            the terrainEnabled to set
     */
    public void setTerrainEnabled(boolean terrainEnabled) {
        this.terrainEnabled = terrainEnabled;
    }

    /**
     * @return the terrainEnabled
     */
    public boolean isTerrainEnabled() {
        return terrainEnabled;
    }

    /**
     * @param terrainScaler
     *            the terrainScaler to set
     */
    public void setTerrainScaler(double terrainScaler) {
        this.terrainScaler = terrainScaler;
    }

    /**
     * @return the terrainScaler
     */
    public double getTerrainScaler() {
        return terrainScaler;
    }

    /**
     * @param maxColorHeight
     *            the maxColorHeight to set
     */
    public void setMaxColorHeight(short maxColorHeight) {
        this.maxColorHeight = maxColorHeight;
    }

    /**
     * @return the maxColorHeight
     */
    public short getMaxColorHeight() {
        return maxColorHeight;
    }

    /**
     * @param displayMode
     *            the displayMode to set
     */
    public void setDisplayMode(byte displayMode) {
        this.displayMode = displayMode;
    }

    /**
     * @return the displayMode
     */
    public byte getDisplayMode() {
        return displayMode;
    }

    /**
     * @param levels
     *            the levels to set
     */
    public void setLevels(Level[] levels) {
        this.levels = levels;
    }

    /**
     * @return the levels
     */
    public Level[] getLevels() {
        return levels;
    }

    /**
     * @param fogRadius
     *            the fogRadius to set
     */
    public void setFogRadius(int fogRadius) {
        this.fogRadius = fogRadius;
    }

    /**
     * @return the fogRadius
     */
    public int getFogRadius() {
        return fogRadius;
    }
}