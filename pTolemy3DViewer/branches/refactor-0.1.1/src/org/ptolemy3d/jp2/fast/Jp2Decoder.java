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
package org.ptolemy3d.jp2.fast;

import java.io.IOException;
import java.util.Arrays;

/**
 * JP2 Decoder
 */
class Jp2Decoder {
	private final Jp2Parser parser;
	private final EntropyDecoder entropyDec;

	public Jp2Decoder(Jp2Parser jp2Parser) {
		this.parser = jp2Parser;
		this.entropyDec = new EntropyDecoder(parser.header.COD_ecOptions, parser.header.COD_cblk);
	}

	/** Decode data for next resolution */
	public byte[] getNextResolution(int res, byte[] prevPixels) throws IOException {
		//Allocate new texture
		final int width = parser.getWidth(res);
		final int height = parser.getHeight(res);
		
		//Get previous wavelet data
		final short[][] waveletData = new short[parser.getNumChannels()][width * height];
		if(res != 0) {
			parser.textureToNextWaveletRef(waveletData, prevPixels, width, parser.getWidth(res-1), parser.getHeight(res-1));
		}
		
		//Decoding
		byte[] streamBuffer = parser.getWaveletData(res);
		for(int c = 0; c < parser.getNumChannels(); c++) {
			getDataAtRes(streamBuffer, res, c, waveletData[c]);
		}
		
		//Convert to texture
		final byte[] pixels = new byte[parser.getNumChannels() * width * height];
		parser.waveletToTexture(pixels, waveletData, width, height);
		return pixels;
	}

	/**
	 *  reslvl - resolution level
	 *  c- component
	 *
	 *  at resolution 0, there is no wavelet reconstruction, just get LL band.
	 *  greater than 0 res gets the HL , LH,HH bands and performs 2d wavelet recons.
	 *  with LL gotten band
	 */
	void getDataAtRes(byte[] streamBuffer, int reslvl, int c, short[] data) {
		Subband node = parser.header.subband;
		while(node.resLvl != reslvl) {
			node = node.subb_LL;
		}

		if(reslvl == 0) {
			Arrays.fill(data, (short)0);
			
			getSubbandData(streamBuffer, node.w, node, c, data);
		}
		else {
			getSubbandData(streamBuffer, node.w, node.subb_LH, c, data);
			getSubbandData(streamBuffer, node.w, node.subb_HL, c, data);
			getSubbandData(streamBuffer, node.w, node.subb_HH, c, data);

			wavelet2Dreconstruction(node, data);
		}
	}

	// get all code block data for this subband and store it in imgData
	protected void getSubbandData(byte[] streamBuffer, int NomWidth, Subband sb, int c, short[] data) {
		int s = sb.sbandIdx;
		int r = sb.resLvl;
		int blkw, blkh, bulx, buly;
		// data arrays are set up: tile | component | resolution | (subband index) | y | x | layer
		int[][][] off = parser.header.CB_off[c][r][s];
		short[][][] len = parser.header.CB_len[c][r][s];
		boolean[][] inc = parser.header.cbInc[c][r][s];
		
		int cn = (sb.ulcx + sb.nomCBlkW) / sb.nomCBlkW - 1;
		int cm = (sb.ulcy + sb.nomCBlkH) / sb.nomCBlkH - 1;

		// set magBits
		// mag bits for this tile and comp?
		int gb = 0, exp = 0;
		if(parser.header.QCC_ValSet[1][c]) {
			gb = parser.header.QCC_guardBits[1][c];
			exp = parser.header.QCC_exp[1][c][sb.resLvl][sb.sbandIdx];
		}
		else if(parser.header.QCC_ValSet[0][c]) {
			gb = parser.header.QCC_guardBits[0][c];
			exp = parser.header.QCC_exp[0][c][sb.resLvl][sb.sbandIdx];
		}
		else { // use defaults
			gb = parser.header.QCD_guardBits;
			exp = parser.header.QCD_exp[sb.resLvl][sb.sbandIdx];
		}
		sb.magbits = gb + exp - 1;
		int sb_start, sb_len;
		//loop through the t,c,r,s and get all of the code blocks
		for(int m = 0; m < inc.length; m++) { // vertical loop
			buly = getBlockUly(sb, m, cm);
			blkh = getBlockHt(sb, m, cm, buly, inc.length);
			for(int n = 0; n < inc[0].length; n++) { // horizontal loop
				if(inc[m][n]) {
					bulx = getBlockUlx(sb, n, cn);
					blkw = getBlockWt(sb, n, cn, bulx, inc[0].length);
					sb_start = off[m][n][0] - parser.header.progPackStart[sb.resLvl];
					sb_len = len[m][n][0];
					int[] out_data = entropyDec.decode(sb.gOrient, streamBuffer, sb_start, sb_len,
							parser.header.cbTpLyr[c][r][s][m][n],
							parser.header.msbSk[c][r][s][m][n], blkw, blkh);

					dequantize(NomWidth, c, out_data, sb, blkw, blkh, buly, bulx, data);
				}
			}
		}
	}

