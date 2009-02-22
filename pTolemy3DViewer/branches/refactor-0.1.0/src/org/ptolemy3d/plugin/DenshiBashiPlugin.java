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
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.util.ByteReader;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.Position;

public class DenshiBashiPlugin implements Plugin
{
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;
    /** Plugin index*/
    private int pindex;
    /** */
    private GL gl;

    private int MAX_ALT = Integer.MAX_VALUE,  MIN_ALT = 0;
    private boolean STATUS = true;
    private int QueryWidth = 10000;
    private String DbashiGetUrl = "/";
    private String denchu;
    private String densen;
    private String kairo;
    private String userID = "0";    // for drawing text
    private BufferedImage FeatureBuffer = null;
    private Graphics graphics = null;
    private double feature_center_x = 0,  feature_center_z = 0;
    private final int MAX_FEATURES_SHOW = 16;
    private byte[] featuresByteDat = new byte[512 * 512 * 4];
    private boolean resetFeatureText = false;
    private int[] stringWidths = new int[MAX_FEATURES_SHOW];
    private int[] feature_text_ix;
    private FontMetrics fmet;
    private static final Font nicekanjifont = new Font("MS UI Gothic", Font.BOLD, 27);
    Color bg_color = new Color(255, 255, 255, 100);
    private int FEATURE_MIN_KYORI = 65;
    private int feature_minx = Integer.MAX_VALUE,  feature_minz = Integer.MAX_VALUE,  feature_maxx = Integer.MIN_VALUE,  feature_maxz = Integer.MIN_VALUE;
    private int[] feature_Id;
    private String[] feature_name;
    private Vector<Integer>[] feature_boxarray;
    private int[] numDensen;
    private int[][] denchu_to;
    private int[][] kairo_id;
    private float[][][] kairo_color;
    private double[][] feature_real_coord; // [index][x,y]
    private double[][] obj_axis;  // these two are for sherical model only
    private double[] obj_angle;
    private double[][] obj_end;
    private int numShowableFeatures = 0;
    private double ticker = 0;
    private int kairo_highlight_id = -1;
    private int NumBashi = 0;
    private int features_id = -1;
    private boolean forceFeatureLoad;
    int BOX_ARRAY_MAX = 10;

    public DenshiBashiPlugin(){}
    public void init(Ptolemy3D ptolemy)
    {
    	this.ptolemy = ptolemy;
    }

