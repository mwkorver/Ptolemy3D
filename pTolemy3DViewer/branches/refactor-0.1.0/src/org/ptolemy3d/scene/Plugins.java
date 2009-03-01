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

import java.util.Vector;

import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.io.Communicator;

/**
 * <H1>Overview</H1>
 * <BR>
 * Manage plugins and interface them with Ptolemy3D:<BR>
 * <ul>
 * <li>Initialize plugins</li>
 * <li>Load balance for plugins data downloading.</li>
 * <li>Plugins rendering: including visibility, geometry update with landscape incoming datas ...</li>
 * <li>Geometry picking: select plugin object with the mouse.</li>
 * </ul>
 */
public class Plugins {

    /** Plugin list */
    protected Vector<Plugin> plugins;
    private int pluginCue = 0;		//TODO Don't know its uses ...
    private int tl_rotation = 0;
    private boolean running = false;

    protected Plugins() {
        this.plugins = new Vector<Plugin>();
    }

    /** Number of plugins.
     * @return the number of plugins */
    public final int getNumPlugins() {
        return plugins.size();
    }

    /**
     * Instanciate and add a plugin in the list.
     * @param className the fully qualified name of the desired class.
     * @param pluginParams plugin parameters
     */
    public final void addPlugin(String className, String pluginParams) {
        Plugin plugin = null;
        try {
            Class<?> clazz = Class.forName(className);
            Plugin pluginInstance = (Plugin) clazz.newInstance();

            plugin = pluginInstance;
        }
        catch (Exception e) {
            IO.printlnError("Plugin not found: " + className);
        }

        if (plugin != null) {
            int index = plugins.size();

            try {
                plugin.setPluginIndex(index);
                plugin.setPluginParameters(pluginParams);
            }
            catch (Exception e) {
                IO.printStackPlugin(e);
            }

            plugins.add(plugin);
        }
    }

    protected final void initGL(DrawContext drawContext) {

        if (plugins != null) {
            for (Plugin plugin : plugins) {
                if (plugin != null) {
                    try {
                        plugin.initGL(drawContext);
                    }
                    catch (Exception e) {
                        IO.printStackPlugin(e);
                    }
                }
            }
        }
    }

    protected void prepareFrame(DrawContext drawContext) {
        GL gl = drawContext.getGl();
        Ptolemy3DGLCanvas canvas = drawContext.getCanvas();

        running = true;

        boolean hasPlugins = plugins.size() > 0;
        if (hasPlugins && (!canvas.getCameraMovement().isActive) && (canvas.getCameraMovement().inAutoPilot == 0)) {
            try {
                plugins.get(pluginCue++).motionStop(gl);
            }
            catch (Exception e) {
                IO.printStackPlugin(e);
            }

            if (pluginCue >= plugins.size()) {
                pluginCue = 0;
            }
        }
    }

    protected final void draw(DrawContext drawContext) {
        int numPlugins = plugins.size();
        for (int i = numPlugins - 1; i >= 0; i--) {
            try {
                plugins.get(i).draw(drawContext);
            }
            catch (Exception e) {
                IO.printStackRenderer(e);
            }
        }
    //FIXME Not handled
//		for (int i = numPlugins - 1; i >= 0; i--) {
//			if (plugins.get(i).getType() == Ptolemy3DPlugin.PLUGINTYPE_VECTOR) {
//				((VectorPlugin) plugins.get(i)).drawVectorLabels();
//			}
//		}
    }

    protected final void destroyGL(DrawContext drawContext) {

        for (int h = 0; h < plugins.size(); h++) {
            try {
                plugins.get(h).destroyGL(drawContext);
            }
            catch (Exception e) {
                IO.printStackPlugin(e);
            }
        }
    }

    /** Call by the tile loader to let the plugin request data. */
    public final void tileLoaderAction(Communicator JC) {
        if (!running) {
            return;
        }
        boolean hasPlugins = plugins.size() > 0;
        if (hasPlugins) {
            try {
                plugins.get(tl_rotation++).tileLoaderAction(JC);
            }
            catch (Exception e) {
                IO.printStackPlugin(e);
            }
            if (tl_rotation >= plugins.size()) {
                tl_rotation = 0;
            }
        }
    }

    /** Execute a plugin command.
     * @param commandName command type
     * @param commandParams command parameters
     * @return if any value is returned, return it in a Stirng. */
    public final String pluginAction(int pluginIndex, String commandName, String commandParams) {
        boolean hasPlugins = plugins.size() > 0;
        if (!hasPlugins) {
            return "NA";
        }
        if ((pluginIndex > plugins.size()) || (pluginIndex <= 0)) {
            return "NA";
        }
        try {
            return plugins.get(pluginIndex - 1).pluginAction(commandName, commandParams);
        }
        catch (RuntimeException e) {
            IO.printStackPlugin(e);
            return null;
        }
    }

    /** Called when landscape status has been changed. Plugin must reload some data due to this change. */
    public final void reloadData() {
        boolean hasPlugins = plugins.size() > 0;
        if (hasPlugins) {
            for (int i = 0; i < plugins.size(); i++) {
                try {
                    plugins.get(i).reloadData();
                }
                catch (RuntimeException e) {
                    IO.printStackPlugin(e);
                }
            }
        }
    }

    /** Pick plugins. */
    public final synchronized boolean pick(double[] intersectPoint, double[][] ray) {
        boolean hasPlugins = plugins.size() > 0;
        if (hasPlugins) {
            for (int i = plugins.size() - 1; i >= 0; i--) {
                try {
                    if (plugins.get(i).pick(intersectPoint, ray)) {
                        return true;
                    }
                }
                catch (RuntimeException e) {
                    IO.printStackPlugin(e);
                }
            }
        }
        return false;
    }

    /** Called when the landscape has been picked.
     * @param intersectPoint picking intersection point. */
    public synchronized boolean onPick(double[] intersectPoint) {
        boolean hasPlugins = plugins.size() > 0;
        if (hasPlugins) {
            for (int i = plugins.size() - 1; i >= 0; i--) {
                try {
                    if (plugins.get(i).onPick(intersectPoint)) {
                        return true;
                    }
                }
                catch (RuntimeException e) {
                    IO.printStackPlugin(e);
                }
            }
        }
        return false;
    }
}
