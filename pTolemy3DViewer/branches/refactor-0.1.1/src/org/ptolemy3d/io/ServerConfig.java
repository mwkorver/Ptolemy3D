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
package org.ptolemy3d.io;


/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class ServerConfig {
	/** */
	public final String headerKeys;
	/** */
	public final String dataServers;
	/** */
	public final String[] jp2Locations;
	/** */
	public final String[] DEMLocation;
	/** */
	public final boolean keepAlives;
	/** */
	public final String urlAppends;
	
	public ServerConfig(String headerKeys, String dataServers,
			String[] locations, String[] DEMLocation,
			boolean keepAlives, String urlAppends) {
		this.headerKeys = headerKeys;
		this.dataServers = dataServers;
		this.jp2Locations = locations;
		this.DEMLocation = DEMLocation;
		this.keepAlives = keepAlives;
		this.urlAppends = urlAppends;
	}
	
    public String getUrlAppender() {
        if (urlAppends != null && urlAppends.length() > 0) {
            if (urlAppends.charAt(0) != '?') {
                return "?" + urlAppends;
            }
            return urlAppends;
        }
        return "";
    }
}
