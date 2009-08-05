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
package org.ptolemy3d.tool.batch;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

import fortune.Fortune;

class DemConvertor {

	MeshStrip meshstrips;
	public float[] dem = null;
	public double[] coordx = null;
	public double[] coordy = null;
	double[] sig;
	public int[] inside_ix;
	public int[][] wall_ix;
	public int[] corner_ix = new int[4];
	public boolean verbose = true;
	double cellw;
	int numTinPoints = 0;
	double tx = 0;
	double ty = 0;
	int tile_width = 0;
	boolean needsTriangleUpdate;

	public DemConvertor(float[] dem, int width, double tx, double ty) {
		this.dem = dem;

		this.tile_width = width;
		this.tx = tx;
		this.ty = ty;

		needsTriangleUpdate = true;
		buildTinPointsFromDem();

		// test sig
		cellw = width / Math.sqrt(dem.length);
	}

	private void buildTinPointsFromDem() {

		int rows = (int) Math.sqrt(dem.length);
		double inc = tile_width / (rows - 1);

		coordx = new double[dem.length];
		coordy = new double[dem.length];
		sig = new double[dem.length];

		int ix;
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < rows; j++) {
				ix = i * rows + j;
				coordx[ix] = j * inc;
				coordy[ix] = i * inc;
			}
		}
	}

	public void setSignificance() {
		needsTriangleUpdate = true;
		int row_w = (int) Math.pow(coordx.length, 0.5);
		int inside_w = row_w - 1;

		double ul, u, ur, l, r, dl, d, dr, ht;
		int i = 0, j = 0;
		try {

			for (i = 1; i < inside_w; i++) {

				for (j = 1; j < inside_w; j++) {
					ul = dem[((i - 1) * row_w) + (j - 1)];
					u = dem[((i - 1) * row_w) + j];
					ur = dem[((i - 1) * row_w) + (j + 1)];

					l = dem[(i * row_w) + (j - 1)];
					r = dem[(i * row_w) + (j + 1)];

					dl = dem[((i + 1) * row_w) + (j - 1)];
					d = dem[((i + 1) * row_w) + j];
					dr = dem[((i + 1) * row_w) + (j + 1)];

					ht = dem[(i * row_w) + j];

					sig[(i * row_w) + j] = (getPerpDist(ul, dr, ht) +
							getPerpDist(l, r, ht) +
							getPerpDist(u, d, ht) +
							getPerpDist(ur, dl, ht)) / 4.0;

				}
			}

		}
		catch (Exception e) {
			if (verbose) {
				System.out.println(row_w + " out " + ((i * row_w) + (j - 1)));
			}
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void setWallSignificance() {
		int row_w = (int) Math.pow(coordx.length, 0.5);
		int ix;
		// top
		for (int i = 1; i < row_w - 1; i++) {
			sig[i] = getPerpDist(dem[i - 1], dem[i + 1], dem[i]);

			ix = (i * row_w) + (row_w - 1);
			sig[ix] = getPerpDist(dem[((i - 1) * row_w) + (row_w - 1)], dem[((i + 1) * row_w) + (row_w - 1)], dem[ix]);

			ix = ((row_w - 1) * row_w) + i;
			sig[ix] = getPerpDist(dem[ix - 1], dem[ix + 1], dem[ix]);

			ix = i * row_w;
			sig[ix] = getPerpDist(dem[(i - 1) * row_w], dem[(i + 1) * row_w], dem[ix]);
		}
	}

	public void runInsidePass(int maxpts) {
		setSignificance();

		int row_w = (int) Math.pow(coordx.length, 0.5);
		int inside_w = row_w - 1;

		int least_sig;
		double least_val;
		int ix = 0;

		int numpoints = (row_w - 2) * (row_w - 2); // take out the walls

		if ((maxpts >= numpoints) || (maxpts < 0)) {
			return;
		}

		boolean[] deleteFlag = new boolean[sig.length];
		Arrays.fill(deleteFlag, false);

		while (numpoints > maxpts) {
			least_val = Double.MAX_VALUE;
			least_sig = 0;

			for (int i = 1; i < inside_w; i++) {
				for (int j = 1; j < inside_w; j++) {
					ix = (i * row_w) + j;
					if ((sig[ix] < least_val) && (!deleteFlag[ix])) {
						least_sig = ix;
						least_val = sig[ix];
					}
				}
			}
			deleteFlag[least_sig] = true;
			numpoints--;
		}

		setInsideArr(deleteFlag, numpoints);

	}

	private void setInsideArr(boolean[] del, int numpts) {
		int row_w = (int) Math.pow(coordx.length, 0.5);
		int inside_w = row_w - 1;

		inside_ix = new int[numpts];
		int ix;
		int in_ix = 0;
		for (int i = 1; i < inside_w; i++) {
			for (int j = 1; j < inside_w; j++) {
				ix = (i * row_w) + j;
				if (!del[ix]) {
					inside_ix[in_ix++] = ix;
				}
			}
		}
	}

	public void runInsidePass(double significance) {
		setSignificance();

		int row_w = (int) Math.pow(coordx.length, 0.5);
		int inside_w = row_w - 1;

		int ix;

		int numpoints = (row_w - 2) * (row_w - 2);  // subtract out walls

		if (significance <= 0) {
			return;
		}

		boolean[] deleteFlag = new boolean[sig.length];
		Arrays.fill(deleteFlag, false);

		for (int i = 1; i < inside_w; i++) {
			for (int j = 1; j < inside_w; j++) {
				ix = (i * row_w) + j;
				if (sig[ix] < significance) {
					deleteFlag[ix] = true;
					numpoints--;
				}
			}
		}

		setInsideArr(deleteFlag, numpoints);
	}

	public void runWallPass(double significance) {
		setWallSignificance();

		boolean[] deleteFlag = new boolean[sig.length];
		Arrays.fill(deleteFlag, false);

		int row_w = (int) Math.pow(coordx.length, 0.5);

		int[] numpts = new int[4];
		Arrays.fill(numpts, row_w - 2);

		int ix;
		for (int i = 1; i < row_w - 1; i++) {
			ix = i;
			if (sig[ix] < significance) {
				deleteFlag[ix] = true;
				numpts[0]--;
			}
			ix = (i * row_w) + (row_w - 1);
			if (sig[ix] < significance) {
				deleteFlag[ix] = true;
				numpts[1]--;
			}
			ix = ((row_w - 1) * row_w) + i;
			if (sig[ix] < significance) {
				deleteFlag[ix] = true;
				numpts[2]--;
			}
			ix = i * row_w;
			if (sig[ix] < significance) {
				deleteFlag[ix] = true;
				numpts[3]--;
			}
		}

		setWallArr(deleteFlag, numpts);
	}

	public void runWallPass(int maxpts) {
		setWallSignificance();

		boolean[] deleteFlag = new boolean[sig.length];
		Arrays.fill(deleteFlag, false);

		int row_w = (int) Math.pow(coordx.length, 0.5);
		int ix;

		int[] numpts = new int[4];
		Arrays.fill(numpts, row_w - 2);
		int least_id;
		double least_val;

		while (maxpts < numpts[0]) {
			least_id = 0;
			least_val = Double.MAX_VALUE;
			for (int i = 1; i < row_w - 1; i++) {
				ix = i;
				if (sig[ix] < least_val) {
					least_id = ix;
					least_val = sig[ix];

				}
			}
			deleteFlag[least_id] = true;
			numpts[0]--;
		}

		while (maxpts < numpts[1]) {
			least_id = 0;
			least_val = Double.MAX_VALUE;
			for (int i = 1; i < row_w - 1; i++) {
				ix = (i * row_w) + (row_w - 1);
				if (sig[ix] < least_val) {
					least_id = ix;
					least_val = sig[ix];

				}
			}
			deleteFlag[least_id] = true;
			numpts[1]--;
		}

		while (maxpts < numpts[2]) {
			least_id = 0;
			least_val = Double.MAX_VALUE;
			for (int i = 1; i < row_w - 1; i++) {
				ix = ((row_w - 1) * row_w) + i;
				if (sig[ix] < least_val) {
					least_id = ix;
					least_val = sig[ix];

				}
			}
			deleteFlag[least_id] = true;
			numpts[2]--;
		}

		while (maxpts < numpts[3]) {
			least_id = 0;
			least_val = Double.MAX_VALUE;
			for (int i = 1; i < row_w - 1; i++) {
				ix = i * row_w;
				if (sig[ix] < least_val) {
					least_id = ix;
					least_val = sig[ix];

				}
			}
			deleteFlag[least_id] = true;
			numpts[3]--;
		}

		setWallArr(deleteFlag, numpts);

	}

	private void setWallArr(boolean[] del, int[] numpts) {
		wall_ix = new int[4][];

		for (int i = 0; i < 4; i++) {
			wall_ix[i] = new int[numpts[i]];
		}

		int row_w = (int) Math.pow(coordx.length, 0.5);

		// add corners
		// top left
		corner_ix[0] = 0;

		//top right
		corner_ix[1] = row_w - 1;

		//bottom right
		corner_ix[2] = (row_w - 1) * row_w + (row_w - 1);

		//bottom left
		corner_ix[3] = (row_w - 1) * row_w;


		int top = 0, bot = 0, left = 0, right = 0;
		int ix = 0;

		for (int i = 1; i < row_w - 1; i++) {
			ix = i;  // top goes from left to right
			if (!del[ix]) {
				wall_ix[0][top++] = ix;
			}
			ix = (i * row_w) + (row_w - 1);  // right goes from top to bottom
			if (!del[ix]) {
				wall_ix[1][right++] = ix;
			}
			ix = ((row_w - 1) * row_w) + i;  // bottom goes from left to right
			if (!del[ix]) {
				wall_ix[2][bot++] = ix;
			}
			ix = i * row_w;   // left goes top to bottom
			if (!del[ix]) {
				wall_ix[3][left++] = ix;
			}
		}
	}

	private double getPerpDist(double ht1, double ht2, double cht) {
		double v1_x = cellw * 2;
		double v1_y = ht2 - ht1;
		double mag1 = Math.pow((v1_x * v1_x) + (v1_y * v1_y), 0.5);

		double v2_x = cellw;
		double v2_y = cht - ht1;

		double mag2 = Math.pow((v2_y * v2_y) + (v2_x * v2_x), 0.5);

		v1_x /= mag1;
		v1_y /= mag1;

		v2_x /= mag2;
		v2_y /= mag2;

		double theta = ((v1_x * v2_x) + (v1_y * v2_y));

		if (theta > 1) {
			theta = 1;
		}

		return Math.sin(Math.acos(theta)) * mag2;
	}

	public void runWallPass() {
		needsTriangleUpdate = true;
	}

	public void triangulate() {

		if ((wall_ix == null) || (inside_ix == null)) {
			return;
		}

		int ptr = 0;

		int nP = inside_ix.length + wall_ix[0].length + wall_ix[1].length + wall_ix[2].length + wall_ix[3].length + corner_ix.length;
		pts = new double[nP][4];

		for (int i = 0; i < corner_ix.length; i++) {
			pts[ptr][0] = coordx[corner_ix[i]];
			pts[ptr][1] = coordy[corner_ix[i]];
			pts[ptr][2] = ptr;
			pts[ptr][3] = dem[corner_ix[i]];
			ptr++;
		}

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < wall_ix[i].length; j++) {
				pts[ptr][0] = coordx[wall_ix[i][j]];
				pts[ptr][1] = coordy[wall_ix[i][j]];
				pts[ptr][2] = ptr;
				pts[ptr][3] = dem[wall_ix[i][j]];
				ptr++;
			}
		}

		for (int i = 0; i < inside_ix.length; i++) {
			pts[ptr][0] = coordx[inside_ix[i]];
			pts[ptr][1] = coordy[inside_ix[i]];
			pts[ptr][2] = ptr;
			pts[ptr][3] = dem[inside_ix[i]];
			ptr++;
		}

		if (verbose) {
			System.out.println("Fortunes algo start:");
		}
		long start_time = System.currentTimeMillis();
		if (verbose) {
			System.out.println("creating Object");
		}
		fortune = new Fortune(pts, 0, 0, tile_width);
		if (verbose) {
			System.out.println("Fortunes algo run:");
		}
		fortune.run();
		if (verbose) {
			System.out.println("number of edges : " + fortune.trimesh.numEdges);
		}
		if (verbose) {
			System.out.println("number of triangles : " + fortune.trimesh.numTris);
		}
		if (verbose) {
			System.out.println("time : " + (System.currentTimeMillis() - start_time));
		}

		fortune.slipBack(pts);
		fortune.trimesh.removeEdgeTriangles(pts);
		fortune.trimesh.faceUpTriangles(pts);
	}

	public void stripeTris() {

		meshstrips = new MeshStrip(fortune.trimesh, pts);

		int numtris = 0;
		int bestMode = MeshStrip.GREEDY;
		if (verbose) {
			System.out.println("mesh striping start (GREEDY):");
		}
		long start_time = System.currentTimeMillis();
		meshstrips.setMode(MeshStrip.GREEDY);
		meshstrips.start();
		if (verbose) {
			System.out.println("time : " + (System.currentTimeMillis() - start_time));
		}
		if (verbose) {
			System.out.println("number of strips : " + meshstrips.strips.size());
		}

		numtris = meshstrips.strips.size();

		if (verbose) {
			System.out.println("mesh striping start (GENEROUS):");
		}
		start_time = System.currentTimeMillis();
		meshstrips.setMode(MeshStrip.GENEROUS);
		meshstrips.start();
		if (verbose) {
			System.out.println("time : " + (System.currentTimeMillis() - start_time));
		}
		if (verbose) {
			System.out.println("number of strips : " + meshstrips.strips.size());
		}

		if (meshstrips.strips.size() < numtris) {
			bestMode = MeshStrip.GENEROUS;
			numtris = meshstrips.strips.size();
		}

		if (verbose) {
			System.out.println("mesh striping start (HYBRID):");
		}
		start_time = System.currentTimeMillis();
		meshstrips.setMode(MeshStrip.HYBRID);
		meshstrips.start();
		if (verbose) {
			System.out.println("time : " + (System.currentTimeMillis() - start_time));
		}
		if (verbose) {
			System.out.println("number of strips : " + meshstrips.strips.size());
		}

		if (meshstrips.strips.size() < numtris) {
			bestMode = MeshStrip.HYBRID;
			numtris = meshstrips.strips.size();
		}

		if (verbose) {
			System.out.println("mesh striping start (Longest Path):");
		}
		start_time = System.currentTimeMillis();
		meshstrips.setMode(MeshStrip.LONGEST_PATH);
		meshstrips.startLongestPath();
		if (verbose) {
			System.out.println("time : " + (System.currentTimeMillis() - start_time));
		}
		if (verbose) {
			System.out.println("number of strips : " + meshstrips.strips.size());
		}

		if (meshstrips.strips.size() < numtris) {
			bestMode = MeshStrip.LONGEST_PATH;
			numtris = meshstrips.strips.size();
		}

		meshstrips.selectMode(bestMode);
	}

	public void createJetstreamFile() {
		try {
			//DataOutputStream dout = new DataOutputStream(new FileOutputStream(file));
			DataOutputStream dout = new DataOutputStream(bout);
			// output number of points (integer) x,y,z (y is the height marker)
			// x , y , tile width
			dout.writeDouble(tx);
			dout.writeDouble(ty);
			dout.writeInt(tile_width);

			dout.writeInt(pts.length);
			// output points (float)
			for (int i = 0; i < pts.length; i++) {
				dout.writeFloat((float) pts[i][0]);
				dout.writeFloat((float) pts[i][3]);
				dout.writeFloat((float) pts[i][1]);
			}
			// output number of strips(integer)
			int numStrips = meshstrips.strips.size();
			dout.writeInt(numStrips);
			Strip strip;
			// output strips, which are index numbers of the points (integer)
			for (int i = 0; i < numStrips; i++) {
				strip = (Strip) meshstrips.strips.elementAt(i);
				dout.writeInt(strip.v.length);
				for (int j = 0; j < strip.v.length; j++) {
					dout.writeInt(strip.v[j]);
				}
			}
			dout.close();
		}
		catch (Exception e) {
		}
	}
	public ByteArrayOutputStream bout = new ByteArrayOutputStream();
	public Fortune fortune;
	public double[][] pts;
}
