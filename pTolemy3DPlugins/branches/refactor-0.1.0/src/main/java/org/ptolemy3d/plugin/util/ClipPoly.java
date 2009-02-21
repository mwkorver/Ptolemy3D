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

import java.util.Vector;

import org.ptolemy3d.plugin.util.VectorNode;


public class ClipPoly
{

    VectorNode[] points = null;
    private static final int LEFT_CUT = 1;
    private static final int RIGHT_CUT = 2;
    private static final int TOP_CUT = 3;
    private static final int BOTTOM_CUT = 4;
    private static final int ARB_CUT = 5;
    public static final double EPS = 0.000000001;
    private Vector<VectorNode> hpoints = new Vector<VectorNode>(4, 5);

    public ClipPoly()
    {
    }

    public ClipPoly(int numpts)
    {
        points = new VectorNode[numpts];
    }

    private VectorNode[] makePointArray(Vector<VectorNode> vpoints)
    {
        int s = vpoints.size();
        VectorNode[] pts = new VectorNode[s + 1];
        VectorNode tmp;
        for (int i = 0; i < s; i++)
        {
            tmp = vpoints.elementAt(i);
            pts[i] = new VectorNode(tmp.rp[0], tmp.rp[1], 0, false);
        }
        tmp = vpoints.elementAt(0);
        pts[s] = new VectorNode(tmp.rp[0], tmp.rp[1], 0, false);
        return pts;
    }

    public final void resetPoints(VectorNode[] pts)
    {
        points = pts;
    }

    private VectorNode[] clipSutherlandHodgman(int clipline, double v, VectorNode[] points)
    {
        if (points.length < 1)
        {
            return points;
        }
        hpoints.clear();
        int neval, oeval = eval(clipline, points[0], v);
        if (points.length == 1 && oeval >= 0)
        {
            hpoints.addElement(points[0]);
        }
        else
        {
            for (int i = 1; i <= points.length; i++)
            {
                VectorNode p = points[i % points.length];
                if ((neval = eval(clipline, p, v)) != oeval)
                {
                    if (neval != 0 && oeval != 0)
                    {
                        hpoints.addElement(intersect(clipline, points[i - 1], p, v));
                    }
                    oeval = neval;
                }
                if (neval >= 0)
                {
                    hpoints.addElement(p);
                }
            }
        }
        return makePointArray(hpoints);
    }

    public VectorNode[] clip(ClipPoly mustBeConvex)
    {

        VectorNode[] cpoints = mustBeConvex.points;
        int clipline = 0;
        double v;
        if (cpoints.length < 2)
        {
            throw new IllegalStateException("Need at least two points.");
        }
        if (cpoints.length == 2)
        {
            clipline = getCutLine(cpoints[0], cpoints[1]);
            v = ((clipline == LEFT_CUT) || (clipline == RIGHT_CUT)) ? cpoints[0].rp[0] : cpoints[0].rp[1];
            return clipSutherlandHodgman(clipline, v, this.points);
        }
        int i = 1;
        VectorNode p1 = cpoints[0];
        VectorNode p2 = cpoints[1];
        VectorNode px = null;

        VectorNode[] toClip = this.points;
        do
        {
            clipline = getCutLine(p1, p2);
            v = ((clipline == LEFT_CUT) || (clipline == RIGHT_CUT)) ? p1.rp[0] : p1.rp[1];
            toClip = clipSutherlandHodgman(clipline, v, toClip);
            if (toClip.length == 0)
            {
                break;
            // Folge von kolinearen Punkten ignorieren ...
            }
            while (++i <= cpoints.length)
            {
                px = cpoints[i % cpoints.length];
                p1 = p2;
                if (eval(clipline, px, v) != 0)
                {
                    break;
                }
            }
            p2 = px;
        }
        while (i <= cpoints.length);

        return toClip;
    }

    public void setPoint(int index, int x, int y)
    {
        points[index] = new VectorNode(x, y, 0, true);
    }

    public VectorNode[] getPoints()
    {
        return points;
    }

    public int numPoints()
    {
        return points.length;
    }

