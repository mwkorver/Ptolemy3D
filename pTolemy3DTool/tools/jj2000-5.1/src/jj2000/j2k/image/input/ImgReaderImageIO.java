/** START OF UPGRADE */

package jj2000.j2k.image.input;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import jj2000.j2k.JJ2KExceptionHandler;
import jj2000.j2k.image.DataBlk;
import jj2000.j2k.image.DataBlkInt;

/**
 * @author Jerome JOUVIE
 * */
public class ImgReaderImageIO extends ImgReader {
	/** @return true if extension is supported */
	public static boolean isSupported(String extension) {
		return ImageIO.getImageReadersBySuffix(extension).hasNext();
	}
	
	public final BufferedImage image;
	
    /** DC offset value used when reading image */
    public static int DC_OFFSET = 128;
    
    /** Buffer for the 3 components of each pixel(in the current block) */
    private int[][] barr = new int[3][];

    /** Data block used only to store coordinates of the buffered blocks */
    private DataBlkInt dbi = new DataBlkInt();
    
    /** The number of bits that determine the nominal dynamic range */
    private int rb;
    
    /** Temporary DataBlkInt object (needed when encoder uses floating-point
    filters). This avoid allocating new DataBlk at each time */
	private DataBlkInt intBlk;

	/**
	 * Creates a new PPM file reader from the specified file name.
	 *
	 * @param fname The input file name.
	 *
	 * @param IOException If an error occurs while opening the file.
	 * */
	public ImgReaderImageIO(String fname) throws IOException {
		this(new File(fname));
	}

	/**
	 * Creates a new PPM file reader from the specified file.
	 *
	 * @param file The input file.
	 *
	 * @param IOException If an error occurs while opening the file.
	 * */
	public ImgReaderImageIO(File file) throws IOException {
		this(new FileInputStream(file));
	}

	/**
	 * Creates a new PPM file reader from the specified RandomAccessFile
	 * object. The file header is read to acquire the image size.
	 *
	 * @param in From where to read the data
	 *
	 * @exception EOFException if an EOF is read
	 * @exception IOException if an error occurs when opening the file
	 * */
	private ImgReaderImageIO(InputStream in)throws EOFException,IOException {
		this(ImageIO.read(in));
	}
	/**
	 * Creates a new PPM file reader from the specified RandomAccessFile
	 * object. The file header is read to acquire the image size.
	 *
	 * @param in From where to read the data
	 *
	 * @exception EOFException if an EOF is read
	 * @exception IOException if an error occurs when opening the file
	 * */
	public ImgReaderImageIO(BufferedImage bi) {
		image = bi;
		w = image.getWidth();
		h = image.getHeight();
		nc = 3;
		rb = 8;
	}

	/**
	 * Closes the underlying file from where the image data is being read. No
	 * operations are possible after a call to this method.
	 *
	 * @exception IOException If an I/O error occurs.
	 * */
	public void close() throws IOException {
	}


	/**
	 * Returns the number of bits corresponding to the nominal range of the
	 * data in the specified component. This is the value rb (range bits) that
	 * was specified in the constructor, which normally is 8 for non bilevel
	 * data, and 1 for bilevel data.
	 *
	 * <P>If this number is <i>b</b> then the nominal range is between
	 * -2^(b-1) and 2^(b-1)-1, since unsigned data is level shifted to have a
	 * nominal avergae of 0.
	 *
	 * @param c The index of the component.
	 *
	 * @return The number of bits corresponding to the nominal range of the
	 * data. For floating-point data this value is not applicable and the
	 * return value is undefined.
	 * */
	public int getNomRangeBits(int c) {
		// Check component index
		if (c<0 || c>2)
			throw new IllegalArgumentException();

		return rb;
	}

	/**
	 * Returns the position of the fixed point in the specified component
	 * (i.e. the number of fractional bits), which is always 0 for this
	 * ImgReader.
	 *
	 * @param c The index of the component.
	 *
	 * @return The position of the fixed-point (i.e. the number of fractional
	 * bits). Always 0 for this ImgReader.
	 * */
	public int getFixedPoint(int c) {
		// Check component index
		if (c<0 || c>2)
			throw new IllegalArgumentException();
		return 0;
	}


