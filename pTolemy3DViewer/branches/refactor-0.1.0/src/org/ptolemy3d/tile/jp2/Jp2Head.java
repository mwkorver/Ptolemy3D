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
import java.util.Arrays;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.tile.Jp2TileLoader;

/*
 * given a URL to a jpeg 2000 file, this class<BR>
 * grabs the main header and all tilepart headers<BR>
 * <BR>
 * stream is open throughout most of the duration of this code, and is closed when header info is taken <BR>
 */

/**
 * Internal to JP2 Decoder
 */
public class Jp2Head
{
	/////////////////////////////////////// Markers //////////////////////////////////////////
	protected final static short SOC = (short) 0xff4f;
	protected final static short SOT = (short) 0xff90;
	protected final static short SOD = (short) 0xff93;
	protected final static short EOC = (short) 0xffd9;
	protected final static short SIZ = (short) 0xff51;
	protected final static int SSIZ_DEPTH_BITS = 7;
	protected final static int MAX_COMP_BITDEPTH = 38;
	protected final static short COD = (short) 0xff52;
	protected final static int SCOX_PRECINCT_PARTITION = 1;
	protected final static int PRECINCT_PARTITION_DEF_SIZE = 0xffff;
	protected final static short QCD = (short) 0xff5c;
	protected final static short QCC = (short) 0xff5d;
	protected final static int SQCX_GB_SHIFT = 5;
	protected final static int SQCX_GB_MSK = 7;
	protected final static int SQCX_NO_QUANTIZATION = 0x00;
	protected final static int SQCX_SCALAR_DERIVED = 0x01;
	protected final static int SQCX_SCALAR_EXPOUNDED = 0x02;
	protected final static int SQCX_EXP_SHIFT = 3;
	protected final static int SQCX_EXP_MASK = (1 << 5) - 1;
	protected final static short PPT = (short) 0xff61;
	protected final static int RES_LY_COMP_POS_PROG = 1;
	protected final static int INIT_LBLOCK = 3;
	////////////////////////////////////// end Markers //////////////////////////////////

	//FIXME static fields
	protected static Jp2Block jp2Header = null;

	protected static int res; // use this as abbr because res level is needed so often
	protected int imgW, imgH;
	protected int mv1, mv2, mv3;           // max value for each component
	protected int ls1, ls2, ls3;

	public final int getImageW() { return imgW; }
	public final int getImageH() { return imgH; }

	protected ByteArrayReader jpst = new ByteArrayReader(this);
	protected byte[][] pkdPktHeaders;    // Values Stored from the main and Tile headers
	// FROM the SIZ marker
	protected int imgOrigX,  imgOrigY,  tileW,  tileH,  tilingOrigX,  tilingOrigY,  nComp,  nTiles;
	protected int compSubsX[];
	protected int compSubsY[];
	protected int origBitDepth[];
	protected boolean isOrigSigned[];    // From the COD marker
	protected byte COD_cstyle,  COD_Prog,  COD_mct,  COD_reslvls,  COD_ecOptions,  COD_filterType;
	protected byte[] COD_precval;
	protected short COD_nlayers;
	protected int[] COD_cblk;    // From the QCD marker
	protected byte QCD_qStyle;
	protected int[][] QCD_exp;
	protected boolean QCD_rev = false;
	protected boolean QCD_der = false;
	protected boolean QCD_expound = false;
	protected int QCD_guardBits;    //From QCC markers
	protected byte[][] QCC_qStyle = null;
	protected int[][][][] QCC_exp;
	protected boolean[][] QCC_rev;
	protected boolean[][] QCC_der;
	protected boolean[][] QCC_expound;
	protected int[][] QCC_guardBits;
	protected boolean[][] QCC_ValSet;  // set to true if QCC marker is found in tile headers
	// Xtras
	protected int ntX,  ntY,  ctoy,  culy,  ctox,  culx;
	protected int[] precW;
	protected int[] precH;
	protected int[] rangeBits;
	protected int fileLength;

	protected Subband treeRoot = null;  // this subband is the subband node at res 0
	public int subbandLL_w(int res) {
		Subband nd = treeRoot;
		while (nd.resLvl != res) {
			nd = nd.subb_LL;
		}
		return nd.w;
	}

	private int tileBoxLen;
	private int[][] subRange;
	protected TagTreeDecoder[][][][] tdInclA;
	protected TagTreeDecoder[][][][] tdBDA;
	protected int[][][][][] lblock;
	protected int sbx[][][];
	protected int sby[][][];
	protected PktHeaderBitReader pkthdrbitrdr = new PktHeaderBitReader();

