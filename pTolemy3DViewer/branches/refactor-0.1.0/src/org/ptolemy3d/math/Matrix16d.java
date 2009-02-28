/**
 * Graphic Engine
 * Copyright © 2004-2008 Jérôme JOUVIE (Jouvieje)
 * 
 * PROJECT INFORMATIONS
 * ====================
 * Author   Jérôme JOUVIE (Jouvieje)
 * Email    jerome.jouvie@gmail.com
 * Site     http://jerome.jouvie.free.fr/
 * Homepage http://jerome.jouvie.free.fr/OpenGl/Projects/GraphicEngineCore.php
 * Version  GraphicEngineCore v0.1.5 Build 16-11-2008
 * 
 * LICENSE
 * =======
 * 
 * GNU GENERAL PUBLIC LICENSE (GPL)
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.ptolemy3d.math;

/**
 * A matrix 4x4 with double precision.
 */
public class Matrix16d
{
	public double[] m = new double[16];

	public static Matrix16d identity()
	{
		Matrix16d matrix = new Matrix16d();
		
		matrix.m[0 ] = 1;
		matrix.m[1 ] = 0;
		matrix.m[2 ] = 0;
		matrix.m[3 ] = 0;

		matrix.m[4 ] = 0;
		matrix.m[5 ] = 1;
		matrix.m[6 ] = 0;
		matrix.m[7 ] = 0;

		matrix.m[8 ] = 0;
		matrix.m[9 ] = 0;
		matrix.m[10] = 1;
		matrix.m[11] = 0;
		
		matrix.m[12] = 0;
		matrix.m[13] = 0;
		matrix.m[14] = 0;
		matrix.m[15] = 1;

		return matrix;
	}
	public static Matrix16d from(Matrix9d src)
	{
		Matrix16d matrix = new Matrix16d();
		
		matrix.m[ 0] = src.m[0][0];
		matrix.m[ 1] = src.m[0][1];
		matrix.m[ 2] = src.m[0][2];
		matrix.m[ 3] = 0;

		matrix.m[ 4] = src.m[1][0];
		matrix.m[ 5] = src.m[1][1];
		matrix.m[ 6] = src.m[1][2];
		matrix.m[ 7] = 0;

		matrix.m[ 8] = src.m[2][0];
		matrix.m[ 9] = src.m[2][1];
		matrix.m[10] = src.m[2][2];
		matrix.m[11] = 0;
		
		matrix.m[12] = 0;
		matrix.m[13] = 0;
		matrix.m[14] = 0;
		matrix.m[15] = 1;

		return matrix;
	}

	public final void copyFrom(Matrix9d src)
	{
		m[ 0] = src.m[0][0];
		m[ 1] = src.m[0][1];
		m[ 2] = src.m[0][2];

		m[ 4] = src.m[1][0];
		m[ 5] = src.m[1][1];
		m[ 6] = src.m[1][2];

		m[ 8] = src.m[2][0];
		m[ 9] = src.m[2][1];
		m[10] = src.m[2][2];
	}
	
	public final void identityMatrix()
	{
		m[0 ] = 1;
		m[1 ] = 0;
		m[2 ] = 0;
		m[3 ] = 0;

		m[4 ] = 0;
		m[5 ] = 1;
		m[6 ] = 0;
		m[7 ] = 0;

		m[8 ] = 0;
		m[9 ] = 0;
		m[10] = 1;
		m[11] = 0;
		
		m[12] = 0;
		m[13] = 0;
		m[14] = 0;
		m[15] = 1;
	}
	
	public final void translate(double x, double y, double z)
	{
		m[12] += m[0] * x + m[4] * y + m[ 8] * z;
		m[13] += m[1] * x + m[5] * y + m[ 9] * z;
		m[14] += m[2] * x + m[6] * y + m[10] * z;
	}
	
	public final void rotateX(double angleInRadians)
	{
		double cosA = Math.cos(angleInRadians);
		double sinA = Math.sin(angleInRadians);
		
		double m4, m5, m6, m8, m9, m10;

		m4  = cosA * this.m[4] + sinA * this.m[ 8];
		m5  = cosA * this.m[5] + sinA * this.m[ 9];
		m6  = cosA * this.m[6] + sinA * this.m[10];

		m8  = - sinA * this.m[4] + cosA * this.m[ 8];
		m9  = - sinA * this.m[5] + cosA * this.m[ 9];
		m10 = - sinA * this.m[6] + cosA * this.m[10];

		this.m[ 4] = m4;
		this.m[ 5] = m5;
		this.m[ 6] = m6;

		this.m[ 8] = m8;
		this.m[ 9] = m9;
		this.m[10] = m10;
	}
	