	/**
	 * Returns, in the blk argument, the block of image data containing the
	 * specifed rectangular area, in the specified component. The data is
	 * returned, as a reference to the internal data, if any, instead of as a
	 * copy, therefore the returned data should not be modified.
	 *
	 * <P> After being read the coefficients are level shifted by subtracting
	 * 2^(nominal bit range - 1)
	 *
	 * <P>The rectangular area to return is specified by the 'ulx', 'uly', 'w'
	 * and 'h' members of the 'blk' argument, relative to the current
	 * tile. These members are not modified by this method. The 'offset' and
	 * 'scanw' of the returned data can be arbitrary. See the 'DataBlk' class.
	 *
	 * <P>If the data array in <tt>blk</tt> is <tt>null</tt>, then a new one
	 * is created if necessary. The implementation of this interface may
	 * choose to return the same array or a new one, depending on what is more
	 * efficient. Therefore, the data array in <tt>blk</tt> prior to the
	 * method call should not be considered to contain the returned data, a
	 * new array may have been created. Instead, get the array from
	 * <tt>blk</tt> after the method has returned.
	 *
	 * <P>The returned data always has its 'progressive' attribute unset
	 * (i.e. false).
	 *
	 * <P>When an I/O exception is encountered the JJ2KExceptionHandler is
	 * used. The exception is passed to its handleException method. The action
	 * that is taken depends on the action that has been registered in
	 * JJ2KExceptionHandler. See JJ2KExceptionHandler for details.
	 *
	 * <P>This method implements buffering for the 3 components: When the
	 * first one is asked, all the 3 components are read and stored until they
	 * are needed.
	 *
	 * @param blk Its coordinates and dimensions specify the area to
	 * return. Some fields in this object are modified to return the data.
	 *
	 * @param c The index of the component from which to get the data. Only 0,
	 * 1 and 3 are valid.
	 *
	 * @return The requested DataBlk
	 *
	 * @see #getCompData
	 *
	 * @see JJ2KExceptionHandler
	 **/
	public final DataBlk getInternCompData(DataBlk blk, int c) {
		// Check component index
		if (c<0||c>2)
			throw new IllegalArgumentException();

		// Check type of block provided as an argument
		if(blk.getDataType()!=DataBlk.TYPE_INT){
			if(intBlk==null)
				intBlk = new DataBlkInt(blk.ulx,blk.uly,blk.w,blk.h);
			else{
				intBlk.ulx = blk.ulx;
				intBlk.uly = blk.uly;
				intBlk.w = blk.w;
				intBlk.h = blk.h;
			}
			blk = intBlk;
		}

		// If asking a component for the first time for this block, read the 3
		// components
		if ((barr[c] == null) ||
				(dbi.ulx > blk.ulx) || (dbi.uly > blk.uly) ||
				(dbi.ulx+dbi.w < blk.ulx+blk.w) ||
				(dbi.uly+dbi.h < blk.uly+blk.h)) {
			int i;
			int[] red,green,blue;

			// Reset data arrays if needed
			if (barr[c] == null || barr[c].length < blk.w*blk.h) {
				barr[c] = new int[blk.w*blk.h];
			}
			blk.setData(barr[c]);

			i = (c+1)%3;
			if (barr[i] == null || barr[i].length < blk.w*blk.h) {
				barr[i] = new int[blk.w*blk.h];
			}
			i = (c+2)%3;
			if (barr[i] == null || barr[i].length < blk.w*blk.h) {
				barr[i] = new int[blk.w*blk.h];
			}

			// set attributes of the DataBlk used for buffering
			dbi.ulx = blk.ulx;
			dbi.uly = blk.uly;
			dbi.w = blk.w;
			dbi.h = blk.h;

			red = barr[0];
			green = barr[1];
			blue = barr[2];
			{
				// Read line by line
				for (int x = 0; x < blk.w; x++) {
					for (int y = 0; y < blk.h; y++) {
						int color = image.getRGB(blk.ulx + x, blk.uly + y);
						
						// Read every third sample
						int k = x + y*blk.w;
						blue[k]  = ((color&0x0000FF)      ) - DC_OFFSET;
						green[k] = ((color&0x00FF00) >> 8 ) - DC_OFFSET;
						red[k]   = ((color&0xFF0000) >> 16) - DC_OFFSET;
					}
				}
			}
			barr[0] = red;
			barr[1] = green;
			barr[2] = blue;

			// Set buffer attributes
			blk.setData(barr[c]);
			blk.offset = 0;
			blk.scanw = blk.w;
		}
		else { //Asking for the 2nd or 3rd block component
			blk.setData(barr[c]);
			blk.offset = (blk.ulx-dbi.ulx)*dbi.w+blk.ulx-dbi.ulx;
			blk.scanw = dbi.scanw;
		}

		// Turn off the progressive attribute
		blk.progressive = false;
		return blk;
	}

