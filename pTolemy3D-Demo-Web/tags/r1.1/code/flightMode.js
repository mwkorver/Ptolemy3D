// Camera can fly at a constant altitude from the terrain (true) or
// can fly at a constant altitude from the zero altitude reference (false):

var camera = ptolemy.getCamera();
camera.setTerrainFollowMode( !camera.getTerrainFollowMode() );
