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
package org.ptolemy3d.manager;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Unit;
import org.ptolemy3d.debug.IO;
import org.ptolemy3d.globe.ElevationDem;
import org.ptolemy3d.globe.ElevationTin;
import org.ptolemy3d.globe.Layer;
import org.ptolemy3d.globe.MapData;
import org.ptolemy3d.globe.Tile;
import org.ptolemy3d.globe.MapData.MapWavelet;
import org.ptolemy3d.io.BasicCommunicator;
import org.ptolemy3d.io.Communicator;
import org.ptolemy3d.jp2.Jp2Block;
import org.ptolemy3d.jp2.Jp2Decoder;
import org.ptolemy3d.jp2.Jp2Head;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix9d;
import org.ptolemy3d.scene.Landscape;
import org.ptolemy3d.view.Camera;
import org.ptolemy3d.view.CameraMovement;
import org.ptolemy3d.view.Position;

public class Jp2TileLoader implements Runnable {

	private final static int NUMCOMPS = 3;
	protected QuadBufferPool quadBufferPool;
	protected Jp2Head[][] masterH;
	private Jp2Decoder[][] jpd;
	public int[] tileSize;
	public boolean on = false;
	public boolean isSleeping;
	/** Contains all */
	private MapData[/* level */][/* tile:0..LEVEL_NUMTILES */] mapDatas;
	private Jp2Block[/* level */][/* tile:0..LEVEL_NUMTILES */] jp2Blocks;
	private boolean[/* level */][/* tile:0..LEVEL_NUMTILES */] tileLock;
	private boolean[/* level */] found;
	private Communicator[] JC;
	private Communicator JC_APP;
	private short[][] targetData;
	private Vector<Integer>[] texTrash;
	private int[] quadtiles;
	private Thread thread;
	private long idleFrom,  lastIdle;

	// Temporary variables
	private Matrix9d utilMatrix = new Matrix9d();
	private Matrix9d cloneMat = new Matrix9d();

	public Jp2TileLoader() {

		on = true;
		JC = new Communicator[Ptolemy3D.getConfiguration().dataServers.length];

		final int numLevels = Ptolemy3D.getScene().landscape.getLayers().length;

		found = new boolean[Layer.LEVEL_NUMTILES];
		mapDatas = new MapData[numLevels][Layer.LEVEL_NUMTILES];
		jp2Blocks = new Jp2Block[numLevels][Layer.LEVEL_NUMTILES];
		tileLock = new boolean[numLevels][Layer.LEVEL_NUMTILES];

		texTrash = new Vector[MapData.MAX_NUM_RESOLUTION];
		for (int w = 0; w < MapData.MAX_NUM_RESOLUTION; w++) {
			texTrash[w] = new Vector<Integer>(10);
		}

		quadBufferPool = new QuadBufferPool();
	}