	public Jp2Head(byte[] hdat)
	{
		jpst.setByteArray(hdat);
		// read Main header and extract tile sizes and other parameters.
		parseHeader();

		// do some calcs at the end.
		ntX = (imgOrigX + imgW - tilingOrigX + tileW - 1) / tileW;
		ntY = (imgOrigY + imgH - tilingOrigY + tileH - 1) / tileH;
		int ctY = 0, ctX = 0, c = 0;
		ctoy = (ctY == 0) ? imgOrigY : tilingOrigY + ctY * tileH;  // make calcs for tile 0
		culy = (ctoy + compSubsY[c] - 1) / compSubsY[c];

		ctox = (ctX == 0) ? imgOrigX : tilingOrigX + ctX * tileW;
		culx = (ctox + compSubsX[c] - 1) / compSubsX[c];

		rangeBits = calcMixedBitDepths(origBitDepth, null);

		// arrays to hold our packet information]
		res = COD_reslvls;
		initTree(); // inits a model of our subband tree : to be used for ALL tiles
		findSubInResLvl();

		tdInclA = new TagTreeDecoder[nComp][res + 1][][];
		tdBDA = new TagTreeDecoder[nComp][res + 1][][];
		lblock = new int[nComp][res + 1][][][];
		sbx = new int[nComp][res + 1][];
		sby = new int[nComp][res + 1][];

		for (int st1 = 0; st1 < nComp; st1++)
		{
			for (int st2 = 0; st2 <= res; st2++)
			{
				int ct4 = getPrecMaxX(st1, st2) * getPrecMaxY(st1, st2);

				lblock[st1][st2] = new int[subRange[st2][1] + 1][][];
				sbx[st1][st2] = new int[subRange[st2][1] + 1];
				sby[st1][st2] = new int[subRange[st2][1] + 1];
				tdInclA[st1][st2] = new TagTreeDecoder[subRange[st2][1] + 1][ct4];
				tdBDA[st1][st2] = new TagTreeDecoder[subRange[st2][1] + 1][ct4];

				for (int s = subRange[st2][0]; s <= subRange[st2][1]; s++)
				{
					sbx[st1][st2][s] = getSBNumCodeBlocksXY(st2, s, true);
					sby[st1][st2][s] = getSBNumCodeBlocksXY(st2, s, false);
					lblock[st1][st2][s] = new int[sbx[st1][st2][s]][sby[st1][st2][s]];
				}

			}
		}

		pkdPktHeaders = new byte[nTiles][];
	}

	/**
	 * functions to parse tile headers
	 */
	private void readTileHeaders()
	{
		String snbytes = null;
		String srate = null;

		int tnbytes = 0;
		double rate;
		boolean rateInBytes = true;

		if (snbytes != null) {
			tnbytes = Integer.parseInt(snbytes);
		}
		else if (srate != null) {
			rate = Double.parseDouble(srate);
			tnbytes = (int) (rate * imgW * imgH) / 8;
			rateInBytes = false;
		}
		else { // no rate, just do it all baby!!
		}

		// init arrays to Ntiles size
		int[] tileParts = new int[nTiles];  // only used in this function call
		int[][] tilePartLen = new int[nTiles][];
		int[][] tilePartNum = new int[nTiles][];
		jp2Header.firstPackOff = new int[nTiles][];
		int[] tilePartsRead = new int[nTiles];
		int totTilePartsRead = 0;

		// Read all tile-part headers from all tiles.
		int tp = 0, tptot = 0;
		int tile, psot, tilePart, lsot, nrOfTileParts;
		int remainingTileParts = nTiles; // at least as many tile-parts as tiles

		// init our packet header holders

		while (remainingTileParts != 0)
		{

			lsot = tileBoxLen;

			if (lsot != 10)
			{
				return; //wrong length for sot marker
				// Isot
			}
			tile = jpst.readUnsignedShort();
			if (tile > 65534)
			{
				return; // tile index too high
			}
			// Psot
			psot = jpst.readInt();
			if (psot < 0)
			{
				return; // length larger than supported
				// TPsot
			}
			tilePart = jpst.read();
			// TNsot
			nrOfTileParts = jpst.read();

			if (tilePart == 0)
			{
				remainingTileParts += nrOfTileParts - 1;
				tileParts[tile] = nrOfTileParts;
				tilePartLen[tile] = new int[nrOfTileParts];
				tilePartNum[tile] = new int[nrOfTileParts];
				jp2Header.firstPackOff[tile] = new int[nrOfTileParts];
			}
			// Decode and store the tile-part header (i.e. until a SOD marker is
					// found)
			while (readTileBox(jpst.readShort(), jpst.readUnsignedShort(), tile) != 1);
			tilePartLen[tile][tilePart] = psot;

			tilePartNum[tile][tilePart] = totTilePartsRead;
			totTilePartsRead++;

			tp = tilePartsRead[tile];

			// Set tile part position and header length
			jp2Header.firstPackOff[tile][tp] = jpst.getPos() - 2; // for soc marker and boxlen

			tilePartsRead[tile]++;
			remainingTileParts--;
			tptot++;

		}
	}

	/**********************************************************************************
	 *
	 *    functions to parse header
	 **********************************************************************************/
	private boolean parseHeader()
	{
		jpst.readShort();
		// read in the size box, this needs to be the first box in the stream
		while (readMainBox(jpst.readShort(), jpst.readUnsignedShort()) != 1);

		ls3 = 1 << (origBitDepth[2] - 1);
		mv3 = (1 << origBitDepth[2]) - 1;
		ls2 = 1 << (origBitDepth[1] - 1);
		mv2 = (1 << origBitDepth[1]) - 1;
		ls1 = 1 << (origBitDepth[0] - 1);
		mv1 = (1 << origBitDepth[0]) - 1;

		return true;
	}

