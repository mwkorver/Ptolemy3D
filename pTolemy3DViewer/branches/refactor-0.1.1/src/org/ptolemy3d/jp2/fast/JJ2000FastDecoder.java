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
package org.ptolemy3d.jp2.fast;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import jj2000.j2k.image.output.ImgWriterArrayByte;

import org.ptolemy3d.data.Texture;
import org.ptolemy3d.io.Stream;

/**
 * A clean interface based on a fast but unclean light version of JJ200 decoder.
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class JJ2000FastDecoder implements org.ptolemy3d.jp2.Decoder {
	// ========================== TEST ================================
	public static void main(String argv[]) throws Throwable {
		final JJ2000FastDecoder decoder = new JJ2000FastDecoder(new Stream(new File(argv[0] + ".jp2")));
		for(int res = 0; res < decoder.getNumWavelets(); res++) {
			long start = System.nanoTime();
			Texture wavelet = decoder.parseWavelet(res);
			System.out.println("[Wavelet:"+res+"] Time spent: "+(System.nanoTime() - start)/10e9);
			
			BufferedImage bi = ImgWriterArrayByte.createBufferedImage(wavelet.width, wavelet.height, wavelet.pixels);
			ImageIO.write(bi, "PNG", new File(argv[0] + "-" + res + ".png"));
		}
	}
	// ========================== TEST ================================
	
	public final Stream stream;
	private Jp2Parser jp2Parser;
	private Jp2Decoder jp2Decoder;
	private byte[] pixels;
	private int pixelsRes;
	
	public JJ2000FastDecoder(Stream stream) {
		this.stream = stream;
		delete();
	}
	
	
	public void parseHeader() throws IOException {
		jp2Parser = new Jp2Parser(stream);
	}
	
	public int getNumWavelets() {
		try {
			if(jp2Parser == null) {
				parseHeader();
			}
			return jp2Parser.getNumWavelets();
		} catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public Texture parseWavelet(int res) {
		try {
			//Initialize
			if(jp2Parser == null) {
				parseHeader();
			}
			
			final int realRes;
			if(res > pixelsRes) {
				if(jp2Decoder == null) {
					jp2Decoder = new Jp2Decoder(jp2Parser);
				}
				
				//Decode every wavelet until the resolution asked
				for(int i = pixelsRes + 1; i <= res; i++) {
					pixels = jp2Decoder.getNextResolution(i, pixels);
					pixelsRes = res;
				}
				realRes = res;
			}
			else {
				realRes = pixelsRes;
			}
			
			//Create texture
			final int width = jp2Parser.getWidth(realRes);
			final int height = jp2Parser.getHeight(realRes);
			return new Texture(pixels, width, height);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void delete() {
		jp2Parser = null;
		jp2Decoder = null;
		pixels = null;
		pixelsRes = -1;
	}
}
