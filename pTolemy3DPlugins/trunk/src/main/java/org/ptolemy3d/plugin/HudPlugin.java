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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DConfiguration;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.font.FntFont;
import org.ptolemy3d.font.FntFontRenderer;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.scene.Sky;
import org.ptolemy3d.tile.Jp2TileLoader;
import org.ptolemy3d.util.TextureLoaderGL;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.LatLonAlt;

/**
 * Default ptolemy HUD.<BR>
 */
public class HudPlugin implements Plugin
{
	/** Instance of ptolemy*/
	private Ptolemy3D ptolemy;
	/** HUD Enabled */
	public boolean drawHud = true;
	/** HUD Background Texture IDs */
	private int[] backgroundTexID = null;

	/* Prefix / Suffix */
	public String hudPrefix1 = "Center:";
	public String hudSuffix1 = "dd";
	public String hudPrefix2 = "D/L Speed:";
	public String hudSuffix2 = "kb/sec";;
	public String hudPrefix3 = "Altitude:";
	public String hudSuffix3 = "m";

	//Font
	public String HudFontFile;

	/** Download speed */
	public double kbpersec = 0;		//TODO That's may be a good thing to move this elsewhere
	private int kbps_ticker = 0;
	private long last_time = System.currentTimeMillis();

	public HudPlugin() {}
	public void init(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
	}

	/**
	 * Init HUD
	 */
	public void initGL(GL gl)
	{
		backgroundTexID = new int[1];

		byte[] data = new byte[4];
		Ptolemy3DConfiguration settings = ptolemy.configuration;
		data[0] = (byte) settings.hudBackgroundColor.getRed();
		data[1] = (byte) settings.hudBackgroundColor.getGreen();
		data[2] = (byte) settings.hudBackgroundColor.getBlue();
		data[3] = (byte) settings.hudBackgroundColor.getAlpha();
		TextureLoaderGL.setGLTexture(gl, backgroundTexID, 4, 1, 1, data, false);

		if(ptolemy.fontTextRenderer == null) {
			FntFont font;
			try {
				font = new FntFont(HudFontFile);
			}
			catch(Exception e) {
				font = null;
				IO.printStack(e);
			}
			if(font != null) {
				ptolemy.fontTextRenderer = new FntFontRenderer(ptolemy, font);
				ptolemy.fontTextRenderer.initGL(gl);
			}
		}
		if(ptolemy.fontNumericRenderer == null) {
			ptolemy.fontNumericRenderer = ptolemy.fontTextRenderer;
		}

		//String ustr = (UNITS == 0) ? "/XY.png" : "/dd.png";
		//
		//addResourcePng("/ctr.png" ,hud_ix,1 );
		//addResourcePng(ustr ,hud_ix,2 );
		//addResourcePng("/kb.png" ,hud_ix,3 );
		//addResourcePng("/alt.png" ,hud_ix,4 );
		//addResourcePng("/m.png" ,hud_ix,5 );
		//
		//addTextTex(ustr ,128,32,hud_ix,2);
		//addTextTex("kb/sec: ",128,32,hud_ix,3);
		//addTextTex("alt:",128,32,hud_ix,4);
		//addTextTex("m",32,32,hud_ix,5);
	}

	public void destroyGL(GL gl)
	{
		if (backgroundTexID != null) {
			gl.glDeleteTextures(1, backgroundTexID, 0);
			backgroundTexID = null;
		}
	}