	private boolean passMainHeader()
	{
		jpst.readShort();

		// read in the size box, this needs to be the first box in the stream
		while (passMainBox(jpst.readShort(), jpst.readUnsignedShort()) != 1);

		return true;
	}

	private int passMainBox(short marker, int boxLen)
	{
		if (marker == SOT)
		{
			//jpst.seek(jpst.getPos() - 4);
			tileBoxLen = boxLen;
			return 1;
		}
		else
		{
			skipBytes(boxLen - 2);
			return 0;
		}
	}

	private int readMainBox(short marker, int boxLen)
	{
		switch (marker)
		{
			case SIZ:
				readSIZ();
				break;
			case SOT: // start of tile part, stop loop.
				tileBoxLen = boxLen;
				return 1;
			case COD:
				readCOD();
				break;
			case QCD:
				readQCD();
				break;
			case QCC:
				readQCC(0);
				break;
			default: // skip it boxLen bytes
			skipBytes(boxLen - 2);
			break;
		}
		return 0;
	}

	private int readTileBox(short marker, int boxLen, int tile)
	{
		switch (marker)
		{
			case SOD:
				tileBoxLen = boxLen;
				return 1;
			case PPT:
				readPPT(boxLen, tile);
				break;
				/*
            case QCC:
            readQCC(tile+1);
            break;
				 */
			default: // skip it boxLen bytes
				skipBytes(boxLen - 2);
			break;
		}
		return 0;
	}
	/*
	 *  values for quantization
	 *
	 */

	private void readQCD()
	{
		int maxb, minb;
		// Sqcd (quantization style)
		QCD_qStyle = (byte) jpst.readUnsignedByte();

		QCD_guardBits = (QCD_qStyle >> SQCX_GB_SHIFT) & SQCX_GB_MSK;
		QCD_qStyle &= ~(SQCX_GB_MSK << SQCX_GB_SHIFT);

		// If the main header is being read set default value of
		// dequantization spec
		switch (QCD_qStyle)
		{
			case SQCX_NO_QUANTIZATION:
				QCD_rev = true;
				break;
			case SQCX_SCALAR_DERIVED:
				QCD_der = true;
				break;
			case SQCX_SCALAR_EXPOUNDED:
				QCD_expound = true;
				break;
		}

		// loop over res levels
		QCD_exp = new int[COD_reslvls + 1][];

		for (int i = 0; i <= COD_reslvls; i++)
		{
			if (i == 0)
			{ // Only the LL subband
				minb = 0;
				maxb = 1;
			}
			else
			{
				minb = 1;
				maxb = 4;
			}
			QCD_exp[i] = new int[maxb];

			for (int j = minb; j < maxb; j++)
			{
				QCD_exp[i][j] = ((jpst.readUnsignedByte()) >> SQCX_EXP_SHIFT) & SQCX_EXP_MASK;
			}
		} // end loop on subbands
	}

	private void readQCC(int tileIdx)
	{
		int cComp;          // current component
		int tmp;

		// init our arrays if not set yet
		if (QCC_qStyle == null)
		{
			// add 1 for default qcc
			QCC_qStyle = new byte[nTiles + 1][nComp];
			QCC_exp = new int[nTiles + 1][nComp][][];
			QCC_rev = new boolean[nTiles + 1][nComp];
			QCC_der = new boolean[nTiles + 1][nComp];
			QCC_expound = new boolean[nTiles + 1][nComp];
			QCC_guardBits = new int[nTiles + 1][nComp];
			QCC_ValSet = new boolean[nTiles + 1][nComp];
		}

		// Cqcc
		cComp = (nComp < 257) ? jpst.readUnsignedByte() : jpst.readUnsignedShort();
		QCC_ValSet[tileIdx][cComp] = true;
		// Sqcc (quantization style)
		tmp = jpst.readUnsignedByte();
		QCC_guardBits[tileIdx][cComp] = ((tmp >> SQCX_GB_SHIFT) & SQCX_GB_MSK);
		tmp &= ~(SQCX_GB_MSK << SQCX_GB_SHIFT);
		QCC_qStyle[tileIdx][cComp] = (byte) tmp;

		// If main header is being read, set default for component in all
		// tiles
		switch (tmp)
		{
			case SQCX_NO_QUANTIZATION:
				QCC_rev[tileIdx][cComp] = true;
				break;
			case SQCX_SCALAR_DERIVED:
				QCC_der[tileIdx][cComp] = true;
				break;
			case SQCX_SCALAR_EXPOUNDED:
				QCC_expound[tileIdx][cComp] = true;
				break;
			default:
				break;
		}

		int minb, maxb;
		QCC_exp[tileIdx][cComp] = new int[COD_reslvls + 1][];

		for (int rl = 0; rl <= COD_reslvls; rl++)
		{ // Loop on resolution levels
			// Find the number of subbands in the resolution level
			if (rl == 0)
			{ // Only the LL subband
				minb = 0;
				maxb = 1;
			}
			else
			{
				minb = 1;
				maxb = 4;
			}
			QCC_exp[tileIdx][cComp][rl] = new int[maxb];

			for (int j = minb; j < maxb; j++)
			{
				tmp = jpst.readUnsignedByte();
				QCC_exp[tileIdx][cComp][rl][j] = (tmp >> SQCX_EXP_SHIFT) & SQCX_EXP_MASK;
			}// end for j
		}// end for rl
	}

