/* $LICENSE$ */
package org.ptolemy3d.jp2.fast;

import java.io.IOException;
import java.util.Arrays;

import org.ptolemy3d.debug.IO;
import org.ptolemy3d.io.Stream;

class Jp2Header {
	private final Stream stream;
	private ByteInputStream is;
	
	/* parseMainHeader */
	private int mv1, mv2, mv3; // max value for each component
	private int ls1, ls2, ls3;
	/* readSIZ */
	private int imageWidth, imageHeight;
	private int imgOrigX, imgOrigY, tileW, tileH, tilingOrigX, tilingOrigY, numChannels, numTiles;
	private int[] compSubsX, compSubsY, origBitDepth;
	private boolean[] isOrigSigned;
	/* readCOD */
	protected byte COD_ecOptions;
	protected int[] COD_cblk;
	private byte COD_cstyle, COD_Prog, COD_reslvls;
	private int precW, precH;
	private short COD_nlayers;
	/* readQCD */
	private byte QCD_qStyle;
	protected int QCD_guardBits;
	protected int[][] QCD_exp;
	/* readQCC */
	private byte[][] QCC_qStyle = null;
	protected int[][][][] QCC_exp;
	private boolean[][] QCC_rev;
	private boolean[][] QCC_der;
	private boolean[][] QCC_expound;
	protected int[][] QCC_guardBits;
	protected boolean[][] QCC_ValSet; // set to true if QCC marker is found in tile headers
	/* analyseMainHeader */
	private int maxRes, ntX, ntY, ctoy, culy, ctox, culx;
	private byte[][] pkdPktHeaders;
	private int[][] subRange;
	private int[][][] sbx, sby;
	private int[][][][][] lblock;
	private TagTreeDecoder[][][][] tdInclA, tdBDA;
	protected Subband subband;
	/* readSOTorSOD */
	private int tileBoxLen;

	public Jp2Header(Stream stream) throws IOException {
		this.stream = stream;
		this.packetHeaders = new PacketHeaders();
		parseHeader();
	}
	
	//=============================================================================================
	// Main Header
	//=============================================================================================
	
	public void parseHeader() throws IOException {
		is = new ByteInputStream(stream.createInputStream());
		parseMainHeader();
		analyseMainHeader();
		readTileHeader();
		readPacketHeader();
		is.close();
	}

	private boolean parseMainHeader() throws IOException {
		is.readShort();
		
		// read in the size box, this needs to be the first box in the stream
		while(readMainBox(is.readShort(), is.readUnsignedShort()) != 1) {
		}

		ls3 = 1 << (origBitDepth[2] - 1);
		mv3 = (1 << origBitDepth[2]) - 1;
		ls2 = 1 << (origBitDepth[1] - 1);
		mv2 = (1 << origBitDepth[1]) - 1;
		ls1 = 1 << (origBitDepth[0] - 1);
		mv1 = (1 << origBitDepth[0]) - 1;

		return true;
	}
	private static interface MainHeader {
		public final static short SIZ = (short)0xff51;
		public final static short SOT = (short)0xff90;
		public final static short COD = (short)0xff52;
		public final static short QCD = (short)0xff5c;
		public final static short QCC = (short)0xff5d;
	}
	private int readMainBox(short marker, int boxLen) throws IOException {
		switch(marker) {
			case MainHeader.SIZ: readSIZ(); break;
			case MainHeader.SOT: readSOTorSOD(boxLen); return 1;
			case MainHeader.COD: readCOD(); break;
			case MainHeader.QCD: readQCD(); break;
			case MainHeader.QCC: readQCC(0); break;
			default:
				is.skip(boxLen - 2);
				break;
		}
		return 0;
	}
	
	/** SIZ Chunk */
	private static interface SIZ {
		public final static int SSIZ_DEPTH_BITS = 7;
		public final static int MAX_COMP_BITDEPTH = 38;
	}
	private void readSIZ() throws IOException {
		// Read the capability of the codestream (Rsiz)
		is.readUnsignedShort();
		imageWidth = is.readInt();
		imageHeight = is.readInt();
		imageWidth -= imgOrigX = is.readInt(); // XOsiz
		imageHeight -= imgOrigY = is.readInt(); // Y0siz
		tileW = is.readInt();
		tileH = is.readInt();
		tilingOrigX = is.readInt(); // XTOsiz
		tilingOrigY = is.readInt(); // YTOsiz
		
		// Read number of components and initialize related arrays
		numChannels = is.readUnsignedShort();
		origBitDepth = new int[numChannels];
		isOrigSigned = new boolean[numChannels];
		compSubsX = new int[numChannels];
		compSubsY = new int[numChannels];
		
		// Read bitdepth and downsampling factors for each component
		for(int i = 0; i < numChannels; i++) {
			int tmp = is.readUnsignedByte();
			isOrigSigned[i] = ((tmp >>> SIZ.SSIZ_DEPTH_BITS) == 1);
			origBitDepth[i] = (tmp & ((1 << SIZ.SSIZ_DEPTH_BITS) - 1)) + 1;
			if((origBitDepth[i] + (isOrigSigned[i] ? 1 : 0)) > SIZ.MAX_COMP_BITDEPTH) {
				
			}
			compSubsX[i] = is.readUnsignedByte();
			compSubsY[i] = is.readUnsignedByte();
		}
		numTiles = ((imgOrigX + imageWidth - tilingOrigX + tileW - 1) / tileW)
			   * ((imgOrigY + imageHeight - tilingOrigY + tileH - 1) / tileH);
	}
	
