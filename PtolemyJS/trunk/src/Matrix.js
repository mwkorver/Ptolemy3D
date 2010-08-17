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
 * @fileoverview This contains classes and functions related to Matrices.
 * Matrices are stored in a 16 length array in row major order, because
 * "visually" it is more easy to specify the values of the matrix at creation
 * time. Independently of this 'toArray' method allows to return the matrix
 * as an array in both column major order (ready for OpenGL operations)
 * or row major order.
 *
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * Creates a new matrix instance given 16 number values which must be specified
 * in row major order, which is more "visually easy". Anyway, later when you want
 * to pass the 16-array to WebGL 'toArray()' method returns by default the matrix
 * in column major order which is suitable for GL.
 * Parameters represent a matrix like:
 * @example
 * | m11 m12 m13 m14 |
 * | m21 m22 m23 m24 |
 * | m31 m32 m33 m34 |
 * | m41 m42 m43 m44 |
 * @constructor
 */
Ptolemy.Matrix = function(m11, m12, m13, m14, m21, m22, m23, m24, m31, m32, m33, m34, m41, m42, m43, m44) {
    // Row 1
    this.m11 = m11;
    this.m12 = m12;
    this.m13 = m13;
    this.m14 = m14;
    // Row 2
    this.m21 = m21;
    this.m22 = m22;
    this.m23 = m23;
    this.m24 = m24;
    // Row 3
    this.m31 = m31;
    this.m32 = m32;
    this.m33 = m33;
    this.m34 = m34;
    // Row 4
    this.m41 = m41;
    this.m42 = m42;
    this.m43 = m43;
    this.m44 = m44;
};

/**
 * Check if this matrix is equal to the specified Matrix.
 * @param {Matrix} Matrix to compare this one.
 * @return True if they are euquals, flase otherwise.
 */
Ptolemy.Matrix.prototype.equals = function(matrix) {
    return (this.m11 == matrix.m11) && (this.m12 == matrix.m12) && (this.m13 == matrix.m13) && (this.m14 == matrix.m14)
    && (this.m21 == matrix.m21) && (this.m22 == matrix.m22) && (this.m23 == matrix.m23) && (this.m24 == matrix.m24)
    && (this.m31 == matrix.m31) && (this.m32 == matrix.m32) && (this.m33 == matrix.m33) && (this.m34 == matrix.m34)
    && (this.m41 == matrix.m41) && (this.m42 == matrix.m42) && (this.m43 == matrix.m43) && (this.m44 == matrix.m44);
};

/**
 * Gets a copy of the matrix in as an array.
 * @param {Boolean} order Specifies if array must be in row major order, that is,
 * true means row major order, otherwise (null or false) is column major order.
 *
 * @example Given the matrix
 * | m11 m12 m13 m14 |
 * | m21 m22 m23 m24 |
 * | m31 m32 m33 m34 |
 * | m41 m42 m43 m44 |
 *
 * using toArray() or toArray(false) returns the array
 * [m11, m21, m31, m41, m12, m22, m32, m42, m13, m23, m33, m43, m14, m24, m34, m44]
 *
 * while using toArray(true) will return:
 * [m11, m12, m13, m14, m21, m22, m23, m24, m31, m32, m33, m34, m41, m42, m43, m44]
 *
 * @returns An array with a copy of the values of the matrix, by default or if 'order'
 * is null or false, it returns in column major order which is ready for OpenGL operations, 
 * otherwise (if order is true) it return the array in row major order .
 */
Ptolemy.Matrix.prototype.toArray = function(order) {
    if (!order) {
        // Column major order
        return [
        this.m11, this.m21, this.m31, this.m41,
        this.m12, this.m22, this.m32, this.m42,
        this.m13, this.m23, this.m33, this.m43,
        this.m14, this.m24, this.m34, this.m44
        ];
    } else {
        // Row major order
        return [
        this.m11, this.m12, this.m13, this.m14,
        this.m21, this.m22, this.m23, this.m24,
        this.m31, this.m32, this.m33, this.m34,
        this.m41, this.m42, this.m43, this.m44
        ];
    }
};

/**
 * Gets a string representation of the matrix.
 */
Ptolemy.Matrix.prototype.toString = function() {
    return "Matrix[ " +
    "Row1[" + this.m11 + ", " + this.m12 + ", " + this.m13 + ", " + this.m14 + "] " +
    "Row2[" + this.m21 + ", " + this.m22 + ", " + this.m23 + ", " + this.m24 + "] " +
    "Row3[" + this.m31 + ", " + this.m32 + ", " + this.m33 + ", " + this.m34 + "] " +
    "Row4[" + this.m41 + ", " + this.m42 + ", " + this.m43 + ", " + this.m44 + "] " +
    "]";
};