	protected final void setServers() {
		JC_APP = null;
		for (int i = 0; i < JC.length; i++) {
			// FIXME
			// JC[i] = new Ptolemy3DSocketCommunicator(ptolemy.settings.Server);
			// if ((!ptolemy.settings.isKeepAlive[i]) ||
			// (!JC[i].test("/Ptolemy3Dkey")))
			// {
			// IO.println("using basic server");
			JC[i] = new BasicCommunicator(Ptolemy3D.getConfiguration().server);
			// }

			if (JC[i] != null) {
				JC[i].setServer(Ptolemy3D.getConfiguration().dataServers[i]);
				JC[i].setHeaderKey(Ptolemy3D.getConfiguration().headerKeys[i]);
				JC[i].setUrlAppend(Ptolemy3D.getConfiguration().urlAppends[i]);
			}
		}
		// look for a match
		// for(int i=0;i<JC.length;i++)
		// if (jetApplet.MDataServer[i].equals(jetApplet.Server))
		// JC_APP = JC[i];

		if (JC_APP == null) {
			JC_APP = new BasicCommunicator(Ptolemy3D.getConfiguration().server);
		}

		// search for a default files for locations

		masterH = new Jp2Head[JC.length][];
		jpd = new Jp2Decoder[JC.length][];

		byte[] hdat = null;
		for (int i = 0; i < JC.length; i++) {
			if (JC[i] == null) {
				continue;
			}

			try {
				if (Ptolemy3D.getConfiguration().locations[i] != null) {
					masterH[i] = new Jp2Head[Ptolemy3D.getConfiguration().locations[i].length];
					jpd[i] = new Jp2Decoder[Ptolemy3D.getConfiguration().locations[i].length];
					for (int h = 0; h < Ptolemy3D.getConfiguration().locations[i].length; h++) {
						JC[i].requestMapData(
								Ptolemy3D.getConfiguration().locations[i][h] + "default.jp2", 0,
								Communicator.HEADERMAXLEN);

						int t = JC[i].getHeaderReturnCode();

						if ((t == 200) || (t == 206)) {
							hdat = JC[i].getData(Communicator.HEADERMAXLEN + 1);
							masterH[i][h] = new Jp2Head(hdat);
							jpd[i][h] = new Jp2Decoder(masterH[i][h]);
						}
						else {
							JC[i].flushNotFound();
							masterH[i][h] = null;
							jpd[i][h] = null;
						}
					}
				}
			}
			catch (IOException e) {
				IO.printError("ERROR: default.jp2 not found.");
				IO.printStack(e);
			}
		}

		// fill in any missing headers

		Jp2Head repHeader = null;
		Jp2Decoder repJpd = null;
		// begin on a per server basis
		for (int i = 0; i < JC.length; i++) {
			repHeader = null;
			if (Ptolemy3D.getConfiguration().locations[i] != null) {
				for (int h = 0; h < Ptolemy3D.getConfiguration().locations[i].length; h++) {
					if (masterH[i][h] != null) {
						repHeader = masterH[i][h];
						repJpd = jpd[i][h];
						break;
					}
				}
				if (repHeader != null) {
					for (int h = 0; h < Ptolemy3D.getConfiguration().locations[i].length; h++) {
						if (masterH[i][h] == null) {
							masterH[i][h] = repHeader;
							jpd[i][h] = repJpd;
						}
					}
				}
			}
		}
		// now just do the rest with first found header
		repHeader = null;
		for (int i = 0; i < masterH.length; i++) {
			if (masterH[i] != null) {
				for (int h = 0; h < masterH[i].length; h++) {
					if (masterH[i][h] != null) {
						repHeader = masterH[i][h];
						repJpd = jpd[i][h];
					}
				}
			}
		}
		for (int i = 0; i < masterH.length; i++) {
			if (masterH[i] != null) {
				for (int h = 0; h < masterH[i].length; h++) {
					if (masterH[i][h] == null) {
						masterH[i][h] = repHeader;
						jpd[i][h] = repJpd;
					}
				}
			}
		}

		if (repHeader == null) {
			IO.println("default j2k file missing. Stopping program.");
		}
		else {
			targetData = new short[NUMCOMPS][repHeader.getImageW() * repHeader.getImageH()];

			tileSize = new int[MapData.MAX_NUM_RESOLUTION];
			quadtiles = new int[MapData.MAX_NUM_RESOLUTION];
			for (int res = 0, j = MapData.MAX_NUM_RESOLUTION - 1; res < MapData.MAX_NUM_RESOLUTION; res++, j--) {
				int nd_w = repHeader.subbandLL_w(res);
				tileSize[res] = nd_w;
				quadtiles[res] = (repHeader.getImageW() / nd_w) * (repHeader.getImageW() / nd_w);
			}

			for (int p = 0; p < tileLock.length; p++) {
				Arrays.fill(tileLock[p], false);
			}
			quadBufferPool.init(quadtiles, tileSize);
		}

	}

	public final void run() {
		setServers();

		if (masterH == null) {
			return;
		}

		boolean isIdle;
		thread = Thread.currentThread();
		thread.setPriority(1);
		isSleeping = false;
		lastIdle = System.currentTimeMillis();
		CameraMovement cameraController = Ptolemy3D.getCanvas().getCameraMovement();

		while (on) {

			// TODO - Is necessari to know if render is active?
//					if (!ptolemy.canvas.isRenderingThreadAlive()) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
			}
//			continue;
//			}

			isIdle = false;
			setHeaders();
			try {
				if (!setData(1) && !setData(0) && (!cameraController.isActive) && (cameraController.inAutoPilot == 0)) {
					isIdle = true;
					idleFrom = System.currentTimeMillis();
					if (idleFrom - lastIdle > 5000) {
						try {
							isSleeping = true;
							if (on) {
								Thread.sleep(9999999);
							}
						}
						catch (InterruptedException e) {
							isSleeping = false;
							isIdle = false;
						}
					}
				}
				if (!isIdle) {
					lastIdle = System.currentTimeMillis();
				}
			}
			catch (IOException e) {
			}

			// run plugins
			if (on) {
				Ptolemy3D.getScene().plugins.tileLoaderAction(JC_APP);
				// sleep the tile loader for smoother flying
			}
			try {
				if (on) {
					final int flightFrameDelay = Ptolemy3D.getConfiguration().flightFrameDelay;
					Thread.sleep(
							((cameraController.isActive || (cameraController.inAutoPilot > 0)) && on) ? flightFrameDelay
									: 1, 0);
				}
			}
			catch (InterruptedException e) {
			}
		}