	/*
	 *
	 *   read in packet headers
	 */
	private void readPPT(int boxlen, int tile)
	{
		int indx;
		// Zppt (index of PPM marker)
		indx = jpst.readUnsignedByte();
		// Ippt (packed packet headers)
		if ((pkdPktHeaders[tile] == null) || (pkdPktHeaders[tile].length < boxlen - 3))
		{
			pkdPktHeaders[tile] = null;
			pkdPktHeaders[tile] = new byte[boxlen - 3];
		}
		jpst.readFully(pkdPktHeaders[tile], 0, boxlen - 3);
		//if (pkdPktHeaders[tile] == null)
		//	pkdPktHeaders[tile] = new ByteArrayOutputStream();

		//pkdPktHeaders[tile].write(tilePkdPktHeaders,0,tilePkdPktHeaders.length);
	}

	private void readCOD()
	{
		//try{

		COD_cstyle = (byte) jpst.readUnsignedByte();
		COD_Prog = (byte) jpst.readUnsignedByte();
		COD_nlayers = (short) jpst.readUnsignedShort();
		COD_mct = (byte) jpst.readUnsignedByte();// Multiple component transform
		COD_reslvls = (byte) jpst.readUnsignedByte();
		// Read the code-blocks dimensions
		COD_cblk = new int[2];
		COD_cblk[0] = 1 << (jpst.readUnsignedByte() + 2);
		COD_cblk[1] = 1 << (jpst.readUnsignedByte() + 2);
		COD_ecOptions = (byte) jpst.readUnsignedByte();
		// Read wavelet filter for tile or image
		COD_filterType = (byte) jpst.readUnsignedByte();
		// Get precinct partition sizes
		if ((COD_cstyle & SCOX_PRECINCT_PARTITION) != 0)
		{
		}
		else
		{
			precW = new int[1];
			precH = new int[1];
			precW[0] = 1 << (PRECINCT_PARTITION_DEF_SIZE & 0x000F);
			precH[0] = 1 << (((PRECINCT_PARTITION_DEF_SIZE & 0x00F0) >> 4));
		}

		//}
		//catch (IOException e){}
	}

	private void readSIZ()
	{
		int tmp;
		// Read the capability of the codestream (Rsiz)
		jpst.readUnsignedShort();
		imgW = jpst.readInt();
		imgH = jpst.readInt();
		imgW -= imgOrigX = jpst.readInt(); // XOsiz
		imgH -= imgOrigY = jpst.readInt(); // Y0siz
		tileW = jpst.readInt();
		tileH = jpst.readInt();
		tilingOrigX = jpst.readInt(); // XTOsiz
		tilingOrigY = jpst.readInt(); // YTOsiz
		// Read number of components and initialize related arrays
		nComp = jpst.readUnsignedShort();
		origBitDepth = new int[nComp];
		isOrigSigned = new boolean[nComp];
		compSubsX = new int[nComp];
		compSubsY = new int[nComp];
		// Read bitdepth and downsampling factors for each component
		for (int i = 0; i < nComp; i++)
		{
			tmp = jpst.readUnsignedByte();
			isOrigSigned[i] = ((tmp >>> SSIZ_DEPTH_BITS) == 1);
			origBitDepth[i] = (tmp & ((1 << SSIZ_DEPTH_BITS) - 1)) + 1;
			if ((origBitDepth[i] + (isOrigSigned[i] ? 1 : 0)) > MAX_COMP_BITDEPTH)
			{
			}

			compSubsX[i] = jpst.readUnsignedByte();
			compSubsY[i] = jpst.readUnsignedByte();
		}
		nTiles =
			((imgOrigX + imgW - tilingOrigX + tileW - 1) / tileW) *
			((imgOrigY + imgH - tilingOrigY + tileH - 1) / tileH);
	}

	/*** util functions ***/
	/*
	 * ctX which tile we are at x, starting at 0
	 */
	protected final int getCompWidth(int c, int rl, int ctX)
	{
		int ntulx;

		int dl = COD_reslvls - rl; // Revert level indexation (0 is hi-res)
		// Calculate starting X of next tile X-wise at reference grid hi-res
		ntulx = (ctX < ntX - 1) ? tilingOrigX + (ctX + 1) * tileW : imgOrigX + imgW;
		// Convert reference grid hi-res to component grid hi-res
		ntulx = (ntulx + compSubsX[c] - 1) / compSubsX[c];
		// Starting X of current tile at component grid hi-res is culx[c]
		// The difference at the rl level is the width
		return (ntulx + (1 << dl) - 1) / (1 << dl) - (culx + (1 << dl) - 1) / (1 << dl);
	}

