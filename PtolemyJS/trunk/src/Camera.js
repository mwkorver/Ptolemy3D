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
 * @fileoverview Camera is responsible to render a Globe with its objects. 
 * The same Globe can be rendered by different Cameras.
 *
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * @class Camera instance associated with the specified HTML canvas and
 * that renders the specified Globe instance.
 */

/**
 * Creates a new Camera instance associated with the specified HTML canvas and
 * that renders the specified Globe instance.
 * 
 * A camera has the attributs:
 * canvas: The canvas DOM element where globe is rendered.
 * globe: A reference to the Globe instance to be rendered.
 * gl: GL context reference.
 * prMatrixStack: Stack for projection matrices.
 * mvMatrixStack: Stack for modelview matrices.
 * drawContext: Constains a reference to some important objects and is passed to every
 *              renderable object so they can have acces it them (for example the GL context).
 * 
 * @constructor
 * @param {DOMElement} canvas The DOM element.
 * @param {Ptolemy.Globe} globe A Globe reference to be rendered by the Camera.
 */
Ptolemy.Camera = function(canvas, globe) {
    this.canvas = canvas;
    this.globe = globe;
    try {
        this.gl = WebGLDebugUtils.makeDebugContext(this.canvas.getContext("experimental-webgl"), this.throwOnGLError);
        this.gl.viewportWidth = this.canvas.width;
        this.gl.viewportHeight = this.canvas.height;

        // Create projection and modelview stack matrices.
        this.mvMatrixStack = new Ptolemy.MatrixStack();
        this.prMatrixStack = new Ptolemy.MatrixStack();

        // TODO - Improve shaders implementation
        // Initialize shaders
        Ptolemy.initializeShaders(this.gl);

        // Colors for clean up
        this.gl.clearColor(0.0, 0.0, 0.0, 1.0);
        this.gl.clearDepth(1.0);

        // Create the DrawContext instance
        this.drawContext = new Ptolemy.DrawContext();
        this.drawContext.gl = this.gl;
        this.drawContext.camera = this;

        // Initialize other class attributes
        // TODO - Better implemented in a sublcass like OrbitCamera.
        this.yaw = 0;
        this.pitch = 0;
        this.roll = 0;

        this.projectionMatrix = null;
        this.translationMatrix = null;
        this.distance = 0;
        this.rotationQuaternion = null;
        this.rotationMatrix = null;

        // Set an initial orientation.
        this.setProjection();
        this.setDistance(Ptolemy.Globe.RADIUS() * 10);
        this.setRollPitchYaw(
        Ptolemy.Angle.fromDegrees(0),
        Ptolemy.Angle.fromDegrees(0),
        Ptolemy.Angle.fromDegrees(0)
        );

    } catch(e) {
        }
    if (!this.gl) {
        alert("Could not initialise WebGL, sorry :-(");
    }
};

Ptolemy.Camera.prototype.throwOnGLError = function(err, funcName, args) {
    throw "Ptolemy ERROR: " + WebGLDebugUtils.glEnumToString(err)
    + " was caused by call to '" + funcName
    + "' with arguments '" + args + "'.";
};

/**
 * Makes the current top matrices in modelview and perspective stack the current matrices.
 * @private
 */
Ptolemy.Camera.prototype.setMatrixUniform = function() {
    this.gl.uniformMatrix4fv(Ptolemy.shaderProgram.pMatrixUniform, false, new WebGLFloatArray(this.prMatrixStack.current().toArray()));
    this.gl.uniformMatrix4fv(Ptolemy.shaderProgram.mvMatrixUniform, false, new WebGLFloatArray(this.mvMatrixStack.current().toArray()));
};

Ptolemy.Camera.prototype.setProjection = function() {
    this.projectionMatrix = Ptolemy.Matrix.fromPerspective(
    45,
    this.gl.viewportWidth / this.gl.viewportHeight,
    0.1,
    Ptolemy.Globe.RADIUS() / 2);
};

Ptolemy.Camera.prototype.setDistance = function(distance) {
    this.distance = distance;
    this.translationMatrix = Ptolemy.Matrix.fromTranslation(0, 0, -distance);
};

Ptolemy.Camera.prototype.getDistance = function() {
    return this.distance;
};

/**
 * Sets the roll, pitch and yaw of the camera given three Angles.
 * @param {Angle} roll
 * @param {Angle} pitch
 * @param {Angle} yaw
 */
Ptolemy.Camera.prototype.setRollPitchYaw = function(roll, pitch, yaw) {
    this.rotationQuaternion = Ptolemy.Quaternion.fromRollPitchYaw(roll, pitch, yaw);
    this.rotationMatrix = Ptolemy.Matrix.fromQuaternion(this.rotationQuaternion);
};

// TODO - Add attributes to control camera rotation, position etc and also
// to control perspective parameters.
Ptolemy.Camera.prototype.getRoll = function() {
    return this.rotationQuaternion.getRoll();
};
Ptolemy.Camera.prototype.getPitch = function() {
    return this.rotationQuaternion.getPitch();
};
Ptolemy.Camera.prototype.getYaw = function() {
    return this.rotationQuaternion.getYaw();
};

/**
 * Renders the Globe with all the RenderableObjects.
 */
Ptolemy.Camera.prototype.render = function() {

    this.gl.viewport(0, 0, this.gl.viewportWidth, this.gl.viewportHeight);
    this.gl.clear(this.gl.COLOR_BUFFER_BIT | this.gl.DEPTH_BUFFER_BIT);

    // Clear matrix stacks on every rendering loop.
    this.mvMatrixStack.clear();
    this.prMatrixStack.clear();

    this.prMatrixStack.load(this.projectionMatrix);
    this.mvMatrixStack.load(Ptolemy.Matrix.IDENTITY());
    this.mvMatrixStack.mult(this.translationMatrix);
    this.mvMatrixStack.mult(this.rotationMatrix);

    // Render globe and renderableObjects
    var objects = this.globe.renderableObjectsArray;
    for (var i = 0; i < objects.length; i++) {
        objects[i].render(this.drawContext);

        // Every renderable object can modify matrix stack so after every
        // object.render() call "put" current top matrix on the stack
        // in the GL context.
        this.setMatrixUniform();
    }
};

/**
 * DrawContext stores objects needed in the rendering loop by renderable objects.
 * @class
 */
Ptolemy.DrawContext = function() {};
Ptolemy.DrawContext.prototype.gl = null;
Ptolemy.DrawContext.prototype.camera = null;