    public void setPluginIndex(int index)
	{
    	this.pindex = index;
	}
    public void setPluginParameters(String param)
    {
    	StringTokenizer stok = new StringTokenizer(param);

    	try
    	{
    		String ky;
    		while (stok.hasMoreTokens())
    		{
    			ky = stok.nextToken();

    			if (ky.equalsIgnoreCase("-qw"))
    			{
    				QueryWidth = Integer.parseInt(stok.nextToken());
    			}
    			else if (ky.equalsIgnoreCase("-status"))
    			{
    				STATUS = (Integer.parseInt(stok.nextToken()) == 1) ? true : false;
    			}
    			else if (ky.equalsIgnoreCase("-denchu"))
    			{
    				denchu = java.net.URLEncoder.encode(stok.nextToken(), "UTF-8");
    			}
    			else if (ky.equalsIgnoreCase("-densen"))
    			{
    				densen = java.net.URLEncoder.encode(stok.nextToken(), "UTF-8");
    			}
    			else if (ky.equalsIgnoreCase("-kairo"))
    			{
    				kairo = java.net.URLEncoder.encode(stok.nextToken(), "UTF-8");
    			}
    			else if (ky.equalsIgnoreCase("-maxscale"))
    			{
    				MAX_ALT = Integer.parseInt(stok.nextToken());
    			}
    			else if (ky.equalsIgnoreCase("-minscale"))
    			{
    				MIN_ALT = Integer.parseInt(stok.nextToken());
    			}
    			else if (ky.equalsIgnoreCase("-labelscale"))
    			{
    				FEATURE_MIN_KYORI = Integer.parseInt(stok.nextToken());
    			}
    			else if (ky.equalsIgnoreCase("-jsp"))
    			{
    				DbashiGetUrl = stok.nextToken();
    			}
    			else if (ky.equalsIgnoreCase("-user"))
    			{
    				userID = stok.nextToken();
    			}
    		}

    	}
    	catch (Exception e)
    	{
    	}
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

    	if ((!STATUS) || (camera.getVerticalAltitudeMeters() < MIN_ALT) || (camera.getVerticalAltitudeMeters() > MAX_ALT)) {
    		return;
    	}

    	this.gl = gl;

    	gl.glShadeModel(GL.GL_SMOOTH);
    	gl.glEnable(GL.GL_BLEND);
    	gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

    	gl.glDisable(GL.GL_TEXTURE_2D);

    	//Enable light
    	gl.glEnable(GL.GL_LIGHT0);
    	gl.glEnable(GL.GL_LIGHT1);
    	gl.glEnable(GL.GL_LIGHTING);

    	if (resetFeatureText) {
    		setFeatureTex();
    		resetFeatureText = false;
    	}

    	double ty;

    	for (int i = 0; i < NumBashi; i++)
    	{
    		gl.glPushMatrix();
    		ty = landscape.groundHeight(feature_real_coord[i][0], feature_real_coord[i][1], 0);
			// translate to top of earth, then rotate to position.
			gl.glRotated(obj_angle[i], obj_axis[i][0], obj_axis[i][1], obj_axis[i][2]);
			gl.glTranslated(0, EARTH_RADIUS + ty, 0);
    		// draw the object
    		drawBashiBase();
    		drawBashiBoxes(i);
    		drawPowerLines(i);

    		gl.glPopMatrix();
    	}

    	gl.glDisable(GL.GL_LIGHT0);
    	gl.glDisable(GL.GL_LIGHT1);
    	gl.glDisable(GL.GL_LIGHTING);


    	gl.glEnable(GL.GL_TEXTURE_2D);
    	gl.glColor3f(1.0f, 1.0f, 1.0f);
    	// text labels

    	// face labels, use a temp mat so we don't have threading issues.
//  	cameraMat.copyTo(unit.renderTmpMat);	//Done in the core
//  	unit.renderTmpMat.invert(unit.renderCloneMat);
//  	for (int col = 0; col < 3; col++)
//  	{
//  	for (int row = 0; row < 3; row++)
//  	{
//  	M[col * 4 + row] = com.aruke.ptolemy.scene.renderTmpMat.m[col][row];
//  	}
//  	}

    	double dx, dy, dz, scale, feature_kyori;

    	for (int i = 0; i < NumBashi; i++)
    	{
    		gl.glPushMatrix();
    		ty = landscape.groundHeight(feature_real_coord[i][0], feature_real_coord[i][1], 0) + 15;

			gl.glTranslated(obj_end[i][0] * (EARTH_RADIUS + ty), obj_end[i][1] * (EARTH_RADIUS + ty), obj_end[i][2] * (EARTH_RADIUS + ty));
    		gl.glMultMatrixd(camera.cameraMatInv.m, 0);	//FIXME Change not tested

			dx = camera.cameraPos[0] - (obj_end[i][0] * (EARTH_RADIUS + ty));
			dy = camera.cameraPos[1] - (obj_end[i][1] * (EARTH_RADIUS + ty));
			dz = camera.cameraPos[2] - (obj_end[i][2] * (EARTH_RADIUS + ty));

			scale = 1;
    		feature_kyori = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    		if (feature_kyori > FEATURE_MIN_KYORI)
    		{
    			dx /= feature_kyori;
    			dy /= feature_kyori;
    			dz /= feature_kyori;
    			scale = (feature_kyori * Math.cos(Math3D.angle3dvec(
    					camera.cameraMat.m[0][2], camera.cameraMat.m[1][2], camera.cameraMat.m[2][2],
    					dx, dy, dz, false))) / FEATURE_MIN_KYORI;
    		}

    		gl.glScaled(scale, scale, scale);

    		drawBashiName(i);

    		gl.glPopMatrix();
    	}
    	ticker += 0.15;
    	gl.glShadeModel(GL.GL_FLAT);
    	gl.glDisable(GL.GL_BLEND);
    	gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);

