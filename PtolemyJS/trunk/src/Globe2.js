/**
 * Ptolemy 3D - Open Source Virtual Globe framework.
 * Please visit ptolemy3d.org for more information on the project.
 * Copyright (C) 2010 
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
 * @author Jerome JOUVIE <jerome.jouvie@gmail.com>
 */

function log(msg) {
	document.getElementById("LOG").innerHTML += (msg + "\n");
}

function ddToRad(dd) {
	return ((dd / 1000000.) * (Math.PI / 180.0));
}

function degToDd(dd) {
	return dd * 1000000;
}

/** Render dara */

Ptolemy.RenderData = function(gl) {
	var NUM_POLY = Ptolemy.Globe.NUM_POLY();
	
	var numVertices = (NUM_POLY + 1) * (NUM_POLY + 1);
	var numIndices = NUM_POLY * NUM_POLY * 6;
	
	this.positions = new Float32Array(numVertices * 3);
	this.texcoords1 = new Float32Array(numVertices * 2);
	this.indices = new Uint16Array(numIndices);
	
	var iid = 0;
	for (var j = 0; j < NUM_POLY; j++) {
		for (var i = 0; i < NUM_POLY; i++) {
			var index = (j * (NUM_POLY + 1)) + i;

			var i0 = index;
			var i1 = index + 1;
			var i2 = index + (NUM_POLY + 1);
			var i3 = index + (NUM_POLY + 1) + 1;

			this.indices[iid++] = i0;
			this.indices[iid++] = i1;
			this.indices[iid++] = i2;

			this.indices[iid++] = i2;
			this.indices[iid++] = i1;
			this.indices[iid++] = i3;
		}
	}
	
	this.positionsBuffer = gl.createBuffer();
	this.texcoords1Buffer = gl.createBuffer();
	
	this.indicesBuffer = gl.createBuffer();
	this.indicesBufferNum = numIndices;
	gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.indicesBuffer);
	gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, this.indices, gl.STATIC_DRAW);
	gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, null);
}
Ptolemy.RenderData.prototype = {
	draw: function(gl, texture) {
		if(texture != null) {
			texture.bind(0);
		}
		
		if (Ptolemy.shaderProgram.uUseTexture1 != null) {
			gl.uniform1i(Ptolemy.shaderProgram.uUseTexture1, (texture != null) ? 1 : 0);
		}
		
		gl.bindBuffer(gl.ARRAY_BUFFER, this.positionsBuffer);
		gl.bufferData(gl.ARRAY_BUFFER, this.positions, gl.STATIC_DRAW);
		gl.vertexAttribPointer(Ptolemy.shaderProgram.vertexPositionAttribute, 3, gl.FLOAT, false, 0, 0);
		
		if(Ptolemy.shaderProgram.vertexTexCoord1Attribute >= 0) {
			gl.bindBuffer(gl.ARRAY_BUFFER, this.texcoords1Buffer);
			gl.bufferData(gl.ARRAY_BUFFER, this.texcoords1, gl.STATIC_DRAW);
			gl.vertexAttribPointer(Ptolemy.shaderProgram.vertexTexCoord1Attribute, 2, gl.FLOAT, false, 0, 0);
		}
		
		gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.indicesBuffer);
		gl.drawElements(gl.TRIANGLES, this.indicesBufferNum, gl.UNSIGNED_SHORT, 0);
		
		if(texture != null) {
			texture.unbind(0);
		}
	}
}

/** Tile */

