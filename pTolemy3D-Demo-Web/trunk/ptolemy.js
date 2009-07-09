// Reset values, to restore initial globe position.
var init_lat = 41.26;
var init_lon = -96.42;
var init_alt = 45000000;
var init_dir = 0;
var init_pitch = -20;
var init_camera_follow_dem = true;
var init_terrain_scale = 1;
var init_terrain_status = true;

/*
 * Called once the applet is initialized and ptolemy system started.
 */
function ptolemyStart(){

    // Get main references
    ptapplet = document.getElementById("pTolemy3D").getSubApplet();
    ptolemy = ptapplet.getPtolemy();
    canvas = ptapplet.getCanvas();
    camera = canvas.getCameraMovement();
    scene = ptolemy.getScene();
    land = scene.getLandscape();

    // Set initial camera position
    // Create a new Position object
    position = ptolemy.createInstance("org.ptolemy3d.view.Position");
    position.setLatitude(init_lat);
    position.setLongitude(init_lon);
    position.setAltitude(init_alt);

    camera.setOrientation(position, init_dir, init_pitch);
    camera.setFollowDem(init_camera_follow_dem);

    // Set landscape parameters
    land.setTerrainEnabled(init_terrain_status);
    land.setTerrainScaler(init_terrain_scale);
}

/*
 * Called once the applet is stopped.
 */
function ptolemyStop(){
    // Do nothing
}

/*
 * Reset. Set the initial content of textarea.
 */
function reset(){
    ptolemyStart();
    $('#freecode').load('./code/reset.js','',function (responseText, textStatus, XMLHttpRequest) {
        $('#freecode').val(responseText);
    });
}

/*
 * Moves the camera.
 */
function exexCameraMoveCode(){

    // Get parameters
    var lat = document.getElementById("latitude").value;
    var lon = document.getElementById("longitude").value;
    var alt = document.getElementById("altitude").value;
    var dir = document.getElementById("direction").value;
    var pit = document.getElementById("pitch").value;
    var realflight = document.getElementById("realflight").checked;
    var followdem = document.getElementById("followdem").checked;

    // Get ptolemy references
    ptapplet = document.getElementById("pTolemy3D").getSubApplet();
    canvas = ptapplet.getCanvas();
    camera = canvas.getCameraMovement();

    // Set new position
    camera.setPosition(lat, lon, alt);
    camera.setDirection(dir);
    camera.setPitch(pit);
    camera.setRealisticFlight(realflight);
    camera.setFollowDem(followdem);
}

/*
 * Moves the camera flight
 */
function exexCameraFlyCode(){

    // Get parameters
    var lat = document.getElementById("latitude").value;
    var lon = document.getElementById("longitude").value;
    var alt = document.getElementById("altitude").value;
    var dir = document.getElementById("direction").value;
    var pit = document.getElementById("pitch").value;
    var realflight = document.getElementById("realflight").checked;
    var followdem = document.getElementById("followdem").checked;

    // Get ptolemy references
    ptapplet = document.getElementById("pTolemy3D").getSubApplet();
    canvas = ptapplet.getCanvas();
    camera = canvas.getCameraMovement();

    // Set new position
    camera.flyTo(lat, lon, alt);
    //    camera.setDirection(dir);
    //    camera.setPitch(pit);
    //    camera.setRealisticFlight(realflight);
    //    camera.setFollowDem(followdem);
}

/*
 * Put camera code in the code area
 */
function seeCameraCode(){

    $("#tabs").tabs('select', 0);

    $('#freecode').val('// Get parameters \n\
var lat = document.getElementById("latitude").value; \n\
var lon = document.getElementById("longitude").value; \n\
var alt = document.getElementById("altitude").value; \n\
var dir = document.getElementById("direction").value; \n\
var pit = document.getElementById("pitch").value; \n\
var realflight = document.getElementById("realflight").checked; \n\
var followdem = document.getElementById("followdem").checked; \n\
 \n\
// Get ptolemy references \n\
ptapplet = document.getElementById("pTolemy3D").getSubApplet(); \n\
canvas = ptapplet.getCanvas(); \n\
camera = canvas.getCameraMovement(); \n\
 \n\
// Set new position \n\
camera.setPosition(lat, lon, alt); \n\
camera.setDirection(dir); \n\
camera.setPitch(pit); \n\
camera.setRealisticFlight(realflight); \n\
camera.setFollowDem(followdem); \n\
');

}

/*
 * Execute terrain code
 */
function exexTerrainCode(){

    // Get parameters
    var terrainEnabled = document.getElementById("terrainEnabled").checked;
    var terrainScaler = document.getElementById("terrainScaler").value;
    
    // Get main references
    ptapplet = document.getElementById("pTolemy3D").getSubApplet();
    ptolemy = ptapplet.getPtolemy();
    scene = ptolemy.getScene();
    land = scene.getLandscape();

    land.setTerrainEnabled(terrainEnabled);
    land.setTerrainScaler(terrainScaler);
}

/*
 * Put terrain code in the code area
 */
function seeTerrainCode(){

    $("#tabs").tabs('select', 0);

    $('#freecode').val('// Get parameters\n\
var terrainEnabled = document.getElementById("terrainEnabled").checked;\n\
var terrainScaler = document.getElementById("terrainScaler").value;\n\
\n\
// Get main references\n\
ptapplet = document.getElementById("pTolemy3D").getSubApplet();\n\
ptolemy = ptapplet.getPtolemy();\n\
scene = ptolemy.getScene();\n\
land = scene.getLandscape();\n\
\n\
land.setTerrainEnabled(terrainEnabled);\n\
land.setTerrainScaler(terrainScaler);');

}