	/** SOT Chunk */
	private void readSOTorSOD(int boxLen) {
		tileBoxLen = boxLen;
	}
	
	/** COD Chunk */
	private static interface COD {
		public final static int SCOX_PRECINCT_PARTITION = 1;
		public final static int PRECINCT_PARTITION_DEF_SIZE = 0xffff;
	}
	private void readCOD() throws IOException {
		COD_cstyle = (byte)is.readUnsignedByte();
		COD_Prog = (byte)is.readUnsignedByte();
		COD_nlayers = (short)is.readUnsignedShort();
		/*byte COD_mct = (byte)*/is.readUnsignedByte();	// Multiple component transform
		COD_reslvls = (byte)is.readUnsignedByte();
		
		// Read the code-blocks dimensions
		COD_cblk = new int[2];
		COD_cblk[0] = 1 << (is.readUnsignedByte() + 2);
		COD_cblk[1] = 1 << (is.readUnsignedByte() + 2);
		COD_ecOptions = (byte)is.readUnsignedByte();
		
		// Read wavelet filter for tile or image
		/*byte COD_filterType = (byte)*/is.readUnsignedByte();
		
		// Get precinct partition sizes
		if((COD_cstyle & COD.SCOX_PRECINCT_PARTITION) != 0) {
			
		}
		else {
			precW = 1 << (COD.PRECINCT_PARTITION_DEF_SIZE & 0x000F);
			precH = 1 << (((COD.PRECINCT_PARTITION_DEF_SIZE & 0x00F0) >> 4));
		}
	}
	
	/** QCD Chunk */
	private static interface QCD {
		public final static int SQCX_GB_SHIFT = 5;
		public final static int SQCX_GB_MSK = 7;
		public final static int SQCX_NO_QUANTIZATION = 0x00;
		public final static int SQCX_SCALAR_DERIVED = 0x01;
		public final static int SQCX_SCALAR_EXPOUNDED = 0x02;
		public final static int SQCX_EXP_SHIFT = 3;
		public final static int SQCX_EXP_MASK = (1 << 5) - 1;
	}
	private void readQCD() throws IOException {
		// Sqcd (quantization style)
		QCD_qStyle = (byte)is.readUnsignedByte();

		QCD_guardBits = (QCD_qStyle >> QCD.SQCX_GB_SHIFT) & QCD.SQCX_GB_MSK;
		QCD_qStyle &= ~(QCD.SQCX_GB_MSK << QCD.SQCX_GB_SHIFT);

		// If the main header is being read set default value of dequantization spec
		/*boolean QCD_rev, QCD_der, QCD_expound;
		switch(QCD_qStyle) {
			case QCD.SQCX_NO_QUANTIZATION:
				QCD_rev = true;
				break;
			case QCD.SQCX_SCALAR_DERIVED:
				QCD_der = true;
				break;
			case QCD.SQCX_SCALAR_EXPOUNDED:
				QCD_expound = true;
				break;
		}*/

		// loop over res levels
		QCD_exp = new int[COD_reslvls + 1][];

		for(int i = 0; i <= COD_reslvls; i++) {
			int minb, maxb;
			if(i == 0) { // Only the LL subband
				minb = 0;
				maxb = 1;
			}
			else {
				minb = 1;
				maxb = 4;
			}
			QCD_exp[i] = new int[maxb];

			for(int j = minb; j < maxb; j++) {
				QCD_exp[i][j] = ((is.readUnsignedByte()) >> QCD.SQCX_EXP_SHIFT) & QCD.SQCX_EXP_MASK;
			}
		}
	}

