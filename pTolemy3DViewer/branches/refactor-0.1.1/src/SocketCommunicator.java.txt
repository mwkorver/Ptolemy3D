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
import java.io.PrintWriter;
import java.net.Socket;

import org.ptolemy3d.debug.IO;

@Deprecated
class SocketCommunicator implements Communicator
{
	private static final int[] ContentLength = {67, 111, 110, 116, 101, 110, 116, 45, 76, 101, 110, 103, 116, 104, 58, 32};

	private boolean isValid;
	private String hkey = null;
	private long mark_start;
	private long max_life = 1000 * 60 * 2;
	private double tot_br = 0;
	private byte[] clnum = new byte[10];
	private Socket socket;
	private PrintWriter writer;
	private InputStream in;
	private String Server = "";
	private String qrystr = "";

	// if tile header is bigger than this ByteArrayReader will grab more data, but this seems to hold
	// most headers.
	public SocketCommunicator(String server)
	{
		Server = server;
		isValid = true;
		qrystr = "";
	}

	public void setServer(String server)
	{
		this.Server = server;
	}

	public void setUrlAppend(String qstr)
	{
		boolean set = false;
		if (qstr != null)
		{
			if (qstr.length() > 0)
			{
				if (qstr.charAt(0) != '?')
				{
					qstr = "?" + qstr;
				}
				this.qrystr = qstr;
				set = true;
			}
		}
		if (!set)
		{
			this.qrystr = "";
		}
	}

	public void setHeaderKey(String key)
	{
		this.hkey = key;
		if (this.hkey != null)
		{
			if (this.hkey.length() == 0)
			{
				this.hkey = null;
			}
		}
	}

	private void openSocket()
	{
		final int port = 80;

		try {
			if (socket != null) {
				socket.close();
				IO.printlnConnection("Opening socket to " + Server + ":" + port);
			}

			socket = new Socket(Server, port);
			writer = new PrintWriter(socket.getOutputStream());
			in = /*new BufferedInputStream*/(socket.getInputStream());

			mark_start = System.currentTimeMillis();
			tot_br = 0;
		}
		catch (Exception e) {
			isValid = false;
		}
	}

	/*
	 *  this times out and refreshes the socket as well
	 */
	public synchronized void endSocket()
	{
		try
		{
			if (in != null) {
				in.close();
			}
			if (writer != null) {
				writer.close();
			}
			if (socket != null) {
				socket.close();
			}

			in = null;
			writer = null;
			socket = null;
		} catch (IOException e) {}
	}

	private void timeout()
	{
		endSocket();
		openSocket();
	}

	public void verify()
	{
		if ((socket == null) || !socket.isConnected() || socket.isInputShutdown() || socket.isOutputShutdown())
		{
			openSocket();
		}
	}

	public void requestData(String file, int start, int end) throws IOException
	{
		// time out if this socket has been alive too long
		if ((System.currentTimeMillis() - mark_start) > max_life) {
			timeout();
		}
		else {
			verify();
		}

		String requestUrl = "GET " + file + " HTTP/1.1 \r\nHost: " + Server + " \r\nConnection: keep-alive \r\nRange: bytes=" + start + "-" + end;
		IO.printlnConnection("SocketCommunicator.requestData1: " + requestUrl);

		writer.print(requestUrl);
		if (this.hkey != null) {
			writer.print(" \r\nAuthorization: Basic " + this.hkey);
		}
		writer.print("\r\n\r\n");
		writer.flush();
	}

	public void requestMapData(String file, int start, int end) throws IOException
	{
		// time out if this socket has been alive too long
		if ((System.currentTimeMillis() - mark_start) > max_life) {
			timeout();
		}
		else {
			verify();
		}

		String requestUrl = "GET " + file + qrystr + " HTTP/1.1 \r\nHost: " + Server + " \r\nConnection: keep-alive \r\nRange: bytes=" + start + "-" + end;
		IO.printlnConnection("SocketCommunicator.requestMapData1: " + requestUrl);

		writer.print(requestUrl);
		if (this.hkey != null) {
			writer.print(" \r\nAuthorization: Basic " + this.hkey);
		}
		writer.print("\r\n\r\n");
		writer.flush();
	}

	public void requestData(String file) throws IOException
	{
		// time out if this socket has been alive too long
		if ((System.currentTimeMillis() - mark_start) > max_life) {
			timeout();
		}

		String requestUrl = "GET " + file + " HTTP/1.1 \r\nHost: " + Server + " \r\nConnection: keep-alive \r\nCache-Control: max-age=7200";
		IO.printlnConnection("SocketCommunicator.requestData2: " + requestUrl);

		writer.print(requestUrl);
		if (this.hkey != null) {
			writer.print(" \r\nAuthorization: " + this.hkey);
		}
		writer.print("\r\n\r\n");
		writer.flush();
	}