	/** Draw HUD */
	public void draw(GL gl)
	{
		if (!drawHud) {
			return;
		}

		if(ptolemy.fontTextRenderer == null || ptolemy.fontNumericRenderer == null) {
			IO.printError("Font Numeric Renderer destroyed !");
			return;
		}
		final FntFontRenderer fontText    = ptolemy.fontTextRenderer;
		final FntFontRenderer fontNumeric = ptolemy.fontNumericRenderer;

		final Sky sky = ptolemy.scene.sky;
		final Camera camera = ptolemy.camera;
		final LatLonAlt latLonAlt = camera.getLatAltLon();
		final Ptolemy3DUnit unit = ptolemy.unit;
		final int DDBuffer = unit.DD;

		int sclsize = 50;
		double logoScaler = (camera.getVerticalAltitudeMeters() > sky.horizonAlt) ? ((sky.horizonAlt * unit.coordSystemRatio) / 2) : 5;
		double aspect = ptolemy.events.drawAspectRatio;
		double pixratiox = (((Math.tan(Math3D.degToRad * 60.0) * sclsize) * aspect) / ptolemy.events.drawWidth);
		//double pixratioy = ( (Math.tan(JetMath3d.degToRadConst * 60.0) * sclsize)           / ptolemy.drawable.drawHeigh);

		double indentx = pixratiox;

		double ulx, uly, urx;

		ulx = -((Math.tan((60.0 * Math3D.degToRad) / 2) * sclsize) * aspect) + indentx;
		urx = (((Math.tan((60.0 * Math3D.degToRad) / 2) * sclsize) * aspect));
		uly = -(Math.tan(Math3D.degToRad * 30.0) * sclsize);

		/* Keep text at constant size, when window is resized */
		float /*scalerX = 1, */scalerY = 1;
		Dimension scrnsize = Toolkit.getDefaultToolkit().getScreenSize();
		//scalerX = (float) scrnsize.width / (float)ptolemy.events.drawWidth;
		scalerY = (float) scrnsize.height / (float)ptolemy.events.drawHeight;

		gl.glDisable(GL.GL_DEPTH_TEST);
		gl.glEnable(GL.GL_BLEND);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glTranslated(0, 0, -logoScaler);

		logoScaler /= sclsize;

		gl.glScaled(logoScaler, logoScaler, logoScaler);

		gl.glColor3f(1.0f, 1.0f, 1.0f);

		// bg box
		gl.glBindTexture(GL.GL_TEXTURE_2D, backgroundTexID[0]);
		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3d(ulx, uly + (1.157 * scalerY), 0);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3d(ulx, uly, 0);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3d(urx, uly, 0);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3d(urx, uly + (1.157 * scalerY), 0);
		gl.glEnd();

		int decplace = 5;

		float str_length = 0;

		fontText.bindTexture(gl);
		str_length += fontText.drawLeftToRight(gl, hudPrefix1, ulx, uly + 0.09);

		double dval;

		fontNumeric.bindTexture(gl);
		{//DD
			//lat
			dval = latLonAlt.getLatitudeDD() / DDBuffer;//using texture font
			str_length += fontNumeric.drawLeftToRight(gl, dval, decplace, ulx + str_length, uly, 0, 1, 1);
			//lon
			dval = latLonAlt.getLongitudeDD() / DDBuffer;//using texture font
			str_length += fontNumeric.drawLeftToRight(gl, dval, decplace, ulx + str_length + 1, uly, 0, 1, 1);
		}
		fontText.bindTexture(gl);

		str_length += fontText.drawLeftToRight(gl, hudSuffix1, ulx + str_length + 1, uly);	//dd or xy

		double start_mid = -(hudPrefix2.length()) / 2; //TO DO: maybe use average width factor of font

		if (start_mid < ulx + str_length + 1) {
			start_mid = ulx + str_length + (2.0 * aspect);
		}

		str_length = 0;

		str_length += fontText.drawLeftToRight(gl, hudPrefix2, start_mid, uly + 0.09);			//"download speed"
		long _time = System.currentTimeMillis();
		double elapsed = (double) (_time - last_time) / 1000;

		kbps_ticker++;
		if (kbps_ticker > 2)
		{
			if (ptolemy.tileLoader == null) {
				ptolemy.tileLoader = new Jp2TileLoader(ptolemy);
			}
			kbpersec = (ptolemy.tileLoader.getBytesRead(-1) / 1000) / elapsed;//using texture font
			kbps_ticker = 0;
		}
		//reset
		if (elapsed > 5)
		{
			last_time = _time;
			ptolemy.tileLoader.clearBytesRead(-1);
		}
		fontNumeric.bindTexture(gl);
		str_length += fontNumeric.drawLeftToRight(gl, kbpersec, 1, start_mid + str_length, uly, 0, 1, 1);//value
		fontText.bindTexture(gl);
		str_length += fontText.drawLeftToRight(gl, hudSuffix2, start_mid + str_length, uly);			//"kb/sec"

		double endLTR = start_mid + str_length; //end position of left to right strings;
		str_length = 0;
		int alt = (int) Math.rint(camera.getVerticalAltitudeMeters());
		double approx_length = (hudSuffix3.length() + hudPrefix3.length() + Integer.toString(alt).length()) * aspect;
		if ((urx - approx_length) < endLTR) {
			urx = endLTR + approx_length + (2.0 * aspect);
		}

		/*Right to left*/
		str_length += fontText.drawRightToLeft(gl, hudSuffix3, urx, uly);	//m
		fontNumeric.bindTexture(gl);
		str_length += fontNumeric.drawRightToLeft(gl, alt, urx + str_length, uly);
		fontText.bindTexture(gl);
		str_length += fontText.drawRightToLeft(gl, hudPrefix3, urx + str_length, uly + 0.09);	//alt

		// map scale
		/*
        double mapscale = (camera.altMeter * 0.57735026919189393373967470078005 * 2) / ((double)GL_WIDTH / 72.0 / 39.3701);
        drawNumericalValue((int)mapscale ,0 ,urx-22,uly+ 0.2,0, 1,1);
        //drawNumericalValue(String.valueOf( (int)mapscale ),urx-22,uly+ 0.2,0, 1,1);
		 */

		gl.glPopMatrix();

		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDisable(GL.GL_BLEND);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_DECAL);