	/**
	 * Returns, in the blk argument, a block of image data containing the
	 * specifed rectangular area, in the specified component. The data is
	 * returned, as a copy of the internal data, therefore the returned data
	 * can be modified "in place".
	 *
	 * <P> After being read the coefficients are level shifted by subtracting
	 * 2^(nominal bit range - 1)
	 *
	 * <P>The rectangular area to return is specified by the 'ulx', 'uly', 'w'
	 * and 'h' members of the 'blk' argument, relative to the current
	 * tile. These members are not modified by this method. The 'offset' of
	 * the returned data is 0, and the 'scanw' is the same as the block's
	 * width. See the 'DataBlk' class.
	 *
	 * <P>If the data array in 'blk' is 'null', then a new one is created. If
	 * the data array is not 'null' then it is reused, and it must be large
	 * enough to contain the block's data. Otherwise an 'ArrayStoreException'
	 * or an 'IndexOutOfBoundsException' is thrown by the Java system.
	 *
	 * <P>The returned data has its 'progressive' attribute unset
	 * (i.e. false).
	 *
	 * <P>When an I/O exception is encountered the JJ2KExceptionHandler is
	 * used. The exception is passed to its handleException method. The action
	 * that is taken depends on the action that has been registered in
	 * JJ2KExceptionHandler. See JJ2KExceptionHandler for details.
	 *
	 * @param blk Its coordinates and dimensions specify the area to
	 * return. If it contains a non-null data array, then it must have the
	 * correct dimensions. If it contains a null data array a new one is
	 * created. The fields in this object are modified to return the data.
	 *
	 * @param c The index of the component from which to get the data. Only
	 * 0,1 and 2 are valid.
	 *
	 * @return The requested DataBlk
	 *
	 * @see #getInternCompData
	 *
	 * @see JJ2KExceptionHandler
	 * */
	public final DataBlk getCompData(DataBlk blk, int c) {
		// NOTE: can not directly call getInterCompData since that returns
		// internally buffered data.
		int ulx,uly,w,h;

		// Check type of block provided as an argument
		if(blk.getDataType()!=DataBlk.TYPE_INT){
			DataBlkInt tmp = new DataBlkInt(blk.ulx,blk.uly,blk.w,blk.h);
			blk = tmp;
		}

		int bakarr[] = (int[])blk.getData();
		// Save requested block size
		ulx = blk.ulx;
		uly = blk.uly;
		w = blk.w;
		h = blk.h;
		// Force internal data buffer to be different from external
		blk.setData(null);
		getInternCompData(blk,c);
		// Copy the data
		if (bakarr == null) {
			bakarr = new int[w*h];
		}
		if (blk.offset == 0 && blk.scanw == w) {
			// Requested and returned block buffer are the same size
			System.arraycopy(blk.getData(),0,bakarr,0,w*h);
		}
		else { // Requested and returned block are different
			for (int i=h-1; i>=0; i--) { // copy line by line
				System.arraycopy(blk.getData(),blk.offset+i*blk.scanw,
						bakarr,i*w,w);
			}
		}
		blk.setData(bakarr);
		blk.offset = 0;
		blk.scanw = blk.w;
		return blk;
	}

	/**
	 * Returns true if the data read was originally signed in the specified
	 * component, false if not. This method always returns false since PPM
	 * data is always unsigned.
	 *
	 * @param c The index of the component, from 0 to N-1.
	 *
	 * @return always false, since PPM data is always unsigned.
	 * */
	public boolean isOrigSigned(int c) {
		// Check component index
		if (c < 0 || c > 2)
			throw new IllegalArgumentException();
		return false;
	}

	/**
	 * Returns a string of information about the object, more than 1 line
	 * long. The information string includes information from the underlying
	 * RandomAccessFile (its toString() method is called in turn).
	 *
	 * @return A string of information about the object.  
	 * */
	public String toString() {
		return "ImgReaderPNG: WxH = " + w + "x" + h + ", Component = 0,1,2";
	}
}
/** END OF UPGRADE */