	public final void multiply(Matrix9d matrix)
	{
		double m0, m1, m2, m4, m5, m6, m8, m9, m10;
		
		m0  = matrix.m[0][0] * this.m[0] + matrix.m[0][1] * this.m[4] + matrix.m[0][2] * this.m[ 8];
		m1  = matrix.m[0][0] * this.m[1] + matrix.m[0][1] * this.m[5] + matrix.m[0][2] * this.m[ 9];
		m2  = matrix.m[0][0] * this.m[2] + matrix.m[0][1] * this.m[6] + matrix.m[0][2] * this.m[10];

		m4  = matrix.m[1][0] * this.m[0] + matrix.m[1][1] * this.m[4] + matrix.m[1][2] * this.m[ 8];
		m5  = matrix.m[1][0] * this.m[1] + matrix.m[1][1] * this.m[5] + matrix.m[1][2] * this.m[ 9];
		m6  = matrix.m[1][0] * this.m[2] + matrix.m[1][1] * this.m[6] + matrix.m[1][2] * this.m[10];

		m8  = matrix.m[2][0] * this.m[0] + matrix.m[2][1] * this.m[4] + matrix.m[2][2] * this.m[ 8];
		m9  = matrix.m[2][0] * this.m[1] + matrix.m[2][1] * this.m[5] + matrix.m[2][2] * this.m[ 9];
		m10 = matrix.m[2][0] * this.m[2] + matrix.m[2][1] * this.m[6] + matrix.m[2][2] * this.m[10];
		
		this.m[ 0] = m0;
		this.m[ 1] = m1;
		this.m[ 2] = m2;

		this.m[ 4] = m4;
		this.m[ 5] = m5;
		this.m[ 6] = m6;

		this.m[ 8] = m8;
		this.m[ 9] = m9;
		this.m[10] = m10;
	}
	
	public final void multiply(Matrix16d matrix)
	{
		double m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15;
		
		m0  = matrix.m[ 0] * this.m[ 0] + matrix.m[ 1] * this.m[ 4] + matrix.m[ 2] * this.m[ 8] + matrix.m[ 3] * this.m[12];
		m1  = matrix.m[ 0] * this.m[ 1] + matrix.m[ 1] * this.m[ 5] + matrix.m[ 2] * this.m[ 9] + matrix.m[ 3] * this.m[13];
		m2  = matrix.m[ 0] * this.m[ 2] + matrix.m[ 1] * this.m[ 6] + matrix.m[ 2] * this.m[10] + matrix.m[ 3] * this.m[14];
		m3  = matrix.m[ 0] * this.m[ 3] + matrix.m[ 1] * this.m[ 7] + matrix.m[ 2] * this.m[11] + matrix.m[ 3] * this.m[15];

		m4  = matrix.m[ 4] * this.m[ 0] + matrix.m[ 5] * this.m[ 4] + matrix.m[ 6] * this.m[ 8] + matrix.m[ 7] * this.m[12];
		m5  = matrix.m[ 4] * this.m[ 1] + matrix.m[ 5] * this.m[ 5] + matrix.m[ 6] * this.m[ 9] + matrix.m[ 7] * this.m[13];
		m6  = matrix.m[ 4] * this.m[ 2] + matrix.m[ 5] * this.m[ 6] + matrix.m[ 6] * this.m[10] + matrix.m[ 7] * this.m[14];
		m7  = matrix.m[ 4] * this.m[ 3] + matrix.m[ 5] * this.m[ 7] + matrix.m[ 6] * this.m[11] + matrix.m[ 7] * this.m[15];

		m8  = matrix.m[ 8] * this.m[ 0] + matrix.m[ 9] * this.m[ 4] + matrix.m[10] * this.m[ 8] + matrix.m[11] * this.m[12];
		m9  = matrix.m[ 8] * this.m[ 1] + matrix.m[ 9] * this.m[ 5] + matrix.m[10] * this.m[ 9] + matrix.m[11] * this.m[13];
		m10 = matrix.m[ 8] * this.m[ 2] + matrix.m[ 9] * this.m[ 6] + matrix.m[10] * this.m[10] + matrix.m[11] * this.m[14];
		m11 = matrix.m[ 8] * this.m[ 3] + matrix.m[ 9] * this.m[ 7] + matrix.m[10] * this.m[11] + matrix.m[11] * this.m[15];

		m12 = matrix.m[12] * this.m[ 0] + matrix.m[13] * this.m[ 4] + matrix.m[14] * this.m[ 8] + matrix.m[15] * this.m[12];
		m13 = matrix.m[12] * this.m[ 1] + matrix.m[13] * this.m[ 5] + matrix.m[14] * this.m[ 9] + matrix.m[15] * this.m[13];
		m14 = matrix.m[12] * this.m[ 2] + matrix.m[13] * this.m[ 6] + matrix.m[14] * this.m[10] + matrix.m[15] * this.m[14];
		m15 = matrix.m[12] * this.m[ 3] + matrix.m[13] * this.m[ 7] + matrix.m[14] * this.m[11] + matrix.m[15] * this.m[15];
		
		this.m[ 0] = m0;
		this.m[ 1] = m1;
		this.m[ 2] = m2;
		this.m[ 3] = m3;

		this.m[ 4] = m4;
		this.m[ 5] = m5;
		this.m[ 6] = m6;
		this.m[ 7] = m7;
		
		this.m[ 8] = m8;
		this.m[ 9] = m9;
		this.m[10] = m10;
		this.m[11] = m11;

		this.m[12] = m12;
		this.m[13] = m13;
		this.m[14] = m14;
		this.m[15] = m15;
	}
	
