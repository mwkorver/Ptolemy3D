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
 * @fileoverview Animator controls the scene render frame rate.
 *
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

// TODO - For the moment the animator simply works using JS setInterval functions.
//        Improve class to control key frame rate.

/**
 * Creates a new instance of Animator associated with a camera.
 * @constructor
 * @param {Camera} camera Camera with which animator is associated.
 */
Ptolemy.Animator = function(camera) {
    this.camera = camera;

	this.intervalId = null;

	this.data = new Date(); // Used to compute elapsed time.
	this.frameTime = 0;
	this.frameNumber = 0;
	this.framesPerSecond = 0;
	this.timeBase = 0;
	this.computeTime = 2000; // Recompute frame rate every 2 seconds
};

Ptolemy.Animator.prototype.startRendering = function() {
    this.intervalId = setInterval("this.camera.render();", 15);
};

Ptolemy.Animator.prototype.stopRendering = function() {
    clearInterval(this.intervalId);
};

// This method is not used but it is a base code to get some control
// over frame rate. Maybe we can use in the future when coding the animator
// taking into account the frame rate.
Ptolemy.Animator.prototype.render = function() {

    this.frameTime = this.data.getTime();

    this.camera.render();

    this.frameNumber++;
    var  time = this.data.getTime();
    this.frameTime = System.currentTimeMillis() - this.frameTime;
    if (time - this.timebase > this.computeTime){
        this.framesPerSecond = this.frameNumber * 1000.0 / (time - this.timebase);
        this.timebase = time;
        this.frameNumber = 0;
    }
};


