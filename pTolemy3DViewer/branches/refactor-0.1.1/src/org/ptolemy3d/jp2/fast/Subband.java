/*
 * CVS identifier:
 *
 * $Id: Subband.java,v 1.47 2001/10/18 14:27:14 grosbois Exp $
 *
 * Class:                   Subband
 *
 * Description:             Asbtract element for a tree strcuture for
 *                          a description of subbands.
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

class Subband {
	public final static int WT_ORIENT_LL = 0;
	public final static int WT_ORIENT_HL = 1;
	public final static int WT_ORIENT_LH = 2;
	public final static int WT_ORIENT_HH = 3;

	public boolean isNode;
	public int orientation;
	public int gOrient;
	public int resLvl;
	public int level;
	public int anGainExp;
	public int sbandIdx = 0;
	public int ulcx;
	public int ulcy;
	public int ulx;
	public int uly;
	public int w;
	public int h;
	public int nomCBlkW;
	public int nomCBlkH;
	public int tilex; // tile index that this subband belongs to.
	public int magbits = 0;
	public Subband parent;
	public Subband subb_LL;
	public Subband subb_LH;
	public Subband subb_HL;
	public Subband subb_HH;

	public Subband() {}

	/**
	 *
	 * @param w The top-level width
	 *
	 * @param h The top-level height
	 *
	 * @param ulcx The horizontal coordinate of the upper-left corner with
	 * respect to the canvas origin, in the component grid.
	 *
	 * @param ulcy The vertical  coordinate of the upper-left corner with
	 * respect to the canvas origin, in the component grid.
	 *
	 * @param lvls The number of levels (or LL decompositions) in the
	 * tree.
	 *
	 * @param hfilters The horizontal wavelet synthesis filters for each
	 * resolution level, starting at resolution level 0.
	 *
	 * @param vfilters The vertical wavelet synthesis filters for each
	 * resolution level, starting at resolution level 0.
	 *
	 * @see Subband#Subband(int, int, int, int, int, int, int, int)
	 * */
	public Subband(int w, int h, int ulcx, int ulcy, int lvls, int tx, int cbw, int cbh) {
		Subband cur;
		this.w = w;
		this.h = h;
		this.ulcx = ulcx;
		this.ulcy = ulcy;
		this.resLvl = lvls;
		this.tilex = tx;
		this.nomCBlkW = cbw;
		this.nomCBlkH = cbh;

		cur = this;
		for(int i = 0; i < lvls; i++) {
			cur = cur.split();
		}
	}

	protected Subband split() {
		// Modify this element into a node and set the filters
		isNode = true;

		// Create childs
		subb_LL = new Subband();
		subb_LH = new Subband();
		subb_HL = new Subband();
		subb_HH = new Subband();

		// Assign parent
		subb_LL.parent = this;
		subb_HL.parent = this;
		subb_LH.parent = this;
		subb_HH.parent = this;

		// Initialize childs
		// LL subband
		subb_LL.nomCBlkW = this.nomCBlkW;
		subb_LL.nomCBlkH = this.nomCBlkH;
		subb_LL.level = level + 1;
		subb_LL.ulcx = (ulcx + 1) >> 1;
		subb_LL.ulcy = (ulcy + 1) >> 1;
		subb_LL.ulx = ulx;
		subb_LL.uly = uly;
		subb_LL.w = ((ulcx + w + 1) >> 1) - subb_LL.ulcx;
		subb_LL.h = ((ulcy + h + 1) >> 1) - subb_LL.ulcy;
		subb_LL.gOrient = gOrient;
		// If this subband in in the all LL path (i.e. it's global orientation
		// is LL) then child LL band contributes to a lower resolution level.
		subb_LL.resLvl = (gOrient == WT_ORIENT_LL) ? resLvl - 1 : resLvl;
		subb_LL.anGainExp = anGainExp;
		subb_LL.sbandIdx = (sbandIdx << 2);
		// HL subband
		subb_HL.nomCBlkW = this.nomCBlkW;
		subb_HL.nomCBlkH = this.nomCBlkH;
		subb_HL.orientation = WT_ORIENT_HL;
		subb_HL.gOrient = (gOrient == WT_ORIENT_LL) ? WT_ORIENT_HL : gOrient;
		subb_HL.level = subb_LL.level;
		subb_HL.ulcx = ulcx >> 1;
		subb_HL.ulcy = subb_LL.ulcy;
		subb_HL.ulx = ulx + subb_LL.w;
		subb_HL.uly = uly;
		subb_HL.w = ((ulcx + w) >> 1) - subb_HL.ulcx;
		subb_HL.h = subb_LL.h;
		subb_HL.resLvl = resLvl;
		subb_HL.anGainExp = anGainExp + 1;
		subb_HL.sbandIdx = (sbandIdx << 2) + 1;
		// LH subband
		subb_LH.nomCBlkW = this.nomCBlkW;
		subb_LH.nomCBlkH = this.nomCBlkH;
		subb_LH.orientation = WT_ORIENT_LH;
		subb_LH.gOrient = (gOrient == WT_ORIENT_LL) ? WT_ORIENT_LH : gOrient;
		subb_LH.level = subb_LL.level;
		subb_LH.ulcx = subb_LL.ulcx;
		subb_LH.ulcy = ulcy >> 1;
		subb_LH.ulx = ulx;
		subb_LH.uly = uly + subb_LL.h;
		subb_LH.w = subb_LL.w;
		subb_LH.h = ((ulcy + h) >> 1) - subb_LH.ulcy;
		subb_LH.resLvl = resLvl;
		subb_LH.anGainExp = anGainExp + 1;
		subb_LH.sbandIdx = (sbandIdx << 2) + 2;
		// HH subband
		subb_HH.nomCBlkW = this.nomCBlkW;
		subb_HH.nomCBlkH = this.nomCBlkH;
		subb_HH.orientation = WT_ORIENT_HH;
		subb_HH.gOrient = (gOrient == WT_ORIENT_LL) ? WT_ORIENT_HH : gOrient;
		subb_HH.level = subb_LL.level;
		subb_HH.ulcx = subb_HL.ulcx;
		subb_HH.ulcy = subb_LH.ulcy;
		subb_HH.ulx = subb_HL.ulx;
		subb_HH.uly = subb_LH.uly;
		subb_HH.w = subb_HL.w;
		subb_HH.h = subb_LH.h;
		subb_HH.resLvl = resLvl;
		subb_HH.anGainExp = anGainExp + 2;
		subb_HH.sbandIdx = (sbandIdx << 2) + 3;
		// Return reference to LL subband
		return subb_LL;
	}

	public Subband traverse(int numlvl) {
		Subband s = this;
		for(int i = 0; i < numlvl; i++) {
			s = s.subb_LL;
		}
		return s;
	}
}