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
 * @fileoverview This contains class and functions related to Angles.
 * Operations with angles are common so it has sense to create a class with
 * the basic operations.
 *
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * Creates a new instance of Angle given a pair degree-radians values,
 * <b>Don't use this constructor directly. Instead use factory methods:</b>
 * @example
 * var angle = Ptolemy.Angle.fromDegrees(45);
 *
 * An Angle instance has two attributes: degrees and radians both storing the same
 * angle in different representation.
 *
 * @constructor
 */
Ptolemy.Angle = function(degrees, radians) {
    this.degrees = degrees;
    this.radians = radians;
};

/**
 * Creates a new Angle instance as addition of this angle with the specified one, that is,
 * result = this + angle.
 * @param {Angle} angle 
 * @return New Angle instance
 */
Ptolemy.Angle.prototype.add = function(angle) {
    return Ptolemy.Angle.fromDegrees(this.degrees + angle.degrees);
};

/**
 * Creates a new Angle instance as the substraction of the specified angle to this one, that is,
 * result = this-angle.
 * @param {Angle} angle 
 * @return New Angle instance
 */
Ptolemy.Angle.prototype.substract = function(angle) {
    return Ptolemy.Angle.fromDegrees(this.degrees - angle.degrees);
};

/**
 * Computes the cosine of this angel.
 */
Ptolemy.Angle.prototype.cos = function() {
    return Math.cos(this.radians);
};

/**
 * Computes the cosine of half this angel.
 */
Ptolemy.Angle.prototype.cosHalfAngle = function() {
    return Math.cos(0.5 * this.radians);
};

/**
 * Computes the sine of this angel.
 */
Ptolemy.Angle.prototype.sin = function() {
    return Math.sin(this.radians);
};

/**
 * Computes the sine of half this angel.
 */
Ptolemy.Angle.prototype.sinHalfAngle = function() {
    return Math.sin(0.5 * this.radians);
};

/**
 * Angle constants and class methods.
 */
Ptolemy.Angle.DEGREES_TO_RADIANS = Math.PI / 180.0;
Ptolemy.Angle.RADIANS_TO_DEGREES = 180.0 / Math.PI;

/**
 * Creates a new Angle instance from the specified degrees.
 * @param {Number} degrees
 */
Ptolemy.Angle.fromDegrees = function(degrees) {
    return new Ptolemy.Angle(degrees, Ptolemy.Angle.DEGREES_TO_RADIANS * degrees);
};

/**
 * Creates a new Angle instance from the specified radians.
 * @param {Number} radians
 */
Ptolemy.Angle.fromRadians = function(radians) {
    return new Ptolemy.Angle(radians * Ptolemy.Angle.RADIANS_TO_DEGREES, radians);
};