/**
 * Creates a new Matrix instance which is a cloned (with the same values) of this one.
 */
Ptolemy.Matrix.prototype.clone = function() {
    return new Ptolemy.Matrix(
        this.m11, this.m12, this.m13, this.m14,
        this.m21, this.m22, this.m23, this.m24,
        this.m31, this.m32, this.m33, this.m34,
        this.m41, this.m42, this.m43, this.m44
        );
};

/**
 * Creates a new Matrix instance which corresponds to the multiplication of this matrix
 * with the specified one: (THIS) x (MATRIX) = (RESULT)
 * @param {Matrix} matrix
 * @return New Matrix instance.
 */
Ptolemy.Matrix.prototype.multiply = function(matrix) {
    return new Ptolemy.Matrix(
        // Row 1
        (this.m11 * matrix.m11) + (this.m12 * matrix.m21) + (this.m13 * matrix.m31) + (this.m14 * matrix.m41),
        (this.m11 * matrix.m12) + (this.m12 * matrix.m22) + (this.m13 * matrix.m32) + (this.m14 * matrix.m42),
        (this.m11 * matrix.m13) + (this.m12 * matrix.m23) + (this.m13 * matrix.m33) + (this.m14 * matrix.m43),
        (this.m11 * matrix.m14) + (this.m12 * matrix.m24) + (this.m13 * matrix.m34) + (this.m14 * matrix.m44),
        // Row 2
        (this.m21 * matrix.m11) + (this.m22 * matrix.m21) + (this.m23 * matrix.m31) + (this.m24 * matrix.m41),
        (this.m21 * matrix.m12) + (this.m22 * matrix.m22) + (this.m23 * matrix.m32) + (this.m24 * matrix.m42),
        (this.m21 * matrix.m13) + (this.m22 * matrix.m23) + (this.m23 * matrix.m33) + (this.m24 * matrix.m43),
        (this.m21 * matrix.m14) + (this.m22 * matrix.m24) + (this.m23 * matrix.m34) + (this.m24 * matrix.m44),
        // Row 3
        (this.m31 * matrix.m11) + (this.m32 * matrix.m21) + (this.m33 * matrix.m31) + (this.m34 * matrix.m41),
        (this.m31 * matrix.m12) + (this.m32 * matrix.m22) + (this.m33 * matrix.m32) + (this.m34 * matrix.m42),
        (this.m31 * matrix.m13) + (this.m32 * matrix.m23) + (this.m33 * matrix.m33) + (this.m34 * matrix.m43),
        (this.m31 * matrix.m14) + (this.m32 * matrix.m24) + (this.m33 * matrix.m34) + (this.m34 * matrix.m44),
        // Row 4
        (this.m41 * matrix.m11) + (this.m42 * matrix.m21) + (this.m43 * matrix.m31) + (this.m44 * matrix.m41),
        (this.m41 * matrix.m12) + (this.m42 * matrix.m22) + (this.m43 * matrix.m32) + (this.m44 * matrix.m42),
        (this.m41 * matrix.m13) + (this.m42 * matrix.m23) + (this.m43 * matrix.m33) + (this.m44 * matrix.m43),
        (this.m41 * matrix.m14) + (this.m42 * matrix.m24) + (this.m43 * matrix.m34) + (this.m44 * matrix.m44)
        );
};

/**
 * Computes this inverse matrix.<br>
 * NOTE: This code is a port from WWJ0.6 Matrix.computeGeneralInverse() methos.
 * Also see: http://www.euclideanspace.com/maths/algebra/matrix/functions/inverse/fourD/index.htm
 *
 * @returns Inverse matrix.
 */
