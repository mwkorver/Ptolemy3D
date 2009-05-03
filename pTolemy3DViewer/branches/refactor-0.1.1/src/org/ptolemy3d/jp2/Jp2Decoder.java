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
import org.ptolemy3d.manager.Texture;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class Jp2Decoder implements Decoder {
	/** */
	private final Stream stream;
	/** */
	private DecoderContext decoder;

	public Jp2Decoder(Stream stream) {
		this.stream = stream;
	}
	
	/** @return the number of wavelets in the jp2 */
	public synchronized int getNumWavelets() {
		try {
			return getDecoder().getNumWavelets();
		} catch(Exception e) {
			e.printStackTrace();
			return 0;
		}
	
	}
	
	/** @param resolution resolution ID starting from 0 (smallest size) */
	public synchronized Texture parseWavelet(int resolution) {
		try {
			return getDecoder().parseWavelet(resolution);
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private final DecoderContext getDecoder() {
		if(decoder == null) {
			decoder = new DecoderContext(stream);
		}
		return decoder;
	}
}
