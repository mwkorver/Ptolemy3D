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
package org.ptolemy3d.tool.batch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Given an input directory with .hdr and .dat files it generates .bdm files
 * ready for ptolemy3d viewer:
 *
 * DemCutter ./indata/ -102000000 41000000 102400 12800 00 ./outdata/ 1000000 .hdr .dat 1 0
 *
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class DemCutter {

	static final String INDEX_FILENAME = "indexheaders.idx";
	static String DEMDIR = "";
	static Vector<DemHeader> DEMHEADERS;
	static int mult = 1;
	static String headerFile = ".asc";
	static String dataFile = ".asc";
	//private static final int ASCII_FILE = 0;
	//private static final int BINARY_FILE = 1;
	private static final int DEM_OUTPUT = 0;
	private static final int TIN_OUTPUT = 1;
	static int outputType = DEM_OUTPUT;
	static boolean isBinary = false;
	static int fileType = 0;

	public static void main(String args[]) {

		if (args.length < 12) {
			System.out.println("usage : DemCutter [dem file directory] [startx] [starty] [tile w] [cell width] [area] [outdir] [mult] [hdr] [dat] [isBinary] [outputType] [inside sig] [wall sig]");
			System.exit(1);
		}

		System.out.println("---------------------------------------------");
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			System.out.println("Argument " + i + ": " + arg);
		}
		System.out.println("---------------------------------------------");

		DEMDIR = args[0];

		int minX = Integer.parseInt(args[1]);
		int maxY = Integer.parseInt(args[2]);
		int tileW = Integer.parseInt(args[3]);
		//        int maxX = minX + tileW;
		//        int minY = maxY - tileW;
		int cellw = Integer.parseInt(args[4]);
		String area = args[5];
		String outdir = args[6];
		mult = Integer.parseInt(args[7]);

		headerFile = args[8];
		dataFile = args[9];

		isBinary = args[10].equals("1") ? true : false;

		outputType = Integer.parseInt(args[11]);

		double inside_sig = 0.3;
		double wall_sig = 0.3;

		if ((outputType == 1) && (args.length > 12)) {
			inside_sig = Double.parseDouble(args[12]);
		}
		if ((outputType == 1) && (args.length > 13)) {
			wall_sig = Double.parseDouble(args[13]);
		}

		String fsuffix = (outputType == 0) ? ".bdm" : ".tin";

		int nCellsX = tileW / cellw + 1;
		int nCellsY = tileW / cellw + 1;

		System.out.println("Num cells: TileW/CellW = " + tileW + "/" + cellw + "=" + nCellsX);
		float[] dem = new float[nCellsY * nCellsX];

		Arrays.fill(dem, 0);

		System.out.println("---------------------------------------------");
		System.out.println("Processing...");

		DEMHEADERS = new Vector<DemHeader>(10, 10);

		String filename = area + "x" + minX + "y" + maxY + fsuffix;

		readHeaderFiles();
		System.out.println("---------------------------------------------");

		int numHeaders = DEMHEADERS.size();

		//        double x_cd, y_cd;
		//        int xpos, ypos;
		int yval, xval;
		for (int i = 0; i < nCellsY; i++) {
			yval = maxY - (i * cellw);
			for (int j = 0; j < nCellsX; j++) {
				xval = minX + (j * cellw);
				// go through dem headers, get best resolution dem                
				float dv = getBestDemValue(xval, yval, numHeaders);
				System.out.println("Elev (" + xval + "/" + yval + "): " + dv);

				if (((i == 0) && (j == 0)) ||
						((i == 0) && (j == (nCellsX - 1))) ||
						((i == (nCellsY - 1)) && (j == 0)) ||
						((i == (nCellsY - 1)) && (j == (nCellsX - 1)))) {
					dem[i * nCellsX + j] = (float) Math.floor(dv); // to fit the above layer
				}
				else {
					dem[i * nCellsX + j] = dv;
				}
			}
		}

		DemHeader dh;
		for (int k = 0; k < numHeaders; k++) {
			dh = (DemHeader) DEMHEADERS.elementAt(k);
			dh.closeFile();
		}
		// write the data to a file
		try {
			DataOutputStream fout = new DataOutputStream(new FileOutputStream(outdir + filename));

			switch (outputType) {
			case DEM_OUTPUT:
				for (int i = 0; i < dem.length; i++) {
					fout.writeShort((short) dem[i]);
				}
				break;
			case TIN_OUTPUT:
				DemConvertor demconv = new DemConvertor(dem, tileW, minX, maxY);
				demconv.verbose = true;
				demconv.runInsidePass(inside_sig);
				demconv.runWallPass(wall_sig);
				demconv.triangulate();
				demconv.stripeTris();
				demconv.createJetstreamFile();
				fout.write(demconv.bout.toByteArray());
				break;
			}
			fout.close();

		}
		catch (Exception e) {
			Logger.getLogger(DemCutter.class.getName()).severe(e.toString());
			e.printStackTrace();
		}
	}

	private static float getBestDemValue(double xval, double yval, int numHeaders) {
		float val = 0;
		DemHeader dh;
		int xpos, ypos;
		double bestRes = Double.MAX_VALUE;

		for (int k = 0; k < numHeaders; k++) {
			dh = (DemHeader) DEMHEADERS.elementAt(k);

			// allow on bottom and left borders, but not on top and right.
			// this is consistent with the way we determine the array position because we round to the nearest value.
			boolean c1 = (yval >= dh.minY);
			boolean c2 = (yval < dh.maxY);
			boolean c3 = (xval >= dh.minX);
			boolean c4 = (xval < dh.maxX);

			if (c1 && c2 && c3 && c4) {
				if (dh.cellsize < bestRes) {

					if (yval == dh.minY) // because we allow values on the left and bottom border
					{
						ypos = 0;
					}
					else {
						ypos = (int) Math.round((double) (dh.pmaxY - yval) / dh.cellsize);
					}

					if (xval == dh.minX) // because we allow values on the left and bottom border
					{
						xpos = 0;
					}
					else {
						xpos = (int) Math.round((double) (xval - dh.pminX) / dh.cellsize);
					}

					float tmpval = dh.read(xpos, ypos);

					if ((tmpval != dh.NODATA_value) && (tmpval >= 0)) {
						bestRes = dh.cellsize;
						val = tmpval;
					}
				}
			}
		}

		return val;
	}

	private static final void readHeaderFiles() {

		// Create a file filter that only accepts files finished with header extension.
		FilenameFilter filter = new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.endsWith(headerFile);
			}
		};

		try {
			File ddir = new File(DEMDIR);

			// If index file doesn't exist then builds it.
			File index = new File(ddir, INDEX_FILENAME);
			if (!index.exists()) {

				System.out.println("Index file doesn't exists. Generating...");

				// Read header files and write to index file
				String[] record = ddir.list(filter);
				BufferedWriter out = new BufferedWriter(new FileWriter(index));
				String temp = "";
				for (int i = 0; i < record.length; i++) {
					if (record[i].indexOf(headerFile) != -1) {
						System.out.println("Found header file: " + DEMDIR + record[i]);
						DemHeader dh = new DemHeader(DEMDIR + record[i], mult, headerFile, dataFile, isBinary);

						temp = DEMDIR + record[i] + "," +
						mult + "," +
						headerFile + "," +
						dataFile + "," +
						isBinary + "," +
						dh.ncols + "," +
						dh.nrows + "," +
						dh.xllcorner + "," +
						dh.yllcorner + "," +
						dh.cellsize + "," +
						dh.NODATA_value + "," +
						dh.byteorder + "\n";
						out.write(temp);
					}
				}
				out.close();
			}

			// Read the index file
			BufferedReader in = new BufferedReader(new FileReader(index));
			String str;
			System.out.println("Reading index file...");
			while ((str = in.readLine()) != null) {
				String[] fields = str.split(",");

				String filename = fields[0];
				double mul = Double.valueOf(fields[1]);
				String header = fields[2];
				String datFile = fields[3];
				boolean binary = Boolean.valueOf(fields[4]);
				int ncols = Integer.valueOf(fields[5]);
				int nrows = Integer.valueOf(fields[6]);
				double xllcorner = Double.valueOf(fields[7]);
				double yllcorner = Double.valueOf(fields[8]);
				double cellsize = Double.valueOf(fields[9]);
				double NODATA_value = Double.valueOf(fields[10]);
				String byteorder = fields[11];

				DEMHEADERS.add(new DemHeader(filename, mul, header, datFile, binary, ncols, nrows, xllcorner, yllcorner, cellsize, NODATA_value, byteorder));
			}
			in.close();
		}
		catch (Exception e) {
			Logger.getLogger(DemCutter.class.getName()).severe(e.toString());
		}
	}
}