		// Free memory
		clearTextureData();
		quadBufferPool.release();
		System.gc();
		// Stop connections
		dropConnections();

		// Wait while texture
		while (Ptolemy3D.getTextureManager() != null) {
			try {
				Thread.sleep(50);
			}
			catch (InterruptedException e) {
			}
		}
		Ptolemy3D.setTileLoader(null);
	}

	protected final void clearTextureData() {
		if (mapDatas == null) {
			return;
		}
		final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();
		for (int i = 0; i < mapDatas.length; i++) {
			for (int j = 0; j < mapDatas[i].length; j++) {
				final MapData mapData = mapDatas[i][j];
				if (mapData == null) {
					continue;
				}
				mapDataManager.remove(mapData);
				mapDatas[i][j] = null;
				jp2Blocks[i][j] = null;
			}
			mapDatas[i] = null;
			jp2Blocks[i] = null;
		}
		mapDatas = null;
		jp2Blocks = null;
	}

	private final boolean setData(int getVisible) throws IOException {
		final Layer[] levels = Ptolemy3D.getScene().landscape.getLayers();

		int jp2DatServer;
		int locIndex;
		int res = 0;
		// make requests for data not gotten
		byte[] data;

		final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();
		
		final Camera camera = Ptolemy3D.getCanvas().getCamera();
		camera.vpMat.copyTo(utilMatrix);
		utilMatrix.invert(cloneMat);

		if ((levels.length > 3) && (quadBufferPool.full >= 0)) {
loop: 		for (int p = 0; p < levels.length; p++) {
				final Layer level = levels[p];
				if (!level.isStatus()) {
					for (int j = 0; j < mapDatas[p].length; j++) {
						final MapData tile = mapDatas[p][j];
						final MapWavelet tileLevel = tile.getWavelet(quadBufferPool.full);
						if (tileLevel.imageDataReady) {
							mapDataManager.dropTo(tile, quadBufferPool.full - 1);
							if (quadBufferPool.full == -1) { // 08-2008 fix
								break loop;
							}
							break;
						}
					}
				}
			}
		}

		for (int p = 0; p < levels.length; p++) {
			final Layer level = levels[p];
			if (!level.isStatus()) {
				continue;
			}

			/****** headers ***********/
			int tileId = getClosestTile(1, p, getVisible, 0);
			// IO.println("Level:" + p + " TILE:"+tileId);
			if (tileId != -1) {
				data = null;
				locIndex = 0;
				jp2DatServer = 0;
				while ((data == null) && (jp2DatServer < JC.length)) {
					if ((JC[jp2DatServer] == null) || (Ptolemy3D.getConfiguration().locations[jp2DatServer] == null)) {
						jp2DatServer++;
						continue;
					}

					JC[jp2DatServer].requestMapData(
							Ptolemy3D.getConfiguration().locations[jp2DatServer][locIndex] + mapDatas[p][tileId].fileBase + ".jp2", 0,
							Communicator.HEADERMAXLEN);
					int ret = JC[jp2DatServer].getHeaderReturnCode();
					if ((ret == 200) || (ret == 206)) {
						data = JC[jp2DatServer].getData(Communicator.HEADERMAXLEN + 1);
					}
					else {
						JC[jp2DatServer].flushNotFound();
						data = null;
					}
					if (data != null) {
						mapDatas[p][tileId].dataServerID = jp2DatServer;
						mapDatas[p][tileId].dataLocID = locIndex;
						break;
					}

					locIndex++;
					if (locIndex >= Ptolemy3D.getConfiguration().locations[jp2DatServer].length) {
						locIndex = 0;
						jp2DatServer++;
					}
				}
				if (data != null) {
					try {
						masterH[mapDatas[p][tileId].dataServerID][mapDatas[p][tileId].dataLocID].setTileHeader(jp2Blocks[p][tileId], data);
						mapDatas[p][tileId].hasData = true;
					}
					catch (Exception ex) {
						IO.printStackConnection(ex);
						System.err.println("TileLoader.setData(): " + ex.toString());
						System.err.println("File: " + Ptolemy3D.getConfiguration().locations[jp2DatServer][locIndex] + mapDatas[p][tileId].fileBase + ".jp2");
					}
				}
				mapDatas[p][tileId].mapRequest = true;
			}

			/***** : dem ****************/
			// tile = (JSlandscape.terrainEnabled) ? getClosestTile(3, p,
			// getVisible,0) : -1;
			if (tileId != -1) {
				boolean elevationSet = false;
				locIndex = 0;
				jp2DatServer = 0;
				while (!elevationSet && (jp2DatServer < JC.length)) {
					if ((JC[jp2DatServer] == null) || (Ptolemy3D.getConfiguration().DEMLocation[jp2DatServer] == null)) {
						jp2DatServer++;
						continue;
					}
					int ret = -1;
					boolean isTin = true;
					final boolean USE_TIN = Ptolemy3D.getConfiguration().useTIN;
					if (USE_TIN) {
						JC[jp2DatServer].requestMapData(Ptolemy3D.getConfiguration().DEMLocation[jp2DatServer][locIndex] + "tin/" + mapDatas[p][tileId].fileBase + ".tin");
						ret = JC[jp2DatServer].getHeaderReturnCode();
					}

					if ((ret != 200) && (ret != 206)) {
						if (USE_TIN) {
							JC[jp2DatServer].flushNotFound();
							// len = (JSlandscape.DEMSize[p] *
							// JSlandscape.DEMSize[p] * 2)-1;
							// JC[jp2DatServer].requestMapData(jetApplet.DEMLocation[jp2DatServer][locIndex]
							// + jp2tiles[p][tile].filebase + ".bdm" , 0 , len);
						}
						JC[jp2DatServer].requestMapData(Ptolemy3D.getConfiguration().DEMLocation[jp2DatServer][locIndex] + mapDatas[p][tileId].fileBase + ".bdm");
						isTin = false;
						ret = JC[jp2DatServer].getHeaderReturnCode();
					}

					if ((ret == 200) || (ret == 206)) {
						if (isTin) {
							mapDatas[p][tileId].tin = new ElevationTin(
									JC[jp2DatServer].getData());
						}
						else {
							mapDatas[p][tileId].dem = new ElevationDem(
									JC[jp2DatServer].getData());
						}
						elevationSet = true;
					}
					else {
						JC[jp2DatServer].flushNotFound();
					}

					locIndex++;
					if (locIndex >= Ptolemy3D.getConfiguration().DEMLocation[jp2DatServer].length) {
						locIndex = 0;
						jp2DatServer++;
					}
				}
				mapDatas[p][tileId].elevationRequest = true;
			}

			/**************************************************************************/
			int quads;
			double tdist, dist, furthest = -1;
			int fix, bix;

			for (res = 0; res < MapData.MAX_NUM_RESOLUTION; res++) {
				if (p < (levels.length - 1)) {
					if (res == (MapData.MAX_NUM_RESOLUTION - 1)) {
						if (levels[p + 1].isVisible()) {
							continue;
						}
					}
				}
				tileId = getClosestTile(2, p, 1, res);
				quads = 0;
				if ((res > 0) && (tileId != -1)) {
					tdist = getDistanceTileCenter(camera.getCameraX(),
							camera.getCameraY(), p, tileId);
					// tangle = getAngleTileCenter(p,tile);
					fix = -1;
					furthest = -1;
					bix = -1;
					for (int j = 0; j < mapDatas[p].length; j++) {
						final MapData tile = mapDatas[p][j];
						if (tile == null) {
							continue;
						}
						final MapWavelet tileLevel = tile.getWavelet(res);
						if (tileLevel.tileGotten) {
							if (!tileInView(p, j)) {
								bix = j;
							}
							dist = getDistanceTileCenter(camera.getCameraX(), camera.getCameraY(),
									p, j);
							if (dist > furthest) {
								furthest = dist;
								fix = j;
							}
							quads++;
						}
					}

					if (quads >= quadtiles[res]) {
						if ((tdist < furthest) || (bix != -1)) {
							fix = (bix != -1) ? bix : fix;
							for (int q = res; q < MapData.MAX_NUM_RESOLUTION; q++) {
								final MapData tile = mapDatas[p][fix];
								final MapWavelet tileLevel = tile.getWavelet(q);

								tileLevel.tileGotten = false;
								if (tileLevel.textureID != -1) {
									addToTrash(tileLevel.textureID, q);
								}
							}
							mapDataManager.dropTo(mapDatas[p][fix], res - 1);
						}
						else {
							tileId = -1;
						}
					}
				} // if ((res > 0) && (tile != -1)){

				if (tileId != -1) {
					boolean isSet = false;
					if ((!mapDatas[p][tileId].hasData) && (Ptolemy3D.getConfiguration().backgroundImageUrl != null)) {
						// try to grab a png image
						data = null;
						locIndex = 0;
						jp2DatServer = 0;
						while ((data == null) && (jp2DatServer < JC.length)) {
							if ((JC[jp2DatServer] == null) || (Ptolemy3D.getConfiguration().locations[jp2DatServer] == null)) {
								jp2DatServer++;
								continue;
							}

							final MapData mapData = mapDatas[p][tileId];
							final String s = Ptolemy3D.getConfiguration().backgroundImageUrl +
							"&lat=" + mapData.getLat() + "&lon=" + mapData.getLon() + "&scale=" + mapData.getMapSize() +
							"&w=" + tileSize[res] + "&h=" + tileSize[res] + "&res=" + res;

							JC[jp2DatServer].requestMapData(s);
							int t = JC[jp2DatServer].getHeaderReturnCode();
							if ((t == 200) || (t == 206)) {
								data = JC[jp2DatServer].getData();
							}
							else {
								JC[jp2DatServer].flushNotFound();
								data = null;
							}

							locIndex++;
							if (locIndex >= Ptolemy3D.getConfiguration().locations[jp2DatServer].length) {
								locIndex = 0;
								jp2DatServer++;
							}
						}
						if (data != null) {		//FIXME PngDecoder
//							int[] meta = new int[5];
//							byte[] pngdata = PngDecoder.decode(data, meta);
//							// process png file.
//							if((meta[2] == 3) || (meta[2] == 2)) { // only rgb color models used
//								isSet = jp2Tiles[p][tileId].setImageData(pngdata, meta, res);
//							}
						}
					}
					else {
						final int dataServerId = mapDatas[p][tileId].dataServerID;
						final int dataLocId = mapDatas[p][tileId].dataLocID;

						jp2Blocks[p][tileId].requestMapData(
								JC[dataServerId],
								Ptolemy3D.getConfiguration().locations[dataServerId][dataLocId] + mapDatas[p][tileId].fileBase + ".jp2", res);

						int ret = JC[dataServerId].getHeaderReturnCode();
						if ((ret == 200) || (ret == 206)) {
							data = jp2Blocks[p][tileId].getData(
									JC[dataServerId], res);
						}
						else {
							JC[dataServerId].flushNotFound();
							data = null;
						}

						if (data != null) {
							final Jp2Head jp2Head = masterH[dataServerId][dataLocId];

							if (res > 0) {
								// clear data array
								Arrays.fill(targetData[0], (short) 0);
								Arrays.fill(targetData[1], (short) 0);
								Arrays.fill(targetData[2], (short) 0);

								int prevRes = res - 1;
								byte[] imgData = mapDataManager.getImageData(mapDatas[p][tileId], prevRes);
								if (imgData == null) {
									mapDataManager.dropTo(mapDatas[p][tileId], -1);
									break;
								}

								// copy the data from this buffer to the target array
								jp2Head.prepareImageData(targetData, imgData, tileSize[res], tileSize[prevRes]);
							}
							jpd[dataServerId][dataLocId].getNextResolution(jp2Blocks[p][tileId], 0, targetData, res, data);
							isSet = mapDataManager.setImageData(mapDatas[p][tileId], targetData, res, jp2Head);
						}
					}
					mapDatas[p][tileId].getWavelet(res).tileGotten = isSet;
					return true;
				}
			}
		}
		return false;
	}

	private final int getClosestTile(int type, int lvl, int isVisible, int res) {
		Landscape landscape = Ptolemy3D.getScene().landscape;

		// IO.println("GET CLOSEST TILE:" + type + " " + lvl + " " + isVisible +
		// " " + res);
		double dist, closest = Double.MAX_VALUE;
		int six = -1;
		boolean inView = true;
		boolean doCheck = false;
		Camera camera = Ptolemy3D.getCanvas().getCamera();

		for (int j = 0; j < mapDatas[lvl].length; j++) {
			final MapData tile = mapDatas[lvl][j];
			if (tile == null) {
				continue;
			}
			doCheck = false;
			switch (type) {
				case 1: // header
				if (!tile.mapRequest) {
					doCheck = true;
				}
				break;
				case 2: // jp2
					if ((!tile.getWavelet(res).tileGotten) && ((tile.hasData) || ((Ptolemy3D.getConfiguration().backgroundImageUrl != null) && (res <= 2) && (tile.mapRequest)))) {
						if (tile.elevationRequest || (!landscape.isTerrainEnabled())) {
							doCheck = true;
							if ((res > 1) && (!tile.getWavelet(res - 1).tileGotten)) {
								doCheck = false;
							}
						}
					}
					break;
				case 3:
					if (!tile.elevationRequest) {
						doCheck = true;
					}
					break;
			}

			if (doCheck) {
				if (isVisible > 0) {
					inView = tileInView(lvl, j);
				}
				else {
					inView = true;
				}

				if (inView) {
					dist = getDistanceTileCenter(camera.getCameraX(),
							camera.getCameraY(), lvl, j);
					if (dist < closest) {
						closest = dist;
						six = j;
					}
				}
			}
		}
		return six;
	}

	private double getDistanceTileCenter(double x, double z, int p, int j) {
		final Landscape landscape = Ptolemy3D.getScene().landscape;
		final Layer[] levels = landscape.getLayers();

		double tx, tz, dist;

		tx = mapDatas[p][j].getLon() + (levels[p].getTileSize() / 2);
		tz = mapDatas[p][j].getLat() + (levels[p].getTileSize() / 2);

		dist = Math3D.distance2D(x, tx, z, tz);

		double dist2;

		if (x < 0) {
			tx = x + (landscape.getMaxLongitude() - mapDatas[p][j].getLon()) + (x + landscape.getMaxLongitude());
		}
		else {
			tx = x + (landscape.getMaxLongitude() - x) + (mapDatas[p][j].getLon() + landscape.getMaxLongitude());
		}

		dist2 = Math3D.distance2D(x, tx, z, tz);
		return ((dist < dist2) ? dist : dist2);
	}

	private boolean tileInView(int lvl, int j) {
		final Layer level = Ptolemy3D.getScene().landscape.getLayers()[lvl];
		final Camera camera = Ptolemy3D.getCanvas().getCamera();
		final Position latLonAlt = camera.getPosition();

		int MAXANGLE = 50;
		{
			// above ?
					if ((latLonAlt.getLongitudeDD() >= mapDatas[lvl][j].getLon()) && (latLonAlt.getLongitudeDD() <= mapDatas[lvl][j].getLon() + level.getTileSize()) && (latLonAlt.getLatitudeDD() <= mapDatas[lvl][j].getLat()) && (latLonAlt.getLatitudeDD() >= mapDatas[lvl][j].getLat() - level.getTileSize())) {
						return true;
					}
					int k;
					double posy, posz, cy, cz;
					posy = -Math.sin(camera.getPitchRadians());
					posz = -Math.cos(camera.getPitchRadians());
					cy = Math.sin(camera.getPitchRadians()) * latLonAlt.getAltitudeDD();
					cz = Math.cos(camera.getPitchRadians()) * latLonAlt.getAltitudeDD();
					double[] coord = new double[3];

					Math3D.setSphericalCoord(
							((latLonAlt.getLongitudeDD() < mapDatas[lvl][j].getLon()) ? mapDatas[lvl][j].getLon()
									: (latLonAlt.getLongitudeDD() > (mapDatas[lvl][j].getLon() + level.getTileSize())) ? (mapDatas[lvl][j].getLon() + level.getTileSize())
											: latLonAlt.getLongitudeDD()),
											((latLonAlt.getLatitudeDD() > mapDatas[lvl][j].getLat()) ? mapDatas[lvl][j].getLat()
													: (latLonAlt.getLatitudeDD() < (mapDatas[lvl][j].getLat() - level.getTileSize())) ? (mapDatas[lvl][j].getLat() - level.getTileSize())
															: latLonAlt.getLatitudeDD()), coord);

					utilMatrix.transform(coord);
					for (k = 0; k < 3; k++) {
						coord[k] = (coord[k] * Unit.EARTH_RADIUS);
					}
					if ((coord[2] > 0) && (Math3D.angle3dvec(0, posy, posz, coord[0], coord[1] - cy, coord[2] - (Unit.EARTH_RADIUS + cz), true) <= MAXANGLE)) {
						return true;
					}
					return false;
		}
	}

	private boolean checkInScene(int p, double mx, double mz) {
		final Layer level = Ptolemy3D.getScene().landscape.getLayers()[p];

		final MapData[] tiles = mapDatas[p];
		for (int q = 0; q < tiles.length; q++) {
			final MapData tile = tiles[q];
			if ((tile.getMapSize() == level.getTileSize()) && (tile.getLon() == mx) && (tile.getLat() == mz)) {
				tile.inScene = true;
				return true;
			}
		}
		return false;
	}

	private final void setHeaders() {
		final Layer[] levels = Ptolemy3D.getScene().landscape.getLayers();
		final MapDataManager mapDataManager = Ptolemy3D.getMapDataManager();

		Arrays.fill(found, false);

		int q;
		for (int p = 0; p < levels.length; p++) {
			final Layer level = levels[p];

			if (mapDatas != null) {
				final MapData[] tiles = mapDatas[p];
				for (q = 0; q < Layer.LEVEL_NUMTILES; q++) {
					final MapData tile = tiles[q];
					if (tile != null) {
						tile.inScene = false;
					}
				}
			}

			for (int k = 0; k < level.getTiles().length; k++) {
				if (mapDatas[p][k] == null) {
					mapDatas[p][k] = mapDataManager.get(p, level.getTileSize(), level.getTiles()[k].getFmx(), level.getTiles()[k].getFmz());
					jp2Blocks[p][k] = new Jp2Block();
					jp2Blocks[p][k].fileBase = mapDatas[p][k].fileBase;
					found[k] = true;
				}
				else {
					found[k] = checkInScene(p, level.getTiles()[k].getFmx(), level.getTiles()[k].getFmz());
				}
			}
			// make a second pass, fill in tiles that were out of the scene.
			q = 0;
			for (int k = 0; k < found.length; k++) {
				if (!found[k]) {
					for (; q < mapDatas[p].length; q++) {
						final MapData mapData = mapDatas[p][q];
						if (!mapData.inScene) {
//							mapDataManager.remove(mapData);
//							mapDatas[p][q] = mapDataManager.get(p, level.getTileSize(), level.getTiles()[k].getFmx(), level.getTiles()[k].getFmz());
//							jp2Blocks[p][q] = new Jp2Block();
//							jp2Blocks[p][q].fileBase = mapDatas[p][q].fileBase;
							
							mapDataManager.moveToTrash(mapData);
							mapData.set(p, level.getTileSize(), level.getTiles()[k].getFmx(), level.getTiles()[k].getFmz());
							final Jp2Block jp2Header = jp2Blocks[p][q];
							jp2Header.fileBase = mapData.fileBase;
							
							mapData.inScene = true;
							break;
						}
					}
				}
			}
		}
	}

	public final MapData getHeader(double x, double z, int lvl) {
		final Layer level = Ptolemy3D.getScene().landscape.getLayers()[lvl];

		for (int m = 0; m < mapDatas[lvl].length; m++) {
			if ((mapDatas[lvl][m] != null) && (mapDatas[lvl][m].getLon() <= x) &&
					((mapDatas[lvl][m].getLon() + level.getTileSize()) > x) && (mapDatas[lvl][m].getLat() >= z) &&
					((mapDatas[lvl][m].getLat() - level.getTileSize()) < z)) {
				return mapDatas[lvl][m];
			}
		}
		return null;
	}

	/**
	 * Search for jp2 tile downloaded.
	 *
	 * @param getLon()
	 *            longitude in DD
	 * @param getLat()
	 *            latitude in DD
	 * @param all
	 *            true to search for tile without any image data
	 * @return a jp2 tile, if any has been found.
	 */
	public MapData getJp2Tile(double lon, double lat, boolean all) {
		final Layer[] levels = Ptolemy3D.getScene().landscape.getLayers();
		final int lvl = levels.length - 1;
		final Layer level = levels[lvl];

		for (int m = 0; m < mapDatas[lvl].length; m++) {
			if ((mapDatas[lvl][m] != null) && (mapDatas[lvl][m].getLon() <= lon) && ((mapDatas[lvl][m].getLon() + level.getTileSize()) > lon) && (mapDatas[lvl][m].getLat() >= lat) && ((mapDatas[lvl][m].getLat() - level.getTileSize()) < lat) && ((mapDatas[lvl][m].getWavelet(0).imageDataReady) || (all))) {
				return mapDatas[lvl][m];
			}
		}
		return null;
	}

	protected MapData getAlternateData(Tile tile) {
		final MapData orig = tile.mapData;
		final double x = tile.getFmx();
		final double z = tile.getFmz();
		final int lvl = tile.getLevelID();

		if (lvl == 0) {
			return null;
			// find which tile is below me
		}

		final Layer[] levels = Ptolemy3D.getScene().landscape.getLayers();
		int below = lvl - 1;

		while (true) {
			final Layer levelBelow = levels[below];

			for (int m = 0; m < mapDatas[below].length; m++) {
				if ((mapDatas[below][m] != null) && (mapDatas[below][m].getLon() <= x) && ((mapDatas[below][m].getLon() + levelBelow.getTileSize()) > x) && (mapDatas[below][m].getLat() >= z) && ((mapDatas[below][m].getLat() - levelBelow.getTileSize()) < z)) {
					if (mapDatas[below][m].getWavelet(0).imageDataReady) {
						return mapDatas[below][m];
					}
					else // can't be more than 1
					{
						break;
					}
				}
			}
			if (below == 0) {
				break;
			}
			below--;
		}
		// if nothing is found, return the original header for dem data.
		return orig;
	}

	protected final void addToTrash(int tid, int res) {
		texTrash[res].add(tid);
	}

	public synchronized void getTextureTrash(Vector<Integer>[] copy) {
		for (int i = 0; i < texTrash.length; i++) {
			final Vector<Integer> src = texTrash[i];
			final Vector<Integer> dst = copy[i];
			dst.clear();
			if (src.size() > 0) {
				dst.addAll(src);
			}
			src.clear();
		}
	}

	/**
	 * @return true if the level has at least one jp2 tile with valid image data.
	 */
	public synchronized final boolean levelHasImageData(int lvl) {
		if (mapDatas == null) {
			return false;
		}

		final MapData[] tiles = mapDatas[lvl];
		if (tiles == null) {
			return false;
		}

		for (int i = 0; i < tiles.length; i++) {
			final MapData tile = tiles[i];
			if ((tile != null) && tile.getWavelet(0).imageDataReady) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return he number of bytes read by the download services.
	 *
	 * @return the number of bytes read by the download services.
	 */
	public double getBytesRead(int server) {
		if (server == -1) {
			double tot = 0;
			for (int i = 0; i < JC.length; i++) {
				if (JC[i] != null) {
					tot += JC[i].getBytesRead();
				}
			}
			return tot;
		}
		return JC[server].getBytesRead();
	}

	/** Reset the number of bytes read by the download services. */
	public void clearBytesRead(int server) {
		if (server == -1) {
			for (int i = 0; i < JC.length; i++) {
				if (JC[i] != null) {
					JC[i].clearBytesRead();
				}
			}
		}
		else {
			JC[server].clearBytesRead();
		}
	}

	public void setServerHeaderKey(int server_id, String key) {
		if ((server_id > 0) && (server_id < JC.length) && (JC[server_id] != null)) {
			JC[server_id].setHeaderKey(key);
		}
	}

	public void updateUrlAppend(int server_id, String urlapp) {
		if ((server_id > 0) && (server_id < JC.length) && (JC[server_id] != null)) {
			JC[server_id].setUrlAppend(urlapp);
		}
	}

	protected void dropConnections() {
		for (int curServer = 0; curServer < JC.length; curServer++) {
			if (JC[curServer] != null) {
				JC[curServer].endSocket();
				// JC[curServer] = null;
			}
		}
	}

	/** Acquire jp2 tile for the specified level.
	 * @param lastLevel true is the level is the last. */
	public int acquireTile(Layer level, boolean lastLevel)
	{
		int numTilesAssigned = 0;

		final Tile[] tiles = level.getTiles();
		for (int i = 0; i < Layer.LEVEL_NUMTILES; i++) {
			final Tile tile = tiles[i];

			tile.mapData = getHeader(tile.getFmx(), tile.getFmz(), tile.getLevelID());
			if ((tile.mapData == null) || (!tile.isImageDataReady(0) && !lastLevel)) {
				tile.mapData = getAlternateData(tile);
			}
			if ((tile.mapData != null) && (tile.mapData.getNumWavelets() > 0)){
				numTilesAssigned++;
			}
		}

		return numTilesAssigned;
	}
}