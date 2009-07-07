// Other methods to set the camera positioning:
var camera = ptolemy.getCamera();
var lat = camera.getLatitude();
var lon = camera.getLongitude();
var alt = camera.getAltitude();
var dir = camera.getDirection();
var tilt = camera.getPitch();

var inc = 10;

// Uncomment the preferred method:
//camera.setOrientation(lat, lon + inc, alt, dir - inc, tilt - inc);
camera.setAltitude(alt + inc * 10000);
//camera.setPitchDegrees(tilt + inc, 0.1);

