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
import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DEvents;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.io.BasicCommunicator;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.scene.Sky;
import org.ptolemy3d.util.PngDecoder;
import org.ptolemy3d.util.TextureLoaderGL;
import org.ptolemy3d.view.Camera;

public class LogoPlugin implements Plugin
{
	/** ptolemy Instance */
	private Ptolemy3D ptolemy;
    /** Plugin index*/
    private int pindex;

	private int numLogos = 0;
	private byte[][] logo;
	private int[][] logoMeta;
	private int[][] texId;
	int[] orientation;    // orientation
	private boolean isSet = false;
	private boolean STATUS = true;

	public LogoPlugin(){}
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
		STATUS = true;
		Communicator JC = new BasicCommunicator(ptolemy.configuration.server);
		StringTokenizer tokens = new StringTokenizer(param, ",");
		numLogos = tokens.countTokens() / 2;
		logo = new byte[numLogos][];

		byte[] idata;
		logoMeta = new int[numLogos][4];
		texId = new int[numLogos][1];
		orientation = new int[numLogos];

		try
		{
			for (int i = 0; i < numLogos; i++)
			{
				texId[i][0] = -1;
				JC.requestData(tokens.nextToken());
				orientation[i] = Integer.parseInt(tokens.nextToken());
				int t = JC.getHeaderReturnCode();
				if ((t == 200) || (t == 206))
				{
					idata = PngDecoder.decode(JC.getData(), logoMeta[i]);
					// copy data
//					int w = (logoMeta[i][0] <= 256) ? logoMeta[i][0] : 256;
//					int h = (logoMeta[i][1] <= 32) ? logoMeta[i][1] : 32;
					logo[i] = new byte[256 * 32 * logoMeta[i][3]];

					for (int a = 0; a < 32; a++)
					{
						for (int b = 0; b < 256; b++)
						{
							int offset = (a * 256 * logoMeta[i][3]) + (b * logoMeta[i][3]);
							int logo_offset = (a * logoMeta[i][0] * logoMeta[i][3]) + (b * logoMeta[i][3]);
							if ((logoMeta[i][0] > b) && (logoMeta[i][1] > a))
							{
								for (int c = 0; c < logoMeta[i][3]; c++)
								{
									logo[i][offset + c] = idata[logo_offset + c];
								}
							}
						}
					}
				}
				else
				{
					JC.flushNotFound();
				}
			}

		}
		catch (IOException e)
		{
			IO.printStackPlugin(e);
		}
		JC = null;
	}

	public void initGL(GL gl)
	{

	}

	public void motionStop(GL gl)
	{
	}

	public void draw(GL gl)
	{
		if (!STATUS) {
			return;
		}

		final Sky sky = ptolemy.scene.sky;
		final Camera camera = ptolemy.camera;
		final Ptolemy3DUnit unit = ptolemy.unit;
		final Ptolemy3DEvents drawable = ptolemy.events;

		double logoScaler = (camera.getVerticalAltitudeMeters() > sky.horizonAlt) ? ((sky.horizonAlt * unit.coordSystemRatio) / 2) : 5;

		// can only set tex if open gl is init, so we need to set our textures in this loop.
		if (!isSet) {
			for (int i = 0; i < numLogos; i++) {
				TextureLoaderGL.setGLTexture(gl, texId[i], logoMeta[i][3], 256, 32, logo[i], false);
			}
			isSet = true;
		}

		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_BLEND);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
		gl.glColor3f(1.0f, 1.0f, 1.0f);

		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glTranslated(0, 0, -logoScaler);
		logoScaler /= 5;
		gl.glScaled(logoScaler, logoScaler, logoScaler);

		double pixratiox = (((Math.tan(Math3D.degToRad * 60.0) * 5) * drawable.screenAspectRatio) / drawable.screenWidth);
		double pixratioy = ((Math.tan((Math3D.degToRad * 60.0)) * 5) / drawable.screenHeight);

		float texw, texh;

		double thextan = ((Math.tan((60.0 * Math3D.degToRad) / 2) * 5) * drawable.screenAspectRatio);
		double theytan = Math.tan(Math3D.degToRad * 30.0) * 5;

		double ulx, uly;
		float w, h;
		for (int i = 0; i < numLogos; i++)
		{
			if (texId[i][0] == -1) {
				continue;
			}

			texw = (float) logoMeta[i][0] / 256;
			texh = (float) logoMeta[i][1] / 32;

			w = (float) logoMeta[i][0] / 90;
			h = (float) logoMeta[i][1] / 90;

			gl.glBindTexture(GL.GL_TEXTURE_2D, texId[i][0]);

			switch (orientation[i])
			{
				case 1:
					ulx = thextan + pixratiox;
					uly = theytan - pixratioy;
					break;
				case 2:
					ulx = (thextan - (pixratiox * logoMeta[i][0] * drawable.screenHeight / 775.0)) - pixratiox;
					uly = theytan - pixratioy;
					break;
				case 4:
					ulx = (thextan - (pixratiox * logoMeta[i][0] * drawable.screenHeight / 775.0)) - pixratiox;
					uly = -(theytan - (pixratioy * logoMeta[i][1] * (drawable.screenHeight / 770.0))) + pixratioy + 0.15;
					break;
				default: // LLC
					ulx = thextan + pixratiox;
					uly = -(theytan - (pixratioy * logoMeta[i][1] * (drawable.screenHeight / 770.0))) + pixratioy + 0.15;
					break;
			}

			gl.glBegin(GL.GL_TRIANGLE_STRIP);
				gl.glTexCoord2f(0.0f, 0);
				gl.glVertex3d(ulx, uly, 0);

				gl.glTexCoord2f(0.0f, texh);
				gl.glVertex3d(ulx, uly - h, 0);

				gl.glTexCoord2f(texw, 0);
				gl.glVertex3d(ulx + w, uly, 0);

				gl.glTexCoord2f(texw, texh);
				gl.glVertex3d(ulx + w, uly - h, 0);
			gl.glEnd();
		}

		gl.glPopMatrix();
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_BLEND);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);
	}

	public void destroyGL(GL gl)
	{
		for (int h = 0; h < texId.length; h++)
		{
			if (texId[h][0] != -1)
			{
				gl.glDeleteTextures(1, texId[h], 0);
				texId[h][0] = -1;
			}
		}
	}

	public String pluginAction(String commandname, String command_params)
	{
		if (commandname.equalsIgnoreCase("status"))
		{
			STATUS = (Integer.parseInt(command_params) == 1) ? true : false;
		}
		else if (commandname.equalsIgnoreCase("getLayerName"))
		{
			return "PTOLEMY3D_PLUGIN_LOGO_" + pindex;
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