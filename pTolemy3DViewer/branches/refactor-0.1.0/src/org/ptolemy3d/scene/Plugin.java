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
package org.ptolemy3d.scene;

import java.io.IOException;

import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.io.Communicator;

/**
 * <H1>Overview</H1>
 * <BR>
 * This is the interface between the plugin manager and a user-defined plugin.<BR>
 * The plugin manager, like the name indicates , manage plugins and do the link with Ptolemy3D viewer.
 *
 * A Ptolemy3D plugin definition.
 */
public interface Plugin {

    /** Initialize all OpenGL datas.<BR>
     * <I>OpenGL Context is current.</I>
     * @see #init(Ptolemy3D) */
    public void initGL(DrawContext drawContext);

    /** Render OpenGL geometry. */
    public void draw(DrawContext drawContext);

    /** Destroy all OpenGL Related Datas.<BR>
     * <I>OpenGL Context is current.</I> */
    public void destroyGL(DrawContext drawContext);

    /** Notify the index of the plugin.*/
    public void setPluginIndex(int index);

    /** Called a single time at initialisation. */
    public void setPluginParameters(String param);

    /** Called when camera motion is stopped. */
    public void motionStop(GL gl);

    /** Ray trace to find an intersection with the plugin geometry. */
    public boolean pick(double[] intersectionPoint, double[][] ray);

    /** Called when the landscape has been picked.
     * @param intersectPoint picking intersection point. */
    public boolean onPick(double[] intersectPoint);

    /** Call by the tile loader to let the plugin request data. */
    public void tileLoaderAction(Communicator JC) throws IOException;

    /** Execute a plugin command.
     * @param commandName command type
     * @param commandParams command parameters
     * @return if any value is returned, return it in a Stirng. */
    public String pluginAction(String commandName, String commandParams);

    /** Called when landscape status has been changed. Plugin must reload some data due to this change. */
    public void reloadData();
}