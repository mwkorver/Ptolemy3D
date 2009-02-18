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

import javax.media.opengl.GLJPanel;

import org.ptolemy3d.Ptolemy3DEvents;

/**
 *
 */
public class Ptolemy3DJPanel extends GLJPanel
{
	private static final long serialVersionUID = 1L;

	private final Ptolemy3DEvents events;

	public Ptolemy3DJPanel(Ptolemy3DEvents events)
	{
		this.events = events;
		this.addGLEventListener(this.events);
	}
}