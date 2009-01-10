/**
 * Graphic Engine
 * Copyright © 2004-2008 Jérôme JOUVIE (Jouvieje)
 *
 * PROJECT INFORMATIONS
 * ====================
 * Author   Jérôme JOUVIE (Jouvieje)
 * Email    jerome.jouvie@gmail.com
 * Site     http://jerome.jouvie.free.fr/
 * Homepage http://jerome.jouvie.free.fr/OpenGl/Projects/GraphicEngineCore.php
 * Version  GraphicEngineCore v0.1.5 Build 16-11-2008
 *
 * LICENSE
 * =======
 *
 * GNU GENERAL PUBLIC LICENSE (GPL)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.ptolemy3d.debug;

import org.ptolemy3d.debug.Profiler.ProfilerEvent;

class ProfilerEventReport
{
	public ProfilerEvent event;
	public boolean active;

	public long startTime;
	public long endTime;

	public long duration;
	public long numPolygons;
	public long numVertices;

	public float durationAverage;
	public long numFrames;

	public ProfilerEventReport()
	{
		event = null;
		update();
		durationAverage = 0;
		numFrames = 0;
	}

	protected void update()
	{
		//Calculate average
		if(numFrames > 0) {
			durationAverage = (durationAverage+duration) / (numFrames+1);
		}

		active      = false;
		startTime   = 0;
		endTime     = 0;
		duration    = 0;
		numPolygons = 0;
		numVertices = 0;
		numFrames++;
	}
}
