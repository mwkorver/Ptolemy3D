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
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugins;
import org.ptolemy3d.scene.Sky;
import org.ptolemy3d.tile.Level;
import org.ptolemy3d.view.CameraMovement;
import org.ptolemy3d.view.LatLonAlt;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <H1>Configuration</H1> <BR>
 * Set of Ptolemy3D configuration parameters.<BR>
 * This is used for example to specify the map server, globe geometry settings
 * (number of levels)), the initial view position, ...<BR>
 * <BR>
 * List of parameters description can be found at: <a
 * href="http://ptolemy3d.org/wiki/PtolemyViewerConfig">Ptolemy3D wiki</a>.<BR>
 */
public class Ptolemy3DConfiguration {
	private static interface Requiered {
		/* Server */
		public final static String Server = "Server";
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
		public final static String LayerMIN_ = "LayerMIN_";
		public final static String LayerMAX_ = "LayerMAX_";
	}
	private static interface Optional {
		/* Server */
		public final static String MUrlAppend = "MUrlAppend";
		public final static String MHeaderKey = "MHeaderKey";
		public final static String MDataServer = "MDataServer";
		public final static String nMapDataSvr = "nMapDataSvr";
		public final static String isKeepAlive = "isKeepAlive";
		/* Unit */
		public final static String DDBuffer = "DDBuffer";	//Jerome: I think nothing will work if different to 1E6
		/* Initial position */
		public final static String Orientation = "Orientation";
		/* Layer */
		public final static String LayerDIVIDER_ = "LayerDIVIDER_";
		/* Plugin */
		public final static String NumPlugins = "NumPlugins";
		public final static String PluginType_ = "PluginType_";
		public final static String PluginParam_ = "PluginParam_";
		/* Misc */
		public final static String FOLLOWDEM = "FOLLOWDEM";
		public final static String HTSCALE = "HTSCALE";
		public final static String KEYBOARD = "KEYBOARD";
		public final static String TIN = "TIN";
		public final static String BackgroundImageUrl = "BackgroundImageUrl";
		public final static String SubTextures = "SubTextures";
		public final static String HORIZON = "HORIZON";
		public final static String MAX_ALT = "MAX_ALT";
		public final static String TILECOLOR = "TILECOLOR";
		public final static String BGCOLOR = "BGCOLOR";
		public final static String HUDBGRGBA = "HUDBGRGBA";
		public final static String FlightFrameDelay = "FlightFrameDelay";
	}
	private static interface Deprecated {
		public final static String FontFace = "FontFace";
		public final static String FontStyle = "FontStyle";
		public final static String bold = "bold";
		public final static String plain = "plain";
		public final static String italic = "italic";
		public final static String TextRGBA = "TextRGBA";
		public final static String NumberRGBA = "NumberRGBA";
		public final static String isOffscreenRendering = "isOffscreenRendering";
	}
	
	// Logger instance.
	private static final Logger logger = Logger.getLogger(Ptolemy3DConfiguration.class.getName());
	// Ptolemy3D Instance
	private final Ptolemy3D ptolemy;
	// Globe radius
	public final static int EARTH_RADIUS = 500000;	//6378136;
	// Clearing color (back ground color)
	public float[] backgroundColor = {0.541176470588f, 0.540176470588f, 0.540176470588f};
	// Initial position of the camera. Use to call FlightMovement.setOrientation
	public String initialPosition = null;

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

	/** @Deprecated Not used no more */
	private Font defaultFont = null;
	/** @Deprecated Not used no more */
	private Color numberColor = new Color(240, 240, 240);
	/** @Deprecated Not used no more */
	private Color textColor = new Color(250, 250, 250);
	/** @Deprecated Not supported no more */
	protected boolean offscreenRender = false;
	
	/**
	 * Creates a new configuration instance.
	 * 
	 * @param ptolemy
	 */
	public Ptolemy3DConfiguration(Ptolemy3D ptolemy) {
		this.ptolemy = ptolemy;
	}

