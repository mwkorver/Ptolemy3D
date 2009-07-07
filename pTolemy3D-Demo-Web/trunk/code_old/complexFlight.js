// Move the camera using more complex forms.
// Given a latitude, longitude (in decimal degrees) and an altitude (in meters):

// Get current values
var camera = ptolemy.getCamera();
var lat = camera.getLatitude();
var lon = camera.getLongitude();
var alt = camera.getAltitude();
var inc = 10;
var arc = 0.0;
var dir = camera.getDirection();
var tilt = camera.getPitch();
var cruise_tilt = 0;
var speed = 10000;

// Uncomment the preferred method:
//camera.flyToArc(lat, lon + inc, alt, arc);
camera.flyToPositionSpeed(lat, lon + inc, alt, dir, tilt, arc, cruise_tilt, speed);

