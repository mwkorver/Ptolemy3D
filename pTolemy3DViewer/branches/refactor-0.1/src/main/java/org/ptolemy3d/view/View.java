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
package org.ptolemy3d.view;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix16d;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.scene.Sky;

/**
 * Modelview and Perspective matrices.
 */
public class View
{
	/** Ptolemy3D Instance */
	private final Ptolemy3D ptolemy;

	/** 'Field Of View' in degrees */
	public final double fov = 60.0;
	/** Perspective matrix */
	public Matrix16d perspective = new Matrix16d();
	/** Modelview matrix */
	public Matrix16d modelview = new Matrix16d();

	public View(Ptolemy3D ptolemy)
	{
		this.ptolemy = ptolemy;
	}

	public void update(GL gl)
	{
		/* update perspective */
		updatePerspective(gl);

		/* Update modelview */
		CameraMovement cameraController = ptolemy.cameraController;
		try {
			cameraController.setCamera(modelview);
		} catch (Exception e) {
			IO.printError(e.getMessage());
			cameraController.stopMovement();
			modelview.identityMatrix();
		}
	}

	private final void updatePerspective(GL gl)
	{
		final Ptolemy3DUnit unit = ptolemy.unit;
		final CameraMovement cameraController = ptolemy.cameraController;
		final Camera camera = ptolemy.camera;
		final Landscape landscape = ptolemy.scene.landscape;
		final Sky sky = ptolemy.scene.sky;
		final int FAR_CLIP = landscape.farClip;
		final int FOG_RADIUS = landscape.fogRadius;
		final double aspectRatio = ptolemy.events.drawAspectRatio;
		
		// maximize our precision
		if (sky.fogStateOn)
		{
			if (camera.getVerticalAltitudeMeters() > sky.horizonAlt) {
				gl.glDisable(GL.GL_FOG);
				perspective.setPerspectiveProjection(fov, aspectRatio, ((sky.horizonAlt * unit.coordSystemRatio) / 2), FAR_CLIP);
				sky.fogStateOn = false;
			}
			else {
				// minimize our back z clip for precision
				double gamma = 0.5235987756 - camera.getPitchRadians();
				if (gamma > Math3D.halfPI) {
					perspective.setPerspectiveProjection(fov, aspectRatio, 1, FOG_RADIUS + 10000);
				}
				else {
					double maxView = Math.tan(gamma) * (camera.getLatAltLon().getAltitudeDD() + cameraController.ground_ht) * 2;
					if (maxView >= FOG_RADIUS + 10000) {
						perspective.setPerspectiveProjection(fov, aspectRatio, 1, FOG_RADIUS + 10000);
					}
					else {
						perspective.setPerspectiveProjection(fov, aspectRatio, 1, maxView);
					}
				}
			}
		}
		else
		{
			if (camera.getVerticalAltitudeMeters() > sky.horizonAlt) {
				//On resize, force update aspectRatio
				perspective.setPerspectiveProjection(fov, aspectRatio, ((sky.horizonAlt * unit.coordSystemRatio) / 2), FAR_CLIP);
			}
			else {
				gl.glEnable(GL.GL_FOG);
				perspective.setPerspectiveProjection(fov, aspectRatio, 1, FOG_RADIUS + 10000);
				sky.fogStateOn = true;
			}
		}
	}
}