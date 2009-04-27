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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Stream {
	/* Read buffer size (download buffer size may be different - internal to java) */
	private final int BUFFER_SIZE = 4 * 1024;
	
	/** Distant stream */
	private final URL distant;
	/** Local stream */
	private URI local;
	
	@Deprecated	//Keeped but will be removed if not needed
	private final String key;

	/** Construct a stream to be downloaded */
	public Stream(URL url) {
		this(url, null);
	}
	/** Construct a stream to be downloaded */
	public Stream(URL url, String key) {
		this.distant = url;
		this.key = key;
	}

	/** Construct a stream already downloaded */
	public Stream(File file) {
		this(null, null);
		this.local = file.toURI();
	}
	
	/** @return true if the file is or has been downloaded */
	public boolean download() throws IOException {
		if (!isDownloaded()) {
			final HttpURLConnection connection = open();
			read(connection);
		}
		return (local != null);
	}
	private HttpURLConnection open() throws IOException {
		IO.printfConnection("Requesting: %s\n", distant);
		final HttpURLConnection connection = (HttpURLConnection)distant.openConnection();
		if (key != null)  {
			connection.addRequestProperty("Authorization", "Basic " + key);
		}
		return connection;
	}
	private void read(HttpURLConnection connection) throws IOException {
		final int length = connection.getContentLength();
		
		final URI cachedURI = Ptolemy3D.getFileSystemCache().getCacheFileFor(distant);
        IO.printfConnection("Local cache: %s\n", cachedURI);

        final InputStream in = connection.getInputStream();
        final FileOutputStream os = new FileOutputStream(new File(cachedURI));
        
        byte[] buffer = new byte[BUFFER_SIZE];
        int remaining = length;
        while (remaining > 0)  {
//        int remaining = 0;
//        while ((remaining = in.available()) > 0)  {
        	int read = in.read(buffer, 0, (remaining > buffer.length) ? buffer.length : remaining);
        	os.write(buffer, 0, read);
        	remaining -= read;
        }

        in.close();
        os.close();
        connection = null;
        
		local = cachedURI;
	}

	/** @return true if the file has been downloaded */
	public boolean isDownloaded() {
		return (local != null);
	}
	
	/** @return the downloaded file (on the local system) */
	public File createFile() {
		if(local == null) {
			throw new IllegalStateException();
		}
		
		final String scheme = local.getScheme();
		if(scheme.equals("file")) {
			final File file = new File(local.getPath());
			if(file.exists()) {
				return file;
			}
		}
		return null;
	}
	
	/** @return the downloaded file has a local file */
	public InputStream createInputStream() {
		if(local == null) {
			throw new IllegalStateException();
		}
		
		String scheme = local.getScheme();
		if(scheme.equals("file")) {
			try {
				return new FileInputStream(new File(local.getPath()));
			}
			catch(FileNotFoundException e) {
				return null;
			}
		}
		else {
			throw new RuntimeException();
		}
	}
}