	/** QCC Chunk */
	private static interface QCC extends QCD {
		
	}
	private void readQCC(int tileIdx) throws IOException {
		// init our arrays if not set yet
		if(QCC_qStyle == null) {
			// add 1 for default qcc
			QCC_qStyle = new byte[numTiles + 1][numChannels];
			QCC_exp = new int[numTiles + 1][numChannels][][];
			QCC_rev = new boolean[numTiles + 1][numChannels];
			QCC_der = new boolean[numTiles + 1][numChannels];
			QCC_expound = new boolean[numTiles + 1][numChannels];
			QCC_guardBits = new int[numTiles + 1][numChannels];
			QCC_ValSet = new boolean[numTiles + 1][numChannels];
		}

		// Cqcc
		int cComp = (numChannels < 257) ? is.readUnsignedByte() : is.readUnsignedShort();
		QCC_ValSet[tileIdx][cComp] = true;
		
		// Sqcc (quantization style)
		int tmp = is.readUnsignedByte();
		QCC_guardBits[tileIdx][cComp] = ((tmp >> QCC.SQCX_GB_SHIFT) & QCC.SQCX_GB_MSK);
		tmp &= ~(QCC.SQCX_GB_MSK << QCC.SQCX_GB_SHIFT);
		QCC_qStyle[tileIdx][cComp] = (byte)tmp;

		// If main header is being read, set default for component in all tiles
		switch(tmp) {
			case QCC.SQCX_NO_QUANTIZATION:
				QCC_rev[tileIdx][cComp] = true;
				break;
			case QCC.SQCX_SCALAR_DERIVED:
				QCC_der[tileIdx][cComp] = true;
				break;
			case QCC.SQCX_SCALAR_EXPOUNDED:
				QCC_expound[tileIdx][cComp] = true;
				break;
			default:
				break;
		}

		int minb, maxb;
		QCC_exp[tileIdx][cComp] = new int[COD_reslvls + 1][];

		// Loop on resolution levels
		for(int res = 0; res <= COD_reslvls; res++) {
			// Find the number of subbands in the resolution level
			if(res == 0) { // Only the LL subband
				minb = 0;
				maxb = 1;
			}
			else {
				minb = 1;
				maxb = 4;
			}
			QCC_exp[tileIdx][cComp][res] = new int[maxb];

			for(int j = minb; j < maxb; j++) {
				tmp = is.readUnsignedByte();
				QCC_exp[tileIdx][cComp][res][j] = (tmp >> QCC.SQCX_EXP_SHIFT) & QCC.SQCX_EXP_MASK;
			}
		}
	}
	
	private void analyseMainHeader() {
		// do some calcs at the end.
		ntX = (imgOrigX + imageWidth - tilingOrigX + tileW - 1) / tileW;
		ntY = (imgOrigY + imageHeight - tilingOrigY + tileH - 1) / tileH;
		int ctY = 0, ctX = 0, c = 0;
		ctoy = (ctY == 0) ? imgOrigY : tilingOrigY + ctY * tileH; // make calcs for tile 0
		culy = (ctoy + compSubsY[c] - 1) / compSubsY[c];

		ctox = (ctX == 0) ? imgOrigX : tilingOrigX + ctX * tileW;
		culx = (ctox + compSubsX[c] - 1) / compSubsX[c];

		/*int[] rangeBits = */calcMixedBitDepths(origBitDepth, null);

		// arrays to hold our packet information]
		maxRes = COD_reslvls;
		initSubband(); // inits a model of our subband tree : to be used for ALL tiles
		findSubInResLvl();

		tdInclA = new TagTreeDecoder[numChannels][maxRes + 1][][];
		tdBDA = new TagTreeDecoder[numChannels][maxRes + 1][][];
		lblock = new int[numChannels][maxRes + 1][][][];
		sbx = new int[numChannels][maxRes + 1][];
		sby = new int[numChannels][maxRes + 1][];

		for(int st1 = 0; st1 < numChannels; st1++) {
			for(int st2 = 0; st2 <= maxRes; st2++) {
				int ct4 = getPrecMaxX(st1, st2) * getPrecMaxY(st1, st2);

				lblock[st1][st2] = new int[subRange[st2][1] + 1][][];
				sbx[st1][st2] = new int[subRange[st2][1] + 1];
				sby[st1][st2] = new int[subRange[st2][1] + 1];
				tdInclA[st1][st2] = new TagTreeDecoder[subRange[st2][1] + 1][ct4];
				tdBDA[st1][st2] = new TagTreeDecoder[subRange[st2][1] + 1][ct4];

				for(int s = subRange[st2][0]; s <= subRange[st2][1]; s++) {
					sbx[st1][st2][s] = getSBNumCodeBlocksXY(st2, s, true);
					sby[st1][st2][s] = getSBNumCodeBlocksXY(st2, s, false);
					lblock[st1][st2][s] = new int[sbx[st1][st2][s]][sby[st1][st2][s]];
				}

			}
		}

		pkdPktHeaders = new byte[numTiles][];
	}
	protected void initSubband() {
		/*
		 * there is a subband root for every tile and every component.
		 * however, to keep things simple, we will use tile 0,0 and comp 0 for every
		 * tree access.  a spec of our decoder is that all tiles are uniform.
		 */
		int tilex = 0;
		subband = new Subband(
				getCompWidth(0, maxRes, 0),
				getCompHeight(0, maxRes, 0),
				getULX(0, maxRes),
				getULY(0, maxRes), maxRes,
				tilex,
				COD_cblk[0],
				COD_cblk[1]);
	}
	private final void findSubInResLvl() {
		subRange = new int[maxRes + 1][2];
		// Dyadic decomposition
		for(int i = maxRes; i > 0; i--) {
			subRange[i][0] = 1;
			subRange[i][1] = 3;
		}
		subRange[0][0] = 0;
		subRange[0][1] = 0;
	}
	private int getSBNumCodeBlocksXY(int resolution, int ix, boolean isX) {
		int tmp;
		int apox = 0;
		int apoy = 0;
		Subband sb = getSubbandByIdx(resolution, ix);
		if(isX) {
			tmp = sb.ulcx - apox + sb.nomCBlkW;
			return (tmp + sb.w - 1) / sb.nomCBlkW - (tmp / sb.nomCBlkW - 1);
		}
		else {
			tmp = sb.ulcy - apoy + sb.nomCBlkH;
			return (tmp + sb.h - 1) / sb.nomCBlkH - (tmp / sb.nomCBlkH - 1);
		}
	}
	/* start at tree, level Res, and go to down resolution 0 */
	private Subband getSubbandByIdx(int rl, int sbi) {
		Subband sb = subband;
		// go down to specified resolution level.
		while(sb.resLvl > rl) {
			sb = sb.subb_LL;
		}

		// if at reslevl 0, just return, no children
		if(rl == 0) {
			return sb;
		}
		switch(sbi) {
			case 1: return sb.subb_HL;
			case 2: return sb.subb_LH;
			case 3: return sb.subb_HH;
		}
		return null;
	}
	
