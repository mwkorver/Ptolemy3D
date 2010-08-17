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
* @fileoverview Globe contains the objects to be rendered.
* The Globe can have one or more elevation models which provides the terrain elevations. 
* Elevation models are used from the beggining to the end of the array to server terrain elevation 
* information.
* In the same way a Globe contains an array of RenderableObject which are rendered from the
* beggining to the end of the array.
* 
* @author Antonio Santiago asantiagop(at)gmail(dot)com
*/

/**
 * Creates a new Globe instance.
 * @constructor
 */
Ptolemy.Globe = function() {
    this.sectorsArray = [];
    this.elevationModelsArray = [];
    this.renderableObjectsArray = [];

	this.initializeSectors();
};

/**
* Add a new elevation to the elevation model array. 
* @param elevationModel
*/
Ptolemy.Globe.prototype.addElevationModel = function(elevationModel) {
    this.elevationModelArray.push(elevationModel);
};

/**
* Adds a new renderable object to renderable objects array.
* @param renderableObject
*/
Ptolemy.Globe.prototype.addRenderableObject = function(renderableObject) {
    this.renderableObjectsArray.push(renderableObject);
};

Ptolemy.Globe.prototype.initializeSectors = function() {
    for (var lat = Ptolemy.Globe.MIN_LAT; lat < Ptolemy.Globe.MAX_LAT; lat += Ptolemy.Globe.LAT_STEP) {
        for (var lon = Ptolemy.Globe.MIN_LON; lon < Ptolemy.Globe.MAX_LON; lon += Ptolemy.Globe.LON_STEP) {
            var sector = new Ptolemy.Sector(lat, lon, lat + Ptolemy.Globe.LAT_STEP, lon + Ptolemy.Globe.LON_STEP);
            this.addRenderableObject(sector);
        }
    }
};

/**
 * Constants and class methods.
 */

// TODO - Globe is considered a sphere so translation methods translates from cartesian
//		to spherical coordinates.
//		We will need to improve this and use some elipsoid to represent earth and more
//		acurate values, mainly when computing distances, etc.
Ptolemy.Globe.RADIUS = function() {
    return 6378137.0;
};

/**
 * Computes the geographic position from given cartesion coordinates.
 *
 * NOTE: This function cosiders the earth as an sphere. See
 * See: http://en.wikipedia.org/wiki/Spherical_coordinate_system
 *      http://mathworld.wolfram.com/SphericalCoordinates.html
 *
 * @param {Vector4} vector The cartesion position to convert to geographic.
 * @return A Position instance containing the geographic position.
 */
Ptolemy.Globe.computeGeographicFromCartesion = function(vector) {
    var radius = vector.length();
    var theta = Math.atan2(vector.y / vector.x);
    var phi = Mat.acos(vector.x / radius);

    var lat = Ptolemy.Angle.fromDegrees(90).substract(phi);
    var lon = Ptolemy.Angle.fromDegrees(180).substract(theta);
    var latlon = new Ptolemy.LatLon(lat, lon);

    return new Ptolemy.Position(latlon, radius);
};

/**
 * Computes cartesion position (x,y,z) in model coordinates from given
 * geographic position.
 *
 * NOTE: This function cosiders the earth as an sphere. See
 * See: http://en.wikipedia.org/wiki/Spherical_coordinate_system
 *      http://mathworld.wolfram.com/SphericalCoordinates.html
 *
 * @param {Position} position The geographic position to convert to cartesion.
 * @return A Vector4 instance with x,y,z componentes containing the cartesion values.
 */
Ptolemy.Globe.computeCartesionFromGeographic = function(position) {
    var lat = position.latlon.latitude;
    var lon = position.latlon.longitude;
    var elev = position.elevation;

    var phi = Ptolemy.Angle.fromDegrees(90).substract(lat);
    var theta = Ptolemy.Angle.fromDegrees(180).add(lon);
    var radius = Ptolemy.Globe.RADIUS() + elev;

    var x = radius * theta.cos() * phi.sin();
    var y = radius * theta.sin() * phi.sin();
    var z = radius * phi.cos();

    return new Ptolemy.Vector4(x, y, z);
};


