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
package org.ptolemy3d.plugin;

import static org.ptolemy3d.Ptolemy3DConfiguration.EARTH_RADIUS;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.font.FntFont;
import org.ptolemy3d.font.FntFontRenderer;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.plugin.util.GMLDoc;
import org.ptolemy3d.plugin.util.XsltPOI;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.scene.Sky;
import org.ptolemy3d.util.ByteReader;
import org.ptolemy3d.util.PngDecoder;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.LatLonAlt;

public class POILabelPlugin implements Plugin
{
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;

	private boolean WFS = false;//use WFS or not;

	private String wfs_filter = null;
	private String typename = null;

	private String stylesheet = null;//TODO:need a default
	private XsltPOI xsltOutput;
	private String wfs_max_features = "20";


	//texture fonts
	public static String LabelFontFile = null;
	private FntFontRenderer label_font;
	private boolean labelTexSet = false;

	private boolean kanjiLabel = true;
	private float font_h =1.8f;
	private float font_w =1.3f;
	
	private int FEATURE_MIN_KYORI = 70;
	private int MAX_ALT = Integer.MAX_VALUE,  MIN_ALT = 0;
	private static final Font nicekanjifont = new Font("MS UI Gothic", Font.BOLD | Font.TRUETYPE_FONT, 27);
	private FontMetrics fmet;
	private boolean STATUS = true;
	private boolean pinpoint = false;
	private boolean jspquery = true;
	int features_id;
	private int[] icon_tex_id;
	private float[] icon_ratio;
	private int IconQueryWidth = 10000;
	private String IconGetUrl = "/";
	private String IconPrefix = "";
	private int[] icon_filter = null;
	private int feature_minx = Integer.MAX_VALUE,  feature_minz = Integer.MAX_VALUE,  feature_maxx = Integer.MIN_VALUE,  feature_maxz = Integer.MIN_VALUE;
	private double feature_center_x = 0,  feature_center_z = 0;
	private int numFeatures = 0;
	private String[] feature_label;
	private int[] feature_Id;
	private int[] feature_ObjectId;
	private String FEATURE_ENCODING = "UTF-8";
	private final int MAX_FEATURES_SHOW = 16;
	private int[] max_dist,  min_dist;
	private Color bg_color,  f_color,  border;
	private int numShowableFeatures = 0;
	private byte[] featuresByteDat;
	private BufferedImage FeatureBuffer = null;
	private Graphics graphics = null;
	private int[] feature_text_ix;
	private int[] stringWidths;
	private byte[] highlight;
	private byte[] distToSel;
	private Hashtable<String,String> hl_list;
    private double[][] feature_real_coord; // [index][x,y]
    private double[][] obj_end;
    private String LAYER = "";
    private String enc_layer = "";
    private String onclickF = "poiClicked";
    private int NumIcons = 0;
    private boolean resetFeatureText = false;
    private int loadGraphicRequestId;
    private int setGraphicRequestId;
    private boolean forceFeatureLoad;
    private boolean clearDepth;
    private byte[] imgdat = null;
    int r_size;
    private final byte HLT_RINGS = (byte) 1;
    private final byte HLT_BOX = (byte) 2;
    private int label_o = 0;
    private static final int VALIGN_ABOVE = (1 << 0);
    private static final int VALIGN_MIDDLE = (1 << 1);
    private static final int VALIGN_BELOW = (1 << 2);
    private static final int HALIGN_LEFT = (1 << 3);
    private static final int HALIGN_CENTER = (1 << 4);
    private static final int HALIGN_RIGHT = (1 << 5);
    private int centerPoint = -1;

    public POILabelPlugin(){}

    public void init(Ptolemy3D ptolemy)
    {
    	this.ptolemy = ptolemy;

    	features_id = -1;
    	featuresByteDat = new byte[512 * 512 * 4];
    	hl_list = new Hashtable<String,String>(5, 5);
    	loadGraphicRequestId = -1;
    	setGraphicRequestId = -1;
    }

    public void setPluginIndex(int index)
	{
	}
    public void setPluginParameters(String param)
    {
    	clearDepth = true;
    	StringTokenizer stok;

    	// set up defaults
    	MIN_ALT = 0;
    	MAX_ALT = Integer.MAX_VALUE;
    	label_o |= HALIGN_RIGHT;
    	label_o |= VALIGN_MIDDLE;
    	bg_color = setColor("255:255:255:0");
    	f_color = setColor("0:0:0");
    	border = setColor("255:255:255:0");

    	stok = new StringTokenizer(param, "***");
    	String parameters;
    	while (stok.hasMoreTokens())
    	{
    		parameters = stok.nextToken();
    		if (parameters.substring(0, 1).equals("P"))
    		{

    			StringTokenizer ptok = new StringTokenizer(parameters, " ");
    			String ky;
    			while (ptok.hasMoreTokens())
    			{
    				try
    				{
    					ky = ptok.nextToken();
    					if (ky.equalsIgnoreCase("-position"))
    					{
    						String position = ptok.nextToken().toUpperCase();
    						if (position.indexOf("A") != -1)
    						{
    							label_o |= VALIGN_ABOVE;
    						}
    						if (position.indexOf("M") != -1)
    						{
    							label_o |= VALIGN_MIDDLE;
    						}
    						if (position.indexOf("B") != -1)
    						{
    							label_o |= VALIGN_BELOW;
    						}
    						if (position.indexOf("L") != -1)
    						{
    							label_o |= HALIGN_LEFT;
    						}
    						if (position.indexOf("C") != -1)
    						{
    							label_o |= HALIGN_CENTER;
    						}
    						if (position.indexOf("R") != -1)
    						{
    							label_o |= HALIGN_RIGHT;
    						}
    					}
    					else if (ky.equalsIgnoreCase("-status"))
    					{
    						STATUS = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
    					}
    					else if (ky.equalsIgnoreCase("-clearDepth"))
    					{
    						clearDepth = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
    					}
    					else if (ky.equalsIgnoreCase("-pinpoint"))
    					{
    						pinpoint = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
    					}
    					else if (ky.equalsIgnoreCase("-jspquery"))
    					{
    						jspquery = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
    					}
    					else if (ky.equalsIgnoreCase("-numicons"))
    					{
    						NumIcons = Integer.parseInt(ptok.nextToken());
    						max_dist = new int[NumIcons];
    						min_dist = new int[NumIcons];

    						Arrays.fill(max_dist, Integer.MAX_VALUE);
    						Arrays.fill(min_dist, 0);
    					}
    					else if (ky.equalsIgnoreCase("-iconprefix"))
    					{
    						IconPrefix = ptok.nextToken();
    					}
    					else if (ky.equalsIgnoreCase("-mindistance"))
    					{
    						FEATURE_MIN_KYORI = Integer.parseInt(ptok.nextToken());
    					}
    					else if (ky.equalsIgnoreCase("-onclick"))
    					{
    						onclickF = ptok.nextToken();
    					}
    					else if (ky.equalsIgnoreCase("-encoding"))
    					{
    						FEATURE_ENCODING = ptok.nextToken();
    					}
    					else if (ky.equalsIgnoreCase("-bgcolor"))
    					{
    						bg_color = setColor(ptok.nextToken());
    					}
    					else if (ky.equalsIgnoreCase("-fontcolor"))
    					{
    						f_color = setColor(ptok.nextToken());
    					}
    					else if (ky.equalsIgnoreCase("-bordercolor"))
    					{
    						border = setColor(ptok.nextToken());
    					}
    					else if (ky.equalsIgnoreCase("-minalt"))
    					{
    						MIN_ALT = Integer.parseInt(ptok.nextToken());
    					}
    					else if (ky.equalsIgnoreCase("-maxalt"))
    					{
    						MAX_ALT = Integer.parseInt(ptok.nextToken());
    						if (MAX_ALT == 0)
    						{
    							MAX_ALT = Integer.MAX_VALUE;
    						}
    					}
    					else if (ky.equalsIgnoreCase("-jspurl"))
    					{
    						IconGetUrl = ptok.nextToken();
    					}
    					else if (ky.equalsIgnoreCase("-layer"))
    					{
    						LAYER = ptok.nextToken();
    						enc_layer = java.net.URLEncoder.encode(LAYER, "UTF-8");
    					}
    					else if (ky.equalsIgnoreCase("-iconquerywidth"))
    					{
    						IconQueryWidth = Integer.parseInt(ptok.nextToken());
    					}
    					else if (ky.equalsIgnoreCase("-iconmeta"))
    					{
    						StringTokenizer iconmeta = new StringTokenizer(ptok.nextToken(), ";");
    						StringTokenizer icondat;
    						while (iconmeta.hasMoreTokens())
    						{
    							icondat = new StringTokenizer(iconmeta.nextToken(), ":");
    							int i_id = Integer.parseInt(icondat.nextToken()) - 1;
    							max_dist[i_id] = (Integer.parseInt(icondat.nextToken()));
    							min_dist[i_id] = (Integer.parseInt(icondat.nextToken()));
    						}
    					}
    					else if (ky.equalsIgnoreCase("-kanjiLabel")){
    						kanjiLabel=(Integer.parseInt(ptok.nextToken()) == 1) ? true:false;
    					}
    					//adding wfs support
    					else if (ky.equalsIgnoreCase("WFS")){
    						WFS = true;
    					}
    					else if (ky.equalsIgnoreCase("-typename")){
    						typename = ptok.nextToken();
    					}
    					else if (ky.equalsIgnoreCase("-wfsurl")){
    						IconGetUrl = ptok.nextToken()+"&typename="+typename;
    					}
    					else if (ky.equalsIgnoreCase("-filter")){
    						wfs_filter = ptok.nextToken();
    					}
    					else if (ky.equalsIgnoreCase("-stylesheet")){
    						stylesheet = ptok.nextToken();
    					}
    					else if (ky.equalsIgnoreCase("-maxFeatures")){
    						wfs_max_features = ptok.nextToken();
    					}


    				}
    				catch (Exception e)
    				{
    				}

    			}
    		}
    		else
    		{  // support for old way
    			StringTokenizer ptok = new StringTokenizer(parameters, ",");
    			try
    			{
    				STATUS = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
    				IconGetUrl = ptok.nextToken();
    				if (IconGetUrl.indexOf("?") == -1)
    				{
    					IconGetUrl += "?";
    				}
    				enc_layer = java.net.URLEncoder.encode(ptok.nextToken(), "UTF-8");

    				IconQueryWidth = Integer.parseInt(ptok.nextToken());

    				NumIcons = Integer.parseInt(ptok.nextToken());
    				IconPrefix = ptok.nextToken();

    				FEATURE_MIN_KYORI = Integer.parseInt(ptok.nextToken());

    				FEATURE_ENCODING = ptok.nextToken();

    				bg_color = setColor(ptok.nextToken());
    				f_color = setColor(ptok.nextToken());
    				border = setColor(ptok.nextToken());

    				MIN_ALT = Integer.parseInt(ptok.nextToken());
    				MAX_ALT = Integer.parseInt(ptok.nextToken());
    				if (MAX_ALT == 0)
    				{
    					MAX_ALT = Integer.MAX_VALUE;
    				}
    				max_dist = new int[NumIcons];
    				min_dist = new int[NumIcons];

    				Arrays.fill(max_dist, Integer.MAX_VALUE);
    				Arrays.fill(min_dist, 0);

    				if (ptok.hasMoreTokens())
    				{
    					StringTokenizer iconmeta = new StringTokenizer(ptok.nextToken(), ";");
    					StringTokenizer icondat;
    					while (iconmeta.hasMoreTokens())
    					{
    						icondat = new StringTokenizer(iconmeta.nextToken(), ":");
    						int i_id = Integer.parseInt(icondat.nextToken()) - 1;
    						max_dist[i_id] = (Integer.parseInt(icondat.nextToken()));
    						min_dist[i_id] = (Integer.parseInt(icondat.nextToken()));
    					}
    				}

    			}
    			catch (Exception e)
    			{
    			}
    		}
    	}

    	icon_tex_id = new int[NumIcons];
    	icon_ratio = new float[NumIcons];
    	Arrays.fill(icon_tex_id, -1);
    }