Ptolemy.Tile = function(level) {
	this.tileWidth = level.tileWidth;
	this.tileHeight = level.tileHeight;
	this.tileLon = 0;
	this.tileLat = 0;
	this.visible = false;
	this.tileMap = null;
}
Ptolemy.Tile.prototype = {
	update: function(globe, level, lon, lat, visible) {
		this.tileLon = lon;
		this.tileLat = lat;
		this.visible = visible;
		if(this.visible) {
			this.tileMap = globe.getMap(level, this);
		}
		else {
			this.tileMap = null;
		}
	},
	
	render: function(gl, globe) {
		var renderData = globe.renderData;
		if(!this.visible) {
			return;
		}
		
		// Vertex data
		var vertices = renderData.positions;
		var texCoords1 = renderData.texcoords1;
		var vid = 0;
		var uvid = 0;
		
		var NUM_POLY = Ptolemy.Globe.NUM_POLY();
		var RADIUS = Ptolemy.Globe.RADIUS();
		
		var startLon = this.tileLon;
		var startLat = this.tileLat;
		
		var dLon = this.tileWidth  / NUM_POLY;
		var dLat = this.tileHeight / NUM_POLY;
		
		var lat = startLat;
		for (var j = 0; j <= NUM_POLY; j++) {
			var cosY = Math.cos(ddToRad(lat)) * RADIUS;
			var sinY = Math.sin(ddToRad(lat)) * RADIUS;
			
			var lon = startLon;
			for (var i = 0; i <= NUM_POLY; i++) {
				var cosX = Math.cos(ddToRad(lon));
				var sinX = Math.sin(ddToRad(lon));

				var x,y,z;
				x = cosY * sinX;
				y = sinY;
				z = cosY * cosX;
				
				var u,v;
				u = i / NUM_POLY;
				v = j / NUM_POLY;

				vertices[vid++] = x;
				vertices[vid++] = y;
				vertices[vid++] = z;

				texCoords1[uvid++] = u;
				texCoords1[uvid++] = 1 - v; //TODO Why the inversion ?

				lon += dLon;
			}
			lat += dLat;
		}
		
		// Texture
		var tex = null;
		if (this.tileMap != null) {
			tex = this.tileMap.texture
		}
		
		renderData.draw(gl, tex);
	}
}

/** Level */

Ptolemy.Level = function(id_, tileWidth_, tileHeight_) {
	this.NUM_TILE_LON = Math.min(8, degToDd(360) / tileWidth_);
	this.NUM_TILE_LAT = Math.min(8, degToDd(180) / tileHeight_);

	this.id = id_;
	this.tileWidth  = tileWidth_;  // Longitude
	this.tileHeight = tileHeight_; // Latitude
	
	this.tiles = new Array(this.NUM_TILE_LON * this.NUM_TILE_LAT);
	for(var i = 0; i < this.tiles.length; i++) {
		this.tiles[i] = new Ptolemy.Tile(this);
	}
	this.centerLon = 0;
	this.centerLat = 0;
}
Ptolemy.Level.prototype = {
	update: function(gl) {
		var numLon = this.NUM_TILE_LON / 2;
		if(numLon < 1) {
			numLon = 1;
		}
		var numLat = this.NUM_TILE_LAT / 2;
		if(numLat < 1) {
			numLat = 1;
		}
		
		var t = 0;
		var lat = this.centerLat - numLat * this.tileHeight;
		for(var j = -numLat; j < numLat; j++, lat += this.tileHeight) {
			var lon = this.centerLon - numLon * this.tileWidth;
			for(var i = -numLon; i < numLon; i++, lon += this.tileWidth) {
				this.tiles[t].update(globe, this, lon, lat, true);
				t++;
			}
		}
	},
	
	render: function(gl, globe) {
		var numLon = this.NUM_TILE_LON / 2;
		if(numLon < 1) {
			numLon = 1;
		}
		var numLat = this.NUM_TILE_LAT / 2;
		if(numLat < 1) {
			numLat = 1;
		}
		
		var t = 0;
		for(var j = -numLat; j < numLat; j++) {
			for(var i = -numLon; i < numLon; i++) {
				this.tiles[t].render(gl, globe);
				t++;
			}
			
		}
	}
}


/**
 * TMS
 */

Ptolemy.TMS = function() {
    this.levels = [];
}
Ptolemy.TMS.prototype = {
	update: function(gl) {
		for(var i = 0; i < this.levels.length; i++) {
			this.levels[i].update(gl, this);
		}
	},
	
	render : function(gl, globe) {
		for(var i = 0; i < this.levels.length; i++) {
			this.levels[i].render(gl, globe);
		}
	}
}

/**
 * Creates a new Globe instance.
 * @constructor
 */
Ptolemy.Globe = function() {
	this.tmsServer = ".";
    this.tmsPyramids = [];
	this.mapQueue = null;
	this.renderData = null;
	this.worldAxis = new Ptolemy.WorldAxis();
	// FPS counter
	this.fps = 0;
	this.fpsDT = 0;
	this.fpsCount = 0;
	this.fpsLastT = -1;
};