	protected class PacketHeaders {
	    // arrays to hold Tile header data
	    protected int[][] firstPackOff;

	    /*
	     * CODEBLOCK INFO:: tile | component | resolution | orientation(subband) | y | x | layer
	     * length of each code block
	     */
	    protected short[][][][][][][] CB_len;	// val cannot exceed 4096, short will do
	    protected int[][][][][][][] CB_off;	// offset to each codeblock in stream
	    protected boolean[][][][][][] cbInc;	// for tag tree inclusion
	    protected int[][][][][][][] cbTpLyr; 	// trunc points per layer
	    protected int[][][][][][] msbSk;		// trunc points per layer

	    //starting point in stream from this progression level
	    protected int[][] progPackStart;
		//ending point in stream from this progression level
	    protected int[][] progPackEnd;
	    
		public byte[] getData(int res) throws IOException {
			int start = progPackStart[0][res];
			int end = progPackEnd[0][res];
			byte[] data = new byte[end - start + 1];
			is = new ByteInputStream(stream.createInputStream());
			is.skip(start);
			is.read(data, 0, data.length);
			is.close();
			return data;
		}
	}
	
	//=============================================================================================
	// Tile Header
	//=============================================================================================
	
	private void readTileHeader() throws IOException {
		// init arrays to Ntiles size
		int[] tileParts = new int[numTiles]; // only used in this function call
		int[][] tilePartLen = new int[numTiles][];
		int[][] tilePartNum = new int[numTiles][];
		packetHeaders.firstPackOff = new int[numTiles][];
		int[] tilePartsRead = new int[numTiles];
		int totTilePartsRead = 0;

		// Read all tile-part headers from all tiles.
		int tp = 0, tptot = 0;
		int tile, psot, tilePart, lsot, nrOfTileParts;
		int remainingTileParts = numTiles; // at least as many tile-parts as tiles

		// init our packet header holders

		while(remainingTileParts != 0) {
			lsot = tileBoxLen;

			if(lsot != 10) {
				return; //wrong length for sot marker Isot
			}
			tile = is.readUnsignedShort();
			if(tile > 65534) {
				return; // tile index too high
			}
			// Psot
			psot = is.readInt();
			if(psot < 0) {
				return; // length larger than supported TPsot
			}
			tilePart = is.read();
			// TNsot
			nrOfTileParts = is.read();

			if(tilePart == 0) {
				remainingTileParts += nrOfTileParts - 1;
				tileParts[tile] = nrOfTileParts;
				tilePartLen[tile] = new int[nrOfTileParts];
				tilePartNum[tile] = new int[nrOfTileParts];
				packetHeaders.firstPackOff[tile] = new int[nrOfTileParts];
			}
			// Decode and store the tile-part header (i.e. until a SOD marker is found)
			while(readTileBox(is.readShort(), is.readUnsignedShort(), tile) != 1) {
			}
			tilePartLen[tile][tilePart] = psot;

			tilePartNum[tile][tilePart] = totTilePartsRead;
			totTilePartsRead++;

			tp = tilePartsRead[tile];

			// Set tile part position and header length
			packetHeaders.firstPackOff[tile][tp] = is.getPosition() - 2; // for soc marker and boxlen

			tilePartsRead[tile]++;
			remainingTileParts--;
			tptot++;

		}
	}
	private static interface TileBox {
		public final static short SOD = (short)0xff93;
		public final static short PPT = (short)0xff61;
	}
	private int readTileBox(short marker, int boxLen, int tile) throws IOException {
		switch(marker) {
			case TileBox.SOD: readSOTorSOD(boxLen); return 1;
			case TileBox.PPT: readPPT(boxLen, tile); break;
			//case TileBox.QCC: readQCC(tile+1); break;
			default: // skip it boxLen bytes
				is.skip(boxLen - 2);
				break;
		}
		return 0;
	}
	private void readPPT(int boxlen, int tile) throws IOException {
		// Zppt (index of PPM marker)
		is.readUnsignedByte();
		
		// Ippt (packed packet headers)
		if((pkdPktHeaders[tile] == null) || (pkdPktHeaders[tile].length < boxlen - 3)) {
			pkdPktHeaders[tile] = null;
			pkdPktHeaders[tile] = new byte[boxlen - 3];
		}
		is.read(pkdPktHeaders[tile], 0, boxlen - 3);
	}

