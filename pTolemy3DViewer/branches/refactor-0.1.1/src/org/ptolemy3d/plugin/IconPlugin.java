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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.view.Camera;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;

/**
 * IconPlugin shows an image at the specified position.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 */
public class IconPlugin implements Plugin {

	private static final String NAME = "ICON_PLUGIN_";
	private int index = -1;
	private boolean status = true;
	//
	private String imageName = "";
	private double latitude = 0;
	private double longitude = 0;
	//
	private boolean iconLoaded = false;
	private boolean errorLoading = false;
	private boolean initTexture = false;
	private BufferedImage bufferedImage = null;
	private Texture texture = null;

	public void initGL(DrawContext drawContext) {
		if (!iconLoaded) {

			Thread newThread = new Thread(new Runnable() {

				public void run() {
					URL url = null;
					try {
						String server = Ptolemy3D.getConfiguration()
								.getServer();
						url = new URL("http://" + server + imageName);
						bufferedImage = ImageIO.read(url);
					} catch (IOException ex) {
						Logger.getLogger(IconPlugin.class.getName()).warning(
								"Can't load icon: '" + url + "'");
						Logger.getLogger(IconPlugin.class.getName()).log(
								Level.WARNING, null, ex);
						errorLoading = true;
					} finally {
						iconLoaded = true;
					}
				}
			}, "IconPluginThread");

			newThread.start();
		}
	}

	public void destroyGL(DrawContext drawContext) {
	}

	public void setPluginIndex(int index) {
		this.index = index;
	}

	public void setPluginParameters(String params) {
		String values[] = params.split(",");
		imageName = values[0];
		latitude = Double.valueOf(values[1]);
		longitude = Double.valueOf(values[2]);
	}

	public void motionStop(GL gl) {
	}

	public boolean pick(double[] intersectPoint, double[][] ray) {
		return false;
	}

	public boolean onPick(double[] intersectPoint) {
		return false;
	}

	public String pluginAction(String commandname, String command_params) {
		if (commandname.equalsIgnoreCase("status")) {
			status = (Integer.parseInt(command_params) == 1) ? true : false;
		} else if (commandname.equalsIgnoreCase("getLayerName")) {
			return NAME + index;
		}
		return null;
	}

	public void reloadData() {
	}

	/**
	 * Renders the icon.
	 * 
	 * @param drawContext
	 */
	public void draw(DrawContext drawContext) {

		GL gl = drawContext.getGL();
		Camera camera = drawContext.getCanvas().getCamera();

		double latDD = latitude * Unit.getDDFactor();
		double lonDD = longitude * Unit.getDDFactor();

		// Check if our point is in the visible side of the globe.
		if (!status || !camera.isPointInView(lonDD, latDD)) {
			return;
		}

		// If the icon image is not loaded return or there was an error loading
		// it then returns.
		if (!iconLoaded || errorLoading) {
			return;
		}

		// Initialize the texture
		if (!initTexture) {
			initTexture = true;
			texture = TextureIO.newTexture(bufferedImage, false);
		}

		// Store previous matrices
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPushMatrix();

		// Store attributes that will change
		gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_ENABLE_BIT
				| GL.GL_COLOR_BUFFER_BIT);

		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		// Compute coordinates
		double point[] = camera.computeCartesianSurfacePoint(lonDD, latDD);

		// Get screen coordinates before altering projection and modelview
		// matrices.
		double scr[] = Camera.worldToScreen(drawContext, point);

		// Set an orthographic projection.
		int viewport[] = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(0.0f, viewport[2], 0.0f, viewport[3], -1.0f, 1.0f);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();

		// Bind texture and draw icon
		texture.bind();
		TextureCoords textureCoords = texture.getImageTexCoords();
		int width = texture.getImageWidth();
		int height = texture.getImageHeight();

		gl.glBegin(GL.GL_QUADS);
		gl.glTexCoord2f(textureCoords.left(), textureCoords.bottom());
		gl.glVertex2d(scr[0], scr[1]);

		gl.glTexCoord2f(textureCoords.right(), textureCoords.bottom());
		gl.glVertex2d(scr[0] + width, scr[1]);

		gl.glTexCoord2f(textureCoords.right(), textureCoords.top());
		gl.glVertex2d(scr[0] + width, scr[1] + height);

		gl.glTexCoord2f(textureCoords.left(), textureCoords.top());
		gl.glVertex2d(scr[0], scr[1] + height);
		gl.glEnd();

		// Restore attributes
		gl.glPopAttrib();

		// Restore matrices
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
	}

}
