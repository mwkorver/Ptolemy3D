/**
 * Graphic Engine
 * Copyright © 2004-2008 Jerome JOUVIE (Jouvieje)
 * 
 * PROJECT INFORMATIONS
 * ====================
 * Author   Jerome JOUVIE (Jouvieje)
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

import java.util.Vector;

class Profiler {
	class ProfilerEventReport {
		public ProfilerEvent event;
		public boolean active;

		public long startTime;
		public long endTime;

		public long duration;
		public long numPolygons;
		public long numVertices;

		public float durationAverage;
		public long numFrames;

		public ProfilerEventReport() {
			event = null;
			update();
			durationAverage = 0;
			numFrames = 0;
		}

		protected void update() {
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
	
	static interface ProfilerEvent {
	}
	
	public final Timer  counter;
	public Vector<ProfilerEvent> events;

	private ProfilerEventReport[] report;

	public Profiler(Timer counter) {
		this.counter = counter;

		events = new Vector<ProfilerEvent>();
		report = null;
	}

	public void registerEvent(ProfilerEvent event) {
		events.add(event);
	}

	public void unregisterEvent(ProfilerEvent event) {
		events.remove(event);
	}

	public void start() {
		counter.update();

		if(report == null || report.length < events.size()) {
			report = new ProfilerEventReport[events.size()];
			for(int i = 0; i < report.length; i++) {
				ProfilerEventReport eventReport = new ProfilerEventReport();
				eventReport.event = events.get(i);
				report[i] = eventReport;
			}
		}

		for(ProfilerEventReport eventReport : report) {
			eventReport.update();
		}
	}

	public ProfilerEventReport getEventReport(ProfilerEvent event) {
		if(report != null) {
			for(int i = 0; i < report.length; i++) {
				ProfilerEventReport eventReport = report[i];
				if(eventReport.event == event) {
					return eventReport;
				}
			}
		}
		return null;
	}

	public void onEventStart(ProfilerEvent event) {
		ProfilerEventReport eventReport = getEventReport(event);
		if(eventReport != null) {
			eventReport.active = true;
			eventReport.startTime = counter.getTimeNanos();
			eventReport.endTime = 0;
		}
	}

	public void onEventEnd(ProfilerEvent event) {
		ProfilerEventReport eventReport = getEventReport(event);
		if(eventReport != null) {
			eventReport.endTime = counter.getTimeNanos();
			eventReport.duration += (eventReport.endTime - eventReport.startTime) / 1000;

			eventReport.active = false;
		}
	}

	public void profileGeometry(int numPolys, int numVertices) {
		if(report != null) {
			for(ProfilerEventReport eventReport : report) {
				if(eventReport.active) {
					eventReport.numPolygons += numPolys;
					eventReport.numVertices += numVertices;
				}
			}
		}
	}

	public void end() {
		counter.update();
	}
}
