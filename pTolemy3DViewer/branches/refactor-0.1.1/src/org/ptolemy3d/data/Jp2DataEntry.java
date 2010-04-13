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
package org.ptolemy3d.data;

import java.lang.ref.SoftReference;
import java.net.URL;
import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.io.DataFinder;
import org.ptolemy3d.io.Stream;
import org.ptolemy3d.jp2.Decoder;
import org.ptolemy3d.jp2.fast.JJ2000FastDecoder;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
class Jp2DataEntry extends MapDataEntry {
	public final static int NUM_DECODERUNIT = MapData.MAX_NUM_RESOLUTION;
	public static int decodedAccumulatedTime = 0;
	
	/** Decoder */
	private SoftReference<Decoder> decoderSoftRef;
	/** Number of wavelets to decode */
	private int numWavelets;
	
	public Jp2DataEntry(MapData mapData) {
		super(mapData);
		numWavelets = -1;
	}
	
	@Override
	public URL findData(MapData mapData) {
		final DataFinder dataFinder = Ptolemy3D.getDataFinder();
		return dataFinder.findJp2(mapData);
	}

	@Override
	public Stream findDataFromCache(MapData mapData) {
		final DataFinder dataFinder = Ptolemy3D.getDataFinder();
		return dataFinder.findJp2FromCache(mapData);
	}
	
	private Decoder getOrCreateDecoder(Stream stream) {
		if((decoderSoftRef == null) || (decoderSoftRef.get() == null)) {
			Decoder decoder = new JJ2000FastDecoder(stream);	//A lighter / faster version of JJ200 Decoder
			//Decoder decoder = new JJ2000Decoder(stream);		//JJ200 Decoder
			numWavelets = decoder.getNumWavelets();
			decoderSoftRef = new SoftReference<Decoder>(decoder);
		}
		return decoderSoftRef.get();
	}

	@Override
	public boolean decode() {
		if(!isJustDownloaded()) {
			return false;
		}
		final Stream stream = getStream();
		
		int nextResolution = getNextDecoderUnit();
		if((numWavelets >= 0) && (nextResolution >= numWavelets)) {
			return true;
		}
		final Decoder decoder = getOrCreateDecoder(stream);
		
		Texture texture = null;
//		for(int res = 4; res >= nextResolution; res--) {
//			texture = readWavelet(stream, res);
//			if(texture != null) {
//				nextResolution = res;
//				break;
//			}
//		}
		if(texture == null) {
			IO.printfParser("Parse wavelet: %s@%d/%d\n", mapData.key, (nextResolution + 1), numWavelets);
			long start = System.currentTimeMillis();
			texture = decoder.parseWavelet(nextResolution);
			long duration = (System.currentTimeMillis() - start);
			decodedAccumulatedTime += duration;
			IO.printlnParser("extract: wavelet "+nextResolution+" in "+duration+" ms ["+decodedAccumulatedTime+"]");
		}
		boolean decoded = (texture != null);
		if(decoded) {
			mapData.newTexture = texture;
			mapData.mapResolution = nextResolution;
//			writeWavelet(stream, texture, nextResolution);
		}
		else {
			//Invalidate cache
			stream.invalidateCache();
		}
		// Check if the decoded has more map
		if(nextResolution >= numWavelets) {
			IO.printlnParser("Decoding finished ...");
			decoderSoftRef = null;
		}
		return decoded;
	}
	
	/* Convert jp2 to another format in the cache */
//	private boolean save = false;
//	private boolean saveInTga = false;
//	private boolean saveUseGZ = false;
//	private Texture readWavelet(Stream stream, int resolution) {
//		if (!save || resolution < 3) {
//			return null;
//		}
//		
//		try {
//			final String ext = getWaveletUncompressedExt();
//			final File texFile = new File(stream.createFile() + ext);
//			
//			long start = System.currentTimeMillis();
//			
//			InputStream fis = new BufferedInputStream(new FileInputStream(texFile));
//			if (saveUseGZ) {
//				fis = new GZIPInputStream(fis);
//			}
//			
//			Texture texture = null;
//			if (saveInTga) {
//				org.ptolemy3d.tga.Tga tga = new org.ptolemy3d.tga.Tga();
//				texture = tga.read(fis);
//			}
//			/*else if (saveInDds) {
//				org.ptolemy3d.dds.Dds dds = new org.ptolemy3d.dds.Dds();
//				texture = dds.read(fis);
//			}*/
//			else {
//				DataInputStream is = new DataInputStream(fis);
//				int width = is.readInt();
//				int height = is.readInt();
//				int length = is.readInt();
//				byte[] pixels = new byte[length];
//				int offset = 0;
//				while(length > 0) {
//					int read = is.read(pixels, offset, length);
//					if(read > 0) {
//						length -= read;
//						offset += read;
//					}
//					else if(read == -1) {
//						break;
//					}
//				}
//				texture = new Texture(pixels, width, height);
//			}
//			fis.close();
//			
//			long duration = (System.currentTimeMillis() - start);
//			System.out.println("read: "+texFile.getName()+" in "+duration+" ms");
//			
//			return texture;
//		} catch (Throwable e) {
//			return null;
//		}
//	}
//	private void writeWavelet(Stream stream, Texture texture, int resolution) {
//		final String ext = getWaveletUncompressedExt();
//		File dest = new File(stream.createFile() + ext);
//		if (!save || (resolution < 3) || dest.exists()) {
//			return;
//		}
//		
//		try {
//			long start = System.currentTimeMillis();
//			
//			final File texFile = File.createTempFile("tex-dump", ext);
//			OutputStream fos = new BufferedOutputStream(new FileOutputStream(texFile));
//			if (saveUseGZ) {
//				fos = new GZIPOutputStream(fos);
//			}
//			
//			if (saveInTga) {
//				org.ptolemy3d.tga.Tga tga = new org.ptolemy3d.tga.Tga(texture);
//				tga.write(fos);
//			}
//			/*else if (saveInDds) {
//				org.ptolemy3d.dds.Dds dds = new org.ptolemy3d.dds.Dds(texture);
//				dds.write(fos);
//			}*/
//			else {
//				DataOutputStream os = new DataOutputStream(fos);
//				os.writeInt(texture.width);
//				os.writeInt(texture.height);
//				os.writeInt(texture.pixels.length);
//				os.write(texture.pixels, 0, texture.pixels.length);
//			}
//			fos.close();
//			
//			long duration = (System.currentTimeMillis() - start);
//			System.out.println("write: "+dest.getName()+" in "+duration+" ms");
//			
//			texFile.renameTo(dest);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	private String getWaveletUncompressedExt() {
//		String ext;
//		if (saveInTga) {
//			ext = ".tga";
//		}
//		/*else if (saveInDds) {
//			ext = ".dds";
//		}*/
//		else {
//			ext = ".tex";
//		}
//		if (saveUseGZ) {
//			ext += ".gz";
//		}
//		return ext;
//	}

	@Override
	public int getNextDecoderUnit() {
		final int next = mapData.mapResolution + 1;
		if(next >= NUM_DECODERUNIT) {
			return -1;
		}
		return next;
	}

	@Override
	public boolean isDecoded(int unit) {
		return false;
	}
}