	protected final int getCompHeight(int c, int rl, int ctY)
	{
		int ntuly;
		int dl = COD_reslvls - rl; // Revert level indexation (0 is hi-res)
		// Calculate starting Y of next tile Y-wise at reference grid hi-res
		ntuly = (ctY < ntY - 1) ? tilingOrigY + (ctY + 1) * tileH : imgOrigY + imgH;
		// Convert reference grid hi-res to component grid hi-res
		ntuly = (ntuly + compSubsY[c] - 1) / compSubsY[c];
		// Starting Y of current tile at component grid hi-res is culy[c]
		// The difference at the rl level is the height
		return (ntuly + (1 << dl) - 1) / (1 << dl) - (culy + (1 << dl) - 1) / (1 << dl);
	}

	protected final int getULX(int c, int rl)
	{
		int dl = COD_reslvls - rl;
		return (culx + (1 << dl) - 1) / (1 << dl);
	}

	protected final int getULY(int c, int rl)
	{
		int dl = COD_reslvls - rl;
		return (culy + (1 << dl) - 1) / (1 << dl);
	}
	//public ByteArrayInputStream getPacketHeaders(int tile){
	//	return new ByteArrayInputStream(pkdPktHeaders[tile].toByteArray());
	//}
	protected void initTree()
	{
		// there is a subband root for every tile and every component.
		// however, to keep things simple, we will use tile 0,0 and comp 0 for every
		// tree access.  a spec of our decoder is that all tiles are uniform.
		int tilex = 0;

		treeRoot = new Subband(getCompWidth(0, res, 0), getCompHeight(0, res, 0), getULX(0, res), getULY(0, res), res, tilex,
				COD_cblk[0], COD_cblk[1]);
	}

