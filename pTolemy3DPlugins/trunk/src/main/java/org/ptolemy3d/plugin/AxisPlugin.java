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

import com.sun.opengl.util.j2d.TextRenderer;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DConfiguration;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.scene.Plugin;
import org.ptolemy3d.view.Camera;

/**
 * Basic demonstration of plugin creating. Shows the cartesian axis used in
 * ptolemy3D: RED - X, GREEN - Y, BLUE - Z.
 *
 * @author Antonio Santiago <asantiagop@gmail.com>
 */
public class AxisPlugin implements Plugin
{

    private static final String NAME = "AXIS_PLUGIN_";
    private int index = -1;
    private boolean status = true;
    //
    private Ptolemy3D ptolemy = null;
    private GL gl = null;
    private TextRenderer renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 14));
    //
    private boolean showLabels = false;

    /**
     * Initialize all non OpenGL datas. This is called just after the object instanciation.
     * OpenGL Context is NOT current here, initialize all OpenGL related datas in <code>initGL</code>.
     */
    public void init(Ptolemy3D ptolemy)
    {
        this.ptolemy = ptolemy;
    }

    /**
     * Notify the index of the plugin.
     */
    public void setPluginIndex(int index)
    {
        this.index = index;
    }

    /**
     * Called a single time at initialisation.
     */
    public void setPluginParameters(String params)
    {
        String p[] = params.split(",");
        if (p.length > 0)
        {
            showLabels = true;
        }
    }

    /**
     * Called when camera motion is stopped.
     */
    public void motionStop(GL gl)
    {
    }

    /**
     * Ray trace to find an intersection with the plugin geometry.
     */
    public boolean pick(double[] intersectPoint, double[][] ray)
    {
        return false;
    }

    /**
     * Called when the landscape has been picked.
     * @param intersectPoint picking intersection point.
     */
    public boolean onPick(double[] intersectPoint)
    {
        return false;
    }

    /**
     * Call by the tile loader to let the plugin request data.
     */
    public void tileLoaderAction(Communicator com) throws IOException
    {
    }

    /**
     * Execute a plugin command.
     *
     * @param commandName command type
     * @param commandParams command parameters
     * @return if any value is returned, return it in a Stirng.
     */
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

    /**
     * Called when landscape status has been changed. Plugin must reload some data due to this change.
     */
    public void reloadData()
    {
    }

    /**
     * Initialize all OpenGL datas. OpenGL Context is current.
     */
    public void initGL(GL gl)
    {
    }

    /**
     * Render OpenGL geometry.
     */
    public void draw(GL gl)
    {
        if (!status)
        {
            return;
        }

        this.gl = gl;

        double rad = Ptolemy3DConfiguration.EARTH_RADIUS * 1.1;
        double x_axis[] =
        {
            rad, 0, 0
        };
        double y_axis[] =
        {
            0, rad, 0
        };
        double z_axis[] =
        {
            0, 0, rad
        };

        // Store attributes that will change
        gl.glPushAttrib(
                GL.GL_CURRENT_BIT |
                GL.GL_ENABLE_BIT |
                GL.GL_LINE_BIT);
        gl.glDisable(GL.GL_TEXTURE_2D);

        // Draw line axis
        gl.glLineWidth(3f);
        gl.glBegin(GL.GL_LINES);
        gl.glColor3f(1.0f, 0f, 0f);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3dv(x_axis, 0);
        gl.glColor3f(0f, 1.0f, 0f);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3dv(y_axis, 0);
        gl.glColor3f(0f, 0f, 1.0f);
        gl.glVertex3d(0, 0, 0);
        gl.glVertex3dv(z_axis, 0);
        gl.glEnd();

        // Get screen coordinates
        double x_scr[] = worldToScreen(x_axis);
        double y_scr[] = worldToScreen(y_axis);
        double z_scr[] = worldToScreen(z_axis);

        // Render axis labels
        if (showLabels)
        {
            // Check what points are visible before changing the matrices.
            boolean x_visible = isCartesianPointInView(x_axis);
            boolean y_visible = isCartesianPointInView(y_axis);
            boolean z_visible = isCartesianPointInView(z_axis);

            // Store previous matrices and set orthographic perspective
            int viewport[] = new int[4];
            gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);

            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glOrtho(0, viewport[2], 0, viewport[3], -1, 1);
            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();

            // Draw text
            renderer.beginRendering(viewport[2], viewport[3]);
            if (x_visible)
            {
                renderer.setColor(Color.RED);
                renderer.draw("AXIS X", (int) x_scr[0], (int) x_scr[1]);
            }
            if (y_visible)
            {
                renderer.setColor(Color.GREEN);
                renderer.draw("AXIS Y", (int) y_scr[0], (int) y_scr[1]);
            }
            if (z_visible)
            {
                renderer.setColor(Color.BLUE);
                renderer.draw("AXIS Z", (int) z_scr[0], (int) z_scr[1]);
            }
            renderer.endRendering();

            gl.glMatrixMode(GL.GL_MODELVIEW);
            gl.glPopMatrix();
            gl.glMatrixMode(GL.GL_PROJECTION);
            gl.glPopMatrix();
        }

        // Restore attributes and matrices.
        gl.glPopAttrib();
    }

    /**
     * Checks if a point is the visible side of the globe.
     * @param point
     * @return
     */
    public boolean isCartesianPointInView(final double point[])
    {
    	return Math3D.isPointInView(point);
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

    /**
     * Destroy all OpenGL Related Datas. OpenGL Context is current.
     */
    public void destroyGL(GL gl)
    {
    }
}
