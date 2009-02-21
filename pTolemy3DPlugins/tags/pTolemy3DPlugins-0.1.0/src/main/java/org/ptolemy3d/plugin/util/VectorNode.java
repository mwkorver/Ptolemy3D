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
package org.ptolemy3d.plugin.util;

import static org.ptolemy3d.Ptolemy3DConfiguration.EARTH_RADIUS;

import javax.media.opengl.GL;

import org.ptolemy3d.Ptolemy3D;
import org.ptolemy3d.Ptolemy3DUnit;
import org.ptolemy3d.math.Math3D;
import org.ptolemy3d.math.Matrix9d;
import org.ptolemy3d.math.Quaternion4d;
import org.ptolemy3d.math.Vector3d;
import org.ptolemy3d.scene.Landscape;

public class VectorNode
{
	private static final int LSTY_SOLID = 1;
	private static final int LSTY_DOTTED = 2;
	private static final int LSTY_DASHED = 3;
	private static final int LSTY_HATCHED = 4;

	public float raise;
	// real x,y, and z of node
	public int[] rp;
	// interpolation points that follow this node.
	public double[][] ipts;

	public VectorNode(int x, int z, float r, boolean applyTransform)
	{
		raise = r;
		rp = new int[2];
		ipts = new double[1][3];
		rp[0] = x;
		rp[1] = z;

		Math3D.setSphericalCoord(rp[0], rp[1], ipts[0]);

		Vector3d.scale(ipts[0], EARTH_RADIUS + raise);

		//setPointElevations(raise);
	}

	public final void draw(GL gl, boolean interp)
	{
		if (ipts != null) {
			int len = (interp) ? ipts.length : 1;
			for (int i = 0; i < len; i++) {
				gl.glVertex3d(ipts[i][0], ipts[i][1], ipts[i][2]);
			}
		}
	}

	/**
	 * Draw with a style
	 */
	public final void draw(GL gl, boolean interp, int linestyle, VectorNode nn)
	{
		switch (linestyle) {
			case LSTY_SOLID:
				draw(gl, true);
				break;
			case LSTY_DOTTED:
				break;
			case LSTY_DASHED:
				break;
			case LSTY_HATCHED:
				break;
		}
	}

	/** This function does not support interpolation */
	public final void draw(GL gl, double height)
	{
		Vector3d.normalize(ipts[0]);

		double h = EARTH_RADIUS + height;
		gl.glVertex3d(ipts[0][0] * h, ipts[0][1] * h, ipts[0][2] * h);
	}

	/**
	 * interpolates own point with point passed., ilen is the length between interpolated points
	 */
	public final void interpolate(VectorNode nn, double ilen, float VECTOR_RAISE, int maxints)
	{
		final Ptolemy3DUnit unit = Ptolemy3D.ptolemy.unit;

		Quaternion4d quat = new Quaternion4d();
		Matrix9d rotMat = new Matrix9d();

		double[][] ipts = null;
		{
			ilen = (ilen / unit.DD) * Math3D.degToRad;
			//Vector3d.normalize(this.ipts[0]);
			double angle = Math3D.angle3dvec(this.ipts[0][0], this.ipts[0][1], this.ipts[0][2], nn.ipts[0][0], nn.ipts[0][1], nn.ipts[0][2], false);
			int nInc = (int) (angle / ilen);

			nInc = ((nInc > maxints) && (maxints != 0)) ? maxints : nInc;
			if (nInc >= 1) {
				double ainc = angle / nInc;
				double[] cr = new double[3];

				Vector3d.cross(cr, this.ipts[0], nn.ipts[0]);
				Vector3d.normalize(cr);

				ipts = new double[nInc][3];
				for (int i = 0; i < nInc; i++) {
					quat.set(i * ainc, cr);
					quat.setMatrix(rotMat);
					System.arraycopy(this.ipts[0], 0, ipts[i], 0, 3);
					rotMat.transform(ipts[i]);
				}
			}
		}
		if (ipts != null) {
			this.ipts = null;
			this.ipts = ipts;
		}
		setPointElevations(VECTOR_RAISE);
	}

	public final void setPointElevations()
	{
		setPointElevations(raise);
	}

	public final void setPointElevations(float vectorRaise)
	{
		final Landscape landscape = Ptolemy3D.ptolemy.scene.landscape;
		final Ptolemy3DUnit unit = Ptolemy3D.ptolemy.unit;

		double[] pt = new double[3];
		for (int i = 0; i < ipts.length; i++) {
			Vector3d.normalize(ipts[i]);
			Math3D.setMapCoord(ipts[i], pt);
			double ty = landscape.groundHeight(pt[1] * unit.DD, pt[0] * unit.DD, 0) + vectorRaise;
			ipts[i][0] *= EARTH_RADIUS + ty;
			ipts[i][1] *= EARTH_RADIUS + ty;
			ipts[i][2] *= EARTH_RADIUS + ty;
		}
	}

	public static boolean pointInPoly(VectorNode[] inPoints, double xpt, double ypt, int N)
	{
		int counter = 0;
		int i;
		double xinters;
		int p1, p2;
		p1 = 0;
		for (i = 1; i <= N; i++)
		{
			p2 = i % N;
			if (ypt > Math.min(inPoints[p1].rp[1], inPoints[p2].rp[1]))
			{
				if (ypt <= Math.max(inPoints[p1].rp[1], inPoints[p2].rp[1]))
				{
					if (xpt <= Math.max(inPoints[p1].rp[0], inPoints[p2].rp[0]))
					{
						if (inPoints[p1].rp[1] != inPoints[p2].rp[1])
						{
							xinters = (ypt - inPoints[p1].rp[1]) * (inPoints[p2].rp[0] - inPoints[p1].rp[0]) / (inPoints[p2].rp[1] - inPoints[p1].rp[1]) + inPoints[p1].rp[0];
							if (inPoints[p1].rp[0] == inPoints[p2].rp[0] || xpt <= xinters)
							{
								counter++;
							}
						}
					}
				}
			}
			p1 = p2;
		}

		if (counter % 2 == 0) {
			return false;
		}
		else {
			return true;
		}
	}

	public static final void getCentroid(VectorNode[] pts, double[] opts)
	{
		int i;
		double cent_weight_x = 0.0, cent_weight_y = 0.0;
		double len, total_len = 0;

		for (i = 1; i < pts.length; i++) {
			len = Math3D.distance2D(pts[i - 1].rp[0], pts[i].rp[0], pts[i - 1].rp[1], pts[i].rp[1]);
			cent_weight_x += len * ((float) (pts[i - 1].rp[0] + pts[i].rp[0]) / 2);
			cent_weight_y += len * ((float) (pts[i - 1].rp[1] + pts[i].rp[1]) / 2);
			total_len += len;
		}
		opts[0] = (float) (cent_weight_x / total_len);
		opts[1] = (float) (cent_weight_y / total_len);
	}
}