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
 * @fileoverview This contains class and functions related to Vectors.
 * Althought Vectors have four dimensions operations works with the first
 * three component x,y,z. Operations that works for the four components
 * of the vector are named with a final '4', like 'add4' or 'substract4'.
 * 
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * Creates a new Vector4 instance given the float values. The parameter 'w' is optional
 * and if it doesn't exists the DEFAULT_W constant is used.
 * @constructor
 * @param {Number} x
 * @param {Number} y
 * @param {Number} z
 * @param {Number} w
 */
Ptolemy.Vector4 = function(x, y, z, w) {
    this.x = x;
    this.y = y;
    this.z = z;
    if (w==null) {
        this.w = Ptolemy.Vector4.DEFAULT_W();
    } else {
        this.w = w;
    }
};

/**
 * Check if this vector is equal to the specified vector.
 * @param {Vector4} vector Vector
 * @return True if they are equals, false otherwise.
 */
Ptolemy.Vector4.prototype.equals = function(vector) {
    return (this.x == vector.x) && (this.y == vector.y) && (this.z == vector.z) && (this.w == vector.w);
};

/**
 * Creates a new Vector instance which is a clone (with the same value) of
 * the specified one.
 * @param {Vector} vector Vector to clone
 * @returns New Vector instance.
 */
Ptolemy.Vector4.prototype.clone = function(vector) {
    return new Ptolemy.Vector4(vecot.x, vector.y, vector.z, vector.w);
};

/**
 * Gets a copy of the Vector object as an array. 
 * @return Array with a copy of the vector object values.
 */
Ptolemy.Vector4.prototype.toArray = function() {
    return [this.x, this.y, this.z, this.w];
};

/**
 * Converts Vector4 to s string representation
 * @return A string
 */
Ptolemy.Vector4.prototype.toString = function() {
    return "Vector4[" + this.toArray() + "]";
};

/**
 * Adds the three first components of this vector with the specified one.
 * @param {Vector4} vector Vector
 * @returns New Vector instance.
 */
Ptolemy.Vector4.prototype.add = function(vector) {
    return new Ptolemy.Vector4(
    this.x + vector.x,
    this.y + vector.y,
    this.z + vector.z,
    this.w);
};

/**
 * Adds the four components of this Vector with the specified one.
 * @param {Vector4} vector Vector
 * @returns New Vector instance.
 */
Ptolemy.Vector4.prototype.add4 = function(vector) {
    return new Ptolemy.Vector4(
    this.x + vector.x,
    this.y + vector.y,
    this.z + vector.z,
    this.w + vector.w);
};

/**
 * Substract to the three first components of this vector the specified one.
 * @param {Vector4} vector Vector
 * @returns New Vector instance.
 */
Ptolemy.Vector4.prototype.substract = function(vector) {
    return new Ptolemy.Vector4(
    this.x - vector.x,
    this.y - vector.y,
    this.z - vector.z,
    this.w);
};

/**
 * Substract to the four components of this Vector the specified one.
 * @param {Vector4} vector Vector
 * @returns New Vector instance.
 */
Ptolemy.Vector4.prototype.substract4 = function(vector) {
    return new Ptolemy.Vector4(
    this.x - vector.x,
    this.y - vector.y,
    this.z - vector.z,
    this.w - vector.w);
};

/**
 * Scale the three first components of this Vector by the specified value.
 * @param {Number} value Value to scale the vector
 * @returns New Vector instance.
 */
Ptolemy.Vector4.prototype.scale = function(value) {
    return new Ptolemy.Vector4(
    this.x * value,
    this.y * value,
    this.z * value,
    this.w);
};

/**
 * Divide the three first components of this Vector by the specified value.
 * @param {Number} value Value to divide the vector
 * @returns New Vector instance.
 */
Ptolemy.Vector4.prototype.divide = function(value) {
    return new Ptolemy.Vector4(
    this.x / value,
    this.y / value,
    this.z / value,
    this.w);
};

/**
 * Computes the dot product (only for the x,y,z components) between this vector 
 * and the specified one: this (dot) vector.
 * @param {Vector} vector
 * @return Number which corresponds to the dot product.
 */
