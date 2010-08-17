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
 * @fileoverview Input handler is attached to a Camera and controls the 
 * Camera movement.
 *
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * Creates a new Input instances attached to the specified camera.
 * @constructor
 * @param camera
 */
Ptolemy.Input = function(camera) {
    this.camera = camera;
    this.mouseDown = false;
    this.lastMouseX = 0;
    this.lastMouseY = 0;
};

Ptolemy.Input.prototype.registerEvents = function(canvasElement) {
    canvasElement.onmousedown = this.handleMouseDown;
    canvasElement.onmouseup = this.handleMouseUp;
    canvasElement.onmousemove = this.handleMouseMove;

    canvasElement.onmousewheel = this.handleMouseWheel;
    document.addEventListener('DOMMouseScroll', this.handleMouseWheel, false);

    canvasElement.input = input;
};

Ptolemy.Input.prototype.handleMouseWheel = function(event) {
	var delta = 0;
    if (!event) event = window.event;

    // normalize the delta
    if (event.wheelDelta) {
        // IE and Opera
        delta = event.wheelDelta * 50000;
    } else if (event.detail) {
        // W3C
        delta = -event.detail * 100000;
    }
    input.camera.setDistance(input.camera.getDistance() + delta);
};

Ptolemy.Input.prototype.handleMouseDown = function(event) {
    var input = this.input;
    input.mouseDown = true;
    input.lastMouseX = event.clientX;
    input.lastMouseY = event.clientY;
};

Ptolemy.Input.prototype.handleMouseUp = function(event) {
    var input = this.input;
    input.mouseDown = false;
};

Ptolemy.Input.prototype.handleMouseMove = function(event) {
    var input = this.input;
    if (!input.mouseDown) {
        return;
    }
    var newX = event.clientX;
    var newY = event.clientY;

    var deltaX = newX - input.lastMouseX;
    var deltaY = newY - input.lastMouseY;

    input.camera.setRollPitchYaw(
    input.camera.getRoll().add(Ptolemy.Angle.fromDegrees(deltaY)),
    input.camera.getPitch().add(Ptolemy.Angle.fromDegrees(deltaX)),
    input.camera.getYaw());

    input.lastMouseX = newX
    input.lastMouseY = newY;
};


