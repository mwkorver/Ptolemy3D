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

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.tile.Jp2Tile;
import org.ptolemy3d.tile.Level;
import org.ptolemy3d.util.ByteReader;
import org.ptolemy3d.util.PngDecoder;
import org.ptolemy3d.util.TextureLoaderGL;
import org.ptolemy3d.plugin.util.ClipPoly;
import org.ptolemy3d.plugin.util.VectorNode;
import org.ptolemy3d.view.Camera;

public class BuildingPlugin implements Plugin
{
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;

	private boolean STATUS = true;
	private String GetUrl = "";
	private String ENC_LAYER = "",  LAYER = "";
	/*
    private final static int SEA_LEVEL = 0;
    private final static int FROM_GROUND = 1;
	 */
	private int ht_ref_type = 0;
	private int DISP_MAXZ = 0;
	private int DISP_MINZ = 0;
	private int DisplayRadius = 0;
	private int feature_minx = Integer.MAX_VALUE,  feature_minz = Integer.MAX_VALUE,  feature_maxx = Integer.MIN_VALUE,  feature_maxz = Integer.MIN_VALUE;
	private int QueryWidth = 10000,  numBldgs = 0;
	private int[] bu_ids;  // id in database
	private double[] bu_z;  // height of building
	private boolean[] isFixed;
	private VectorNode[][][][] bu_shape;
	private double[][] ctds; // centroids
	private int[] bu_dwid = null;
	private int[] tex_w = null;
	private int[] tex_h = null;
	private static int numWalls = 0;
	private static byte[][] Walls;
	private static int[][] WallMeta;
	private static String wallPrefix = "";
	private boolean ALL_FIXED = false;
	private boolean forceDataLoad;
	private static int[][] tex_id = new int[0][0];
	float[] norm = new float[3];
	private int loadWallId = -1;
	double[] outpoints = new double[2];
	ClipPoly mask = new ClipPoly(4), polygon = new ClipPoly();

	public BuildingPlugin(){}
	public void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;

