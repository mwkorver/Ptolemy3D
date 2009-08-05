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
package fortune;

import java.util.Vector;

/**
 * Code from: http://www.diku.dk/hjemmesider/studerende/duff/Fortune/
 */
class EventQueue {
	Vector<EventPoint> events = new Vector<EventPoint>(100, 50);

	public void insert(EventPoint p) {
		int siz = events.size();

		if (siz == 0) {
			events.add(p);
			this.Events = p;
			return;
		}

		EventPoint ep;
		for (int i = 0; i < siz; i++) {
			ep = events.get(i);
			if (p.x > ep.x || p.x == ep.x && p.y > ep.y) {
				if (ep.Next != null) {
					continue;
				} else {
					// add after this object
					events.add(i + 1, p);
					ep.Next = p;
					p.Prev = ep;
					break;
				}
			}

			if (p.x != ep.x || p.y != ep.y || (p instanceof CirclePoint)) {
				// add element before this one
				events.add(i, p);
				p.Prev = ep.Prev;
				p.Next = ep;
				if (ep.Prev != null) {
					ep.Prev.Next = p;
				}
				ep.Prev = p;
				break;
			} else {
				p.Prev = p;
				System.out.println("Double point ignored: " + p.toString());
				break;
			}
		}

		this.Events = events.elementAt(0);
	}

	public void remove(EventPoint eventpoint) {

		if (eventpoint.Next != null)
			eventpoint.Next.Prev = eventpoint.Prev;

		if (eventpoint.Prev != null)
			eventpoint.Prev.Next = eventpoint.Next;

		events.remove(eventpoint);

		if (events.size() > 0)
			this.Events = events.get(0);
		else
			this.Events = null;
	}

	/**
	 * return the first object in the stack, then remove it from the queue
	 **/
	public EventPoint pop() {
		EventPoint eventpoint = events.get(0);
		if (eventpoint != null) {
			EventPoint Events = eventpoint.Next;
			if (Events != null) {
				Events.Prev = null;
			}
		}
		events.remove(eventpoint);

		if (events.size() > 0)
			this.Events = events.get(0);
		else
			this.Events = null;

		return eventpoint;
	}

	EventPoint Events;
}
