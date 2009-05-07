/*
 * CVS identifier:
 *
 * $Id: MQDecoder.java,v 1.32 2001/10/17 16:58:00 grosbois Exp $
 *
 * Class:                   MQDecoder
 *
 * Description:             Class that encodes a number of bits using the
 *                          MQ arithmetic decoder
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
/* 
 * CVS identifier:
 * 
 * $Id: StdEntropyDecoder.java,v 1.30 2001/10/25 12:12:16 qtxjoas Exp $
 * 
 * Class:                   StdEntropyDecoder
 * 
 * Description:             Entropy decoding engine of stripes in code-blocks
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

import java.util.Arrays;

class EntropyDecoder
{
	private final static int qe[] = {0x5601, 0x3401, 0x1801, 0x0ac1, 0x0521, 0x0221, 0x5601, 0x5401, 0x4801, 0x3801,
			0x3001, 0x2401, 0x1c01, 0x1601, 0x5601, 0x5401, 0x5101, 0x4801, 0x3801, 0x3401, 0x3001, 0x2801, 0x2401,
			0x2201, 0x1c01, 0x1801, 0x1601, 0x1401, 0x1201, 0x1101, 0x0ac1, 0x09c1, 0x08a1, 0x0521, 0x0441, 0x02a1,
			0x0221, 0x0141, 0x0111, 0x0085, 0x0049, 0x0025, 0x0015, 0x0009, 0x0005, 0x0001, 0x5601};
	private final static int nMPS[] = {1, 2, 3, 4, 5, 38, 7, 8, 9, 10, 11, 12, 13, 29, 15, 16, 17, 18, 19, 20, 21, 22,
			23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 45, 46};
	private final static int nLPS[] = {1, 6, 9, 12, 29, 33, 6, 14, 14, 14, 17, 18, 20, 21, 14, 14, 15, 16, 17, 18, 19,
			19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 46};
	private final static int switchLM[] = {1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

	private final static int initStates[] = {46, 3, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	private final boolean doer = false;
	private final static int OPT_BYPASS = 1;
	private final static int OPT_RESET_MQ = 1 << 1;
	private final static int OPT_REG_TERM = 1 << 2;
	private final static int OPT_VERT_STR_CAUSAL = 1 << 3;
	private final static int OPT_ER_TERM = 1 << 4;
	private final static int OPT_SEG_MARKERS = 1 << 5;
	private static final int STRIPE_HEIGHT = 4;
	private static final int NUM_NON_BYPASS_MS_BP = 4;
	private static final int ZC_LUT_BITS = 8;
	private static final int ZC_LUT_LH[] = new int[1 << ZC_LUT_BITS];
	private static final int ZC_LUT_HL[] = new int[1 << ZC_LUT_BITS];
	private static final int ZC_LUT_HH[] = new int[1 << ZC_LUT_BITS];
	private static final int SC_LUT_BITS = 9;
	private static final int SC_LUT[] = new int[1 << SC_LUT_BITS];
	private static final int SC_LUT_MASK = (1 << 4) - 1;
	private static final int SC_SPRED_SHIFT = 31;
	private static final int INT_SIGN_BIT = 1 << 31;
	private static final int MR_LUT_BITS = 9;
	private static final int MR_LUT[] = new int[1 << MR_LUT_BITS];
	private static final int NUM_CTXTS = 19;
	private static final int RLC_CTXT = 1;
	private static final int UNIF_CTXT = 0;
	private static final int SEG_MARKER = 10;
	private static final int STATE_SEP = 16;
	private static final int STATE_SIG_R1 = 1 << 15;
	private static final int STATE_VISITED_R1 = 1 << 14;
	private static final int STATE_NZ_CTXT_R1 = 1 << 13;
	private static final int STATE_H_L_SIGN_R1 = 1 << 12;
	private static final int STATE_H_R_SIGN_R1 = 1 << 11;
	private static final int STATE_V_U_SIGN_R1 = 1 << 10;
	private static final int STATE_V_D_SIGN_R1 = 1 << 9;
	private static final int STATE_PREV_MR_R1 = 1 << 8;
	private static final int STATE_H_L_R1 = 1 << 7;
	private static final int STATE_H_R_R1 = 1 << 6;
	private static final int STATE_V_U_R1 = 1 << 5;
	private static final int STATE_V_D_R1 = 1 << 4;
	private static final int STATE_D_UL_R1 = 1 << 3;
	private static final int STATE_D_UR_R1 = 1 << 2;
	private static final int STATE_D_DL_R1 = 1 << 1;
	private static final int STATE_D_DR_R1 = 1;
	private static final int STATE_SIG_R2 = STATE_SIG_R1 << STATE_SEP;
	private static final int STATE_VISITED_R2 = STATE_VISITED_R1 << STATE_SEP;
	private static final int STATE_NZ_CTXT_R2 = STATE_NZ_CTXT_R1 << STATE_SEP;
	private static final int STATE_H_L_SIGN_R2 = STATE_H_L_SIGN_R1 << STATE_SEP;
	private static final int STATE_H_R_SIGN_R2 = STATE_H_R_SIGN_R1 << STATE_SEP;
	private static final int STATE_V_U_SIGN_R2 = STATE_V_U_SIGN_R1 << STATE_SEP;
	private static final int STATE_V_D_SIGN_R2 = STATE_V_D_SIGN_R1 << STATE_SEP;
	private static final int STATE_PREV_MR_R2 = STATE_PREV_MR_R1 << STATE_SEP;
	private static final int STATE_H_L_R2 = STATE_H_L_R1 << STATE_SEP;
	private static final int STATE_H_R_R2 = STATE_H_R_R1 << STATE_SEP;
	private static final int STATE_V_U_R2 = STATE_V_U_R1 << STATE_SEP;
	private static final int STATE_V_D_R2 = STATE_V_D_R1 << STATE_SEP;
	private static final int STATE_D_UL_R2 = STATE_D_UL_R1 << STATE_SEP;
	private static final int STATE_D_UR_R2 = STATE_D_UR_R1 << STATE_SEP;
	private static final int STATE_D_DL_R2 = STATE_D_DL_R1 << STATE_SEP;
	private static final int STATE_D_DR_R2 = STATE_D_DR_R1 << STATE_SEP;
	private static final int SIG_MASK_R1R2 = STATE_SIG_R1 | STATE_SIG_R2;
	private static final int VSTD_MASK_R1R2 = STATE_VISITED_R1 | STATE_VISITED_R2;
	private static final int ZC_MASK = (1 << 8) - 1;
	private static final int SC_SHIFT_R1 = 4;
	private static final int SC_SHIFT_R2 = SC_SHIFT_R1 + STATE_SEP;
	private static final int SC_MASK = (1 << SC_LUT_BITS) - 1;
	private static final int MR_MASK = (1 << 9) - 1;

	static {
		int i, j;
		int inter_sc_lut[];
		int ds, us, rs, ls;
		int dsgn, usgn, rsgn, lsgn;
		int h, v;
		ZC_LUT_LH[0] = 2;
		for(i = 1; i < 16; i++) { // Two or more diagonal coeffs significant
			ZC_LUT_LH[i] = 4;
		}
		for(i = 0; i < 4; i++) { // Only one diagonal coeff significant
			ZC_LUT_LH[1 << i] = 3;
		}
		for(i = 0; i < 16; i++) {
			ZC_LUT_LH[STATE_V_U_R1 | i] = 5;
			ZC_LUT_LH[STATE_V_D_R1 | i] = 5;
			ZC_LUT_LH[STATE_V_U_R1 | STATE_V_D_R1 | i] = 6;
		}
		// - One horiz. neighbor significant, diagonal/vertical non-significant
		ZC_LUT_LH[STATE_H_L_R1] = 7;
		ZC_LUT_LH[STATE_H_R_R1] = 7;
		// - One horiz. significant, no vertical significant, one or more
		// diagonal significant
		for(i = 1; i < 16; i++) {
			ZC_LUT_LH[STATE_H_L_R1 | i] = 8;
			ZC_LUT_LH[STATE_H_R_R1 | i] = 8;
		}
		// - One horiz. significant, one or more vertical significant,
		// diagonal irrelevant
		for(i = 1; i < 4; i++) {
			for(j = 0; j < 16; j++) {
				ZC_LUT_LH[STATE_H_L_R1 | (i << 4) | j] = 9;
				ZC_LUT_LH[STATE_H_R_R1 | (i << 4) | j] = 9;
			}
		}
		// - Two horiz. significant, others irrelevant
		for(i = 0; i < 64; i++) {
			ZC_LUT_LH[STATE_H_L_R1 | STATE_H_R_R1 | i] = 10;
		}

		// HL

		// - No neighbors significant
		ZC_LUT_HL[0] = 2;
		// - No horizontal or vertical neighbors significant
		for(i = 1; i < 16; i++) { // Two or more diagonal coeffs significant
			ZC_LUT_HL[i] = 4;
		}
		for(i = 0; i < 4; i++) { // Only one diagonal coeff significant
			ZC_LUT_HL[1 << i] = 3;
		}
		// - No vertical significant, diagonal irrelevant
		for(i = 0; i < 16; i++) {
			// One horiz. significant
			ZC_LUT_HL[STATE_H_L_R1 | i] = 5;
			ZC_LUT_HL[STATE_H_R_R1 | i] = 5;
			// Two horiz. significant
			ZC_LUT_HL[STATE_H_L_R1 | STATE_H_R_R1 | i] = 6;
		}
		// - One vert. significant, diagonal/horizontal non-significant
		ZC_LUT_HL[STATE_V_U_R1] = 7;
		ZC_LUT_HL[STATE_V_D_R1] = 7;
		// - One vert. significant, horizontal non-significant, one or more
		// diag. significant
		for(i = 1; i < 16; i++) {
			ZC_LUT_HL[STATE_V_U_R1 | i] = 8;
			ZC_LUT_HL[STATE_V_D_R1 | i] = 8;
		}
		// - One vertical significant, one or more horizontal significant,
		// diagonal irrelevant
		for(i = 1; i < 4; i++) {
			for(j = 0; j < 16; j++) {
				ZC_LUT_HL[(i << 6) | STATE_V_U_R1 | j] = 9;
				ZC_LUT_HL[(i << 6) | STATE_V_D_R1 | j] = 9;
			}
		}
		for(i = 0; i < 4; i++) {
			for(j = 0; j < 16; j++) {
				ZC_LUT_HL[(i << 6) | STATE_V_U_R1 | STATE_V_D_R1 | j] = 10;
			}
		}
		int[] twoBits = {3, 5, 6, 9, 10, 12};

		int[] oneBit = {1, 2, 4, 8};
		int[] twoLeast = {3, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15}; // Figures
		// (between 0 and 15) countaining, at least, 2 bits on in its
		// binary representation.

		int[] threeLeast = {7, 11, 13, 14, 15}; // Figures
		// (between 0 and 15) countaining, at least, 3 bits on in its
		// binary representation.

		// - None significant
		ZC_LUT_HH[0] = 2;

		// - One horizontal+vertical significant, none diagonal
		for(i = 0; i < oneBit.length; i++) {
			ZC_LUT_HH[oneBit[i] << 4] = 3; // - Two or more horizontal+vertical significant, diagonal non-signif
		}
		for(i = 0; i < twoLeast.length; i++) {
			ZC_LUT_HH[twoLeast[i] << 4] = 4; // - One diagonal significant, horiz./vert. non-significant
		}
		for(i = 0; i < oneBit.length; i++) {
			ZC_LUT_HH[oneBit[i]] = 5; // - One diagonal significant, one horiz.+vert. significant
		}
		for(i = 0; i < oneBit.length; i++) {
			for(j = 0; j < oneBit.length; j++) {
				ZC_LUT_HH[(oneBit[i] << 4) | oneBit[j]] = 6; // - One diag signif, two or more horiz+vert signif
			}
		}
		for(i = 0; i < twoLeast.length; i++) {
			for(j = 0; j < oneBit.length; j++) {
				ZC_LUT_HH[(twoLeast[i] << 4) | oneBit[j]] = 7; // - Two diagonal significant, none horiz+vert significant
			}
		}
		for(i = 0; i < twoBits.length; i++) {
			ZC_LUT_HH[twoBits[i]] = 8; // - Two diagonal significant, one or more horiz+vert significant
		}
		for(j = 0; j < twoBits.length; j++) {
			for(i = 1; i < 16; i++) {
				ZC_LUT_HH[(i << 4) | twoBits[j]] = 9; // - Three or more diagonal significant, horiz+vert irrelevant
			}
		}
		for(i = 0; i < 16; i++) {
			for(j = 0; j < threeLeast.length; j++) {
				ZC_LUT_HH[(i << 4) | threeLeast[j]] = 10;
			}
		}
		inter_sc_lut = new int[36];
		inter_sc_lut[(2 << 3) | 2] = 15;
		inter_sc_lut[(2 << 3) | 1] = 14;
		inter_sc_lut[(2 << 3) | 0] = 13;
		inter_sc_lut[(1 << 3) | 2] = 12;
		inter_sc_lut[(1 << 3) | 1] = 11;
		inter_sc_lut[(1 << 3) | 0] = 12 | INT_SIGN_BIT;
		inter_sc_lut[(0 << 3) | 2] = 13 | INT_SIGN_BIT;
		inter_sc_lut[(0 << 3) | 1] = 14 | INT_SIGN_BIT;
		inter_sc_lut[(0 << 3) | 0] = 15 | INT_SIGN_BIT;

		for(i = 0; i < (1 << SC_LUT_BITS) - 1; i++) {
			ds = i & 0x01; // significance of down neighbor
			us = (i >> 1) & 0x01; // significance of up neighbor
			rs = (i >> 2) & 0x01; // significance of right neighbor
			ls = (i >> 3) & 0x01; // significance of left neighbor
			dsgn = (i >> 5) & 0x01; // sign of down neighbor
			usgn = (i >> 6) & 0x01; // sign of up neighbor
			rsgn = (i >> 7) & 0x01; // sign of right neighbor
			lsgn = (i >> 8) & 0x01; // sign of left neighbor
			// Calculate 'h' and 'v' as in VM text
			h = ls * (1 - 2 * lsgn) + rs * (1 - 2 * rsgn);
			h = (h >= -1) ? h : -1;
			h = (h <= 1) ? h : 1;
			v = us * (1 - 2 * usgn) + ds * (1 - 2 * dsgn);
			v = (v >= -1) ? v : -1;
			v = (v <= 1) ? v : 1;
			// Get context and sign predictor from 'inter_sc_lut'
			SC_LUT[i] = inter_sc_lut[(h + 1) << 3 | (v + 1)];
		}
		inter_sc_lut = null;

		// Initialize the MR lookup tables

		// None significant, prev MR off
		MR_LUT[0] = 16;
		// One or more significant, prev MR off
		for(i = 1; i < (1 << (MR_LUT_BITS - 1)); i++) {
			MR_LUT[i] = 17;
		}
		// Previous MR on, significance irrelevant
		for(; i < (1 << MR_LUT_BITS); i++) {
			MR_LUT[i] = 18;
		}
	}
	
    /** The current most probable signal for each context */
    private int[] mPS;
    /** The current index of each context */
    private int[] I;
    /** The current bit code */
    private int c;
    /** The bit code counter */
    private int cT;
    /** The current interval */
    private int a;
    /** The last byte read */
    private int b;
    /** Flag indicating if a marker has been found */
    private boolean markerFound;
    
	private int pos, count;
	private byte[] buffer;
	private int[] state;
	private int options;
	private int Blkw, Blkh, MsbSk;

	/*
	 *  init this entropy decoder with header values
	 *  we don't call this in the constructor in case we want to use this
	 *  decoder for a different file.
	 */
	public EntropyDecoder(byte COD_ecOptions, int[] COD_cblk) {
		options = COD_ecOptions;
		state = new int[(COD_cblk[0] + 2) * ((COD_cblk[1] + 1) / 2 + 2)];

		initMQ();
	}

	/*
	 *
	 *  entry point for running tier 1 decoding on code block
	 */
	static private int[] out_data = null;

	public int[] decode(int sbot, byte[] block, int sb_start, int sb_len, int[] trunc, int msbSk, int blkw, int blkh) {

		int zc_lut[]; // The ZC lookup table to use
		int npasses = 0; // The number of coding passes to perform
		int curbp; // The current magnitude bit-plane (starts at 30)
		boolean error = false; // Error indicator
		boolean isterm;

		if(out_data == null) {
			out_data = new int[blkw * blkh];
		}
		else {
			if(out_data.length < blkw * blkh) {
				out_data = null;
				out_data = new int[blkw * blkh];
			}
			else {
				Arrays.fill(out_data, 0);
			}
		}
		
		Blkw = blkw;
		Blkh = blkh;
		MsbSk = msbSk;
		// reset state array
		Arrays.fill(state, 0);

		for(int k = 0; k < trunc.length; k++) {
			npasses += trunc[k];
		}
		nextSegment(block, sb_start, sb_len);
		resetCtxts();

		// Choose correct ZC lookup table for global orientation
		switch(sbot) {
			case Subband.WT_ORIENT_HL:
				zc_lut = ZC_LUT_HL;
				break;
			case Subband.WT_ORIENT_LH:
			case Subband.WT_ORIENT_LL:
				zc_lut = ZC_LUT_LH;
				break;
			case Subband.WT_ORIENT_HH:
				zc_lut = ZC_LUT_HH;
				break;
			default:
				zc_lut = ZC_LUT_HL;
				break;
		}

		curbp = 30 - MsbSk;

		// First bit-plane has only the cleanup pass
		if(curbp >= 0 && npasses > 0) {
			isterm = (options & OPT_REG_TERM) != 0
					|| ((options & OPT_BYPASS) != 0 && (31 - NUM_NON_BYPASS_MS_BP - MsbSk) >= curbp);
			error = cleanuppass(out_data, curbp, state, zc_lut, isterm);
			npasses--;
			if(!error || !doer) {
				curbp--;
			}
		}

		// Other bit-planes have the three coding passes
		if(!error || !doer) {
			while(curbp >= 0 && npasses > 0) {
				// Do not use bypass decoding mode
				isterm = (options & OPT_REG_TERM) != 0;
				error = sigProgPass(out_data, curbp, state, zc_lut, isterm);
				npasses--;
				if(npasses <= 0 || (error && doer)) {
					break;
				}
				isterm = (options & OPT_REG_TERM) != 0
						|| ((options & OPT_BYPASS) != 0 && (31 - NUM_NON_BYPASS_MS_BP - MsbSk > curbp));
				error = magRefPass(out_data, curbp, state, isterm);
				npasses--;
				if(npasses <= 0 || (error && doer)) {
					break;
				}
				isterm = (options & OPT_REG_TERM) != 0
						|| ((options & OPT_BYPASS) != 0 && (31 - NUM_NON_BYPASS_MS_BP - MsbSk) >= curbp);
				error = cleanuppass(out_data, curbp, state, zc_lut, isterm);
				npasses--;
				if(error && doer) {
					break;
					// Goto next bit-plane
				}
				curbp--;
			}
		}
		return out_data;

	}

	private boolean cleanuppass(int[] data, int bp, int state[], int zc_lut[], boolean isterm) {
		int j, sj; // The state index for line and stripe
		int k, sk; // The data index for line and stripe
		int dscanw; // The data scan-width
		int sscanw; // The state scan-width
		int jstep; // Stripe to stripe step for 'sj'
		int kstep; // Stripe to stripe step for 'sk'
		int stopsk; // The loop limit on the variable sk
		int csj; // Local copy (i.e. cached) of 'state[j]'
		int setmask; // The mask to set current and lower bit-planes to 1/2
		// approximation
		int sym; // The decoded symbol
		int rlclen; // Length of RLC
		int ctxt; // The context to use
		int s; // The stripe index
		boolean causal; // Flag to indicate if stripe-causal context
		// formation is to be used
		int nstripes; // The number of stripes in the code-block
		int sheight; // Height of the current stripe
		int off_ul, off_ur, off_dr, off_dl; // offsets
		boolean error; // The error condition

		// Initialize local variables
		dscanw = Blkw;
		sscanw = Blkw + 2;
		jstep = sscanw * STRIPE_HEIGHT / 2 - Blkw;
		kstep = dscanw * STRIPE_HEIGHT - Blkw;
		setmask = (3 << bp) >> 1;
		nstripes = (Blkh + STRIPE_HEIGHT - 1) / STRIPE_HEIGHT;
		causal = (options & OPT_VERT_STR_CAUSAL) != 0;

		// Pre-calculate offsets in 'state' for diagonal neighbors
		off_ul = -sscanw - 1; // up-left
		off_ur = -sscanw + 1; // up-right
		off_dr = sscanw + 1; // down-right
		off_dl = sscanw - 1; // down-left

		// Decode stripe by stripe
		sk = 0;
		sj = sscanw + 1;
		for(s = nstripes - 1; s >= 0; s--, sk += kstep, sj += jstep) {
			sheight = (s != 0) ? STRIPE_HEIGHT : Blkh - (nstripes - 1) * STRIPE_HEIGHT;
			stopsk = sk + Blkw;
			// Scan by set of 1 stripe column at a time
			for(; sk < stopsk; sk++, sj++) {
				// Start column
				j = sj;
				csj = state[j];
				top_half: {
					// Check for RLC: if all samples are not significant, not
					// visited and do not have a non-zero context, and column is
					// full height, we do RLC.
					if(csj == 0 && state[j + sscanw] == 0 && sheight == STRIPE_HEIGHT) {
						if(decodeSymbol(RLC_CTXT) != 0) {
							// run-length is significant, decode length
							rlclen = decodeSymbol(UNIF_CTXT) << 1;
							rlclen |= decodeSymbol(UNIF_CTXT);
							// Set 'k' and 'j' accordingly
							k = sk + rlclen * dscanw;
							if(rlclen > 1) {
								j += sscanw;
								csj = state[j];
							}
						}
						else { // RLC is insignificant
							// Goto next column
							continue;
						}
						// We just decoded the length of a significant RLC
						// and a sample became significant
						// Use sign coding
						if((rlclen & 0x01) == 0) {
							// Sample that became significant is first row of
							// its column half
							ctxt = SC_LUT[(csj >> SC_SHIFT_R1) & SC_MASK];
							sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
							// Update the data
							data[k] = (sym << 31) | setmask;
							// Update state information (significant bit,
							// visited bit, neighbor significant bit of
							// neighbors, non zero context of neighbors, sign
							// of neighbors)
							if(rlclen != 0 || !causal) {
								// If in causal mode do not change
								// contexts of previous stripe.
								state[j + off_ul] |= STATE_NZ_CTXT_R2 | STATE_D_DR_R2;
								state[j + off_ur] |= STATE_NZ_CTXT_R2 | STATE_D_DL_R2;
							}
							// Update sign state information of neighbors
							if(sym != 0) {
								csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2
										| STATE_V_U_SIGN_R2;
								if(rlclen != 0 || !causal) {
									// If in causal mode do not change
									// contexts of previous stripe.
									state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2 | STATE_V_D_SIGN_R2;
								}
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_H_L_SIGN_R1
										| STATE_D_UL_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_H_R_SIGN_R1
										| STATE_D_UR_R2;
							}
							else {
								csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2;
								if(rlclen != 0 || !causal) {
									// If in causal mode do not change
									// contexts of previous stripe.
									state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2;
								}
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_D_UL_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_D_UR_R2;
							}
							// Changes to csj are saved later
							if((rlclen >> 1) != 0) {
								// Sample that became significant is in
								// bottom half of column => jump to bottom
								// half
								break top_half;
							}
							// Otherwise sample that became significant is in
							// top half of column => continue on top half
						}
						else {
							// Sample that became significant is second row of
							// its column half
							ctxt = SC_LUT[(csj >> SC_SHIFT_R2) & SC_MASK];
							sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
							// Update the data
							data[k] = (sym << 31) | setmask;
							// Update state information (significant bit,
							// neighbor significant bit of neighbors,
							// non zero context of neighbors, sign of neighbors)
							state[j + off_dl] |= STATE_NZ_CTXT_R1 | STATE_D_UR_R1;
							state[j + off_dr] |= STATE_NZ_CTXT_R1 | STATE_D_UL_R1;
							// Update sign state information of neighbors
							if(sym != 0) {
								csj |= STATE_SIG_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1 | STATE_V_D_SIGN_R1;
								state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1 | STATE_V_U_SIGN_R1;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2
										| STATE_H_L_SIGN_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2
										| STATE_H_R_SIGN_R2;
							}
							else {
								csj |= STATE_SIG_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1;
								state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2;
							}
							// Save changes to csj
							state[j] = csj;
							if((rlclen >> 1) != 0) {
								// Sample that became significant is in bottom
								// half of column => we're done with this
								// column
								continue;
							}
							// Otherwise sample that became significant is in
							// top half of column => we're done with top
							// column
							j += sscanw;
							csj = state[j];
							break top_half;
						}
					}
					// Do half top of column
					// If any of the two samples is not significant and has
					// not been visited in the current bit-plane we can not
					// skip them
					if((((csj >> 1) | csj) & VSTD_MASK_R1R2) != VSTD_MASK_R1R2) {
						k = sk;
						// Scan first row
						if((csj & (STATE_SIG_R1 | STATE_VISITED_R1)) == 0) {
							// Use zero coding
							if(decodeSymbol(zc_lut[csj & ZC_MASK]) != 0) {
								// Became significant
								// Use sign coding
								ctxt = SC_LUT[(csj >>> SC_SHIFT_R1) & SC_MASK];
								sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
								// Update the data
								data[k] = (sym << 31) | setmask;
								// Update state information (significant bit,
								// visited bit, neighbor significant bit of
								// neighbors, non zero context of neighbors,
								// sign of neighbors)
								if(!causal) {
									// If in causal mode do not change
									// contexts of previous stripe.
									state[j + off_ul] |= STATE_NZ_CTXT_R2 | STATE_D_DR_R2;
									state[j + off_ur] |= STATE_NZ_CTXT_R2 | STATE_D_DL_R2;
								}
								// Update sign state information of neighbors
								if(sym != 0) {
									csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2
											| STATE_V_U_SIGN_R2;
									if(!causal) {
										// If in causal mode do not change
										// contexts of previous stripe.
										state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2 | STATE_V_D_SIGN_R2;
									}
									state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1
											| STATE_H_L_SIGN_R1 | STATE_D_UL_R2;
									state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1
											| STATE_H_R_SIGN_R1 | STATE_D_UR_R2;
								}
								else {
									csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2;
									if(!causal) {
										// If in causal mode do not change
										// contexts of previous stripe.
										state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2;
									}
									state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_D_UL_R2;
									state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_D_UR_R2;
								}
							}
						}
						if(sheight < 2) {
							csj &= ~(STATE_VISITED_R1 | STATE_VISITED_R2);
							state[j] = csj;
							continue;
						}
						// Scan second row
						if((csj & (STATE_SIG_R2 | STATE_VISITED_R2)) == 0) {
							k += dscanw;
							// Use zero coding
							if(decodeSymbol(zc_lut[(csj >>> STATE_SEP) & ZC_MASK]) != 0) {
								// Became significant
								// Use sign coding
								ctxt = SC_LUT[(csj >>> SC_SHIFT_R2) & SC_MASK];
								sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
								// Update the data
								data[k] = (sym << 31) | setmask;
								// Update state information (significant bit,
								// visited bit, neighbor significant bit of
								// neighbors, non zero context of neighbors,
								// sign of neighbors)
								state[j + off_dl] |= STATE_NZ_CTXT_R1 | STATE_D_UR_R1;
								state[j + off_dr] |= STATE_NZ_CTXT_R1 | STATE_D_UL_R1;
								// Update sign state information of neighbors
								if(sym != 0) {
									csj |= STATE_SIG_R2 | STATE_VISITED_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1
											| STATE_V_D_SIGN_R1;
									state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1 | STATE_V_U_SIGN_R1;
									state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2
											| STATE_H_L_SIGN_R2;
									state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2
											| STATE_H_R_SIGN_R2;
								}
								else {
									csj |= STATE_SIG_R2 | STATE_VISITED_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1;
									state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1;
									state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2;
									state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2;
								}
							}
						}
					}
					csj &= ~(STATE_VISITED_R1 | STATE_VISITED_R2);
					state[j] = csj;
					// Do half bottom of column
					if(sheight < 3) {
						continue;
					}
					j += sscanw;
					csj = state[j];
				} // end of 'top_half' block
				// If any of the two samples is not significant and has
				// not been visited in the current bit-plane we can not
				// skip them
				if((((csj >> 1) | csj) & VSTD_MASK_R1R2) != VSTD_MASK_R1R2) {
					k = sk + (dscanw << 1);
					// Scan first row
					if((csj & (STATE_SIG_R1 | STATE_VISITED_R1)) == 0) {
						// Use zero coding
						if(decodeSymbol(zc_lut[csj & ZC_MASK]) != 0) {
							// Became significant
							// Use sign coding
							ctxt = SC_LUT[(csj >> SC_SHIFT_R1) & SC_MASK];
							sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
							// Update the data
							data[k] = (sym << 31) | setmask;
							// Update state information (significant bit,
							// visited bit, neighbor significant bit of
							// neighbors, non zero context of neighbors,
							// sign of neighbors)
							state[j + off_ul] |= STATE_NZ_CTXT_R2 | STATE_D_DR_R2;
							state[j + off_ur] |= STATE_NZ_CTXT_R2 | STATE_D_DL_R2;
							// Update sign state information of neighbors
							if(sym != 0) {
								csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2
										| STATE_V_U_SIGN_R2;
								state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2 | STATE_V_D_SIGN_R2;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_H_L_SIGN_R1
										| STATE_D_UL_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_H_R_SIGN_R1
										| STATE_D_UR_R2;
							}
							else {
								csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2;
								state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_D_UL_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_D_UR_R2;
							}
						}
					}
					if(sheight < 4) {
						csj &= ~(STATE_VISITED_R1 | STATE_VISITED_R2);
						state[j] = csj;
						continue;
					}
					// Scan second row
					if((csj & (STATE_SIG_R2 | STATE_VISITED_R2)) == 0) {
						k += dscanw;
						// Use zero coding
						if(decodeSymbol(zc_lut[(csj >>> STATE_SEP) & ZC_MASK]) != 0) {
							// Became significant
							// Use sign coding
							ctxt = SC_LUT[(csj >>> SC_SHIFT_R2) & SC_MASK];
							sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
							// Update the data
							data[k] = (sym << 31) | setmask;
							// Update state information (significant bit,
							// visited bit, neighbor significant bit of
							// neighbors, non zero context of neighbors,
							// sign of neighbors)
							state[j + off_dl] |= STATE_NZ_CTXT_R1 | STATE_D_UR_R1;
							state[j + off_dr] |= STATE_NZ_CTXT_R1 | STATE_D_UL_R1;
							// Update sign state information of neighbors
							if(sym != 0) {
								csj |= STATE_SIG_R2 | STATE_VISITED_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1
										| STATE_V_D_SIGN_R1;
								state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1 | STATE_V_U_SIGN_R1;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2
										| STATE_H_L_SIGN_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2
										| STATE_H_R_SIGN_R2;
							}
							else {
								csj |= STATE_SIG_R2 | STATE_VISITED_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1;
								state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2;
							}
						}
					}
				}
				csj &= ~(STATE_VISITED_R1 | STATE_VISITED_R2);
				state[j] = csj;
			}
		}

		// Decode segment marker if we need to
		if((options & OPT_SEG_MARKERS) != 0) {
			sym = decodeSymbol(UNIF_CTXT) << 3 | decodeSymbol(UNIF_CTXT) << 2 | decodeSymbol(UNIF_CTXT) << 1
					| decodeSymbol(UNIF_CTXT);
			// Set error condition accordingly
			error = sym != SEG_MARKER;
		}
		else { // We can not detect any errors
			error = false;
		}

		// Check the error resilient termination
		if(isterm && (options & OPT_ER_TERM) != 0) {
			error = checkPredTerm();
		}

		// Reset the MQ context states if we need to
		if((options & OPT_RESET_MQ) != 0) {
			resetCtxts();
		}

		// Return error condition
		return error;
	}

	/**
	 * Performs the significance propagation pass on the specified data and
	 * bit-plane. It decodes all insignificant samples which have, at least,
	 * one of its immediate eight neighbors already significant, using the ZC
	 * and SC primitives as needed. It toggles the "visited" state bit to 1
	 * for all those samples.
	 *
	 * <P>This method also checks for segmentation markers if those are
	 * present and returns true if an error is detected, or false
	 * otherwise. If an error is detected it means that the bit stream contains
	 * some erroneous bit that have led to the decoding of incorrect
	 * data. This data affects the whole last decoded bit-plane (i.e. 'bp'). If
	 * 'true' is returned the 'conceal' method should be called and no more
	 * passes should be decoded for this code-block's bit stream.
	 *
	 * @param cblk The code-block data to decode
	 *
	 * @param mq The MQ-coder to use
	 *
	 * @param bp The bit-plane to decode
	 *
	 * @param state The state information for the code-block
	 *
	 * @param zc_lut The ZC lookup table to use in ZC.
	 *
	 * @param isterm If this pass has been terminated. If the pass has been
	 * terminated it can be used to check error resilience.
	 *
	 * @return True if an error was detected in the bit stream, false otherwise.
	 * */
	private boolean sigProgPass(int data[], int bp, int state[], int zc_lut[], boolean isterm) {
		int j, sj; // The state index for line and stripe
		int k, sk; // The data index for line and stripe
		int dscanw; // The data scan-width
		int sscanw; // The state scan-width
		int jstep; // Stripe to stripe step for 'sj'
		int kstep; // Stripe to stripe step for 'sk'
		int stopsk; // The loop limit on the variable sk
		int csj; // Local copy (i.e. cached) of 'state[j]'
		int setmask; // The mask to set current and lower bit-planes to 1/2
		// approximation
		int sym; // The symbol to code
		int ctxt; // The context to use
		int s; // The stripe index
		boolean causal; // Flag to indicate if stripe-causal context
		// formation is to be used
		int nstripes; // The number of stripes in the code-block
		int sheight; // Height of the current stripe
		int off_ul, off_ur, off_dr, off_dl; // offsets
		boolean error; // The error condition

		// Initialize local variables
		dscanw = Blkw;
		sscanw = Blkw + 2;
		jstep = sscanw * STRIPE_HEIGHT / 2 - Blkw;
		kstep = dscanw * STRIPE_HEIGHT - Blkw;
		setmask = (3 << bp) >> 1;
		nstripes = (Blkh + STRIPE_HEIGHT - 1) / STRIPE_HEIGHT;
		causal = (options & OPT_VERT_STR_CAUSAL) != 0;

		// Pre-calculate offsets in 'state' for diagonal neighbors
		off_ul = -sscanw - 1; // up-left
		off_ur = -sscanw + 1; // up-right
		off_dr = sscanw + 1; // down-right
		off_dl = sscanw - 1; // down-left

		// Decode stripe by stripe
		sk = 0;
		sj = sscanw + 1;
		for(s = nstripes - 1; s >= 0; s--, sk += kstep, sj += jstep) {
			sheight = (s != 0) ? STRIPE_HEIGHT : Blkh - (nstripes - 1) * STRIPE_HEIGHT;
			stopsk = sk + Blkw;
			// Scan by set of 1 stripe column at a time
			for(; sk < stopsk; sk++, sj++) {
				// Do half top of column
				j = sj;
				csj = state[j];
				// If any of the two samples is not significant and has a
				// non-zero context (i.e. some neighbor is significant) we can
				// not skip them
				if((((~csj) & (csj << 2)) & SIG_MASK_R1R2) != 0) {
					k = sk;
					// Scan first row
					if((csj & (STATE_SIG_R1 | STATE_NZ_CTXT_R1)) == STATE_NZ_CTXT_R1) {
						// Use zero coding
						if(decodeSymbol(zc_lut[csj & ZC_MASK]) != 0) {
							// Became significant
							// Use sign coding
							ctxt = SC_LUT[(csj >>> SC_SHIFT_R1) & SC_MASK];
							sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
							// Update data
							data[k] = (sym << 31) | setmask;
							// Update state information (significant bit,
							// visited bit, neighbor significant bit of
							// neighbors, non zero context of neighbors, sign
							// of neighbors)
							if(!causal) {
								// If in causal mode do not change contexts of
								// previous stripe.
								state[j + off_ul] |= STATE_NZ_CTXT_R2 | STATE_D_DR_R2;
								state[j + off_ur] |= STATE_NZ_CTXT_R2 | STATE_D_DL_R2;
							}
							// Update sign state information of neighbors
							if(sym != 0) {
								csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2
										| STATE_V_U_SIGN_R2;
								if(!causal) {
									// If in causal mode do not change
									// contexts of previous stripe.
									state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2 | STATE_V_D_SIGN_R2;
								}
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_H_L_SIGN_R1
										| STATE_D_UL_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_H_R_SIGN_R1
										| STATE_D_UR_R2;
							}
							else {
								csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2;
								if(!causal) {
									// If in causal mode do not change
									// contexts of previous stripe.
									state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2;
								}
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_D_UL_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_D_UR_R2;
							}
						}
						else {
							csj |= STATE_VISITED_R1;
						}
					}
					if(sheight < 2) {
						state[j] = csj;
						continue;
					}
					// Scan second row
					if((csj & (STATE_SIG_R2 | STATE_NZ_CTXT_R2)) == STATE_NZ_CTXT_R2) {
						k += dscanw;
						// Use zero coding
						if(decodeSymbol(zc_lut[(csj >>> STATE_SEP) & ZC_MASK]) != 0) {
							// Became significant
							// Use sign coding
							ctxt = SC_LUT[(csj >>> SC_SHIFT_R2) & SC_MASK];
							sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
							// Update data
							data[k] = (sym << 31) | setmask;
							// Update state information (significant bit,
							// visited bit, neighbor significant bit of
							// neighbors, non zero context of neighbors, sign
							// of neighbors)
							state[j + off_dl] |= STATE_NZ_CTXT_R1 | STATE_D_UR_R1;
							state[j + off_dr] |= STATE_NZ_CTXT_R1 | STATE_D_UL_R1;
							// Update sign state information of neighbors
							if(sym != 0) {
								csj |= STATE_SIG_R2 | STATE_VISITED_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1
										| STATE_V_D_SIGN_R1;
								state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1 | STATE_V_U_SIGN_R1;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2
										| STATE_H_L_SIGN_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2
										| STATE_H_R_SIGN_R2;
							}
							else {
								csj |= STATE_SIG_R2 | STATE_VISITED_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1;
								state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2;
							}
						}
						else {
							csj |= STATE_VISITED_R2;
						}
					}
					state[j] = csj;
				}
				// Do half bottom of column
				if(sheight < 3) {
					continue;
				}
				j += sscanw;
				csj = state[j];
				// If any of the two samples is not significant and has a
				// non-zero context (i.e. some neighbor is significant) we can
				// not skip them
				if((((~csj) & (csj << 2)) & SIG_MASK_R1R2) != 0) {
					k = sk + (dscanw << 1);
					// Scan first row
					if((csj & (STATE_SIG_R1 | STATE_NZ_CTXT_R1)) == STATE_NZ_CTXT_R1) {
						// Use zero coding
						if(decodeSymbol(zc_lut[csj & ZC_MASK]) != 0) {
							// Became significant
							// Use sign coding
							ctxt = SC_LUT[(csj >>> SC_SHIFT_R1) & SC_MASK];
							sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
							// Update data
							data[k] = (sym << 31) | setmask;
							// Update state information (significant bit,
							// visited bit, neighbor significant bit of
							// neighbors, non zero context of neighbors, sign
							// of neighbors)
							state[j + off_ul] |= STATE_NZ_CTXT_R2 | STATE_D_DR_R2;
							state[j + off_ur] |= STATE_NZ_CTXT_R2 | STATE_D_DL_R2;
							// Update sign state information of neighbors
							if(sym != 0) {
								csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2
										| STATE_V_U_SIGN_R2;
								state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2 | STATE_V_D_SIGN_R2;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_H_L_SIGN_R1
										| STATE_D_UL_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_H_R_SIGN_R1
										| STATE_D_UR_R2;
							}
							else {
								csj |= STATE_SIG_R1 | STATE_VISITED_R1 | STATE_NZ_CTXT_R2 | STATE_V_U_R2;
								state[j - sscanw] |= STATE_NZ_CTXT_R2 | STATE_V_D_R2;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_L_R1 | STATE_D_UL_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_H_R_R1 | STATE_D_UR_R2;
							}
						}
						else {
							csj |= STATE_VISITED_R1;
						}
					}
					if(sheight < 4) {
						state[j] = csj;
						continue;
					}
					// Scan second row
					if((csj & (STATE_SIG_R2 | STATE_NZ_CTXT_R2)) == STATE_NZ_CTXT_R2) {
						k += dscanw;
						// Use zero coding
						if(decodeSymbol(zc_lut[(csj >>> STATE_SEP) & ZC_MASK]) != 0) {
							// Became significant
							// Use sign coding
							ctxt = SC_LUT[(csj >>> SC_SHIFT_R2) & SC_MASK];
							sym = decodeSymbol(ctxt & SC_LUT_MASK) ^ (ctxt >>> SC_SPRED_SHIFT);
							// Update data
							data[k] = (sym << 31) | setmask;
							// Update state information (significant bit,
							// visited bit, neighbor significant bit of
							// neighbors, non zero context of neighbors, sign
							// of neighbors)
							state[j + off_dl] |= STATE_NZ_CTXT_R1 | STATE_D_UR_R1;
							state[j + off_dr] |= STATE_NZ_CTXT_R1 | STATE_D_UL_R1;
							// Update sign state information of neighbors
							if(sym != 0) {
								csj |= STATE_SIG_R2 | STATE_VISITED_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1
										| STATE_V_D_SIGN_R1;
								state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1 | STATE_V_U_SIGN_R1;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2
										| STATE_H_L_SIGN_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2
										| STATE_H_R_SIGN_R2;
							}
							else {
								csj |= STATE_SIG_R2 | STATE_VISITED_R2 | STATE_NZ_CTXT_R1 | STATE_V_D_R1;
								state[j + sscanw] |= STATE_NZ_CTXT_R1 | STATE_V_U_R1;
								state[j + 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DL_R1 | STATE_H_L_R2;
								state[j - 1] |= STATE_NZ_CTXT_R1 | STATE_NZ_CTXT_R2 | STATE_D_DR_R1 | STATE_H_R_R2;
							}
						}
						else {
							csj |= STATE_VISITED_R2;
						}
					}
					state[j] = csj;
				}
			}
		}

		error = false;

		// Check the error resilient termination
		if(isterm && (options & OPT_ER_TERM) != 0) {
			error = checkPredTerm();
		}

		// Reset the MQ context states if we need to
		if((options & OPT_RESET_MQ) != 0) {
			resetCtxts();
		}

		// Return error condition
		return error;
	}

	/**
	 * Performs the magnitude refinement pass on the specified data and
	 * bit-plane. It decodes the samples which are significant and which do not
	 * have the "visited" state bit turned on, using the MR primitive. The
	 * "visited" state bit is not mofified for any samples.
	 *
	 * <P>This method also checks for segmentation markers if those are
	 * present and returns true if an error is detected, or false
	 * otherwise. If an error is detected it means that the bit stream contains
	 * some erroneous bit that have led to the decoding of incorrect
	 * data. This data affects the whole last decoded bit-plane (i.e. 'bp'). If
	 * 'true' is returned the 'conceal' method should be called and no more
	 * passes should be decoded for this code-block's bit stream.
	 *
	 * @param cblk The code-block data to decode
	 *
	 * @param mq The MQ-decoder to use
	 *
	 * @param bp The bit-plane to decode
	 *
	 * @param state The state information for the code-block
	 *
	 * @param isterm If this pass has been terminated. If the pass has been
	 * terminated it can be used to check error resilience.
	 *
	 * @return True if an error was detected in the bit stream, false otherwise.
	 * */
	private boolean magRefPass(int data[], int bp, int state[], boolean isterm) {
		int j, sj; // The state index for line and stripe
		int k, sk; // The data index for line and stripe
		int dscanw; // The data scan-width
		int sscanw; // The state scan-width
		int jstep; // Stripe to stripe step for 'sj'
		int kstep; // Stripe to stripe step for 'sk'
		int stopsk; // The loop limit on the variable sk
		int csj; // Local copy (i.e. cached) of 'state[j]'
		int setmask; // The mask to set lower bit-planes to 1/2 approximation
		int resetmask; // The mask to reset approximation bit-planes
		int sym; // The symbol to decode
		int s; // The stripe index
		int nstripes; // The number of stripes in the code-block
		int sheight; // Height of the current stripe
		boolean error; // The error condition

		// Initialize local variables
		dscanw = Blkw;
		sscanw = Blkw + 2;
		jstep = sscanw * STRIPE_HEIGHT / 2 - Blkw;
		kstep = dscanw * STRIPE_HEIGHT - Blkw;
		setmask = (1 << bp) >> 1;
		resetmask = (-1) << (bp + 1);
		nstripes = (Blkh + STRIPE_HEIGHT - 1) / STRIPE_HEIGHT;

		// Decode stripe by stripe
		sk = 0;
		sj = sscanw + 1;
		for(s = nstripes - 1; s >= 0; s--, sk += kstep, sj += jstep) {
			sheight = (s != 0) ? STRIPE_HEIGHT : Blkh - (nstripes - 1) * STRIPE_HEIGHT;
			stopsk = sk + Blkw;
			// Scan by set of 1 stripe column at a time
			for(; sk < stopsk; sk++, sj++) {
				// Do half top of column
				j = sj;
				csj = state[j];
				// If any of the two samples is significant and not yet
				// visited in the current bit-plane we can not skip them
				if((((csj >>> 1) & (~csj)) & VSTD_MASK_R1R2) != 0) {
					k = sk;
					// Scan first row
					if((csj & (STATE_SIG_R1 | STATE_VISITED_R1)) == STATE_SIG_R1) {
						// Use MR primitive
						sym = decodeSymbol(MR_LUT[csj & MR_MASK]);
						// Update the data
						data[k] &= resetmask;
						data[k] |= (sym << bp) | setmask;
						// Update the STATE_PREV_MR bit
						csj |= STATE_PREV_MR_R1;
					}
					if(sheight < 2) {
						state[j] = csj;
						continue;
					}
					// Scan second row
					if((csj & (STATE_SIG_R2 | STATE_VISITED_R2)) == STATE_SIG_R2) {
						k += dscanw;
						// Use MR primitive
						sym = decodeSymbol(MR_LUT[(csj >>> STATE_SEP) & MR_MASK]);
						// Update the data
						data[k] &= resetmask;
						data[k] |= (sym << bp) | setmask;
						// Update the STATE_PREV_MR bit
						csj |= STATE_PREV_MR_R2;
					}
					state[j] = csj;
				}
				// Do half bottom of column
				if(sheight < 3) {
					continue;
				}
				j += sscanw;
				csj = state[j];
				// If any of the two samples is significant and not yet
				// visited in the current bit-plane we can not skip them
				if((((csj >>> 1) & (~csj)) & VSTD_MASK_R1R2) != 0) {
					k = sk + (dscanw << 1);
					// Scan first row
					if((csj & (STATE_SIG_R1 | STATE_VISITED_R1)) == STATE_SIG_R1) {
						// Use MR primitive
						sym = decodeSymbol(MR_LUT[csj & MR_MASK]);
						// Update the data
						data[k] &= resetmask;
						data[k] |= (sym << bp) | setmask;
						// Update the STATE_PREV_MR bit
						csj |= STATE_PREV_MR_R1;
					}
					if(sheight < 4) {
						state[j] = csj;
						continue;
					}
					// Scan second row
					if((state[j] & (STATE_SIG_R2 | STATE_VISITED_R2)) == STATE_SIG_R2) {
						k += dscanw;
						// Use MR primitive
						sym = decodeSymbol(MR_LUT[(csj >>> STATE_SEP) & MR_MASK]);
						// Update the data
						data[k] &= resetmask;
						data[k] |= (sym << bp) | setmask;
						// Update the STATE_PREV_MR bit
						csj |= STATE_PREV_MR_R2;
					}
					state[j] = csj;
				}
			}
		}

		error = false;

		// Check the error resilient termination
		if(isterm && (options & OPT_ER_TERM) != 0) {
			error = checkPredTerm();
		}

		// Reset the MQ context states if we need to
		if((options & OPT_RESET_MQ) != 0) {
			resetCtxts();
		}

		// Return error condition
		return error;
	}

	private final int decodeSymbol(int context) {
		int q;
		int la;
		int index;
		int decision;

		index = I[context];
		q = qe[index];

		// NOTE: (a < 0x8000) is equivalent to ((a & 0x8000)==0)
		// since 'a' is always less than or equal to 0xFFFF

		// NOTE: conditional exchange guarantees that A for MPS is
		// always greater than 0x4000 (i.e. 0.375)
		// => one renormalization shift is enough for MPS
		// => no need to do a renormalization while loop for MPS

		a -= q;
		if((c >>> 16) < a) {
			if(a >= 0x8000) {
				decision = mPS[context];
			}
			else {
				la = a;
				// -- MPS Exchange
				if(la >= q) {
					decision = mPS[context];
					I[context] = nMPS[index];
					// -- Renormalize (MPS: no need for while loop)
					if(cT == 0) {
						byteIn();
					}
					la <<= 1;
					c <<= 1;
					cT--;
					// -- End renormalization
				}
				else {
					decision = 1 - mPS[context];
					if(switchLM[index] == 1) {
						mPS[context] = 1 - mPS[context];
					}
					I[context] = nLPS[index];
					// -- Renormalize
					do {
						if(cT == 0) {
							byteIn();
						}
						la <<= 1;
						c <<= 1;
						cT--;
					}
					while(la < 0x8000);
					// -- End renormalization
				}
				// -- End MPS Exchange
				a = la;
			}
		}
		else {
			la = a;
			c -= (la << 16);
			// -- LPS Exchange
			if(la < q) {
				la = q;
				decision = mPS[context];
				I[context] = nMPS[index];
				// -- Renormalize (MPS: no need for while loop)
				if(cT == 0) {
					byteIn();
				}
				la <<= 1;
				c <<= 1;
				cT--;
				// -- End renormalization
			}
			else {
				la = q;
				decision = 1 - mPS[context];
				if(switchLM[index] == 1) {
					mPS[context] = 1 - mPS[context];
				}
				I[context] = nLPS[index];
				// -- Renormalize
				do {
					if(cT == 0) {
						byteIn();
					}
					la <<= 1;
					c <<= 1;
					cT--;
				}
				while(la < 0x8000);
				// -- End renormalization
			}
			// -- End LPS Exchange

			a = la;
		}
		return decision;
	}

	private boolean checkPredTerm() {
		int k; // Number of bits that where added in the termination process
		int q;
		if(b != 0xFF && !markerFound) {
			return true;
		}
		if(cT != 0 && !markerFound) {
			return true;
		}
		if(cT == 1) {
			return false;
		}
		if(cT == 0) {
			if(!markerFound) {
				// Get next byte and check
				b = readByte() & 0xFF;
				if(b <= 0x8F) {
					return true;
				}
			}
			// Adjust cT for last byte
			cT = 8;
		}
		k = cT - 1;
		q = 0x8000 >> k;
		a -= q;
		if((c >>> 16) < a) {
			// Error: MPS interval decoded
			return true;
		}
		c -= (a << 16);
		a = q;
		// -- Renormalize
		do {
			if(cT == 0) {
				byteIn();
			}
			a <<= 1;
			c <<= 1;
			cT--;
		}
		while(a < 0x8000);
		// -- End renormalization
		// -- End LPS Exchange

		// 7) Everything seems OK, we have checked the C register for the LPS
		// symbols and ensured that it is followed by bits synthetized by the
		// termination marker.
		return false;
	}

	private void byteIn() {
		if(!markerFound) {
			if(b == 0xFF) {
				b = readByte() & 0xFF; // Convert EOFs (-1) to 0xFF

				if(b > 0x8F) {
					markerFound = true;
					// software-convention decoder: c unchanged
					cT = 8;
				}
				else {
					c += 0xFE00 - (b << 9);
					cT = 7;
				}
			}
			else {
				b = readByte() & 0xFF; // Convert EOFs (-1) to 0xFF
				c += 0xFF00 - (b << 8);
				cT = 8;
			}
		}
		else {
			// software-convention decoder: c unchanged
			cT = 8;
		}
	}

	private final void resetCtxts() {
		System.arraycopy(initStates, 0, I, 0, I.length);
		Arrays.fill(mPS, 0);
	}

	private final void nextSegment(byte buf[], int off, int len) {
		// Set the new input
		buffer = buf;
		pos = off;
		count = off + len;
		// Reinitialize MQ
		resetMQ();
	}

	private void resetMQ() {
		// --- INITDEC
		markerFound = false;

		// Read first byte
		b = readByte() & 0xFF;

		// Software conventions decoder
		c = (b ^ 0xFF) << 16;
		byteIn();
		c = c << 7;
		cT = cT - 7;
		a = 0x8000;
		// End of INITDEC ---
	}

	private void initMQ() {
		// Default initialization of the statistics bins is MPS=0 and I=0
		I = new int[NUM_CTXTS];
		mPS = new int[NUM_CTXTS];
		// Save the initial states
		resetMQ();

		resetCtxts();
	}

	private int readByte() {
		if(pos < count) {
			return (int)buffer[pos++] & 0xFF;
		}
		else {
			return -1;
		}
	}
}