		loadWallId = -1;
	}

	public void setPluginIndex(int index)
	{
	}
	public void setPluginParameters(String param)
	{
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
							final Ptolemy3DUnit unit = ptolemy.unit;
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
						else if (ky.equalsIgnoreCase("-enableSelect"))
						{
						}
						else if (ky.equalsIgnoreCase("-numwalls"))
						{
							numWalls = Integer.parseInt(ptok.nextToken());
						}
						else if (ky.equalsIgnoreCase("-wallimg"))
						{
							wallPrefix = ptok.nextToken();
						}
					}
					catch (Exception e)
					{
					}
				}
			}
			else
			{
				StringTokenizer ptok = new StringTokenizer(parameters, ",");
				try
				{
					STATUS = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
					ht_ref_type = Integer.parseInt(ptok.nextToken());

					GetUrl = ptok.nextToken();
					if (GetUrl.indexOf("?") == -1)
					{
						GetUrl += "?";
					}
					LAYER = ptok.nextToken();
					ENC_LAYER = java.net.URLEncoder.encode(LAYER, "UTF-8");
					QueryWidth = Integer.parseInt(ptok.nextToken());
					DisplayRadius = Integer.parseInt(ptok.nextToken());
					DISP_MAXZ = Integer.parseInt(ptok.nextToken());
					// check to see if there are any wall images
					if (ptok.hasMoreTokens())
					{
						numWalls = Integer.parseInt(ptok.nextToken());
						wallPrefix = ptok.nextToken();
					}
				}
				catch (Exception e)
				{
				}
			}
		}
		if ((numWalls > 0) && (wallPrefix.length() > 0))
		{
			tex_id = new int[numWalls][1];
			for (int i = 0; i < numWalls; i++)
			{
				tex_id[i][0] = -1;
			}
			if (numWalls > 0)
			{
				Walls = new byte[numWalls][];
				WallMeta = new int[numWalls][];
			}
		}

	}

	public void initGL(GL gl)
	{

	}

	// called when motion stops.
	public void motionStop(GL gl)
	{
		final Camera camera = ptolemy.camera;
		if ((camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS)) {
			return;
		}

		try {
			fixPoly();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/*
    fix the closest polygon and then flag it fixed
    to fix a polygon, find all tiles that the polygon lies in and clip it for each.  if only in 1 tile, then no fixing is needed.
	 */

	private void fixPoly()
	{
		if (ALL_FIXED) {
			return;
		}

		final Camera camera = ptolemy.camera;

		int i;
		int fix_id = -1;
		double closest = Double.MAX_VALUE;
		double dist;
		boolean hasTile;
		Vector<Jp2Tile> tiles = new Vector<Jp2Tile>(2, 2);
		for (i = 0; i < numBldgs; i++)
		{
			if (!isFixed[i])
			{
				dist = Math3D.distance3D(camera.getLongitudeDD(), ctds[i][0], camera.getAltitudeDD(), bu_z[i], camera.getLatitudeDD(), ctds[i][1]);
				if (dist < closest)
				{
					closest = dist;
					fix_id = i;
				}
			}
		}

		if (fix_id != -1)
		{
			int j, k, m, q;
			int numpolys;
			double ti_x, ti_z;
			for (j = 0; j < bu_shape[fix_id].length; j++)
			{
				numpolys = bu_shape[fix_id][j].length;

				for (k = 0; k < numpolys; k++)
				{ //poly
					tiles.clear();
					for (m = 0; m < bu_shape[fix_id][j][k].length; m++)
					{ // rings
						ti_x = bu_shape[fix_id][j][k][m].rp[0];
						ti_z = bu_shape[fix_id][j][k][m].rp[1];

						Jp2Tile tile = ptolemy.tileLoader.getJp2Tile(ti_x, ti_z, true);
						if (tile == null)
						{
							return;
						}
						hasTile = false;
						for (q = 0; q < tiles.size(); q++)
						{
							if (tiles.elementAt(q) == tile)
							{
								hasTile = true;
								break;
							}
						}
						if (!hasTile)
						{
							tiles.add(tile);
						}
					}
					int numtiles = tiles.size();
					if (numtiles > 1)
					{
						polygon.resetPoints(bu_shape[fix_id][j][k]);
						VectorNode[][] polyData = new VectorNode[numtiles + bu_shape[fix_id][j].length - 1][];
						System.arraycopy(bu_shape[fix_id][j], 0, polyData, 0, bu_shape[fix_id][j].length);
						int toend = 0;
						/*
						 * put first cut poly in same place,
						 * append the rest at the end so we don't cross over them again.
						 * - because above we loop on the original number of polgons-
						 */
						final Level[] levels = ptolemy.scene.landscape.levels;
						for (q = 0; q < numtiles; q++)
						{
							final Jp2Tile tile = tiles.elementAt(q);
							final Level level = levels[tile.level];

							mask.setPoint(0, tile.lon, tile.lat - level.getTileSize());
							mask.setPoint(1, tile.lon, tile.lat);
							mask.setPoint(2, tile.lon + level.getTileSize(), tile.lat);
							mask.setPoint(3, tile.lon + level.getTileSize(), tile.lat - level.getTileSize());

							polyData[q + k + toend] = polygon.clip(mask);
							toend = bu_shape[fix_id][j].length - 1 - k;
						}
						bu_shape[fix_id][j] = polyData;
					}
				}
			}
			isFixed[fix_id] = true;
		}
		else
		{
			ALL_FIXED = true;
		}
	}

	public synchronized void draw(GL gl)
	{
		final Landscape landscape = ptolemy.scene.landscape;
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		if ((camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS)) {
			return;
		}

		int i, j, k, m;

		gl.glShadeModel(GL.GL_SMOOTH);
		//Enable light
		gl.glEnable(GL.GL_LIGHT0);
		gl.glEnable(GL.GL_LIGHT1);
		gl.glEnable(GL.GL_LIGHTING);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

		double rulx = 0, rulz = 0;
		double builfloor, gfloor;
		double roofht;

		double repvalx, repvaly;

		double drcheck = DisplayRadius;

		for (i = 0; i < numBldgs; i++)
		{
			//FIXME || true !
			if (((Math3D.dot(
					(ctds[i][0] - camera.cameraPos[0]), (0 - camera.cameraPos[1]), (ctds[i][1] - camera.cameraPos[2]),
					-camera.cameraMat.m[0][2], -camera.cameraMat.m[1][2], -camera.cameraMat.m[2][2]) >= 0) || true) &&
					(Math3D.distance3D(camera.getLongitudeDD(), ctds[i][0], camera.getAltitudeDD(), bu_z[i], camera.getLatitudeDD(), ctds[i][1]) < drcheck))
			{
				for (j = 0; j < bu_shape[i].length; j++)
				{
					// multi
					for (k = 0; k < bu_shape[i][j].length; k++)
					{
						//poly
						boolean hastex = false;
						// draw the walls

						if ((numWalls > 0) && (bu_dwid[i] > 0)) {
							if (tex_id[bu_dwid[i] - 1][0] != -1) {
								gl.glBindTexture(GL.GL_TEXTURE_2D, tex_id[bu_dwid[i] - 1][0]);
								hastex = true;
							}
							else if (Walls[bu_dwid[i] - 1] != null) {
								TextureLoaderGL.setGLTexture(gl, tex_id[bu_dwid[i] - 1], WallMeta[bu_dwid[i] - 1][3], WallMeta[bu_dwid[i] - 1][0], WallMeta[bu_dwid[i] - 1][1], Walls[bu_dwid[i] - 1], false);
							}
							else {
								loadWallId = bu_dwid[i] - 1;
							}
						}

						if (!hastex) {
							gl.glDisable(GL.GL_TEXTURE_2D);
							gl.glColor3f(0.482353f, 0.4862745f, 0.49804f);
						}

						VectorNode.getCentroid(bu_shape[i][j][k], outpoints);
						gfloor = landscape.groundHeight(outpoints[0], outpoints[1], 0);
						builfloor = gfloor;

						roofht = bu_z[i] + ((ht_ref_type == 1) ? gfloor : 0);
						repvaly = 1;
						repvalx = 1;
						if ((tex_h != null) && (tex_h[i] > 0))
						{
							repvaly = (bu_z[i] / unit.coordSystemRatio) / tex_h[i];
						}
						gl.glBegin(GL.GL_QUADS);
						for (m = 0; m < bu_shape[i][j][k].length - 1; m++)
						{
							if (m < (bu_shape[i][j][k].length - 1))
							{
								setNormal(bu_shape[i][j][k][m].rp[0], bu_shape[i][j][k][m].rp[1],
										bu_shape[i][j][k][m + 1].rp[0], bu_shape[i][j][k][m + 1].rp[1],
										builfloor);
							}
							gl.glNormal3f(norm[0], norm[1], norm[2]);
							if (hastex)
							{
								if ((tex_w != null) && (tex_w[i] > 0))
								{
									repvalx = (Math3D.distance2D(bu_shape[i][j][k][m].rp[0], bu_shape[i][j][k][m + 1].rp[0], bu_shape[i][j][k][m].rp[1], bu_shape[i][j][k][m + 1].rp[1]) / unit.coordSystemRatio) / tex_w[i];
								}
								gl.glTexCoord2d(0, 0);
								bu_shape[i][j][k][m].draw(gl, roofht);
								gl.glTexCoord2d(repvalx, 0);
								bu_shape[i][j][k][m + 1].draw(gl, roofht);
								gl.glTexCoord2d(repvalx, repvaly);
								bu_shape[i][j][k][m + 1].draw(gl, builfloor);
								gl.glTexCoord2d(0, repvaly);
								bu_shape[i][j][k][m].draw(gl, builfloor);
							}
							else
							{
								bu_shape[i][j][k][m].draw(gl, roofht);
								bu_shape[i][j][k][m + 1].draw(gl, roofht);
								bu_shape[i][j][k][m + 1].draw(gl, builfloor);
								bu_shape[i][j][k][m].draw(gl, builfloor);
							}
						}
						gl.glEnd();

						if (!hastex)
						{
							gl.glEnable(GL.GL_TEXTURE_2D);
						}

						rulx = outpoints[0];
						rulz = outpoints[1];

						hastex = false;
						Jp2Tile tile = ptolemy.tileLoader.getJp2Tile(rulx, rulz, false);

						if (tile != null)
						{
							int glset = tile.curRes;
							while (tile.getTextureId(glset) == -1)
							{
								glset--;
								if (glset < 0)
								{
									break;
								}
							}
							if (glset >= 0)
							{
								hastex = true;
								gl.glBindTexture(GL.GL_TEXTURE_2D, tile.getTextureId(glset));
								rulx = tile.lon;
								rulz = tile.lat;
							}
						}
							//////////////////////// roof //////////////////////////////////
							double rstw = 0;
							if (!hastex)
							{
								gl.glDisable(GL.GL_TEXTURE_2D);
								gl.glColor3f(0.352941f, 0.4078431f, 0.4078431f);
							}
							else
							{
								final Level[] levels = ptolemy.scene.landscape.levels;
								final Level level = levels[tile.level];
								rstw = level.getTileSize();
							}
							gl.glBegin(GL.GL_POLYGON);
							gl.glNormal3f(0, 1, 0);
							for (m = bu_shape[i][j][k].length - 1; m >= 0; m--)
							{
								if (hastex)
								{
									gl.glTexCoord2d(((bu_shape[i][j][k][m].rp[0] - rulx) / rstw), (rulz - bu_shape[i][j][k][m].rp[1]) / rstw);
								}
								bu_shape[i][j][k][m].draw(gl, roofht);
							}
							gl.glEnd();
							if (!hastex)
							{
								gl.glEnable(GL.GL_TEXTURE_2D);
							}
						}
					}
			}
		}

		gl.glShadeModel(GL.GL_FLAT);
		gl.glDisable(GL.GL_LIGHT0);
		gl.glDisable(GL.GL_LIGHT1);
		gl.glDisable(GL.GL_LIGHTING);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);
	}

	public void destroyGL(GL gl)
	{
		for (int h = 0; h < tex_id.length; h++)
		{
			if (tex_id[h][0] != -1)
			{
				gl.glDeleteTextures(1, tex_id[h], 0);
				tex_id[h][0] = -1;
			}
		}
	}
	/*
    u   =   (ui, uj, uk)T
    v   =   (vi, vj, vk)T.
    Then   u * v   =   ( uj vk - uk vj ,   uk vi - ui vk ,   ui vj - uj vi )T.

    but we can simplify.
	 */

	private void setNormal(int cx, int cz, int rx, int rz, double floor)
	{
		norm[0] = (rz - cz);
		norm[1] = 0;
		norm[2] = -(rx - cx);
		double mag1 = Math.sqrt((norm[0] * norm[0]) + (norm[2] * norm[2]));
		norm[0] /= mag1;
		norm[2] /= mag1;
	}

	private final void loadData(boolean force, Communicator JC) throws IOException
	{
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;

		int tx, ty;

		tx = (int) camera.getLongitudeDD();
		ty = (int) camera.getLatitudeDD();

		int dispr = (DisplayRadius / 2);

		if ((((tx - dispr) < feature_minx) || ((tx + dispr) > feature_maxx) || ((ty + dispr) > feature_maxz) || ((ty - dispr) < feature_minz)) || (force))
		{
			byte[] data;

			feature_minx = tx - (QueryWidth / 2);
			feature_minz = ty - (QueryWidth / 2);
			feature_maxx = tx + (QueryWidth / 2);
			feature_maxz = ty + (QueryWidth / 2);
			JC.requestData(GetUrl + "&LAYER=" + ENC_LAYER + "&BBOX=" + feature_minx + "," + feature_minz + "," + feature_maxx + "," + feature_maxz + "&FACTOR=" + String.valueOf(unit.DD));
			data = JC.getData();

			int[] cur =
			{
					0
			};

			int maxpoints = 0;

			int numBldgs = ByteReader.readInt(data, cur);
			int[] bu_ids = new int[numBldgs];  // id in database
			double[] bu_z = new double[numBldgs];   // height of building
			VectorNode[][][][] bu_shape = new VectorNode[numBldgs][][][];
			double[][] ctds = new double[numBldgs][2];
			boolean[] isFixed = new boolean[numBldgs];
			int[] bu_dwid = null, tex_w = null, tex_h = null;
			if (data[cur[0]++] == 1)
			{
				bu_dwid = new int[numBldgs];
			}
			if (data[cur[0]++] == 1)
			{
				tex_w = new int[numBldgs];
			}
			if (data[cur[0]++] == 1)
			{
				tex_h = new int[numBldgs];
			}
			ALL_FIXED = false;

			int nummulti, numpoly, numrings, i, j, k, m;
			double len, tlen, cwx, cwz;
			int prev_x = 0, prev_z = 0;
			boolean isFirstPoint;
			for (i = 0; i < numBldgs; i++)
			{
				isFirstPoint = true;
				ctds[i][0] = ctds[i][1] = 0;
				tlen = cwx = cwz = 0;
				// id
				bu_ids[i] = ByteReader.readInt(data, cur);
				// ht
				bu_z[i] = ByteReader.readInt(data, cur) * unit.coordSystemRatio;

				// geometry
				nummulti = ByteReader.readInt(data, cur);
				bu_shape[i] = new VectorNode[nummulti][][];
				for (j = 0; j < nummulti; j++)
				{
					numpoly = ByteReader.readInt(data, cur);
					bu_shape[i][j] = new VectorNode[numpoly][];
					for (k = 0; k < numpoly; k++)
					{
						numrings = ByteReader.readInt(data, cur);
						if (numrings > maxpoints)
						{
							maxpoints = numrings;
						}
						bu_shape[i][j][k] = new VectorNode[numrings];
						for (m = 0; m < numrings; m++)
						{
							bu_shape[i][j][k][m] = new VectorNode(ByteReader.readInt(data, cur), ByteReader.readInt(data, cur), 0, true);
							if (!isFirstPoint)
							{
								len = Math3D.distance2D(prev_x, bu_shape[i][j][k][m].rp[0], prev_z, bu_shape[i][j][k][m].rp[1]);
								cwx += len * ((prev_x + bu_shape[i][j][k][m].rp[0]) / 2);
								cwz += len * ((prev_z + bu_shape[i][j][k][m].rp[1]) / 2);
								tlen += len;
							}
							else
							{
								isFirstPoint = false;
							}
							prev_x = bu_shape[i][j][k][m].rp[0];
							prev_z = bu_shape[i][j][k][m].rp[1];
						}
					}
				}

				ctds[i][0] = (cwx / tlen);
				ctds[i][1] = (cwz / tlen);

				if (bu_dwid != null)
				{
					bu_dwid[i] = ByteReader.readInt(data, cur);
				}
				if (tex_w != null)
				{
					tex_w[i] = ByteReader.readInt(data, cur);
				}
				if (tex_h != null)
				{
					tex_h[i] = ByteReader.readInt(data, cur);
				}
			}

			// if drawing wait to transfer data

			this.numBldgs = numBldgs;
			this.bu_ids = bu_ids;
			this.bu_z = bu_z;
			this.bu_shape = bu_shape;
			this.ctds = ctds;
			this.isFixed = isFixed;
			this.bu_dwid = bu_dwid;
			this.tex_w = tex_w;
			this.tex_h = tex_h;
		}
	}

	public String pluginAction(String commandname, String command_params)
	{
		if (commandname.equalsIgnoreCase("refresh"))
		{
			try
			{
				forceDataLoad = true;
			}
			catch (Exception e)
			{
			}
		}
		else if (commandname.equalsIgnoreCase("getLayerName"))
		{
			return LAYER;
		}
		else if (commandname.equalsIgnoreCase("status"))
		{
			STATUS = (Integer.parseInt(command_params) == 1) ? true : false;
		}
		else if (commandname.equalsIgnoreCase("getInfo"))
		{
			return "buildings," + STATUS + "," + ht_ref_type + "," + QueryWidth + "," + DisplayRadius + "," + DISP_MAXZ;
		}
		return null;
	}

	public void loadWallImg(int i, Communicator JC)
	{
		try
		{
			WallMeta[i] = new int[4];
			JC.requestData(wallPrefix + i + ".png");
			int t = JC.getHeaderReturnCode();
			if ((t == 200) || (t == 206))
			{
				Walls[i] = PngDecoder.decode(JC.getData(), WallMeta[i]);
			}
			else
			{
				JC.flushNotFound();
			}
		}
		catch (Exception e)
		{
		}
	}

	public boolean onPick(double[] intersectPoint)
	{
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Camera camera = ptolemy.camera;

		if ((camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS)) {
			return false;
		}
		int i, j, k;

		double tx, ty;
		{
			double[] out = new double[3];
			Math3D.setMapCoord(intersectPoint, out);
			ty = out[0] * unit.DD; // y
			tx = out[1] * unit.DD; // x
		}

		for (i = 0; i < numBldgs; i++)
		{
			for (j = 0; j < bu_shape[i].length; j++)
			{
				for (k = 0; k < bu_shape[i][j].length; k++)
				{
					if (VectorNode.pointInPoly(bu_shape[i][j][k], tx, ty, bu_shape[i][j][k].length))
					{
						ptolemy.callJavascript("actionById", String.valueOf(bu_ids[i]), String.valueOf(LAYER), null);
						return false;
					}
				}
			}
		}
		return false;
	}

	public void tileLoaderAction(Communicator JC) throws IOException
	{
		final Camera camera = ptolemy.camera;

		if ((!STATUS) || (camera.getVerticalAltitudeMeters() >= DISP_MAXZ)) {
			return;
		}

		loadData(forceDataLoad, JC);
		forceDataLoad = false;
		if (loadWallId != -1) {
			loadWallImg(loadWallId, JC);
			loadWallId = -1;
		}
	}

	public boolean pick(double[] intpt, double[][] ray)
	{
		return false;
	}

	public void reloadData()
	{
	}
}