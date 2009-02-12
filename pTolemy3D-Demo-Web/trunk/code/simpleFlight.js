// Move the camera using the 'flyTo' method on its simplest form.
// Given a latitude, longitude (in decimal degrees) and an altitude (in meters):\n
// Get current values
var camera = ptolemy.getCamera();
var lat = camera.getLatitude();
var lon = camera.getLongitude();
var alt = camera.getAltitude();
var inc = 10;

camera.flyTo(lat, lon + inc, alt);

// Also you can use the 'flyStop' method to stop the flight movement.
// camera.flyStop();
