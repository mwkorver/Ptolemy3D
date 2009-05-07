/*
 * CVS identifier:
 *
 * $Id: TagTreeDecoder.java,v 1.7 2001/08/23 08:04:48 grosbois Exp $
 *
 * Class:                   TagTreeDecoder
 *
 * Description:             Decoder of tag trees
 *
 *
 *
 * COPYRIGHT:
 * 
 * This software module was originally developed by Rapha�l Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askel�f (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, F�lix Henry, Gerard Mozelle and Patrice Onno (Canon Research
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

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

/**
 * This class implements the tag tree decoder. A tag tree codes a 2D
 * matrix of integer elements in an efficient way. The decoding
 * procedure 'update()' updates a value of the matrix from a stream of
 * coded data, given a threshold. This procedure decodes enough
 * information to identify whether or not the value is greater than
 * or equal to the threshold, and updates the value accordingly.
 *
 * <P>In general the decoding procedure must follow the same sequence
 * of elements and thresholds as the encoding one. The encoder is
 * implemented by the TagTreeEncoder class.
 *
 * <P>Tag trees that have one dimension, or both, as 0 are allowed for
 * convenience. Of course no values can be set or coded in such cases.
 *
 * @see jj2000.j2k.codestream.writer.TagTreeEncoder
 * */
class TagTreeDecoder {
	/** The horizontal dimension of the base level */
	private int w = -1;
	/** The vertical dimensions of the base level */
	private int h = -1;
	/** The number of levels in the tag tree */
	private int lvls;
	/** The tag tree values. The first index is the level,
	 * starting at level 0 (leafs). The second index is the element
	 * within the level, in lexicographical order. */
	private int treeV[][];
	/** The tag tree state. The first index is the level, starting at
	 * level 0 (leafs). The second index is the element within the
	 * level, in lexicographical order. */
	private int treeS[][];

	/**
	 * Creates a tag tree decoder with 'w' elements along the
	 * horizontal dimension and 'h' elements along the vertical
	 * direction. The total number of elements is thus 'vdim' x
	 * 'hdim'.
	 *
	 * <P>The values of all elements are initialized to
	 * Integer.MAX_VALUE (i.e. no information decoded so far). The
	 * states are initialized all to 0.
	 *
	 * @param h The number of elements along the vertical direction.
	 *
	 * @param w The number of elements along the horizontal direction.
	 *
	 *
	 * */
	public TagTreeDecoder(int h, int w) {
		set(h, w);
	}

	public void set(int h, int w) {
		int i;
		// Check arguments
		if(w < 0 || h < 0) {
			throw new IllegalArgumentException();
		}
		// Initialize dimensions
		this.w = w;
		this.h = h;
		// Calculate the number of levels
		if(w == 0 || h == 0) {
			lvls = 0; // Empty tree
		}
		else {
			lvls = 1;
			while(h != 1 || w != 1) { // Loop until we reach root
				w = (w + 1) >> 1;
				h = (h + 1) >> 1;
				lvls++;
			}
		}
		// Allocate tree values and states
		if((treeV == null) || (treeV.length < lvls)) {
			treeV = new int[lvls][];
			treeS = new int[lvls][];
		}
		w = this.w;
		h = this.h;
		for(i = 0; i < lvls; i++) {
			if((treeV[i] == null) || (treeV[i].length < h * w)) {
				treeV[i] = new int[h * w];
				treeS[i] = new int[h * w];
			}
			// Initialize to infinite value
			Arrays.fill(treeV[i], Integer.MAX_VALUE);
			Arrays.fill(treeS[i], 0);

			// (no need to initialize to 0 since it's the default)

			w = (w + 1) >> 1;
			h = (h + 1) >> 1;
		}

	}

	/**
	 * Decodes information for the specified element of the tree,
	 * given the threshold, and updates its value. The information
	 * that can be decoded is whether or not the value of the element
	 * is greater than, or equal to, the value of the
	 * threshold.
	 *
	 * @param m The vertical index of the element.
	 *
	 * @param n The horizontal index of the element.
	 *
	 * @param t The threshold to use in decoding. It must be non-negative.
	 *
	 * @param in The stream from where to read the coded information.
	 *
	 * @return The updated value at position (m,n).
	 *
	 * @exception IOException If an I/O error occurs while reading
	 * from 'in'.
	 *
	 * @exception EOFException If the ned of the 'in' stream is
	 * reached before getting all the necessary data.
	 *
	 *
	 * */
	public int update(int m, int n, int t, PktHeaderBitReader in) {
		int k, tmin;
		int idx, ts, tv;

		// Check arguments
		if(m >= h || n >= w || t < 0) {
			throw new IllegalArgumentException();
		}

		// Initialize
		k = lvls - 1;
		tmin = treeS[k][0];

		// Loop on levels
		idx = (m >> k) * ((w + (1 << k) - 1) >> k) + (n >> k);
		while(true) {
			// Cache state and value
			ts = treeS[k][idx];
			tv = treeV[k][idx];
			if(ts < tmin) {
				ts = tmin;
			}
			while(t > ts) {
				if(tv >= ts) { // We are not done yet
					if(in.readBit() == 0) { // '0' bit
						// We know that 'value' > treeS[k][idx]
						ts++;
					}
					else { // '1' bit
						// We know that 'value' = treeS[k][idx]
						tv = ts++;
					}
					// Increment of treeS[k][idx] done above
				}
				else { // We are done, we can set ts and get out
					ts = t;
					break; // get out of this while
				}
			}
			// Update state and value
			treeS[k][idx] = ts;
			treeV[k][idx] = tv;
			// Update tmin or terminate
			if(k > 0) {
				tmin = ts < tv ? ts : tv;
				k--;
				// Index of element for next iteration
				idx = (m >> k) * ((w + (1 << k) - 1) >> k) + (n >> k);
			}
			else {
				// Return the updated value
				return tv;
			}
		}
	}
}