	public void requestMapData(String file) throws IOException
	{
		// time out if this socket has been alive too long
		if ((System.currentTimeMillis() - mark_start) > max_life) {
			timeout();
		}
		String requestUrl = "GET " + file + qrystr + " HTTP/1.1 \r\nHost: " + Server + " \r\nConnection: keep-alive \r\nCache-Control: max-age=7200";
		IO.printlnConnection("SocketCommunicator.requestMapData: " + requestUrl);

		writer.print(requestUrl);
		if (this.hkey != null) {
			writer.print(" \r\nAuthorization: " + this.hkey);
		}
		writer.print("\r\n\r\n");
		writer.flush();
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
		byte[] resp = null;
		try {
			int bytesread = 0, offset = 0;
			int rec_len = passHeaderGetContentLength();
			if (rec_len < len) {
				len = rec_len;
			}
			resp = new byte[len];

			while (len > 0) {
				bytesread = in.read(resp, offset, len);
				offset += bytesread;
				len -= bytesread;
			}

			tot_br += offset;

		}
		catch (IOException e) {
			IO.printlnConnection(e.getMessage());
			IO.printlnConnection("Reconnecting.");
			timeout();
		}
		return resp;
	}

	public byte[] getData()
	{
		byte[] resp = null;
		try
		{
			int len = passHeaderGetContentLength();
			int bytesread, offset = 0;
			resp = new byte[len];
			while (len > 0) {
				bytesread = in.read(resp, offset, len);
				offset += bytesread;
				len -= bytesread;
			}
		}
		catch (IOException e)
		{
			IO.printlnConnection(e.getMessage());
			IO.printlnConnection("Reconnecting.");
			timeout();
		}
		return resp;
	}

	private void passHeader() throws IOException
	{
		int b;
		// read past header (2 crlf's)
		while ((b = (byte) in.read()) != -1) {
			if (b == 13) {
				if (in.read() == 10) {
					if (in.read() == 13) {
						if (in.read() == 10) {
							break;
						}
					}
				}
			}
		}
	}

	private int passHeaderGetContentLength() throws IOException
	{
		int b, i;
		int val = 0;
		while ((b = (byte) in.read()) != -1) {
			if (b == 13) {
				if ((b = (byte) in.read()) == 10) {
					if ((b = (byte) in.read()) == 13) {
						if ((b = (byte) in.read()) == 10) {
							break;
						}
					}
				}
			}
			if (b == ContentLength[0]) {
				boolean isCL = true;
				for (i = 1; i < ContentLength.length; i++) {
					if (in.read() != ContentLength[i]) {
						isCL = false;
						break;
					}
				}
				if (isCL) {
					int k = 0;
					while ((clnum[k++] = (byte) in.read()) != 10);
					// add to val
					//int kplace = k;
					k--;
					int place = 0;
					while (k >= 0) {
						if ((clnum[k] >= 48) && (clnum[k] <= 57)) {
							val += (clnum[k] - 48) * Math.pow(10, place++);
						}
						k--;
					}
					//val = Integer.parseInt(new String(num,0,kplace-2));
				}
			}

		}
		return val;
	}

	public int getHeaderReturnCode() throws IOException
	{
		int n = -1;
		boolean retry = false;
		int c = 0;
		byte b;
		try
		{
			// space is 32.
			// read to first space
			while (in.read() != 32);
			// read to next space
			// up to 8 digit number

			while ((b = (byte) in.read()) != 32) {
				clnum[c++] = b;

				if (c == 3) {
					break;
				}

				if (b == 72) {
					while (in.read() != 32);
					c = 0;
				}
			}
			//n = Integer.parseInt(new String(num,0,c));
			// add to val
			//int kplace = c;
			n = 0;
			c--;
			int place = 0;

			while (c >= 0) {
				if ((clnum[c] >= 48) && (clnum[c] <= 57)) {
					n += (clnum[c] - 48) * Math.pow(10, place++);
				}
				c--;
			}

			// the 3 codees we are expecting are 200,206, and 404(not found)
			if ((n != 200) && (n != 206) && (n != 404)) {
				retry = true;
			}
		}
		catch (NumberFormatException e) {
			retry = true;
			IO.printlnConnection("Ptolemy3DSocketCommunicator.getHeaderReturnCode():"+e.getMessage());
		}

		if (retry) {
			// read out a few more bytes
			byte[] msg = new byte[100];
			c = 0;

			for (int y = 0; y < 100; y++) {
				msg[c++] = (byte) in.read();
			}

			IO.printlnConnection(new String(msg));
			IO.printlnConnection("Data is corrupt, reconnecting.");

			timeout();
		}

		return n;
	}

	public void readOutStream(int pos)
	{
		try
		{
			int len = HEADERMAXLEN - pos;
//			int bytesread;
			while (len > 0)
			{
//				bytesread = (int) in.skip(len);
//				len -= bytesread;

	        	in.read();
	        	len--;
			}
		}
		catch (IOException e)
		{
		}
	}

	public void flushNotFound() throws IOException
	{
		int len = passHeaderGetContentLength();
		if (len == 0) {
			passHeader();
		}

		else {
			for (int g = 0; g < len; g++) {
				in.read();
			}
		}
	}
	// check to see whether multiple request socket communication is ok.
	public boolean test(String file)
	{
		try {
			verify();

			if (isValid) {
				byte[] b1, b2;

				requestMapData(file, 0, 1);
				b1 = getData(1);
				requestMapData(file, 0, 1);
				b2 = getData(1);

				if (b1[0] != b2[0]) {
					return false;
				}
			}
			else {
				return false;
			}
		}
		catch (Exception e) {
			IO.printlnConnection(e.getMessage());
			IO.printlnConnection("Unable to use socket, switching to http connection.");
			return false;
		}
		return true;
	}
	
	public InputStream getInputStream(){
		return null;
	}
}
