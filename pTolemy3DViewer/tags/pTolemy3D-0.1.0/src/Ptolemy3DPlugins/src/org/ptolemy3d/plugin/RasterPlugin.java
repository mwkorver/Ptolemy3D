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
import java.util.StringTokenizer;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.io.BasicCommunicator;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.util.TextureLoaderGL;
import org.ptolemy3d.view.Camera;

public class RasterPlugin implements Plugin
{
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;
    /** Plugin index*/
    private int pindex;

	private boolean STATUS = true;
	private short transparency;
	boolean forceReload = false;
	int maxDemRes, demRes, clicker;
	double center_x = -9, center_z = -9;
	private float[] r_dem = null;
	byte[] img;
	float[] rasterPms = new float[4]; // rasterPms[0] , rasterPms[1]
	int[] tran_color = { 0, 0, 0 };
	private Communicator JC;
	int[] tex_id = { -1 };
	String MapserverUrl = "";
	int PixelWidth;
	float half_pxw;
	float Scale;
	double AboveGround;
	private int DISP_MAXZ = Integer.MAX_VALUE;
	private int DISP_MINZ = 0;

	public RasterPlugin(){}
	public void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;

		JC = new BasicCommunicator(ptolemy.configuration.server);
	}

	public void setPluginIndex(int index)
	{
		this.pindex = index;
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
						else if (ky.equalsIgnoreCase("-mapsvurl"))
						{
							MapserverUrl = ptok.nextToken();
						}
						else if (ky.equalsIgnoreCase("-pixelwidth"))
						{
							PixelWidth = Integer.parseInt(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-scale"))
						{
							Scale = Float.parseFloat(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-zraise"))
						{
							AboveGround = Integer.parseInt(ptok.nextToken()) * unit.coordSystemRatio;
						}
						else if (ky.equalsIgnoreCase("-transparency"))
						{
							transparency = (short) (Float.parseFloat(ptok.nextToken()) * 255);
						}
						else if (ky.equalsIgnoreCase("-transparencycolor"))
						{
							StringTokenizer tcstok = new StringTokenizer(ptok.nextToken(), ":");
							for (int k = 0; k < 3; k++)
							{
								tran_color[k] = Integer.parseInt(tcstok.nextToken());
							}
						}
						else if (ky.equalsIgnoreCase("-maxDemRes"))
						{
							maxDemRes = Integer.parseInt(ptok.nextToken());
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
					MapserverUrl = ptok.nextToken();
					PixelWidth = Integer.parseInt(ptok.nextToken());
					Scale = Float.parseFloat(ptok.nextToken());
					AboveGround = Integer.parseInt(ptok.nextToken()) * unit.coordSystemRatio;
					transparency = (short) (Float.parseFloat(ptok.nextToken()) * 255);
					StringTokenizer tcstok = new StringTokenizer(ptok.nextToken(), ":");
					for (int k = 0; k < 3; k++)
					{
						tran_color[k] = Integer.parseInt(tcstok.nextToken());
					}
					maxDemRes = Integer.parseInt(ptok.nextToken());

				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			if (MapserverUrl.indexOf("?") == -1)
			{
				MapserverUrl += "?";
			}
			half_pxw = Scale / 2 * unit.coordSystemRatio;
		}
	}

	public void initGL(GL gl)
	{

	}

	public void motionStop(GL gl)
	{
		final Camera camera = ptolemy.camera;
		if ((camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS)) {
			return;
		}

		try {
			loadRaster(gl, forceReload);
		}
		catch (Exception e){}
	}

	public synchronized void draw(GL gl)
	{
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Camera camera = ptolemy.camera;
		if ((forceReload) || (camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS)) {
			return;
		}

		gl.glEnable(GL.GL_BLEND);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
		if (tex_id[0] != -1)
		{
			gl.glColor3f(1.0f, 1.0f, 1.0f);
			gl.glBindTexture(GL.GL_TEXTURE_2D, tex_id[0]);

			{
				double rx = (center_x / unit.DD) - (Scale / 2);
				double ry = (center_z / unit.DD) + (Scale / 2);

				double theta1 = (rx + 180) * Math3D.degToRad;  // startx
				double phi1 = (ry) * Math3D.degToRad; //starty

				double theta2 = (rx + Scale + 180) * Math3D.degToRad;  // startx
				double phi2 = (ry - Scale) * Math3D.degToRad; //starty

				int n = demRes - 1;

				float tex_w = rasterPms[0] / n;
				float tex_h = rasterPms[1] / n;

				double t1, t2, t3;
				double radius; // radius
				for (int j = 0; j < n; j++)
				{
					t1 = phi1 + j * (phi2 - phi1) / n;
					t2 = phi1 + (j + 1) * (phi2 - phi1) / n;

					gl.glBegin(GL.GL_TRIANGLE_STRIP);
					for (int i = 0; i <= n; i++)
					{
						t3 = (theta1 + i * (theta2 - theta1) / n);
						radius = EARTH_RADIUS + r_dem[j * maxDemRes + i] + AboveGround;
						gl.glTexCoord2f(tex_w * i, tex_h * j);
						gl.glVertex3d((radius) * Math.cos(t1) * Math.sin(t3), (radius) * Math.sin(t1), (radius) * Math.cos(t1) * Math.cos(t3));
						radius = EARTH_RADIUS + r_dem[(j + 1) * maxDemRes + i] + AboveGround;
						gl.glTexCoord2f(tex_w * i, tex_h * (j + 1));
						gl.glVertex3d((radius) * Math.cos(t2) * Math.sin(t3), (radius) * Math.sin(t2), (radius) * Math.cos(t2) * Math.cos(t3));
					}
					gl.glEnd();
				}
			}
		}
		gl.glDisable(GL.GL_BLEND);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);

	}

	public void destroyGL(GL gl)
	{
		if (tex_id[0] != -1)
		{
			gl.glDeleteTextures(1, tex_id, 0);
			tex_id[0] = -1;
		}
	}

	private final void loadRaster(GL gl, boolean force) throws Exception
	{
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Camera camera = ptolemy.camera;

		if ((center_x == camera.getLongitudeDD()) && (center_z == camera.getLatitudeDD()) && (!force)) {
			return;
		}

		center_x = camera.getLongitudeDD();
		center_z = camera.getLatitudeDD();

		demRes = 0;
		clicker = 11;

		double tx, tz;
		tx = center_x / unit.DD;
		tz = center_z / unit.DD;

		JC.requestData(MapserverUrl + "&lon=" + tx + "&lat=" + tz + "&scale=" + Scale + "&w=" + PixelWidth + "&h=" + PixelWidth + "&itype=1&uid=" + center_x);
		int t = JC.getHeaderReturnCode();
		if ((t == 200) || (t == 206))
		{
			TextureLoaderGL.loadRaster(gl, rasterPms, tex_id, JC.getData(), img, transparency, tran_color);
		}
		forceReload = false;
	}

	private final void setTransparency(String pct)
	{
		try
		{
			transparency = (short) (Float.parseFloat(pct) * 255);
			forceReload = true;
		}
		catch (Exception e)
		{
		}
	}

	private final void setDEM()
	{
		final Landscape landscape = ptolemy.scene.landscape;
		final Ptolemy3DUnit unit = ptolemy.unit;

		if ((tex_id[0] == -1) || (!landscape.terrainEnabled && (demRes != 0))) {
			return;
		}

		if ((demRes >= maxDemRes) && (clicker < 10)) {
			clicker++;
			return;
		}
		else if (clicker < 5) {
			clicker++;
			return;
		}
		clicker = 0;

		if (demRes == 0) {
			demRes = 2;
		}
		demRes *= demRes;
		if (demRes > maxDemRes) {
			demRes = maxDemRes;
		}
		double rx, ry, cellw, cellh;

		if (r_dem == null) {
			r_dem = new float[maxDemRes * maxDemRes];
		}

		rx = center_x - (Scale / 2 * unit.DD);
		ry = center_z + (Scale / 2 * unit.DD);
		cellw = (Scale * unit.DD) / (demRes - 1);
		cellh = -(Scale * unit.DD) / (demRes - 1);

		for (int i = 0; i < demRes; i++) {
			for (int j = 0; j < demRes; j++) {
				r_dem[i * maxDemRes + j] = (float) landscape.groundHeight(rx + (cellw * j), ry + (cellh * i), 0);
			}
		}
	}

	public String pluginAction(String commandname, String command_params)
	{
		final Ptolemy3DUnit unit = ptolemy.unit;

		if (commandname.equalsIgnoreCase("status"))
		{
			STATUS = (Integer.parseInt(command_params) == 1) ? true : false;
		}
		else if (commandname.equalsIgnoreCase("setTransparency"))
		{
			setTransparency(command_params);
		}
		else if (commandname.equalsIgnoreCase("getLayerName"))
		{
			return "PTOLEMY3D_PLUGIN_RASTER_" + pindex;
		}
		else if (commandname.equalsIgnoreCase("setUrl"))
		{
			MapserverUrl = command_params;
			forceReload = true;
		}
		else if (commandname.equalsIgnoreCase("getInfo"))
		{
			return "raster," + STATUS + "," + MapserverUrl + "," + PixelWidth + "," + Scale + "," + (AboveGround / unit.coordSystemRatio) + "," + ((float) transparency / 255) + "," + tran_color[0] + ":" + tran_color[1] + ":" + tran_color[2] + "," + maxDemRes;
		}
		else if (commandname.equalsIgnoreCase("setInfo"))
		{
			setPluginParameters(command_params);
			forceReload = true;
		}
		return null;
	}

	public boolean onPick(double[] intersectPoint)
	{
		return false;
	}

	public void tileLoaderAction(Communicator JC) throws IOException
	{
		setDEM();
	}

	public boolean pick(double[] intpt, double[][] ray)
	{
		return false;
	}

	public void reloadData()
	{
		forceReload = true;
	}
}