Ptolemy.Globe.prototype = {
	setTmsServer : function(server) {
		this.tmsServer = server;
	},
	
	// TODO Hardcoded init
	addLevel : function(level) {
		var tms = new Ptolemy.TMS();
		tms.levels.push(level);
		this.tmsPyramids.push(tms);
	},
	
	getMap : function(level, tile) {
		var id = level.id;
		var x = tile.tileLon + degToDd(180);
		var y = tile.tileLat + degToDd(90);
		
		x = x / level.tileWidth;
		y = y / level.tileHeight;
		
		var url = id+"/"+x+"/"+y+".png";
		var entry = this.mapQueue.getEntry(url);
		return entry.data;
	},
	
	update : function(gl) {
		if(this.mapQueue == null) {
			this.mapQueue = new Ptolemy.AsyncTextureQueue(gl, this.tmsServer);
		}
		this.mapQueue.update();
		
	    for (var i = 0; i < this.tmsPyramids.length; i++) {
			this.tmsPyramids[i].update(gl);
	    }
	    
	    this.updateFPS();
	},
	
	updateFPS : function(gl) {
    	var now = new Date().getTime();
    	if(this.fpsLastT < 0) {
    		this.fpsLastT = now;
    	}
	    
	    this.fpsDT += (now - this.fpsLastT);
	    this.fpsCount++;
	    this.fpsLastT = now;
	    
	    if(this.fpsDT > 3000) {
	    	this.fps = 1000 * this.fpsCount / this.fpsDT;
	    	this.fpsDT = 0;
	    	this.fpsCount = 0;
	    	log("FPS: " + this.fps);
	    }
	},
	
	render : function(camera, dc) {
		var gl = dc.gl;
		this.update(gl);
		
        camera.setMatrixUniform();
        
		if(this.renderData == null) {
			this.renderData = new Ptolemy.RenderData(gl);
		}
		
		this.worldAxis.render(gl);
	    for (var i = 0; i < this.tmsPyramids.length; i++) {
			this.tmsPyramids[i].render(gl, this);
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

Ptolemy.Globe.NUM_POLY = function() {
    return 16;
};

/*
 *
 */

Ptolemy.WorldAxis = function() {
	this.verticesBuffer = null;
	this.indicesBuffer = null;
};

Ptolemy.WorldAxis.prototype = {
	init : function(gl) {
		var siz = 2 * Ptolemy.Globe.RADIUS();
	
		var positions = new Float32Array([
			 0.0,  0.0,  0.0,
			 siz,  0.0,  0.0,
			 0.0,  0.0,  0.0,
			 0.0,  siz,  0.0,
			 0.0,  0.0,  0.0,
			 0.0,  0.0,  siz
		]);

		var indices = new Uint16Array([
			0, 1,
			2, 3,
			4, 5
		]);
		
		this.verticesBuffer = gl.createBuffer();
	    gl.bindBuffer(gl.ARRAY_BUFFER, this.verticesBuffer);
	    gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STATIC_DRAW);
	    gl.bindBuffer(gl.ARRAY_BUFFER, null);
	
	    this.indicesBuffer = gl.createBuffer();
	    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.indicesBuffer);
	    gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, indices, gl.STATIC_DRAW);
	    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, null);
	},
	
	render : function(gl) {
		if (this.indicesBuffer == null) {
			this.init(gl);
		}
	
		if (Ptolemy.shaderProgram.uUseTexture1 != null) {
			gl.uniform1i(Ptolemy.shaderProgram.uUseTexture1, 0);
		}
		
	    gl.bindBuffer(gl.ARRAY_BUFFER, this.verticesBuffer);
	    gl.vertexAttribPointer(Ptolemy.shaderProgram.vertexPositionAttribute, 3, gl.FLOAT, false, 0, 0);
	
		if (Ptolemy.shaderProgram.vertexTexCoord1Attribute >= 0) {
		    gl.vertexAttribPointer(Ptolemy.shaderProgram.vertexTexCoord1Attribute, 2, gl.FLOAT, false, 0, 0);
		}
	
	    gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, this.indicesBuffer);
	    gl.drawElements(gl.LINES, 6, gl.UNSIGNED_SHORT, 0);
	}
}

// ========================================
// From SpiderGL example

Ptolemy.AsyncTextureNode = function(relUrl_) {
	this.relUrl = relUrl_;
	this.id = 0;
	this.timestamp = -1;
	this.req = null;
	this.data = null;
}

Ptolemy.AsyncTextureNodeComparator = function(a, b) {
	// higher timestamp first
	if (a.timestamp != b.timestamp) {
		return (b.timestamp - a.timestamp);
	}
	// lower id first
	return (a.id - b.id);
}

