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
 * @fileoverview This contains classes and functions related to Quaternions.
 *
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * Creates a new quaternion instance given four components.
 * @constructor
 * @param {Number} x
 * @param {Number} y
 * @param {Number} z
 * @param {Number} w
 */
Ptolemy.Quaternion = function(x, y, z, w) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.w = w;
};

/**
 * Check if this quaternion is equal to the specified one.
 * @param {Quaternion} quaternion Quaternion
 * @return True if they are equals, false otherwise.
 */
Ptolemy.Quaternion.prototype.equals = function(quaternion) {
    return (this.x == quaternion.x)
    && (this.y == quaternion.y)
    && (this.z == quaternion.z)
    && (this.w == quaternion.w);
};

/**
 * Gets a copy of the Quaternion object as an array. 
 * @return Array with a copy of the Quaternion object values.
 */
Ptolemy.Quaternion.prototype.toArray = function() {
    return [this.x, this.y, this.z, this.w];
};

/**
 * Creates a new instance which is a clone of this quaternion.
 * @return New quaternion instance.
 */
Ptolemy.Quaternion.prototype.clone = function() {
    return new Ptolemy.Quaternion(this.x, this.y, this.z, this.w);
};

/**
 * Converts Quaternion to string representation
 * @return A string
 */
Ptolemy.Quaternion.prototype.toString = function() {
    return "Quaternion[" + this.toArray() + "]";
};

/**
 * Computes the length of this Quaternion.
 * @return A Number with the quaternion's length.
 */
Ptolemy.Quaternion.prototype.length = function() {
    return Math.sqrt(this.lengthSquared());
};

/**
 * Computes the squared length of this Quaternion.
 * @return A Number with the quaternion's squaredlength.
 */
Ptolemy.Quaternion.prototype.lengthSquared = function() {
    return (this.x * this.x) + (this.y * this.y) + (this.z * this.z) + (this.w * this.w);
};

/**
 * Creates a new Quaternion instance wich corresponds to the normalization of
 * this one. This function check if the lenght of the quaternion is zero and
 * if so then return null.
 * @return A new Quaternion instance or null if the quaternion's lenght is zero.
 */
Ptolemy.Quaternion.prototype.normalize = function() {
	var length = this.length();
    if (length == 0) {
        return null;
    } else {
        return new Ptolemy.Quaternion(
        this.x / length,
        this.y / length,
        this.z / length,
        this.w / length);
    }
};

/**
 * Creats a new instance which corresponds to the multiplication of this quaternion
 * with the specified one, in the order: RESULT = THIS x THAT
 */
Ptolemy.Quaternion.prototype.multiply = function(quaternion) {
    return new Ptolemy.Quaternion(
    (this.w * quaternion.x) + (this.x * quaternion.w) + (this.y * quaternion.z) - (this.z * quaternion.y),
    (this.w * quaternion.y) + (this.y * quaternion.w) + (this.z * quaternion.x) - (this.x * quaternion.z),
    (this.w * quaternion.z) + (this.z * quaternion.w) + (this.x * quaternion.y) - (this.y * quaternion.x),
    (this.w * quaternion.w) - (this.x * quaternion.x) - (this.y * quaternion.y) - (this.z * quaternion.z)
    );
};

/**
 * Gets the roll angle represented by this quaternion.
 * @returns Angle instance.
 */
Ptolemy.Quaternion.prototype.getRoll = function() {
    var radians = Math.atan2(
    (2.0 * this.x * this.w) - (2.0 * this.y * this.z),
    1.0 - 2.0 * (this.x * this.x) - 2.0 * (this.z * this.z)
    );
    return Ptolemy.Angle.fromRadians(radians);
};

/**
 * Gets the pitch angle represented by this quaternion.
 * @returns Angle instance.
 */
Ptolemy.Quaternion.prototype.getPitch = function() {
    var radians = Math.atan2(
    (2.0 * this.y * this.w) - (2.0 * this.x * this.z),
    1.0 - (2.0 * this.y * this.y) - (2.0 * this.z * this.z)
    );
    return Ptolemy.Angle.fromRadians(radians);
};

