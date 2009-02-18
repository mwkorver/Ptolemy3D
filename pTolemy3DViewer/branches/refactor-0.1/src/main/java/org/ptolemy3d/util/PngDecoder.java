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
package org.ptolemy3d.util;

import java.io.ByteArrayInputStream;
import java.util.zip.InflaterInputStream;

public class PngDecoder
{
    private static final int IHDR = 0x49484452;
    private static final int PLTE = 0x504c5445;
    private static final int IDAT = 0x49444154;
    private static final int IEND = 0x49454e44;
    private static final int bKGD = 0x624b4744;
    private static final int tRNS = 0x74524e53;
    private static final short[] sig =
    {
        137, 80, 78, 71, 13, 10, 26, 10
    };
    private static final int[] offs =
    {
        0, 0, 4, 0, 2, 0, 1, 0
    };
    private static final int[] sp =
    {
        8, 8, 8, 4, 4, 2, 2, 1
    };

    /*  no need to support these right now.
    private static final int cHRM = 0x6348524d;
    private static final int gAMA = 0x67414d41;
    private static final int hIST = 0x68495354;
    private static final int pHYs = 0x70485973;
    private static final int sBIT = 0x73424954;
    private static final int tEXt = 0x74455874;
    private static final int tIME = 0x74494d45;
    private static final int zTXt = 0x7a545874;
    private static final int sRGB = 0x73524742;
    private static final int sPLT = 0x73504c54;
    private static final int oFFs = 0x6f464673;
    private static final int sCAL = 0x7343414c;
    private static final int iCCP = 0x69434350;
    private static final int pCAL = 0x7043414c;
    private static final int iTXt = 0x69545874;
    private static final int gIFg = 0x67494667;
    private static final int gIFx = 0x67494678;
     */
    /*
    static but Thread safe.
    does not support interlacing
     */
    public static byte[] decode(byte[] dat, int[] meta)
    {
        // the meta array will hold all image info that we would like to store
        // for now, just width and height and color model
        int error = 0;

        int chunk_type;
        int chunk_len;
        int chunk_data_start;
        int chunk_crc;
        byte pl_trans[] = null;

        int img_width = 0, img_height = 0;

        if (meta == null)
        {
            meta = new int[3];
        }
        if (meta.length < 3)
        {
            meta = new int[3];
        }

        byte[] idat = null;

        for (int i = 0; i < sig.length; i++)
        {
            if ((short) (dat[i] & 0xFF) != sig[i])
            {
                error = 99;
                return null;
            }
        }

        // go through chunks
        chunk_type = -1;
        int[] cur = new int[1];
        cur[0] = sig.length;
        byte[][] pal = null;

        int bit_depth = 0, color_type = 0, compression = 0, filter = 0, interlace = 0;
        boolean hasAlpha = false;

        while (chunk_type != IEND)
        {
            chunk_len = ByteReader.readInt(dat, cur);
            chunk_type = ByteReader.readInt(dat, cur);
            chunk_data_start = cur[0];

            switch (chunk_type)
            {
                case IEND:
                    break;
                case IHDR:
                    if (chunk_len != 13)
                    {
                        error = 1;
                    }
                    img_width = meta[0] = ByteReader.readInt(dat, cur);
                    img_height = meta[1] = ByteReader.readInt(dat, cur);
                    bit_depth = (dat[cur[0]++] & 0xff);

                    color_type = meta[2] = (dat[cur[0]++] & 0xff);
                    compression = (dat[cur[0]++] & 0xff);
                    filter = (dat[cur[0]++] & 0xff);
                    interlace = (dat[cur[0]++] & 0xff);
                    if ((color_type == 4) || (color_type == 6))
                    {
                        hasAlpha = true;
                    }
                    break;
                case PLTE:
                    if (chunk_len % 3 != 0)
                    {
                        error = 2;
                    }
                    int numcolors = (int) (chunk_len / 3);

                    pal = new byte[numcolors][3];
                    for (int k = 0; k < numcolors; k++)
                    {
                        pal[k][0] = (byte) (dat[cur[0]++] & 0xff);
                        pal[k][1] = (byte) (dat[cur[0]++] & 0xff);
                        pal[k][2] = (byte) (dat[cur[0]++] & 0xff);
                    }
                    break;
                case IDAT:
                    if (idat == null)
                    {
                        idat = new byte[chunk_len];
                        System.arraycopy(dat, cur[0], idat, 0, chunk_len);
                    }
                    else
                    {
                        byte[] nidat = new byte[idat.length + chunk_len];
                        System.arraycopy(idat, 0, nidat, 0, idat.length);
                        System.arraycopy(dat, cur[0], nidat, idat.length, chunk_len);
                        idat = nidat;
                    }
                    cur[0] += chunk_len;
                    break;
                case tRNS:
                    pl_trans = new byte[chunk_len];
                    for (int yy = 0; yy < chunk_len; yy++)
                    {
                        pl_trans[yy] = (byte) (dat[cur[0]++] & 0xff);
                    }
                    hasAlpha = true;
                    break;
                default:
                    cur[0] += chunk_len;
                    break;
            }
            chunk_crc = ByteReader.readInt(dat, cur);
        }

        int scaler = 1;
        if (color_type == 2)
        {
            scaler = 3;
        }
        else if (color_type == 4)
        {
            scaler = 2;
        }
        else if (color_type == 6)
        {
            scaler = 4;
        }

        if (meta.length > 3)
        {
            meta[3] = (hasAlpha) ? 4 : 3;
        }
        int numcomponents = (hasAlpha) ? 4 : 3;
        byte[] out = new byte[img_width * img_height * numcomponents];

        InflaterInputStream infstr = new InflaterInputStream(new ByteArrayInputStream(idat, 0, idat.length));
        int bytesPerRow = (int) (img_width * scaler * ((float) bit_depth / 8));
        int a, b, pi = 0, ix = 0;
        int bpp = Math.max(1, (bit_depth * scaler) / 8);
        byte[] rowdata, prev;

        try
        {
            // decode the data
            if (interlace == 1)
            {
                int numPasses = 7;
                int maxSpacingX = 0;
                int maxSpacingY = 0;
                int widestPass = 0;
                int minSpacingX = img_width;
                for (int i = 0; i < numPasses; i++)
                {
                    int sp_x = sp[i + 1];
                    if (sp_x < minSpacingX)
                    {
                        minSpacingX = sp_x;
                        widestPass = i;
                    }
                    maxSpacingX = Math.max(maxSpacingX, sp_x);
                    maxSpacingY = Math.max(maxSpacingY, sp[i]);
                }
                int MaxPassWidth = ((img_width / maxSpacingX) * countPixels(widestPass, maxSpacingX) +
                        countPixels(widestPass, img_width % maxSpacingX));

                int fillSize = Math.max(1, 8 / bit_depth);
                int rowSize = (int) (MaxPassWidth * scaler * ((float) bit_depth / 8) + bpp);
                rowdata = new byte[rowSize];
                prev = new byte[rowSize];
                for (a = 0; a < rowSize; a++)
                {
                    rowdata[a] = 0;
                }
                for (int pass = 0; pass < numPasses; pass++)
                {

                    int passWidth = ((img_width / maxSpacingX) * countPixels(pass, maxSpacingX) + countPixels(pass, img_width % maxSpacingX));
                    int extra = passWidth % fillSize;
                    if (extra > 0)
                    {
                        passWidth += (fillSize - extra);
                    }
                    int rowIncrement = sp[pass];
                    int colIncrement = sp[pass + 1];
                    int colStart = offs[pass + 1];
                    int row = offs[pass];
                    int pix_val;
                    bytesPerRow = (int) (passWidth * scaler * ((float) bit_depth / 8));
                    for (int i = 0; i < rowSize; i++)
                    {
                        prev[i] = 0;
                    }
                    while (row < img_height)
                    {
                        rowdata = getFilteredRow(rowdata, prev, bytesPerRow, bpp, infstr);
                        int col = colStart;
                        int x = 0;
                        int poff;
                        while (col < img_width)
                        {
                            switch (color_type)
                            {
                                case 0:
                                    poff = (row * img_width * 3) + (col * 3);
                                    ix = getPixel(x++, bit_depth, rowdata);
                                    for (b = 0; b < 3; b++)
                                    {
                                        out[poff + b] = (byte) ix;
                                    }
                                    break;
                                case 3:
                                    poff = (row * img_width * numcomponents) + (col * numcomponents);
                                    pix_val = getPixel(x++, bit_depth, rowdata);
                                    System.arraycopy(pal[pix_val], 0, out, poff, 3);
                                    if (numcomponents == 4)
                                    {
                                        if (pl_trans.length > pix_val)
                                        {
                                            out[poff + 3] = pl_trans[pix_val];
                                        }
                                        else
                                        {
                                            out[poff + 3] = (byte) 255;
                                        }
                                    }
                                    break;
                                case 2:
                                case 6:
                                    poff = (row * img_width * scaler) + (col * scaler);
                                    for (b = 0; b < scaler; b++)
                                    {
                                        out[poff + b] = rowdata[x++];
                                    }
                                    break;
                                case 4:
                                    poff = (row * img_width * 4) + (col * 4);
                                    for (b = 0; b < 3; b++)
                                    {
                                        out[poff + b] = rowdata[x];
                                    }
                                    x++;
                                    out[poff + 3] = rowdata[x++];
                                    break;
                            }
                            col += colIncrement;
                        }
                        row += rowIncrement;
                    }
                }
            }
            else
            {
                rowdata = new byte[bytesPerRow];
                prev = new byte[bytesPerRow];
                int pix_val, poff;
                for (int rownum = 0; rownum < img_height; rownum++)
                {
                    rowdata = getFilteredRow(rowdata, prev, bytesPerRow, bpp, infstr);
                    // copy the row data to the out array
                    switch (color_type)
                    {
                        case 0:
                            for (a = 0; a < img_width; a++)
                            {
                                ix = getPixel(a, bit_depth, rowdata);
                                for (b = 0; b < 3; b++)
                                {
                                    out[pi++] = (byte) ix;
                                }
                            }
                            break;
                        case 3:
                            for (a = 0; a < img_width; a++)
                            {
                                poff = (rownum * img_width + a) * numcomponents;
                                pix_val = getPixel(a, bit_depth, rowdata);
                                System.arraycopy(pal[pix_val], 0, out, poff, 3);
                                if (numcomponents == 4)
                                {
                                    if (pl_trans.length > pix_val)
                                    {
                                        out[poff + 3] = pl_trans[pix_val];
                                    }
                                    else
                                    {
                                        out[poff + 3] = (byte) 255;
                                    }
                                }
                            }
                            break;
                        case 4://expand grayscale to fill an rgba space
                            for (a = 0; a < bytesPerRow; a += 2)
                            {
                                for (b = 0; b < 3; b++)
                                {
                                    out[pi++] = rowdata[a];
                                }
                                out[pi++] = rowdata[a + 1];
                            }
                            break;
                        default:
                            System.arraycopy(rowdata, 0, out, rownum * bytesPerRow, bytesPerRow);
                            break;
                    }
                }
            } // else
        }
        catch (Exception e)
        {
        }

        return out;

    }

