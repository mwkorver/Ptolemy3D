Ptolemy.SampleTile = function() {
	this.initialized = false;
	this.axisVertices = null;
	this.axisPositionsBuffer = null;
};

Ptolemy.SampleTile.prototype.initialize = function(dc) {
    var gl = dc.gl;
    this.axisVertices = [
    0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
    0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
    0.0, 0.0, 0.0, 0.0, 0.0, 1.0
    ];
    this.axisPositionsBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, this.axisPositionsBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new WebGLFloatArray(this.axisVertices), gl.STATIC_DRAW);
    this.axisPositionsBuffer.itemSize = 3;
    this.axisPositionsBuffer.numItems = 6;
    this.initialized = true;
};

Ptolemy.SampleTile.prototype.render = function(dc) {
    var gl = dc.gl;
    if (!this.initialized) {
        this.initialize(dc);
    }
    gl.bindBuffer(gl.ARRAY_BUFFER, this.axisPositionsBuffer);
    gl.vertexAttribPointer(Ptolemy.shaderProgram.vertexPositionAttribute, this.axisPositionsBuffer.itemSize, gl.FLOAT, false, 0, 0);

    //gl.lineWidth(4);
    gl.drawArrays(gl.LINES, 0, this.axisPositionsBuffer.numItems);
};

// TODO - Simply ignore this.
var oldPtolemy = function() {
    ////////////////////////////////////////////////////////////////////////////
    function createRandomElevationTile(xbase, ybase) {
        xbase = 0;
        ybase = 0;
        var yBands = 20;
        var xBands = 20;
        var pointsData = [];
        for (var y = 0; y <= yBands; y++) {
            for (var x = 0; x <= xBands; x++) {
                pointsData.push(x);
                pointsData.push(y);
                pointsData.push(Math.random() * 0.75);
            }
        }
        var indexData = [];
        for (var y = 0; y < yBands; y++) {
            for (var x = 0; x < xBands; x++) {
                var first = y * (xBands + 1) + x;
                var second = first + 1;
                var third = first + (xBands + 1);
                var fourth = third + 1;

                indexData.push(first);
                indexData.push(second);
                indexData.push(second);
                indexData.push(fourth);
                indexData.push(fourth);
                indexData.push(first);

                indexData.push(first);
                indexData.push(third);
                indexData.push(third);
                indexData.push(fourth);
                indexData.push(fourth);
                indexData.push(first);
            }
        }

        var tile = {};
        tile.pointsData = pointsData;
        tile.pointsDataItemsSize = 3;
        tile.pointsDataNumItems = pointsData.length / 3;
        tile.indexData = indexData;
        tile.indexDataItemsSize = 1;
        tile.indexDataNumItems = indexData.length;

        return tile;
    }
} ();