	//=============================================================================================
	// Packet Header
	//=============================================================================================
	
	protected final PacketHeaders packetHeaders;
	protected boolean readPacketHeader() throws IOException {
		packetHeaders.CB_len = new short[numTiles][numChannels][maxRes + 1][][][][];
		packetHeaders.CB_off = new int[numTiles][numChannels][maxRes + 1][][][][];
		packetHeaders.cbInc = new boolean[numTiles][numChannels][maxRes + 1][][][]; // for tag tree inclusion
		packetHeaders.cbTpLyr = new int[numTiles][numChannels][maxRes + 1][][][][]; // trunc points per layer
		packetHeaders.msbSk = new int[numTiles][numChannels][maxRes + 1][][][];
		packetHeaders.progPackStart = new int[numTiles][maxRes + 1];
		packetHeaders.progPackEnd = new int[numTiles][maxRes + 1];
		
		// read in the packed packet headers
		for(int k = 0; k < numTiles; k++) {
			parsePacketHeaders(k);
		}

		return true;
	}
	
	private static interface PacketHeader {
		public final static int RES_LY_COMP_POS_PROG = 1;
		public final static int INIT_LBLOCK = 3;
	}
	private PktHeaderBitReader pkthdrbitrdr = new PktHeaderBitReader();
	private void parsePacketHeaders(int theTile) throws IOException {
		// 4 counters
		boolean case1 = true;
		int ct1 = 0, ct2 = 0, ct3 = 0, rl = 0, ly, tmp, tmp2, totnewtp, ctp, m, n;
		int curPos = packetHeaders.firstPackOff[theTile][0]; // for tile 0, offset 0
		pkthdrbitrdr.set(pkdPktHeaders[theTile]);

		switch(COD_Prog) {
			/*case LY_RES_COMP_POS_PROG:
			ct1 = COD_nlayers;// layers
			ct2 = Res + 1;// res levels add 1, to include 0 and 5
			break;
			 */
			case PacketHeader.RES_LY_COMP_POS_PROG:
				ct1 = maxRes + 1;// res levels add 1, to include 0 and 5
				ct2 = COD_nlayers;// layers
				case1 = false;
				break;
			default:
				break;
		}
		ct3 = numChannels; // components

		boolean initTH;
		for(int st1 = 0; st1 < numChannels; st1++) {
			for(int st2 = 0; st2 <= maxRes; st2++) {
				initTH = false;

				if(packetHeaders.CB_len[theTile][st1][st2] == null) {
					initTH = true;
				}
				else {
					try {
						if(packetHeaders.CB_len[theTile][st1][st2][0].length != subRange[st2][1] + 1) {
							initTH = true;
						}
					}
					catch(Exception e) {
						initTH = true;
					}
				}

				if(initTH) {
					packetHeaders.CB_len[theTile][st1][st2] = new short[subRange[st2][1] + 1][][][];
					packetHeaders.CB_off[theTile][st1][st2] = new int[subRange[st2][1] + 1][][][];
					packetHeaders.cbInc[theTile][st1][st2] = new boolean[subRange[st2][1] + 1][][];
					packetHeaders.cbTpLyr[theTile][st1][st2] = new int[subRange[st2][1] + 1][][][];
					packetHeaders.msbSk[theTile][st1][st2] = new int[subRange[st2][1] + 1][][];
				}

				for(int s = subRange[st2][0]; s <= subRange[st2][1]; s++) {
					initTH = false;

					if(packetHeaders.CB_len[theTile][st1][st2][s] == null) {
						initTH = true;
					}
					else {
						try {
							if((packetHeaders.CB_len[theTile][st1][st2][s].length != sbx[st1][st2][s])
									|| (packetHeaders.CB_len[theTile][st1][st2][s][0].length != sby[st1][st2][s])
									|| (packetHeaders.CB_len[theTile][st1][st2][s][0][0].length != COD_nlayers)) {
								initTH = true;
							}
						}
						catch(Exception e) {
							initTH = true;
						}
					}

					if(initTH) {
						try {
							packetHeaders.CB_len[theTile][st1][st2][s] = new short[sbx[st1][st2][s]][sby[st1][st2][s]][COD_nlayers];
							packetHeaders.CB_off[theTile][st1][st2][s] = new int[sbx[st1][st2][s]][sby[st1][st2][s]][COD_nlayers];
							packetHeaders.cbInc[theTile][st1][st2][s] = new boolean[sbx[st1][st2][s]][sby[st1][st2][s]];
							packetHeaders.cbTpLyr[theTile][st1][st2][s] = new int[sbx[st1][st2][s]][sby[st1][st2][s]][COD_nlayers];
							packetHeaders.msbSk[theTile][st1][st2][s] = new int[sbx[st1][st2][s]][sby[st1][st2][s]];
						}
						catch(OutOfMemoryError e) {
							IO.printError("OutOfMemoryError allocating memory for [" + theTile + "][" + st1 + "]["
									+ st2 + "][" + s + "]");
							throw e;
						}
					}

					for(int k = sby[st1][st2][s] - 1; k >= 0; k--) {
						Arrays.fill(lblock[st1][st2][s][k], PacketHeader.INIT_LBLOCK);
						Arrays.fill(packetHeaders.cbInc[theTile][st1][st2][s][k], false);
						Arrays.fill(packetHeaders.msbSk[theTile][st1][st2][s][k], 0);

						for(int jj = 0; jj < sby[st1][st2][s]; jj++) {
							Arrays.fill(packetHeaders.CB_len[theTile][st1][st2][s][k][jj], (short)0);
							Arrays.fill(packetHeaders.CB_off[theTile][st1][st2][s][k][jj], 0);
							Arrays.fill(packetHeaders.cbTpLyr[theTile][st1][st2][s][k][jj], 0);
						}
					}
				}
			}
		}
		for(int st1 = 0; st1 < ct1; st1++) {
			packetHeaders.progPackStart[theTile][st1] = curPos;
			for(int st2 = 0; st2 < ct2; st2++) {
				rl = (case1) ? st2 : st1;
				ly = (case1) ? st1 : st2;
				for(int st3 = 0; st3 < ct3; st3++) {
					final int ct4 = getPrecMaxX(st3, rl) * getPrecMaxY(st3, rl);
					for(int st4 = 0; st4 < ct4; st4++) {
						/***** start 4 tier loop ****/
						// parse the packet header
						pkthdrbitrdr.sync();
						// is the packet empty?

						if(pkthdrbitrdr.readBit() == 0) {
							// no code blocks
							continue;
						}
						// there are code blocks so...
						// loop on subbands
						for(int s = subRange[rl][0]; s <= subRange[rl][1]; s++) {
							if(tdInclA[st3][rl][s][st4] == null) {
								tdInclA[st3][rl][s][st4] = new TagTreeDecoder(sby[st3][rl][s], sbx[st3][rl][s]);
								tdBDA[st3][rl][s][st4] = new TagTreeDecoder(sby[st3][rl][s], sbx[st3][rl][s]);
							}
							else {
								tdInclA[st3][rl][s][st4].set(sby[st3][rl][s], sbx[st3][rl][s]);
								tdBDA[st3][rl][s][st4].set(sby[st3][rl][s], sbx[st3][rl][s]);
							}

							// loop on x,y code blocks
							for(m = 0; m < sby[st3][rl][s]; m++) {
								for(n = 0; n < sbx[st3][rl][s]; n++) {
									// inclusion not set yet ?
									ctp = arraySum(packetHeaders.cbTpLyr[theTile][st3][rl][s][m][n]);

									if((!packetHeaders.cbInc[theTile][st3][rl][s][m][n]) || (ctp == 0)) {

										// Read inclusion using tag-tree
										tmp = tdInclA[st3][rl][s][st4].update(m, n, ly + 1, pkthdrbitrdr);
										if(tmp > ly) { // Not included
											continue;
										}

										// Read bitdepth using tag-tree
										tmp = 1;// initialization
										for(tmp2 = 1; tmp >= tmp2; tmp2++) {
											tmp = tdBDA[st3][rl][s][st4].update(m, n, tmp2, pkthdrbitrdr);
										}
										packetHeaders.msbSk[theTile][st3][rl][s][m][n] = tmp2 - 2;

										totnewtp = 1;
										// set trunc points to 0 for this layer
										packetHeaders.cbTpLyr[theTile][st3][rl][s][m][n][ly] = 0;
										// set inclusion
										packetHeaders.cbInc[theTile][st3][rl][s][m][n] = true;
									}
									else {
										// included?
										if(pkthdrbitrdr.readBit() != 1) {
											continue;
										}
										totnewtp = 1;
									}

									// Read new truncation points
									if(pkthdrbitrdr.readBit() == 1) {// if bit is 1
										totnewtp++;

										// if next bit is 0 do nothing
										if(pkthdrbitrdr.readBit() == 1) {//if is 1
											totnewtp++;

											tmp = pkthdrbitrdr.readBits(2);
											totnewtp += tmp;

											// If next 2 bits are not 11 do nothing
											if(tmp == 0x3) { //if 11
												tmp = pkthdrbitrdr.readBits(5);
												totnewtp += tmp;

												// If next 5 bits are not 11111 do nothing
												if(tmp == 0x1F) {//if 11111
													totnewtp += pkthdrbitrdr.readBits(7);
												}
											}
										}
									}
									packetHeaders.cbTpLyr[theTile][st3][rl][s][m][n][ly] = totnewtp;

									// Code-block length

									while(pkthdrbitrdr.readBit() != 0) {
										lblock[st3][rl][s][m][n]++;
									}
									packetHeaders.CB_len[theTile][st3][rl][s][m][n][ly] = (short)pkthdrbitrdr
											.readBits(lblock[st3][rl][s][m][n] + log2(totnewtp));
									packetHeaders.CB_off[theTile][st3][rl][s][m][n][ly] = curPos;
									curPos += packetHeaders.CB_len[theTile][st3][rl][s][m][n][ly];

								}
							} // end on code block loops
						} // end loop on subbands
						/***** end 4 tier loop ****/
					}
				}
			}
			packetHeaders.progPackEnd[theTile][st1] = curPos;
		}
	}

