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
 * @fileoverview MatrixStack tries to emulate the matrix stack offered by OpenGL.
 * MatrixStack allows to push and pop matrices (implemented as Matrix objects).
 *
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * Creates a new MatrixStack instance.
 */
Ptolemy.MatrixStack = function() {
    this.matrixStackArray = [];
};

/**
 * Pushes the current matrix stack by one, duplicating the current matrix
 */
Ptolemy.MatrixStack.prototype.push = function() {
    var current = this.current();
    if (current) {
        this.matrixStackArray.push(current.clone());
    }
};

/**
 * Pops the current matrix stack, replacing the current matrix with the one below it on the stack
 * @returns the removed matrix
 */
Ptolemy.MatrixStack.prototype.pop = function() {
    if (this.matrixStackArray.length > 0) {
        return this.matrixStackArray.pop();
    } else {
        return null;
    }
};

/**
 * Clears the stack.
 */
Ptolemy.MatrixStack.prototype.clear = function() {
    this.matrixStackArray = [];
};

/**
 * Replace the current matrix with the specified matrix
 * @param {Matrix} matrix
 */
Ptolemy.MatrixStack.prototype.load = function(matrix) {
    this.matrixStackArray.pop();
    this.matrixStackArray.push(matrix);
};

/**
 * Gets the matrix at top of the stack or null if it is empty.
 */
Ptolemy.MatrixStack.prototype.current = function() {
    var length = this.matrixStackArray.length;
    if (length > 0) {
        return this.matrixStackArray[length - 1];
    }
    return null;
};

/**
 * Multiply the current matrix with the specified matrix
 * @param {Matrix} matrix
 */
Ptolemy.MatrixStack.prototype.mult = function(matrix) {
    var current = this.current();
    if (current) {
        var newCurrent = current.multiply(matrix);
        this.load(newCurrent);
    } else {
        this.load(matrix);
    }
};