Ptolemy.AsyncTextureQueue = function(gl, url, onLoad) {
	this.gl = gl;
	this._nodes = { };
	this._nodesCount = 0;
	this._url = url;
	this._timestamp = 0;
	this._maxError = 1.0;
	this._cache = [ ];
	this._maxCacheSize = 1024;
	this._maxOngoingRequests = 2;
	this._currentOngoingRequests = 0;
	this._toRequest = [ ];
	this._toRenderFullRes = [ ];
	this._toRenderHalfRes = [ ];
	this._readyItems = [ ];
}

Ptolemy.AsyncTextureQueue.prototype = {
	_createData : function(image) {
		var texOpts = {
			minFilter : this.gl.LINEAR_MIPMAP_LINEAR,
			magFilter : this.gl.LINEAR,
			generateMipmap : true
		};

		var data = {
			texture : new SglTexture2D(this.gl, image, texOpts)
		};
		return data;
	},

	_destroyData : function(data) {
		data.texture.destroy();
		data.texture = null;
	},

	_requestNode : function(n) {
		if (n.req) return;

		var that = this;
		var url = this._url + "/" + n.relUrl;
		var image = new SglImageRequest(url);

		var watcher = new SglRequestWatcher([image], function(w) {
			that._currentOngoingRequests--;
			that._readyItems.push(n);
		});
		n.req = watcher;

		this._currentOngoingRequests++;
		image.send();
	},

	_collectNodes : function() {
		this._toRequest = [ ];

		for(var key in this._nodes) {
			var n = this._nodes[key];
		
			var needed = true;
			var usable = ((n.data) ? (true) : (false))
			
			n.timestamp = this._timestamp;

			// if node data is not available, request node and return
			if (!usable) {
				if (!n.req) {
					this._toRequest.push(n);
				}
			}
			else {
				// the node is needed and available
			}
		}
	},

	_updateCache : function() {
		var combined = this._cache.concat(this._readyItems);
		this._readyItems = [ ];
		combined.sort(Ptolemy.AsyncTextureNodeComparator);

		var cl = combined.length;
		var k  = (this._maxCacheSize < cl) ? (this._maxCacheSize) : (cl);

		this._cache = combined.splice(0, k);
		var outOfCache = combined;

		var n = null;
		var r = null;
		var failed = [ ];

		for (var i=0; i<k; ++i) {
			n = this._cache[i];
			r = n.req;
			n.req = null;

			if (!r) continue;
			if (!r.succeeded) {
				failed.push(i);
				continue;
			}

			n.data = this._createData(r.requests[0].image);
			log("Tile map: " + n.relUrl);
		}

		var fc = failed.length;
		var reinsertedCount = 0;

		k = outOfCache.length;
		for (var i=0; i<k; ++i) {
			n = outOfCache[i];
			r = n.req;
			n.req = null;

			if (r) continue;

			if (reinsertedCount < fc) {
				this._cache[failed[reinsertedCount]] = n;
				reinsertedCount++;
			}
			else {
				this._destroyData(n.data);
				n.data = null;
			}
		}

		if (reinsertedCount > 0) {
			this._cache.sort(AsyncTextureNodeComparator);
		}
	},

	_requestNodes : function() {
		var cs = this._cache.length;
		var lastItem = (cs > 0) ? (this._cache[cs-1]) : (null);

		this._toRequest.sort(Ptolemy.AsyncTextureNodeComparator);
		var requestableCount = this._maxOngoingRequests - this._currentOngoingRequests;

		var requested = 0;
		var k = this._toRequest.length;
		var n = null;
		var canRequest = false;

		for (var i=0; ((i<k) && (requested<requestableCount)); ++i) {
			n = this._toRequest[i];
			if (n.req) continue; // ongoing;

			canRequest = true;
			if ((lastItem != null) && (cs >= this._maxCacheSize)) {
				canRequest = (AsyncTextureNodeComparator(n, lastItem) < 0);
			}
			if (canRequest) {
				this._requestNode(n);
				requested++
			}
		}
	},
	
	getEntry : function(url) {
		var n = this._nodes[url];
		if(n != null) {
			return n;
		}
		
		n = new Ptolemy.AsyncTextureNode(url);
		this._nodes[url] = n;
		this._nodesCount++;
		return n;
	},

	update : function() {
		this._timestamp++;
		this._collectNodes();
		this._updateCache();
		this._requestNodes();
	}
};

