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
package org.ptolemy3d.scene;

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.Vector;

class SceneProducer implements ImageProducer
{
	/** The list of image consumers for this image producer */
	private volatile Vector<ImageConsumer> consumers;
	private int height;
	private int weight;
	private byte[] data;
	private boolean isBugged;

	public SceneProducer(byte[] d, int w, int h)
	{
		this.consumers = new Vector<ImageConsumer>();
		this.height = h;
		this.weight = w;
		this.data = d;
		this.isBugged = false;

		// scan data to see if last line is black
		for (int i = 0; i < weight * 3; i++) {
			if (data[i] != 0) {
				isBugged = true;
				break;
			}
		}
	}

	public final synchronized void addConsumer(ImageConsumer ic) {
		if(ic != null && !consumers.contains(ic)) {
			consumers.addElement(ic);
		}
	}

	public boolean isConsumer(ImageConsumer ic) {
		return consumers.contains(ic);
	}

	public synchronized void removeConsumer(ImageConsumer ic) {
		consumers.removeElement(ic);
	}

	public void startProduction(ImageConsumer ic) {
		//The default color model (0xAARRGGBB) used in Java
		final ColorModel cm = ColorModel.getRGBdefault();

		// Register ic
		if (ic != null) {
			addConsumer(ic);
		}

		final ImageConsumer[] cons;
		synchronized (this) {
			cons = new ImageConsumer[consumers.size()];
			consumers.copyInto(cons);
		}

		int hints;
		hints = ImageConsumer.SINGLEFRAME | ImageConsumer.SINGLEPASS;
		hints |= ImageConsumer.COMPLETESCANLINES | ImageConsumer.TOPDOWNLEFTRIGHT;

		for (int i = cons.length - 1; i >= 0; i--) {
			cons[i].setColorModel(cm);
			cons[i].setDimensions(weight, height);
			cons[i].setHints(hints);
		}

		int[] pixbuf = new int[weight];
		int offset;
		int dWt = (isBugged) ? (weight + 1) : weight;

		for (int i = 0; i < height; i++) {
			offset = (height - 1 - i) * dWt * 3;
			for (int j = 0; j < weight; j++) {
				pixbuf[j] = (0xFF << 24) | ((data[offset++] & 0xFF) << 16) | ((data[offset++] & 0xFF) << 8) | (data[offset++] & 0xFF);
			}
			for (int h = cons.length - 1; h >= 0; h--) {
				cons[h].setPixels(0, i, weight, 1, cm, pixbuf, 0, weight);
			}
		}

		for (int i = cons.length - 1; i >= 0; i--) {
			cons[i].imageComplete(ImageConsumer.SINGLEFRAMEDONE);
		}

		// Signal that image is complete
		for (int i = cons.length - 1; i >= 0; i--) {
			cons[i].imageComplete(ImageConsumer.STATICIMAGEDONE);
		}

		// Remove the consumers since all the data has been sent
		synchronized (this) {
			for (int i = cons.length - 1; i >= 0; i--) {
				consumers.removeElement(cons[i]);
			}
		}
	}

	public void requestTopDownLeftRightResend(ImageConsumer ic) {}
}