Ptolemy.Matrix.prototype.inverse=function(){
    var cf_11 = a.m22 * (a.m33 * a.m44 - a.m43 * a.m34) - a.m23 * (a.m32 * a.m44 - a.m42 * a.m34) + a.m24 * (a.m32 * a.m43 - a.m42 * a.m33);
    var cf_12 = -(a.m21 * (a.m33 * a.m44 - a.m43 * a.m34) - a.m23 * (a.m31 * a.m44 - a.m41 * a.m34) + a.m24 * (a.m31 * a.m43 - a.m41 * a.m33));
    var cf_13 = a.m21 * (a.m32 * a.m44 - a.m42 * a.m34) - a.m22 * (a.m31 * a.m44 - a.m41 * a.m34) + a.m24 * (a.m31 * a.m42 - a.m41 * a.m32);
    var cf_14 = -(a.m21 * (a.m32 * a.m43 - a.m42 - a.m33) - a.m22 * (a.m31 * a.m43 - a.m41 * a.m33) + a.m23 * (a.m31 * a.m42 - a.m41 * a.m32));
    var cf_21 = a.m12 * (a.m33 * a.m44 - a.m43 - a.m34) - a.m13 * (a.m32 * a.m44 - a.m42 * a.m34) + a.m14 * (a.m32 * a.m43 - a.m42 * a.m33);
    var cf_22 = -(a.m11 * (a.m33 * a.m44 - a.m43 * a.m34) - a.m13 * (a.m31 * a.m44 - a.m41 * a.m34) + a.m14 * (a.m31 * a.m43 - a.m41 * a.m33));
    var cf_23 = a.m11 * (a.m32 * a.m44 - a.m42 * a.m34) - a.m12 * (a.m31 * a.m44 - a.m41 * a.m34) + a.m14 * (a.m31 * a.m42 - a.m41 * a.m32);
    var cf_24 = -(a.m11 * (a.m32 * a.m43 - a.m42 * a.m33) - a.m12 * (a.m31 * a.m43 - a.m41 * a.m33) + a.m13 * (a.m31 * a.m42 - a.m41 * a.m32));
    var cf_31 = a.m12 * (a.m23 * a.m44 - a.m43 * a.m24) - a.m13 * (a.m22 * a.m44 - a.m42 * a.m24) + a.m14 * (a.m22 * a.m43 - a.m42 * a.m23);
    var cf_32 = -(a.m11 * (a.m23 * a.m44 - a.m43 * a.m24) - a.m13 * (a.m21 * a.m44 - a.m41 * a.m24) + a.m14 * (a.m24 * a.m43 - a.m41 * a.m23));
    var cf_33 = a.m11 * (a.m22 * a.m44 - a.m42 * a.m24) - a.m12 * (a.m21 * a.m44 - a.m41 * a.m24) + a.m14 * (a.m21 * a.m42 - a.m41 * a.m22);
    var cf_34 = -(a.m11 * (a.m22 * a.m33 - a.m32 * a.m23) - a.m12 * (a.m21 * a.m33 - a.m31 * a.m23) + a.m13 * (a.m21 * a.m32 - a.m31 * a.m22));
    var cf_41 = a.m12 * (a.m23 * a.m34 - a.m33 * a.m24) - a.m13 * (a.m22 * a.m34 - a.m32 * a.m24) + a.m14 * (a.m22 * a.m33 - a.m32 * a.m23);
    var cf_42 = -(a.m11 * (a.m23 * a.m34 - a.m33 * a.m24) - a.m13 * (a.m21 * a.m34 - a.m31 * a.m24) + a.m14 * (a.m21 * a.m33 - a.m31 * a.m23));
    var cf_43 = a.m11 * (a.m22 * a.m34 - a.m32 * a.m24) - a.m12 * (a.m21 * a.m34 - a.m31 * a.m24) + a.m14 * (a.m21 * a.m32 - a.m31 * a.m22);
    var cf_44 = -(a.m11 * (a.m22 * a.m33 - a.m32 * a.m23) - a.m12 * (a.m21 * a.m33 - a.m31 * a.m23) + a.m13 * (a.m21 * a.m32 - a.m31 * a.m22));

    var det = (a.m11 * cf_11) + (a.m12 * cf_12) + (a.m13 * cf_13) + (a.m14 * cf_14);

    if (det==0){
        return null;
    }
    return new Ptolemy.Matrix(
        cf_11 / det, cf_21 / det, cf_31 / det, cf_41 / det,
        cf_12 / det, cf_22 / det, cf_32 / det, cf_42 / det,
        cf_13 / det, cf_23 / det, cf_33 / det, cf_43 / det,
        cf_14 / det, cf_24 / det, cf_34 / det, cf_44 / det);
};

/**
 * Matrix constants and class methods
 */

/**
 * Return the identity matrix.
 */
Ptolemy.Matrix.IDENTITY = function() {
    return new Ptolemy.Matrix(
        1, 0, 0, 0,
        0, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1);
};

/**
 * Creates a new matrix instances from the first 16 elements of an array. The array can
 * be specified in column or row major order, simply indicate it with the 'order' parameter.
 * @param {Array} marray
 * @param {Boolean} order True means the array is supposed to be in row major order, otherwise
 * it is suppose to be in column major order.
 */
