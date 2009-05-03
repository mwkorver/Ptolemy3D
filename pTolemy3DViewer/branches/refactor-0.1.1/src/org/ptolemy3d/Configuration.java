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

import java.applet.Applet;
import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.exceptions.Ptolemy3DConfigurationException;
import org.ptolemy3d.exceptions.Ptolemy3DException;
import org.ptolemy3d.globe.Layer;
import org.ptolemy3d.io.ServerConfig;
import org.ptolemy3d.plugin.Sky;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugins;
import org.ptolemy3d.scene.Scene;
import org.ptolemy3d.view.CameraMovement;
import org.ptolemy3d.view.Position;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Configuration class loads and stores a set of Ptolemy3D configuration parameters.
 * This is used for example to specify the map server date, globe geometry settings
 * (number of levels)), the initial view position, etc.
 * 
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class Configuration {
	private static interface Requiered {
		/* Server */
		public final static String Server = "Server";
		public final static String NumMapDataServer = "NumMapDataServers";
		public final static String MapDataSvr = "MapDataSvr";
		public final static String MapJp2Store = "MapJp2Store";
		public final static String MapDemStore = "MapDemStore";
		public final static String LocationRoot = "LocationRoot";
		public final static String Area = "Area";
		public final static String DEM = "DEM";		
		/* Unit */
		public final static String CenterX = "CenterX";
		public final static String CenterY = "CenterY";
		/* Layers */
		public final static String NumLayers = "NumLayers";
		public final static String LayerWidth_ = "LayerWidth_";
		public final static String LayerMin_ = "LayerMin_";
		public final static String LayerMax_ = "LayerMax_";
	}
	private static interface Optional {
		/* Server */
		public final static String MUrlAppend = "MUrlAppend";
		public final static String MHeaderKey = "MHeaderKey";
		public final static String numMapDataServer = "MDataServer";
		public final static String isKeepAlive = "isKeepAlive";
		/* Unit */
		public final static String DD = "DD";	//Jerome: I think nothing will work if different to 1E6
		/* Initial position */
		public final static String Orientation = "Orientation";
		/* Layer */
		public final static String LayerDivider_ = "LayerDivider_";
		/* Plugin */
		public final static String NumPlugins = "NumPlugins";
		public final static String PluginType_ = "PluginType_";
		public final static String PluginParam_ = "PluginParam_";
		/* Misc */
		public final static String FollowDEM = "FollowDEM";
		public final static String HeightScale = "HeightScale";
		public final static String TIN = "TIN";
		public final static String BackgroundImageUrl = "BackgroundImageUrl";
		public final static String Horizon = "Horizon";
		public final static String MaxAltitude = "MaxAltitude";
		public final static String TileColor = "TileColor";
		public final static String BackgroundColor = "BackgroundColor";
		public final static String FlightFrameDelay = "FlightFrameDelay";
	}

	// Logger instance.
	private static final Logger logger = Logger.getLogger(Configuration.class.getName());
	// Clearing color (back ground color)
	public float[] backgroundColor = {0.541176470588f, 0.540176470588f, 0.540176470588f};
	// Initial position of the camera.
    public Position initialCameraPosition = new Position(0, 0, 0);
    public double initialCameraDirection = 0;
    public double initialCameraPitch = 0;

    protected ServerConfig[] servers = null;
    protected String server = "";
	public String area = "";
	public int flightFrameDelay = 1;
	public boolean useTIN = false;
	public String backgroundImageUrl = null;
    public boolean follorDEM = true;

    public Configuration() {
    	loadSettings(null);
    }
    
    /**
     * Creates a new configuration instance given the specified file.
     *
     * @param canvas
     * @param xmlFile
     */
    public Configuration(String xmlFile) {
        loadSettings(buildXMLDocument(xmlFile));
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
    public void loadSettings(Element docelem) throws Ptolemy3DException {
        final Scene scene = Ptolemy3D.getScene();
        final Landscape landScape = scene.landscape;
        final Sky sky = scene.sky;
        final Plugins plugins = scene.plugins;

        // Coordinate System Units
        try {
            int meterX = getRequieredParameterInt(Requiered.CenterX, docelem);
            int meterZ = getRequieredParameterInt(Requiered.CenterY, docelem);
            Unit.setMeterX(meterX);
            Unit.setMeterZ(meterZ);

            int dd = getOptionalParameterInt(Optional.DD, docelem);
            if (dd != -1) {
                Unit.setDDFactor(dd);
            }
        }
        catch (Ptolemy3DConfigurationException ex) {
            // Abort the load process if some required parameter fails.
            logger.severe(ex.getMessage());
            return;
        }
        
		// Server informations
		server = getRequieredParameter(Requiered.Server, docelem);

		// Area
        area = getRequieredParameter(Requiered.Area, docelem);

        // MapDataServer
		int nDataServers = getRequieredParameterInt(Requiered.NumMapDataServer, docelem);
		servers = new ServerConfig[nDataServers];
		for (int i = 1; i <= nDataServers; i++) {
			String dataServers = getRequieredParameter(Requiered.MapDataSvr + i, docelem);
			String headerKeys = getOptionalParameter(Optional.MHeaderKey + i, docelem);
			String urlAppends = getOptionalParameter(Optional.MUrlAppend + i, docelem);

			String[] locations;
			String temppm = getRequieredParameter(Requiered.MapJp2Store + i, docelem);
			if (temppm == null) {
				locations = null;
			}
			else {
				StringTokenizer ptok = new StringTokenizer(temppm, ",");
				locations = new String[ptok.countTokens()];
				for (int nfloc = 0; nfloc < locations.length; nfloc++) {
					locations[nfloc] = addTrailingChar(ptok.nextToken(),
					"/");
				}
			}

			String[] DEMLocation;
			temppm = getRequieredParameter(Requiered.MapDemStore + i, docelem);
			if (temppm == null) {
				DEMLocation = null;
			}
			else {
				StringTokenizer ptok = new StringTokenizer(temppm, ",");
				DEMLocation = new String[ptok.countTokens()];
				for (int nfloc = 0; nfloc < DEMLocation.length; nfloc++) {
					DEMLocation[nfloc] = addTrailingChar(ptok.nextToken(), "/");
				}
			}

			boolean keepAlives;
			temppm = getOptionalParameter(Optional.isKeepAlive + i, docelem);
			if (temppm == null) {
				keepAlives = true;
			}
			else {
				keepAlives = (temppm.equals("0")) ? false : true;
			}
			
			servers[i - 1] = new ServerConfig(headerKeys, dataServers, locations, DEMLocation, keepAlives, urlAppends);
		}

        try {
            // Layers: Landscape levels (resolution levels) and layers
            // properties
        	final int numLayers = getRequieredParameterInt(Requiered.NumLayers, docelem);
            final Layer[] layers = new Layer[numLayers];
            landScape.globe.setLevels(layers);

            for (int i = 0; i < numLayers; i++) {
                int tilePixel = getRequieredParameterInt(Requiered.LayerWidth_ + (i + 1), docelem);
                int minZoom = getRequieredParameterInt(Requiered.LayerMin_ + (i + 1), docelem);
                int maxZoom = getRequieredParameterInt(Requiered.LayerMax_ + (i + 1), docelem);
                int divider = getOptionalParameterInt(Optional.LayerDivider_ + (i + 1), docelem);
                if (divider == -1) {
                    divider = 0;
                }

                final Layer level = new Layer(i, tilePixel, minZoom, maxZoom, divider);
                layers[i] = level;
            }
        }
        catch (Ptolemy3DConfigurationException ex) {
            // Abort the load process if some required parameter fails.
            logger.severe(ex.getMessage());
            return;
        }

        try {
            // Initial position of the camera.
            String orientation = getOptionalParameter(Optional.Orientation, docelem);
            if (orientation != null) {
                String[] values = orientation.split(",");
                if (values.length == 5) {
                    try {
                        double lat = Double.parseDouble(values[0]);
                        double lon = Double.parseDouble(values[1]);
                        double alt = Double.parseDouble(values[2]) / Unit.getCoordSystemRatio();
                        double dir = Double.parseDouble(values[3]);
                        double pitch = Double.parseDouble(values[4]);
                        initialCameraPosition = new Position(lat, lon, alt);
                        initialCameraDirection = dir;
                        initialCameraPitch = pitch;
                    }
                    catch (NumberFormatException e) {
                        String message = "Some of the values in the " + Optional.Orientation + " property are invalids.\nThe property will be ignored";
                        logger.warning(message);
                    }
                }
                else {
                    String message = "The number of values for the " + Optional.Orientation + " property does not correspond to (lat,lon,alt,dir,pitch). " + "You have specified a different number.\nThe property will be ignored";
                    logger.warning(message);
                }
            }
        }
        catch (Ptolemy3DConfigurationException ex) {
            // Abort the load process if some required parameter fails.
            logger.severe(ex.getMessage());
            return;
        }
        
        // Load other optionals
        try {
            useTIN = getOptionalParameterBoolean(Optional.TIN, docelem);
            follorDEM = getOptionalParameterBoolean(Optional.FollowDEM, docelem);

            // Other optional properties
            flightFrameDelay = getOptionalParameterInt(Optional.FlightFrameDelay, docelem);
            if (flightFrameDelay == -1) {
                flightFrameDelay = 1;
            }

            int horizon = getOptionalParameterInt(Optional.Horizon, docelem);
            if (horizon != -1) {
                sky.horizonAlt = horizon;
            }
            int maxalt = getOptionalParameterInt(Optional.MaxAltitude, docelem);
            if (maxalt != -1) {
                CameraMovement.MAXIMUM_ALTITUDE = maxalt;
            }

            // Background image
            backgroundImageUrl = getOptionalParameter(Optional.BackgroundImageUrl, docelem);
            if (backgroundImageUrl != null && !backgroundImageUrl.endsWith("?")) {
                backgroundImageUrl = backgroundImageUrl + "?";
            }

            String s;
            if ((s = getOptionalParameter(Optional.HeightScale, docelem)) != null) {
                landScape.setMaxColorHeight(Short.parseShort(s));
            }
            if ((s = getOptionalParameter(Optional.TileColor, docelem)) != null) {
                final StringTokenizer st = new StringTokenizer(s, ",");
                if (st.countTokens() > 2) {
                    landScape.getTileColor()[0] = (Float.parseFloat(st.nextToken()) / 255);
                    landScape.getTileColor()[1] = (Float.parseFloat(st.nextToken()) / 255);
                    landScape.getTileColor()[2] = (Float.parseFloat(st.nextToken()) / 255);
                }
            }
            if ((s = getOptionalParameter(Optional.BackgroundColor, docelem)) != null) {
                final StringTokenizer st = new StringTokenizer(s, ",");
                if (st.countTokens() > 2) {
                    backgroundColor[0] = (Float.parseFloat(st.nextToken()) / 255);
                    backgroundColor[1] = (Float.parseFloat(st.nextToken()) / 255);
                    backgroundColor[2] = (Float.parseFloat(st.nextToken()) / 255);
                }
            }
        }
        catch (Ptolemy3DConfigurationException ex) {
            // Abort the load process if some required parameter fails.
            logger.severe(ex.getMessage());
            return;
        }

        // Plugins
        try {
            int numPlugins = getOptionalParameterInt(Optional.NumPlugins, docelem);
            for (int p = 0; p < numPlugins; p++) {
                try {
                    plugins.addPlugin(getOptionalParameter(Optional.PluginType_ + (p + 1), docelem),
                    		getOptionalParameter(Optional.PluginParam_ + (p + 1), docelem));
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
    	
		final Applet applet = Ptolemy3D.getJavascript().getApplet();
		String parameter = applet.getParameter(name);

		if (parameter == null) {
			String message = "The requiered parameter '" + name + "' is missing.";
			throw new Ptolemy3DConfigurationException(message);
		}
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

        try {
            return Integer.parseInt(getRequieredParameter(name));
        }
        catch (NumberFormatException e) {
            String message = "The requiered parameter '" + name + "' is not a valid integer value.";
            throw new Ptolemy3DConfigurationException(message);
        }
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

        try {
            return Float.parseFloat(getRequieredParameter(name));
        }
        catch (NumberFormatException e) {
            String message = "The requiered parameter '" + name + "' is not a valid float value.";
            throw new Ptolemy3DConfigurationException(message);
        }
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

        try {
            NodeList nodeList = elem.getElementsByTagName(name);
            return  ((Element) nodeList.item(0)).getChildNodes().item(0).getNodeValue();
        }
        catch (Exception ex) {
            String message = "The requiered parameter '" + name + "' is missing.\nException message: " + ex.getMessage();
            throw new Ptolemy3DConfigurationException(message);
        }
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

        try {
            return Integer.parseInt(getRequieredParameter(name, elem));
        }
        catch (NumberFormatException e) {
            String message = "The requiered parameter '" + name + "' is not a valid integer value.";
            throw new Ptolemy3DConfigurationException(message);
        }
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

        try {
            return Float.parseFloat(getRequieredParameter(name, elem));
        }
        catch (NumberFormatException e) {
            String message = "The requiered parameter '" + name + "' is not a valid float value.";
            throw new Ptolemy3DConfigurationException(message);
        }
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
}
