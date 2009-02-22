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


import java.io.FileInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.util.Texture;
import org.ptolemy3d.plugin.util.VectorNode;

public class GisToolPlugin implements Plugin
{
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;
    /** Plugin index*/
    private int pindex;

	private boolean STATUS = true;
	private float[] stroke = {0.847f, 0.572f, 0.153f};
	private float[] pcolor = {0.9f, 0.9f, 0.9f};
	private float PSIZE = 4.0f;
	private float LWIDTH = 2.0f;
	private int tex_id = -1;
	byte[] data = null;
	private byte[] img = null;
	private float[] rasterPms = new float[4];
	private short transparency = (short) 255;
	int[] tran_color = {0, 0, 0};
	private boolean setRaster,  useWF;
	private float[] r_dem = null;
	private float RAISE = 10;
	private boolean clickEnabled = true;
	private int resetPtElevations = 1;
	private double raster_sx,  raster_sy,  raster_tx,  raster_ty,  raster_px,  raster_py,  raster_avght;
	private Vector<VectorNode> pointStore = null;
	/*
    0 - draw line
    1 - draw poly
    2 - raster
    3 - draw dist circle
	 */
	private int mode = -1;

	public GisToolPlugin() {}
	public void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;

		this.RAISE *= ptolemy.unit.getCoordSystemRatio();
	}

	public void setPluginIndex(int index)
	{
		this.pindex = index;
	}
    public void setPluginParameters(String param)
	{
		StringTokenizer stok = new StringTokenizer(param, ",");
		STATUS = (Integer.parseInt(stok.nextToken()) == 1) ? true : false;
	}

	public void initGL(GL gl)
	{
	}

	public void motionStop(GL gl)
	{
		resetPtElevations--;
	}

	public void draw(GL gl)
	{
		if ((!STATUS) || (mode < 0)) {
			return;
		}

		final Ptolemy3DUnit unit = ptolemy.unit;

		if ((mode == 2) && (setRaster))
		{
			if (tex_id != -1) {
				ptolemy.textureManager.unload(gl, tex_id);
			}
			
			img = null;
			final Texture tex = Texture.loadRaster(rasterPms, data, img, transparency, tran_color);
			if(tex != null) {
				tex_id = ptolemy.textureManager.load(gl, tex.data, tex.width, tex.height, tex.format, false, -1);
			}
			raster_px = rasterPms[2];
			raster_py = rasterPms[3];
			setRaster = false;
			if (!useWF)
			{
				VectorNode pt1 = pointStore.elementAt(0);
				VectorNode pt2 = pointStore.elementAt(2);

				raster_tx = Math.min(pt1.rp[0], pt2.rp[0]) / unit.getDD();
				raster_ty = Math.max(pt1.rp[1], pt2.rp[1]) / unit.getDD();
				raster_sx = ((Math.max(pt1.rp[0], pt2.rp[0]) - Math.min(pt1.rp[0], pt2.rp[0])) / unit.getDD()) / raster_px;
				raster_sy = ((Math.max(pt1.rp[1], pt2.rp[1]) - Math.min(pt1.rp[1], pt2.rp[1])) / unit.getDD()) / raster_py;
			}
		}

		if ((mode == 2) && (tex_id > 0))
		{

			gl.glEnable(GL.GL_TEXTURE_2D);
			gl.glEnable(GL.GL_BLEND);
			gl.glDisable(GL.GL_DEPTH_TEST);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
			gl.glColor3f(1.0f, 1.0f, 1.0f);
			gl.glBindTexture(GL.GL_TEXTURE_2D, tex_id);
			{
				double theta1 = (raster_tx + 180) * Math3D.DEGREE_TO_RADIAN_FACTOR;  // startx
				double phi1 = (raster_ty) * Math3D.DEGREE_TO_RADIAN_FACTOR; //starty

				double x_w = raster_sx * raster_px;
				double y_w = raster_sy * raster_py;

				double theta2 = (raster_tx + x_w + 180) * Math3D.DEGREE_TO_RADIAN_FACTOR;  // startx
				double phi2 = (raster_ty - y_w) * Math3D.DEGREE_TO_RADIAN_FACTOR; //starty

				int n = 0;
				if (r_dem == null)
				{
					n = (int) ((x_w > y_w) ? x_w : y_w);  // 1 degree breaks
					if (n <= 2)
					{
						n = 4;
					}
				}
				else
				{
					n = (int) Math.sqrt(r_dem.length) - 1;
				}

				float tex_w = rasterPms[0] / n;
				float tex_h = rasterPms[1] / n;

				double t1, t2, t3;
				double radius = EARTH_RADIUS + raster_avght + RAISE; // radius
				for (int j = 0; j < n; j++)
				{
					t1 = phi1 + j * (phi2 - phi1) / n;
					t2 = phi1 + (j + 1) * (phi2 - phi1) / n;

					gl.glBegin(GL.GL_TRIANGLE_STRIP);
					for (int i = 0; i <= n; i++)
					{
						t3 = (theta1 + i * (theta2 - theta1) / n);
						if (r_dem != null)
						{
							radius = EARTH_RADIUS + r_dem[j * (n + 1) + i] + RAISE;
						}
						gl.glTexCoord2f(tex_w * i, tex_h * j);
						gl.glVertex3d((radius) * Math.cos(t1) * Math.sin(t3), (radius) * Math.sin(t1), (radius) * Math.cos(t1) * Math.cos(t3));
						if (r_dem != null)
						{
							radius = EARTH_RADIUS + r_dem[(j + 1) * (n + 1) + i] + RAISE;
						}
						gl.glTexCoord2f(tex_w * i, tex_h * (j + 1));
						gl.glVertex3d((radius) * Math.cos(t2) * Math.sin(t3), (radius) * Math.sin(t2), (radius) * Math.cos(t2) * Math.cos(t3));
					}
					gl.glEnd();
				}
			}
			gl.glDisable(GL.GL_BLEND);
			gl.glEnable(GL.GL_DEPTH_TEST);
			gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);
		}

		gl.glDisable(GL.GL_TEXTURE_2D);

		int numPoints = pointStore.size();

		// reinterpolate the points every once in a while to fit the dem
		if (resetPtElevations < 0)
		{
			for (int m = 0; m < numPoints; m++)
			{
				(pointStore.elementAt(m)).setPointElevations(RAISE);
			}
			resetPtElevations = 5;
		}

		gl.glColor3f(stroke[0], stroke[1], stroke[2]);
		gl.glLineWidth(LWIDTH);

		gl.glBegin(GL.GL_LINE_STRIP);
		for (int m = 0; m < numPoints; m++)
		{
			(pointStore.elementAt(m)).draw(gl, true);
		}
		gl.glEnd();

		gl.glPointSize(PSIZE);
		gl.glColor3f(pcolor[0], pcolor[1], pcolor[2]);
		gl.glBegin(GL.GL_POINTS);
		int rednode = numPoints - (((mode == 1) && (numPoints > 2)) ? 2 : 1);
		for (int m = 0; m < numPoints; m++)
		{
			if (m == rednode)
			{
				gl.glColor3f(1.0f, 0, 0);
			}
			(pointStore.elementAt(m)).draw(gl, false);
		}
		gl.glEnd();
		gl.glEnable(GL.GL_TEXTURE_2D);

	}

	public void destroyGL(GL gl)
	{
		if (tex_id != -1) {
			ptolemy.textureManager.unload(gl, tex_id);
			tex_id = -1;
		}
	}

	private final void setMode(String pms)
	{
		pointStore = new Vector<VectorNode>(5, 5);
		setRaster = false;
		r_dem = null;
		tex_id = -1;	//FIXME unbind ?
		clickEnabled = true;
		try
		{
			mode = Integer.parseInt(pms);
			if (mode > 2)
			{
				mode = -1;
			}
		}
		catch (Exception e)
		{
			mode = -1;
		}
	}

	public final void setRasterFile(String pms)
	{
		try
		{
			StringTokenizer stok = new StringTokenizer(pms, ",");

			useWF = (stok.nextToken().equals("1")) ? true : false;
			String imfile = stok.nextToken();
			if (useWF)
			{
				raster_sx = Double.parseDouble(stok.nextToken());
				raster_sy = Double.parseDouble(stok.nextToken());
				raster_tx = Double.parseDouble(stok.nextToken());
				raster_ty = Double.parseDouble(stok.nextToken());
			}
			transparency = (short) (Float.parseFloat(stok.nextToken()) * 255);

			String ftype = imfile.substring(imfile.length() - 4, imfile.length()).toUpperCase();

			//check file type, only support png for now...
			if (ftype.equals(".PNG"))
			{
				FileInputStream fin = new FileInputStream(imfile);
				int len = fin.available();
				data = new byte[len];
				int bytesread, offset = 0;
				while (len > 0)
				{
					bytesread = fin.read(data, offset, len);
					offset += bytesread;
					len -= bytesread;
				}
				fin.close();
				setRaster = true;
				r_dem = null;
			}
			else
			{
				return;
			}
		}
		catch (Exception e)
		{
//			ptolemy.sendErrorMessage("Security Exception : Unable to read file.");
		}
	}

	private final void setRasterDEM(String pms)
	{
		if (tex_id <= 0) {
			return;
		}
		int numrows = Integer.parseInt(pms);
		r_dem = new float[numrows * numrows];

		final Ptolemy3DUnit unit = ptolemy.unit;

		double cellw = ((raster_sx * raster_px) / (numrows - 1)) * unit.getDD();
		double cellh = ((raster_sy * raster_py) / (numrows - 1)) * unit.getDD();
		double rx = (int) (raster_tx * unit.getDD());
		double ry = (int) (raster_ty * unit.getDD());
		for (int i = 0; i < numrows; i++)
		{
			for (int j = 0; j < numrows; j++)
			{
				r_dem[i * numrows + j] = (float) ptolemy.scene.landscape.groundHeight(rx + (cellw * j), ry - (cellh * i), 0);
			}
		}
	}

	public String pluginAction(String commandname, String command_params)
	{
		if (commandname.equalsIgnoreCase("status"))
		{
			STATUS = (Integer.parseInt(command_params) == 1) ? true : false;
		}
		else if (commandname.equalsIgnoreCase("setMode"))
		{
			setMode(command_params);
		}
		else if (commandname.equalsIgnoreCase("setRasterFile"))
		{
			setRasterFile(command_params);
		}
		else if (commandname.equalsIgnoreCase("setRasterDEM"))
		{
			setRasterDEM(command_params);
		}
		else if (commandname.equalsIgnoreCase("getInfo"))
		{
			return "gtl," + STATUS;
		}
		else if (commandname.equalsIgnoreCase("setInfo"))
		{
			setInfo(command_params);
		}
		else if (commandname.equalsIgnoreCase("getLayerName"))
		{
			return "PTOLEMY3D_PLUGIN_GISTOOL_" + pindex;
		}
		else if (commandname.equalsIgnoreCase("addPoint"))
		{
			try
			{
				final Ptolemy3DUnit unit = ptolemy.unit;

				StringTokenizer stok = new StringTokenizer(command_params, ",");
				double tx = Double.parseDouble(stok.nextToken());
				double ty = Double.parseDouble(stok.nextToken());
				float raise = (stok.hasMoreTokens()) ? Float.parseFloat(stok.nextToken()) : RAISE;
				if (mode >= 0)
				{
					VectorNode point;
					point = new VectorNode((int) (tx * unit.getDD()), (int) (ty * unit.getDD()), raise, true);
					addPoint(point);
				}
			}
			catch (Exception e)
			{
			}
		}
		return null;
	}

	private final void setInfo(String pms)
	{
		StringTokenizer stok = new StringTokenizer(pms);

		try
		{
			String ky;
			while (stok.hasMoreTokens())
			{
				ky = stok.nextToken();

				if (ky.equalsIgnoreCase("-stroke"))
				{
					stroke[0] = Integer.parseInt(stok.nextToken()) / 255.f;
					stroke[1] = Integer.parseInt(stok.nextToken()) / 255.f;
					stroke[2] = Integer.parseInt(stok.nextToken()) / 255.f;
				}
				else if (ky.equalsIgnoreCase("-pcolor"))
				{
					pcolor[0] = Integer.parseInt(stok.nextToken()) / 255.f;
					pcolor[1] = Integer.parseInt(stok.nextToken()) / 255.f;
					pcolor[2] = Integer.parseInt(stok.nextToken()) / 255.f;
				}
				else if (ky.equalsIgnoreCase("-status"))
				{
					STATUS = (Integer.parseInt(stok.nextToken()) == 1) ? true : false;
				}
				else if (ky.equalsIgnoreCase("-psize"))
				{
					PSIZE = Float.parseFloat(stok.nextToken());
				}
				else if (ky.equalsIgnoreCase("-lwidth"))
				{
					LWIDTH = Float.parseFloat(stok.nextToken());
				}
				else if (ky.equalsIgnoreCase("-mode"))
				{
					setMode(stok.nextToken());
				}
				else if (ky.equalsIgnoreCase("-clickEnabled"))
				{
					clickEnabled = stok.nextToken().equals("1") ? true : false;
				}
			}

		}
		catch (Exception e)
		{
		}
	}

	private boolean addPoint(VectorNode point)
	{
		int itwt = 200;

		int numPts = pointStore.size();

		if (mode == 2)
		{
			if (numPts == 1)
			{
				// add points to make a square
				VectorNode pt1 = pointStore.elementAt(0);
				VectorNode pt2 = new VectorNode(point.rp[0], pt1.rp[1], RAISE, false);
				pt1.interpolate(pt2, itwt, RAISE, 40);
				pointStore.add(pt2);
				pt2.interpolate(point, itwt, RAISE, 40);
				pointStore.add(point);
				VectorNode pt3 = new VectorNode(pt1.rp[0], point.rp[1], RAISE, false);
				point.interpolate(pt3, itwt, RAISE, 40);
				pointStore.add(pt3);
				VectorNode pt5 = new VectorNode(pt1.rp[0], pt1.rp[1], RAISE, false);
				pt3.interpolate(pt5, itwt, RAISE, 40);
				pointStore.add(pt5);
				raster_avght = 2;
				return false;
			}
			else
			{
				if (numPts > 1)
				{
					pointStore.clear();
				}
				pointStore.add(point);
			}
		}
		else
		{
			if (numPts > 0)
			{
				VectorNode prevpoint = pointStore.elementAt((((mode == 0) || (numPts <= 2)) ? numPts - 1 : numPts - 2));
				prevpoint.interpolate(point, itwt, RAISE, 40);
			}
			pointStore.add(point);
			if (mode == 1)
			{
				if (numPts >= 2)
				{
					if (numPts > 3)
					{
						pointStore.removeElementAt(numPts - 1);
					}
					VectorNode pt1 = pointStore.elementAt(0);
					VectorNode polypoint = new VectorNode(pt1.rp[0], pt1.rp[1], RAISE, false);
					point.interpolate(polypoint, itwt, RAISE, 40);
					pointStore.add(polypoint);
				}
			}

		}
		return false;

	}

	public boolean onPick(double[] intersectPoint)
	{
		final Ptolemy3DUnit unit = ptolemy.unit;

		if ((mode < 0) || (!clickEnabled)) {
			return false;
		}
		VectorNode point;

		double[] pt = new double[3];
		Math3D.setMapCoord(intersectPoint, pt);
		point = new VectorNode((int) (pt[1] * unit.getDD()), (int) (pt[0] * unit.getDD()), RAISE, true);
		return addPoint(point);
	}

	public void tileLoaderAction(Communicator JC) throws IOException
	{
	}

	public boolean pick(double[] intpt, double[][] ray)
	{
		return false;
	}

	public void reloadData()
	{
	}
}
