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
    ptapplet = document.getElementById('pTolemy3D').getSubApplet();
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



