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
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.Unit;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.Position;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;

/**
 * IconPlugin shows an image at the specified position.<br/>
 * This plugins accepts three parameters:
 * 
 * <ul>
 * <li>Image path, path relative to the 'Server' configuration element, that
 * points to the image.</li>
 * <li>latitude and longitude, position of the icon.</li>
 * <li></li>
 * </ul>
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 */
public class IconPlugin implements Plugin {

	private boolean status = true;
	//
	private String fileUrl = "";
	private Position position = null;
	//
	private boolean iconLoaded = false;
	private boolean errorLoading = false;
	private boolean initTexture = false;
	private BufferedImage bufferedImage = null;
	private Texture texture = null;

	/**
	 * Default constructor.
	 */
	public IconPlugin() {

	}

	/**
	 * Creates a new instance with the specified image.
	 * 
	 * @param fileUrl
	 */
	public IconPlugin(String fileUrl, Position position) {
		this.fileUrl = fileUrl;
		this.position = position;
	}

	/**
	 * Set the image url to be used as the icon.
	 * 
	 * @param fileUrl
	 */
	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	public String getFileUrl() {
		return this.fileUrl;
	}

	/**
	 * Specified the icon position.
	 * 
	 * @param position
	 */
	public void setPosition(Position position) {
		this.position = position;
	}

	public Position getPosition() {
		return this.position;
	}

	public void initGL(DrawContext drawContext) {

		if (!iconLoaded && !errorLoading) {

			Thread newThread = new Thread(new Runnable() {

				public void run() {
					URL url = null;
					try {
						url = new URL(fileUrl);
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

	public void setPluginParameters(String params) {
		// TODO - Remove this method ????
		
		String values[] = params.split(",");
		this.fileUrl = values[0];
	}

	public void motionStop(GL gl) {
	}

	public boolean pick(double[] intersectPoint, double[][] ray) {
		return false;
	}

	public boolean onPick(double[] intersectPoint) {
		return false;
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

		double latDD = position.getLatitudeDD();
		double lonDD = position.getLongitudeDD();

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

		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glEnable(GL.GL_BLEND);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		// Compute coordinates
		double point[] = Camera.computeCartesianSurfacePoint(lonDD, latDD);

		// Get screen coordinates before altering projection and modelview
		// matrices.
		double scr[] = camera.worldToScreen(drawContext, point);

		// Set an orthographic projection.
		int viewport[] = new int[4];
		Ptolemy3DGLCanvas canvas = drawContext.getCanvas();
		viewport[0] = canvas.getX();
		viewport[1] = canvas.getY();
		viewport[2] = canvas.getWidth();
		viewport[3] = canvas.getHeight();

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