    	this.gl = null;
    }

    private void drawBashiName(int i)
    {
    	if ((feature_text_ix[i] == -1) || (features_id == -1))
    	{
    		return;
    	}
    	gl.glBindTexture(GL.GL_TEXTURE_2D, features_id);

    	float pixw = (stringWidths[feature_text_ix[i]] + 5) / 16f;  // add 16 for bitmap, 4 for padding, and 1 for border.
    	if (pixw > 32)
    	{
    		pixw = 32;
    	}
    	float texw = pixw / 32;

    	gl.glBegin(GL.GL_TRIANGLE_STRIP);
    	gl.glTexCoord2f(0, ((float) (feature_text_ix[i] + 1) / 16));
    	gl.glVertex3d(0, 0, 0);

    	gl.glTexCoord2f(texw, ((float) (feature_text_ix[i] + 1) / 16));
    	gl.glVertex3d(pixw, 0, 0);

    	gl.glTexCoord2f(0, ((float) feature_text_ix[i] / 16));
    	gl.glVertex3d(0, 2, 0);

    	gl.glTexCoord2f(texw, ((float) feature_text_ix[i] / 16));
    	gl.glVertex3d(pixw, 2, 0);
    	gl.glEnd();
    }

    private void drawPowerLines(int index)
    {

    	if (numDensen[index] <= 0)
    	{
    		return;
    	}
    	float[] col =
    	{
    			0, 0, 0
    	};

    	int HT = 15;
    	int denchu_id;
    	for (int i = 0; i < numDensen[index]; i++)
    	{
    		denchu_id = -1;
    		for (int j = 0; j < NumBashi; j++)
    		{
    			if (feature_Id[j] == denchu_to[index][i])
    			{
    				denchu_id = j;
    				break;
    			}
    		}

    		if (denchu_id != -1)
    		{




    			double tx, tz;

    			///////////  draw the power line ////////////////
    			if (kairo_highlight_id == kairo_id[index][i])
    			{
    				System.arraycopy(kairo_color[index][i], 0, col, 0, 3);
    				gl.glLineWidth(2.2f);
    			}
    			else
    			{
    				col[0] = col[1] = col[2] = 0;
    				gl.glLineWidth(1);
    			}
    			gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, col, 0);
    			gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, col, 0);

    			gl.glBegin(GL.GL_LINES);
    			gl.glVertex3d(0, HT, 0);
    			tx = feature_real_coord[denchu_id][0] - feature_real_coord[index][0];
    			tz = feature_real_coord[denchu_id][1] - feature_real_coord[index][1];
    			gl.glVertex3d(tx, HT, tz);
    			gl.glEnd();

    			double mag1 = Math.sqrt((tx * tx) + (tz * tz));
    			tx /= mag1;
    			tz /= mag1;
    			System.arraycopy(kairo_color[index][i], 0, col, 0, 3);
    			gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, col, 0);
    			gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, col, 0);
    			gl.glBegin(GL.GL_TRIANGLES);
    			double end = (ticker + kairo_id[index][i]) % mag1;
    			float wingspan = 0.4f;

    			gl.glVertex3d(tx * (end), HT, tz * (end));
    			gl.glVertex3d(tx * (end - 0.5), HT + wingspan, tz * (end - 0.5));
    			gl.glVertex3d(tx * (end - 0.5), HT - wingspan, tz * (end - 0.5));

    			gl.glVertex3d(tx * (end), HT, tz * (end));
    			gl.glVertex3d(tx * (end - 0.5), HT - wingspan, tz * (end - 0.5));
    			gl.glVertex3d(tx * (end - 0.5), HT + wingspan, tz * (end - 0.5));

    			// to get cross use perp vector
    			gl.glVertex3d(tx * (end), HT, tz * (end));
    			gl.glVertex3d(tx * (end - 0.5) + (-tz * wingspan), HT, tz * (end - 0.5) + (tx * wingspan));
    			gl.glVertex3d(tx * (end - 0.5) + (-tz * -wingspan), HT, tz * (end - 0.5) + (tx * -wingspan));

    			gl.glVertex3d(tx * (end), HT, tz * (end));
    			gl.glVertex3d(tx * (end - 0.5) + (-tz * -wingspan), HT, tz * (end - 0.5) + (tx * -wingspan));
    			gl.glVertex3d(tx * (end - 0.5) + (-tz * wingspan), HT, tz * (end - 0.5) + (tx * wingspan));

    			gl.glEnd();
    		}
    	}
    }

    private void drawBashiBoxes(int index)
    {

    	if ((feature_boxarray[index] == null) || (feature_boxarray[index].size() < 1))
    	{
    		return;
    	}
    	float[] col =
    	{
    			0, 0, 0
    	};
    	double tx = 0, ty = 0;
    	float box_width = 0.5f, box_ht = 0.7f, box_depth = 0.8f;

    	for (int i = 0; i < feature_boxarray[index].size(); i++)
    	{
    		switch (feature_boxarray[index].get(i))
    		{
    			case 1:
    				col[0] = 0f;
    				col[1] = 0.f;
    				col[2] = 1.f;
    				tx = -1;
    				ty = 1;
    				break;
    			case 2:
    				col[0] = 0f;
    				col[1] = 1.f;
    				col[2] = 0.f;
    				tx = 1;
    				ty = 1;
    				break;
    			case 3:
    				col[0] = 1f;
    				col[1] = 0.f;
    				col[2] = 0.f;
    				tx = -1;
    				ty = -1;
    				break;
    			case 4:
    				col[0] = 1.0f;
    				col[1] = 0f;
    				col[2] = 1f;
    				tx = 1;
    				ty = -1;
    				break;
    		}

    		gl.glPushMatrix();
    		gl.glTranslated(tx, ty + 10, 0);

    		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, col, 0);
    		col[0] /= 1.5;
    		col[1] /= 1.5;
    		col[2] /= 1.5;
    		gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, col, 0);

    		gl.glBegin(GL.GL_QUADS);
    		gl.glNormal3d(0, 1, 0);
    		gl.glVertex3f(box_width, box_ht, -box_depth);	//Top Right Of The Quad (Top)
    		gl.glVertex3f(-box_width, box_ht, -box_depth);	//Top Left Of The Quad (Top)
    		gl.glVertex3f(-box_width, box_ht, box_depth);	//Bottom Left Of The Quad (Top)
    		gl.glVertex3f(box_width, box_ht, box_depth);	//Bottom Right Of The Quad (Top)
    		gl.glNormal3d(0, -1, 0);
    		gl.glVertex3f(box_width, -box_ht, box_depth);	//Top Right Of The Quad (Bottom)
    		gl.glVertex3f(-box_width, -box_ht, box_depth);	//Top Left Of The Quad (Bottom)
    		gl.glVertex3f(-box_width, -box_ht, -box_depth);	//Bottom Left Of The Quad (Bottom)
    		gl.glVertex3f(box_width, -box_ht, -box_depth);	//Bottom Right Of The Quad (Bottom)
    		gl.glNormal3d(0, 0, -1);
    		gl.glVertex3f(box_width, box_ht, box_depth);	//Top Right Of The Quad (Front)
    		gl.glVertex3f(-box_width, box_ht, box_depth);	//Top Left Of The Quad (Front)
    		gl.glVertex3f(-box_width, -box_ht, box_depth);	//Bottom Left Of The Quad (Front)
    		gl.glVertex3f(box_width, -box_ht, box_depth);	//Bottom Right Of The Quad (Front)
    		gl.glNormal3d(0, 0, 1);
    		gl.glVertex3f(box_width, -box_ht, -box_depth);	//Top Right Of The Quad (Back)
    		gl.glVertex3f(-box_width, -box_ht, -box_depth);	//Top Left Of The Quad (Back)
    		gl.glVertex3f(-box_width, box_ht, -box_depth);	//Bottom Left Of The Quad (Back)
    		gl.glVertex3f(box_width, box_ht, -box_depth);	//Bottom Right Of The Quad (Back)
    		gl.glNormal3d(-1, 0, 0);
    		gl.glVertex3f(-box_width, box_ht, box_depth);	//Top Right Of The Quad (Left)
    		gl.glVertex3f(-box_width, box_ht, -box_depth);	//Top Left Of The Quad (Left)
    		gl.glVertex3f(-box_width, -box_ht, -box_depth);	//Bottom Left Of The Quad (Left)
    		gl.glVertex3f(-box_width, -box_ht, box_depth);	//Bottom Right Of The Quad (Left)
    		gl.glNormal3d(1, 0, 0);
    		gl.glVertex3f(box_width, box_ht, -box_depth);	//Top Right Of The Quad (Right)
    		gl.glVertex3f(box_width, box_ht, box_depth);	//Top Left Of The Quad (Right)
    		gl.glVertex3f(box_width, -box_ht, box_depth);	//Bottom Left Of The Quad (Right)
    		gl.glVertex3f(box_width, -box_ht, -box_depth);	//Bottom Right Of The Quad (Right)
    		gl.glEnd();
    		gl.glPopMatrix();
    	}


    }

    private final void drawBashiBase()
    {

    	// walls
    	int numSlices = 14;
    	float radius = 0.5f;
    	int HT = 15;
    	double tx, ty, px, py, nx, ny, mag1;
    	float thetastep = (float) (Math3D.TWO_PI_VALUE / numSlices);

    	float[] norm =
    	{
    			0.588f, 0.4235f, 0.2431f
    	};
    	gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, norm, 0);
    	norm[0] /= 1.5;
    	norm[1] /= 1.5;
    	norm[2] /= 1.5;
    	gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, norm, 0);

    	gl.glBegin(GL.GL_QUADS);

    	for (int i = 0; i < numSlices; i++)
    	{
    		tx = Math.cos(thetastep * i) * radius;
    		ty = Math.sin(thetastep * i) * radius;
    		px = Math.cos(thetastep * (i + 1)) * radius;
    		py = Math.sin(thetastep * (i + 1)) * radius;

    		nx = (px - tx);
    		ny = (py - ty);
    		mag1 = Math.sqrt((ny * ny) + (nx * nx));
    		ny /= mag1;
    		nx /= mag1;
    		gl.glNormal3d(nx, 0, ny);

    		gl.glVertex3d(tx, 0, ty);
    		gl.glVertex3d(tx, HT, ty);
    		gl.glVertex3d(px, HT, py);
    		gl.glVertex3d(px, 0, py);
    	}

    	gl.glEnd();

    	// cap
    	gl.glBegin(GL.GL_POLYGON);
    	gl.glNormal3d(0, 1, 0);
    	for (int i = 0; i <= numSlices; i++)
    	{
    		gl.glVertex3d(Math.sin(thetastep * i) * radius, HT, Math.cos(thetastep * i) * radius);
    	}
    	gl.glEnd();
    }
    // features are loaded in the range of the lowest layer.
    public final void loadFeatures(boolean force, Communicator JC) throws IOException
    {
    	final Ptolemy3DUnit unit = ptolemy.unit;
    	final Camera camera = ptolemy.camera;
    	final Position latLonAlt = camera.getPosition();

    	int tx, ty;
    	tx = (int) latLonAlt.getLongitudeDD();
    	ty = (int) latLonAlt.getLatitudeDD();

    	float area_minx, area_maxz, area_maxx, area_minz;
    	area_minx = (tx - (QueryWidth / 8));
    	area_maxz = (ty + (QueryWidth / 8));
    	area_maxx = (tx + (QueryWidth / 8));
    	area_minz = (ty - (QueryWidth / 8));

    	if (((area_minx < feature_minx) || (area_maxx > feature_maxx) || (area_maxz > feature_maxz) || (area_minz < feature_minz)) || (force))
    	{
    		byte[] data = null;
    		feature_minx = tx - (QueryWidth / 2);
    		feature_minz = ty - (QueryWidth / 2);
    		feature_maxx = tx + (QueryWidth / 2);
    		feature_maxz = ty + (QueryWidth / 2);
    		JC.requestData(DbashiGetUrl + "&USER=" + userID + "&KAIRO=" + kairo + "&DENCHU=" + denchu + "&DENSEN=" + densen + "&BBOX=" + feature_minx + "," + feature_minz + "," + feature_maxx + "," + feature_maxz + "&FACTOR=" + String.valueOf(unit.getDD()));

    		data = JC.getData();

    		int[] cur =
    		{
    				0
    		};
    		int NumBashi = ByteReader.readInt(data, cur);
    		int[] feature_Id = new int[NumBashi];
    		Vector<Integer>[] boxarray = new Vector[NumBashi];
    		int[] denchu_type = new int[NumBashi];
    		int[] numDensen = new int[NumBashi];
    		int[][] denchu_to = new int[NumBashi][];
    		int[][] kairo_id = new int[NumBashi][];
    		double[][] feature_real_coord = new double[NumBashi][2];
    		String[] feature_name = new String[NumBashi];
    		int[] feature_text_ix = new int[NumBashi];
    		float[][][] kairo_color = new float[NumBashi][][];

    		double[][] obj_axis = new double[NumBashi][3];
    		double[][] obj_end = new double[NumBashi][3];
    		double[] obj_angle = new double[NumBashi];

			double[] a =
    		{
    				0, 1, 0
    		};

    		StringTokenizer stok;

    		int llen;
    		for (int i = 0; i < NumBashi; i++)
    		{
    			feature_text_ix[i] = -1;
    			feature_Id[i] = ByteReader.readInt(data, cur);

    			denchu_type[i] = ByteReader.readInt(data, cur);

    			llen = ByteReader.readInt(data, cur);
    			if (llen > 0)
    			{
    				feature_name[i] = new String(data, cur[0], llen);
    				cur[0] += llen;
    			}
    			else
    			{
    				feature_name[i] = null;
    			}

    			llen = ByteReader.readInt(data, cur);
    			if (llen > 0)
    			{
    				boxarray[i] = new Vector<Integer>(4);
    				stok = new StringTokenizer(new String(data, cur[0], llen), ",");// comma delim list of box numbers  ie.  1,3,4
    				while (stok.hasMoreTokens())
    				{
    					boxarray[i].add(Integer.parseInt(stok.nextToken()));
    				}
    				stok = null;
    				cur[0] += llen;
    			}
    			else
    			{
    				boxarray[i] = null;
    			}

    			feature_real_coord[i][0] = ByteReader.readInt(data, cur);
    			feature_real_coord[i][1] = ByteReader.readInt(data, cur);

    			{ // translate to virtual world coordinates
    				Math3D.setSphericalCoord(feature_real_coord[i][0], feature_real_coord[i][1], obj_end[i]);
    				obj_angle[i] = Math3D.angle3dvec(a[0], a[1], a[2], obj_end[i][0], obj_end[i][1], obj_end[i][2], true);
    				Vector3d.cross(obj_axis[i], a, obj_end[i]);
    			}

    			numDensen[i] = ByteReader.readInt(data, cur);
    			if (numDensen[i] > 0)
    			{
    				denchu_to[i] = new int[numDensen[i]];
    				kairo_id[i] = new int[numDensen[i]];
    				kairo_color[i] = new float[numDensen[i]][3];
    				for (int h = 0; h < numDensen[i]; h++)
    				{
    					kairo_id[i][h] = ByteReader.readInt(data, cur);
    					denchu_to[i][h] = ByteReader.readInt(data, cur);
    					llen = ByteReader.readInt(data, cur);
    					if (llen > 0)
    					{
    						stok = new StringTokenizer(new String(data, cur[0], llen), ",");// comma delim list of box numbers  ie.  1,3,4
    						int e = 0;
    						while (stok.hasMoreTokens())
    						{
    							kairo_color[i][h][e++] = Float.parseFloat(stok.nextToken()) / 255;
    						}
    						stok = null;
    						cur[0] += llen;
    					}
    				}
    			}

    		}

    		this.feature_text_ix = feature_text_ix;
    		this.feature_name = feature_name;
    		this.kairo_color = kairo_color;
    		this.NumBashi = NumBashi;
    		this.feature_Id = feature_Id;
    		this.feature_boxarray = boxarray;
    		this.numDensen = numDensen;
    		this.denchu_to = denchu_to;
    		this.kairo_id = kairo_id;
    		this.obj_angle = obj_angle;
    		this.obj_axis = obj_axis;
    		this.obj_end = obj_end;
    		this.feature_real_coord = feature_real_coord;
    	}
    }

    public void destroyGL(GL gl)
    {
    	if (FeatureBuffer != null)
    	{
    		FeatureBuffer.flush();
    		FeatureBuffer = null;
    	}
    }

    public String pluginAction(String commandname, String command_params)
    {
    	if (commandname.equalsIgnoreCase("status"))
    	{
    		if ((STATUS = (Integer.parseInt(command_params) == 1) ? true : false))
    		{
    			forceFeatureLoad = true;
    		}
    	}
    	else if (commandname.equalsIgnoreCase("getInfo"))
    	{
    		return "dbashi";
    	}
    	else if (commandname.equalsIgnoreCase("setInfo"))
    	{
    	}
    	else if (commandname.equalsIgnoreCase("getLayerName"))
    	{
    		return "PTOLEMY3D_PLUGIN_DBASHI_" + pindex;
    	}
    	else if (commandname.equalsIgnoreCase("refresh"))
    	{
    		forceFeatureLoad = true;
    	}
    	else if (commandname.equalsIgnoreCase("setKairoHL"))
    	{
    		kairo_highlight_id = Integer.parseInt(command_params);
    	}


    	return null;
    }

    public boolean onPick(double[] intersectPoint)
    {
    	return false;
    }

    public void tileLoaderAction(Communicator JC) throws IOException
    {
    	final Camera camera = ptolemy.camera;
    	if ((!STATUS) || (camera.getVerticalAltitudeMeters() < MIN_ALT) || (camera.getVerticalAltitudeMeters() > MAX_ALT)) {
    		return;
    	}
    	loadFeatures(forceFeatureLoad, JC);
    	prepareFeatures(forceFeatureLoad);
    	forceFeatureLoad = false;
    }

    public boolean pick(double[] intpt, double[][] ray)
    {
    	final Landscape landscape = ptolemy.scene.landscape;
    	final Camera camera = ptolemy.camera;
    	if ((!STATUS) || (camera.getVerticalAltitudeMeters() < MIN_ALT) || (camera.getVerticalAltitudeMeters() > MAX_ALT)) {
    		return false;        // walls
    	}

    	int numSlices = 14;
    	float radius = 0.5f;
    	int HT = 15;
    	double ground;

    	double tx, ty, tz;


    	double feature_kyori, ht, pixw, scale;
    	float thetastep = (float) (Math3D.TWO_PI_VALUE / numSlices);
    	float box_width = 0.5f, box_ht = 0.7f, box_depth = 0.8f;
    	double[] tmpcoor = new double[3];

    	intpt[0] = intpt[1] = intpt[2] = -999;
    	for (int i = 0; i < NumBashi; i++)
    	{
    		ground = landscape.groundHeight(feature_real_coord[i][0], feature_real_coord[i][1], 0);
    		// bashi base
    		for (int j = 0; j <= numSlices; j++)
    		{
    			tx = feature_real_coord[i][0] + Math.cos(thetastep * j) * radius;
    			ty = feature_real_coord[i][1] + Math.sin(thetastep * j) * radius;
    			if (j > 0)
    			{
    				if (pickMacro(i, intpt, ray, tx, ground, ty))
    				{
    					return true;
    				}
    				if (pickMacro(i, intpt, ray, tx, ground + HT, ty))
    				{
    					return true;
    				}
    			}
    			else
    			{
    				Math3D.setTriVert(0, tx, ground, ty);
    				Math3D.setTriVert(1, tx, ground + HT, ty);
    			}
    		}
    		// top
    		Math3D.setTriVert(0, feature_real_coord[i][0] - radius, ground + HT, feature_real_coord[i][1] - radius);
    		Math3D.setTriVert(1, feature_real_coord[i][0] - radius, ground + HT, feature_real_coord[i][1] + radius);
    		if (pickMacro(i, intpt, ray, feature_real_coord[i][0] + radius, ground + HT, feature_real_coord[i][1] - radius))
    		{
    			return true;
    		}
    		if (pickMacro(i, intpt, ray, feature_real_coord[i][0] + radius, ground + HT, feature_real_coord[i][1] + radius))
    		{
    			return true;            // boxes
    		}
    		int boxnum;
    		if (feature_boxarray[i] != null)
    		{
    			for (int j = 0; j < feature_boxarray[i].size(); j++)
    			{
    				boxnum = feature_boxarray[i].get(j);
    				tx = ((boxnum == 1) || (boxnum == 3)) ? -1 : 1;
    				ty = ((boxnum == 3) || (boxnum == 4)) ? -1 : 1;

    				tx += feature_real_coord[i][0];
    				double offht = ground + 10 + ty;
    				ty = feature_real_coord[i][1];

    				Math3D.setTriVert(0, tx - box_width, offht - box_ht, ty - box_depth);
    				Math3D.setTriVert(1, tx - box_width, offht + box_ht, ty - box_depth);
    				// left
    				if (pickMacro(i, intpt, ray, tx - box_width, offht - box_ht, ty + box_depth))
    				{
    					return true;
    				}
    				if (pickMacro(i, intpt, ray, tx - box_width, offht + box_ht, ty + box_depth))
    				{
    					return true;
    					//front
    				}
    				if (pickMacro(i, intpt, ray, tx + box_width, offht - box_ht, ty + box_depth))
    				{
    					return true;
    				}
    				if (pickMacro(i, intpt, ray, tx + box_width, offht + box_ht, ty + box_depth))
    				{
    					return true;
    					//right
    				}
    				if (pickMacro(i, intpt, ray, tx + box_width, offht - box_ht, ty - box_depth))
    				{
    					return true;
    				}
    				if (pickMacro(i, intpt, ray, tx + box_width, offht + box_ht, ty - box_depth))
    				{
    					return true;
    				}
    				Math3D.setTriVert(0, tx + box_width, offht + box_ht, ty + box_depth);
    				Math3D.setTriVert(1, tx - box_width, offht + box_ht, ty + box_depth);
    				//top
    				if (pickMacro(i, intpt, ray, tx + box_width, offht + box_ht, ty - box_depth))
    				{
    					return true;
    				}
    				if (pickMacro(i, intpt, ray, tx - box_width, offht + box_ht, ty - box_depth))
    				{
    					return true;
    					//back
    				}
    				if (pickMacro(i, intpt, ray, tx + box_width, offht - box_ht, ty - box_depth))
    				{
    					return true;
    				}
    				if (pickMacro(i, intpt, ray, tx - box_width, offht - box_ht, ty - box_depth))
    				{
    					return true;
    					//bottom
    				}
    				if (pickMacro(i, intpt, ray, tx + box_width, offht - box_ht, ty + box_depth))
    				{
    					return true;
    				}
    				if (pickMacro(i, intpt, ray, tx - box_width, offht - box_ht, ty + box_depth))
    				{
    					return true;
    				}
    			}
    		}

    		// label
    		if (feature_text_ix[i] == -1)
    		{
    			continue;
    		}
    		ht = ground + 15;

    		tx = obj_end[i][0] * (EARTH_RADIUS + ht);
			ty = obj_end[i][1] * (EARTH_RADIUS + ht);
			tz = obj_end[i][2] * (EARTH_RADIUS + ht);

    		tmpcoor[0] = camera.cameraPos[0] - tx;
    		tmpcoor[1] = camera.cameraPos[1] - ty;
    		tmpcoor[2] = camera.cameraPos[2] - tz;

    		feature_kyori = Math.sqrt((tmpcoor[0] * tmpcoor[0]) + (tmpcoor[1] * tmpcoor[1]) + (tmpcoor[2] * tmpcoor[2]));
    		scale = 1;
    		if (feature_kyori > FEATURE_MIN_KYORI)
    		{
    			Vector3d.normalize(tmpcoor);

    			scale = (feature_kyori * Math.cos(Math3D.angle3dvec(camera.cameraMat.m[0][2], camera.cameraMat.m[1][2],
    					camera.cameraMat.m[2][2],
    					tmpcoor[0], tmpcoor[1], tmpcoor[2], false))) / FEATURE_MIN_KYORI;
    		}

    		pixw = (stringWidths[feature_text_ix[i]] + 5) / 16f;  // add 16 for bitmap, 4 for padding, and 1 for border.
    		if (pixw > 32)
    		{
    			pixw = 32;
    		}
    		Math3D.setTriVert(0, tx, ty, tz);

    		tmpcoor[0] = (pixw + 2) * scale;
    		tmpcoor[1] = 0;
    		tmpcoor[2] = 0;
    		camera.cameraMat.transform(tmpcoor);
    		Math3D.setTriVert(1, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]);

    		tmpcoor[0] = 0;
    		tmpcoor[1] = 2 * scale;
    		tmpcoor[2] = 0;
    		camera.cameraMat.transform(tmpcoor);

    		if (pickMacro(i, intpt, ray, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]))
    		{
    			return true;
    		}
    		tmpcoor[0] = (pixw + 2) * scale;
    		tmpcoor[1] = 2 * scale;
    		tmpcoor[2] = 0;
    		camera.cameraMat.transform(tmpcoor);

    		if (pickMacro(i, intpt, ray, tx + tmpcoor[0], ty + tmpcoor[1], tz + tmpcoor[2]))
    		{
    			return true;
    		}
    	}

    	return false;
    }

    public final void prepareFeatures(boolean force)
    {
    	final Camera camera = ptolemy.camera;
    	final Position latLonAlt = camera.getPosition();

    	if ((latLonAlt.getLongitudeDD() == feature_center_x) && (latLonAlt.getLatitudeDD() == feature_center_z) && (!force)) {
    		return;
    	}

    	feature_center_x = latLonAlt.getLongitudeDD();
    	feature_center_z = latLonAlt.getLatitudeDD();

    	if (FeatureBuffer != null)
    	{
    		FeatureBuffer.flush();
    		FeatureBuffer = null;
    	}
    	FeatureBuffer = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    	graphics = FeatureBuffer.createGraphics();
    	graphics.setColor(bg_color);
    	graphics.fillRect(0, 0, 512, 512);
    	graphics.setFont(nicekanjifont);
    	fmet = graphics.getFontMetrics();

    	if (feature_text_ix != null)
    	{
    		Arrays.fill(feature_text_ix, -1);
    	}
    	Arrays.fill(stringWidths, -1);

    	int[] setIdStore = new int[MAX_FEATURES_SHOW];

    	double dist, shortest = Double.MAX_VALUE;
    	numShowableFeatures = 0;

    	int setId;

    	while (numShowableFeatures < MAX_FEATURES_SHOW)
    	{
    		setId = -1;
    		shortest = Double.MAX_VALUE;
    		for (int i = 0; i < NumBashi; i++)
    		{
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
    		if (feature_name[setId] != null)
    		{
    			stringWidths[numShowableFeatures] = fmet.stringWidth(feature_name[setId]);
    			graphics.setColor(Color.black);
    			graphics.drawString(feature_name[setId], 2, (32 * (numShowableFeatures + 1)) - 5);
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

    private final void setFeatureTex()
    {
    	if (features_id != -1)
    	{
    		ptolemy.textureManager.update(gl, features_id, featuresByteDat, 512, 512, GL.GL_RGBA);
    	}
    	else
    	{
    		features_id = ptolemy.textureManager.load(gl, featuresByteDat, 512, 512, GL.GL_RGBA, true, -1);
    	}
    }

    private boolean pickMacro(int index, double[] intpt, double[][] ray, double tx, double ty, double tz)
    {
    	Math3D.setTriVert(2, tx, ty, tz);
    	if (Math3D.pickTri(intpt, ray))
    	{
    		pluginClickEvent(index);
    		return true;
    	}
    	return false;
    }

    private final void pluginClickEvent(int id)
    {
    	try {
//    		ptolemy.callJavascript("poiClicked", String.valueOf(feature_Id[id]), null, null);
    	}
    	catch (Exception e) {
    	}
    }

    public void reloadData()
    {
    }
}