		//drawFocalPoint();
	}

	/* Plugin functions */

	public boolean onPick(double[] intersectPoint)
	{
		return false;
	}

	public String pluginAction(String commandname, String command_params)
	{
		return null;
	}

	public int getType()
	{
		return 0;
	}

	public void motionStop(GL gl)
	{

	}

	public boolean pick(double[] intpt, double[][] ray)
	{
		return false;
	}

	public void reloadData()
	{

	}

	public void setPluginIndex(int index)
	{
	}
    public void setPluginParameters(String param)
	{
    	try
    	{
        	StringTokenizer tokens = new StringTokenizer(param);
    		while (tokens.hasMoreTokens())
    		{
    			String token = tokens.nextToken();

    			if (token.equalsIgnoreCase("-hudPrefix1")) {
    				hudPrefix1 = formatParameter(tokens.nextToken());
    			}
    			else if (token.equalsIgnoreCase("-hudSuffix1")) {
    				hudSuffix1 = formatParameter(tokens.nextToken());
    			}
    			else if (token.equalsIgnoreCase("-hudPrefix2")) {
    				hudPrefix2 = formatParameter(tokens.nextToken());
    			}
    			else if (token.equalsIgnoreCase("-hudSuffix2")) {
    				hudSuffix2 = formatParameter(tokens.nextToken());
    			}
    			else if (token.equalsIgnoreCase("-hudPrefix3")) {
    				hudPrefix3 = formatParameter(tokens.nextToken());
    			}
    			else if (token.equalsIgnoreCase("-hudSuffix3")) {
    				hudSuffix3 = formatParameter(tokens.nextToken());
    			}
    		}
    	}
    	catch (Exception e)
    	{
    	}
    }
	private final String formatParameter(String parameter)
	{
		return parameter.replace("%20", " ");
	}

	public void tileLoaderAction(Communicator JC) throws IOException
	{

	}
}