	/*
	 *  and now the fun begins...
	 * our objectives from this parsing is to get the
	 * offsets and lengths to our code blocks.
	 *
	 * packets correspond to a resolution - layer - component - precinct
	 * each precinct contains a number of code passes
	 *
	 *  known tile settings for later : curPos , bin
	 *
	 */
	private void parsePacketHeaders(int theTile)
	{
		// 4 counters
		boolean case1 = true;
		int ct1 = 0, ct2 = 0, ct3 = 0, rl = 0, ly, tmp, tmp2, totnewtp, ctp, m, n;
		int curPos = jp2Header.firstPackOff[theTile][0]; // for tile 0, offset 0
		int st1 = 0, st2 = 0, st3 = 0, st4 = 0;
		pkthdrbitrdr.set(pkdPktHeaders[theTile]);

		try
		{
			switch (COD_Prog)
			{
				/*case LY_RES_COMP_POS_PROG:
                ct1 = COD_nlayers;// layers
                ct2 = Res + 1;// res levels add 1, to include 0 and 5
                break;
				 */
				case RES_LY_COMP_POS_PROG:
					ct1 = res + 1;// res levels add 1, to include 0 and 5
					ct2 = COD_nlayers;// layers
					case1 = false;
					break;
				default:
					break;
			}
			ct3 = nComp; // components

			//TileHeader.progPackStart[theTile] = new int[ct1];
			//TileHeader.progPackEnd[theTile] = new int[ct1];
			boolean initTH;
			for (st1 = 0; st1 < nComp; st1++)
			{
				for (st2 = 0; st2 <= res; st2++)
				{
					initTH = false;

					if (jp2Header.CB_len[theTile][st1][st2] == null) {
						initTH = true;
					}
					else {
						try {
							if (jp2Header.CB_len[theTile][st1][st2][0].length != subRange[st2][1] + 1) {
								initTH = true;
							}
						}
						catch (Exception e) {
							initTH = true;
						}
					}

					if (initTH) {
						jp2Header.CB_len[theTile][st1][st2] = new short[subRange[st2][1] + 1][][][];
						jp2Header.CB_off[theTile][st1][st2] = new int[subRange[st2][1] + 1][][][];
						jp2Header.cbInc[theTile][st1][st2] = new boolean[subRange[st2][1] + 1][][];
						jp2Header.cbTpLyr[theTile][st1][st2] = new int[subRange[st2][1] + 1][][][];
						jp2Header.msbSk[theTile][st1][st2] = new int[subRange[st2][1] + 1][][];
					}

					for (int s = subRange[st2][0]; s <= subRange[st2][1]; s++)
					{
						initTH = false;

						if (jp2Header.CB_len[theTile][st1][st2][s] == null)
						{
							initTH = true;
						}
						else
						{
							try
							{
								if ((jp2Header.CB_len[theTile][st1][st2][s].length != sbx[st1][st2][s]) || (jp2Header.CB_len[theTile][st1][st2][s][0].length != sby[st1][st2][s]) || (jp2Header.CB_len[theTile][st1][st2][s][0][0].length != COD_nlayers))
								{
									initTH = true;
								}
							}
							catch (Exception e)
							{
								initTH = true;
							}
						}


						if (initTH)
						{
							try {
								jp2Header.CB_len[theTile][st1][st2][s] = new short[sbx[st1][st2][s]][sby[st1][st2][s]][COD_nlayers];
								jp2Header.CB_off[theTile][st1][st2][s] = new int[sbx[st1][st2][s]][sby[st1][st2][s]][COD_nlayers];
								jp2Header.cbInc[theTile][st1][st2][s] = new boolean[sbx[st1][st2][s]][sby[st1][st2][s]];
								jp2Header.cbTpLyr[theTile][st1][st2][s] = new int[sbx[st1][st2][s]][sby[st1][st2][s]][COD_nlayers];
								jp2Header.msbSk[theTile][st1][st2][s] = new int[sbx[st1][st2][s]][sby[st1][st2][s]];
							}
							catch(OutOfMemoryError e) {
								IO.printError("OutOfMemoryError allocating memory for ["+theTile+"]["+st1+"]["+st2+"]["+s+"]");
								throw e;
							}
						}

						for (int k = sby[st1][st2][s] - 1; k >= 0; k--)
						{
							Arrays.fill(lblock[st1][st2][s][k], INIT_LBLOCK);
							Arrays.fill(jp2Header.cbInc[theTile][st1][st2][s][k], false);
							Arrays.fill(jp2Header.msbSk[theTile][st1][st2][s][k], 0);

							for (int jj = 0; jj < sby[st1][st2][s]; jj++)
							{
								Arrays.fill(jp2Header.CB_len[theTile][st1][st2][s][k][jj], (short) 0);
								Arrays.fill(jp2Header.CB_off[theTile][st1][st2][s][k][jj], 0);
								Arrays.fill(jp2Header.cbTpLyr[theTile][st1][st2][s][k][jj], 0);
							}
						}
					}
				}
			}
			int ct4;
			for (st1 = 0; st1 < ct1; st1++)
			{
				jp2Header.progPackStart[theTile][st1] = curPos;
				for (st2 = 0; st2 < ct2; st2++)
				{
					rl = (case1) ? st2 : st1;
					ly = (case1) ? st1 : st2;
					for (st3 = 0; st3 < ct3; st3++)
					{
						ct4 = getPrecMaxX(st3, rl) * getPrecMaxY(st3, rl);
						for (st4 = 0; st4 < ct4; st4++)
						{
							/***** start 4 tier loop ****/
							// parse the packet header
							pkthdrbitrdr.sync();
							// is the packet empty?

									if (pkthdrbitrdr.readBit() == 0)
									{
										// no code blocks
										continue;
									}
							// there are code blocks so...
							// loop on subbands
							for (int s = subRange[rl][0]; s <= subRange[rl][1]; s++)
							{
								if (tdInclA[st3][rl][s][st4] == null)
								{
									tdInclA[st3][rl][s][st4] = new TagTreeDecoder(sby[st3][rl][s], sbx[st3][rl][s]);
									tdBDA[st3][rl][s][st4] = new TagTreeDecoder(sby[st3][rl][s], sbx[st3][rl][s]);
								}
								else
								{
									tdInclA[st3][rl][s][st4].set(sby[st3][rl][s], sbx[st3][rl][s]);
									tdBDA[st3][rl][s][st4].set(sby[st3][rl][s], sbx[st3][rl][s]);
								}


								// loop on x,y code blocks
								for (m = 0; m < sby[st3][rl][s]; m++)
								{
									for (n = 0; n < sbx[st3][rl][s]; n++)
									{
										// inclusion not set yet ?
												ctp = arraySum(jp2Header.cbTpLyr[theTile][st3][rl][s][m][n]);

										if ((!jp2Header.cbInc[theTile][st3][rl][s][m][n]) || (ctp == 0))
										{

											// Read inclusion using tag-tree
											tmp = tdInclA[st3][rl][s][st4].update(m, n, ly + 1, pkthdrbitrdr);
											if (tmp > ly)
											{ // Not included
												continue;
											}

											// Read bitdepth using tag-tree
											tmp = 1;// initialization
											for (tmp2 = 1; tmp >= tmp2; tmp2++)
											{
												tmp = tdBDA[st3][rl][s][st4].update(m, n, tmp2, pkthdrbitrdr);
											}
											jp2Header.msbSk[theTile][st3][rl][s][m][n] = tmp2 - 2;


											totnewtp = 1;
											// set trunc points to 0 for this layer
											jp2Header.cbTpLyr[theTile][st3][rl][s][m][n][ly] = 0;
											// set inclusion
											jp2Header.cbInc[theTile][st3][rl][s][m][n] = true;
										}
										else
										{
											// included?
													if (pkthdrbitrdr.readBit() != 1)
													{
														continue;
													}
													totnewtp = 1;
										}

										// Read new truncation points
										if (pkthdrbitrdr.readBit() == 1)
										{// if bit is 1
											totnewtp++;

											// if next bit is 0 do nothing
											if (pkthdrbitrdr.readBit() == 1)
											{//if is 1
												totnewtp++;

												tmp = pkthdrbitrdr.readBits(2);
												totnewtp += tmp;

												// If next 2 bits are not 11 do nothing
												if (tmp == 0x3)
												{ //if 11
													tmp = pkthdrbitrdr.readBits(5);
													totnewtp += tmp;

													// If next 5 bits are not 11111 do nothing
													if (tmp == 0x1F)
													{//if 11111
														totnewtp +=
															pkthdrbitrdr.readBits(7);
													}
												}
											}
										}
										jp2Header.cbTpLyr[theTile][st3][rl][s][m][n][ly] = totnewtp;

										// Code-block length

										while (pkthdrbitrdr.readBit() != 0)
										{
											lblock[st3][rl][s][m][n]++;
										}
										jp2Header.CB_len[theTile][st3][rl][s][m][n][ly] = (short) pkthdrbitrdr.readBits(lblock[st3][rl][s][m][n] + log2(totnewtp));
										jp2Header.CB_off[theTile][st3][rl][s][m][n][ly] = curPos;
										curPos += jp2Header.CB_len[theTile][st3][rl][s][m][n][ly];

									}
								} // end on code block loops
							} // end loop on subbands
							/***** end 4 tier loop ****/
						}
					}
				}
				jp2Header.progPackEnd[theTile][st1] = curPos;
			}
		} // end try
		catch (IOException e)
		{
		}

	}