	private void wavelet2Dreconstruction(Subband sb, short[] data) {
		int ulx, uly, w, h, imgw;
		int i, j, k;
		int offset;
		short[] buf = new short[sb.w];
		ulx = sb.ulx;
		uly = sb.uly;
		w = sb.w;
		h = sb.h;
		imgw = w;

		//Perform the horizontal reconstruction
		offset = uly * imgw + ulx;
		if(sb.ulcx % 2 == 0) { // start index is even => use LPF
			for(i = 0; i < h; i++, offset += imgw) {
				System.arraycopy(data, offset, buf, 0, w);
				synthetize_lpf(buf, 0, (w + 1) / 2, 1, buf, (w + 1) / 2, w / 2, 1, data, offset, 1);
			}
		}
		else { // start index is odd => use HPF
			for(i = 0; i < h; i++, offset += imgw) {
				System.arraycopy(data, offset, buf, 0, w);
				synthetize_hpf(buf, 0, w / 2, 1, buf, w / 2, (w + 1) / 2, 1, data, offset, 1);
			}
		}

		//Perform the vertical reconstruction
		offset = uly * imgw + ulx;
		if(sb.ulcy % 2 == 0) { // start index is even => use LPF
			for(j = 0; j < w; j++, offset++) {
				for(i = h - 1, k = offset + i * imgw; i >= 0; i--, k -= imgw) {
					buf[i] = data[k];
				}
				synthetize_lpf(buf, 0, (h + 1) / 2, 1, buf, (h + 1) / 2, h / 2, 1, data, offset, imgw);
			}

		}
		else { // start index is odd => use HPF
			for(j = 0; j < w; j++, offset++) {
				for(i = h - 1, k = offset + i * imgw; i >= 0; i--, k -= imgw) {
					buf[i] = data[k];
				}
				synthetize_hpf(buf, 0, h / 2, 1, buf, h / 2, (h + 1) / 2, 1, data, offset, imgw);
			}
		}
	}

	/*
	 * only performing the 5x3 for int values
	 */
	protected void synthetize_lpf(short[] lowSig, int lowOff, int lowLen, int lowStep, short[] highSig, int highOff,
			int highLen, int highStep, short[] outSig, int outOff, int outStep) {

		int i;
		int outLen = lowLen + highLen; //Length of the output signal
		int iStep = 2 * outStep; //Upsampling in outSig
		int ik; //Indexing outSig
		int lk; //Indexing lowSig
		int hk; //Indexing highSig

		/*
		 *Generate even samples (inverse low-pass filter)
		 */

		//Initialize counters
		lk = lowOff;
		hk = highOff;
		ik = outOff;

		//Handle tail boundary effect. Use symmetric extension.
		if(outLen > 1) {
			outSig[ik] = (short)(lowSig[lk] - ((highSig[hk] + 1) >> 1));
		}
		else {
			outSig[ik] = lowSig[lk];
		}

		lk += lowStep;
		hk += highStep;
		ik += iStep;

		//Apply lifting step to each "inner" sample.
		for(i = 2; i < outLen - 1; i += 2) {
			outSig[ik] = (short)(lowSig[lk] - ((highSig[hk - highStep] + highSig[hk] + 2) >> 2));

			lk += lowStep;
			hk += highStep;
			ik += iStep;
		}

		//Handle head boundary effect if input signal has odd length.
		if((outLen % 2 == 1) && (outLen > 2)) {
			outSig[ik] = (short)(lowSig[lk] - ((2 * highSig[hk - highStep] + 2) >> 2));
		}

		/*
		 *Generate odd samples (inverse high pass-filter)
		 */

		//Initialize counters
		hk = highOff;
		ik = outOff + outStep;

		//Apply first lifting step to each "inner" sample.
		for(i = 1; i < outLen - 1; i += 2) {
			// Since signs are inversed (add instead of substract)
			// the +1 rounding dissapears.
			outSig[ik] = (short)(highSig[hk] + ((outSig[ik - outStep] + outSig[ik + outStep]) >> 1));

			hk += highStep;
			ik += iStep;
		}

		//Handle head boundary effect if input signal has even length.
		if(outLen % 2 == 0 && outLen > 1) {
			outSig[ik] = (short)(highSig[hk] + outSig[ik - outStep]);
		}
	}

