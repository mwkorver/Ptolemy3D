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
package org.ptolemy3d.demcutter;

import java.util.Vector;
import fortune.DelTriangle;
import fortune.TriangleMesh;

public class MeshStrip {

    public static final int GREEDY = 0;
    public static final int GENEROUS = 1;
    public static final int HYBRID = 2;
    public static final int LONGEST_PATH = 3;
    TriangleMesh tmesh;
    private Vector<Strip>[] stripsArray = new Vector[4];
    public Vector<Strip> strips = null;
    private Vector[] fansArray = new Vector[4];
    public Vector fans = null;
    private int mode = -1;
    private int v_mode = 0;
    double[][] pts;

    public MeshStrip(TriangleMesh tm, double[][] pts) {
        this.tmesh = tm;
        this.pts = pts;
        reset();
    }

    private void reset() {
        tmesh.resetTriangles();
    }

    public void start() {

        if (mode == -1) {
            setMode(GENEROUS);
        }

        while (true) {
            int lowest = getLeastConn();
            if (lowest < 0) {
                break;
            }
            DelTriangle node = tmesh.triangles[lowest];
            Strip strip = new Strip(node);
            strips.add(strip);
            node.decrementAllPartners();
            addToStrip(node, strip);
        }
    }

    public void setMode(int m) {
        reset();

        mode = v_mode = m;
        if (stripsArray[m] == null) {
            stripsArray[m] = new Vector<Strip>(10, 10);
        }
        if (fansArray[m] == null) {
            fansArray[m] = new Vector(10, 10);
        }

        fans = fansArray[m];
        strips = stripsArray[m];
        strips.clear();
        fans.clear();
    }

    public void selectMode(int m) {
        strips = stripsArray[m];
    }

    public void startLongestPath() {
        int i;
        DelTriangle dt, ld = null;
        Strip lstrip, strip;

        try {

            while (true) {
                lstrip = null;
                ld = null;

                int longest = 0;
                for (i = 0; i < tmesh.numTris; i++) {
                    dt = tmesh.triangles[i];
                    if (!dt.striped) {
                        strip = searchPath(dt, new Strip(dt));
                        if (strip.numTris > longest) {
                            lstrip = strip;
                            longest = strip.numTris;
                            ld = dt;
                        }
                    }
                }

                if (lstrip == null) {
                    break;
                }

                ld.decrementAllPartners();
                for (i = 0; i < lstrip.numTris; i++) {
                    tmesh.triangles[lstrip.ids[i]].striped = true;
                    if (i > 0) {
                        tmesh.triangles[lstrip.ids[i]].setOddVert(tmesh.triangles[lstrip.ids[i - 1]]);
                    }
                }
                lstrip.end(pts);
                strips.add(lstrip);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }



    }

    private Strip searchPath(DelTriangle node, Strip strip) {

        DelTriangle chk = null;
        if (node.numpartners == 0) {
            return strip;
        }
        Strip[] stripsArr = new Strip[node.numpartners];

        int rot;

        for (int i = 0; i < node.numpartners; i++) {
            chk = node.partners[i];

            if (strip.findTriangleById(chk.id)) {
                continue;
            }

            rot = -1;
            if (strip.pos == 3) { // check all sides
                if ((chk.striped) || (!chk.edgeMatch(strip.v[2], strip.v[1]))) {
                    if ((chk.striped) || (!chk.edgeMatch(strip.v[1], strip.v[0]))) {
                        if ((chk.striped) || (!chk.edgeMatch(strip.v[0], strip.v[2]))) {
                            continue;
                        } else {
                            rot = 1;
                        }
                    } else {
                        rot = 2;
                    }
                }
            } else {
                if (strip.numTris % 2 == 0) {
                    if ((chk.striped) || (!chk.edgeMatch(strip.v[strip.pos - 2], strip.v[strip.pos - 1]))) {
                        continue;
                    }
                } else {
                    if ((chk.striped) || (!chk.edgeMatch(strip.v[strip.pos - 1], strip.v[strip.pos - 2]))) {
                        continue;
                    }
                }
            }

            stripsArr[i] = new Strip(strip);

            if (rot != -1) {
                stripsArr[i].rotateStarter(rot);
            }

            stripsArr[i].append(chk.setOddVertLp(node), chk.id);
            stripsArr[i] = searchPath(chk, stripsArr[i]);
        }
        // find which strip is the longest and set it.
        int lon_id = -1;
        int lon_val = 0;//strip.numTris;
        for (int i = 0; i < node.numpartners; i++) {
            if (stripsArr[i] != null) {
                if (stripsArr[i].numTris > lon_val) {
                    lon_id = i;
                    lon_val = stripsArr[i].numTris;
                }
            }
        }
        if (lon_id == -1) {
            return strip;
        } else {
            return stripsArr[lon_id];
        }
    }

    private void addToStrip(DelTriangle node, Strip strip) {
        node.striped = true;

        if (node.conn == 0) {
            strip.end(pts);
            return;
        }
        // look for connected triangles, go to the lowest one.
        DelTriangle partner = null;
        DelTriangle chk = null;

        if (mode == HYBRID) {
            v_mode = (node.id % 2 == 0) ? GREEDY : GENEROUS;
        }

        int val = Integer.MAX_VALUE;
        switch (v_mode) {
            case GENEROUS:
                val = Integer.MAX_VALUE;
                break;
            case GREEDY:
                val = -Integer.MAX_VALUE;
                break;
        }

        int srot = -1, rot = -1;

        for (int i = 0; i < node.numpartners; i++) {
            chk = node.partners[i];
            rot = -1;

            if (strip.pos == 3) { // check all sides
                if ((chk.striped) || (!chk.edgeMatch(strip.v[2], strip.v[1]))) {
                    if ((chk.striped) || (!chk.edgeMatch(strip.v[1], strip.v[0]))) {
                        if ((chk.striped) || (!chk.edgeMatch(strip.v[0], strip.v[2]))) {
                            continue;
                        } else {
                            rot = 1;
                        }
                    } else {
                        rot = 2;
                    }
                }
            } else {
                if (strip.numTris % 2 == 0) {
                    if ((chk.striped) || (!chk.edgeMatch(strip.v[strip.pos - 2], strip.v[strip.pos - 1]))) {
                        continue;
                    }
                } else {
                    if ((chk.striped) || (!chk.edgeMatch(strip.v[strip.pos - 1], strip.v[strip.pos - 2]))) {
                        continue;
                    }
                }
            }

            switch (v_mode) {
                case GENEROUS:
                    if (chk.conn < val) {
                        partner = chk;
                        val = chk.conn;
                        srot = rot;
                    }
                    break;
                case GREEDY:
                    if (chk.conn > val) {
                        partner = chk;
                        val = chk.conn;
                        srot = rot;
                    }
                    break;
            }
        }

        if (partner == null) {
            strip.end(pts);
            return;
        }

        if (srot != -1) {
            strip.rotateStarter(srot);
        }

        strip.append(partner.setOddVert(node));
        addToStrip(partner, strip);

    }

    private int getLeastConn() {
        DelTriangle dt;
        int low = Integer.MAX_VALUE;
        int lix = -1;
        for (int i = 0; i < tmesh.numTris; i++) {
            dt = tmesh.triangles[i];
            if (!dt.striped) {
                if (dt.conn < low) {
                    lix = i;
                    low = dt.conn;
                }
            }
        }
        return lix;
    }

    private int getMostConn() {
        DelTriangle dt;
        int low = -Integer.MAX_VALUE;
        int lix = -1;
        for (int i = 0; i < tmesh.numTris; i++) {
            dt = tmesh.triangles[i];
            if (!dt.striped) {
                if (dt.conn > low) {
                    lix = i;
                    low = dt.conn;
                }
            }
        }
        return lix;
    }
}

class Strip {

