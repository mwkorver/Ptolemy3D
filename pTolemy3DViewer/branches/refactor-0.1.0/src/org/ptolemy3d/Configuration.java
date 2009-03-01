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
package org.ptolemy3d;

import java.awt.Color;
import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.exceptions.Ptolemy3DConfigurationException;
import org.ptolemy3d.exceptions.Ptolemy3DException;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugins;
import org.ptolemy3d.scene.Scene;
import org.ptolemy3d.scene.Sky;
import org.ptolemy3d.tile.Level;
import org.ptolemy3d.view.CameraMovement;
import org.ptolemy3d.view.Position;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Configuration class loads and stores a set of Ptolemy3D configuration parameters.
 * This is used for example to specify the map server date, globe geometry settings
 * (number of levels)), the initial view position, etc.
 * 
 * @author Jerome Jouvie
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class Configuration {

    // ////////////////////////////////////////////////
    // Required params
    // Server
    public final static String SERVER = "Server";
    public final static String NUM_MAP_DATA_SERVER = "NumMapDataServers";
    public final static String MAP_DATA_SERVER = "MapDataSvr";
    public final static String MAP_JP2_STORE = "MapJp2Store";
    public final static String MAP_DEM_STORE = "MapDemStore";
    public final static String LOCATION_ROOT = "LocationRoot";
    public final static String AREA = "Area";
    public final static String DEM = "DEM";
    // Unit
    public final static String CENTER_X = "CenterX";
    public final static String CENTER_Y = "CenterY";
    // Layers
    public final static String NUM_LAYERS = "NumLayers";
    public final static String LAYER_WIDTH = "LayerWidth_";
    public final static String LAYER_MIN = "LayerMin_";
    public final static String LAYER_MAX = "LayerMax_";

    // ////////////////////////////////////////////////
    // Optional params
    // Server
    public final static String MURL_APPEND = "MUrlAppend";
    public final static String MHEADER_KEY = "MHeaderKey";
    public final static String IS_KEEP_ALIVE = "isKeepAlive";
    // Unit
    public final static String DD = "DDBuffer"; // Jerome: I think nothing
    // Initial position
    public final static String ORIENTARION = "Orientation";
    // Layer
    public final static String LAYER_DIVIDER = "LayerDivider_";
    // Plugin
    public final static String NUM_PLUGINS = "NumPlugins";
    public final static String PLUGIN_TYPE = "PluginType_";
    public final static String PLUGIN_PARAM = "PluginParam_";
    // Misc
    public final static String FOLLOW_DEM = "FollowDEM";
    public final static String HTSCALE = "HTSCALE";
    public final static String KEYBOARD = "KEYBOARD";
    public final static String TIN = "TIN";
    public final static String BACKGROUND_IMAGE_URL = "BackgroundImageUrl";
    public final static String SUBTEXTURES = "SubTextures";
    public final static String HORIZON = "Horizon";
    public final static String MAX_ALT = "MAX_ALT";
    public final static String TILE_COLOR = "TileColor";
    public final static String BACKGROUND_COLOR = "BackgroundColor";
    public final static String HUD_BG_RGBA = "HUDBGRGBA";
    public final static String FLIGHT_FRAME_DELAY = "FlightFrameDelay";

    // Logger instance.
    private static final Logger logger = Logger.getLogger(Configuration.class.getName());
    // Ptolemy3D Instance
    private final Ptolemy3DGLCanvas canvas;

    // Clearing color (back ground color)
    public float[] backgroundColor = {0.541176470588f, 0.540176470588f, 0.540176470588f};
    public String server = "";
    public String[] dataServers;
    public String[] urlAppends;
    public String[] headerKeys;
    public boolean[] keepAlives;
    public String[][] locations;
    public String area = "";
    public String DEMLocation[][];
    public int flightFrameDelay = 1;
    public boolean useSubtexturing = false;
    public boolean useTIN = false;
    public Color hudBackgroundColor = new Color(20, 20, 20, 128);
    public String backgroundImageUrl = null;

    /**
     * Creates a new configuration instance that configures the specified canvas
     * and the Ptolemy3D scene.
     * 
     * @param canvas
     */
    public Configuration(Ptolemy3DGLCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Creates a new configuration instance.
     *
     * @param canvas
     * @param xmlFile
     */
    public Configuration(Ptolemy3DGLCanvas canvas, String xmlFile) {
        this.canvas = canvas;
        loadSettings(xmlFile);
    }

    /**
     * Parse an XML file and construct the document elements for Ptolemy3D.
     */
    private Element buildXMLDocument(String xmlFile) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        Document document = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (xmlFile.indexOf("http://") == -1) {
                File file = new File(xmlFile);
                document = builder.parse(file);
            }
            else {
                document = builder.parse(xmlFile);
            }
        }
        catch (SAXException sxe) {
            Exception x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            IO.printStack(x);
        }
        catch (Exception e) {
            logger.severe(e.getMessage());
            IO.printStack(e);
        }

        if (document == null) {
            return null;
        }

        return document.getDocumentElement();
    }

    /**
     * Loads the configuration settings. If docelem is null then settings are
     * loaded from applet configuration.
     *
     * @param docelem
     */
    public void loadSettings(String xmlFile) throws Ptolemy3DException {

        Element docelem = buildXMLDocument(xmlFile);

        final CameraMovement cameraController = canvas.getCameraMovement();
        final Scene scene = Ptolemy3D.getScene();
        final Landscape landScape = scene.landscape;
        final Sky sky = scene.sky;
        final Plugins plugins = scene.plugins;

        // Load settings. If any configuration exception occurs then stop the
        // loading process and throws a general exception.
        try {

            // Coordinate System Units
            int meterX = getRequieredParameterInt(CENTER_X, docelem);
            int meterZ = getRequieredParameterInt(CENTER_Y, docelem);
            Unit.setMeterX(meterX);
            Unit.setMeterZ(meterZ);

            int dd = getOptionalParameterInt(DD, docelem);
            if (dd != -1) {
                Unit.setDDFactor(dd);
            }

            // Server information
            server = getRequieredParameter(SERVER, docelem);
            // Area
            area = getRequieredParameter(AREA, docelem);

            // Layers: Landscape levels (resolution levels) and layers
            // properties
            int numLayers = getRequieredParameterInt(NUM_LAYERS, docelem);
            landScape.setLevels(new Level[numLayers]);

            for (int i = 0; i < numLayers; i++) {
                int tilePixel = getRequieredParameterInt(LAYER_WIDTH + (i + 1),
                                                         docelem);
                int minZoom = getRequieredParameterInt(LAYER_MIN + (i + 1),
                                                       docelem);
                int maxZoom = getRequieredParameterInt(LAYER_MAX + (i + 1),
                                                       docelem);
                int divider = getOptionalParameterInt(LAYER_DIVIDER + (i + 1),
                                                      docelem);
                if (divider == -1) {
                    divider = 0;
                }

                Level level = new Level(i, minZoom, maxZoom, tilePixel, divider);
                landScape.getLevels()[i] = level;
            }

            // Background image
            backgroundImageUrl = getOptionalParameter(BACKGROUND_IMAGE_URL,
                                                      docelem);
            if (backgroundImageUrl != null && !backgroundImageUrl.endsWith("?")) {
                backgroundImageUrl = backgroundImageUrl + "?";
            }

            // Initial position of the camera.
            String orientation = getOptionalParameter(ORIENTARION, docelem);
            if (orientation != null) {
                String[] values = orientation.split(",");
                if (values.length == 5) {
                    try {
                        double lat = Double.parseDouble(values[0]);
                        double lon = Double.parseDouble(values[1]);
                        double alt = Double.parseDouble(values[2]);
                        double dir = Double.parseDouble(values[3]);
                        double pitch = Double.parseDouble(values[4]);
                        cameraController.setOrientation(new Position(lat, lon, alt), dir, pitch);
                    }
                    catch (NumberFormatException e) {
                        String message = "Some of the values in the " + ORIENTARION + " property are invalids.\nThe property will be ignored";
                        logger.warning(message);
                    }
                }
                else {
                    String message = "The number of values for the " + ORIENTARION + " property does not correspond to (lat,lon,alt,dir,pitch). " + "You have specified a different number.\nThe property will be ignored";
                    logger.warning(message);
                }
            }

            // Other optional properties
            flightFrameDelay = getOptionalParameterInt(FLIGHT_FRAME_DELAY, docelem);
            if (flightFrameDelay == -1) {
                flightFrameDelay = 1;
            }

            int horizon = getOptionalParameterInt(HORIZON, docelem);
            if (horizon != -1) {
                sky.horizonAlt = horizon;
            }
            int maxalt = getOptionalParameterInt(MAX_ALT, docelem);
            if (maxalt != -1) {
                CameraMovement.MAXIMUM_ALTITUDE = maxalt;
            }

            useTIN = getOptionalParameterBoolean(TIN, docelem);
            useSubtexturing = getOptionalParameterBoolean(SUBTEXTURES, docelem);

            String s;
            if ((s = getOptionalParameter(HUD_BG_RGBA, docelem)) != null) {
                hudBackgroundColor = getAwtColor(s);
            }
            if ((s = getOptionalParameter(FOLLOW_DEM, docelem)) != null) {
                cameraController.setFollowDem(s.equals("1") ? true : false);
            }
            s = getOptionalParameter(KEYBOARD, docelem);
            cameraController.inputs.keyboardEnabled = ((s != null) && (s.equals("0"))) ? false : true;

            if ((s = getOptionalParameter(HTSCALE, docelem)) != null) {
                landScape.setMaxColorHeight(Short.parseShort(s));
            }
            if ((s = getOptionalParameter(TILE_COLOR, docelem)) != null) {
                final StringTokenizer st = new StringTokenizer(s, ",");
                if (st.countTokens() > 2) {
                    landScape.getTileColor()[0] = (Float.parseFloat(st.nextToken()) / 255);
                    landScape.getTileColor()[1] = (Float.parseFloat(st.nextToken()) / 255);
                    landScape.getTileColor()[2] = (Float.parseFloat(st.nextToken()) / 255);
                }
            }
            if ((s = getOptionalParameter(BACKGROUND_COLOR, docelem)) != null) {
                final StringTokenizer st = new StringTokenizer(s, ",");
                if (st.countTokens() > 2) {
                    backgroundColor[0] = (Float.parseFloat(st.nextToken()) / 255);
                    backgroundColor[1] = (Float.parseFloat(st.nextToken()) / 255);
                    backgroundColor[2] = (Float.parseFloat(st.nextToken()) / 255);
                }
            }

            // Plugins
            int numPlugins = getOptionalParameterInt(NUM_PLUGINS, docelem);
            for (int p = 0; p < numPlugins; p++) {
                try {
                    plugins.addPlugin(getOptionalParameter(PLUGIN_TYPE + (p + 1), docelem), getOptionalParameter(
                            PLUGIN_PARAM + (p + 1), docelem));
                }
                catch (Exception ex) {
                    logger.severe("Problem loading plugins: " + ex.toString());
                }
            }

        }
        catch (Ptolemy3DConfigurationException ex) {
            // Abort the load process if some required parameter fails.
            logger.severe(ex.getMessage());
            return;
        }

        // //////////////////////////////////////////////////////////////////
        // //////////////////////////////////////////////////////////////////
        // //////////////////////////////////////////////////////////////////
        // //////////////////////////////////////////////////////////////////
        int nDataServers = getRequieredParameterInt(NUM_MAP_DATA_SERVER,
                                                    docelem);
        headerKeys = new String[nDataServers];
        dataServers = new String[nDataServers];
        locations = new String[nDataServers][];
        DEMLocation = new String[nDataServers][];
        keepAlives = new boolean[nDataServers];
        urlAppends = new String[nDataServers];

        for (int i = 1; i <= nDataServers; i++) {
            dataServers[i - 1] = getRequieredParameter(MAP_DATA_SERVER + i,
                                                       docelem);
            headerKeys[i - 1] = getOptionalParameter(MHEADER_KEY + i, docelem);
            urlAppends[i - 1] = getOptionalParameter(MURL_APPEND + i, docelem);

            String temppm = getRequieredParameter(MAP_JP2_STORE + i, docelem);
            if (temppm == null) {
                locations[i - 1] = null;
            }
            else {
                StringTokenizer ptok = new StringTokenizer(temppm, ",");
                locations[i - 1] = new String[ptok.countTokens()];
                for (int nfloc = 0; nfloc < locations[i - 1].length; nfloc++) {
                    locations[i - 1][nfloc] = addTrailingChar(ptok.nextToken(),
                                                              "/");
                }
            }

            temppm = getRequieredParameter(MAP_DEM_STORE + i, docelem);
            if (temppm == null) {
                DEMLocation[i - 1] = null;
            }
            else {
                StringTokenizer ptok = new StringTokenizer(temppm, ",");
                DEMLocation[i - 1] = new String[ptok.countTokens()];
                for (int nfloc = 0; nfloc < DEMLocation[i - 1].length; nfloc++) {
                    DEMLocation[i - 1][nfloc] = addTrailingChar(ptok.nextToken(), "/");
                }
            }

            temppm = getOptionalParameter(IS_KEEP_ALIVE + i, docelem);
            if (temppm == null) {
                keepAlives[i - 1] = true;
            }
            else {
                keepAlives[i - 1] = (temppm.equals("0")) ? false : true;
            }
        }

    }

    /**
     * Get a required parameter from applet. An exception is triggered if the
     * parameter was not found.
     *
     * @param name
     *            Parameter name.
     * @return String value.
     * @throws Ptolemy3DConfigurationException
     *             if the param doesn't exists.
     */
    private String getRequieredParameter(String name)
            throws Ptolemy3DConfigurationException {

        String parameter = null;

//		final Applet applet = ptolemy.javascript.getApplet();
//		parameter = applet.getParameter(name);
//
//		if (parameter == null) {
//			parameter = null;
//			String message = "The requiered parameter '" + name
//					+ "' is missing.";
//			throw new Ptolemy3DConfigurationException(message);
//		}
        return parameter;
    }

    /**
     * Get a required parameter from applet. An exception is triggered if the
     * parameter was not found.
     *
     * @param name
     *            Parameter name.
     * @return int value.
     * @throws Ptolemy3DConfigurationException
     *             if the param doesn't exists or doesn't contains a valid
     *             integer value.
     */
    private int getRequieredParameterInt(String name)
            throws Ptolemy3DConfigurationException {

        int value = 0;
        String strValue = getRequieredParameter(name);
        try {
            value = Integer.parseInt(strValue);
        }
        catch (NumberFormatException e) {
            String message = "The requiered parameter '" + name + "' is not a valid integer value.";
            throw new Ptolemy3DConfigurationException(message);
        }

        return value;
    }

    /**
     * Get a required parameter from applet. An exception is triggered if the
     * parameter was not found.
     *
     * @param name
     *            Parameter name.
     * @return float value.
     * @throws Ptolemy3DConfigurationException
     *             if the param doesn't exists or doesn't contains a valid float
     *             value.
     */
    private float getRequieredParameterFloat(String name)
            throws Ptolemy3DConfigurationException {

        float value = 0;
        String strValue = getRequieredParameter(name);
        try {
            value = Float.parseFloat(strValue);
        }
        catch (NumberFormatException e) {
            String message = "The requiered parameter '" + name + "' is not a valid float value.";
            throw new Ptolemy3DConfigurationException(message);
        }

        return value;
    }

    /**
     * Get a required parameter from applet. An exception is triggered if the
     * parameter was not found.
     *
     * @param name
     *            Parameter name.
     * @return boolean value.
     * @throws Ptolemy3DConfigurationException
     *             if the param doesn't exists.
     */
    private boolean getRequieredParameterBoolean(String name)
            throws Ptolemy3DConfigurationException {
        String strValue = getRequieredParameter(name);
        return strValue.equals("1") ? true : false;
    }

    /**
     * Get a required parameter. An exception is triggered if the parameter was
     * not found. If 'elem' is null the parameter is get from applet params,
     * otherwise it is get from a XML config file.
     *
     * @param name
     *            Parameter name.
     * @param elem
     *            XML document.
     * @return String value.
     * @throws Ptolemy3DConfigurationException
     *             if the param doesn't exists.
     */
    private String getRequieredParameter(String name, Element elem)
            throws Ptolemy3DConfigurationException {

        // Check if the setting must be read from XML file or from applet's
        // params.
        if (elem == null) {
            return getRequieredParameter(name);
        }

        String parameter;
        try {
            NodeList nodeList = elem.getElementsByTagName(name);
            parameter = ((Element) nodeList.item(0)).getChildNodes().item(0).getNodeValue();
        }
        catch (Exception ex) {
            parameter = null;
            String message = "The requiered parameter '" + name + "' is missing.\nException message: " + ex.getMessage();
            throw new Ptolemy3DConfigurationException(message);
        }

        return parameter;
    }

    /**
     * Get a required parameter. An exception is triggered if the parameter was
     * not found. If 'elem' is null the parameter is get from applet params,
     * otherwise it is get from a XML config file.
     *
     * @param name
     *            Parameter name.
     * @param elem
     *            XML document.
     * @return int value.
     * @throws Ptolemy3DConfigurationException
     *             if the param doesn't exists or doesn't contains a valid
     *             integer value.
     */
    private int getRequieredParameterInt(String name, Element elem)
            throws Ptolemy3DConfigurationException {

        // Check if the setting must be read from XML file or from applet's
        // params.
        if (elem == null) {
            return getRequieredParameterInt(name);
        }

        int value = 0;
        String strValue = getRequieredParameter(name, elem);
        try {
            value = Integer.parseInt(strValue);
        }
        catch (NumberFormatException e) {
            String message = "The requiered parameter '" + name + "' is not a valid integer value.";
            throw new Ptolemy3DConfigurationException(message);
        }

        return value;
    }

    /**
     * Get a required parameter. An exception is triggered if the parameter was
     * not found. If 'elem' is null the parameter is get from applet params,
     * otherwise it is get from a XML config file.
     *
     * @param name
     *            Parameter name.
     * @param elem
     *            XML document.
     * @return float value.
     * @throws Ptolemy3DConfigurationException
     *             if the param doesn't exists or doesn't contains a valid float
     *             value.
     */
    private float getRequieredParameterFloat(String name, Element elem)
            throws Ptolemy3DConfigurationException {

        // Check if the setting must be read from XML file or from applet's
        // params.
        if (elem == null) {
            return getRequieredParameterFloat(name);
        }

        float value = 0;
        String strValue = getRequieredParameter(name, elem);
        try {
            value = Float.parseFloat(strValue);
        }
        catch (NumberFormatException e) {
            String message = "The requiered parameter '" + name + "' is not a valid float value.";
            throw new Ptolemy3DConfigurationException(message);
        }

        return value;
    }

    /**
     * Get a required parameter. An exception is triggered if the parameter was
     * not found. If 'elem' is null the parameter is get from applet params,
     * otherwise it is get from a XML config file.
     *
     * @param name
     *            Parameter name.
     * @param elem
     *            XML document.
     * @return boolean value.
     * @throws Ptolemy3DConfigurationException
     *             if the param doesn't exists.
     */
    private boolean getRequieredParameterBoolean(String name, Element elem)
            throws Ptolemy3DConfigurationException {

        // Check if the setting must be read from XML file or from applet's
        // params.
        if (elem == null) {
            return getRequieredParameterBoolean(name);
        }

        String strValue = getRequieredParameter(name, elem);
        return strValue.equals("1") ? true : false;
    }

    /**
     * Get an optional parameter. If 'elem' is null the parameter is get from
     * applet params, otherwise it is get from a XML config file.
     *
     * @param name
     * @param elem
     * @return null if the param doesn't exist.
     */
    private String getOptionalParameter(String name, Element elem) {
        try {
            return getRequieredParameter(name, elem);
        }
        catch (Ptolemy3DConfigurationException e) {
            return null;
        }
    }

    /**
     * Get an optional parameter. If 'elem' is null the parameter is get from
     * applet params, otherwise it is get from a XML config file.
     *
     * @param name
     * @param elem
     * @return -1 if the param doesn't exist.
     */
    private int getOptionalParameterInt(String name, Element elem) {
        try {
            return getRequieredParameterInt(name, elem);
        }
        catch (Ptolemy3DConfigurationException e) {
            return -1;
        }
    }

    /**
     * Get an optional parameter. If 'elem' is null the parameter is get from
     * applet params, otherwise it is get from a XML config file.
     *
     * @param name
     * @param elem
     * @return -1 if the param doesn't exist.
     */
    private float getOptionalParameterFloat(String name, Element elem) {
        try {
            return getRequieredParameterFloat(name, elem);
        }
        catch (Ptolemy3DConfigurationException e) {
            return -1;
        }
    }

    /**
     * Get an optional parameter. If 'elem' is null the parameter is get from
     * applet params, otherwise it is get from a XML config file.
     *
     * @param name
     * @param elem
     * @return false if the param doesn't exist.
     */
    private boolean getOptionalParameterBoolean(String name, Element elem) {
        try {
            return getRequieredParameterBoolean(name, elem);
        }
        catch (Ptolemy3DConfigurationException e) {
            return false;
        }
    }

    /**
     * Adds the specified char to the end of the string if it doesn't finish
     * with that char.
     */
    private String addTrailingChar(String s, String tr) {
        if (!s.substring(s.length() - 1, s.length()).equals(tr)) {
            return s + tr;
        }
        else {
            return s;
        }
    }

    /**
     * Gets a color from the string representation.
     *
     * @param col
     * @return
     */
    private Color getAwtColor(String col) {
        final StringTokenizer st = new StringTokenizer(col, ",");
        if (st.countTokens() > 2) {
            int r = Integer.parseInt(st.nextToken());
            int g = Integer.parseInt(st.nextToken());
            int b = Integer.parseInt(st.nextToken());
            int a = 255;
            if (st.hasMoreTokens()) {
                a = Integer.parseInt(st.nextToken());
            }
            return new Color(r, g, b, a);
        }
        return Color.BLACK;
    }
}