    private final Color setColor(String col)
    {
    	StringTokenizer cstok;
    	cstok = new StringTokenizer(col, ":");
    	int r = Integer.parseInt(cstok.nextToken());
    	int g = Integer.parseInt(cstok.nextToken());
    	int b = Integer.parseInt(cstok.nextToken());
    	int a = 255;
    	if (cstok.hasMoreTokens())
    	{
    		a = Integer.parseInt(cstok.nextToken());
    	}
    	return new Color(r, g, b, a);
    }

    public void initGL(GL gl)
    {

    }

    public void motionStop(GL gl)
    {
    }

    public synchronized void draw(GL gl)
    {
    	final Landscape landscape = ptolemy.scene.landscape;
    	final Camera camera = ptolemy.camera;
    	final Sky sky = ptolemy.scene.sky;
    	final Ptolemy3DUnit unit = ptolemy.unit;

    	if ((!STATUS) || (camera.getVerticalAltitudeMeters() < MIN_ALT) || (camera.getVerticalAltitudeMeters() > MAX_ALT))  {
    		return;
    	}

    	gl.glEnable(GL.GL_BLEND);
    	gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
    	// clear the depth buffer, this will cause the POI to stack.
    	if (clearDepth)  {
    		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
    	}
    	if (sky.fogStateOn)  {
    		gl.glDisable(GL.GL_FOG);
    	}
    	int yoffset = 0;
    	double xoffset = 0;
    	if ((label_o & VALIGN_ABOVE) != 0) {
    		yoffset = 2;
    	}
    	if ((label_o & VALIGN_MIDDLE) != 0) {
    		yoffset = 0;
    	}
    	if ((label_o & VALIGN_BELOW) != 0) {
    		yoffset = -2;
    	}
    	if ((label_o & HALIGN_RIGHT) != 0) {
    		xoffset = 2;
    	}

    	if (resetFeatureText) {
    		setFeatureTex(gl);
    		resetFeatureText = false;
    	}

    	if ((numShowableFeatures > 0) && (features_id != -1))
    	{
    		double cp_X = 0, cp_Z = 0, cp_Y = 0;
    		final int raise = 50;

    		for (int i = 0; i < numFeatures; i++)
    		{
    			if (centerPoint == feature_ObjectId[i])
    			{
    				cp_Y = landscape.groundHeight(feature_real_coord[i][0], feature_real_coord[i][1], 0) + (raise * unit.coordSystemRatio);

    				cp_X = (obj_end[i][0] * (EARTH_RADIUS + cp_Y));
    				cp_Z = (obj_end[i][2] * (EARTH_RADIUS + cp_Y));
    				cp_Y = (obj_end[i][1] * (EARTH_RADIUS + cp_Y));
    				break;
    			}
    		}

    		double scale;
    		double ty, pixw;
    		double px, py, pz;

    		float texw;

    		for (int i = 0; i < numFeatures; i++)
    		{
    			if (feature_text_ix[i] != -1)
    			{
    				gl.glColor3f(1.0f, 1.0f, 1.0f);
    				if (((feature_Id[i] - 1) >= NumIcons) || (feature_ObjectId[i] == -1) || ((camera.getVerticalAltitudeMeters() > max_dist[feature_Id[i] - 1]) && (max_dist[feature_Id[i] - 1] != 0)) || (camera.getVerticalAltitudeMeters() < min_dist[feature_Id[i] - 1]))
    				{
    					continue;
    				}

    				gl.glPushMatrix();

    				ty = landscape.groundHeight(feature_real_coord[i][0], feature_real_coord[i][1], 0) + (raise * unit.coordSystemRatio);

    				px = obj_end[i][0] * (EARTH_RADIUS + ty);
    				py = obj_end[i][1] * (EARTH_RADIUS + ty);
    				pz = obj_end[i][2] * (EARTH_RADIUS + ty);

    				gl.glTranslated(px, py, pz);

    				gl.glMultMatrixd(camera.cameraMatInv.m, 0);

    				scale = Math3D.getScreenScaler(px, py, pz, FEATURE_MIN_KYORI);

    				gl.glScaled(scale, scale, scale);

    				// lift again if collission
    				gl.glTranslated(0, 2.2 * feature_real_coord[i][2], 0);

    				pixw = (stringWidths[i] + 5) / 16f;  // add 16 for bitmap, 4 for padding, and 1 for border.
    				if (pixw > 32)
    				{
    					pixw = 32;
    				}
    				texw = (float) pixw / 32;

    				if ((label_o & HALIGN_LEFT) != 0)
    				{
    					xoffset = -pixw;
    				}
    				else if ((label_o & HALIGN_CENTER) != 0)
    				{
    					xoffset = -(pixw / 2.0) + 1;
    				}
    				
    				/*Write the label to the screen
    				If not kanji, use the texture font*/
    				if(!kanjiLabel){
    					if(!labelTexSet) setLabelTex(gl);
    					gl.glDisable(GL.GL_DEPTH_TEST);
    					gl.glEnable(GL.GL_BLEND);
    					gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
    					//label background box
    					//I approximate the box width as 0.9*length of the label string
    					gl.glBindTexture(GL.GL_TEXTURE_2D,labelBgTex);
    					gl.glBegin(GL.GL_QUADS);
    						gl.glTexCoord2f(0, 0  );
    						gl.glVertex3d(xoffset , yoffset + 2 , 0);
    						gl.glTexCoord2f(0, 1  );
    						gl.glVertex3d(xoffset , yoffset , 0);
    						gl.glTexCoord2f(1, 1  );
    						gl.glVertex3d(xoffset + 0.9*feature_label[i].length(), yoffset , 0);
    						gl.glTexCoord2f(1, 0  );
    						gl.glVertex3d(xoffset + 0.9*feature_label[i].length() , yoffset + 2, 0);
    					gl.glEnd();

    					//TODO:we can make the labels vanish smoothly by making the alpha value a function of altitude
    					gl.glColor4f((float)(f_color.getRed())/255,(float)f_color.getGreen()/255,(float)f_color.getBlue()/255,3*MIN_ALT*(float)f_color.getAlpha()/(255*(float)ptolemy.camera.getVerticalAltitudeMeters()));
    					gl.glColor3f((float)(f_color.getRed())/255,(float)f_color.getGreen()/255,(float)f_color.getBlue()/255);
    					//gl.glPolygonOffset(1.0f, 3.0f);
    					label_font.bindTexture(gl);
    					float w = label_font.drawLeftToRight(gl, feature_label[i], xoffset, yoffset, font_h, font_w);

    					
    					//label border	
    					gl.glDisable(GL.GL_TEXTURE_2D);
    					int linew = 1;
    					gl.glColor4f((float)(border.getRed())/255,(float)border.getGreen()/255,(float)border.getBlue()/255,(float)border.getAlpha()/255);
    					gl.glLineWidth(linew);
    					gl.glBegin(GL.GL_LINE_LOOP);
    						gl.glVertex3d(xoffset-2.1, yoffset + 2, 0 );
    						gl.glVertex3d(xoffset-2.1 , yoffset , 0);
    						gl.glVertex3d(xoffset + 0.1 + 0.9*feature_label[i].length(), yoffset , 0);
    						gl.glVertex3d(xoffset + 0.1 + 0.9*feature_label[i].length(), yoffset + 2, 0);
    					gl.glEnd();


    					gl.glEnable(GL.GL_DEPTH_TEST);
    					gl.glDisable(GL.GL_BLEND);
    					gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);
    					gl.glColor3f(1.f,1.f,1.f);//reset colour
    					gl.glEnable(GL.GL_TEXTURE_2D);
    				}
    				else{
        				gl.glBindTexture(GL.GL_TEXTURE_2D, features_id);
        				gl.glBegin(GL.GL_TRIANGLE_STRIP);
        				gl.glTexCoord2f(0, ((float) (feature_text_ix[i] + 1) / 16));
        				gl.glVertex3d(xoffset, yoffset, 0);
        				gl.glTexCoord2f(texw, ((float) (feature_text_ix[i] + 1) / 16));
        				gl.glVertex3d(xoffset + pixw, yoffset, 0);
        				gl.glTexCoord2f(0, ((float) feature_text_ix[i] / 16));
        				gl.glVertex3d(xoffset, 2 + yoffset, 0);
        				gl.glTexCoord2f(texw, ((float) feature_text_ix[i] / 16));
        				gl.glVertex3d(xoffset + pixw, 2 + yoffset, 0);
        				gl.glEnd();
    				}

    				// draw icon
    				int fid = feature_Id[i] - 1;
    				if (fid < 0)
    				{
    					fid = 0;
    				}
    				if (icon_tex_id[fid] == -1)
    				{
    					loadGraphicRequestId = fid;
    				}
    				setGraphic(gl);
    				if (icon_tex_id[fid] != -1)
    				{
    					gl.glBindTexture(GL.GL_TEXTURE_2D, icon_tex_id[fid]);
    					gl.glBegin(GL.GL_TRIANGLE_STRIP);
    					gl.glTexCoord2f(0, icon_ratio[fid]);
    					gl.glVertex3d(0, 0, 0);
    					gl.glTexCoord2f(icon_ratio[fid], icon_ratio[fid]);
    					gl.glVertex3d(2, 0, 0);
    					gl.glTexCoord2f(0, 0);
    					gl.glVertex3d(0, 2, 0);
    					gl.glTexCoord2f(icon_ratio[fid], 0);
    					gl.glVertex3d(2, 2, 0);
    					gl.glEnd();
    				}

    				if (highlight[i] != (byte) 0)
    				{
    					drawHighlight(gl, highlight[i]);
    				}
    				gl.glPopMatrix();

    				if (pinpoint)
    				{
    					gl.glDisable(GL.GL_TEXTURE_2D);
    					gl.glLineWidth(1.f);
    					gl.glColor3f(0.f, 0.f, 0.f);
    					gl.glBegin(GL.GL_LINE_STRIP);
    					gl.glVertex3d(px, py, pz);

    					{
    						gl.glVertex3d(obj_end[i][0] * (EARTH_RADIUS + (ty - (raise * unit.coordSystemRatio))),
    								obj_end[i][1] * (EARTH_RADIUS + (ty - (raise * unit.coordSystemRatio))),
    								obj_end[i][2] * (EARTH_RADIUS + (ty - (raise * unit.coordSystemRatio))));
    					}
    					gl.glEnd();
    					gl.glEnable(GL.GL_TEXTURE_2D);
    				}

    				if ((distToSel[i] == (byte) 1) && (centerPoint != -1))
    				{
    					drawDistLine(gl, px, py, pz, cp_X, cp_Y, cp_Z);
    				}
    			}
    		} // end for loop
    	}

