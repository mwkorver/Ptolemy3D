/** START OF UPGRADE */

package jj2000.j2k.image.output;

import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import jj2000.j2k.image.BlkImgDataSrc;
import jj2000.j2k.image.DataBlk;

/**
 * @author Jerome JOUVIE
 */
public class ImgWriterImageIO extends ImgWriterArrayByte
{
	/** */
	private final File out;

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
	public ImgWriterImageIO(String fname, BlkImgDataSrc imgSrc, int n1, int n2, int n3) throws IOException {
		super(imgSrc, n1, n2, n3);
		
		final File out = new File(fname);
		if(out.exists() && !out.delete()) {
			throw new IOException("Could not reset file");
		}
		this.out = out;
	}

	/**
	 * Closes the underlying file or netwrok connection to where the data is
	 * written. Any call to other methods of the class become illegal after a
	 * call to this one.
	 *
	 * @exception IOException If an I/O error occurs.
	 * */
	public void close() throws IOException {
		super.close();
		ImageIO.write(createBufferedImage(width, height, pixels), "PNG", out);
	}

	/**
	 * Returns a string of information about the object, more than 1 line
	 * long. The information string includes information from the underlying
	 * RandomAccessFile (its toString() method is called in turn).
	 *
	 * @return A string of information about the object.
	 * */
	public String toString() {
		return "ImgWriterPNG: WxHnC = " + width + "x" + height + "x" + numChannels + "\nUnderlying File:\n" + out.toString();
	}
}
/** END OF UPGRADE */
