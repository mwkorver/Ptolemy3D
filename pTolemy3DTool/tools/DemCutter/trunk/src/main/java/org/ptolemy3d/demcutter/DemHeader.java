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
package org.ptolemy3d.demcutter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * DemHeader stores HDR fields.
 * 
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public class DemHeader {

    private static final Logger logger = Logger.getLogger(DemHeader.class.getName());
    private String headerFile = ".hdr";
    private String dataFile = ".asc";
    private int ncols;
    private int nrows;
    private double xllcorner;
    private double yllcorner;
    private double cellsize;
    private double NODATA_value;
    private String byteorder;
    private String file;
    private boolean isBinary = false;
    private double maxX, maxY, minX, minY;
    private double pmaxX, pmaxY, pminX, pminY;
    private RandomAccessFile rafile;
    private int rfpos = 0;
    private int rfpos_x = 0;
    private int rfpos_y = 0;
    private boolean fileExists = false;
    private StringTokenizer stok = null;
    private double mult = 1;

    /**
     *
     * @param filename
     * @param mult
     * @param hdr
     * @param dat
     * @param isBinary
     * @param ncols
     * @param nrows
     * @param xllcorner
     * @param yllcorner
     * @param cellsize
     * @param NODATA_value
     * @param byteorder
     */
    public DemHeader(String filename, double mult, String hdr, String dat, boolean isBinary,
            int ncols, int nrows, double xllcorner, double yllcorner, double cellsize,
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

    /**
     * 
     * @param file
     * @param mult
     * @param hdr
     * @param dat
     * @param isBinary
     */
    public DemHeader(String file, double mult, String hdr, String dat, boolean isBinary) {
        this.headerFile = hdr;
        this.dataFile = dat;
        this.isBinary = isBinary;
        this.file = file;
        this.mult = mult;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            logger.severe(e.getMessage());
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
        } catch (IOException e) {
            logger.severe(e.getMessage());
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
        } catch (Exception e) {
            logger.severe(e.getMessage());
            fileExists = false;
        }
    }

    public void closeFile() {
        try {
            rafile.close();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }

    public float read(int xpos, int ypos) {
        if (isBinary) {
            return readBinFloat(xpos, ypos);
        } else {
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
            } else {
                return 0;
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
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
            } else {
                return 0;
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return 0;
    }

    /**
     * @return the maxX
     */
    public double getMaxX() {
        return maxX;
    }

    /**
     * @return the maxY
     */
    public double getMaxY() {
        return maxY;
    }

    /**
     * @return the minX
     */
    public double getMinX() {
        return minX;
    }

    /**
     * @return the minY
     */
    public double getMinY() {
        return minY;
    }

    /**
     * @return the cellsize
     */
    public double getCellsize() {
        return cellsize;
    }

    /**
     * @return the pmaxX
     */
    public double getPmaxX() {
        return pmaxX;
    }

    /**
     * @return the pmaxY
     */
    public double getPmaxY() {
        return pmaxY;
    }

    /**
     * @return the pminX
     */
    public double getPminX() {
        return pminX;
    }

    /**
     * @return the pminY
     */
    public double getPminY() {
        return pminY;
    }

    /**
     * @return the ncols
     */
    public int getNcols() {
        return ncols;
    }

    /**
     * @return the nrows
     */
    public int getNrows() {
        return nrows;
    }

    /**
     * @return the xllcorner
     */
    public double getXllcorner() {
        return xllcorner;
    }

    /**
     * @return the yllcorner
     */
    public double getYllcorner() {
        return yllcorner;
    }

    /**
     * @return the NODATA_value
     */
    public double getNODATA_value() {
        return NODATA_value;
    }

    /**
     * @return the byteorder
     */
    public String getByteorder() {
        return byteorder;
    }
}