class DemHeader {
	/*static*/ String headerFile = ".asc";
	/*static*/ String dataFile = ".asc";

	public int ncols;
	public int nrows;
	public double xllcorner;
	public double yllcorner;
	public double cellsize;
	public double NODATA_value;
	public String byteorder;
	public String file;
	boolean isBinary = false;
	public double maxX,  maxY,  minX,  minY;
	public double pmaxX,  pmaxY,  pminX,  pminY;
	RandomAccessFile rafile;
	int rfpos = 0;
	int rfpos_x = 0;
	int rfpos_y = 0;
	boolean fileExists = false;
	StringTokenizer stok = null;
	double mult = 1;

	public DemHeader(String filename, double mult, String hdr, String dat, boolean isBinary,
			int ncols, int nrows,
			double xllcorner, double yllcorner, double cellsize,
			double NODATA_value, String byteorder) {
		this.headerFile = hdr;
		this.dataFile = dat;
		this.isBinary = isBinary;
		this.file = filename;
		this.mult = mult;

		this.ncols = ncols;
		this.nrows = nrows;
		this.xllcorner = xllcorner;
		this.yllcorner = yllcorner;
		this.cellsize = cellsize;
		this.NODATA_value = NODATA_value;
		this.byteorder = byteorder;

		init();
	}

