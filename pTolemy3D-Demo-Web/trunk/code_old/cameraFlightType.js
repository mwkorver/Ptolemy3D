// Change the camera flight type:
var camera = ptolemy.getCamera();

camera.setRealisticFlight( !camera.getRealisticFlight() );

// Change parameters used in when the realistic mode is on:
camera.setAccelleration(1);
camera.setVelocity(1);
camera.setAngularSpeed(1);

var lat = camera.getLatitude();
var lon = camera.getLongitude();
var alt = camera.getAltitude();
var inc = 10;

camera.flyTo(lat, lon + inc, alt);
