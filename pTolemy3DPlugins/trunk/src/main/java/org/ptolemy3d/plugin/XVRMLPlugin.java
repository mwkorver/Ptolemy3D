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

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Light;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.util.ByteReader;
import org.ptolemy3d.util.Texture;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.LatLonAlt;

public class XVRMLPlugin implements Plugin
{
	/***** block types ********/
	private final static int TY_SHAPE = 77;
	private final static int TY_3DPOLYGON = 1;
	private final static int TY_3DLINE = 2;
	private final static int TY_3DPOINTS = 3;
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;
	/**  */
	private GL gl;

	private boolean STATUS = true;
	private String GetUrl = "";
	private String GetVrml = "";
	private String ENC_LAYER = "",  LAYER = "";
	private int DISP_MAXZ = Integer.MAX_VALUE;
	private int DISP_MINZ = 0;
	private int MAX_OBJ = 150;
	private int DisplayRadius = 0;
	private int feature_minx = Integer.MAX_VALUE,  feature_minz = Integer.MAX_VALUE,  feature_maxx = Integer.MIN_VALUE,  feature_maxz = Integer.MIN_VALUE;
	private int QueryWidth = 10000,  numVObjs = 0;
	private int[] obj_x;
	private int[] obj_z;
	private double[][] obj_axis;
	private double[] obj_angle;
	private float[][] obj_tf;
	private int[] obj_vrmlid;
	private float[] m = new float[12];
	Hashtable<String,XvrmlObj> xvrml_display = new Hashtable<String,XvrmlObj>(10, 5);
	byte[] data;
	int[] cur =
	{
			0
	};
	int numShapes;
	int current_object = -1;
	int current_shape;
	boolean diffuseOn = false;
	boolean ALL_OBJECTS_LOADED = true;
	private boolean forceDataLoad;
	int current_texture;

