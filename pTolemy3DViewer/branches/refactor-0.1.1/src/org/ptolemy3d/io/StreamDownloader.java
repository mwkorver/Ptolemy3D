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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class StreamDownloader {
	private final int BUFFER_SIZE = 4 * 1024;
	
	private final FileCacheSystem cache;
	private final URL url;
	private final String key;
	
	private HttpURLConnection connection;
	private Stream out;

	public StreamDownloader(FileCacheSystem cache, URL url, String key) {
		this.cache = cache;
		this.url = url;
		this.key = key;
	}

	public Stream download() throws IOException {
		if (!isReady()) {
			final int length = open();
			read(length);
		}
		return getStream();
	}
	private int open() throws IOException {
		connection = (HttpURLConnection) url.openConnection();
		if (key != null)  {
			connection.addRequestProperty("Authorization", "Basic " + key);
		}
		final int length = connection.getContentLength();
		return length;
	}
	private void read(int length) throws IOException {
		final URI cachedURI = cache.getCacheFileFor(url);
		
        final InputStream in = connection.getInputStream();
        final FileOutputStream os = new FileOutputStream(new File(cachedURI));
        
        byte[] buffer = new byte[BUFFER_SIZE];
        int remaining = length;
        while (remaining > 0)  {
        	int read = in.read(buffer, 0, (remaining > buffer.length) ? buffer.length : remaining);
        	os.write(buffer, 0, read);
        	remaining -= read;
        }

        in.close();
        os.close();
        
        out = new Stream(cachedURI);
	}

	public boolean isReady() {
		return (out != null);
	}

	public Stream getStream() {
		if (!isReady()) {
			throw new IllegalStateException();
		}
		return out;
	}
}
