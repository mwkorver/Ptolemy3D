/* 
 * CVS identifier:
 * 
 * $Id: PktHeaderBitReader.java,v 1.10 2001/09/14 09:29:45 grosbois Exp $
 * 
 * Class:                   PktHeaderBitReader
 * 
 * Description:             Bit based reader for packet headers
 * 
 * 
 * 
 * COPYRIGHT:
 * 
 * This software module was originally developed by Raphaël Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askelöf (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, Félix Henry, Gerard Mozelle and Patrice Onno (Canon Research
 * Centre France S.A) in the course of development of the JPEG2000
 * standard as specified by ISO/IEC 15444 (JPEG 2000 Standard). This
 * software module is an implementation of a part of the JPEG 2000
 * Standard. Swiss Federal Institute of Technology-EPFL, Ericsson Radio
 * Systems AB and Canon Research Centre France S.A (collectively JJ2000
 * Partners) agree not to assert against ISO/IEC and users of the JPEG
 * 2000 Standard (Users) any of their rights under the copyright, not
 * including other intellectual property rights, for this software module
 * with respect to the usage by ISO/IEC and Users of this software module
 * or modifications thereof for use in hardware or software products
 * claiming conformance to the JPEG 2000 Standard. Those intending to use
 * this software module in hardware or software products are advised that
 * their use may infringe existing patents. The original developers of
 * this software module, JJ2000 Partners and ISO/IEC assume no liability
 * for use of this software module or modifications thereof. No license
 * or right to this software module is granted for non JPEG 2000 Standard
 * conforming products. JJ2000 Partners have full right to use this
 * software module for his/her own purpose, assign or donate this
 * software module to any third party and to inhibit third parties from
 * using this software module for non JPEG 2000 Standard conforming
 * products. This copyright notice must be included in all copies or
 * derivative works of this software module.
 * 
 * Copyright (c) 1999/2000 JJ2000 Partners.
 * */
package org.ptolemy3d.jp2.fast;

import java.io.IOException;

/**
 * This class provides a bit based reading facility from a byte based one,
 * applying the bit unstuffing procedure as required by the packet headers.
 */
class PktHeaderBitReader {
	private byte[] bais;
	private int bbuf;
	private int bpos;
	private int nextbbuf;
	private int bais_pos;

	public PktHeaderBitReader() {}

	public final void set(byte[] bais) {
		this.bais = bais;
		bais_pos = 0;
	}

	public final int readBit() {
		if(bpos == 0) { // Is bit buffer empty?
			if(bbuf != 0xFF) { // No bit stuffing
				bbuf = read();
				bpos = 8;
				if(bbuf == 0xFF) { // If new bit stuffing get next byte
					nextbbuf = read();
				}
			}
			else { // We had bit stuffing, nextbuf can not be 0xFF
				bbuf = nextbbuf;
				bpos = 7;
			}
		}
		return (bbuf >> --bpos) & 0x01;
	}

	public final int readBits(int n) throws IOException {
		int bits; // The read bits

		// Can we get all bits from the bit buffer?
		if(n <= bpos) {
			return (bbuf >> (bpos -= n)) & ((1 << n) - 1);
		}
		else {
			// NOTE: The implementation need not be recursive but the not
			// recursive one exploits a bug in the IBM x86 JIT and caused
			// incorrect decoding (Diego Santa Cruz).
			bits = 0;
			do {
				// Get all the bits we can from the bit buffer
				bits <<= bpos;
				n -= bpos;
				bits |= readBits(bpos);
				// Get an extra bit to load next byte (here bpos is 0)
				if(bbuf != 0xFF) { // No bit stuffing
					bbuf = read();
					bpos = 8;
					if(bbuf == 0xFF) { // If new bit stuffing get next byte
						nextbbuf = read();
					}
				}
				else { // We had bit stuffing, nextbuf can not be 0xFF
					bbuf = nextbbuf;
					bpos = 7;
				}
			}
			while(n > bpos);
			// Get the last bits, if any
			bits <<= n;
			bits |= (bbuf >> (bpos -= n)) & ((1 << n) - 1);
			// Return result
			return bits;
		}
	}

	public void sync() {
		bbuf = 0;
		bpos = 0;
	}

	private int read() {
		return (bais[bais_pos++] & 0xFF);
	}
}
