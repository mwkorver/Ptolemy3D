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
package org.ptolemy3d.tile.jp2;

import java.io.IOException;

/**
 * This class provides a bit based reading facility from a byte based one,
 * applying the bit unstuffing procedure as required by the packet headers.
 *
 */
class PktHeaderBitReader
{

    byte[] bais;
    int bbuf;
    int bpos;
    int nextbbuf;
    int bais_pos;

    PktHeaderBitReader()
    {
    }

    final void set(byte[] bais)
    {
        this.bais = bais;
        bais_pos = 0;
    }

    final int readBit()
    {
        if (bpos == 0)
        { // Is bit buffer empty?
            if (bbuf != 0xFF)
            { // No bit stuffing
                bbuf = read();
                bpos = 8;
                if (bbuf == 0xFF)
                { // If new bit stuffing get next byte
                    nextbbuf = read();
                }
            }
            else
            { // We had bit stuffing, nextbuf can not be 0xFF
                bbuf = nextbbuf;
                bpos = 7;
            }
        }
        return (bbuf >> --bpos) & 0x01;
    }

    final int readBits(int n) throws IOException
    {
        int bits; // The read bits

        // Can we get all bits from the bit buffer?
        if (n <= bpos)
        {
            return (bbuf >> (bpos -= n)) & ((1 << n) - 1);
        }
        else
        {
            // NOTE: The implementation need not be recursive but the not
            // recursive one exploits a bug in the IBM x86 JIT and caused
            // incorrect decoding (Diego Santa Cruz).
            bits = 0;
            do
            {
                // Get all the bits we can from the bit buffer
                bits <<= bpos;
                n -= bpos;
                bits |= readBits(bpos);
                // Get an extra bit to load next byte (here bpos is 0)
                if (bbuf != 0xFF)
                { // No bit stuffing
                    bbuf = read();
                    bpos = 8;
                    if (bbuf == 0xFF)
                    { // If new bit stuffing get next byte
                        nextbbuf = read();
                    }
                }
                else
                { // We had bit stuffing, nextbuf can not be 0xFF
                    bbuf = nextbbuf;
                    bpos = 7;
                }
            }
            while (n > bpos);
            // Get the last bits, if any
            bits <<= n;
            bits |= (bbuf >> (bpos -= n)) & ((1 << n) - 1);
            // Return result
            return bits;
        }
    }

    void sync()
    {
        bbuf = 0;
        bpos = 0;
    }

    private int read()
    {
        return (bais[bais_pos++] & 0xFF);
    }
}