/*************************************************************************/
/*                                                                       */
/*  SpiderGL                                                             */
/*  JavaScript 3D Graphics Library on top of WebGL                       */
/*                                                                       */
/*  Copyright (C) 2010                                                   */
/*  Marco Di Benedetto                                                   */
/*  Visual Computing Laboratory                                          */
/*  ISTI - Italian National Research Council (CNR)                       */
/*  http://vcg.isti.cnr.it                                               */
/*  mailto: marco[DOT]dibenedetto[AT]isti[DOT]cnr[DOT]it                 */
/*                                                                       */
/*  This file is part of SpiderGL.                                       */
/*                                                                       */
/*  SpiderGL is free software; you can redistribute it and/or modify     */
/*  under the terms of the GNU Lesser General Public License as          */
/*  published by the Free Software Foundation; either version 2.1 of     */
/*  the License, or (at your option) any later version.                  */
/*                                                                       */
/*  SpiderGL is distributed in the hope that it will be useful, but      */
/*  WITHOUT ANY WARRANTY; without even the implied warranty of           */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                 */
/*  See the GNU Lesser General Public License                            */
/*  (http://www.fsf.org/licensing/licenses/lgpl.html) for more details.  */
/*                                                                       */
/*************************************************************************/

function sglTexture2DFromImage(gl, image, options) {
	var opt = sglGetTextureOptions(gl, options);

	var tex = gl.createTexture();
	gl.bindTexture(gl.TEXTURE_2D, tex);
	//gl.texImage2D(gl.TEXTURE_2D, 0, image, opt.flipY, opt.premultiplyAlpha);
	gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, image);
	gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, opt.minFilter);
	gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, opt.magFilter);
	gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S,     opt.wrapS    );
	gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T,     opt.wrapT    );
	if (opt.generateMipmap) {
		gl.generateMipmap(gl.TEXTURE_2D);
	}
	gl.bindTexture(gl.TEXTURE_2D, null);

	var obj = new SglTextureInfo();
	obj.handle           = tex;
	obj.valid            = true;
	obj.target           = gl.TEXTURE_2D;
	obj.format           = gl.RGBA;
	obj.width            = image.width;
	obj.height           = image.height;
	obj.minFilter        = opt.minFilter;
	obj.magFilter        = opt.magFilter;
	obj.wrapS            = opt.wrapS;
	obj.wrapT            = opt.wrapT;
	obj.isDepth          = false;
	obj.depthMode        = 0;
	obj.depthCompareMode = 0;
	obj.depthCompareFunc = 0;

	return obj;
}
function _sglConsolidateOptions(fullOptions, usedOptions)
{
	if (usedOptions) {
		for (var attr in usedOptions) {
			fullOptions[attr] = usedOptions[attr];
		}
	}
	return fullOptions;
}

// SglTextureInfo
/***********************************************************************/
function sglGetTextureOptions(gl, options) {
	var opt = {
		minFilter        : gl.LINEAR,
		magFilter        : gl.LINEAR,
		wrapS            : gl.CLAMP_TO_EDGE,
		wrapT            : gl.CLAMP_TO_EDGE,
		isDepth          : false,
		depthMode        : gl.LUMINANCE,
		depthCompareMode : gl.COMPARE_R_TO_TEXTURE,
		depthCompareFunc : gl.LEQUAL,
		generateMipmap   : false,
		flipY            : true,
		premultiplyAlpha : false,
		onload           : null
	};
	return _sglConsolidateOptions(opt, options);
}

/**
 * Constructs a SglTextureInfo.
 * @class Holds information about a WebGL texture object.
 */
function SglTextureInfo() {
	this.handle           = null;
	this.valid            = false;
	this.target           = 0;
	this.format           = 0;
	this.width            = 0;
	this.height           = 0;
	this.minFilter        = 0;
	this.magFilter        = 0;
	this.wrapS            = 0;
	this.wrapT            = 0;
	this.isDepth          = false;
	this.depthMode        = 0;
	this.depthCompareMode = 0;
	this.depthCompareFunc = 0;
}


// SglTexture2D
/***********************************************************************/
// SglTexture2D(gl, info)
// SglTexture2D(gl, internalFormat, width, height, sourceFormat, sourceType, texels, options)
// SglTexture2D(gl, image, options)
// SglTexture2D(gl, url, options)
/**
 * Constructs a SglTexture2D.
 * @class Manages a WebGL 2D texture object.
 */
