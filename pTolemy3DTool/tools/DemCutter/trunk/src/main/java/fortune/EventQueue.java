/**
 * visual-fortune-algorithm
 * http://github.com/sorbits/visual-fortune-algorithm/tree/master
 * 
 * ABOUT
 * This Java applet implements a visualization of Fortune’s plane-sweep algorithm
 * for creating a voronoi diagram.
 * Here is a live demo of the applet: http://www.diku.dk/hjemmesider/studerende/duff/Fortune/
 * The applet was created by Benny Kjær Nielsen and Allan Odgaard in spring of 2000
 * following a course in Computational Geometry taught by Pawel Winter at DIKU.
 * 
 * LICENSE
 * Permission to copy, use, modify, sell and distribute this software is granted.
 * This software is provided “as is” without express or implied warranty,
 * and with no claim as to its suitability for any purpose.
 * 
 * NOTES
 * The purpose of the source is to visualize the algorithm, it is not a good base
 * for an efficient implementation of the algorithm (it does not run in O(n log n) time).
 * The original source was initially lost and recovered using a Java decompiler
 * so most variable names are nonsensical.
 */
package fortune;

import java.util.Vector;

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
