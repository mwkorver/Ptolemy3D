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

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.ptolemy3d.DrawContext;
import org.ptolemy3d.Ptolemy3DGLCanvas;
import org.ptolemy3d.debug.IO;

/**
 * <H1>Overview</H1> <BR>
 * Manage plugins and interface them with Ptolemy3D:<BR>
 * <ul>
 * <li>Initialize plugins</li>
 * <li>Load balance for plugins data downloading.</li>
 * <li>Plugins rendering: including visibility, geometry update with landscape
 * incoming datas ...</li>
 * <li>Geometry picking: select plugin object with the mouse.</li>
 * </ul>
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Plugins {

	private List<Plugin> plugins;
	private int pluginCue = 0; // TODO Don't know its uses ...

	// private int tl_rotation = 0;
	// private boolean running = false;

	public Plugins() {
		this.plugins = new ArrayList<Plugin>();
	}

	/**
	 * Number of plugins.
	 * 
	 * @return the number of plugins
	 */
	public int getNumPlugins() {
		return plugins.size();
	}

	/**
	 * Adds a new plugin to the list.
	 * 
	 * @param plugin
	 */
	public boolean addPlugin(Plugin plugin) {
		if (plugin != null) {
			return plugins.add(plugin);
		}
		return false;
	}

	/**
	 * Removes a plugin from the list.
	 * 
	 * @param plugin
	 */
	public boolean removePlugin(Plugin plugin) {
		if (plugin != null) {
			return plugins.remove(plugin);
		}
		return false;
	}

	/**
	 * Instanciate and add a plugin in the list.
	 * 
	 * @param className
	 *            the fully qualified name of the desired class.
	 * @param pluginParams
	 *            plugin parameters
	 */
	public boolean addPlugin(String className, String pluginParams) {

		try {
			Class<?> clazz = Class.forName(className);
			Plugin plugin = (Plugin) clazz.newInstance();

			if (plugin != null) {
				plugin.setPluginParameters(pluginParams);
				return plugins.add(plugin);
			}
		} catch (Exception e) {
			IO.printlnError("Plugin not found: " + className);
		}
		return false;
	}

	protected final void initGL(DrawContext drawContext) {

		if (plugins != null) {
			for (Plugin plugin : plugins) {
				if (plugin != null) {
					try {
						plugin.initGL(drawContext);
					} catch (Exception e) {
						IO.printStackPlugin(e);
					}
				}
			}
		}
	}

	protected void prepareFrame(DrawContext drawContext) {
		GL gl = drawContext.getGL();
		Ptolemy3DGLCanvas canvas = drawContext.getCanvas();

		// running = true;

		boolean hasPlugins = plugins.size() > 0;
		if (hasPlugins && (!canvas.getCameraMovement().isActive)
				&& (canvas.getCameraMovement().inAutoPilot == 0)) {
			try {
				plugins.get(pluginCue++).motionStop(gl);
			} catch (Exception e) {
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
			} catch (Exception e) {
				IO.printStackRenderer(e);
			}
		}
		// FIXME Not handled
		// for (int i = numPlugins - 1; i >= 0; i--) {
		// if (plugins.get(i).getType() == Ptolemy3DPlugin.PLUGINTYPE_VECTOR) {
		// ((VectorPlugin) plugins.get(i)).drawVectorLabels();
		// }
		// }
	}

	protected final void destroyGL(DrawContext drawContext) {

		for (int h = 0; h < plugins.size(); h++) {
			try {
				plugins.get(h).destroyGL(drawContext);
			} catch (Exception e) {
				IO.printStackPlugin(e);
			}
		}
	}

	/** Call by the tile loader to let the plugin request data. */
	// public final void tileLoaderAction(Communicator JC) {
	// if (!running) {
	// return;
	// }
	// boolean hasPlugins = plugins.size() > 0;
	// if (hasPlugins) {
	// try {
	// plugins.get(tl_rotation++).tileLoaderAction(JC);
	// }
	// catch (Exception e) {
	// IO.printStackPlugin(e);
	// }
	// if (tl_rotation >= plugins.size()) {
	// tl_rotation = 0;
	// }
	// }
	// }
	/**
	 * Called when landscape status has been changed. Plugin must reload some
	 * data due to this change.
	 */
	public final void reloadData() {
		boolean hasPlugins = plugins.size() > 0;
		if (hasPlugins) {
			for (int i = 0; i < plugins.size(); i++) {
				try {
					plugins.get(i).reloadData();
				} catch (RuntimeException e) {
					IO.printStackPlugin(e);
				}
			}
		}
	}

	/** Pick plugins. */
	public final synchronized boolean pick(double[] intersectPoint,
			double[][] ray) {
		boolean hasPlugins = plugins.size() > 0;
		if (hasPlugins) {
			for (int i = plugins.size() - 1; i >= 0; i--) {
				try {
					if (plugins.get(i).pick(intersectPoint, ray)) {
						return true;
					}
				} catch (RuntimeException e) {
					IO.printStackPlugin(e);
				}
			}
		}
		return false;
	}

	/**
	 * Called when the landscape has been picked.
	 * 
	 * @param intersectPoint
	 *            picking intersection point.
	 */
	public synchronized boolean onPick(double[] intersectPoint) {
		boolean hasPlugins = plugins.size() > 0;
		if (hasPlugins) {
			for (int i = plugins.size() - 1; i >= 0; i--) {
				try {
					if (plugins.get(i).onPick(intersectPoint)) {
						return true;
					}
				} catch (RuntimeException e) {
					IO.printStackPlugin(e);
				}
			}
		}
		return false;
	}
}