	int getPrecMaxX(int c, int r)
	{
		// Subsampling factors
		int xrsiz = compSubsX[c];

		// Tile's coordinates in the image component domain
		int tx0 = getULX(0, r);
		int tx1 = getCompHeight(0, r, 0);

		int tcx0 = (int) Math.ceil(tx0 / (double) (xrsiz));
		int tcx1 = (int) Math.ceil(tx1 / (double) (xrsiz));

		// Tile's coordinates in the reduced resolution image domain
		int trx0 = (int) Math.ceil(tcx0 / (double) (1 << (res - r)));
		int trx1 = (int) Math.ceil(tcx1 / (double) (1 << (res - r)));

		double twoppx = getPPX(r);
		if (trx1 > trx0)
		{
			return (int) Math.ceil(trx1 / twoppx) - (int) Math.floor(trx0 / twoppx);
		}
		else
		{
			return 0;
		}
	}

	int getPrecMaxY(int c, int r)
	{
		int yrsiz = compSubsY[c];
		int ty0 = getULY(0, r);
		int ty1 = getCompWidth(0, r, 0);
		int tcy0 = (int) Math.ceil(ty0 / (double) (yrsiz));
		int tcy1 = (int) Math.ceil(ty1 / (double) (yrsiz));

		// Tile's coordinates in the reduced resolution image domain
		int try0 = (int) Math.ceil(tcy0 / (double) (1 << (res - r)));
		int try1 = (int) Math.ceil(tcy1 / (double) (1 << (res - r)));

		double twoppy = getPPY(r);
		if (try1 > try0)
		{
			return (int) Math.ceil(try1 / twoppy) - (int) Math.floor(try0 / twoppy);
		}
		else
		{
			return 0;
		}
	}

	int getPPX(int reslvl)
	{
		return precW[0];
	}

	int getPPY(int reslvl)
	{
		return precH[0];
	}

	protected static int log2(int x)
	{
		int y, v;
		// No log of 0 or negative
		if (x <= 0)
		{
			throw new IllegalArgumentException("" + x + " <= 0");
		}
		// Calculate log2 (it's actually floor log2)
		v = x;
		y = -1;
		while (v > 0)
		{
			v >>= 1;
		y++;
		}
		return y;
	}

	/*
	 *  start at tree, level Res, and go to down resolution 0
	 */
	Subband getSubbandByIdx(int rl, int sbi)
	{
		Subband sb = treeRoot;
		// go down to specified resolution level.
		while (sb.resLvl > rl)
		{
			sb = sb.subb_LL;
		}

		// if at reslevl 0, just return, no children
		if (rl == 0)
		{
			return sb;
		}
		switch (sbi)
		{
			case 1:
				return sb.subb_HL;
			case 2:
				return sb.subb_LH;
			case 3:
				return sb.subb_HH;
		}
		return null;
	}

	private final void findSubInResLvl()
	{
		subRange = new int[res + 1][2];
		// Dyadic decomposition
		for (int i = res; i > 0; i--)
		{
			subRange[i][0] = 1;
			subRange[i][1] = 3;
		}
		subRange[0][0] = 0;
		subRange[0][1] = 0;
		//  return subRange;
	}
	/*
	 *  c- component
	 *  r - resolution
	 *  ix - index :
	 *  true for x
	 */