function SglTexture2D() {
	this.gl    = arguments[0];
	this._info = null;

	var gl = this.gl;

	var n = arguments.length;
	if ((n == 2) || (n == 3)) {
		if (arguments[1] instanceof Image) {
			this._info = sglTexture2DFromImage(gl, arguments[1], arguments[2]);
		}
		//else if (arguments[1] instanceof String) {
		else if (typeof(arguments[1]) == "string") {
			this._info = sglTexture2DFromUrl(gl, arguments[1], arguments[2]);
		}
		else {
			this._info = arguments[1];
		}
	}
	else {
		this._info = sglTexture2DFromData(gl, arguments[1], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6], arguments[7]);
	}
}

SglTexture2D.prototype = {
	get info() { return this._info; },

	destroy : function() {
		if (this._info.handle == null) return;
		this.gl.deleteTexture(this._info.handle);
		this._info = null;
	},

	get handle() {
		return this._info.handle;
	},

	get isValid() {
		return this._info.valid;
	},

	bind : function(unit) {
		this.gl.activeTexture(this.gl.TEXTURE0 + unit);
		this.gl.bindTexture(this._info.target, this._info.handle);
	},

	unbind : function(unit) {
		this.gl.activeTexture(this.gl.TEXTURE0 + unit);
		this.gl.bindTexture(this._info.target, null);
	},

	generateMipmap : function() {
		this.gl.generateMipmap(this._info.target);
	}
};
/***********************************************************************/

/*********************************************************/

function sglDefineGetter(ClassName, getterName, getterFunc) {
	ClassName.prototype.__defineGetter__(getterName, getterFunc);
}

function sglInherit(DerivedClassName, BaseClassName) {
	DerivedClassName.prototype = new BaseClassName();
	DerivedClassName.prototype.constructor = DerivedClassName;
}

// SglRequest
/*********************************************************/
const SGL_REQUEST_FAILED     = 0;
const SGL_REQUEST_SUCCEEDED  = 1;
const SGL_REQUEST_IDLE       = 2;
const SGL_REQUEST_ONGOING    = 3;
const SGL_REQUEST_ABORTED    = 4;
const SGL_REQUEST_QUEUED     = 5;
const SGL_REQUEST_REJECTED   = 6;
const SGL_REQUEST_CANCELLED  = 7;

/**
 * Constructs a SglRequest.
 * @class Base class for an asynchronous request.
 */
function SglRequest(onReadyCallback) {
	this._priority = null;
	this._status   = SGL_REQUEST_IDLE;
	this._queue    = null;
	this._onReady  = [ ];
	if (onReadyCallback) {
		this.addOnReadyCallback(onReadyCallback);
	}
}

SglRequest.prototype = {
	_createOnReadyCallback : function() {
		var t  = this;
		var cb = function(ok) {
			t._status = (ok) ? (SGL_REQUEST_SUCCEEDED) : (SGL_REQUEST_FAILED);
			if (t._queue) {
				t._queue._requestReady(t);
				this._queue  = null;
			}
			var n = t._onReady.length;
			for (var i=0; i<n; ++i) {
				t._onReady[i](t);
			}
		};
		return cb;
	},

	get status() {
		return this._status;
	},

	get priority() {
		return this._priority;
	},

	set priority(p) {
		this._priority = p;
		if (this._queue) {
			this._queue._updateRequest(this);
		}
	},

	get succeeded() {
		return (this._status == SGL_REQUEST_SUCCEEDED);
	},

	get failed() {
		return (this._status == SGL_REQUEST_FAILED);
	},

	addOnReadyCallback : function(f) {
		if (f) {
			this._onReady.push(f);
			if (this.isReady) {
				f(this);
			}
		}
	},

	removeOnReadyCallback : function(f) {
		var index = -1;
		var n = this._onReady.length;
		for (var i=0; i<n; ++i) {
			if (this._onReady[i] == f) {
				index = i;
				break;
			}
		}
		if (index < 0) return;
		this._onReady.splice(index, 1);
	},

	get isReady() {
		return (this.failed || this.succeeded);
	},

	get queue() {
		return this._queue;
	},

	send : function() {
		if (!this._send) return false;
		var r = this._send();
		if (r) {
			this._status = SGL_REQUEST_ONGOING;
		}
		else {
			this._status = SGL_REQUEST_FAILED;
			var cb = this._createOnReadyCallback();
			cb(false);
		}
		return r;
	},

	cancel : function() {
		if (this._status == SGL_REQUEST_QUEUED) {
			if (this._queue) {
				this._queue._removeQueuedRequest(this);
				this._queue  = null;
			}
			this._status = SGL_REQUEST_CANCELLED;
		}
	},

	abort : function() {
		this.cancel();
		if (this._status == SGL_REQUEST_ONGOING) {
			if (this._queue) {
				this._queue._removeOngoingRequest(this);
				this._queue  = null;
			}
			if (this._abort) {
				this._abort();
			}
			this._status = SGL_REQUEST_ABORTED;
		}
	}
};
/*********************************************************/