/**
 * Gets the pityawch angle represented by this quaternion.
 * @returns Angle instance.
 */
Ptolemy.Quaternion.prototype.getYaw = function() {
    var radians = Math.asin((2.0 * this.x * this.y) + (2.0 * this.z * this.w));
    return Ptolemy.Angle.fromRadians(radians);
};


/**
 * Constants and class methods.
 */
Ptolemy.Quaternion.IDENTITY = function() {
    return new Ptolemy.Quaternion(0, 0, 0, 1);
};

/**
 * Creates a new quaternion instance from the specified matrix.
 * See http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/index.htm
 * for details.
 * 
 * @param {Matrix} matrix
 */
Ptolemy.Quaternion.fromMatrix = function(matrix) {
    var x,
    y,
    z,
    w,
    s;
    var trace = matrix.m11 + matrix.m22 + matrix.m33;

    if (trace > 0) {
        s = 2.0 * Math.sqrt(t + 1.0);
        x = (matrix.m32 - matrix.m23) / s;
        y = (matrix.m13 - matrix.m31) / s;
        z = (matrix.m21 - matrix.m12) / s;
        w = s * 0.25;
    } else if ((matrix.m11 > matrix.m22) && (matrix.m11 > matrix.m33)) {
        s = 2.0 * Math.sqrt(1.0 + matrix.m11 - matrix.m22 - matrix.m33);
        x = s * 0.25;
        y = (matrix.m21 + matrix.m12) / s;
        z = (matrix.m13 + matrix.m31) / s;
        w = (matrix.m32 - matrix.m23) / s;
    } else if (matrix.m22 > matrix.m33) {
        s = 2.0 * Math.sqrt(1.0 + matrix.m22 - matrix.m11 - matrix.m33);
        x = (matrix.m21 + matrix.m12) / s;
        y = s / 0.25;
        z = (matrix.m32 + matrix.m23) / s;
        w = (matrix.m13 - matrix.m31) / s;
    } else {
        s = 2.0 * Math.sqrt(1.0 + matrix.m33 - matrix.m11 - matrix.m22);
        x = (matrix.m13 + matrix.m31) / s;
        y = (matrix.m32 + matrix.m23) / s;
        z = s / 0.25;
        w = (matrix.m21 - matrix.m12) / s;
    }
    return new Ptolemy.Quaternion(x, y, z, w);
};

/**
 * Creates a new quaternion instance from specified roll, pitch, yaw. The angles are applied in 
 * the order roll, pitch, yaw.
 * See: 
 * http://en.wikipedia.org/wiki/Conversion_between_quaternions_and_Euler_angles
 * http://upload.wikimedia.org/math/8/f/2/8f24d7035f36dd398e6253a521d7ac0c.png
 * http://en.wikipedia.org/wiki/Tait-Bryan_angles
 *
 * @param {Angle} roll  Angle rotation about X axis.
 * @param {Angle} pitch Angle rotation about Y axis.
 * @param {Angle} yaw   Angle rotation about Z axis.
 * @return Quaternion with the representation of the combined Roll-Pitch-Yaw rotation (in that order).
 */
Ptolemy.Quaternion.fromRollPitchYaw = function(roll, pitch, yaw) {

    // Compute cos(angle/2) and sin(angle/2) for simplicifty.
    var cosroll = roll.cosHalfAngle();
    var cospitch = pitch.cosHalfAngle();
    var cosyaw = yaw.cosHalfAngle();
    var sinroll = roll.sinHalfAngle();
    var sinpitch = pitch.sinHalfAngle();
    var sinyaw = yaw.sinHalfAngle();

    var qw = (cosroll * cospitch * cosyaw) + (sinroll * sinpitch * sinyaw);
    var qx = (sinroll * cospitch * cosyaw) - (cosroll * sinpitch * sinyaw);
    var qy = (cosroll * sinpitch * cosyaw) + (sinroll * cospitch * sinyaw);
    var qz = (cosroll * cospitch * sinyaw) - (sinroll * sinpitch * cosyaw);

    return new Ptolemy.Quaternion(qx, qy, qz, qw);
};