	/**
	 * Parse an XML file and construct the document elements for Ptolemy3D.
	 */
	public final static Element buildXMLDocument(String xmlFile) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		Document document = null;
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			if (xmlFile.indexOf("http://") == -1) {
				document = builder.parse(new File(xmlFile));
			} else {
				document = builder.parse(xmlFile);
			}
		} catch (SAXException sxe) {
			Exception x = sxe;
			if (sxe.getException() != null) {
				x = sxe.getException();
			}
			IO.printStack(x);
		} catch (Exception e) {
			IO.printStack(e);
		}

		if (document == null) {
			return null;
		}
		return document.getDocumentElement();
	}

	/**
	 * Loads the configuration settings.
	 * 
	 * @param docelem
	 */
	public void loadSettings(Element docelem) throws Ptolemy3DException {
		loadSystemUnit(docelem);

		final CameraMovement cameraController = ptolemy.cameraController;
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Landscape landScape = ptolemy.scene.landscape;
		final Sky sky = ptolemy.scene.sky;
		final Plugins plugins = ptolemy.scene.plugins;

		// Server informations
		server = getRequieredParameter(Requiered.Server, docelem);

		int nDataServers = -1;
		try {
			// parameters for a clustered drill down setup.
			nDataServers = Integer.parseInt(getOptionalParameter(Optional.nMapDataSvr,
					docelem));
		} catch (Exception e) {

		}

		if (nDataServers <= 0) {
			headerKeys = new String[1];
			dataServers = new String[1];
			locations = new String[1][1];
			DEMLocation = new String[1][1];
			keepAlives = new boolean[1];
			urlAppends = new String[1];

			if ((dataServers[0] = getOptionalParameter(Optional.MDataServer, docelem)) == null) {
				dataServers[0] = server;
			}
			locations[0][0] = addTrailingChar(getRequieredParameter(Requiered.LocationRoot, docelem), "/");
			DEMLocation[0][0] = addTrailingChar(getRequieredParameter(Requiered.DEM, docelem), "/");
			keepAlives[0] = true;
			headerKeys[0] = getOptionalParameter(Optional.MHeaderKey, docelem);
			urlAppends[0] = getOptionalParameter(Optional.MUrlAppend, docelem);
		} else {
			headerKeys = new String[nDataServers];
			dataServers = new String[nDataServers];
			locations = new String[nDataServers][];
			DEMLocation = new String[nDataServers][];
			keepAlives = new boolean[nDataServers];
			urlAppends = new String[nDataServers];

			for (int i = 1; i <= nDataServers; i++) {
				dataServers[i - 1] = getRequieredParameter(Requiered.MapDataSvr + i, docelem);
				headerKeys[i - 1] = getOptionalParameter(Optional.MHeaderKey + i, docelem);
				urlAppends[i - 1] = getOptionalParameter(Optional.MUrlAppend + i, docelem);

				String temppm = getRequieredParameter(Requiered.MapJp2Store + i, docelem);
				if (temppm == null) {
					locations[i - 1] = null;
				} else {
					StringTokenizer ptok = new StringTokenizer(temppm, ",");

					locations[i - 1] = new String[ptok.countTokens()];
					for (int nfloc = 0; nfloc < locations[i - 1].length; nfloc++) {
						locations[i - 1][nfloc] = addTrailingChar(ptok
								.nextToken(), "/");
					}
				}

				temppm = getRequieredParameter(Requiered.MapDemStore + i, docelem);
				if (temppm == null) {
					DEMLocation[i - 1] = null;
				} else {
					StringTokenizer ptok = new StringTokenizer(temppm, ",");

					DEMLocation[i - 1] = new String[ptok.countTokens()];
					for (int nfloc = 0; nfloc < DEMLocation[i - 1].length; nfloc++) {
						DEMLocation[i - 1][nfloc] = addTrailingChar(ptok
								.nextToken(), "/");
					}
				}

				temppm = getOptionalParameter(Optional.isKeepAlive + i, docelem);
				if (temppm == null) {
					keepAlives[i - 1] = true;
				} else {
					keepAlives[i - 1] = (temppm.equals("0")) ? false : true;
				}

			}
		}

		// Area
		area = getRequieredParameter(Requiered.Area, docelem);

		// Background image
		backgroundImageUrl = getOptionalParameter(Optional.BackgroundImageUrl, docelem);
		if (backgroundImageUrl != null) {
			if (backgroundImageUrl.indexOf("?") == -1) {
				backgroundImageUrl = backgroundImageUrl + "?";
			}
		}

		// Colors and fonts
		try {
			String s;
			
			if ((s = getOptionalParameter(Optional.FlightFrameDelay, docelem)) != null) {
				flightFrameDelay = Integer.parseInt(s);
			}
			if ((s = getOptionalParameter(Optional.DDBuffer, docelem)) != null) {
				unit.DD = Integer.parseInt(s);
			}
			if ((s = getOptionalParameter(Optional.HORIZON, docelem)) != null) {
				sky.horizonAlt = Integer.parseInt(s);
			}
			if ((s = getOptionalParameter(Optional.MAX_ALT, docelem)) != null) {
				cameraController.maxAlt = Integer.parseInt(s);
			}
			if ((s = getOptionalParameter(Optional.TIN, docelem)) != null) {
				useTIN = s.equals("1") ? true : false;
			}
			if ((s = getOptionalParameter(Optional.SubTextures, docelem)) != null) {
				useSubtexturing = s.equals("1") ? true : false;
			}
			if ((s = getOptionalParameter(Optional.HUDBGRGBA, docelem)) != null) {
				hudBackgroundColor = getAwtColor(s);
			}
			if ((s = getOptionalParameter(Optional.FOLLOWDEM, docelem)) != null) {
				cameraController
						.setFollowDem(s.equals("1") ? true : false);
			}
			s = getOptionalParameter(Optional.KEYBOARD, docelem);
			cameraController.inputs.keyboardEnabled = ((s != null) && (s
					.equals("0"))) ? false : true;

			if ((s = getOptionalParameter(Optional.HTSCALE, docelem)) != null) {
				landScape.maxColorHeight = Short.parseShort(s);
			}
			if ((s = getOptionalParameter(Optional.TILECOLOR, docelem)) != null) {
				final StringTokenizer st = new StringTokenizer(s, ",");
				if(st.countTokens() > 2) {
					landScape.tileColor[0] = (Float.parseFloat(st.nextToken()) / 255);
					landScape.tileColor[1] = (Float.parseFloat(st.nextToken()) / 255);
					landScape.tileColor[2] = (Float.parseFloat(st.nextToken()) / 255);
				}
			}
			if ((s = getOptionalParameter(Optional.BGCOLOR, docelem)) != null) {
				final StringTokenizer st = new StringTokenizer(s, ",");
				if(st.countTokens() > 2) {
					backgroundColor[0] = (Float.parseFloat(st.nextToken()) / 255);
					backgroundColor[1] = (Float.parseFloat(st.nextToken()) / 255);
					backgroundColor[2] = (Float.parseFloat(st.nextToken()) / 255);
				}
			}
		}
		catch(Exception e) {
			logger.warning("Exception while loading optional parameter ("+e.getMessage()+")");
		}
		
		// Deprecated parameters
		try {
			String s;
			
			String fontFace = "Veranda";
			int font_style = Font.BOLD;
			if ((s = getDeprecatedParameter(Deprecated.FontFace, docelem)) != null) {
				fontFace = s;
			}
			if ((s = getDeprecatedParameter(Deprecated.FontStyle, docelem)) != null) {
				if (s.equalsIgnoreCase(Deprecated.bold)) {
					font_style = Font.BOLD;
				} else if (s.equalsIgnoreCase(Deprecated.plain)) {
					font_style = Font.PLAIN;
				} else if (s.equalsIgnoreCase(Deprecated.italic)) {
					font_style = Font.ITALIC;
				}
			}
			try {
				defaultFont = new Font(fontFace, font_style, 31);
			} catch (Exception e) {}

			if ((s = getDeprecatedParameter(Deprecated.NumberRGBA, docelem)) != null) {
				numberColor = getAwtColor(s);
			}
			if ((s = getDeprecatedParameter(Deprecated.TextRGBA, docelem)) != null) {
				textColor = getAwtColor(s);
			}
			s = getDeprecatedParameter(Deprecated.isOffscreenRendering, docelem);
			offscreenRender = ((s != null) && (s.equals("1"))) ? true
					: false;
		}
		catch(Exception e) {}

		initialPosition = getOptionalParameter(Optional.Orientation, docelem);
		if (initialPosition != null) {
			final StringTokenizer st = new StringTokenizer(initialPosition, ",");
			if (st.countTokens() >= 5) {
				double lon = Double.parseDouble(st.nextToken());
				double lat = Double.parseDouble(st.nextToken());
				double alt = Double.parseDouble(st.nextToken());
				double dir = Double.parseDouble(st.nextToken());
				double pitch = Double.parseDouble(st.nextToken());
				cameraController.setOrientation(LatLonAlt.fromDD(lat, lon, alt), dir, pitch);
			}
		}

		// Landscape levels (resolution levels)
		try {
			int NumLevels = Integer.parseInt(getRequieredParameter(Requiered.NumLayers, docelem));
			landScape.levels = new Level[NumLevels];
			for (int i = 0; i < NumLevels; i++) {
				int tilePixel = Integer.parseInt(getRequieredParameter(Requiered.LayerWidth_ + (i + 1), docelem));
				int minZoom = Integer.parseInt(getRequieredParameter(Requiered.LayerMIN_ + (i + 1), docelem));
				int maxZoom = Integer.parseInt(getRequieredParameter(Requiered.LayerMAX_ + (i + 1), docelem));
				String s = getOptionalParameter(Optional.LayerDIVIDER_ + (i + 1), docelem);
				int divider = (s != null) ? Integer.parseInt(s) : 0;

				final Level level = new Level(i, minZoom, maxZoom, tilePixel, divider);
				landScape.levels[i] = level;
			}
		} catch (Ptolemy3DException e) {
			logger.severe(e.getMessage());
			throw e;
		} catch (Exception e) {
			final String message = "Problem while reading a layer parameter ("+e.toString()+")";
			logger.severe(message);
			throw new Ptolemy3DException(message);
		}

		// Plugins
		int numPlugins = -1;
		try {
			numPlugins = Integer.parseInt(getOptionalParameter(Optional.NumPlugins,
					docelem));
		} catch (Exception ex) {}
		for (int p = 0; p < numPlugins; p++) {
			try {
				plugins.addPlugin(getOptionalParameter(Optional.PluginType_ + (p + 1), docelem),
						getOptionalParameter(Optional.PluginParam_ + (p + 1), docelem));
			} catch (Exception e) {
				logger.severe("Problem loading plugins: "+e.toString());
			}
		}
	}

	private void loadSystemUnit(Element docelem) {
		// Coordinate System Units
		int meterX, meterZ;
		float COORDSYS_RATIO;
		meterX = Integer.parseInt(getRequieredParameter(Requiered.CenterX, docelem));
		meterZ = Integer.parseInt(getRequieredParameter(Requiered.CenterY, docelem));
		COORDSYS_RATIO = (float) EARTH_RADIUS / 6378136;

		ptolemy.unit = new Ptolemy3DUnit(meterX, meterZ, COORDSYS_RATIO);
	}

	/* A requiered option. An exception is triggered if the parameter was not found. */
	private String getRequieredParameter(String name, Element elem) {
		final String parameter;
		if (elem != null) {
			String param;
			try {
				final NodeList nodeList = elem.getElementsByTagName(name);
				param = ((Element)nodeList.item(0)).getChildNodes().item(0).getNodeValue();
			}
			catch(Exception e) {
				param = null;
			}
			parameter = param;
		} else {
			final Applet applet = ptolemy.javascript.getApplet();
			parameter = applet.getParameter(name);
		}
		
		if(parameter == null) {
			throw new Ptolemy3DException("The requiered parameter '"+name+"' is missing");
		}
		return parameter;
	}
	/* An optional option. No exception. */
	private String getOptionalParameter(String name, Element elem) {
		try {
			return getRequieredParameter(name, elem);
		} catch (Exception e) {
			return null;
		}
	}
	/* A deprecated option. Print a warn if it is found, but no exception. */
	private String getDeprecatedParameter(String name, Element elem) {
		String parameter;
		try {
			parameter = getRequieredParameter(name, elem);
		} catch (Exception e) {
			parameter = null;
		}
		if(parameter != null) {
			logger.warning("The option '"+name+"' is now deprecated.");
		}
		return parameter;
	}

	private String addTrailingChar(String s, String tr) {
		if (!s.substring(s.length() - 1, s.length()).equals(tr)) {
			return s + tr;
		} else {
			return s;
		}
	}

	private Color getAwtColor(String col) {
		final StringTokenizer st = new StringTokenizer(col, ",");
		if(st.countTokens() > 2) {
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