// SglImageRequest
/*********************************************************/
/**
 * Constructs a SglImageRequest.
 * @class Asynchronous image request.
 */
function SglImageRequest(url, onReadyCallback) {
	SglRequest.apply(this, Array.prototype.slice.call(arguments, 1));
	this._url = url;
	this._img = null;
}

sglInherit(SglImageRequest, SglRequest);

sglDefineGetter(SglImageRequest, "url", function() {
	return this._url;
});

sglDefineGetter(SglImageRequest, "image", function() {
	return this._img;
});

SglImageRequest.prototype._send = function() {
	if (!this._url) return false;

	var img = new Image();
	this._img = img;

	var cb = this._createOnReadyCallback();
	img.onload = function() {
		cb(img.complete);
	};
	img.onerror = function() {
		cb(false);
	};
	img.src = this._url;

	return true;
};

SglImageRequest.prototype._abort = function() {
	;
};
/*********************************************************/


// SglRequestWatcher
/*********************************************************/
/**
 * Constructs a SglRequestWatcher.
 * @class Watches for requests completion.
 */
function SglRequestWatcher(reqs, onReadyCallback) {
	var n = reqs.length;

	this._onReady       = (onReadyCallback) ? (onReadyCallback) : (null);
	this._waitingReqs   = n;
	this._succeededReqs = 0;
	this._failedReqs    = 0;
	this._reqs          = reqs.slice();

	var t  = this;
	var cb = function(r) {
		if (r.succeeded) {
			t._succeededReqs++;
		}
		else if (r.failed) {
			t._failedReqs++;
		}
		t._waitingReqs--;
		if (t.isReady) {
			if (t._onReady) {
				t._onReady(t);
			}
		}
	};

	for (var i=0; i<n; ++i) {
		if (reqs[i].succeeded) {
			this._succeededReqs++;
			this._waitingReqs--;
		}
		else if (reqs[i].failed) {
			this._failedReqs++;
			this._waitingReqs--;
		}
		else {
			reqs[i].addOnReadyCallback(cb);
		}
	}

	if (this._waitingReqs <= 0) {
		if (this._onReady) {
			this._onReady(this);
		}
	}
}

SglRequestWatcher.prototype = {
	get requests() {
		return this._reqs;
	},

	get succeeded() {
		return (this._succeededReqs == this._reqs.length);
	},

	get failed() {
		if ((this._succeededReqs + this._failedReqs) < this._reqs.length) return false;
		return (this._succeededReqs < this._reqs.length);
	},

	get onReady() {
		return this._onReady;
	},

	set onReady(f) {
		this._onReady = f;
		if (this.isReady) {
			if (this._onReady) {
				this._onReady(t);
			}
		}
	},

	get isReady() {
		return ((this._succeededReqs + this._failedReqs) == this._reqs.length);
	},

	send : function() {
		var n = this._reqs.length;
		for (var i=0; i<n; ++i) {
			this._reqs[i].send();
		}
	},

	cancel : function() {
		var n = this._reqs.length;
		for (var i=0; i<n; ++i) {
			this._reqs[i].cancel();
		}
	},

	abort : function() {
		var n = this._reqs.length;
		for (var i=0; i<n; ++i) {
			this._reqs[i].abort();
		}
	}
};
/*********************************************************/


// SglRequestQueue
/*********************************************************/
/**
 * Constructs a SglRequestQueue.
 * @class Asynchronous requests scheduler.
 */
function SglRequestQueue(maxOngoing, maxQueued, onReadyCallback) {
	this._maxOngoing = (maxOngoing > 0) ? (maxOngoing) : (0);
	this._maxQueued  = (maxQueued  > 0) ? (maxQueued ) : (0);

	this._ongoing = [ ];
	this._queued  = [ ];
	this._onReady = null;
	this._minPriorityReq = null;
	this._priorityCompare = null;
	this._dirty = true;
}