	//=============================================================================================
	// Some getters and setters
	//=============================================================================================

	public int subbandLL_w(int res) {
		Subband nd = subband;
		while(nd.resLvl != res) {
			nd = nd.subb_LL;
		}
		return nd.w;
	}
	
	/** ctX which tile we are at x, starting at 0 */
	protected final int getCompWidth(int c, int rl, int ctX) {
		int dl = COD_reslvls - rl; // Revert level indexation (0 is hi-res)
		// Calculate starting X of next tile X-wise at reference grid hi-res
		int ntulx = (ctX < ntX - 1) ? tilingOrigX + (ctX + 1) * tileW : imgOrigX + imageWidth;
		// Convert reference grid hi-res to component grid hi-res
		ntulx = (ntulx + compSubsX[c] - 1) / compSubsX[c];
		// Starting X of current tile at component grid hi-res is culx[c]
		// The difference at the rl level is the width
		return (ntulx + (1 << dl) - 1) / (1 << dl) - (culx + (1 << dl) - 1) / (1 << dl);
	}

	protected final int getCompHeight(int c, int rl, int ctY) {
		int dl = COD_reslvls - rl; // Revert level indexation (0 is hi-res)
		// Calculate starting Y of next tile Y-wise at reference grid hi-res
		int ntuly = (ctY < ntY - 1) ? tilingOrigY + (ctY + 1) * tileH : imgOrigY + imageHeight;
		// Convert reference grid hi-res to component grid hi-res
		ntuly = (ntuly + compSubsY[c] - 1) / compSubsY[c];
		// Starting Y of current tile at component grid hi-res is culy[c]
		// The difference at the rl level is the height
		return (ntuly + (1 << dl) - 1) / (1 << dl) - (culy + (1 << dl) - 1) / (1 << dl);
	}