    private static int getPixel(int a, int bit_depth, byte[] rowdata)
    {
        switch (bit_depth)
        {
            case 1:
                return rowdata[a / 8] >>> (7 - a % 8) & 1;
            case 2:
                int rm = a % 4;
                return (rm == 0) ? rowdata[a / 4] >>> 6 & 0x03 : (rm == 1) ? rowdata[a / 4] >>> 4 & 0x03 : (rm == 2) ? rowdata[a / 4] >>> 2 & 0x03 : rowdata[a / 4] & 0x03;
            case 4:
                return (a % 2 == 0) ? rowdata[a / 2] >>> 4 & 0x0F : rowdata[a / 2] & 0x0F;
            case 8:
                return (int) (rowdata[a] & 0xff);
            default:
                return 0;
        }
    }

    static private byte[] getFilteredRow(byte[] rowdata, byte[] prev, int bytesPerRow, int bpp, InflaterInputStream infstr) throws Exception
    {
        int xc, xp;
        int filterType = infstr.read();
        if (filterType > 1)
        {
            System.arraycopy(rowdata, 0, prev, 0, bytesPerRow);
        }
        int needed = bytesPerRow;
        int bytesread;
        while (needed > 0)
        {
            bytesread = infstr.read(rowdata, bytesPerRow - needed, needed);
            needed -= bytesread;
        }
        // apply filtering
        switch (filterType)
        {
            case 1: // Sub
                for (xc = -bpp  , xp = 0; xp < bytesPerRow; xc++, xp++)
                {
                    rowdata[xp] = (byte) (rowdata[xp] + ((xc >= 0) ? rowdata[xc] : 0));
                }
                break;
            case 2: // Up
                for (xp = 0; xp < bytesPerRow; xp++)
                {
                    rowdata[xp] = (byte) (rowdata[xp] + prev[xp]);
                }
                break;
            case 3: // Average
                for (xc = -bpp  , xp = 0; xp < bytesPerRow; xc++, xp++)
                {
                    rowdata[xp] = (byte) (rowdata[xp] + (((xc >= 0) ? (rowdata[xc] & 0xFF) : 0) + (0xFF & prev[xp])) / 2);
                }
                break;
            case 4: // Paeth
                for (xc = -bpp  , xp = 0; xp < bytesPerRow; xc++, xp++)
                {
                    rowdata[xp] = (byte) (rowdata[xp] + Paeth(((xc >= 0) ? rowdata[xc] : 0), prev[xp], ((xc >= 0) ? prev[xc] : 0)));
                }
                break;
        }
        return rowdata;
    }

    static private int Paeth(byte L, byte u, byte nw)
    {
        int a = 0xFF & L; //  inline byte->int
        int b = 0xFF & u;
        int c = 0xFF & nw;
        int p = a + b - c;
        int pa = p - a;
        if (pa < 0)
        {
            pa = -pa; // inline Math.abs
        }
        int pb = p - b;
        if (pb < 0)
        {
            pb = -pb;
        }
        int pc = p - c;
        if (pc < 0)
        {
            pc = -pc;
        }
        if (pa <= pb && pa <= pc)
        {
            return a;
        }
        if (pb <= pc)
        {
            return b;
        }
        return c;
    }

    static private final int countPixels(int pass, int w)
    {
        int cur = 0;
        int next = offs[pass + 1];
        for (int x = 0; x < w; x++)
        {
            if (x == next)
            {
                cur++;
                next = x + sp[pass + 1];
            }
        }
        return cur;
    }
}
