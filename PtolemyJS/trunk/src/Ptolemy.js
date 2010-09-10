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
 * @fileoverview Ptolemy WebGL Virtual Globe 2010
 * This files creates the Ptolemy namespace and loads all required files. In addition
 * it provides some basic and common functions.
 *
 * @author Antonio Santiago asantiagop(at)gmail(dot)com
 */

/**
 * @function This function is executed when Ptolemy.js code is imported in 
 * your page and is responsible to define the Ptolemy namespace with some common
 * methods.
 */
(function() {
    var loadedScripts = {};

    /**
     * @namespace Ptolemy namespace contains all classes, methods and properties
     * related to Ptolemy.
     */
    Ptolemy = {
        scriptName: 'Ptolemy.js',
        version: '0.0.1',
        versionName: 'nano',

        // TODO - Not used, must be implemented in the right way
        /**
         * Computes the base path of 'Ptolemy.scriptName'.
	 *
	 * @return path
	 */
        getBasePath: function() {
            var scripts = document.getElementsByTagName("script");
            var scriptSrc = null;
            for (var i = 0; i < scripts.length; i++) {
                if (scripts[i].src && scripts[i].src.indexOf(Ptolemy.scriptName) != -1) {
                    scriptSrc = scripts[i].src;
                    break;
                }
            }
            if (scriptSrc == null) {
                alert("Can't get the base path for script name: '" + Ptolemy.scriptName + "'.");
                return;
            }
            var parts = scriptSrc.split("/");
            parts.pop();
            var basePath = parts.join("/");

            return basePath;
        },

        // TODO - Implement function in the right way and test in some borwsers.
        /**
	 * Private function responsible to include only once required JavaScript files. 
	 * The function computes the base location to the Ptolemy.js script and tries
	 * to load all scripts from that base path.
	 * 
	 * @param jsFile Path to the file. Must be relative to Ptolemy.js script location.
	 */        
        includeFile: function(jsFile) {
            if (loadedScripts[jsFile] != null) return;

            var basePath = Ptolemy.getBasePath();

            var head = document.getElementsByTagName("head")[0];
            var scriptElement = document.createElement('script');
            scriptElement.setAttribute('language', 'javascript');
            scriptElement.setAttribute('type', 'text/javascript');
            scriptElement.setAttribute('src', jsFile);
            head.appendChild(scriptElement);
            loadedScripts[jsFile] = jsFile;
        },

        shaderProgram: null,
        // TODO - Improve shaders manipulation.
        /**
	 * Initialize the shaders.
	 * @param gl GL context reference.
	 * @return A shader program with vertex and fragment shader attached.
	 */        
        initializeShaders: function(gl) {
            var fragmentShader = Ptolemy._getShader(gl, "shader-fs");
            var vertexShader = Ptolemy._getShader(gl, "shader-vs");

            this.shaderProgram = gl.createProgram();
            gl.attachShader(this.shaderProgram, vertexShader);
            gl.attachShader(this.shaderProgram, fragmentShader);
            gl.linkProgram(this.shaderProgram);

            if (!gl.getProgramParameter(this.shaderProgram, gl.LINK_STATUS)) {
                alert("Could not initialise shaders");
            }

            gl.useProgram(this.shaderProgram);

            this.shaderProgram.vertexPositionAttribute = gl.getAttribLocation(this.shaderProgram, "aVertexPosition");
            gl.enableVertexAttribArray(this.shaderProgram.vertexPositionAttribute);

            this.shaderProgram.vertexTexCoord1Attribute = gl.getAttribLocation(this.shaderProgram, "aTexCoord1");
            if(this.shaderProgram.vertexTexCoord1Attribute >= 0) {
            	gl.enableVertexAttribArray(this.shaderProgram.vertexTexCoord1Attribute);
            }

            this.shaderProgram.pMatrixUniform = gl.getUniformLocation(this.shaderProgram, "uPMatrix");
            this.shaderProgram.mvMatrixUniform = gl.getUniformLocation(this.shaderProgram, "uMVMatrix");
            
            this.shaderProgram.uUseTexture1 = gl.getUniformLocation(this.shaderProgram, "uUseTexture1");
        },

        /**
	 * Parse the specified shader and compile the program.
	 * @private
	 * @param gl
	 * @param id
	 */
        _getShader: function(gl, id) {
            var shaderScript = document.getElementById(id);
            if (!shaderScript) {
                return null;
            }

            var str = "";
            var k = shaderScript.firstChild;
            while (k) {
                if (k.nodeType == 3) {
                    str += k.textContent;
                }
                k = k.nextSibling;
            }

            var shader;
            if (shaderScript.type == "x-shader/x-fragment") {
                shader = gl.createShader(gl.FRAGMENT_SHADER);
            } else if (shaderScript.type == "x-shader/x-vertex") {
                shader = gl.createShader(gl.VERTEX_SHADER);
            } else {
                return null;
            }

            gl.shaderSource(shader, str);
            gl.compileShader(shader);

            if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
                alert(gl.getShaderInfoLog(shader));
                return null;
            }

            return shader;
        }
    };

// TODO - Finish 'includFile' function and import files automatically
// Include required scripts
/*Ptolemy.includeFile('../src/webgl-debug.js');
    Ptolemy.includeFile('../src/Globe.js');
    Ptolemy.includeFile('../src/Camera.js');
    Ptolemy.includeFile('../src/Input.js');
    Ptolemy.includeFile('../src/Angle.js');
    Ptolemy.includeFile('../src/Vector4.js');
    Ptolemy.includeFile('../src/Matrix.js');
    Ptolemy.includeFile('../src/Quaternion.js');*/

})();
