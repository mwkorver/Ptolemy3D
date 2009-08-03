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
package org.ptolemy3d.globe;

/**
 * @author Jerome JOUVIE (Jouvieje) <jerome.jouvie@gmail.com>
 */
public class ElevationValue {
	/** No elevation */
	public final static int TYPE_DEFAULT = 0;
	/** Elevation come from DEM */
	public final static int TYPE_DEM = 1;
	/** Elevation come from TIN */
	public final static int TYPE_TIN = 2;
	
	/** Elevation value */
	public double height;
	/** Elevation type */
	public int type;
	/** Tell if the elevation is the result of an interpolation (elevation is stored as a grid) */
	public boolean interpolated;
	
	public ElevationValue(double height, int type, boolean interpolated) {
		this.height = height;
		this.type = type;
		this.interpolated = interpolated;
	}
	
	public void blend(ElevationValue that) {
		if(this.interpolated) {// || ((this.type == TYPE_DEM) && (that.type != TYPE_DEM))) {
			// Do nothing
		}
		else if(that.interpolated) {// || ((this.type != TYPE_DEM) && (that.type == TYPE_DEM))) {
			height = that.height;
			interpolated |= that.interpolated;
		}
		else {
			height = (this.height + that.height) / 2;
			//interpolated = true;
		}
	}
}