Ptolemy.Matrix.fromArray = function(marray, order) {
    if (!order) {
        // Column major order
        return new Ptolemy.Matrix(
            marray[0], marray[4], marray[8], marray[12],
            marray[1], marray[5], marray[9], marray[13],
            marray[2], marray[6], marray[10], marray[14],
            marray[3], marray[7], marray[11], marray[15]);
    } else {
        // Row major order
        return new Ptolemy.Matrix(
            marray[0], marray[1], marray[2], marray[3],
            marray[4], marray[5], marray[6], marray[7],
            marray[8], marray[9], marray[10], marray[11],
            marray[12], marray[13], marray[14], marray[15]);
    }
};

/**
 * Creates a Matrix instance for an orthographic projection.
 *  
 * @param {float} left Left vertical clipping panel.
 * @param {float} right Right vertical clipping plane.
 * @param {float} bottom Bottom horizontal clipping plane.
 * @param {float} top Top horizontal clipping plane.
 * @param {float} znear Near clipping plane.
 * @param {float} zfar Far clipping plane.
 * @returns Valid Matrix to set an orthogrpahic projection.
 */
Ptolemy.Matrix.fromOthographic = function(left, right, bottom, top, znear, zfar) {
    var tx = -(right + left) / (right - left);
    var ty = -(top + bottom) / (top - bottom);
    var tz = -(zfar + znear) / (zfar - znear);

    return new Ptolemy.Matrix(
        2 / (left - right), 0, 0, tx,
        0, 2 / (top - bottom), 0, ty,
        0, 0, -2 / (zfar - znear), tz,
        0, 0, 0, 1);
};

/**
 * Creates a Matrix instance for a perspective projection.
 *
 * @param {float} fov Field of view in degrees in the Y direction.
 * @param {float} aspect Aspect ratio
 * @param {float} znear Distance from viewer to near clipping plane.
 * @param {float} zfar Distance from viewer to far clipping plane.
 * @returns Valid Matrix to set a perspective projection. 
 */
Ptolemy.Matrix.fromPerspective = function(fov, aspect, znear, zfar) {
    // Compute the clipping planes.
    var ymax = znear * Math.tan(fov * Ptolemy.Angle.DEGREES_TO_RADIANS / 2);
    var ymin = -ymax;
    var xmin = ymin * aspect;
    var xmax = ymax * aspect;

    return Ptolemy.Matrix.fromFrustum(xmin, xmax, ymin, ymax, znear, zfar);
};

/**
 * Creates a Matrix instance for a frustum.
 *
 * @param {float} left Left vertical clipping panel.
 * @param {float} right Right vertical clipping plane.
 * @param {float} bottom Bottom horizontal clipping plane.
 * @param {float} top Top horizontal clipping plane.
 * @param {float} znear Near clipping plane.
 * @param {float} zfar Far clipping plane.
 * @returns Valid Matrix to set a frustum.
 */
Ptolemy.Matrix.fromFrustum = function(left, right, bottom, top, znear, zfar) {
    var X = 2 * znear / (right - left);
    var Y = 2 * znear / (top - bottom);
    var A = (right + left) / (right - left);
    var B = (top + bottom) / (top - bottom);
    var C = -(zfar + znear) / (zfar - znear);
    var D = -2 * zfar * znear / (zfar - znear);

    return new Ptolemy.Matrix(
        X, 0, A, 0,
        0, Y, B, 0,
        0, 0, C, D,
        0, 0, -1, 0
        );
};

/**
 * Creates a new Matrix instance from the given Quaternion.
 * See http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToMatrix/index.htm.
 * 
 * @param {Quaternion} quaternion
 */
Ptolemy.Matrix.fromQuaternion = function(quaternion) {

    var quatnormal = quaternion.normalize();
    var xx = quatnormal.x * quatnormal.x;
    var xy = quatnormal.x * quatnormal.y;
    var xz = quatnormal.x * quatnormal.z;
    var xw = quatnormal.x * quatnormal.w;
    var yy = quatnormal.y * quatnormal.y;
    var yz = quatnormal.y * quatnormal.z;
    var yw = quatnormal.y * quatnormal.w;
    var zz = quatnormal.z * quatnormal.z;
    var zw = quatnormal.z * quatnormal.w;

    return new Ptolemy.Matrix(
        // Row 1
        1 - 2 * (yy + zz),
        2 * (xy - zw),
        2 * (xz + yw),
        0.0,
        // Row 2
        2 * (xy + zw),
        1 - 2 * (xx + zz),
        2 * (yz - xw),
        0.0,
        // Row 3
        2 * (xz - yw),
        2 * (yz + xw),
        1 - 2 * (xx + yy),
        0.0,
        // Row 4
        0.0, 0.0, 0.0, 1.0);
};

