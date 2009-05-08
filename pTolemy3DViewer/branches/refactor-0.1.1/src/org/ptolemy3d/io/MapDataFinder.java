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

import java.net.URI;
import java.net.URL;
import java.util.Vector;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.Globe;
import org.ptolemy3d.globe.Layer;
import org.ptolemy3d.globe.MapData;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class MapDataFinder {
	/** List of servers */
	private final String host;
	/** List of servers */
	private final int port;
	/** List of servers */
	private final Vector<ServerConfig> servers;
	
	public MapDataFinder(String server, ServerConfig[] initialServerConfig) {
		this.host = parseHost(server);
		this.port = parsePort(server);
		if(initialServerConfig != null) {
			servers = new Vector<ServerConfig>(initialServerConfig.length);
			for(ServerConfig serverConfig : initialServerConfig) {
				servers.add(serverConfig);
			}
		}
		else {
			servers = new Vector<ServerConfig>(1);
		}
	}
	
	public static String parseHost(String server) {
		int index = server.lastIndexOf(":");
		if(index >= 0) {
			try {
				return server.substring(0, index);
			} catch (NumberFormatException t) { }
		}
		return server;
	}
	public static int parsePort(String server) {
		int index = server.lastIndexOf(":");
		if(index >= 0) {
			try {
				return Integer.parseInt(server.substring(index+1));
			} catch (NumberFormatException t) { }
		}
		return 80;
	}
	
	public void addServer(ServerConfig server) {
		servers.add(server);
	}
	
	/** Search map data texture on the available servers.
	 *  @return the map data texture url */
	public URL findMapDataTexture(MapData mapData) {
		final String fileBase = getTextureFileBase(mapData) + ".jp2";
		
		for(ServerConfig serverConfig : servers) {
			if(serverConfig.getJp2Locations() == null) {
				continue;
			}
			
			for(String location : serverConfig.getJp2Locations()) {
				final String s = location + fileBase + serverConfig.getUrlAppender();
				try {
					final URL url = new URL("http", serverConfig.getHost(), serverConfig.getPort(), s);
					url.openStream();
					IO.printfConnection("Map found: %s [http://%s:%d%s]\n", mapData, host, port, s);
					return url;
				}
				catch(Exception e) {
					IO.printfConnection("Map not found: %s [http://%s:%d%s]\n", mapData, host, port, s);
					continue;
				}
			}
		}
		IO.printfConnection("Map not found: %s\n", mapData);
		return null;
	}
	private final String getTextureFileBase(MapData mapData) {
		final Globe globe = Ptolemy3D.getScene().getLandscape().globe;
		final Layer layer = globe.getLayer(mapData.key.layer);
		
		String fileBase = layer.getTileSize() + "/";
		final int divider = layer.getDivider();
		if (divider > 0) {	//as usual
			fileBase += "D" + divider + "/x";
			int div_ = (mapData.key.lon / divider);
			fileBase += String.valueOf((div_ > 0) ? String.valueOf(div_) : "n" + (-div_));
			div_ = (mapData.key.lat / divider);
			fileBase += "y" + ((div_ > 0) ? String.valueOf(div_) : ("n" + (-div_))) + "/";
		}
		else if (divider < 0) {	//super funky style
			fileBase += mapData.key.lon < 0 ? "n" : "p";
			fileBase += mapData.key.lat < 0 ? "n" : "p";
			int absx = Math.abs(mapData.key.lon);
			int absz = Math.abs(mapData.key.lat);
			//pad x and y to 9 digits
			String xStr = padWithZeros(absx, 9);
			String yStr = padWithZeros(absz, 9);
			for (int k = 0; k < 4; k++) {
				fileBase += xStr.charAt(k);
				fileBase += yStr.charAt(k);
				fileBase += "/";
			}
		}
		fileBase += Ptolemy3D.getConfiguration().area + "x" + mapData.key.lon + "y" + mapData.key.lat;

		return fileBase;
	}
	// converts integer to left-zero padded string, len  chars long.
	private static String padWithZeros(int i, int len) {
		String s = Integer.toString(i);
		if (s.length() > len) {
			return s.substring(0, len);
		}
		else if (s.length() < len) {	// pad on left with zeros
			return "000000000000000000000000000".substring(0, len - s.length()) + s;
		}
		else {
			return s;
		}
	}
	
	public Stream findMapDataTextureFromCache(MapData mapData) {
		final String fileBase = getTextureFileBase(mapData) + ".jp2";
		for(ServerConfig serverConfig : servers) {
			if(serverConfig.getJp2Locations() == null) {
				continue;
			}
			
			for(String location : serverConfig.getJp2Locations()) {
				final URI fromCache = Ptolemy3D.getFileSystemCache().getFromCache(location + fileBase);
				if (fromCache != null) {
					IO.printfConnection("Found from cache: %s [%s]\n", mapData.key, fromCache);
					return new Stream(Stream.uriToFile(fromCache));
				}
			}
		}
		return null;
	}
}
