// Terrain can be enabled/disabled:
var terrain = ptolemy.getTerrain();
terrain.setTerrainStatus( !terrain.getTerrainStatus() ); // Change it between true/false.
//
// You can change the vertical terrain exaggeration:
terrain.setTerrainScale( terrain.getTerrainScale()+1 );

// Enable/disable the atmosphere effect:
terrain.setAtmosphere( !terrain.getAtmosphere() );
// And also change the terrain display mode:
terrain.setDisplayMode("0");