	public XVRMLPlugin(){}
	public void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
	}

	public void setPluginIndex(int index)
	{
	}
    public void setPluginParameters(String param)
	{
		final Ptolemy3DUnit unit = ptolemy.unit;

		StringTokenizer stok;

		stok = new StringTokenizer(param, "***");
		// set some defaults

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
						if (ky.equalsIgnoreCase("-status"))
						{
							STATUS = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
						}
						else if (ky.equalsIgnoreCase("-jspurl"))
						{
							GetUrl = ptok.nextToken();
							if (GetUrl.indexOf("?") == -1)
							{
								GetUrl += "?";
							}
						}
						else if (ky.equalsIgnoreCase("-x3durl"))
						{
							GetVrml = ptok.nextToken();
							if (GetVrml.indexOf("?") == -1)
							{
								GetVrml += "?";
							}
						}
						else if (ky.equalsIgnoreCase("-layer"))
						{
							LAYER = ptok.nextToken();
							ENC_LAYER = java.net.URLEncoder.encode(LAYER, "UTF-8");
						}
						else if (ky.equalsIgnoreCase("-querywidth"))
						{
							QueryWidth = Integer.parseInt(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-displayradius"))
						{
							DisplayRadius = (int) (Integer.parseInt(ptok.nextToken()) * unit.coordSystemRatio);
						}
						else if (ky.equalsIgnoreCase("-minalt"))
						{
							DISP_MINZ = Integer.parseInt(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-maxalt"))
						{
							DISP_MAXZ = Integer.parseInt(ptok.nextToken());
							if (DISP_MAXZ == 0)
							{
								DISP_MAXZ = Integer.MAX_VALUE;
							}
						}
						else if (ky.equalsIgnoreCase("-maxobjects"))
						{
							MAX_OBJ = Integer.parseInt(ptok.nextToken());
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
					STATUS = (ptok.nextToken().equals("1")) ? true : false;
					GetUrl = ptok.nextToken();
					if (GetUrl.indexOf("?") == -1)
					{
						GetUrl += "?";
					}
					GetVrml = ptok.nextToken();
					if (GetVrml.indexOf("?") == -1)
					{
						GetVrml += "?";
					}
					LAYER = ptok.nextToken();
					ENC_LAYER = java.net.URLEncoder.encode(LAYER, "UTF-8");
					QueryWidth = Integer.parseInt(ptok.nextToken());
					DisplayRadius = (int) (Integer.parseInt(ptok.nextToken()) * unit.coordSystemRatio);
					DISP_MAXZ = Integer.parseInt(ptok.nextToken());
					if (DISP_MAXZ == 0)
					{
						DISP_MAXZ = Integer.MAX_VALUE;
					}
				}
				catch (Exception e)
				{
				}
			}
		}
	}

	public void initGL(GL gl)
	{

	}
	// called when motion stops.
	public void motionStop(GL gl)
	{
		if (!STATUS) {
			return;
		}

		long start = System.currentTimeMillis();
		long end = start;

		this.gl = gl;
		try {
			while ((end - start) < 150) {
				if (!buildData()) {
					return; // builds vrml object
				}
				end = System.currentTimeMillis();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		this.gl = null;
	}

	public synchronized void draw(GL gl)
	{
		final Landscape landscape = ptolemy.scene.landscape;
		final Light light = ptolemy.scene.light;
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		if ((camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS)) {
			return;
			//gl.glDisable(GLEnum.GL_CULL_FACE);
		}

		this.gl = gl;
		gl.glShadeModel(GL.GL_SMOOTH);
		gl.glEnable(GL.GL_BLEND);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

		int i, j;
		XvrmlObj xobj;
		for (i = 0; i < numVObjs; i++)
		{
			xobj = xvrml_display.get(String.valueOf(obj_vrmlid[i]));
			if ((xobj != null) && (xobj.lists.length > 0))
			{
				gl.glPushMatrix();

				// translate to top of earth, then rotate to position.
				gl.glRotated(obj_angle[i], obj_axis[i][0], obj_axis[i][1], obj_axis[i][2]);
				gl.glTranslated(0, EARTH_RADIUS + landscape.groundHeight(obj_x[i], obj_z[i], 0), 0);

				// apply object specific matrix if there is one
				if (obj_tf[i] != null)
				{
					//	gl.glMultMatrixf(obj_tf[i]);
					gl.glRotated(obj_tf[i][3], obj_tf[i][0], obj_tf[i][1], obj_tf[i][2]);
					gl.glScaled(obj_tf[i][8], obj_tf[i][9], obj_tf[i][10]);
					gl.glTranslated(obj_tf[i][4] * unit.coordSystemRatio, obj_tf[i][5] * unit.coordSystemRatio, obj_tf[i][6] * unit.coordSystemRatio);
				}

				// reset light position
				gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light.light0Position, 0);
				gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, light.light1Position, 0);

				int len = (obj_vrmlid[i] != current_object) ? xobj.lists.length : current_shape;
				for (j = 0; j < len; j++)
				{
					gl.glCallList(xobj.lists[j]);
				}
				gl.glPopMatrix();
			}
		}

		// set light position again in case it was moved

		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light.light0Position, 0);
		gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, light.light1Position, 0);

		gl.glShadeModel(GL.GL_FLAT);
		//gl.glEnable(GL.GL_CULL_FACE);
		gl.glDisable(GL.GL_BLEND);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);

		this.gl = null;
	}

	public void destroyGL(GL gl)
	{
		Enumeration<String> e = xvrml_display.keys();
		while (e.hasMoreElements())
		{
			removeVrmlObj(gl, Integer.parseInt(e.nextElement()));
		}
		xvrml_display.clear();
	}

	private void removeVrmlObj(GL gl, int keyid)
	{
		XvrmlObj xobj = xvrml_display.get(String.valueOf(keyid));
		int len = (keyid != current_object) ? xobj.lists.length : current_shape;
		for (int j = 0; j < len; j++)
		{
			gl.glDeleteLists(xobj.lists[j], 1);
		}

		Enumeration<float[]> imgs = xobj.images.elements();
		while (imgs.hasMoreElements()) {
			float[] tex_meta = imgs.nextElement();
			int tex_id = (int)tex_meta[0];
			ptolemy.textureManager.unload(gl, tex_id);
		}
		xvrml_display.remove(String.valueOf(keyid));
		xobj = null;
	}

	private final void loadData(boolean force, Communicator JC) throws IOException
	{
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Camera camera = ptolemy.camera;
		final LatLonAlt latLonAlt = camera.getLatAltLon();

		int tx, ty;
		tx = (int) latLonAlt.getLongitudeDD();
		ty = (int) latLonAlt.getLatitudeDD();

		int dispr = (DisplayRadius / 2);
		double[] tmp = new double[3];
		double[] a =
		{
				0, 1, 0
		};

		if ((((tx - dispr) < feature_minx) || ((tx + dispr) > feature_maxx) || ((ty + dispr) > feature_maxz) || ((ty - dispr) < feature_minz)) || (force))
		{
			ALL_OBJECTS_LOADED = false;
			feature_minx = tx - (QueryWidth / 2);
			feature_minz = ty - (QueryWidth / 2);
			feature_maxx = tx + (QueryWidth / 2);
			feature_maxz = ty + (QueryWidth / 2);
			JC.requestData(GetUrl + "&LAYER=" + ENC_LAYER + "&FACTOR=" + (unit.DD) + "&BBOX=" + feature_minx + "," + feature_minz + "," + feature_maxx + "," + feature_maxz);
			byte[] data = JC.getData();
			
			if(data != null) {
				int[] cur = {0};

				int numVObjs = ByteReader.readInt(data, cur);
				int[] obj_id = new int[numVObjs];
				int[] obj_x = new int[numVObjs];
				int[] obj_z = new int[numVObjs];
				float[][] obj_tf = new float[numVObjs][];
				int[] obj_vrmlid = new int[numVObjs];
				double[][] obj_axis = new double[numVObjs][3];
				double[] obj_angle = new double[numVObjs];

				for (int i = 0; i < numVObjs; i++)
				{
					obj_id[i] = ByteReader.readInt(data, cur);
					obj_x[i] = ByteReader.readInt(data, cur);
					obj_z[i] = ByteReader.readInt(data, cur);
					{
						Math3D.setSphericalCoord(obj_x[i], obj_z[i], tmp);
						obj_angle[i] = Math3D.angle3dvec(a[0], a[1], a[2], tmp[0], tmp[1], tmp[2], true);
						Vector3d.cross(obj_axis[i], a, tmp);
					}
					obj_vrmlid[i] = ByteReader.readInt(data, cur);
					if (data[cur[0]++] == 1)
					{
						obj_tf[i] = new float[16];
						Arrays.fill(obj_tf[i], 0.f);
						for (int m = 0; m < 12; m++)
						{
							obj_tf[i][m] = ByteReader.readFloat(data, cur);
						}
						obj_tf[i][15] = 1;
					}
				}

				this.numVObjs = numVObjs;
				this.obj_x = obj_x;
				this.obj_z = obj_z;
				this.obj_tf = obj_tf;
				this.obj_vrmlid = obj_vrmlid;
				this.obj_axis = obj_axis;
				this.obj_angle = obj_angle;

				data = null;
			}
		}
	}

	private final void loadClosestObject(Communicator JC) throws IOException
	{
		if ((current_object != -1) || (ALL_OBJECTS_LOADED)) {
			return;
		}

		final Camera camera = ptolemy.camera;
		final LatLonAlt latLonAlt = camera.getLatAltLon();

		int i;
		int b_id = -1;
		double closest = Float.MAX_VALUE;
		double dist;
		String id_str;
		for (i = 0; i < numVObjs; i++)
		{
			if (obj_vrmlid[i] != b_id)
			{
				id_str = String.valueOf(obj_vrmlid[i]);
				if (xvrml_display.get(id_str) == null)
				{

					dist = Math3D.distance3D(latLonAlt.getLongitudeDD(), obj_x[i],
							0, 0,
							latLonAlt.getLatitudeDD(), obj_z[i]);
					if (dist < closest)
					{
						closest = dist;
						b_id = obj_vrmlid[i];
					}
				}
			}
		}
		if (b_id != -1)
		{

			// clear an object if we are over our max size.
			int keyid = -1;
			int delkeyid = -1;
			boolean ObjRemoved = false;
			int xvrmlsize = xvrml_display.size();
			if (xvrml_display.size() > MAX_OBJ)
			{
				for (i = 0; i < xvrmlsize; i++)
				{
					Enumeration<String> thekeys = xvrml_display.keys();
					while (thekeys.hasMoreElements())
					{
						id_str = thekeys.nextElement();
						delkeyid = -1;
						for (int j = 0; j < numVObjs; j++)
						{
							keyid = Integer.parseInt(id_str);
							if (obj_vrmlid[i] == keyid)
							{
								delkeyid = keyid;
								break;
							}
						}
						if (delkeyid != -1)
						{
							// flush this object
							removeVrmlObj(gl, delkeyid);
							ObjRemoved = true;
							break;
						}
					}

					if (!ObjRemoved)
					{
						// remove the first object
						thekeys = xvrml_display.keys();
						id_str = thekeys.nextElement();
						removeVrmlObj(gl, Integer.parseInt(id_str));
					}

				}
			}

			JC.requestData(GetVrml + "&ID=" + b_id + ".x3d");

			int t = JC.getHeaderReturnCode();
			if ((t == 200) || (t == 206))
			{
				data = JC.getData();
				cur[0] = 0;
				current_shape = 0;
				current_object = b_id;
			}
			else
			{
				data = null;
				JC.flushNotFound();
				XvrmlObj xobj = new XvrmlObj();
				xobj.lists = new int[0];
				xvrml_display.put(String.valueOf(b_id), xobj);
			}
		}
		else
		{
			ALL_OBJECTS_LOADED = true;
		}
	}

	private final boolean buildData()
	{
		if (data == null)
		{
			return false;        // object data
		}
		if (current_shape == 0)
		{
			numShapes = readInt();
			XvrmlObj xobj = new XvrmlObj();
			xobj.lists = new int[numShapes];
			xvrml_display.put(String.valueOf(current_object), xobj);
			if (numShapes <= 0)
			{
				data = null;
				current_object = -1;
				return false;
			}
		}

		buildShape();

		current_shape++;
		if (current_shape >= numShapes)
		{
			// free resources
			data = null;
			current_object = -1;
		}
		return true;
	}

	private final void buildShape()
	{
		if (readInt() != TY_SHAPE)
		{
			current_object = numVObjs;
			return;
		}

		float[] tex_meta = null;
		setTransform();
		current_texture = -1;
		XvrmlObj xobj = xvrml_display.get(String.valueOf(current_object));
		// appearance
		int transparency = readInt();

		diffuseOn = false;
		float[] diffuse_color = new float[4];
		String image_url = "";
		int len;
		if (data[cur[0]++] == 1)
		{
			// diffuse color
			diffuseOn = true;
			diffuse_color[0] = readFloat();
			diffuse_color[1] = readFloat();
			diffuse_color[2] = readFloat();
			diffuse_color[3] = 1.0f;
		}
		if (data[cur[0]++] == 1)
		{
			// image texture

			len = readInt();
			image_url = new String(data, cur[0], len);
			cur[0] += len;

			tex_meta = xobj.images.get(image_url);

			if (tex_meta == null)
			{
				tex_meta = new float[4];
				float[] meta = new float[5];
				final Texture tex = Texture.setWebImage(image_url, meta, transparency, null);
				int tex_id = -1;
				if(tex != null) {
					tex_id = ptolemy.textureManager.load(gl, tex.data, tex.width, tex.height, tex.format, false, -1);
				}
				tex_meta[0] = tex_id;
				tex_meta[1] = meta[0];
				tex_meta[2] = meta[1];
				current_texture = tex_id;

				xobj.images.put(image_url, tex_meta);
			}
			else
			{
				current_texture = (int) tex_meta[0];
			}
		}

		xobj.lists[current_shape] = gl.glGenLists(1);
		gl.glNewList(xobj.lists[current_shape], GL.GL_COMPILE);

		if (diffuseOn)
		{
			gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, diffuse_color, 0);
			float[] diffuse_color2 = new float[4];
			for (int h = 0; h < 3; h++)
			{
				diffuse_color2[h] = diffuse_color[h] / 1.5f;
			}
			diffuse_color2[3] = 1;
			gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, diffuse_color2, 0);
		}
		// geometry
		int geom_type = readInt();
		switch (geom_type)
		{
			case TY_3DPOLYGON:
				build3dPoly(xobj, tex_meta);
				break;
			case TY_3DLINE:
				build3dLine(xobj);
				break;
			case TY_3DPOINTS:
				build3dPoints(xobj);
				break;
		}
		if (diffuseOn)
		{
			ptolemy.scene.light.setLights(gl);
		}
		gl.glEndList();
	}

	private final void setLightOptions(float[][][] ccnt)
	{
		if (current_texture == -1)
		{
			gl.glDisable(GL.GL_TEXTURE_2D);
		}
		else
		{
			gl.glBindTexture(GL.GL_TEXTURE_2D, current_texture);
			gl.glEnable(GL.GL_TEXTURE_2D);
		}
		if ((current_texture != -1) || (diffuseOn) || (ccnt[2] != null))
		{
			gl.glEnable(GL.GL_LIGHT0);
			gl.glEnable(GL.GL_LIGHT1);
			gl.glEnable(GL.GL_LIGHTING);
		}
	}

	private final void removeLightOptions(float[][][] ccnt)
	{
		if (current_texture == -1)
		{
			gl.glEnable(GL.GL_TEXTURE_2D);
		}
		if ((current_texture != -1) || (diffuseOn) || (ccnt[2] != null))
		{
			gl.glDisable(GL.GL_LIGHT0);
			gl.glDisable(GL.GL_LIGHT1);
			gl.glDisable(GL.GL_LIGHTING);
		}
	}

	private final void build3dPoly(XvrmlObj xobj, float[] tex_meta)
	{
		int[][] indexes = new int[4][];
		float[][][] ccnt = new float[4][][];

		boolean cpv = (data[cur[0]++] == 1) ? true : false;
		boolean npv = (data[cur[0]++] == 1) ? true : false;
		int len;
		int i, j;
		for (i = 0; i < 4; i++)
		{
			if (data[cur[0]++] == 1)
			{
				len = readInt();
				indexes[i] = new int[len];
				for (j = 0; j < len; j++)
				{
					indexes[i][j] = readInt();
				}
			}
		}
		for (i = 0; i < 4; i++)
		{
			if (data[cur[0]++] == 1)
			{
				ccnt[i] = readCoord(xobj, i);
			}
		}
		if (indexes[0] == null)
		{
			return;
		}
		if ((current_texture > 0) && (ccnt[3] == null) && (ccnt[0] != null))
		{
			float[] rmin = new float[3];
			float[] ranges = new float[3];
			for (i = 0; i < 3; i++)
			{
				rmin[i] = Math3D.min(ccnt[0], i);
				ranges[i] = Math3D.max(ccnt[0], i) - rmin[i];
			}
			int Si = 0;
			float tival = -1;
			int Ti = -1;
			for (i = 1; i < 3; i++)
			{
				if (ranges[i] > ranges[Si])
				{
					Si = i;
				}
			}
			for (i = 0; i < 3; i++)
			{
				if ((ranges[i] > tival) && (i != Si))
				{
					tival = ranges[i];
					Ti = i;
				}
			}
			ccnt[3] = new float[ccnt[0].length][2];
			for (i = 0; i < ccnt[0].length; i++)
			{
				ccnt[3][i][0] = (ccnt[0][i][Si] - rmin[Si]) / ranges[Si];
				ccnt[3][i][1] = (ccnt[0][i][Ti] - rmin[Ti]) / ranges[Ti];

			}
			indexes[3] = null; // just to make sure
		}

		setLightOptions(ccnt);
		int face_ctr = 0, uu;
		float[] use_arr;
		float[] tnorm = new float[3];

		for (int u = 0; u < indexes[0].length; u++)
		{
			if (((u == 0) || (indexes[0][u] == -1)) && (u != (indexes[0].length - 1)))
			{
				if (u != 0)
				{
					gl.glEnd();
				}
				gl.glBegin(GL.GL_POLYGON);
				// set color for this poly if we need to
				if ((ccnt[1] != null) && (!cpv))
				{
					use_arr = (indexes[1] != null) ? ccnt[1][indexes[1][face_ctr]] : ccnt[1][face_ctr];
					gl.glColor3f(use_arr[0], use_arr[1], use_arr[2]);
				}
				else if ((ccnt[2] != null) && (!npv))
				{ // or set normal
					use_arr = (indexes[2] != null) ? ccnt[2][indexes[2][face_ctr]] : ccnt[2][face_ctr];
					gl.glNormal3f(use_arr[0], use_arr[1], use_arr[2]);
				}
				else if (!npv)
				{ // or generate a normal
					uu = (u == 0) ? 0 : u + 1;
					Math3D.computeFaceNormal(ccnt[0][indexes[0][uu]], ccnt[0][indexes[0][uu + 1]], ccnt[0][indexes[0][uu + 2]], tnorm);
					gl.glNormal3f(tnorm[0], tnorm[1], tnorm[2]);
				}
				face_ctr++;
			}

			if (indexes[0][u] != -1)
			{
				if ((ccnt[2] != null) && (npv))
				{
					use_arr = (indexes[2] != null) ? ccnt[2][indexes[2][u]] : ccnt[2][indexes[0][u]];
					gl.glNormal3f(use_arr[0], use_arr[1], use_arr[2]);
				}
				if ((ccnt[3] != null) && (current_texture > 0))
				{
					use_arr = (indexes[3] != null) ? ccnt[3][indexes[3][u]] : ccnt[3][indexes[0][u]];
					float t_x = use_arr[0];
					float t_y = 0.999f - use_arr[1];
					if (tex_meta != null)
					{
						t_x *= tex_meta[1];
						t_y *= tex_meta[2];
					}
					if (t_x < 0)
					{
						t_x = 0;
					}
					if (t_x > tex_meta[1])
					{
						t_x = tex_meta[1] - 0.000001f;
					}
					if (t_y < 0)
					{
						t_y = 0;
					}
					if (t_y > tex_meta[2])
					{
						t_y = tex_meta[2] - 0.000001f;
					}
					gl.glTexCoord2f(t_x, t_y);
				}
				else if ((ccnt[1] != null) && (cpv))
				{
					use_arr = (indexes[1] != null) ? ccnt[1][indexes[1][u]] : ccnt[1][indexes[0][u]];
					gl.glColor3f(use_arr[0], use_arr[1], use_arr[2]);
				}
				gl.glVertex3f(ccnt[0][indexes[0][u]][0], ccnt[0][indexes[0][u]][1], ccnt[0][indexes[0][u]][2]);
			}
		}
		gl.glEnd();
		removeLightOptions(ccnt);
	}

	private float[][] readCoord(XvrmlObj xobj, int ty)
	{
		int j, k, len, len2, ud;
		String defstr = null;
		float[][] coord = null;
		ud = data[cur[0]++];
		len = readInt();
		if (len > 0)
		{
			defstr = new String(data, cur[0], len);
		}
		cur[0] += len;
		if (ud == 2)
		{
			coord = xobj.coord.get(defstr);
		}
		else
		{
			len = readInt();
			len2 = readInt();
			coord = new float[len][len2];
			for (j = 0; j < len; j++)
			{
				for (k = 0; k < len2; k++)
				{
					coord[j][k] = readFloat();
				}
			}
			if (ud == 1)
			{
				xobj.coord.put(defstr, coord);
			}
		}

		if ((ty == 0) && (coord != null))
		{
			float[][] tcoord = new float[coord.length][];
			for (j = 0; j < coord.length; j++)
			{
				tcoord[j] = transform(coord[j]);
			}
			coord = tcoord;
		}
		return coord;
	}

	private final void build3dLine(XvrmlObj xobj)
	{
		boolean cpv = (data[cur[0]++] == 1) ? true : false;

		int[][] indexes = new int[2][];
		float[][][] ccnt = new float[2][][];
		int len;
		int i, j;
		for (i = 0; i < 2; i++)
		{
			if (data[cur[0]++] == 1)
			{
				len = readInt();
				indexes[i] = new int[len];
				for (j = 0; j < len; j++)
				{
					indexes[i][j] = readInt();
				}
			}
		}

		for (i = 0; i < 2; i++)
		{
			if (data[cur[0]++] == 1)
			{
				ccnt[i] = readCoord(xobj, i);
			}
		}

		if (indexes[0] == null)
		{
			return;
		}
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glBegin(GL.GL_LINE_STRIP);
		int face_ctr = 0;
		float[] use_arr;

		for (int u = 0; u < indexes[0].length; u++)
		{
			if ((indexes[0][u] == -1) && (u != (indexes[0].length - 1)))
			{
				gl.glEnd();
				gl.glBegin(GL.GL_LINE_STRIP);
				face_ctr++;
			}
			else if (indexes[0][u] != -1)
			{
				if (ccnt[1] != null)
				{
					use_arr = (cpv) ? (indexes[1] != null) ? ccnt[1][indexes[1][u]] : ccnt[1][indexes[0][u]] : (indexes[1] != null) ? ccnt[1][indexes[1][face_ctr]] : ccnt[1][face_ctr];
					gl.glColor3f(use_arr[0], use_arr[1], use_arr[2]);
				}
				gl.glVertex3f(ccnt[0][indexes[0][u]][0], ccnt[0][indexes[0][u]][1], ccnt[0][indexes[0][u]][2]);
			}
		}
		gl.glEnd();
		gl.glEnable(GL.GL_TEXTURE_2D);
	}

	private final void build3dPoints(XvrmlObj xobj)
	{
		float[][][] ccnt = new float[2][][];
		int i;
		for (i = 0; i < 2; i++)
		{
			if (data[cur[0]++] == 1)
			{
				ccnt[i] = readCoord(xobj, i);
			}
		}

		if (ccnt[0] == null)
		{
			return;
		}
		gl.glPointSize(5.0f);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glBegin(GL.GL_POINTS);
		for (int u = 0; u < ccnt[0].length; u++)
		{
			if ((ccnt[1] != null) && (ccnt[1].length > u))
			{
				gl.glColor3f(ccnt[1][u][0], ccnt[1][u][1], ccnt[1][u][2]);
			}
			else
			{
				gl.glColor3f(0f, 0f, 0f);
			}
			gl.glVertex3f(ccnt[0][u][0], ccnt[0][u][1], ccnt[0][u][2]);
		}
		gl.glEnd();
		gl.glEnable(GL.GL_TEXTURE_2D);
	}

	private final void setTransform()
	{
		for (int i = 0; i < 12; i++)
		{
			m[i] = readFloat();
		}
	}

	private final float[] transform(float[] v)
	{
		float[] tp = new float[3];
		tp[0] = (m[0] * v[0] + m[1] * v[1] + m[2] * v[2]) + m[3];
		tp[1] = (m[4] * v[0] + m[5] * v[1] + m[6] * v[2]) + m[7];
		tp[2] = (m[8] * v[0] + m[9] * v[1] + m[10] * v[2]) + m[11];
		return tp;
	}

	private int readInt()
	{
		return ByteReader.readInt(data, cur);
	}

	private float readFloat()
	{
		return ByteReader.readFloat(data, cur);
	}

	public String pluginAction(String commandname, String command_params)
	{
		try
		{
			if (commandname.equalsIgnoreCase("refresh"))
			{
				forceDataLoad = true;
			}
			else if (commandname.equalsIgnoreCase("status"))
			{
				boolean prevstat = STATUS;
				STATUS = (Integer.parseInt(command_params) == 1) ? true : false;
				if ((!prevstat) && (STATUS))
				{
					forceDataLoad = true;
				}
			}
			else if (commandname.equalsIgnoreCase("getLayerName"))
			{
				return LAYER;
			}
			else if (commandname.equalsIgnoreCase("clearx3d"))
			{
				GL gl_not_available_here = null;	//FIXME OpenGL context not current ...
				destroyGL(gl_not_available_here);
			}
		}
		catch (Exception e)
		{
		}
		return null;
	}

	public boolean onPick(double[] intersectPoint)
	{
		return false;
	}

	class XvrmlObj
	{
		public int[] lists;
		public Hashtable<String, float[]> images = new Hashtable<String, float[]>(1, 4);
		public Hashtable<String,float[][]> coord = new Hashtable<String,float[][]>(10, 10);
	}

	public void tileLoaderAction(Communicator JC) throws IOException
	{
		if (!STATUS)
		{
			return;
		}
		loadClosestObject(JC); // grabs data of nearest vrml object
		loadData(forceDataLoad, JC);
		forceDataLoad = false;
	}

	public boolean pick(double[] intpt, double[][] ray)
	{
		return false;
	}

	public void reloadData()
	{
	}
}