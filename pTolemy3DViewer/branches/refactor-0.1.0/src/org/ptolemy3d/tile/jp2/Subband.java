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

class Subband
{
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
	public int tilex;  // tile index that this subband belongs to.
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
	public Subband(int w, int h, int ulcx, int ulcy, int lvls, int tx, int cbw, int cbh)
	{
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
		for (int i = 0; i < lvls; i++) {
			cur = cur.split();
		}
	}

	protected Subband split()
	{
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

	public Subband traverse(int numlvl)
	{
		Subband s = this;
		for (int i = 0; i < numlvl; i++) {
			s = s.subb_LL;
		}
		return s;
	}
}