/**
 * Creates a translation matrix. Values are in model coordinates, so if you want to see the 
 * center point (0,0,0) at distance 100 in X axis you must specify a value x=-100.
 * @param {Number} x Translation in axis X.
 * @param {Number} y Translation in axis Y.
 * @param {Number} z Translation in axis Z.
 */
Ptolemy.Matrix.fromTranslation = function(x, y, z) {
    return new Ptolemy.Matrix(
        1.0, 0.0, 0.0, x,
        0.0, 1.0, 0.0, y,
        0.0, 0.0, 1.0, z,
        0.0, 0.0, 0.0, 1.0);
};

/**
 * Creates a scale matrix.
 * @param {Number} x Scale in axis X.
 * @param {Number} y Scale in axis Y.
 * @param {Number} z scale in axis Z.
 */
Ptolemy.Matrix.fromScale = function(x, y, z) {
    return new Ptolemy.Matrix(
        x, 0.0, 0.0, 0.0,
        0.0, y, 0.0, 0.0,
        0.0, 0.0, z, 0.0,
        0.0, 0.0, 0.0, 1.0);
};

/**
 * Creates a rotation on axis X matrix.
 * @param {Angle} angle Indicates the rotation angle.
 */
Ptolemy.Matrix.fromRotationX = function(angle) {
    var cos = angle.cos();
    var sin = angle.sin();

    return new Ptolemy.Matrix(
        1.0, 0.0, 0.0, 0.0,
        0.0, cos, -sin, 0.0,
        0.0, sin, cos, 0.0,
        0.0, 0.0, 0.0, 1.0);
};

/**
 * Creates a rotation on axis Y matrix.
 * @param {Angle} angle Indicates the rotation angle.
 */
Ptolemy.Matrix.fromRotationY = function(angle) {
    var cos = angle.cos();
    var sin = angle.sin();

    return new Ptolemy.Matrix(
        cos, 0.0, sin, 0.0,
        0.0, 1.0, 0.0, 0.0,
        -sin, 0.0, cos, 0.0,
        0.0, 0.0, 0.0, 1.0);
};

/**
 * Creates a rotation on axis Z matrix.
 * @param {Angle} angle Indicates the rotation angle.
 */
Ptolemy.Matrix.fromRotationZ = function(angle) {
    var cos = angle.cos();
    var sin = angle.sin();

    return new Ptolemy.Matrix(
        cos, -sin, 0.0, 0.0,
        sin, cos, 0.0, 0.0,
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0);
};

/**
 * Returns a viewing matrix in model coordinates defined by the specified View eye point, reference point indicating
 * the center of the scene, and up vector. The eye point, center point, and up vector are in model coordinates. The
 * returned viewing matrix maps the reference center point to the negative Z axis, and the eye point to the origin,
 * and the up vector to the positive Y axis. When this matrix is used to define an OGL viewing transform along with
 * a typical projection matrix such as {@link #fromPerspective(Angle, double, double, double, double)} , this maps
 * the center of the scene to the center of the viewport, and maps the up vector to the viewoport's positive Y axis
 * (the up vector points up in the viewport). The eye point and reference center point must not be coincident, and
 * the up vector must not be parallel to the line of sight (the vector from the eye point to the reference center
 * point).
 *
 * @param eye    the eye point, in model coordinates.
 * @param center the scene's reference center point, in model coordinates.
 * @param up     the direction of the up vector, in model coordinates.
 *
 * @return a viewing matrix in model coordinates defined by the specified eye point, reference center point, and up
 *         vector.
 *
 * @throws IllegalArgumentException if any of the eye point, reference center point, or up vector are null, if the
 *                                  eye point and reference center point are coincident, or if the up vector and the
 *                                  line of sight are parallel.
 */
Ptolemy.Matrix.fromViewLookAt = function(eye, center, up) {

    // Compute forward vector and normalize it
    var forward = center.substract(eye);
    forward = forward.normalize();

    // Compute left vector
    var left = forward.cross(up);
    left = left.normalize();

    // Normalize up vector
    up = up.normalize();

    var mAxes = new Ptolemy.Matrix(
        left.x, left.y, left.z, 0.0,
        up.x, up.y, up.z, 0.0,
        -forward.x, -forward.y, -forward.z, 0.0,
        0.0, 0.0, 0.0, 1.0);

    var mEye = Ptolemy.Matrix.fromTranslation(-eye.x, -eye.y, -eye.z);

    return mAxes.multiply(mEye);
};


