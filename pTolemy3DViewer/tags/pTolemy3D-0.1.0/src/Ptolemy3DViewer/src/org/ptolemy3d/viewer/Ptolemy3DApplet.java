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
package org.ptolemy3d.viewer;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;

import netscape.javascript.JSObject;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DJavascript;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Sky;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.CameraMovement;

/**
 * Ptolemy3D in an Applet
 */
public class Ptolemy3DApplet extends Applet implements Ptolemy3DJavascript, MouseListener
{
	private static final long serialVersionUID = 1L;

	protected Ptolemy3D ptolemy;
	protected Ptolemy3DCanvas canvas;

	/** Allows communication between applet and javascript */
	private JSObject jsObject;

	public Ptolemy3DApplet() {}

	@Override
	public void init()
	{
		if(jsObject == null) {
			JSObject obj;
			try {
				obj = JSObject.getWindow(this);
			} catch(RuntimeException e) {
				e.printStackTrace();
				obj = null;
			}
			this.jsObject = obj;
		}

		if(ptolemy == null) {
			ptolemy = new Ptolemy3D();
			ptolemy.javascript = this;
			ptolemy.init(null);

			this.canvas = new Ptolemy3DCanvas(ptolemy.events);
			this.canvas.setSize(getSize());

			this.setLayout(new BorderLayout());
			this.add(this.canvas, BorderLayout.CENTER);
		}
	}

	@Override
	public void start()
	{
		if(ptolemy == null) {
			init();
		}
		ptolemy.callJavascript("sc_start", null, null, null);

		requestFocus();
	}

	@Override
	public void stop()
	{
		getInputContext().removeNotify(canvas);

		//Destroy Ptolemy3D engine
		ptolemy.destroy();
		ptolemy = null;

		//Remove 3D component
		removeAll();
		canvas = null;
	}

	@Override
	public void destroy()
	{

	}

