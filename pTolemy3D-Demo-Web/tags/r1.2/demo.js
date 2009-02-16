/**
 * Actions for demo learning web page.
 */

// Reference to Ptolemy object.
var ptolemy;

var init_lat = 132.730000;
var init_lon = 35.010000;
var init_alt = 1260000;
var init_dir = 0;
var init_pitch = -30;
var init_atmos = true;
var init_camx = 0;
var init_camy = 0;
var init_terrain_follow = true;
var init_terrain_scale = 1;
var init_terrain_status = true;
var init_horizon = 10000;

/**
 * Called once the applet is initialized and started.
 */
function sc_start(){
    
    // Create a Ptolemy instance from the applet element identifier
    // in the HTML file.
    ptolemy = new Ptolemy('pTolemy3D');
    
    // TODO - Seems to return undefinied values.
    // Store inital params to allow reset the scene.
    init_lat = ptolemy.getCamera().getLatitude();
    init_lon = ptolemy.getCamera().getLongitude();
    init_alt = ptolemy.getCamera().getAltitude();
    init_dir = ptolemy.getCamera().getDirection();
    init_pitch = ptolemy.getCamera().getPitch();
    init_camx = ptolemy.getCamera().getCameraPositionX();
    init_camy = ptolemy.getCamera().getCameraPositionY();
    init_terrain_follow = ptolemy.getCamera().getTerrainFollowMode();
    init_atmos = ptolemy.getTerrain().getAtmosphere();
    init_terrain_scale = ptolemy.getTerrain().getTerrainScale();
    init_terrain_status = ptolemy.getTerrain().getTerrainStatus();
    init_horizon = ptolemy.getTerrain().getHorizonAlt();
}  

function reset(){
    
    //ptolemy.getCamera().setOrientation(init_lat, init_lon, init_alt, init_dir, init_pitch);
    ptolemy.getCamera().setLatitude(init_lat);
    ptolemy.getCamera().setLongitude(init_lon);
    ptolemy.getCamera().setAltitude(init_alt);
    ptolemy.getCamera().setDirection(init_dir);
    ptolemy.getCamera().setPitch(init_pitch);
    ptolemy.getCamera().setTerrainFollowMode(init_terrain_follow);
    ptolemy.getTerrain().setTerrainStatus(init_terrain_status);
    ptolemy.getTerrain().setAtmosphere(init_atmos);
    ptolemy.getTerrain().setHorizonAlt(init_horizon);

    var example = "// Use the 'ptolemy' object to interact with the viewer.\n\
// Put your code here...\n\
";

    $('#freecode').val(example);
}

///////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////

function simple_flight(){
    
    var example = "// Move the camera using the 'flyTo' method on its simplest form.\n\
// Given a latitude, longitude (in decimal degrees) and an altitude (in meters):\n \n\
// Get current values \n\
var camera = ptolemy.getCamera(); \n\
var lat = camera.getLatitude();\n\
var lon = camera.getLongitude();\n\
var alt = camera.getAltitude();\n\
var inc = 10;\n\n\
camera.flyTo(lat, lon + inc, alt);\n\n\
// Also you can use the 'flyStop' method to stop the flight movement.\n\
// camera.flyStop();";
    
    $('#freecode').val(example);
}

function complex_flight(){
    
    var example = "// Move the camera using more complex forms.\n\
// Given a latitude, longitude (in decimal degrees) and an altitude (in meters):\n \n\
// Get current values \n\
var camera = ptolemy.getCamera(); \n\
var lat = camera.getLatitude();\n\
var lon = camera.getLongitude();\n\
var alt = camera.getAltitude();\n\
var inc = 10;\n\
var arc = 0.0;\n\
var dir = camera.getDirection();\n\
var tilt = camera.getPitch();\n\
var cruise_tilt = 0;\n\
var speed = 10000;\n\n\
// Uncomment the preferred method:\n\
//camera.flyToArc(lat, lon + inc, alt, arc);\n\
camera.flyToPositionSpeed(lat, lon + inc, alt, dir, tilt, arc, cruise_tilt, speed);";
    
    $('#freecode').val(example);    
}

function positioning(){
    
    var example = "// Other methods to set the camera positioning:\n\
var camera = ptolemy.getCamera(); \n\
var lat = camera.getLatitude();\n\
var lon = camera.getLongitude();\n\
var alt = camera.getAltitude();\n\
var dir = camera.getDirection();\n\
var tilt = camera.getPitch();\n\n\
var inc = 10;\n\n\
// Uncomment the preferred method:\n\
//camera.setOrientation(lat, lon + inc, alt, dir - inc, tilt - inc);\n\
camera.setAltitude(alt + inc * 10000);\n\
//camera.setPitchDegrees(tilt + inc, 0.1);";
      
    $('#freecode').val(example); 
}

function camera_flight_type(){

    var example = "// Change the camera flight type:\n\
var camera = ptolemy.getCamera(); \n\n\
camera.setRealisticFlight( !camera.getRealisticFlight() );\n\n\
// Change parameters used in when the realistic mode is on:\n\
camera.setAccelleration(1);\n\
camera.setVelocity(1);\n\
camera.setAngularSpeed(1);\n\n\
var lat = camera.getLatitude();\n\
var lon = camera.getLongitude();\n\
var alt = camera.getAltitude();\n\
var inc = 10;\n\n\
camera.flyTo(lat, lon + inc, alt);";
      
    $('#freecode').val(example);     
}

function click_mode(){
    
    var example = "// Change the default action when on mouse click events:\n\
// Available modes are: ptolemy.mode_display, ptolemy.mode_move and ptolemy.mode_zoom;\n\
var mode = ptolemy.mode_display;\n\
ptolemy.setOnClickMode(mode);";
      
    $('#freecode').val(example); 
}

function terrain_options(){
    
    var example = "// Terrain can be enabled/disabled:\n\
var terrain = ptolemy.getTerrain();\n\
terrain.setTerrainStatus( !terrain.getTerrainStatus() ); // Change it between true/false.\n\n\
// You can change the vertical terrain exaggeration:\n\
terrain.setTerrainScale( terrain.getTerrainScale()+1 );\n\n\
// Enable/disable the atmosphere effect:\n\
terrain.setAtmosphere( !terrain.getAtmosphere() );\n\
// And also change the terrain display mode:\n\
terrain.setDisplayMode(\"0\");";
      
    $('#freecode').val(example); 
}

function terrain_flight_mode(){
    
    var example = "// Camera can fly at a constant altitude from the terrain (true) or\n\
// can fly at a constant altitude from the zero altitude reference (false):\n\n\
var camera = ptolemy.getCamera();\n\
camera.setTerrainFollowMode( !camera.getTerrainFollowMode() );";
      
    $('#freecode').val(example); 
}
