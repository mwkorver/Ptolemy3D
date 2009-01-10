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

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DConfiguration;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.view.Camera;

/**
 * IconPlugin shows an image at the specified position.
 * 
 * @author Antonio Santiago <asantiagop@gmail.com>
 */
public class IconPlugin implements Plugin
{

    private static final String NAME = "ICON_PLUGIN_";
    private int index = -1;
    private boolean status = true;
    //
    private String imageName = "";
    private double latitude = 0;
    private double longitude = 0;
    //
    private Ptolemy3D ptolemy = null;
    private GL gl = null;
    //
    private boolean iconLoaded = false;
    private boolean errorLoading = false;
    private boolean initTexture = false;
    private BufferedImage bufferedImage = null;
    private Texture texture = null;

    public void init(Ptolemy3D ptolemy)
    {
        this.ptolemy = ptolemy;
    }

    public void setPluginIndex(int index)
    {
        this.index = index;
    }

    public void setPluginParameters(String params)
    {
        String values[] = params.split(",");
        imageName = values[0];
        latitude = Double.valueOf(values[1]);
        longitude = Double.valueOf(values[2]);
    }

    public void motionStop(GL gl)
    {
    }

    public boolean pick(double[] intersectPoint, double[][] ray)
    {
        return false;
    }

    public boolean onPick(double[] intersectPoint)
    {
        return false;
    }

    public void tileLoaderAction(Communicator com) throws IOException
    {
    }

    public String pluginAction(String commandname, String command_params)
    {
        if (commandname.equalsIgnoreCase("status"))
        {
            status = (Integer.parseInt(command_params) == 1) ? true : false;
        }
        else if (commandname.equalsIgnoreCase("getLayerName"))
        {
            return NAME + index;
        }
        return null;
    }

    public void reloadData()
    {
    }

    public void initGL(GL gl)
    {
        if (!iconLoaded)
        {

            Thread newThread = new Thread(new Runnable()
            {

                public void run()
                {
                    URL url;
                    try
                    {
                        String server = ptolemy.configuration.server;
                        url = new URL("http://" + server + imageName);
                        bufferedImage = ImageIO.read(url);
                    }
                    catch (IOException ex)
                    {
                        Logger.getLogger(IconPlugin.class.getName()).log(Level.SEVERE, null, ex);
                        errorLoading = true;
                    }
                    finally
                    {
                        iconLoaded = true;
                    }
                }
            });

            newThread.start();
        }
    }

    /**
     * Renders the icon.
     * 
     * @param gl
     */
    public void draw(GL gl)
    {
        // Check if our point is in the visible side of the globe.
        if (!isPointInView(longitude, latitude))
        {
            return;
        }

        // If the icon image is not loaded return or there was an error loading
        // it then returns.
        if (!iconLoaded || errorLoading)
        {
            return;
        }

        // Initiallize the texture
        if (!initTexture)
        {
            initTexture = true;
            texture = TextureIO.newTexture(bufferedImage, false);
        }

        this.gl = gl;

        // Store previous matrices
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();

        // Store attributes that will change
        gl.glPushAttrib(GL.GL_CURRENT_BIT | GL.GL_ENABLE_BIT | GL.GL_COLOR_BUFFER_BIT);

        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // Compute coordinates
        Ptolemy3DUnit unit = ptolemy.unit;

        double point[] = computeCartesianSurfacePoint(latitude, longitude);
        point[0] += 5 * unit.coordSystemRatio;
        point[1] += 5 * unit.coordSystemRatio;
        point[2] += 5 * unit.coordSystemRatio;

        // Get screen coordinates before altering projection and modelview matrices.
        double scr[] = worldToScreen(point);

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

    /**
     * Checks if a point is the visible side of the globe.
     * @param lon
     * @param lat
     * @return
     */
    public static boolean isPointInView(double lon, double lat)
    {
        final Camera camera = Ptolemy3D.ptolemy.camera;

        lat *= 1000000;
        lon *= 1000000;

        //View forward vector
        double[] forward =
        {
            -camera.cameraMat.m[0][2], -camera.cameraMat.m[1][2], -camera.cameraMat.m[2][2]
        };

        //Normal on the globe
        double[] point = new double[3];
        Math3D.setSphericalCoord(lon, lat, point);

        //Angle between vector (front/back face test)
        double dot = (forward[0] * point[0]) + (forward[1] * point[1]) + (forward[2] * point[2]);
        if (dot <= 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Converts a lat/lot position into a cartesion space.
     * @param lat
     * @param lon
     * @return
     */
    private double[] computeCartesianSurfacePoint(double lat, double lon)
    {

        Landscape landscape = ptolemy.scene.landscape;

        lat *= 1000000;
        lon *= 1000000;

        // Transform from lat/lon to cartesion coordinates.
        double point[] = new double[3];
        Math3D.setSphericalCoord(lon, lat, point);

        // Get the terrain elevation
        double terrainElev = landscape.groundHeight(lon, lat, 0);

        point[0] *= (Ptolemy3DConfiguration.EARTH_RADIUS + terrainElev);
        point[1] *= (Ptolemy3DConfiguration.EARTH_RADIUS + terrainElev);
        point[2] *= (Ptolemy3DConfiguration.EARTH_RADIUS + terrainElev);

        return point;
    }

    /**
     * Projects a cartesion point in the camera space and returns the pixel
     * position in OpenGL coordinate space, that is, (0,0) at bottom-lect.
     * @param cartesian
     */
    private double[] worldToScreen(double[] cartesian)
    {
        GLU glu = new GLU();
        double model[] = new double[16];
        double proj[] = new double[16];
        int viewport[] = new int[4];

        gl.glGetDoublev(GL.GL_MODELVIEW_MATRIX, model, 0);
        gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, proj, 0);
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);

        double screen[] = new double[4];
        glu.gluProject(cartesian[0], cartesian[1], cartesian[2], model, 0, proj, 0, viewport, 0, screen, 0);

        return screen;
    }

    public void destroyGL(GL gl)
    {
    }
}
