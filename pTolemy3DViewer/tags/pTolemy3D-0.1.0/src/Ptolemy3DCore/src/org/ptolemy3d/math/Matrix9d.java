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
 * A matrix 3x3 with double precision.
 */
public class Matrix9d
{
    public double[][] m = new double[3][3];

    public Matrix9d()
    {
        identity();
    }

    public final void rotY(double theta)
    {
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        m[0][0] = c;
        m[0][1] = 0;
        m[0][2] = s;
        m[1][0] = 0;
        m[1][1] = 1;
        m[1][2] = 0;
        m[2][0] = -s;
        m[2][1] = 0;
        m[2][2] = c;
    }

    public final void rotX(double theta)
    {
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        m[0][0] = 1;
        m[0][1] = 0;
        m[0][2] = 0;
        m[1][0] = 0;
        m[1][1] = c;
        m[1][2] = -s;
        m[2][0] = 0;
        m[2][1] = s;
        m[2][2] = c;
    }

    public final void rotZ(double theta)
    {
        double c = Math.cos(theta);
        double s = Math.sin(theta);
        m[0][0] = c;
        m[0][1] = -s;
        m[0][2] = 0;
        m[1][0] = s;
        m[1][1] = c;
        m[1][2] = 0;
        m[2][0] = 0;
        m[2][1] = 0;
        m[2][2] = 1;
    }

    public final void identity()
    {
        m[0][0] = 1;
        m[0][1] = 0;
        m[0][2] = 0;
        m[1][0] = 0;
        m[1][1] = 1;
        m[1][2] = 0;
        m[2][0] = 0;
        m[2][1] = 0;
        m[2][2] = 1;
    }

    /**
     * Multiply <code>mat1</code> by <code>mat2</code>.
     * @param dest destination matrix, can be <code>mat1</code> or <code>mat2</code>
     */
    public static final void multiply(Matrix9d mat1, Matrix9d mat2, Matrix9d dest)
    {
    	double m00, m01, m02, m10, m11, m12, m20, m21, m22;

    	m00 = mat1.m[0][0] * mat2.m[0][0] + mat1.m[0][1] * mat2.m[1][0] + mat1.m[0][2] * mat2.m[2][0];
    	m01 = mat1.m[0][0] * mat2.m[0][1] + mat1.m[0][1] * mat2.m[1][1] + mat1.m[0][2] * mat2.m[2][1];
    	m02 = mat1.m[0][0] * mat2.m[0][2] + mat1.m[0][1] * mat2.m[1][2] + mat1.m[0][2] * mat2.m[2][2];

    	m10 = mat1.m[1][0] * mat2.m[0][0] + mat1.m[1][1] * mat2.m[1][0] + mat1.m[1][2] * mat2.m[2][0];
    	m11 = mat1.m[1][0] * mat2.m[0][1] + mat1.m[1][1] * mat2.m[1][1] + mat1.m[1][2] * mat2.m[2][1];
    	m12 = mat1.m[1][0] * mat2.m[0][2] + mat1.m[1][1] * mat2.m[1][2] + mat1.m[1][2] * mat2.m[2][2];

    	m20 = mat1.m[2][0] * mat2.m[0][0] + mat1.m[2][1] * mat2.m[1][0] + mat1.m[2][2] * mat2.m[2][0];
    	m21 = mat1.m[2][0] * mat2.m[0][1] + mat1.m[2][1] * mat2.m[1][1] + mat1.m[2][2] * mat2.m[2][1];
    	m22 = mat1.m[2][0] * mat2.m[0][2] + mat1.m[2][1] * mat2.m[1][2] + mat1.m[2][2] * mat2.m[2][2];

    	dest.m[0][0] = m00;
    	dest.m[0][1] = m01;
    	dest.m[0][2] = m02;

    	dest.m[1][0] = m10;
    	dest.m[1][1] = m11;
    	dest.m[1][2] = m12;

    	dest.m[2][0] = m20;
    	dest.m[2][1] = m21;
    	dest.m[2][2] = m22;
    }

    public final void transform(double[] v)
    {
        double v0 = v[0];
        double v1 = v[1];
        double v2 = v[2];
        v[0] = (m[0][0] * v0 + m[0][1] * v1 + m[0][2] * v2);
        v[1] = (m[1][0] * v0 + m[1][1] * v1 + m[1][2] * v2);
        v[2] = (m[2][0] * v0 + m[2][1] * v1 + m[2][2] * v2);
    }

    public final void invert(Matrix9d src)
    {
        double s = determinant();
        if (s == 0.0) {
            return;
        }
        copyTo(src);

        double oneOverS = 1 / s;
        m[0][0] = (src.m[1][1] * src.m[2][2] - src.m[1][2] * src.m[2][1]) * oneOverS;
        m[0][1] = (src.m[0][2] * src.m[2][1] - src.m[0][1] * src.m[2][2]) * oneOverS;
        m[0][2] = (src.m[0][1] * src.m[1][2] - src.m[0][2] * src.m[1][1]) * oneOverS;

        m[1][0] = (src.m[1][2] * src.m[2][0] - src.m[1][0] * src.m[2][2]) * oneOverS;
        m[1][1] = (src.m[0][0] * src.m[2][2] - src.m[0][2] * src.m[2][0]) * oneOverS;
        m[1][2] = (src.m[0][2] * src.m[1][0] - src.m[0][0] * src.m[1][2]) * oneOverS;

        m[2][0] = (src.m[1][0] * src.m[2][1] - src.m[1][1] * src.m[2][0]) * oneOverS;
        m[2][1] = (src.m[0][1] * src.m[2][0] - src.m[0][0] * src.m[2][1]) * oneOverS;
        m[2][2] = (src.m[0][0] * src.m[1][1] - src.m[0][1] * src.m[1][0]) * oneOverS;
    }

    public double determinant()
    {
        return m[0][0] * (m[1][1] * m[2][2] - m[2][1] * m[1][2]) - m[0][1] * (m[1][0] * m[2][2] - m[2][0] * m[1][2]) + m[0][2] * (m[1][0] * m[2][1] - m[2][0] * m[1][1]);
    }

    public final void copyTo(Matrix9d targ)
    {
    	targ.m[0][0] = m[0][0];
    	targ.m[0][1] = m[0][1];
    	targ.m[0][2] = m[0][2];

    	targ.m[1][0] = m[1][0];
    	targ.m[1][1] = m[1][1];
    	targ.m[1][2] = m[1][2];

    	targ.m[2][0] = m[2][0];
    	targ.m[2][1] = m[2][1];
    	targ.m[2][2] = m[2][2];
    }
}
