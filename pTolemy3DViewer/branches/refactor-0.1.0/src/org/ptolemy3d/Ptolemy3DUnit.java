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

package org.ptolemy3d;

/**
 * System unit used in Ptolemy3D.
 * 
 * @author Jerome Jouvie
 * @author Antonio Santiago <asantiagop(at)gmail(dot)com>
 */
public final class Ptolemy3DUnit {
	/** 1 <=> DD */
	public static final int DEFAULT_DD = 1000000;

	private int dd = DEFAULT_DD;
	/** */
	private int meterX;
	/** */
	private int meterZ;
	/** */
	private float coordSystemRatio;

	/**
	 * Creates a new instance.
	 * 
	 * @param meterX
	 * @param meterZ
	 * @param coordSystemRatio
	 */
	public Ptolemy3DUnit(int meterX, int meterZ, float coordSystemRatio) {
		this.meterX = meterX;
		this.meterZ = meterZ;
		this.coordSystemRatio = coordSystemRatio;
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param meterX
	 * @param meterZ
	 * @param coordSystemRatio
	 * @param dd
	 */
	public Ptolemy3DUnit(int meterX, int meterZ, float coordSystemRatio, int dd) {
		this.meterX = meterX;
		this.meterZ = meterZ;
		this.coordSystemRatio = coordSystemRatio;
		this.dd = dd;
	}

	/**
	 * Convert degrees to DD.
	 * 
	 * @param degrees
	 *            angle in degree to convert
	 * @return the angle mesured in DD unit
	 * @see #ddToDegrees
	 */
	public final int degreesToDD(double degrees) {
		return (int) (degrees * this.dd);
	}

	/**
	 * Convert DD to degrees.
	 * 
	 * @param dd
	 *            angle in DD to convert
	 * @return the angle mesured in degrees unit
	 * @see #degreesToDD
	 */
	public final double ddToDegrees(int dd) {
		return (double) dd / this.dd;
	}

	/**
	 * @return the meterX
	 */
	public int getMeterX() {
		return meterX;
	}

	/**
	 * @return the meterZ
	 */
	public int getMeterZ() {
		return meterZ;
	}

	/**
	 * @return the coordSystemRatio
	 */
	public float getCoordSystemRatio() {
		return coordSystemRatio;
	}

	/**
	 * @return the dD
	 */
	public int getDD() {
		return this.dd;
	}
}