    	if (sky.fogStateOn)
    	{
    		gl.glEnable(GL.GL_FOG);
    	}
    	gl.glDisable(GL.GL_BLEND);
    	gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);

    }

    private final void drawDistLine(GL gl, double px, double py, double pz, double cp_X, double cp_Y, double cp_Z)
    {
    	final Camera camera = ptolemy.camera;
    	final Ptolemy3DUnit unit = ptolemy.unit;

    	double[] vec = new double[3];
    	vec[0] = cp_X - px;
    	vec[1] = cp_Y - py;
    	vec[2] = cp_Z - pz;

    	double dist = Math.sqrt((vec[0] * vec[0]) + (vec[1] * vec[1]) + (vec[2] * vec[2]));
    	vec[0] /= dist;
    	vec[1] /= dist;
    	vec[2] /= dist;

    	byte[] rw_dist = String.valueOf((int) ((dist / unit.coordSystemRatio) * 10)).getBytes();

    	// draw a line
    	gl.glDisable(GL.GL_TEXTURE_2D);
    	gl.glLineWidth(2.f);
    	gl.glColor3f(1.f, 0.76470588f, 0.25490196f);
    	gl.glBegin(GL.GL_LINE_STRIP);
    	gl.glVertex3d(px, py, pz);
    	gl.glVertex3d(cp_X, cp_Y, cp_Z);
    	gl.glEnd();

    	FntFontRenderer numericFont = ptolemy.fontNumericRenderer;
    	if (numericFont != null)
    	{
    		px = px + (vec[0] * dist / 2);
    		py = py + (vec[1] * dist / 2);
    		pz = pz + (vec[2] * dist / 2);
    		double scale = Math3D.getScreenScaler(px, py, pz, FEATURE_MIN_KYORI);

    		gl.glEnable(GL.GL_TEXTURE_2D);
    		// draw the label
    		gl.glColor3f(1.0f, 1.0f, 1.0f);
    		gl.glPushMatrix();
    		gl.glTranslated(px, py, pz);
    		gl.glMultMatrixd(camera.cameraMatInv.m, 0);
    		gl.glScaled(scale * 2, scale * 2, scale * 2);

    		numericFont.bindTexture(gl);

    		for (int u = 0; u < rw_dist.length - 1; u++)
    		{
    			numericFont.drawNumber(gl, rw_dist[u] - 48, u, 0, 0, 1, 1);
    		}
    		numericFont.drawNumber(gl, 10, rw_dist.length - 1, 0, 0, 1, 1);
    		numericFont.drawNumber(gl, rw_dist[rw_dist.length - 1] - 48, rw_dist.length, 0, 0, 1, 1);
    		gl.glPopMatrix();
    	}
    }

    public boolean pick(double[] intpt, double[][] ray)
    {
    	final Landscape landscape = ptolemy.scene.landscape;
    	final Ptolemy3DUnit unit = ptolemy.unit;
    	final Camera camera = ptolemy.camera;

    	if ((!STATUS) || (camera.getVerticalAltitudeMeters() < MIN_ALT) || (camera.getVerticalAltitudeMeters() > MAX_ALT)) {
    		return false;
    	}

    	double tx, ty, tz;
    	double[] tmpcoor = new double[3];

    	int yoffset = 0;
    	double xoffset = 0;
    	if ((label_o & VALIGN_ABOVE) != 0) {
    		yoffset = 2;
    	}
    	if ((label_o & VALIGN_MIDDLE) != 0) {
    		yoffset = 0;
    	}
    	if ((label_o & VALIGN_BELOW) != 0) {
    		yoffset = -2;
    	}
    	if ((label_o & HALIGN_RIGHT) != 0) {
    		xoffset = 2;
    	}
    	if (numShowableFeatures > 0)
    	{
    		intpt[0] = intpt[1] = intpt[2] = -999;
    		double ht, pixw, scale;
    		int raise = 50;

    		for (int i = 0; i < numFeatures; i++)
    		{

    			if (feature_text_ix[i] == -1)
    			{
    				continue;
    			}
    			if (((camera.getVerticalAltitudeMeters() > max_dist[feature_Id[i] - 1]) && (max_dist[feature_Id[i] - 1] != 0)) || (camera.getVerticalAltitudeMeters() < min_dist[feature_Id[i] - 1]))
    			{
    				continue;
    			}
    			ht = landscape.groundHeight(feature_real_coord[i][0], feature_real_coord[i][1], 0) + (raise * unit.coordSystemRatio);

    			tx = obj_end[i][0] * (EARTH_RADIUS + ht);
    			ty = obj_end[i][1] * (EARTH_RADIUS + ht);
    			tz = obj_end[i][2] * (EARTH_RADIUS + ht);

    			scale = Math3D.getScreenScaler(tx, ty, tz, FEATURE_MIN_KYORI);

    			pixw = (stringWidths[i] + 5) / 16f;  // add 16 for bitmap, 4 for padding, and 1 for border.
    			if (pixw > 32)
    			{
    				pixw = 32;
    			}
    			if ((label_o & HALIGN_LEFT) != 0)
    			{
    				xoffset = -pixw;
    			}
    			else if ((label_o & HALIGN_CENTER) != 0)
    			{
    				xoffset = -(pixw / 2.0) + 1;
    			}

    			double i_lift = feature_real_coord[i][2] * 2.2;

    			// label pick
    			tmpcoor[0] = xoffset * scale;
    			tmpcoor[1] = (yoffset + i_lift) * scale;
    			tmpcoor[2] = 0;
    			camera.cameraMat.transform(tmpcoor);
    			Math3D.setTriVert(0, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);
    			tmpcoor[0] = (pixw + xoffset) * scale;
    			tmpcoor[1] = (yoffset + i_lift) * scale;
    			tmpcoor[2] = 0;
    			camera.cameraMat.transform(tmpcoor);
    			Math3D.setTriVert(1, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);
    			tmpcoor[0] = xoffset * scale;
    			tmpcoor[1] = (yoffset + 2 + i_lift) * scale;
    			tmpcoor[2] = 0;
    			camera.cameraMat.transform(tmpcoor);
    			Math3D.setTriVert(2, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);
    			if (Math3D.pickTri(intpt, ray))
    			{
    				pluginClickEvent(i);
    				return true;
    			}
    			tmpcoor[0] = (pixw + xoffset) * scale;
    			tmpcoor[1] = (yoffset + 2 + i_lift) * scale;
    			tmpcoor[2] = 0;
    			camera.cameraMat.transform(tmpcoor);
    			Math3D.setTriVert(2, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);
    			if (Math3D.pickTri(intpt, ray))
    			{
    				pluginClickEvent(i);
    				return true;
    			}

    			// icon pick
    			tmpcoor[0] = 0;
    			tmpcoor[1] = i_lift * scale;
    			tmpcoor[2] = 0;
    			camera.cameraMat.transform(tmpcoor);
    			Math3D.setTriVert(0, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);
    			tmpcoor[0] = 2 * scale;
    			tmpcoor[1] = i_lift * scale;
    			tmpcoor[2] = 0;
    			camera.cameraMat.transform(tmpcoor);
    			Math3D.setTriVert(1, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);
    			tmpcoor[0] = 0;
    			tmpcoor[1] = (2 + i_lift) * scale;
    			tmpcoor[2] = 0;
    			camera.cameraMat.transform(tmpcoor);
    			Math3D.setTriVert(2, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);
    			if (Math3D.pickTri(intpt, ray))
    			{
    				pluginClickEvent(i);
    				return true;
    			}
    			tmpcoor[0] = 2 * scale;
    			tmpcoor[1] = (2 + i_lift) * scale;
    			tmpcoor[2] = 0;
    			camera.cameraMat.transform(tmpcoor);
    			Math3D.setTriVert(2, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);
    			if (Math3D.pickTri(intpt, ray))
    			{
    				pluginClickEvent(i);
    				return true;
    			}

    		} // end for loop
    	}
    	return false;
    }
    // draw a highlight at endpoint
    // hl_id is the highlight identifier.
    private final void drawHighlight(GL gl, int hl_id)
    {
    	gl.glDisable(GL.GL_TEXTURE_2D);
    	switch (hl_id)
    	{
    		case HLT_BOX:
    			gl.glLineWidth(2.f);
    			gl.glColor3f(0.1137f, 0.321f, 0.5568f);
    			gl.glBegin(GL.GL_LINE_STRIP);
    			gl.glVertex3d(0, 0, 0);
    			gl.glVertex3d(2, 0, 0);
    			gl.glVertex3d(2, 2, 0);
    			gl.glVertex3d(0, 2, 0);
    			gl.glVertex3d(0, 0, 0);
    			gl.glEnd();
    			break;
    		case HLT_RINGS:
    			int r_speed = 30;
    			double step = (Math3D.twoPi) / r_speed;

    			gl.glLineWidth(1.5f);
    			gl.glColor3f(0.1137f, 0.321f, 0.5568f);
    			gl.glPushMatrix();
    			gl.glTranslated(1, 1, 0);
    			//gl.glRotated(((System.currentTimeMillis()/40)%360),1,0,0);
    			gl.glBegin(GL.GL_LINE_STRIP);
    			for (int i = 0; i < r_speed; i++)
    			{
    				gl.glVertex3d(Math.cos(step * i) * 1.5, Math.sin(step * i) * 1.5, 0);
    			}
    			gl.glVertex3d(Math.cos(step * 0) * 1.5, Math.sin(step * 0) * 1.5, 0);
    			gl.glEnd();

    			gl.glColor3f(0.5f, 0.78f, 1.f);
    			//gl.glRotated(((System.currentTimeMillis()/35)%360),0,0,1);
    			gl.glBegin(GL.GL_LINE_STRIP);
    			for (int i = 0; i < r_speed; i++)
    			{
    				gl.glVertex3d(Math.cos(step * i) * 1.7, Math.sin(step * i) * 1.7, 0);
    			}
    			gl.glVertex3d(Math.cos(step * 0) * 1.7, Math.sin(step * 0) * 1.7, 0);
    			gl.glEnd();
    			gl.glPopMatrix();
    			break;
    	}
    	gl.glEnable(GL.GL_TEXTURE_2D);
    }

	private int tx_on_prev_load = -1;
	private int ty_on_prev_load = -1;
	// features are loaded in the range of the lowest layer.
	public final void loadFeaturesWFS(boolean force, Communicator JC) throws IOException{
		if(!jspquery) return;

		final Camera camera = ptolemy.camera;
		final LatLonAlt latLonAlt = camera.getLatAltLon();
		
		int tx,ty;
		tx = (int)latLonAlt.getLongitudeDD();
		ty = (int)latLonAlt.getLatitudeDD();

		if(tx==tx_on_prev_load && ty==ty_on_prev_load) return;//no need to reload features if focus hasn't changed

		tx_on_prev_load = tx;
		ty_on_prev_load = ty;

		int factor = ptolemy.unit.DD;

		float area_minx,area_maxz,area_maxx,area_minz;
		area_minx = tx - (IconQueryWidth/8);
		area_maxz = ty + (IconQueryWidth/8);
		area_maxx = tx + (IconQueryWidth/8);
		area_minz = ty - (IconQueryWidth/8);

		if(((area_minx <  feature_minx) || (area_maxx > feature_maxx) || (area_maxz > feature_maxz) || (area_minz < feature_minz)) || (force)){
			feature_minx = tx - (IconQueryWidth/2);
			feature_minz = ty - (IconQueryWidth/2);
			feature_maxx = tx + (IconQueryWidth/2);
			feature_maxz = ty + (IconQueryWidth/2);

			double x1 = (double)(feature_minx)/factor;
			double z1 = (double)(feature_minz)/factor;
			double x2 = (double)(feature_maxx)/factor;
			double z2 = (double)(feature_maxz)/factor;
			String wfs_url = IconGetUrl;
			JC.setServer("WFS_SERVER");//JC.server already exists in wfs_url but we still set this dummy value for check in JC.requestData() 
			if((wfs_filter==null)||(wfs_filter.length() == 0)) {
				wfs_url += "&bbox=" +x1+","+z1+","+x2+","+z2;
			}
			else{
				String bbox = "<BBox><PropertyName>DUMMY</PropertyName><gml:Box><gml:coordinates>"+x1+","+z1+"%20"+x2+","+z2+"</gml:coordinates></gml:Box></BBox>";
				wfs_url += "&filter=<Filter><And>" + wfs_filter + bbox + "</And></Filter>";
			}
			if(wfs_max_features!=null && wfs_max_features.length() != 0){
				wfs_url += "&maxFeatures="+wfs_max_features;
			}
			
			JC.requestData(wfs_url);
			
			
			GMLDoc gmlDoc = new GMLDoc(JC.getInputStream());
			xsltOutput = new XsltPOI(gmlDoc.getDocument(),stylesheet);
			//data = JC.getData();
			//ByteArrayInputStream bais= new ByteArrayInputStream(data);
			//xsltOutput = new XsltPOI(bais,stylesheet);
			//xsltOutput = new XsltPOI(JC.getInputStream(),stylesheet);//TO DO: why not working
			JC.setServer(ptolemy.configuration.server);//otherwise label icons not found later

			xsltOutput.transform();
			//WFS Error
			if(!(xsltOutput.getNodes())){//=="ServiceExceptionReport"){
				System.out.println("ServiceException");//: " + xsltOutput.getElementsByTagName("ServiceException").item(0).getFirstChild().getNodeValue());
				return;
			}
			if(xsltOutput==null){
				System.out.println("Xslt Output: null");
				return;
			}else{
				setParamsXSLT();
				int numFeatures = xsltOutput.getNumberOfFeatures();
				//System.out.println("NOF: " + numFeatures);
				double[][] feature_real_coord = new double[numFeatures][3]; // x,y,stack level
				String[] feature_label = new String[numFeatures];
				int[] feature_Id = new int[numFeatures];
				int[] feature_ObjectId = new int[numFeatures];
				int[] feature_text_ix = new int[numFeatures];
				int[] stringWidths = new int[numFeatures];
				byte[] highlight = new byte[numFeatures];
				byte[] distToSel = new byte[numFeatures];

				double[][] obj_end = new double[numFeatures][3];

				Arrays.fill(stringWidths , -1);
				for(int i=0;i<numFeatures;i++){
					distToSel[i] = (byte)0;
					feature_text_ix[i] = -1;
					feature_ObjectId[i] = Integer.parseInt(xsltOutput.getObjId(i));
					// icon id
					feature_Id[i] = Integer.parseInt(xsltOutput.getId(i));
					if(feature_Id[i] < 1) feature_Id[i] = 1;
					// label
					feature_label[i] = xsltOutput.getLabel(i);
					feature_real_coord[i][0] = (int)Double.valueOf(xsltOutput.getLon(i)).doubleValue()*factor;
					feature_real_coord[i][1] = (int)Double.valueOf(xsltOutput.getLat(i)).doubleValue()*factor;
					feature_real_coord[i][2] = 0;

					// if coords are too close to each other stack 
					if(i > 0){
						double idist;
						double tooclose = 200;
						for(int uu=i-1;uu>=0;uu--){
							idist = Math3D.distance2D(feature_real_coord[uu][0],  feature_real_coord[i][0]  ,feature_real_coord[uu][1] , feature_real_coord[i][1]);
							if(idist < tooclose) feature_real_coord[i][2]++;
						}
					}
					
					Math3D.setSphericalCoord(feature_real_coord[i][0] , feature_real_coord[i][1] , obj_end[i]);
				}

				this.numFeatures = numFeatures;
				this.feature_real_coord = feature_real_coord;
				this.feature_label = feature_label;
				this.feature_Id = feature_Id;
				this.feature_ObjectId = feature_ObjectId;
				this.feature_text_ix = feature_text_ix;
				this.stringWidths = stringWidths;
				this.highlight = highlight;
				this.obj_end = obj_end;
				this.distToSel = distToSel;


				setHlIcons();
			}
			return;
		}
	}

    // features are loaded in the range of the lowest layer.
    public final void loadFeaturesServlet(boolean force, Communicator JC) throws IOException
    {
    	if (!jspquery) {
    		return;
    	}

    	final Ptolemy3DUnit unit = ptolemy.unit;
    	final Camera camera = ptolemy.camera;
    	final LatLonAlt latLonAlt = camera.getLatAltLon();
    	
    	int tx, ty;
    	tx = (int) latLonAlt.getLongitudeDD();
    	ty = (int) latLonAlt.getLatitudeDD();
    	
		if(tx==tx_on_prev_load && ty==ty_on_prev_load) return;

		tx_on_prev_load = tx;
		ty_on_prev_load = ty;

    	float area_minx, area_maxz, area_maxx, area_minz;
    	area_minx = (tx - (IconQueryWidth / 8));
    	area_maxz = (ty + (IconQueryWidth / 8));
    	area_maxx = (tx + (IconQueryWidth / 8));
    	area_minz = (ty - (IconQueryWidth / 8));

    	if (((area_minx < feature_minx) || (area_maxx > feature_maxx) || (area_maxz > feature_maxz) || (area_minz < feature_minz)) || (force))
    	{
    		feature_minx = tx - (IconQueryWidth / 2);
    		feature_minz = ty - (IconQueryWidth / 2);
    		feature_maxx = tx + (IconQueryWidth / 2);
    		feature_maxz = ty + (IconQueryWidth / 2);

    		String request = IconGetUrl + "&LAYER=" + enc_layer + "&BBOX=" + feature_minx + "," + feature_minz + "," + feature_maxx + "," + feature_maxz + "&FACTOR=" + String.valueOf(unit.DD);
    		IO.printlnConnection(request);
    		JC.requestData(request);

    		byte[] data = JC.getData();
    		if(data != null) {
        		IO.printlnConnection(new String(data));

        		int[] cur = { 0 };

        		int numFeatures = ByteReader.readInt(data, cur);

        		double[][] feature_real_coord = new double[numFeatures][3]; // x,y,stack level
        		String[] feature_label = new String[numFeatures];
        		int[] feature_Id = new int[numFeatures];
        		int[] feature_ObjectId = new int[numFeatures];
        		int[] feature_text_ix = new int[numFeatures];
        		int[] stringWidths = new int[numFeatures];
        		byte[] highlight = new byte[numFeatures];
        		byte[] distToSel = new byte[numFeatures];

        		double[][] obj_end = new double[numFeatures][3];

        		Arrays.fill(stringWidths, -1);
        		int llen;
        		for (int i = 0; i < numFeatures; i++)
        		{
        			distToSel[i] = (byte) 0;
        			feature_text_ix[i] = -1;
        			feature_ObjectId[i] = ByteReader.readInt(data, cur);
        			// icon id
        			feature_Id[i] = ByteReader.readInt(data, cur);
        			if (feature_Id[i] < 1)
        			{
        				feature_Id[i] = 1;
        				// label
        			}
        			llen = ByteReader.readInt(data, cur);
        			if (llen > 0)
        			{
        				feature_label[i] = new String(data, cur[0], llen, FEATURE_ENCODING);
        				cur[0] += llen;
        			}
        			else
        			{
        				feature_label[i] = null;
        			}

        			feature_real_coord[i][0] = ByteReader.readInt(data, cur);
        			feature_real_coord[i][1] = ByteReader.readInt(data, cur);
        			feature_real_coord[i][2] = 0;
        			/* if coords are too close to each other stack */
        			if (i > 0)
        			{
        				double idist;
        				double tooclose = 200;
        				for (int uu = i - 1; uu >= 0; uu--)
        				{
        					idist = Math3D.distance2D(feature_real_coord[uu][0], feature_real_coord[i][0], feature_real_coord[uu][1], feature_real_coord[i][1]);
        					if (idist < tooclose)
        					{
        						feature_real_coord[i][2]++;
        					}
        				}
        			}

        			Math3D.setSphericalCoord(feature_real_coord[i][0], feature_real_coord[i][1], obj_end[i]);
        		}

        		this.numFeatures = numFeatures;
        		this.feature_real_coord = feature_real_coord;
        		this.feature_label = feature_label;
        		this.feature_Id = feature_Id;
        		this.feature_ObjectId = feature_ObjectId;
        		this.feature_text_ix = feature_text_ix;
        		this.stringWidths = stringWidths;
        		this.highlight = highlight;
        		this.obj_end = obj_end;
        		this.distToSel = distToSel;

        		setHlIcons();
    		}
    	}
    }

	//Set Parameters based on XSLT values
	private void setParamsXSLT(){
		MAX_ALT = Integer.parseInt(xsltOutput.getMaxAlt());
		if(MAX_ALT == 0) MAX_ALT = Integer.MAX_VALUE;
		MIN_ALT = Integer.parseInt(xsltOutput.getMinAlt());
		bg_color=setColor(xsltOutput.getBgColor());
		f_color=setColor(xsltOutput.getFColor());
		border=setColor(xsltOutput.getBorderColor());
		
		String position = xsltOutput.getPosition();
		if(position.indexOf("A") != -1) label_o |= VALIGN_ABOVE;
		if(position.indexOf("M") != -1) label_o |= VALIGN_MIDDLE;
		if(position.indexOf("B") != -1) label_o |= VALIGN_BELOW;
		if(position.indexOf("L") != -1) label_o |= HALIGN_LEFT;
		if(position.indexOf("C") != -1) label_o |= HALIGN_CENTER;
		if(position.indexOf("R") != -1) label_o |= HALIGN_RIGHT;
		
		kanjiLabel = (xsltOutput.getKanji().equalsIgnoreCase("true")) ? true:false;
		font_h=Float.valueOf(xsltOutput.getFontHeight()).floatValue();
		font_w=Float.valueOf(xsltOutput.getFontWidth()).floatValue();
		
		pinpoint = (xsltOutput.getPinpoint().equalsIgnoreCase("true")) ? true:false;
		FEATURE_MIN_KYORI = Integer.parseInt(xsltOutput.getMinDistance());

		NumIcons = Integer.parseInt(xsltOutput.getNumIcons());
		max_dist = new int[NumIcons];
		min_dist = new int[NumIcons];
		
		Arrays.fill(max_dist,Integer.MAX_VALUE);
		Arrays.fill(min_dist,0);

		IconPrefix = xsltOutput.getIconPrefix();
		icon_tex_id = new int[NumIcons];
		icon_ratio = new float[NumIcons];
		Arrays.fill(icon_tex_id, -1);
	}
	
    private void setGraphic(GL gl)
    {
    	if ((setGraphicRequestId != -1) && (imgdat != null))
    	{
    		icon_tex_id[setGraphicRequestId] = ptolemy.textureManager.load(gl, imgdat, r_size, r_size, GL.GL_RGBA, false, -1);
    		setGraphicRequestId = -1;
    	}
    }

    private void loadGraphic(int n, Communicator JC)
    {
    	byte[] data = null;

    	try
    	{
    		JC.requestData(IconPrefix + n + ".png");
    		int t = JC.getHeaderReturnCode();
    		if ((t == 200) || (t == 206)) {
    			data = JC.getData();
    		}
    		else {
    			JC.flushNotFound();
    		}
    	}
    	catch (Exception e)
    	{
    	}

    	int[] meta = new int[4];
    	byte[] idata = null;

    	if (data != null)
    	{
    		idata = PngDecoder.decode(data, meta);
    	}
    	else
    	{
    		idata = new byte[3];
    		idata[0] = idata[1] = idata[2] = (byte) 0;
    		meta[0] = meta[1] = 1;
    		meta[2] = 0;
    		meta[3] = 3;
    	}
    	int img_size = Math.max(meta[0], meta[1]);

    	r_size = Math.max(Math3D.nextTwoPow(meta[0]), Math3D.nextTwoPow(meta[1]));

    	icon_ratio[n] = (float) img_size / r_size;

    	int cty = meta[3];

    	byte[] img = new byte[(r_size * r_size) * 4];

    	int raster_offset = 0;
    	int offset = 0;
    	int a, b, w;
    	for (a = 0; a < r_size; a++)
    	{
    		for (b = 0; b < r_size; b++)
    		{
    			offset = (a * r_size * 4) + (b * 4);
    			raster_offset = (a * meta[0] * cty) + (b * cty);
    			if ((meta[0] > b) && (meta[1] > a))
    			{
    				for (w = 0; w < 3; w++)
    				{
    					img[offset + w] = idata[raster_offset + w];

    				}
    				img[offset + 3] = (cty == 4) ? idata[raster_offset + 3] : (byte) 255;
    			}
    			else
    			{
    				for (w = 0; w < 4; w++)
    				{
    					img[offset + w] = (byte) 0;
    				}
    			}
    		}
    	}

    	imgdat = img;

    	setGraphicRequestId = n;
    	loadGraphicRequestId = -1;
    }

    public final void prepareFeatures(boolean force)
    {
    	final Camera camera = ptolemy.camera;
    	final LatLonAlt latLonAlt = camera.getLatAltLon();

    	if ((latLonAlt.getLongitudeDD() == feature_center_x) && (latLonAlt.getLatitudeDD() == feature_center_z) && (!force)) {
    		return;
    	}

    	feature_center_x = latLonAlt.getLongitudeDD();
    	feature_center_z = latLonAlt.getLatitudeDD();

    	if (FeatureBuffer != null) {
    		FeatureBuffer.flush();
    	}
    	else {
    		FeatureBuffer = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    	}
    	graphics = FeatureBuffer.createGraphics();
    	if(xsltOutput == null){//come from vector plugin
			graphics.setColor(bg_color);
		}
		else{
			graphics.setColor(setColor(xsltOutput.getBgColor()));
		}
    	graphics.fillRect(0, 0, 512, 512);
    	graphics.setFont(nicekanjifont);
    	fmet = graphics.getFontMetrics();

    	if (feature_text_ix != null) {
    		Arrays.fill(feature_text_ix, -1);
    	}
    	int[] setIdStore = new int[MAX_FEATURES_SHOW];

    	double dist, shortest = Double.MAX_VALUE;
    	numShowableFeatures = 0;
    	int setId, t;
    	boolean inFilter;

    	while (numShowableFeatures < MAX_FEATURES_SHOW)
    	{
    		setId = -1;
    		shortest = Double.MAX_VALUE;
    		for (int i = 0; i < numFeatures; i++)
    		{

    			if (feature_ObjectId[i] == -1)
    			{
    				continue;
    			}
    			if (((camera.getVerticalAltitudeMeters() > max_dist[feature_Id[i] - 1]) && (max_dist[feature_Id[i] - 1] != 0)) || (camera.getVerticalAltitudeMeters() < min_dist[feature_Id[i] - 1]))
    			{
    				continue;
    			}
    			if (icon_filter != null)
    			{
    				inFilter = false;
    				for (t = 0; t < icon_filter.length; t++)
    				{
    					if (icon_filter[t] == feature_Id[i])
    					{
    						inFilter = true;
    						break;
    					}
    				}
    				if (!inFilter)
    				{
    					continue;
    				}
    			}

    			if (feature_text_ix[i] == -1)
    			{
    				dist = Math3D.distance2D(feature_center_x, feature_real_coord[i][0], feature_center_z, feature_real_coord[i][1]);
    				if (dist < shortest)
    				{
    					shortest = dist;
    					setId = i;
    				}
    			}
    		}
    		if (setId == -1)
    		{
    			break;
    		}
    		feature_text_ix[setId] = numShowableFeatures;
    		if (feature_label[setId] != null)
    		{
    			graphics.setColor(border);

    			if (stringWidths[setId] == -1)
    			{
    				stringWidths[setId] = fmet.stringWidth(feature_label[setId]);
    			}
    			graphics.drawRect(0, (32 * numShowableFeatures), stringWidths[setId] + 4, 31);
    			graphics.setColor(f_color);
    			graphics.drawString(feature_label[setId], 2, (32 * (numShowableFeatures + 1)) - 5);
    		}
    		setIdStore[numShowableFeatures++] = setId;
    	}
    	if (numShowableFeatures > 0)
    	{
    		int[] dat = ((DataBufferInt) (FeatureBuffer.getRaster().getDataBuffer())).getData();
    		int a, b;
    		int offset = 0;
    		for (int k = 0; k < numShowableFeatures; k++)
    		{
    			setId = setIdStore[k];
    			for (a = k * 32; a < ((k + 1) * 32); a++)
    			{
    				for (b = 0; b < 512; b++)
    				{
    					featuresByteDat[offset++] = (byte) (dat[(a * 512) + b] >> 16);
    					featuresByteDat[offset++] = (byte) (dat[(a * 512) + b] >> 8);
    					featuresByteDat[offset++] = (byte) (dat[(a * 512) + b] >> 0);
    					featuresByteDat[offset++] = (byte) (dat[(a * 512) + b] >> 24);
    				}
    			}
    		}
    		resetFeatureText = true;
    	}
    }

	int labelBgTex = -1;
	private final void setLabelTex(GL gl)
	{
		FntFont font;
		try {
			font = new FntFont(LabelFontFile);
		}
		catch(IOException e) {
			IO.printStackRenderer(e);
			font = null;
		}
		
		label_font = new FntFontRenderer(ptolemy, font);
		label_font.initGL(gl);
		labelTexSet = true;
		//label background texture
		byte[] data = new byte[4];
		data[0] = (byte)bg_color.getRed();
		data[1] = (byte)bg_color.getGreen();
		data[2] = (byte)bg_color.getBlue();
		data[3] = (byte)bg_color.getAlpha();
		labelBgTex = ptolemy.textureManager.load(gl, data, 1, 1, GL.GL_RGBA, false, -1);
	}
	
    private final void setFeatureTex(GL gl)
    {
    	if (features_id != -1)
    	{
    		gl.glBindTexture(GL.GL_TEXTURE_2D, features_id);
    		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 512, 512, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(featuresByteDat));
    	}
    	else
    	{
    		features_id = ptolemy.textureManager.load(gl, featuresByteDat, 512, 512, GL.GL_RGBA, true, -1);
    	}
    }

    public void destroyGL(GL gl)
    {
    	if (features_id != -1) {
			ptolemy.textureManager.unload(gl, features_id);
    		features_id = -1;
    	}
    	for (int h = 0; h < icon_tex_id.length; h++) {
    		if (icon_tex_id[h] != -1) {
    			ptolemy.textureManager.unload(gl, icon_tex_id[h]);
    			icon_tex_id[h] = -1;
    		}
    	}
    	if(label_font != null) {
        	label_font.destroyGL(gl);
    	}
    	ptolemy.textureManager.unload(gl, labelBgTex);

    	if (FeatureBuffer != null) {
    		FeatureBuffer.flush();
    		FeatureBuffer = null;
    	}
    	if (graphics != null) {
    		graphics.dispose();
    	}
    }

    private void setHlIcons()
    {
    	String val;
    	for (int i = 0; i < numFeatures; i++)
    	{
    		val = hl_list.get(String.valueOf(feature_ObjectId[i]));
    		if (val != null)
    		{
    			if (val.toUpperCase().equals("RINGS"))
    			{
    				highlight[i] = HLT_RINGS;
    			}
    			if (val.toUpperCase().equals("BOX"))
    			{
    				highlight[i] = HLT_BOX;
    			}
    			else
    			{
    				highlight[i] = HLT_RINGS;
    			}
    		}
    		else
    		{
    			highlight[i] = (byte) 0;
    		}
    	}
    }

    private void setIconIdFilter(String ilist)
    {
    	try
    	{
    		StringTokenizer stok = new StringTokenizer(ilist, ",");
    		icon_filter = new int[stok.countTokens()];
    		int i = 0;
    		while (stok.hasMoreTokens())
    		{
    			icon_filter[i++] = Integer.parseInt(stok.nextToken());
    		}
    	}
    	catch (Exception e)
    	{
    		icon_filter = null;
    	}
    	prepareFeatures(true);
    }

    private String colorString(Color c)
    {
    	return c.getRed() + ":" + c.getGreen() + ":" + c.getBlue() + ":" + c.getAlpha();
    }

    public String pluginAction(String commandname, String command_params)
    {
    	final Ptolemy3DUnit unit = ptolemy.unit;

    	icon_filter = null;
    	if (commandname.equalsIgnoreCase("setIconIdFilter"))
    	{
    		setIconIdFilter(command_params);
    	}
    	else if (commandname.equalsIgnoreCase("clearHighlights"))
    	{
    		hl_list.clear();
    		setHlIcons();
    	}
    	else if (commandname.equalsIgnoreCase("removeHighlightIds"))
    	{
    		StringTokenizer hltok = new StringTokenizer(command_params, ",");
    		while (hltok.hasMoreTokens())
    		{
    			hl_list.remove(hltok.nextToken());
    		}
    		setHlIcons();
    	}
    	else if (commandname.equalsIgnoreCase("addHighlightIds"))
    	{

    		StringTokenizer hltok = new StringTokenizer(command_params, ",");
    		String identifier = hltok.nextToken();
    		while (hltok.hasMoreTokens())
    		{
    			hl_list.put(hltok.nextToken(), identifier);
    		}
    		setHlIcons();
    	}
    	else if (commandname.equalsIgnoreCase("refresh"))
    	{
    		try
    		{
    			forceFeatureLoad = true;
    		}
    		catch (Exception e)
    		{
    		}
    	}
    	else if (commandname.equalsIgnoreCase("setCenterPoint"))
    	{
    		try
    		{
    			centerPoint = Integer.parseInt(command_params);
    			if ((centerPoint == -1) && (distToSel != null))
    			{
    				Arrays.fill(distToSel, (byte) 0);
    			}
    		}
    		catch (Exception e)
    		{
    		}
    	}
    	else if (commandname.equalsIgnoreCase("status"))
    	{
    		STATUS = (Integer.parseInt(command_params) == 1) ? true : false;
    		if (STATUS) {
    			forceFeatureLoad = true;
    		}
    	}
    	else if (commandname.equalsIgnoreCase("getLayerName"))
    	{
    		return LAYER;
    	}
    	else if (commandname.equalsIgnoreCase("getInfo"))
    	{
    		return "poi," + STATUS + "," + IconQueryWidth + "," + FEATURE_MIN_KYORI + "," + FEATURE_ENCODING + "," +
    		colorString(bg_color) + "," + colorString(f_color) + "," + colorString(border) + "," + MIN_ALT + "," + MAX_ALT;
    	}
    	else if (commandname.equalsIgnoreCase("setInfo"))
    	{
    		StringTokenizer stok = new StringTokenizer(command_params, ",");
    		STATUS = (Integer.parseInt(stok.nextToken()) == 1) ? true : false;
    		IconQueryWidth = Integer.parseInt(stok.nextToken());
    		FEATURE_MIN_KYORI = Integer.parseInt(stok.nextToken());
    		FEATURE_ENCODING = stok.nextToken();
    		bg_color = setColor(stok.nextToken());
    		f_color = setColor(stok.nextToken());
    		border = setColor(stok.nextToken());
    		MIN_ALT = Integer.parseInt(stok.nextToken());
    		MAX_ALT = Integer.parseInt(stok.nextToken());
    		if (MAX_ALT == 0)
    		{
    			MAX_ALT = Integer.MAX_VALUE;
    		}
    		forceFeatureLoad = true;
    	}
    	else if (commandname.equalsIgnoreCase("putIcon"))
    	{
    		int factor = unit.DD;
    		// gid,icon_id,label,x,y
    		StringTokenizer istok = new StringTokenizer(command_params, ",");

    		int gid = Integer.parseInt(istok.nextToken());
    		int pix = 0;

    		// check to see if this icon exists
    		boolean isNew = true;
    		if (feature_ObjectId != null)
    		{
    			for (pix = 0; pix < feature_ObjectId.length; pix++)
    			{
    				if (feature_ObjectId[pix] == gid)
    				{
    					isNew = false;
    					break;
    				}
    			}
    		}

    		// if new find a useable index
    		if (isNew)
    		{
    			checkArrays();
    			for (pix = 0; pix < feature_ObjectId.length; pix++)
    			{
    				if (feature_ObjectId[pix] == -1)
    				{
    					break;
    				}
    			}
    		}

    		distToSel[pix] = (byte) 0;
    		feature_text_ix[pix] = -1;
    		// gid
    		feature_ObjectId[pix] = gid;
    		//icon id
    		feature_Id[pix] = Integer.parseInt(istok.nextToken());
    		//label
    		feature_label[pix] = istok.nextToken();
    		//coords
    		feature_real_coord[pix][0] = (int) (Double.parseDouble(istok.nextToken()) * factor);
    		feature_real_coord[pix][1] = (int) (Double.parseDouble(istok.nextToken()) * factor);
    		feature_real_coord[pix][2] = 0;

    		/* if coords are too close to each other stack */
    		if (pix > 0)
    		{
    			double idist;
    			double tooclose = 200;
    			for (int uu = pix - 1; uu >= 0; uu--)
    			{
    				idist = Math3D.distance2D(feature_real_coord[uu][0], feature_real_coord[pix][0], feature_real_coord[uu][1], feature_real_coord[pix][1]);
    				if (idist < tooclose)
    				{
    					feature_real_coord[pix][2]++;
    				}
    			}
    		}

    		Math3D.setSphericalCoord(feature_real_coord[pix][0], feature_real_coord[pix][1], obj_end[pix]);
    		forceFeatureLoad = true;
    	}
    	else if (commandname.equalsIgnoreCase("removeIcon"))
    	{
    		int gid = Integer.parseInt(command_params);
    		int pix = 0;
    		if (feature_ObjectId != null)
    		{
    			for (pix = 0; pix < feature_ObjectId.length; pix++)
    			{
    				if (feature_ObjectId[pix] == gid)
    				{
    					feature_ObjectId[pix] = -1;
    					break;
    				}
    			}
    		}
    		forceFeatureLoad = true;
    	}
    	else if (commandname.equalsIgnoreCase("removeAll"))
    	{
    		if (feature_ObjectId != null)
    		{
    			Arrays.fill(feature_ObjectId, -1);
    			forceFeatureLoad = true;
    		}
    	}
    	return null;
    }

    private final void checkArrays()
    {
    	int grow = 10 + numFeatures;
    	if ((feature_ObjectId == null) || (feature_ObjectId.length <= (numFeatures + 1)))
    	{
    		double[][] feature_real_coord = new double[grow][3];
    		String[] feature_label = new String[grow];
    		int[] feature_Id = new int[grow];
    		int[] feature_ObjectId = new int[grow];
    		int[] feature_text_ix = new int[grow];
    		int[] stringWidths = new int[grow];
    		byte[] highlight = new byte[grow];
    		byte[] distToSel = new byte[grow];
    		double[][] obj_end = new double[grow][3];
    		if (this.obj_end != null)
    		{
    			System.arraycopy(this.obj_end, 0, obj_end, 0, numFeatures);
    		}
    		Arrays.fill(stringWidths, -1);
    		Arrays.fill(feature_ObjectId, -1);

    		if (this.feature_ObjectId != null)
    		{
    			// copy current data to new arrays
    			System.arraycopy(this.feature_real_coord, 0, feature_real_coord, 0, numFeatures);
    			System.arraycopy(this.feature_label, 0, feature_label, 0, numFeatures);
    			System.arraycopy(this.feature_Id, 0, feature_Id, 0, numFeatures);
    			System.arraycopy(this.feature_ObjectId, 0, feature_ObjectId, 0, numFeatures);
    			System.arraycopy(this.feature_text_ix, 0, feature_text_ix, 0, numFeatures);
    			System.arraycopy(this.stringWidths, 0, stringWidths, 0, numFeatures);
    			System.arraycopy(this.highlight, 0, highlight, 0, numFeatures);
    			System.arraycopy(this.distToSel, 0, distToSel, 0, numFeatures);
    		}
    		// set new arrays to active
    		this.feature_real_coord = feature_real_coord;
    		this.feature_label = feature_label;
    		this.feature_Id = feature_Id;
    		this.feature_ObjectId = feature_ObjectId;
    		this.feature_text_ix = feature_text_ix;
    		this.stringWidths = stringWidths;
    		this.highlight = highlight;
    		this.obj_end = obj_end;
    		this.distToSel = distToSel;
    	}
    	numFeatures = grow;
    }

    public boolean onPick(double[] intersectPoint)
    {
    	return false;
    }

    public void tileLoaderAction(Communicator JC) throws IOException
    {
    	if (!STATUS) {
    		return;
    	}
    	if(WFS) {
			loadFeaturesWFS(forceFeatureLoad,JC);
			//forceFeatureLoad = true;//WFS can take a while, make sure points are drawn
		}
		else{
			loadFeaturesServlet(forceFeatureLoad,JC);
		}
    	prepareFeatures(forceFeatureLoad);
    	forceFeatureLoad = false;

    	if (loadGraphicRequestId != -1) {
    		loadGraphic(loadGraphicRequestId, JC);
    	}
    }

    private final void pluginClickEvent(int id)
    {
    	try {
    		if (centerPoint != -1) {
    			distToSel[id] = (byte) 1;
    		}
    		else {
    			ptolemy.callJavascript(onclickF, String.valueOf(feature_ObjectId[id]), feature_label[id], null);
    		}
    	} catch (Exception e){}
    }

    public void reloadData()
    {
    }
}