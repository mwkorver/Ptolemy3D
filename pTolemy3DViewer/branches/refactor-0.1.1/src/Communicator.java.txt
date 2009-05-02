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
package org.ptolemy3d.deprecated;

import java.io.IOException;
import java.io.InputStream;

@Deprecated
interface Communicator
{
    /*
     *  this times out and refreshes the socket as well
     */
    public static final int HEADERMAXLEN = 1100;

    public void setServer(String server);

    public void endSocket();

    public void verify();

    public void requestData(String file, int start, int end) throws IOException;

    public void requestData(String file) throws IOException;

    public void requestMapData(String file, int start, int end) throws IOException;

    public void requestMapData(String file) throws IOException;

    public byte[] getData(int len) throws IOException;

    public byte[] getData();

    public int getHeaderReturnCode() throws IOException;

    public void readOutStream(int pos);

    public void flushNotFound() throws IOException;

    public boolean test(String file);

    public void setHeaderKey(String key);

    public void setUrlAppend(String qrystr);

    public double getBytesRead();

    public void clearBytesRead();
    
    public InputStream getInputStream();
}
