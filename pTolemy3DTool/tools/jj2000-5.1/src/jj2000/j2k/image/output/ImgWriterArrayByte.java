/** START OF UPGRADE */

package jj2000.j2k.image.output;

import jj2000.j2k.image.*;

import java.awt.image.BufferedImage;
import java.io.*;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class ImgWriterArrayByte extends ImgWriter
{
	/** */
	protected final int width, height, numChannels;
	/** */
	protected final byte[] pixels;

	/** Value used to inverse level shift. One for each component */
	private final int[] levShift;

	/** The array of indexes of the components from where to get the data */
	private final int[] cps;

	/** The array of the number of fractional bits in the components of the
	    source data */
	private final int[] fb;

	/** A DataBlk, just used to avoid allocating a new one each time
	    it is needed */
	private DataBlkInt db = new DataBlkInt();

	/**
	 * Creates a new writer to the specified file, to write data from the
	 * specified component.
	 *
	 * <p>The three components that will be written as R, G and B must be
	 * specified through the b1, b2 and b3 arguments.</p>
	 *
	 * @param fname The name of the file where to write the data
	 *
	 * @param imgSrc The source from where to get the image data to write.
	 *
	 * @param n1 The index of the first component from where to get the data,
	 * that will be written as the red channel.
	 *
	 * @param n2 The index of the second component from where to get the data,
	 * that will be written as the green channel.
	 *
	 * @param n3 The index of the third component from where to get the data,
	 * that will be written as the green channel.
	 *
	 * @see DataBlk
	 * */
	public ImgWriterArrayByte(BlkImgDataSrc imgSrc, int n1, int n2, int n3) {
		// Check that imgSrc is of the correct type
		// Check that the component index is valid
		if((n1 < 0) || (n1 >= imgSrc.getNumComps()) || (n2 < 0) || (n2 >= imgSrc.getNumComps()) || (n3 < 0)
				|| (n3 >= imgSrc.getNumComps()) || (imgSrc.getNomRangeBits(n1) > 8) || (imgSrc.getNomRangeBits(n2) > 8)
				|| (imgSrc.getNomRangeBits(n3) > 8)) {
			throw new IllegalArgumentException("Invalid component indexes");
		}
		// Initialize
		w = imgSrc.getCompImgWidth(n1);
		h = imgSrc.getCompImgHeight(n1);
		// Check that all components have same width and height
		if(w != imgSrc.getCompImgWidth(n2) || w != imgSrc.getCompImgWidth(n3) || h != imgSrc.getCompImgHeight(n2)
				|| h != imgSrc.getCompImgHeight(n3)) {
			throw new IllegalArgumentException("All components must have the" + " same dimensions and no"
					+ " subsampling");
		}
		w = imgSrc.getImgWidth();
		h = imgSrc.getImgHeight();
		
		width = w;
		height = h;
		numChannels = imgSrc.getNumComps();
		pixels = new byte[w*h*numChannels];
		
		levShift = new int[numChannels];
		cps = new int[numChannels];
		fb = new int[numChannels];
		
		src = imgSrc;
		cps[0] = n1;
		cps[1] = n2;
		cps[2] = n3;
		fb[0] = imgSrc.getFixedPoint(n1);
		fb[1] = imgSrc.getFixedPoint(n2);
		fb[2] = imgSrc.getFixedPoint(n3);

		levShift[0] = 1 << (imgSrc.getNomRangeBits(n1) - 1);
		levShift[1] = 1 << (imgSrc.getNomRangeBits(n2) - 1);
		levShift[2] = 1 << (imgSrc.getNomRangeBits(n3) - 1);
	}

	/**
	 * Closes the underlying file or netwrok connection to where the data is
	 * written. Any call to other methods of the class become illegal after a
	 * call to this one.
	 * */
	public void close() throws IOException {
		src = null;
		db = null;
	}

	/**
	 * Writes all buffered data to the file or resource.
	 * */
	public void flush() {
		
	}

	/**
	 * Writes the data of the specified area to the file, coordinates are
	 * relative to the current tile of the source. Before writing, the
	 * coefficients are limited to the nominal range.
	 *
	 * <p>This method may not be called concurrently from different
	 * threads.</p>
	 *
	 * <p>If the data returned from the BlkImgDataSrc source is progressive,
	 * then it is requested over and over until it is not progressive
	 * anymore.</p>
	 *
	 * @param ulx The horizontal coordinate of the upper-left corner of the
	 * area to write, relative to the current tile.
	 *
	 * @param uly The vertical coordinate of the upper-left corner of the area
	 * to write, relative to the current tile.
	 *
	 * @param width The width of the area to write.
	 *
	 * @param height The height of the area to write.
	 * */
	public void write(int ulx, int uly, int w, int h) {
		// Active tiles in all components have same offset since they are at
		// same resolution (PPM does not support anything else)
		int tOffx = src.getCompULX(cps[0]) - (int)Math.ceil(src.getImgULX() / (double)src.getCompSubsX(cps[0]));
		int tOffy = src.getCompULY(cps[0]) - (int)Math.ceil(src.getImgULY() / (double)src.getCompSubsY(cps[0]));

		// Check the array size
		if(db.data != null && db.data.length < width) {
			// A new one will be allocated by getInternCompData()
			db.data = null;
		}

		// Write the data to the file (line by line)
		for(int i = Math.min(uly + h, height) - 1; i >= 0; i--) {
			// Write into buffer first loop over the three components and write for each
			for(int c = 0; c < numChannels; c++) {
				// Request the data and make sure it is not progressive
				db.ulx = ulx;
				db.uly = i;
				db.w = width;
				db.h = 1;
				do {
					db = (DataBlkInt)src.getInternCompData(db, cps[c]);
				}
				while(db.progressive);
				
				// Local variables for faster access
				int maxVal = (1 << src.getNomRangeBits(cps[c])) - 1;
				int fracBits = fb[c];
				int shift = levShift[c];
				
				// Write all bytes in the line
				final int index = numChannels * ((tOffy+i) * width + (ulx+tOffx)) + c;
				for(int j = width - 1; j >= 0; j--) {
					int val = (db.data[db.offset + j] >>> fracBits) + shift;
					byte valInRange = (byte)((val < 0) ? 0 : ((val > maxVal) ? maxVal : val));
					pixels[index + numChannels * j] = valInRange;
				}
			}
		}
	}

	/**
	 * Writes the source's current tile to the output. The requests of data
	 * issued to the source BlkImgDataSrc object are done by strips, in order
	 * to reduce memory usage.
	 *
	 * <P>If the data returned from the BlkImgDataSrc source is progressive,
	 * then it is requested over and over until it is not progressive any
	 * more.
	 * */
	public void write() {
		int i;
		int tIdx = src.getTileIdx();
		int tw = src.getTileCompWidth(tIdx, 0); // Tile width 
		int th = src.getTileCompHeight(tIdx, 0); // Tile height
		// Write in strips
		for(i = 0; i < th; i += DEF_STRIP_HEIGHT) {
			write(0, i, tw, ((th - i) < DEF_STRIP_HEIGHT) ? th - i : DEF_STRIP_HEIGHT);
		}
	}

	/**
	 * Returns a string of information about the object, more than 1 line
	 * long. The information string includes information from the underlying
	 * RandomAccessFile (its toString() method is called in turn).
	 *
	 * @return A string of information about the object.
	 * */
	public String toString() {
		return "ImgWriterArrayByte: WxH = " + w + "x" + h + ", Components = " + cps[0] + "," + cps[1] + "," + cps[2];
	}

	/** @return width of the image */
	public int getWidth() { return width; }
	/** @return height of the image */
	public int getHeight() { return height; }
	/** @return number of channels of the image */
	public int getNumChannels() { return numChannels; }
	/** @return pixels array */
	public byte[] getPixels() { return pixels; }
	
	public static BufferedImage createBufferedImage(int width, int height, byte[] pixels) {
		BufferedImage bi;
		int[] shifts;
		int numChannels = pixels.length / (width * height);
		switch (numChannels) {
			case 1: bi = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY); shifts = new int[]{0}; break;
			case 3: bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB); shifts = new int[]{16, 8, 0, 24}; break;
			case 4: bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); shifts = new int[]{16, 8, 0, 24}; break;
			default: throw new UnsupportedOperationException();
		}
		
		for(int j = 0, index = 0; j < height; j++) {
			for(int i = 0; i < width; i++) {
				int color = 0;
				for(int c = 0; c < numChannels; c++, index++) {
					color |= (pixels[index]&0xFF) << shifts[c];
				}
				bi.setRGB(i, j, color);
			}
		}
		return bi;
	}
}
/** END OF UPGRADE */