Ptolemy.Globe.LAT_STEP = 10;
Ptolemy.Globe.LON_STEP = 10;

Ptolemy.Globe.MIN_LAT = -90;
Ptolemy.Globe.MAX_LAT = 90;
Ptolemy.Globe.MIN_LON = -180;
Ptolemy.Globe.MAX_LON = 180;



/***************************************************************
* Sector
*/
Ptolemy.Sector = function(minLat, minLon, maxLat, maxLon) {
    this.minLat = minLat;
    this.minLon = minLon;
    this.maxLat = maxLat;
    this.maxLon = maxLon;

    this.initialized = false;
    this.sectorVertices = null;
    this.sectorVerticesBuffer = null;
    this.sectorIndices = null;
    this.sectorIndicesBuffer = null;
};

Ptolemy.Sector.prototype.initialize = function(dc) {
    var gl = dc.gl;
    var p1 = new Ptolemy.Position(new Ptolemy.LatLon(
		Ptolemy.Angle.fromDegrees(this.minLat), 
		Ptolemy.Angle.fromDegrees(this.minLon)), 
		Ptolemy.Globe.RADIUS());
    var p2 = new Ptolemy.Position(new Ptolemy.LatLon(
		Ptolemy.Angle.fromDegrees(this.minLat), 
		Ptolemy.Angle.fromDegrees(this.maxLon)), 
		Ptolemy.Globe.RADIUS());
    var p3 = new Ptolemy.Position(new Ptolemy.LatLon(
		Ptolemy.Angle.fromDegrees(this.maxLat), 
		Ptolemy.Angle.fromDegrees(this.maxLon)), 
		Ptolemy.Globe.RADIUS());
    var p4 = new Ptolemy.Position(new Ptolemy.LatLon(
		Ptolemy.Angle.fromDegrees(this.maxLat), 
		Ptolemy.Angle.fromDegrees(this.minLon)), 
		Ptolemy.Globe.RADIUS());

	var v1 = Ptolemy.Globe.computeCartesionFromGeographic(p1);
	var v2 = Ptolemy.Globe.computeCartesionFromGeographic(p2);
	var v3 = Ptolemy.Globe.computeCartesionFromGeographic(p3);
	var v4 = Ptolemy.Globe.computeCartesionFromGeographic(p4);

    this.sectorVertices = [
    v1.x, v1.y, v1.z,
    v2.x, v2.y, v2.z,
    v3.x, v3.y, v3.z,
    v4.x, v4.y, v4.z
    ];

    this.sectorIndices = [0, 1, 2, 3];

    this.sectorVerticesBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, this.sectorVerticesBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new WebGLFloatArray(this.sectorVertices), gl.STATIC_DRAW);
    this.sectorVerticesBuffer.itemSize = 3;
    this.sectorVerticesBuffer.numItems = 4;

    this.sectorIndicesBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.sectorIndicesBuffer);
    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, new WebGLUnsignedShortArray(this.sectorIndices), gl.STATIC_DRAW);
    this.sectorIndicesBuffer.itemSize = 1;
    this.sectorIndicesBuffer.numItems = 4;

    this.initialized = true;
};

Ptolemy.Sector.prototype.render = function(dc) {
    var gl = dc.gl;
    if (!this.initialized) {
        this.initialize(dc);
    }

    gl.bindBuffer(gl.ARRAY_BUFFER, this.sectorVerticesBuffer);
    gl.vertexAttribPointer(Ptolemy.shaderProgram.vertexPositionAttribute, this.sectorVerticesBuffer.itemSize, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.sectorIndicesBuffer);
    gl.drawElements(gl.LINE_LOOP, this.sectorIndicesBuffer.numItems, gl.UNSIGNED_SHORT, 0);
};