	public DemHeader(String file, double mult, String hdr, String dat, boolean isBinary) {
		this.headerFile = hdr;
		this.dataFile = dat;
		this.isBinary = isBinary;
		this.file = file;
		this.mult = mult;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		}
		catch (FileNotFoundException e) {
			Logger.getLogger(DemCutter.class.getName()).severe(e.toString());
		}

		String demdata;
		int j = 0;
		try {
			for (j = 0; j < 7; j++) {
				demdata = br.readLine();
				System.out.println("Line " + (j + 1) + ": " + demdata);
				switch (j) {
				case 0:
					ncols = Integer.parseInt(demdata.substring(5, demdata.length()).trim());
					break;
				case 1:
					nrows = Integer.parseInt(demdata.substring(5, demdata.length()).trim());
					break;
				case 2:
					xllcorner = Double.parseDouble(demdata.substring(9, demdata.length()).trim()) * mult;
					break;
				case 3:
					yllcorner = Double.parseDouble(demdata.substring(9, demdata.length()).trim()) * mult;
					break;
				case 4:
					cellsize = Double.parseDouble(demdata.substring(8, demdata.length()).trim()) * mult;
					break;
				case 5:
					NODATA_value = Double.parseDouble(demdata.substring(12, demdata.length()).trim());
					break;
				case 6:
					if (isBinary) {
						byteorder = demdata.substring(9, demdata.length()).trim();
					}
					break;
				}
			}
		}
		catch (IOException e) {
			Logger.getLogger(DemCutter.class.getName()).severe(e.toString());
		}

		init();
	}

	private void init() {
		minX = xllcorner;
		minY = yllcorner;

		maxX = minX + (cellsize * ncols); // add +1 cellsize to reach corners
		maxY = minY + (cellsize * nrows); // add +1 cellsize to reach corners

		pminX = xllcorner + (cellsize / 2);
		pminY = yllcorner + (cellsize / 2);

		pmaxX = pminX + (cellsize * (ncols - 1)); // add +1 cellsize to reach corners
		pmaxY = pminY + (cellsize * (nrows - 1)); // add +1 cellsize to reach corners

		prepareDatFile();
	}

	public void prepareDatFile() {
		rfpos = 0;
		rfpos_y = -1;
		try {
			rafile = new RandomAccessFile(new File(file.substring(0, file.indexOf(headerFile)) + dataFile), "r");
			fileExists = true;
		}
		catch (Exception e) {
			Logger.getLogger(DemCutter.class.getName()).severe(e.toString());
			fileExists = false;
		}
	}

	public void closeFile() {
		try {
			rafile.close();
		}
		catch (Exception e) {
			Logger.getLogger(DemCutter.class.getName()).severe(e.toString());
		}
	}

	public float read(int xpos, int ypos) {
		if (isBinary) {
			return readBinFloat(xpos, ypos);
		}
		else {
			return readAsciiFloat(xpos, ypos);
		}
	}

	private float readAsciiFloat(int xpos, int ypos) {

		try {

			if (fileExists) {

				if (ypos > rfpos_y) {
					while ((ypos - 1) > rfpos_y) {
						rafile.readLine();
						rfpos_y++;
					}
					rfpos_y++;
					stok = new StringTokenizer(rafile.readLine(), " ");
					rfpos_x = 0;
				}

				while (rfpos_x < (xpos - 1)) {
					stok.nextToken();
					rfpos_x++;
				}
				rfpos_x++;
				return Float.parseFloat(stok.nextToken());
			}
			else {
				return 0;
			}
		}
		catch (Exception e) {
			Logger.getLogger(DemCutter.class.getName()).severe(e.toString());
		}
		return 0;
	}

	public float readBinFloat(int xpos, int ypos) {
		try {

			if (fileExists) {
				byte b1, b2, b3, b4;

				int fp = ((ypos * ncols) + xpos) * 4;

				rafile.seek(fp);

				b1 = (byte) rafile.read();
				b2 = (byte) rafile.read();
				b3 = (byte) rafile.read();
				b4 = (byte) rafile.read();
				rfpos += 4;
				return Float.intBitsToFloat((int) ((b4) << 24 |
						(b3 & 0xff) << 16 |
						(b2 & 0xff) << 8 |
						(b1 & 0xff)));

			}
			else {
				return 0;
			}

		}
		catch (Exception e) {
			Logger.getLogger(DemCutter.class.getName()).severe(e.toString());
		}
		return 0;
	}
}