	protected final int getULX(int c, int rl) {
		int dl = COD_reslvls - rl;
		return (culx + (1 << dl) - 1) / (1 << dl);
	}

	protected final int getULY(int c, int rl) {
		int dl = COD_reslvls - rl;
		return (culy + (1 << dl) - 1) / (1 << dl);
	}
	
	/**
	 * and now the fun begins...
	 * our objectives from this parsing is to get the
	 * offsets and lengths to our code blocks.
	 *
	 * packets correspond to a resolution - layer - component - precinct
	 * each precinct contains a number of code passes
	 *
	 * known tile settings for later : curPos , bin
	 */
	private int getPrecMaxX(int c, int r) {
		// Subsampling factors
		int xrsiz = compSubsX[c];

		// Tile's coordinates in the image component domain
		int tx0 = getULX(0, r);
		int tx1 = getCompHeight(0, r, 0);

		int tcx0 = (int)Math.ceil(tx0 / (double)(xrsiz));
		int tcx1 = (int)Math.ceil(tx1 / (double)(xrsiz));

		// Tile's coordinates in the reduced resolution image domain
		int trx0 = (int)Math.ceil(tcx0 / (double)(1 << (maxRes - r)));
		int trx1 = (int)Math.ceil(tcx1 / (double)(1 << (maxRes - r)));

		double twoppx = getPPX(r);
		if(trx1 > trx0) {
			return (int)Math.ceil(trx1 / twoppx) - (int)Math.floor(trx0 / twoppx);
		}
		else {
			return 0;
		}
	}

