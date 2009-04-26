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

import org.ptolemy3d.io.Stream;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Jp2Decoder {
	/** */
	private final Stream stream;
	/** JP2 wavelets */
	private final Jp2Wavelet[] wavelets;
	/** */
	private DecoderContext decoder;

	public Jp2Decoder(Stream stream) {
		this.wavelets = new Jp2Wavelet[4];
		this.stream = stream;
	}
	
	/** @param resolution resolution ID starting from 0 (smallest size) */
	public synchronized Jp2Wavelet parseWavelet(int resolution) {
		if (wavelets[resolution] == null) {
			//Alloc
			if(decoder == null) {
				decoder = new DecoderContext(stream);
			}
			
			//Parse
			try {
				wavelets[resolution] = decoder.parseWavelet(resolution);
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			// Free
			boolean empty = false;
			for(int i = 0; i < wavelets.length; i++) {
				if(wavelets[i] == null) {
					empty = true;
				}
			}
			if(!empty) {
				decoder.delete();
				decoder = null;
			}
		}
		return wavelets[resolution];
	}
	
	public Jp2Wavelet getWavelet(int resolution) {
		return wavelets[resolution];
	}
	
	public boolean hasWavelet(int resolution) {
		return (wavelets[resolution] != null);
	}
}
