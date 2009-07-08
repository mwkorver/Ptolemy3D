// Get main references
ptapplet = document.getElementById('pTolemy3D').getSubApplet();
ptolemy = ptapplet.getPtolemy();
canvas = ptapplet.getCanvas();
camera = canvas.getCameraMovement();

// CameraMovement contains all required methods to handle the camera.
lat = 42.13; lon=12.88; alt=1000000;
camera.setPosition(lat, lon, alt);
camera.setDirection(10);
camera.setPitch(-30);

// In addition it contains methods to get current values
//alert("Lat: "+ camera.getPosition().getLatitude() + " Lon: "+ camera.getPosition().getLongitude()+ " Alt: "+camera.getPosition().getAltitude());
//alert(camera.getDirection());
//alert(camera.getPitch());
