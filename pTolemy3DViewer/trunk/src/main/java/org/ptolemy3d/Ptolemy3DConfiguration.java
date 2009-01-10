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
import java.awt.Font;
import java.io.File;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugins;
import org.ptolemy3d.scene.Sky;
import org.ptolemy3d.tile.Level;
import org.ptolemy3d.view.CameraMovement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <H1>Configuration</H1>
 * <BR>
 * Set of Ptolemy3D configuration parameters.<BR>
 * This is used for example to specify the map server, globe geometry settings (number of levels)), the initial view position, ...<BR>
 * <BR>
 * List of parameters description can be found at: <a href="http://ptolemy3d.org/wiki/PtolemyViewerConfig">Ptolemy3D wiki</a>.<BR>
 */
public class Ptolemy3DConfiguration
{
	/**
	 * Parse an XML file and construct the document elements for Ptolemy3D.
	 */
	public final static Element buildXMLDocument(String xmlFile)
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		Document document = null;
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			if (xmlFile.indexOf("http://") == -1) {
				document = builder.parse(new File(xmlFile));
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
			IO.printStack(e);
		}

		if (document == null) {
			return null;
		}
		return document.getDocumentElement();
	}

	/* Contains all datas read from the configuration file. */
	/** Ptolemy3D Instance */
	private final Ptolemy3D ptolemy;

	/** Globe radius */
	public final static int EARTH_RADIUS = 500000;//6378136;

	/** Clearing color (back ground color) */
	public float[] backgroundColor = { 0.541176470588f, 0.540176470588f, 0.540176470588f };
	/** Initial position of the camera. Use to call FlightMovement.setOrientation */
	public String initialPosition = null;

	public String server = "";
	public String[] dataServers;
	public String[] urlAppends;
	public String[] headerKeys;
	public boolean[] keepAlives;
	public String[][] locations;
	public String area = "";
	public String DEMLocation[][];

	public Color numberColor = new Color(240, 240, 240);
	public Color textColor   = new Color(250, 250, 250);
	public Color hudBackgroundColor = new Color(20, 20, 20, 128);

	public Font defaultFont = null;
	public String backgroundImageUrl = null;
	public boolean offscreenRender = false;
	public boolean useSubtexturing = false;
	public boolean useTIN = false;
	public int flightFrameDelay = 1;

	public Ptolemy3DConfiguration(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
	}

	private void loadSystemUnit(Element docelem)
	{
		//Coordinate System Units
		int meterX, meterZ;
		float COORDSYS_RATIO;
		meterX = Integer.parseInt(getXmlParameter("CenterX", docelem));
		meterZ = Integer.parseInt(getXmlParameter("CenterY", docelem));
		COORDSYS_RATIO = (float) EARTH_RADIUS / 6378136;

		ptolemy.unit = new Ptolemy3DUnit(meterX, meterZ, COORDSYS_RATIO);
	}
	public void loadSettings(Element docelem)
	{
		loadSystemUnit(docelem);

		final CameraMovement cameraController = ptolemy.cameraController;
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Landscape landScape = ptolemy.scene.landscape;
		final Sky sky = ptolemy.scene.sky;
		final Plugins plugins = ptolemy.scene.plugins;

		//Read settings
		String fontface = "Veranda";
		int font_style = Font.BOLD;

		//Server informations
		server = getXmlParameter("Server", docelem);

		int nDataServers = -1;
		try {
			// parameters for a clustered drill down setup.
			nDataServers = Integer.parseInt(getXmlParameter("nMapDataSvr", docelem));
		} catch (Exception e) {

		}

		if (nDataServers <= 0)
		{
			headerKeys = new String[1];
			dataServers = new String[1];
			locations = new String[1][1];
			DEMLocation = new String[1][1];
			keepAlives = new boolean[1];
			urlAppends = new String[1];

			if ((dataServers[0] = getXmlParameter("MDataServer", docelem)) == null) {
				dataServers[0] = server;
			}
			locations[0][0] = addTrailingChar(getXmlParameter("LocationRoot", docelem), "/");

			DEMLocation[0][0] = addTrailingChar(getXmlParameter("DEM", docelem), "/");
			keepAlives[0] = true;
			headerKeys[0] = getXmlParameter("MHeaderKey", docelem);
			urlAppends[0] = getXmlParameter("MUrlAppend", docelem);
		}
		else
		{
			headerKeys = new String[nDataServers];
			dataServers = new String[nDataServers];
			locations = new String[nDataServers][];
			DEMLocation = new String[nDataServers][];
			keepAlives = new boolean[nDataServers];
			urlAppends = new String[nDataServers];

			for (int i = 1; i <= nDataServers; i++)
			{
				dataServers[i - 1] = getXmlParameter("MapDataSvr" + i, docelem);
				headerKeys[i - 1] = getXmlParameter("MHeaderKey" + i, docelem);
				urlAppends[i - 1] = getXmlParameter("MUrlAppend" + i, docelem);

				String temppm = getXmlParameter("MapJp2Store" + i, docelem);
				if (temppm == null) {
					locations[i - 1] = null;
				}
				else {
					StringTokenizer ptok = new StringTokenizer(temppm, ",");
					
					locations[i - 1] = new String[ptok.countTokens()];
					for (int nfloc = 0; nfloc < locations[i - 1].length; nfloc++) {
						locations[i - 1][nfloc] = addTrailingChar(ptok.nextToken(), "/");
					}
				}

				temppm = getXmlParameter("MapDemStore" + i, docelem);
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
				
				temppm = getXmlParameter("isKeepAlive" + i, docelem);
				if (temppm == null) {
					keepAlives[i - 1] = true;
				}
				else {
					keepAlives[i - 1] = (temppm.equals("0")) ? false : true;
				}

			}
		}

		area = getXmlParameter("Area", docelem);

		backgroundImageUrl = getXmlParameter("BackgroundImageUrl", docelem);
		if (backgroundImageUrl != null) {
			if (backgroundImageUrl.indexOf("?") == -1) {
				backgroundImageUrl = backgroundImageUrl + "?";
			}
		}


		try {
			String temppm;
			if ((temppm = getXmlParameter("TILECOLOR", docelem)) != null)
			{
				StringTokenizer stok = new StringTokenizer(temppm, ",");
				landScape.tileColor[0] = (Float.parseFloat(stok.nextToken()) / 255);
				landScape.tileColor[1] = (Float.parseFloat(stok.nextToken()) / 255);
				landScape.tileColor[2] = (Float.parseFloat(stok.nextToken()) / 255);
			}
			if ((temppm = getXmlParameter("BGCOLOR", docelem)) != null)
			{
				StringTokenizer stok = new StringTokenizer(temppm, ",");
				backgroundColor[0] = (Float.parseFloat(stok.nextToken()) / 255);
				backgroundColor[1] = (Float.parseFloat(stok.nextToken()) / 255);
				backgroundColor[2] = (Float.parseFloat(stok.nextToken()) / 255);
			}

			if ((temppm = getXmlParameter("FontFace", docelem)) != null)
			{
				fontface = temppm;
			}
			if ((temppm = getXmlParameter("FontStyle", docelem)) != null)
			{
				if (temppm.equalsIgnoreCase("bold"))
				{
					font_style = Font.BOLD;
				}
				else if (temppm.equalsIgnoreCase("plain"))
				{
					font_style = Font.PLAIN;
				}
				else if (temppm.equalsIgnoreCase("italic"))
				{
					font_style = Font.ITALIC;
				}
			}

			try {
				defaultFont = new Font(fontface, font_style, 31);
			} catch (Exception e) {
				IO.println("exception " + e.getMessage());
			}

			if ((temppm = getXmlParameter("NumberRGBA", docelem)) != null) {
				numberColor = getAwtColor(temppm);
			}
			if ((temppm = getXmlParameter("TextRGBA", docelem)) != null) {
				textColor = getAwtColor(temppm);
			}
			if ((temppm = getXmlParameter("HUDBGRGBA", docelem)) != null) {
				hudBackgroundColor = getAwtColor(temppm);
			}
			if ((temppm = getXmlParameter("FlightFrameDelay", docelem)) != null) {
				flightFrameDelay = Integer.parseInt(temppm);
			}
			if ((temppm = getXmlParameter("DDBuffer", docelem)) != null) {
				unit.DD = Integer.parseInt(temppm);
			}
			if ((temppm = getXmlParameter("HORIZON", docelem)) != null) {
				sky.horizonAlt = Integer.parseInt(temppm);
			}
			if ((temppm = getXmlParameter("MAX_ALT", docelem)) != null) {
				cameraController.maxAlt = Integer.parseInt(temppm);
			}
			if ((temppm = getXmlParameter("TIN", docelem)) != null) {
				useTIN = temppm.equals("1") ? true : false;
			}
			if ((temppm = getXmlParameter("SubTextures", docelem)) != null) {
				useSubtexturing = temppm.equals("1") ? true : false;
			}
			if ((temppm = getXmlParameter("FOLLOWDEM", docelem)) != null) {
				cameraController.setFollowDem(temppm.equals("1") ? true : false);
			}
			temppm = getXmlParameter("KEYBOARD", docelem);
			cameraController.inputs.keyboardEnabled = ((temppm != null) && (temppm.equals("0"))) ? false : true;

			//TODO Not used anymore
			//temppm = getXmlParameter("COMPONENT", docelem);
			//landScape.componentEnabled = ((temppm != null) && (temppm.equals("0"))) ? false : true;

			if ((temppm = getXmlParameter("HTSCALE", docelem)) != null) {
				landScape.maxColorHeight = Short.parseShort(temppm);
			}
			temppm = getXmlParameter("isOffscreenRendering", docelem);
			offscreenRender = ((temppm != null) && (temppm.equals("1"))) ? true : false;
		}
		catch (Exception e) {
		}

		initialPosition = getXmlParameter("Orientation", docelem);
		if (initialPosition != null) {
			StringTokenizer pstok = new StringTokenizer(initialPosition, ",");
			if (pstok.countTokens() >= 5) {
				cameraController.setOrientation(
						Double.parseDouble(pstok.nextToken()),
						Double.parseDouble(pstok.nextToken()),
						Double.parseDouble(pstok.nextToken()),
						Double.parseDouble(pstok.nextToken()),
						Double.parseDouble(pstok.nextToken()));
			}
		}

		//Landscape levels (resolution levels)
		int NumLevels = Integer.parseInt(getXmlParameter("NumLayers", docelem));
		landScape.levels = new Level[NumLevels];
		for (int i = 0; i < NumLevels; i++)
		{
			int tilePixel = Integer.parseInt(getXmlParameter("LayerWidth_" + (i + 1), docelem));
			int minZoom = Integer.parseInt(getXmlParameter("LayerMIN_" + (i + 1), docelem));
			int maxZoom = Integer.parseInt(getXmlParameter("LayerMAX_" + (i + 1), docelem));
			String temppm = getXmlParameter("LayerDIVIDER_" + (i + 1), docelem);
			int divider = (temppm != null) ? Integer.parseInt(temppm) : 0;

			Level level = new Level(i, minZoom, maxZoom, tilePixel, divider);
			landScape.levels[i] = level;
		}

		//Plugins
		int numplugins = Integer.parseInt(getXmlParameter("NumPlugins", docelem));
		for (int g = 0; g < numplugins; g++) {
			try {
				plugins.addPlugin(getXmlParameter("PluginType_" + (g + 1), docelem), getXmlParameter("PluginParam_" + (g + 1), docelem));
			}
			catch (Exception e) {
			}
		}
	}

	private String getXmlParameter(String name, Element elem)
	{
		try
		{
			if(elem != null)
			{
				return ((Element) (elem.getElementsByTagName(name).item(0))).getChildNodes().item(0).getNodeValue();
			}
			else
			{
				return ptolemy.javascript.getApplet().getParameter(name);
			}
		}
		catch (Exception e)
		{
			return null;
		}
	}
	private String addTrailingChar(String s, String tr)
	{
		if (!s.substring(s.length() - 1, s.length()).equals(tr))
		{
			return s + tr;
		}
		else
		{
			return s;
		}
	}
	private Color getAwtColor(String col)
	{
		try
		{
			StringTokenizer stok = new StringTokenizer(col, ",");
			int r = Integer.parseInt(stok.nextToken());
			int g = Integer.parseInt(stok.nextToken());
			int b = Integer.parseInt(stok.nextToken());
			if (stok.hasMoreTokens())
			{
				int a = Integer.parseInt(stok.nextToken());
				return new Color(r, g, b, a);
			}
			else
			{
				return new Color(r, g, b);
			}
		}
		catch (Exception e) {}

		return new Color(0, 0, 0, 0);
	}
}