	public void mouseEntered(MouseEvent e) {
		this.requestFocus();
	}
	public void mouseClicked(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	public void mousePressed(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}

	/* Implementations of Ptolemy3DJavascript */

	private Hashtable<String, Cursor> curVec = null;

	public Applet getApplet()
	{
		return this;
	}

	public JSObject getJSObject()
	{
		return jsObject;
	}

	/** @return the instance of Ptolemy3D */
	public Ptolemy3D getPtolemy3D()
	{
		return ptolemy;
	}

	/**
	 * Do a screenshot of next frame in the system clipboard.
	 * @see SceneClipboard#clipboardImage
	 */
	public void copyScene()
	{
		ptolemy.scene.screenshot = true;
	}

	/** @return the camera X position. */
	public double getCameraPositionX()
	{
		return ptolemy.camera.cameraX;
	}
	/** @return the camera Y position. */
	public double getCameraPositionY()
	{
		return ptolemy.camera.cameraY;
	}

	/**
	 * @param longitude longitude, unit is degree
	 * @param latitude latitude, unit is degree
	 * @param altitude altitude, zero is the altitude of the ground with no elevations.
	 * @param direction unit in degrees.
	 * @param pitch unit in degrees.
	 */
	public void setOrientation(double longitude, double latitude, double altitude, double direction, double pitch)
	{
		try {
			final Ptolemy3DUnit unit = ptolemy.unit;

			ptolemy.cameraController.setOrientation(longitude * unit.DD, latitude * unit.DD,
					altitude, direction, pitch);
		}
		catch (Exception e){ IO.printStackJavascript(e); }
	}

	/** longitude: unit is degree. */
	public void setLongitude(double longitude)
	{
		try {
			final CameraMovement cameraController = ptolemy.cameraController;
			final Camera camera = ptolemy.camera;
			final Ptolemy3DUnit unit = ptolemy.unit;

			cameraController.setOrientation(longitude * unit.DD, camera.getLatitudeDD(), camera.getAltitude(),
					camera.getDirectionDegrees(), camera.getPitchDegrees());
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return longitude in degrees. */
	public double getLongitude()
	{
		try {
			final Camera camera = ptolemy.camera;
			return camera.getLongitudeDegrees();
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0.0;
	}

	/** @return latitude: unit is degree. */
	public void setLatitude(double latitude)
	{
		try {
			final CameraMovement cameraController = ptolemy.cameraController;
			final Camera camera = ptolemy.camera;
			final Ptolemy3DUnit unit = ptolemy.unit;

			cameraController.setOrientation(camera.getLongitudeDD(), latitude * unit.DD, camera.getAltitude(),
					camera.getDirectionDegrees(), camera.getPitchDegrees());
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return latitude in degrees. */
	public double getLatitude()
	{
		try {
			final Camera camera = ptolemy.camera;
			return camera.getLatitudeDegrees();
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0.0;
	}

	/**
	 * Set the view altitude, zero is the altitude of the ground with no elevation.<BR>
	 */
	public void setAltitude(double altitude)
	{
		try {
			final CameraMovement cameraController = ptolemy.cameraController;
			final Camera camera = ptolemy.camera;

			cameraController.setOrientation(camera.getLongitudeDD(), camera.getLatitudeDD(), altitude,
					camera.getDirectionDegrees(), camera.getPitchDegrees());
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	/**
	 * @return the altitude, zero is the altitude of the ground with no elevation.
	 * @see #getAltitudeMeters()
	 */
	public double getAltitude()
	{
		try {
			final Camera camera = ptolemy.camera;
			return camera.getAltitude();	//FIXME use camera.altMeter ?
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0.0;
	}
	/**
	 * @return the altitude of the view, in meters, from the zero altitude reference.<BR>
	 * The zero altitude reference is the terrain with no elevation.
	 */
	public final double getAltitudeMeters()
	{
		try {
			final Camera camera = ptolemy.camera;
			return camera.getAltitudeDD();	//FIXME use camera.altMeter ?
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0.0;
	}
	/**
	 * Set the direction, in degrees.
	 */
	public void setDirection(double direction)
	{
		try {
			final CameraMovement cameraController = ptolemy.cameraController;
			final Camera camera = ptolemy.camera;

			cameraController.setOrientation(camera.getLongitudeDD(), camera.getLatitudeDD(), camera.getAltitude(),
					direction, camera.getPitchDegrees());
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return direction: unit is degrees. */
	public double getDirection()
	{
		try {
			final Camera camera = ptolemy.camera;
			return camera.getDirectionDegrees();
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0.0;
	}
	/** Set the pitch, in degrees. */
	public void setPitch(double pitch)
	{
		try {
			final CameraMovement cameraController = ptolemy.cameraController;
			final Camera camera = ptolemy.camera;

			cameraController.setOrientation(camera.getLongitudeDD(), camera.getLatitudeDD(), camera.getAltitude(),
					camera.getDirectionDegrees(), pitch);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return pitch: unit is degrees. */
	public double getPitch()
	{
		try {
			final Camera camera = ptolemy.camera;
			return camera.getPitchDegrees();
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0.0;
	}

	/**
	 * Fly to a position represented by a latitude/longitude/altitude.<BR>
	 * To stop the movement before its end, uses flyStop().
	 * @param lon latitude, unit is degrees.
	 * @param lat longitude, unit is degrees.
	 * @param alt altitude, zero is the altitude of the ground with no elevation.
	 * @see Landscape#DD
	 * @see #flyStop()
	 * FIXME Is the altitude in meters .
	 */
	public void flyTo(double lon, double lat, double alt)
	{
		flyTo(lon, lat, alt, 35);
	}
	public void flyTo(double lon, double lat, double alt, int arc)
	{
		flyTo(lon, lat, alt, -1, 0, 0, 0, arc);
	}
	public void flyTo(double lon, double lat, double alt, int speed, int ld_angle, int fly_angle, int type, int arc_angle)
	{
		try {
			final Ptolemy3DUnit unit = ptolemy.unit;

			ptolemy.cameraController.flyTo(lon * unit.DD, lat * unit.DD, alt,
					speed, ld_angle, fly_angle, type, arc_angle);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	public final void flyToPositionTime(double lon, double lat, double alt,
			double s_direction, double s_tilt, double flight_arc, double cruise_tilt, double time)
	{
		try {
			final Ptolemy3DUnit unit = ptolemy.unit;

			ptolemy.cameraController.flyToPositionTime(lon * unit.DD, lat * unit.DD, alt,
					s_direction, s_tilt, flight_arc, cruise_tilt, time);
		} catch (Exception e) { IO.printStackJavascript(e); }

	}
	/* Speed flight */
	public final void flyToPositionSpeed(
			double lon, double lat, double alt,
			double s_direction, double s_tilt,
			double flight_arc, double cruise_tilt, double speed)
	{
		try {
			final Ptolemy3DUnit unit = ptolemy.unit;

			ptolemy.cameraController.flyToPositionSpeed(lon * unit.DD, lat * unit.DD, alt,
					s_direction, s_tilt, flight_arc, cruise_tilt, speed);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	/**
	 * Stop flight movement, started with flyTo.
	 * @see #flyTo(double, double, double)
	 * @see #flyTo(double, double, double, double)
	 * @see #flyTo(double, double, double, double, double, double, double, double)
	 */
	public void flyStop()
	{
		try {
			ptolemy.cameraController.inAutoPilot = 0;
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	/**
	 * @see #setRealisticFlight(int)
	 */
	public void setVelocity(double nv)
	{
		try {
			ptolemy.cameraController.setVelocity(nv);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	/**
	 * @see #setRealisticFlight(int)
	 */
	public void setAccelleration(int na)
	{
		try {
			ptolemy.cameraController.setAccelleration(na);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	/**
	 * View rotation scaler.<BR>
	 * Default value is 1.
	 */
	public void setAngularSpeed(double v)
	{
		try {
			ptolemy.cameraController.setAngularSpeed(v);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return the rotation scaler. */
	public double getAngularSpeed()
	{
		try {
			return ptolemy.cameraController.getAngularSpeed();
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0;
	}

	/**
	 * Switch between two fly movement behavior. Key behavior and acceleration is dependant of the mode choosed.
	 * Default value is 0.
	 */
	public void setRealisticFlight(int v)
	{
		try {
			ptolemy.cameraController.setRealisticFlight(v);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	public int getRealisticFlight()
	{
		try {
			return ptolemy.cameraController.getRealisticFlight();
		} catch (Exception e) {
			IO.printStackJavascript(e);
			return 0;
		}
	}

	public String pluginAction(int pluginIndex, String commandName, String commandParams)
	{
		try {
			String ret = ptolemy.scene.plugins.pluginAction(pluginIndex, commandName, commandParams);
			if (ptolemy.tileLoader.isSleeping) {
				ptolemy.tileLoaderThread.interrupt();
			}
			return ret;
		} catch (Exception e) {
			IO.printStackJavascript(e);
			return null;
		}
	}

	public int getPluginSize()
	{
		return ptolemy.scene.plugins.getNumPlugins();
	}

	/**
	 * Change landscape display mode.
	 * Display modes:
	 * 0 Default display mode. (Landscape.DISPLAY_STANDARD)
	 * 1 Landscape geometry is rendered in line mode, to visualize tile triangle geometry. (Landscape.DISPLAY_MESH)
	 * 2 Color of the landscape is the elevation. (Landscape.DISPLAY_SHADEDDEM)
	 * 3 Tile color is the jp2 current resolution. (Landscape.DISPLAY_JP2RES)
	 * 4 Tile color is the tile id. (Landscape.DISPLAY_TILEID)
	 */
	public void setDisplayMode(int m)
	{
		try {
			ptolemy.scene.landscape.displayMode = (byte)m;
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return the current display mode */
	public int getDisplayMode()
	{
		try {
			return ptolemy.scene.landscape.displayMode;
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0;
	}

	/** Set terrain elevation scaler.<BR>Default value is 1. */
	public final void setTerrainScale(double terrainScaler)
	{
		try {
			ptolemy.scene.landscape.terrainScaler = terrainScaler;
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return the current terrain elevation scaler. */
	public final double getTerrainScale()
	{
		try {
			return ptolemy.scene.landscape.terrainScaler;
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0.0;
	}

	/** Enable or disable landscape terrain elevation. */
	public final void setTerrainStatus(boolean terrainStatus)
	{
		try {
			ptolemy.scene.landscape.terrainEnabled = terrainStatus;
			ptolemy.scene.plugins.reloadData();
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return the current landscape terrain elevation state (true means elevation enable, false measn no elevation). */
	public final boolean getTerrainStatus()
	{
		try {
			return ptolemy.scene.landscape.terrainEnabled;
		} catch (Exception e) { IO.printStackJavascript(e); }
		return false;
	}

	/** Enable or disable globe atmosphere */
	public final void setAtmosphere(boolean enabled)
	{
		try {
			ptolemy.scene.sky.drawSky = enabled;
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return the atmosphere state (true: enabled, false: disabled) */
	public final boolean getAtmosphere()
	{
		try {
			return ptolemy.scene.sky.drawSky;
		} catch (Exception e) { IO.printStackJavascript(e); }
		return false;
	}

	/**
	 * Set the earth atmosphere altitude, in meters.<BR>
	 * The atmosphere will be visible when the view will be above this altitude.<BR>
	 * Default value is 10000.
	 */
	public final void setHorizonAlt(int altitude)
	{
		try {
			final Sky sky = ptolemy.scene.sky;
			sky.horizonAlt = altitude;
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** @return the horizon atmosphere altitude, in meters. */
	public final int getHorizonAlt()
	{
		try {
			final Sky sky = ptolemy.scene.sky;
			return sky.horizonAlt;
		} catch (Exception e) { IO.printStackJavascript(e); }
		return 0;
	}

	public final void asciiKeyPress(int dec)
	{
		try {
			ptolemy.cameraController.inputs.asciiKeyPress(dec);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	public final void asciiKeyRelease(int dec)
	{
		try {
			ptolemy.cameraController.inputs.asciiKeyRelease(dec);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	public final void clearKeyPress()
	{
		try {
			ptolemy.cameraController.inputs.key_state = 0;
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	/**
	 * Set the behavior of the mouse clicked event.
	 * "0" Display coordinates of the position picked on the terrain and call the javascript <i>plotPosition</i> function.
	 * "1" Move to the picked position on the terrain.
	 * "2" Move and zoom to the picked position on the terrain.
	 */
	public final void setOnClickMode(int mode)
	{
		try {
			ptolemy.cameraController.inputs.currentFunction = mode;
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	public void setCursor(String cur_name)
	{
		if (curVec != null) {
			Cursor cur = curVec.get(cur_name);
			if (cur != null) {
				canvas.setCursor(cur);
			}
			else {
				canvas.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}

	public final void addCursor(String server, String file, String name)
	{
		// load custom cursors
		if (curVec == null) {
			curVec = new Hashtable<String, Cursor>(5, 5);
		}

		Image cimg = null;
		try {
			cimg = Toolkit.getDefaultToolkit().getImage(new URL("http://" + server + file));
		} catch (Exception e) {
			IO.printStackJavascript(e);
		} finally {
			curVec.put(name, Toolkit.getDefaultToolkit().createCustomCursor(cimg, new Point(0, 0), name));
		}
	}

	/**
	 * Follow the terrain elevation, ie fly at a constant altitude from the terrain ("true");
	 * Or, fly at a constant altitude from the zero altitude reference ("false").
	 * Default value is "false"
	 */
	public final void setTerrainFollowMode(boolean follow)
	{
		try {
			ptolemy.cameraController.setFollowDem(follow);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}
	/** Terrain follow mode. */
	public final boolean getTerrainFollowMode()
	{
		try {
			return ptolemy.cameraController.getFollowDem();
		} catch (Exception e) { IO.printStackJavascript(e); }
		return false;
	}

	public final void panorama(double tx, double tz, double camht, int rtimes, double rspeed, double rpitch)
	{
		try {
			ptolemy.cameraController.panorama( tx, tz, camht, rtimes, rspeed, rpitch);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	public final void setServerParameters(String server_index, String ip, String key, String jp2loc, String demloc, String urlappend)
	{
		int serverindex = -1;
		try {
			serverindex = Integer.parseInt(server_index) - 1;
		} catch (Exception e){}

		if ((serverindex >= 0) && (serverindex < ptolemy.configuration.dataServers.length))
		{

			if (ptolemy.tileLoader.on)
			{
				ptolemy.tileLoader.on = false;
				ptolemy.tileLoaderThread.interrupt();
				try {
					ptolemy.tileLoaderThread.join(5000);
				} catch (InterruptedException e) {}
			}
			StringTokenizer ptok;

			ptolemy.configuration.dataServers[serverindex] = ip;
			ptolemy.configuration.serverKeys[serverindex] = key;
			ptolemy.configuration.urlAppends[serverindex] = urlappend;
			if (jp2loc == null) {
				ptolemy.configuration.locations[serverindex] = null;
			}
			else {
				ptok = new StringTokenizer(jp2loc, ",");
				ptolemy.configuration.locations[serverindex] = new String[ptok.countTokens()];
				for (int nfloc = 0; nfloc < ptolemy.configuration.locations[serverindex].length; nfloc++) {
					ptolemy.configuration.locations[serverindex][nfloc] = addTrailingChar(ptok.nextToken(), "/");
				}
			}

			if (demloc == null) {
				ptolemy.configuration.DEMLocation[serverindex] = null;
			}
			else
			{
				ptok = new StringTokenizer(demloc, ",");
				ptolemy.configuration.DEMLocation[serverindex] = new String[ptok.countTokens()];
				for (int nfloc = 0; nfloc < ptolemy.configuration.DEMLocation[serverindex].length; nfloc++) {
					ptolemy.configuration.DEMLocation[serverindex][nfloc] = addTrailingChar(ptok.nextToken(), "/");
				}
			}
			canvas.repaint();
		}
	}
	private final String addTrailingChar(String s, String tr)
	{
		if (!s.substring(s.length() - 1, s.length()).equals(tr)) {
			return s + tr;
		}
		else {
			return s;
		}
	}

	public final void setServerHeaderKey(int server_id, String key)
	{
		try {
			ptolemy.tileLoader.setServerHeaderKey(server_id, key);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	public final void updateUrlAppend(int server_id, String v)
	{
		try {
			ptolemy.tileLoader.updateUrlAppend(server_id, v);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}


	/**
	 * TODO Duplicated with setOrientation ?
	 */
	public final void setPitchDegrees(double pitch, double pitchIncrement)
	{
		try {
			final CameraMovement cameraController = ptolemy.cameraController;
			cameraController.setPitchDegrees(pitch, pitchIncrement);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	/**
	 * Acceleration/Rotation scaler.
	 * Default value is 1.
	 */
	public final void setRelativeMotionSpeedMultiplier(double mult)
	{
		try {
			ptolemy.cameraController.setRelativeMotionSpeedMultiplier(mult);
		} catch (Exception e) { IO.printStackJavascript(e); }
	}

	/**
	 * Return true if the view is moving.
	 */
	public boolean checkFlightStatus()
	{
		if ((ptolemy.cameraController.isActive) || (ptolemy.cameraController.inAutoPilot > 0)) {
			return true;
		}
		else {
			return false;
		}
	}
}