SglRequestQueue.prototype = {
	_sort : function() {
		if (!this._dirty) return;

		this._dirty = false;
		this._minPriorityReq = null;
		if (this._queued.length <= 0) return;

		if (this._priorityCompare) {
			this._queued.sort(this._priorityCompare);
		}
		else {
			this._queued.sort();
		}
		if (this._queued.length > 0) {
			this._minPriorityReq = this._queued[0];
		}
	},

	_updateMinPriority : function() {
		this._minPriorityReq = null;
		var n = this._queued.length;
		if (n <= 0) return;

		this._minPriorityReq = this._queued[0];
		if (!this._dirty) return;

		for (var i=1; i<n; ++i) {
			if (this.comparePriorities(this._queued[i].priority, this._minPriorityReq.priority) <= 0) {
				this._minPriorityReq = this._queued[i];
			}
		}
	},

	_removeFromArray : function(arr, elem) {
		var index = -1;
		var n = arr.length;
		for (var i=0; i<n; ++i) {
			if (arr[i] == elem) {
				index = i;
				break;
			}
		}
		if (index < 0) return;
		arr.splice(index, 1);
	},

	_removeQueuedRequest : function(r) {
		this._removeFromArray(this._queued, r);
		if (this._queued.length <= 0) {
			this._minPriorityReq = null;
		}
		else if (r == this._minPriorityReq) {
			this._updateMinPriority();
		}
	},

	_removeOngoingRequest : function(r) {
		this._removeFromArray(this._ongoing, r);
		this._execute();
	},

	_updateRequest : function(r) {
		if (this.comparePriorities(r.priority, this._minPriorityReq.priority) <= 0) {
			this._minPriorityReq = r;
		}
		this._dirty = true;
	},

	_requestReady : function(r) {
		this._removeOngoingRequest(r);
		if (this._onReady) {
			this._onReady(r);
		}
		this._execute();
	},

	_execute : function() {
		var n = this._queued.length;
		if (n <= 0) return;
		var sendable = (this._maxOngoing > 0) ? (this._maxOngoing - this._ongoing.length) : (n)
		if (sendable <= 0) return;
		if (sendable > n) sendable = n;
		this._sort();
		var r = null;
		for (var i=0; i<sendable; ++i) {
			r = this._queued.pop();
			r.send();
		}
		if (this._queued.length > 0) {
			this._minPriorityReq = this._queued[0];
		}
	},

	comparePriorities : function(a, b) {
		if (this._priorityCompare) {
			return this._priorityCompare(a, b);
		}
		return (a - b);
	},

	get priorityCompare() {
		return this._priorityCompare;
	},

	set priorityCompare(f) {
		this._priorityCompare = f;
		this._updateMinPriority();
	},

	get onReady() {
		return this._onReady;
	},

	set onReady(f) {
		this._onReady = f;
	},

	push : function(r) {
		var ret = {
			victims : null,
			result  : false
		};
		
		if (r.queue) return ret;
		var n = this._queued.length;
		var overflow = ((this._maxQueued > 0) && (n >= this._maxQueued));

		if  (overflow) {
			if (this.comparePriorities(r.priority, this._minPriorityReq.priority) <= 0) {
				r._status = SGL_REQUEST_REJECTED;
				return ret;
			}
		}

		this._queued.push(r);
		r._queue  = this;
		r._status = SGL_REQUEST_QUEUED;
		n++;

		ret.result = true;
	
		var toKill = (this._maxQueued > 0) ? (n - this._maxQueued) : (0);
		if (toKill <= 0) {
			if (this._minPriorityReq != null) {
				if (this.comparePriorities(r.priority, this._minPriorityReq.priority) <= 0) {
					this._minPriorityReq = r;
				}
			}
			else {
				this._minPriorityReq = r;
			}
			this._dirty = true;
		}
		else {
			this._sort();
			ret.victims = this._queued.slice(0, toKill);
			this._queued.splice(0, toKill);
			for (var i=0; i<toKill; ++i) {
				ret.victims[i]._status = SGL_REQUEST_REJECTED;
				ret.victims[i]._queue  = null;
			}
		}

		this._execute();

		return ret;
	}
};
/*********************************************************/