	/**
	 * only performing the 5x3 for int values
	 */
	protected void synthetize_hpf(short[] lowSig, int lowOff, int lowLen, int lowStep, short[] highSig, int highOff,
			int highLen, int highStep, short[] outSig, int outOff, int outStep) {
		int i;
		int outLen = lowLen + highLen; //Length of the output signal
		int iStep = 2 * outStep; //Upsampling in outSig
		int ik; //Indexing outSig
		int lk; //Indexing lowSig
		int hk; //Indexing highSig
		/*
		 *Generate even samples (inverse low-pass filter)
		 */
		//Initialize counters
		lk = lowOff;
		hk = highOff;
		ik = outOff + outStep;

		//Apply lifting step to each "inner" sample.
		for(i = 1; i < outLen - 1; i += 2) {
			outSig[ik] = (short)(lowSig[lk] - ((highSig[hk] + highSig[hk + highStep] + 2) >> 2));

			lk += lowStep;
			hk += highStep;
			ik += iStep;
		}

		if((outLen > 1) && (outLen % 2 == 0)) {
			// symmetric extension.
			outSig[ik] = (short)(lowSig[lk] - ((2 * highSig[hk] + 2) >> 2));
		}
		/*
		 *Generate odd samples (inverse high pass-filter)
		 */

		//Initialize counters
		hk = highOff;
		ik = outOff;

		if(outLen > 1) {
			outSig[ik] = (short)(highSig[hk] + outSig[ik + outStep]);
		}
		else {
			// Normalize for Nyquist gain
			outSig[ik] = (short)(highSig[hk] >> 1);
		}

		hk += highStep;
		ik += iStep;

		//Apply first lifting step to each "inner" sample.
		for(i = 2; i < outLen - 1; i += 2) {
			// Since signs are inversed (add instead of substract)
			// the +1 rounding dissapears.
			outSig[ik] = (short)(highSig[hk] + ((outSig[ik - outStep] + outSig[ik + outStep]) >> 1));
			hk += highStep;
			ik += iStep;
		}

		//Handle head boundary effect if input signal has odd length.
		if(outLen % 2 == 1 && outLen > 1) {
			outSig[ik] = (short)(highSig[hk] + outSig[ik - outStep]);
		}
	}

	private int getBlockWt(Subband sb, int n, int cn, int bulx, int nb) {
		if(n < nb - 1) {
			return (cn + n + 1) * sb.nomCBlkW - (sb.ulcx) + sb.ulx - bulx;
		}
		else {
			return sb.ulx + sb.w - bulx;
		}
	}

	private int getBlockUlx(Subband sb, int n, int cn) {
		if(n == 0) {
			return sb.ulx;
		}
		else {
			return (cn + n) * sb.nomCBlkW - sb.ulcx + sb.ulx;
		}
	}

	private int getBlockHt(Subband sb, int m, int cm, int buly, int nb) {
		if(m < nb - 1) {
			return (cm + m + 1) * sb.nomCBlkH - (sb.ulcy) + sb.uly - buly;
		}
		else {
			return sb.uly + sb.h - buly;
		}
	}

	private int getBlockUly(Subband sb, int m, int cm) {
		if(m == 0) {
			return sb.uly;
		}
		else {
			return (cm + m) * sb.nomCBlkH - sb.ulcy + sb.uly;
		}
	}

	/**
	 * this f8unction performs reversible quant as well as copys our data from the out data array to
	 * our main arrray
	 */
	private void dequantize(int NomWidth, int c, int[] out_data, Subband sb, int blkw, int blkh, int buly, int bulx,
			short[] data) {
		int magBits, shiftBits, of1 = 0, of2 = 0, j;
		magBits = sb.magbits;
		// will always be reversible
		shiftBits = 31 - magBits;
		for(int z = blkh - 1; z >= 0; z--) {
			for(j = 0, of1 = (z * blkw), of2 = ((buly + z) * NomWidth + bulx); j < blkw; j++, of1++, of2++) {
				data[of2] = (short)((out_data[of1] >= 0) ?
						(out_data[of1] >> shiftBits) :
						-((out_data[of1] & 0x7FFFFFFF) >> shiftBits));
			}

		}
	}

	/**
	 * get a ratio of the current subband width with the tile width right now this assumes a square tile
	 */
	protected int getResRatio(int res) {
		Subband node = parser.header.subband;
		while(node.resLvl != (res - 1)) {
			node = node.subb_LL;
		}
		return parser.getWidth()/*tileW*/ / node.w;
	}
}