    public boolean isFirstAdd = true;
    public static final int TYPE_UNDECIDED = 0;
    public static final int TYPE_STRIP = 1;
    public static final int TYPE_FAN = 2;
    private int type = TYPE_UNDECIDED;
    public int[] v = null;
    public int[] ids = null;
    public int pos = 0;
    public int numTris = 0;

    public Strip(Strip strip) {
        v = new int[strip.v.length];
        System.arraycopy(strip.v, 0, v, 0, strip.v.length);
        pos = strip.pos;
        ids = new int[strip.ids.length];
        System.arraycopy(strip.ids, 0, ids, 0, strip.ids.length);
        numTris = strip.numTris;
    }

    public Strip(DelTriangle node) {
        v = new int[10];
        ids = new int[10];
        ids[numTris++] = node.id;
        for (int i = 0; i < 3; i++) {
            v[pos++] = node.v[i];
        }
        numTris = 1;
    }

    public int getType() {
        return type;
    }

    public void setType(int t) {
        type = t;
    }

    public void rotateStarter(int times) {
        int tmp;
        for (int i = 0; i < times; i++) {
            tmp = v[0];
            v[0] = v[1];
            v[1] = v[2];
            v[2] = tmp;
        }
    }

    public void append(int vtcy, int id) {
        append(vtcy);
        if (numTris >= ids.length) {
            int[] nv = new int[ids.length + 10];
            System.arraycopy(ids, 0, nv, 0, ids.length);
            ids = nv;
        }
        ids[numTris++] = id;
    }

    public void append(int vtcy) {
        if (pos >= v.length) {
            int[] nv = new int[v.length + 10];
            System.arraycopy(v, 0, nv, 0, v.length);
            v = nv;
        }
        v[pos++] = vtcy;
    }

    public void end(double[][] pts) {
        if (pos != v.length) {
            int[] nv = new int[pos];
            System.arraycopy(v, 0, nv, 0, pos);
            v = nv;
        }
    }

    public boolean findTriangleById(int id) {
        for (int i = 0; i < numTris; i++) {
            if (ids[i] == id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String str = "";

        for (int i = 0; i < v.length; i++) {
            str += String.valueOf(v[i]) + ",";
        }
        return str;
    }
}