Ptolemy.Vector4.prototype.dot = function(vector) {
    return (this.x * vector.x) + (this.y * vector.y) + (this.z * vector.z);
};

/**
 * Computes the dot product between this vector and the specified one: this (dot) vector,
 * the dot is applied to the four components of the vector.
 * @param {Vector} vector
 * @return Number which corresponds to the dot product.
 */
Ptolemy.Vector4.prototype.dot4 = function(vector) {
    return (this.x * vector.x) + (this.y * vector.y) + (this.z * vector.z) + (this.w * vector.w);
};

/**
 * Computes the cross product (for x,y,z components) between this vector and the specified one.
 * @param {Vector} vector.
 * @returns New Vector instance.
 */
Ptolemy.Vector4.prototype.cross = function(vector) {
    return new Ptolemy.Vector4(
    (this.y * vector.z) - (this.z * vector.y),
    (this.z * vector.x) - (this.x * vector.z),
    (this.x * vector.y) - (this.y * vector.x),
    this.w
    );
};

/**
 * Computes the length of this Vector for the first three components x,y,z.
 * @return a Number with the vector's length.
 */
Ptolemy.Vector4.prototype.length = function() {
    return Math.sqrt(this.lengthSquared());
};

/**
 * Computes the squared length of this Vector for the first three components x,y,z.
 * @return a Number with the vector's length.
 */
Ptolemy.Vector4.prototype.lengthSquared = function() {
    return (this.x * this.x) + (this.y * this.y) + (this.z * this.z);
};

/**
 * Creates a new Vector instance with corresponds with the normalization of this vector.
 * This function check if the computed vector's lenght is zero and if so returns null.
 * @returns New Vector instance or 'null' if the length of the vector is zero.
 */
Ptolemy.Vector4.prototype.normalize = function() {
    var length = this.length();
    if (length == 0) {
        return null;
    } else {
        return new Ptolemy.Vector4(
        this.x / length,
        this.y / length,
        this.z / length,
        this.w
        );
    }
};

/**
 * Computes the squared distance to the specified Vector
 * @param {Vector4} vector
 * @return Squared distance.
 */
Ptolemy.Vector4.prototype.distanceToSquared = function(vector) {
    var distSquared = 0;
    var tempOp = this.x - vector.x;
    distSquared += tempOp * tempOp;
    tempOp = this.y - vector.y;
    distSquared += tempOp * tempOp;
    tempOp = this.z - vector.z;
    distSquared += tempOp * tempOp;
    return distSquared;
};

/**
 * Computes the distance to the specified Vector
 * @param {Vector4} vector
 * @return Computed distance.
 */
Ptolemy.Vector4.prototype.distance = function(vector) {
    return Math.sqrt(this.distanceToSquared(vector));
};

/**
 * Vector constants. Next functions are defined to act as constants ensuring
 * user cant modify the value they return. 
 * To get its values simply invoke as a normal function:
 *
 *   var d = Ptolemy.Vector4.DEFAULT_W();
 */
Ptolemy.Vector4.DEFAULT_W = function() {
    return 1.0;
};

Ptolemy.Vector4.UNIT_X = function() {
    return new Ptolemy.Vector4(1, 0, 0, 0);
};
Ptolemy.Vector4.UNIT_Y = function() {
    return new Ptolemy.Vector4(0, 1, 0, 0);
};
Ptolemy.Vector4.UNIT_Z = function() {
    return new Ptolemy.Vector4(0, 0, 1, 0);
};
Ptolemy.Vector4.UNIT_W = function() {
    return new Ptolemy.Vector4(0, 0, 0, 1);
};

Ptolemy.Vector4.UNIT_X_NEGATIVE = function() {
    return new Ptolemy.Vector4(0 - 1, 0, 0, 0);
};
Ptolemy.Vector4.UNIT_Y_NEGATIVE = function() {
    return new Ptolemy.Vector4(0, 0 - 1, 0, 0);
};
Ptolemy.Vector4.UNIT_Z_NEGATIVE = function() {
    return new Ptolemy.Vector4(0, 0, 0 - 1, 0);
};
Ptolemy.Vector4.UNIT_W_NEGATIVE = function() {
    return new Ptolemy.Vector4(0, 0, 0, 0 - 1);
};