	private int getPrecMaxY(int c, int r) {
		int yrsiz = compSubsY[c];
		int ty0 = getULY(0, r);
		int ty1 = getCompWidth(0, r, 0);
		int tcy0 = (int)Math.ceil(ty0 / (double)(yrsiz));
		int tcy1 = (int)Math.ceil(ty1 / (double)(yrsiz));

		// Tile's coordinates in the reduced resolution image domain
		int try0 = (int)Math.ceil(tcy0 / (double)(1 << (maxRes - r)));
		int try1 = (int)Math.ceil(tcy1 / (double)(1 << (maxRes - r)));

		double twoppy = getPPY(r);
		if(try1 > try0) {
			return (int)Math.ceil(try1 / twoppy) - (int)Math.floor(try0 / twoppy);
		}
		else {
			return 0;
		}
	}

	private int getPPX(int reslvl) {
		return precW;
	}
	private int getPPY(int reslvl) {
		return precH;
	}

	//=============================================================================================
	// 
	//=============================================================================================


	/* we will only be performing rct transformations */
	protected int[] calcMixedBitDepths(int utdepth[], int tdepth[]) {
		if(tdepth == null) {
			tdepth = new int[utdepth.length];
		}
		if(utdepth.length > 3) {
			System.arraycopy(utdepth, 3, tdepth, 3, utdepth.length - 3);
		}
		tdepth[0] = log2((1 << utdepth[0]) + (2 << utdepth[1]) + (1 << utdepth[2]) - 1) - 2 + 1;
		tdepth[1] = log2((1 << utdepth[2]) + (1 << utdepth[1]) - 1) + 1;
		tdepth[2] = log2((1 << utdepth[0]) + (1 << utdepth[1]) - 1) + 1;

		return tdepth;
	}
	
	//=============================================================================================
	// Utilities
	//=============================================================================================

	private static int log2(int x) {
		int y, v;
		// No log of 0 or negative
		if(x <= 0) {
			throw new IllegalArgumentException("" + x + " <= 0");
		}
		// Calculate log2 (it's actually floor log2)
		v = x;
		y = -1;
		while(v > 0) {
			v >>= 1;
			y++;
		}
		return y;
	}
	private static int arraySum(int[] a) {
		int sum = 0;
		for(int i = 0; i < a.length; i++) {
			sum += a[i];
		}
		return sum;
	}
	
	//=============================================================================================
	// Interface
	//=============================================================================================

	public int getWidth(int res) {
		int width = getWidth();
		for(int i = maxRes; i > res; i--) {
			width /= 2;
		}
		return width;
	}
	public int getHeight(int res) {
		int width = getHeight();
		for(int i = maxRes; i > res; i--) {
			width /= 2;
		}
		return width;
	}
	public int getWidth() {
		assert(tileW == imageWidth);
		return imageWidth;
	}
	public int getHeight() {
		assert(tileH == imageHeight);
		return imageHeight;
	}
	public int getNumChannels() {
		return numChannels;
	}
	public int getNumWavelets() {
		return maxRes + 1;
	}
	
	public void prepareImageData(short[][] dst, byte[] src, int width, int prevWidth, int prevHeight) {
		int m = 0;
		for(int k = 0; k < prevHeight; k++) {
			int dix = k * width;
			for(int j = 0; j < prevWidth; j++) {
				int u0, u1, u2;

				u0 = (src[m++] & 0xFF) - ls1;
				u1 = (src[m++] & 0xFF) - ls2;
				u2 = (src[m++] & 0xFF) - ls3;

				// RCT
				dst[1][dix + j] = (short)(u2 - u1);
				dst[2][dix + j] = (short)(u0 - u1);
				dst[0][dix + j] = (short)(((dst[1][dix + j] + dst[2][dix + j]) >> 2) + u1);
			}
		}
	}
	public void waveletToTexture(byte[] tex, short[][] wavelet, int width, int height) {
		int offset = 0;
		for(int i = 0; i < height; i++) {
			for(int j = 0; j < width; j++) {
				int tmp0, tmp1, tmp2, tmp3, k1;

				k1 = (i * width) + j;

				// RCT
				tmp0 = (wavelet[0][k1] - ((wavelet[1][k1] + wavelet[2][k1]) >> 2));

				tmp1 = (wavelet[2][k1] + tmp0) + ls1;
				tmp2 = (tmp0) + ls2;
				tmp3 = (wavelet[1][k1] + tmp0) + ls3;

				tmp1 = (tmp1 < 0) ? 0 : ((tmp1 > mv1) ? mv1 : tmp1);
				tmp2 = (tmp2 < 0) ? 0 : ((tmp2 > mv2) ? mv2 : tmp2);
				tmp3 = (tmp3 < 0) ? 0 : ((tmp3 > mv3) ? mv3 : tmp3);

				tex[offset++] = (byte)tmp1;
				tex[offset++] = (byte)tmp2;
				tex[offset++] = (byte)tmp3;
			}
		}
	}
}
