/**
 * Ptolemy 3D - Open Source Virtual Globe framework.
 * Please visit ptolemy3d.org for more information on the project.
 * Copyright (C) 2010 Antonio Santiago (asantiagop@gmail.com)
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

/**
 * @fileoverview Position represents a point position based on a latitude, longitude and elevation.
 * 
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * Position representas geographical position given a LatLon and an elevation.
 * @constructor
 * @param {Ptolemy.LatLon} latlon
 * @param {Number} eleva.
 */
Ptolemy.Position = function(latlon, elev){
	this.latlon = latlon;
	this.elevation = elev;
};