    private int getCutLine(VectorNode p1, VectorNode p2)
    {
        if ((p1.rp[0] == p2.rp[0]) && (p1.rp[1] == p2.rp[1]))
        {
            throw new IllegalArgumentException("p1.rp == p2.rp");
        }
        if (p1.rp[0] == p2.rp[0])
        {
            return (p1.rp[1] < p2.rp[1]) ? LEFT_CUT : RIGHT_CUT;
        }
        if (p1.rp[1] == p2.rp[1])
        {
            return (p1.rp[0] < p2.rp[0]) ? TOP_CUT : BOTTOM_CUT;
        }
        arbconst[0] = -(p1.rp[1] - p2.rp[1]);
        arbconst[1] = (p1.rp[0] - p2.rp[0]);
        arbconst[2] = -(arbconst[0] * p1.rp[0] + arbconst[1] * p1.rp[1]);

        arbcoeffs[0] = (double) (p2.rp[1] - p1.rp[1]) / (p2.rp[0] - p1.rp[0]);
        arbcoeffs[1] = p1.rp[1] - arbcoeffs[0] * p1.rp[0];
        return ARB_CUT;
    }

    private int eval(int clipline, VectorNode p, double v)
    {
        switch (clipline)
        {
            case LEFT_CUT:
                if (p.rp[0] < v)
                {
                    return -1;
                }
                if (p.rp[0] > v)
                {
                    return 1;
                }
                break;
            case RIGHT_CUT:
                if (p.rp[0] < v)
                {
                    return 1;
                }
                if (p.rp[0] > v)
                {
                    return -1;
                }
                break;
            case TOP_CUT:
                if (p.rp[1] < v)
                {
                    return 1;
                }
                if (p.rp[1] > v)
                {
                    return -1;
                }
                break;
            case BOTTOM_CUT:
                if (p.rp[1] < v)
                {
                    return -1;
                }
                if (p.rp[1] > v)
                {
                    return 1;
                }
                break;
            case ARB_CUT:
                v = (arbconst[0] * p.rp[0] + arbconst[1] * p.rp[1] + arbconst[2]);
                if (v > EPS)
                {
                    return 1;
                }
                if (v < -EPS)
                {
                    return -1;
                }
                break;
        }
        return 0;
    }
    double coeffs[] = new double[2];
    double arbcoeffs[] = new double[2];
    double arbconst[] = new double[3];

    private VectorNode intersect(int clipline, VectorNode a, VectorNode b, double v)
    {
        switch (clipline)
        {
            case LEFT_CUT:
            case RIGHT_CUT:
                if (a.rp[0] == b.rp[0])
                {
                    if (a.rp[1] != v)
                    {
                        throw new IllegalArgumentException("No intersection");
                    }
                    return new VectorNode(b.rp[0], b.rp[1], 0, false);
                }
                linearCoeffs(a, b);
                return new VectorNode((int) v, (int) (coeffs[0] * v + coeffs[1] + 0.5), 0, false);
            case TOP_CUT:
            case BOTTOM_CUT:
                if (a.rp[1] == b.rp[1])
                {
                    if (a.rp[0] != v)
                    {
                        throw new IllegalArgumentException("No intersection");
                    }
                    return new VectorNode(b.rp[0], b.rp[1], 0, false);
                }

                if (a.rp[0] == b.rp[0])
                {
                    return new VectorNode(a.rp[0], (int) v, 0, false);
                }
                linearCoeffs(a, b);
                return new VectorNode((int) ((v - coeffs[1]) / coeffs[0] + 0.5), (int) v, 0, false);
            case ARB_CUT:
                if (a.rp[0] == b.rp[0])
                {
                    if (a.rp[1] == b.rp[1])
                    {
                        if (eval(clipline, a, v) != 0)
                        {
                            throw new IllegalArgumentException("No intersection");
                        }
                        else
                        {
                            return new VectorNode(a.rp[0], a.rp[1], 0, false);
                        }
                    }
                    return new VectorNode(a.rp[0], (int) (a.rp[0] * arbcoeffs[0] + arbcoeffs[1]), 0, false);
                }
                linearCoeffs(a, b);
                double dm = arbcoeffs[0] - coeffs[0];
                if (Math.abs(dm) < EPS)
                {
                    return new VectorNode(b.rp[0], b.rp[1], 0, false);
                }
                double x = (coeffs[1] - arbcoeffs[1]) / dm;
                return new VectorNode((int) (x + 0.5), (int) (x * arbcoeffs[0] + arbcoeffs[1] + 0.5), 0, false);
        }
        throw new IllegalArgumentException("invalid clip" + clipline);
    }

    private final void linearCoeffs(VectorNode p1, VectorNode p2)
    {
        coeffs[0] = (double) (p2.rp[1] - p1.rp[1]) / (p2.rp[0] - p1.rp[0]);
        coeffs[1] = p1.rp[1] - coeffs[0] * p1.rp[0];
    }
}
