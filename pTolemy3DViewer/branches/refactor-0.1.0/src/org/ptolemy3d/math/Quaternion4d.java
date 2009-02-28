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
package org.ptolemy3d.math;

/**
 * A quaternion with double precision.
 */
public class Quaternion4d
{
    double[] q = new double[4];

    public Quaternion4d()
    {
    }

    public Quaternion4d(double theta, double[] v)
    {
        set(theta, v);
    }

    public final void set(double theta, double[] v)
    {
        q[0] = Math.cos(theta / 2);
        q[1] = v[0] * Math.sin(theta / 2);
        q[2] = v[1] * Math.sin(theta / 2);
        q[3] = v[2] * Math.sin(theta / 2);
        double mag = Math.sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3]);
        for (int i = 0; i < 4; i++) {
            q[i] /= mag;
        }
    }

    public final void setMatrix(Matrix9d m)
    {
        m.m[0][0] = q[0] * q[0] + q[1] * q[1] - q[2] * q[2] - q[3] * q[3];
        m.m[1][0] = 2 * (q[2] * q[1] + q[0] * q[3]);
        m.m[2][0] = 2 * (q[3] * q[1] - q[0] * q[2]);

        m.m[0][1] = 2 * (q[1] * q[2] - q[0] * q[3]);
        m.m[1][1] = q[0] * q[0] - q[1] * q[1] + q[2] * q[2] - q[3] * q[3];
        m.m[2][1] = 2 * (q[3] * q[2] + q[0] * q[1]);

        m.m[0][2] = 2 * (q[1] * q[3] + q[0] * q[2]);
        m.m[1][2] = 2 * (q[2] * q[3] - q[0] * q[1]);
        m.m[2][2] = q[0] * q[0] - q[1] * q[1] - q[2] * q[2] + q[3] * q[3];
    }
}