	public void invert3x3()
	{
		double det = determinant3x3();
		if(det == 0) {
			return;
		}
		
		det = 1 / det;
		
		double m0, m1, m2, m4, m5, m6, m8, m9, m10;
		
		m0  = (m[ 5] * m[10] - m[ 6] * m[ 9]) * det;
		m1  = (m[ 9] * m[ 2] - m[10] * m[ 1]) * det;
		m2  = (m[ 1] * m[ 6] - m[ 2] * m[ 5]) * det;
		
		m4  = (m[ 6] * m[ 8] - m[ 4] * m[10]) * det;
		m5  = (m[10] * m[ 0] - m[ 8] * m[ 2]) * det;
		m6  = (m[ 2] * m[ 4] - m[ 0] * m[ 6]) * det;
		
		m8  = (m[ 4] * m[ 9] - m[ 5] * m[ 8]) * det;
		m9  = (m[ 8] * m[ 1] - m[ 9] * m[ 0]) * det;
		m10 = (m[ 0] * m[ 5] - m[ 1] * m[ 4]) * det;
		
		m[ 0] = m0;
		m[ 1] = m1;
		m[ 2] = m2;
		
		m[ 4] = m4;
		m[ 5] = m5;
		m[ 6] = m6;
		
		m[ 8] = m8;
		m[ 9] = m9;
		m[10] = m10;
	}
	public final double determinant3x3()
	{
		return    (m[0] * m[5] - m[1] * m[4]) * m[10]
				- (m[0] * m[6] - m[2] * m[4]) * m[ 9]
				+ (m[1] * m[6] - m[2] * m[5]) * m[ 8];
	}
	
	/**
	 * Set OpenGL Perspective matrix
	 */
	public void setPerspectiveProjection(double fovy, double aspect, double zNear, double zFar)
	{
		double fovyOverTwo = fovy * (Math.PI / 360);	// 1 /2 * Degree to radians

		double  dz    = zNear - zFar;
		double sinus = Math.sin(fovyOverTwo);

		double cotangent = Math.cos(fovyOverTwo) / sinus;
		
		m[ 0] = cotangent / aspect;
		m[ 1] = 0;
		m[ 2] = 0;
		m[ 3] = 0;
		
		m[ 4] = 0;
		m[ 5] = cotangent;
		m[ 6] = 0;
		m[ 7] = 0;
		
		m[ 8] = 0;
		m[ 9] = 0;
		m[10] = (zFar + zNear) / dz;
		m[11] = -1;
		
		m[12] = 0;
		m[13] = 0;
		m[14] = 2 * zNear * zFar / dz;
		m[15] = 0;
	}
}