	private int getSBNumCodeBlocksXY(int r, int ix, boolean isx)
	{
		int tmp;
		int apox = 0;
		int apoy = 0;
		Subband sb = getSubbandByIdx(r, ix);
		if (isx)
		{
			tmp = sb.ulcx - apox + sb.nomCBlkW;
			return (tmp + sb.w - 1) / sb.nomCBlkW - (tmp / sb.nomCBlkW - 1);
		}
		else
		{
			tmp = sb.ulcy - apoy + sb.nomCBlkH;
			return (tmp + sb.h - 1) / sb.nomCBlkH - (tmp / sb.nomCBlkH - 1);
		}
	}

	static int arraySum(int[] a)
	{
		int sum = 0;
		for (int i = 0; i < a.length; i++)
		{
			sum += a[i];
		}
		return sum;
	}
	/*
	 *  we will only be performing rct transformations
	 *
	 */

	protected int[] calcMixedBitDepths(int utdepth[], int tdepth[])
	{

		if (tdepth == null)
		{
			tdepth = new int[utdepth.length];
		}
		if (utdepth.length > 3)
		{
			System.arraycopy(utdepth, 3, tdepth, 3, utdepth.length - 3);
		}
		tdepth[0] = log2((1 << utdepth[0]) + (2 << utdepth[1]) +
				(1 << utdepth[2]) - 1) - 2 + 1;
		tdepth[1] = log2((1 << utdepth[2]) + (1 << utdepth[1]) - 1) + 1;
		tdepth[2] = log2((1 << utdepth[0]) + (1 << utdepth[1]) - 1) + 1;

		return tdepth;
	}

	public boolean setTileHeader(Jp2Block jp2Header, byte[] data)
	{
		this.jp2Header = jp2Header;

		jpst.setByteArray(data);

		// pass over main header
		passMainHeader();
		readTileHeaders();

		boolean initTH = false;
		if (jp2Header.CB_len == null) {
			initTH = true;
		}
		else  {
			try {
				if ((jp2Header.CB_len.length != nTiles) ||
						(jp2Header.CB_len[0].length != nComp) ||
						(jp2Header.CB_len[0][0].length != (res + 1))) {
					initTH = true;
				}
			}
			catch (Exception e) {
				initTH = true;
			}
		}

		if (initTH) {
			jp2Header.CB_len = new short[nTiles][nComp][res + 1][][][][];
			jp2Header.CB_off = new int[nTiles][nComp][res + 1][][][][];
			jp2Header.cbInc = new boolean[nTiles][nComp][res + 1][][][]; // for tag tree inclusion
			jp2Header.cbTpLyr = new int[nTiles][nComp][res + 1][][][][]; // trunc points per layer
			jp2Header.msbSk = new int[nTiles][nComp][res + 1][][][];
			jp2Header.progPackStart = new int[nTiles][res + 1];
			jp2Header.progPackEnd = new int[nTiles][res + 1];
		}

		// read in the packed packet headers
		for (int k = 0; k < nTiles; k++) {
			parsePacketHeaders(k);
		}

		return true;
	}

	private void skipBytes(int len)
	{
		int bytesread;
		while (len > 0)
		{
			bytesread = jpst.skipBytes(len);
			len -= bytesread;
		}
	}

	/**
	 * Prepare image data.<BR>
	 * Return the source parameter for setImageData.
	 * @see #setImageData(byte[], short[][], int)
	 */
	public void prepareImageData(short[][] dst, byte[] src, int size, int prevSize)
	{
		int m = 0;
		for (int k = 0; k < prevSize; k++)
		{
			int dix = k * size;
			for (int j = 0; j < prevSize; j++)
			{
				int u0, u1, u2;

				u0 = (src[m++] & 0xFF) - ls1;
				u1 = (src[m++] & 0xFF) - ls2;
				u2 = (src[m++] & 0xFF) - ls3;

				// RCT
				dst[1][dix + j] = (short) (u2 - u1);
				dst[2][dix + j] = (short) (u0 - u1);
				dst[0][dix + j] = (short) (((dst[1][dix + j] + dst[2][dix + j]) >> 2) + u1);
			}
		}
	}
	/**
	 * @see #prepareImageData(short[][], byte[], int, int)
	 */
	public void setImageData(byte[] dst, short[][] src, int size)
	{
		int offset = 0;
		for (int i = 0; i < size; i++)
		{
			for (int j = 0; j < size; j++)
			{
				int tmp0, tmp1, tmp2, tmp3, k1;

				k1 = (i * size) + j;

				// RCT
				tmp0 = (src[0][k1] - ((src[1][k1] + src[2][k1]) >> 2));

				tmp1 = (src[2][k1] + tmp0) + ls1;
				tmp2 = (tmp0) + ls2;
				tmp3 = (src[1][k1] + tmp0) + ls3;

				tmp1 = (tmp1 < 0) ? 0 : ((tmp1 > mv1) ? mv1 : tmp1);
				tmp2 = (tmp2 < 0) ? 0 : ((tmp2 > mv2) ? mv2 : tmp2);
				tmp3 = (tmp3 < 0) ? 0 : ((tmp3 > mv3) ? mv3 : tmp3);

				dst[offset++] = (byte) tmp1;
				dst[offset++] = (byte) tmp2;
				dst[offset++] = (byte) tmp3;
			}
		}
	}
}

