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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.StringTokenizer;
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.font.FntFontRenderer;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.plugin.util.VectorNode;
import org.ptolemy3d.view.Camera;

public class GendenPlugin implements Plugin
{
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;

	private double gridX = 0;
	private double gridY = 0;
	private int windDir = 0;
	private int circ = -1;
	private int evac = -1;
	private int indoor = -1;
	private int gridSize = 10;
	private int gridDirections = 16;
	private boolean STATUS;
	private boolean paramsSet = false;
	private int GRIDBREAKS = gridDirections * 2;
	private int DISP_MAXZ = Integer.MAX_VALUE;
	private int DISP_MINZ = 0;
	private float gridLineWidth;
	private float[] gridcolor = new float[3];
	private VectorNode gridnodes[][] = null;
	private int[] gtex_id = new int[3];
	private boolean colorsSet = false;
	byte[][] colortextures = new byte[3][4];
	private boolean turnOffGrid = false;

	public GendenPlugin() {}
	public void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;

		Arrays.fill(gtex_id, -1);
		gridcolor[0] = gridcolor[1] = gridcolor[2] = 0.9f;
		gridLineWidth = 2;
	}

	public void setPluginIndex(int index)
	{
	}
    public void setPluginParameters(String param)
	{
		if (param.substring(0, 1).equals("P"))
		{
			StringTokenizer ptok = new StringTokenizer(param, " ");
			String ky;
			STATUS = false;
			while (ptok.hasMoreTokens())
			{
				try
				{
					ky = ptok.nextToken();
					if (ky.equalsIgnoreCase("-status"))
					{
						STATUS = (Integer.parseInt(ptok.nextToken()) == 1) ? true : false;
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
					else if (ky.equalsIgnoreCase("-linew"))
					{
						gridLineWidth = Float.parseFloat(ptok.nextToken());
					}
					else if (ky.equalsIgnoreCase("-stroke"))
					{
						StringTokenizer colstok = new StringTokenizer(ptok.nextToken(), ":");
						gridcolor[0] = (float) Integer.parseInt(colstok.nextToken()) / 255;
						gridcolor[1] = (float) Integer.parseInt(colstok.nextToken()) / 255;
						gridcolor[2] = (float) Integer.parseInt(colstok.nextToken()) / 255;
					}
				}
				catch (Exception e)
				{
				}
			}
		}


		clearGrid();
	}

	public void initGL(GL gl)
	{
	}

	public void motionStop(GL gl)
	{
		/*
        if ((!STATUS) || (gridnodes == null)) return;
        for(int i=0;i<gridnodes.length;i++){
        for(int j=0;j<gridnodes[i].length;j++){
        gridnodes[i][j].setPointElevations();
        }
        }
		 */
	}

	public synchronized void draw(GL gl)
	{
		final Camera camera = ptolemy.camera;
		if ((camera.getVerticalAltitudeMeters() >= DISP_MAXZ) || (camera.getVerticalAltitudeMeters() < DISP_MINZ) || (!STATUS) || (gridnodes == null))
		{
			return;
		}

		if (turnOffGrid)
		{
			clearGrid();
			turnOffGrid = false;
			return;
		}
		boolean calcy = ((!ptolemy.cameraController.isActive) && (ptolemy.cameraController.inAutoPilot == 0));

		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glEnable(GL.GL_BLEND);
		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

		int i, j;

		gl.glColor3f(gridcolor[0], gridcolor[1], gridcolor[2]);
		gl.glLineWidth(gridLineWidth);
		for (i = 0; i <= gridSize; i++)
		{
			gl.glBegin(GL.GL_LINE_STRIP);
			for (j = 0; j < GRIDBREAKS; j++)
			{
				gridnodes[0][i * GRIDBREAKS + j].draw(gl, calcy);
			}
			gridnodes[0][i * GRIDBREAKS].draw(gl, calcy);
			gl.glEnd();
		}

		// draw lines from center.. 16 directions
		gl.glBegin(GL.GL_LINES);
		for (j = 0; j < gridDirections; j++)
		{
			gridnodes[1][0].draw(gl, calcy);
			gridnodes[1][j + 1].draw(gl, calcy);
		}
		gl.glEnd();

		// shade in evacuation ares
		if (paramsSet)
		{
			gl.glColor3f(1.0f, 1.0f, 1.0f);
			if (!colorsSet)
			{
				if (gtex_id[0] == -1)
				{
					for (i = 0; i < 3; i++)
					{
						gtex_id[i] = ptolemy.textureManager.load(gl, colortextures[i], 1, 1, GL.GL_RGBA, true, -1);
					}
				}
				else
				{
					for (i = 0; i < 3; i++)
					{
						gl.glBindTexture(GL.GL_TEXTURE_2D, gtex_id[i]);
						gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, 1, 1, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, ByteBuffer.wrap(colortextures[i]));
					}
				}
				colorsSet = true;
			}
			gl.glEnable(GL.GL_TEXTURE_2D);
			// draw first cirlce
			gl.glBindTexture(GL.GL_TEXTURE_2D, gtex_id[0]);

			gl.glBegin(GL.GL_POLYGON);
			for (i = GRIDBREAKS - 1; i >= 0; i--)
			{
				gl.glTexCoord2d(0, 0);
				gridnodes[0][circ * GRIDBREAKS + i].draw(gl, calcy);
			}
			gridnodes[0][circ * GRIDBREAKS].draw(gl, calcy);
			gl.glEnd();

			// draw evacuate pie depending on wind direction

			if ((evac > circ) && (evac > 0))
			{
				gl.glBindTexture(GL.GL_TEXTURE_2D, gtex_id[1]);
				gl.glBegin(GL.GL_TRIANGLE_STRIP);
				i = ((windDir * 2) - 3);
				if (i < 0)
				{
					i = GRIDBREAKS + i;
				}
				for (j = 0; j <= 6; i++, j++)
				{
					if (i < 0)
					{
						i = GRIDBREAKS + i;
					}
					if (i > GRIDBREAKS - 1)
					{
						i = 0;
					}
					gridnodes[0][evac * GRIDBREAKS + i].draw(gl, calcy);
					gridnodes[0][circ * GRIDBREAKS + i].draw(gl, calcy);
				}
				gl.glEnd();

				// refuge pie

				if (indoor > evac)
				{
					gl.glBindTexture(GL.GL_TEXTURE_2D, gtex_id[2]);
					gl.glBegin(GL.GL_TRIANGLE_STRIP);
					i = ((windDir * 2) - 3);
					if (i < 0)
					{
						i = GRIDBREAKS + i;
					}
					for (j = 0; j <= 6; i++, j++)
					{
						if (i < 0)
						{
							i = GRIDBREAKS + i;
						}
						if (i > GRIDBREAKS - 1)
						{
							i = 0;
						}
						gridnodes[0][indoor * GRIDBREAKS + i].draw(gl, calcy);
						gridnodes[0][evac * GRIDBREAKS + i].draw(gl, calcy);
					}
					gl.glEnd();
				}

			}
			gl.glDisable(GL.GL_TEXTURE_2D);

		}

        FntFontRenderer numericFont = ptolemy.fontNumericRenderer;
        if (numericFont != null)
        {
    		// km numbers
    		double scale;
    		gl.glColor3f(1.0f, 1.0f, 1.0f);
    		gl.glEnable(GL.GL_TEXTURE_2D);
    		numericFont.bindTexture(gl);
    		for (i = 1; i <= gridSize; i++)
    		{

    			scale = Math3D.getScreenScaler(
    					gridnodes[0][i * GRIDBREAKS + 26].ipts[0][0],
    					gridnodes[0][i * GRIDBREAKS + 26].ipts[0][1],
    					gridnodes[0][i * GRIDBREAKS + 26].ipts[0][2],
    					90);

    			gl.glPushMatrix();
    			gl.glTranslated(
    					gridnodes[0][i * GRIDBREAKS + 26].ipts[0][0],
    					gridnodes[0][i * GRIDBREAKS + 26].ipts[0][1],
    					gridnodes[0][i * GRIDBREAKS + 26].ipts[0][2]);
    			gl.glMultMatrixd(camera.cameraMatInv.m, 0);
    			gl.glScaled(scale * 2, scale * 2, scale * 2);
    			if (i < 10)
    			{
    				numericFont.drawNumber(gl, i, 0, 0, 0, 1, 1);
    			}
    			else if (i < 20)
    			{
    				numericFont.drawNumber(gl, 1, 0, 0, 0, 1, 1);
    				numericFont.drawNumber(gl, i - 10, 0.9, 0, 0, 1, 1);
    			}
    			else
    			{
    				numericFont.drawLeftToRight(gl, i, 0, 0, 0, 0, 1, 1);
    			}

    			gl.glPopMatrix();
    		}
        }

		gl.glDisable(GL.GL_BLEND);
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);
	}

	public void destroyGL(GL gl)
	{
		for (int i = 0; i < gtex_id.length; i++)
		{
			if (gtex_id[i] != -1)
			{
				ptolemy.textureManager.unload(gl, gtex_id[i]);
				gtex_id[i] = -1;
			}
		}
	}

	/*
    x,y,gridsize
	 */
	public void setGrid(String params)
	{
		try
		{
			final Ptolemy3DUnit unit = ptolemy.unit;

			int factor = unit.getDD();

			StringTokenizer stok = new StringTokenizer(params, ",");
			gridX = (int) Double.parseDouble(stok.nextToken()) * factor;
			gridY = (int) Double.parseDouble(stok.nextToken()) * factor;
			gridSize = Integer.parseInt(stok.nextToken());

			// create the grid shapes
			VectorNode gridnodes[][] = new VectorNode[2][];
			gridnodes[0] = new VectorNode[((gridSize + 1) * GRIDBREAKS)];
			gridnodes[1] = new VectorNode[gridDirections + 1];

			double step = (Math3D.TWO_PI_VALUE) / GRIDBREAKS;
			double grcirc = gridSize * 1000; // circ is km value
			double grcircstep = grcirc / gridSize;

			double tx, tz;
			int i, j;
			for (i = 0; i <= gridSize; i++)
			{
				for (j = 0; j < GRIDBREAKS; j++)
				{
					tx = gridX + Math.sin(step * j) * (grcircstep * i);
					tz = gridY + Math.cos(step * j) * (grcircstep * i);
					gridnodes[0][i * GRIDBREAKS + j] = new VectorNode((int) tx, (int) tz, 1, true);
				}
			}

			step = (Math3D.TWO_PI_VALUE) / gridDirections;
			double halfstep = step / 2;
			gridnodes[1][0] = new VectorNode((int) gridX, (int) gridY, 1, true);
			for (j = 0; j < gridDirections; j++)
			{
				tx = gridX + Math.cos(step * j - halfstep) * (grcirc);
				tz = gridY + Math.sin(step * j - halfstep) * (grcirc);
				gridnodes[1][j + 1] = new VectorNode((int) tx, (int) tz, 1, true);
			}

			if (this.gridnodes != null)
			{
				for (i = 0; i < this.gridnodes.length; i++)
				{
					if (this.gridnodes[i] != null)
					{
						for (j = 0; j < this.gridnodes[i].length; j++)
						{
							this.gridnodes[i][j] = null;
						}
						this.gridnodes[i] = null;
					}
				}
				this.gridnodes = null;
			}

			this.gridnodes = gridnodes;
			turnOffGrid = false;
			STATUS = true;
		}
		catch (Exception e)
		{
		}
	}
	/*
    windDir,[circ],r:g:b,[evac],r:g:b,[indoor],r:g:b
	 */

	public void setGridParams(String params)
	{
		try
		{
			StringTokenizer ctok;
			StringTokenizer stok = new StringTokenizer(params, ",");
			windDir = Integer.parseInt(stok.nextToken());
			circ = Integer.parseInt(stok.nextToken());
			ctok = new StringTokenizer(stok.nextToken(), ":");
			colortextures[0][0] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[0][1] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[0][2] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[0][3] = (byte) 100;

			evac = Integer.parseInt(stok.nextToken());
			ctok = new StringTokenizer(stok.nextToken(), ":");
			colortextures[1][0] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[1][1] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[1][2] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[1][3] = (byte) 100;

			indoor = Integer.parseInt(stok.nextToken());
			ctok = new StringTokenizer(stok.nextToken(), ":");
			colortextures[2][0] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[2][1] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[2][2] = (byte) (Integer.parseInt(ctok.nextToken()) & 0xff);
			colortextures[2][3] = (byte) 100;

			if (circ >= gridSize)
			{
				circ = gridSize;
			}
			if (evac >= gridSize)
			{
				evac = gridSize;
			}
			if (indoor >= gridSize)
			{
				indoor = gridSize;
			}
			paramsSet = true;
			turnOffGrid = false;
			colorsSet = false;
			STATUS = true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void clearGrid()
	{
		//windDir = 0;
		//circ = -1;
		//evac = -1;
		//indoor = -1;
		//gridSize = 10;
		STATUS = false;
		//paramsSet = false;
	}

	public String pluginAction(String commandname, String command_params)
	{
		if (commandname.equalsIgnoreCase("status"))
		{
			STATUS = (Integer.parseInt(command_params) == 1) ? true : false;
		}
		else if (commandname.equalsIgnoreCase("setGrid"))
		{
			setGrid(command_params);
		}
		else if (commandname.equalsIgnoreCase("setGridParams"))
		{
			setGridParams(command_params);
		}
		else if (commandname.equalsIgnoreCase("clearGridParams"))
		{
			paramsSet = false;
		}
		else if (commandname.equalsIgnoreCase("clearGrid"))
		{
			turnOffGrid = true;
		}
		return null;
	}

	public boolean onPick(double[] intersectPoint)
	{
		return false;
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