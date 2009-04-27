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
import java.net.HttpURLConnection;
import java.net.URL;

import org.ptolemy3d.debug.IO;

@Deprecated
class BasicCommunicator implements Communicator
{
	private InputStream in;
    private URL url;
    private HttpURLConnection conn;
    private double tot_br = 0;
    private String hkey = null;
    private int StartPos = 0, EndPos = 0, clen = 0;
    private String Server = "";
    private String qrystr = "";

    // if tile header is bigger than this ByteArrayReader will grab more data, but this seems to hold
    // most headers.
    public BasicCommunicator(String server)
    {
        this.Server = server;
        tot_br = 0;
        qrystr = "";
    }

    public void setServer(String server)
    {
        this.Server = server;
    }

    public void setHeaderKey(String key)
    {
        this.hkey = key;
        if (this.hkey != null) {
            if (this.hkey.length() == 0) {
                this.hkey = null;
            }
        }
    }

    public void setUrlAppend(String qstr)
    {
        boolean set = false;
        if (qstr != null) {
            if (qstr.length() > 0) {
                if (qstr.charAt(0) != '?') {
                    qstr = "?" + qstr;
                }
                this.qrystr = qstr;
                set = true;
            }
        }
        if (!set) {
            this.qrystr = "";
        }
    }
    /*
     *  this times out and refreshes the socket as well
     */

    public synchronized void endSocket()
    {
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {}
        }
    }

    public void verify()
    {
    }

    public void requestData(String file, int start, int end) throws IOException
    {
    	if(Server.equals("WFS_SERVER")){
			url = new URL(file);
		}
		else{
			url = new URL("http://"+Server + file);
		}
        IO.printlnConnection("BasicCommunicator.requestData: " + url);
        conn  = (HttpURLConnection) url.openConnection();

        if (this.hkey != null)  {
            conn.addRequestProperty("Authorization", "Basic " + this.hkey);
        }
        StartPos = start;
        clen = conn.getContentLength();
        if (end == -1) {
            EndPos = clen;
        }
        else {
            EndPos = end;
        }
    }

    public void requestMapData(String file, int start, int end) throws IOException
    {
        url = new URL("http://" + Server + file + qrystr);

        IO.printlnConnection("BasicCommunicator.requestMapData: " + url);

        conn = (HttpURLConnection) url.openConnection();

        if (this.hkey != null)  {
            conn.addRequestProperty("Authorization", "Basic " + this.hkey);
        }
        StartPos = start;
        clen = conn.getContentLength();
        if (end == -1)  {
            EndPos = clen;
        }
        else  {
            EndPos = end;
        }
    }

    public void requestData(String file) throws IOException
    {
        requestData(file, 0, -1);
    }

    public void requestMapData(String file) throws IOException
    {
        requestMapData(file, 0, -1);
    }

    public double getBytesRead()
    {
        return tot_br;
    }

    public void clearBytesRead()
    {
        tot_br = 0;
    }

    public byte[] getData(int len) throws IOException
    {
        in = conn.getInputStream();

        if (clen < len) {
            len = clen;
        }
        int offset = 0, bytesread = 0;
        byte[] data = new byte[len];

        int lenToStart = StartPos;
        while (lenToStart > 0)  {
        	//1nd way
//        	in.skip(lenToStart);
//        	lenToStart = 0;
        	//2nd way
        	bytesread = in.read(data, 0, lenToStart > len ? len : lenToStart);
        	lenToStart -= bytesread;
        	//3nd way
//        	in.read();
//        	lenToStart--;
        }

        while (len > 0) {
            bytesread = in.read(data, offset, len);
            offset += bytesread;
            len -= bytesread;
        }

        tot_br += offset;
        in.close();
        return data;
    }

    public byte[] getData()
    {
        try {
            return getData(EndPos);
        }
        catch (IOException e) {
            return null;
        }
    }

    public int getHeaderReturnCode() throws IOException
    {
        return conn.getResponseCode();
    }

    public void readOutStream(int pos)
    {
    }

    public void flushNotFound() throws IOException
    {
    }

    // no need to test, if they got the applet this class will work.
    public boolean test(String file)
    {
        return true;
    }
    
	public InputStream getInputStream(){
		try{return conn.getInputStream();}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
}
