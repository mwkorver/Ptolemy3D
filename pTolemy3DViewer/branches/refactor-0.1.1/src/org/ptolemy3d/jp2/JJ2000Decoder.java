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
package org.ptolemy3d.jp2;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.ptolemy3d.data.Texture;
import org.ptolemy3d.io.Stream;


import jj2000.j2k.codestream.HeaderInfo;
import jj2000.j2k.codestream.reader.BitstreamReaderAgent;
import jj2000.j2k.codestream.reader.HeaderDecoder;
import jj2000.j2k.decoder.Decoder;
import jj2000.j2k.decoder.DecoderSpecs;
import jj2000.j2k.entropy.decoder.EntropyDecoder;
import jj2000.j2k.fileformat.reader.FileFormatReader;
import jj2000.j2k.image.ImgDataConverter;
import jj2000.j2k.image.invcomptransf.InvCompTransf;
import jj2000.j2k.image.output.ImgWriterArrayByte;
import jj2000.j2k.io.BEBufferedRandomAccessFile;
import jj2000.j2k.io.RandomAccessIO;
import jj2000.j2k.quantization.dequantizer.Dequantizer;
import jj2000.j2k.roi.ROIDeScaler;
import jj2000.j2k.util.ISRandomAccessIO;
import jj2000.j2k.util.ParameterList;
import jj2000.j2k.wavelet.synthesis.InverseWT;

/**
 * A clean use of JJ200 decoder.<BR>
 * Advantage: all JP2 file are supported. Disadvantage: slow decoder.<BR>
 * <BR>
 * Based on JJ2000 decoder implementation
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class JJ2000Decoder implements org.ptolemy3d.jp2.Decoder {
	// ========================== TEST ================================
	public static void main(String argv[]) throws Throwable {
		final JJ2000Decoder decoder = new JJ2000Decoder(new Stream(new File(argv[0] + ".jp2")));
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
	public ParameterList pl;
	public DecoderSpecs decSpec;
	public InverseWT invWT;
	public int[] depth;
	
	public JJ2000Decoder(Stream stream) {
		this.stream = stream;
	}
	
	/**
	 * Runs the decoder. After completion the exit code is set, a non-zero
	 * value indicates that an error ocurred.
	 */
	public void parseHeader() throws IOException {
		// Default decoder parameters
		final ParameterList defaultParams = new ParameterList();
		for(String[] param : Decoder.getAllParameters()) {
			final String key = param[0];
			final String value = param[param.length - 1];
			if(value != null) {
				defaultParams.put(key, value);
			}
		}
		// Paramter List
		pl = new ParameterList(defaultParams);
		pl.put("nocolorspace", "on");

		// InputStream
		RandomAccessIO in;
		File file = stream.createFile();
		if(file != null) {
			in = new BEBufferedRandomAccessFile(file, "r");
		}
		else {
			in = new ISRandomAccessIO(stream.createInputStream());
		}

		// File Format
		FileFormatReader ff = new FileFormatReader(in);
		ff.readFileFormat();
		if(ff.JP2FFUsed) {
			in.seek(ff.getFirstCodeStreamPos());
		}
		
		// **** Header decoder ****
		HeaderInfo hi = new HeaderInfo();
		HeaderDecoder hd = new HeaderDecoder(in, pl, hi);

		int nCompCod = hd.getNumComps();
		decSpec = hd.getDecoderSpecs();

		// Get demixed bitdepths
		depth = new int[nCompCod];
		for(int i = 0; i < nCompCod; i++) {
			depth[i] = hd.getOriginalBitDepth(i);
		}

		// **** Bit stream reader ****
		BitstreamReaderAgent breader = BitstreamReaderAgent.createInstance(in, hd, pl, decSpec, pl.getBooleanParameter("cdstr_info"), hi);

		// **** Entropy decoder ****
		EntropyDecoder entdec = hd.createEntropyDecoder(breader, pl);

		// **** ROI de-scaler ****
		ROIDeScaler roids = hd.createROIDeScaler(entdec, pl, decSpec);

		// **** Dequantizer ****
		Dequantizer deq = hd.createDequantizer(roids, depth, decSpec);

		// **** Inverse wavelet transform ***
		invWT = InverseWT.createInstance(deq, decSpec);
	}
	
	public synchronized int getNumWavelets() {
		try {
			if(invWT == null) {
				parseHeader();
			}
			final int numWavelets = decSpec.dls.getMin() + 1;
			return numWavelets;
		} catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public Texture parseWavelet(int res) {
		try {
			if(invWT == null) {
				parseHeader();
			}
			
			invWT.setImgResLevel(res);

			// **** Data converter **** (after inverse transform module)
			ImgDataConverter converter = new ImgDataConverter(invWT, 0);

			// **** Inverse component transformation **** 
			InvCompTransf decodedImage = new InvCompTransf(converter, decSpec, depth, pl);
			
			// **** Create image writers/image display ****
			ImgWriterArrayByte img = new ImgWriterArrayByte(decodedImage, 0, 1, 2);
			img.writeAll();
			img.close();
			
			return new Texture(img.getPixels(), img.getWidth(), img.getHeight());
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void delete() {
		pl = null;
		decSpec = null;
		invWT = null;
		depth = null;
	}
}
