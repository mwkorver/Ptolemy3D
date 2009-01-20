/**
 * Ptolemy3D JavaSctipt API.
 *
 * This file contains a set wrapper functions to call PTolemy3D applet methods.
 */ 
    
/**
 * Called when Ptolemy applet is started.
 */
//function sc_start(){
//}

/**
 * Called when the user click on the globe,
 */
//function plotPosition(latitude,longiude,altitude) {
//}
    
/**
 * Spatialcloud object contains the main function to work with the viewer applet.
 * @elemId the identifier of the applet element in the HTML page.
 */
function Ptolemy(elemId){
    
    // Stores the viewer applet HTML element id.
    this.elementId = elemId;
    this.mode_display = 0;
    this.mode_move = 1;
    this.mode_zoom = 2;
    
    /**
     * Returns a reference to the applet object given the 'elemId'.
     */    
    Ptolemy.prototype.getViewerApplet = function(){
        return document.getElementById(this.elementId).getSubApplet();
    }
    
    /**
     * Returns a reference of the viewer's Camera object.
     */
    Ptolemy.prototype.getCamera = function(){
        var applet = this.getViewerApplet();
        
        if(applet != null){
            return new Camera(this.getViewerApplet());   
        } else {
            return null;
        }
    };
    
    /**
     * Returns a reference of the viewer's Terrain object.
     */
    Ptolemy.prototype.getTerrain = function(){
        var applet = this.getViewerApplet();
        
        if(applet != null){
            return new Terrain(this.getViewerApplet());   
        } else {
            return null;
        }
    };

    /**
     * Set the behavior of mouse click event.
     * @param mode Integer value.
     * 0 - Display coordinates of the position picked on the terrain and call the javascript <i>plotPosition</i> function.
     * 1 - Move to the picked position on the terrain.
     * 2 - Move and zoom to the picked position on the terrain.
     */
    Ptolemy.prototype.setOnClickMode = function(mode){
        var applet = this.getViewerApplet();
        
        if(applet != null){
            applet.setOnClickMode(mode);   
        }        
    }
}

/**
 * Camera object encapsulates the functionallities of the camera.
 */
function Camera(applet){

    this.appletRef = applet;
    
    /**
     * Stop fly movement, started with flyTo.
     */
    Camera.prototype.flyStop = function(){
        if(this.appletRef != null){
            this.appletRef.flyStop();
        }
    }

    /**
     * Fly to a position represented by a lattitude/longitude/altitude.
     * To stop the movement before its end, uses flyStop().
     * Usage: flyTo(lattitudeInDegree, longitudeInDegrees, altitude)
     * @param lat lattitude, unit is degree.
     * @param lon longitude, unit is degree.
     * @param alt altitude, unit is in meters. Zero is the altitude of the ground with no elevation.     
     * @see #flyStop()
     * TODO Is the altitude in meters ?
     */
    Camera.prototype.flyTo = function(lat, lon, alt){
        if(this.appletRef != null){
            if(this.checkFlightStatus()){
                return;
            }
            this.appletRef.flyTo(lon, lat, alt);
        }
    }    
    
    /**
     * Fly to a position represented by a lattitude/longitude/altitude and arc degrees.
     * To stop the movement before its end, uses flyStop().
     * Usage: flyTo(lattitudeInDegree, longitudeInDegrees, altitude)
     * @param lat lattitude, unit is degree.
     * @param lon longitude, unit is degree.
     * @param alt altitude, unit is in meters. Zero is the altitude of the ground with no elevation.     
     * @param arc angle in degree, default value is 0.
     * @see #flyStop()
     * TODO Is the altitude in meters ?
     */
    Camera.prototype.flyToArc = function(lat, lon, alt, arc){
        if(this.appletRef != null){
            if(this.checkFlightStatus()){
                return;
            }
            this.appletRef.flyTo(lon, lat, alt, arc);
        }
    }    
    
    /**
     * Fly to a position represented by a lattitude/longitude/altitude and arc degrees.
     * To stop the movement before its end, uses flyStop().
     * Usage: flyTo(lattitudeInDegree, longitudeInDegrees, altitude)
     * @param lat lattitude, unit is degree.
     * @param lon longitude, unit is degree.
     * @param alt altitude, unit is in meters. Zero is the altitude of the ground with no elevation.     
     * @param dir
     * @param tilt
     * @param arc angle in degree, default value is 0.
     * @param cruise_tilt
     * @param speed
     * @see #flyStop()
     * TODO Is the altitude in meters ?
     */
    Camera.prototype.flyToPositionSpeed = function(lat, lon, alt, dir, tilt, arc, cruise_tilt, speed){
        if(this.appletRef != null){
            if(this.checkFlightStatus()){
                return;
            }
            this.appletRef.flyToPositionSpeed(lon, lat, alt, dir, tilt, arc, cruise_tilt, speed);
        }
    }    
    
    /**
     * Return true if the view is moving.
     */
    Camera.prototype.checkFlightStatus = function (){
        if(this.appletRef != null){
            return this.appletRef.checkFlightStatus();
        }
        else return false;
    }
    
    /**
     * Returns the camera X position.
     */
    Camera.prototype.getCameraPositionX = function (){
        if(this.appletRef != null){
            return this.appletRef.getCameraPositionX();
        }
        else return 0;
    }
    
    /**
     * Returns the camera Y position.
     */
    Camera.prototype.getCameraPositionY = function (){
        if(this.appletRef != null){
            return this.appletRef.getCameraPositionY();
        }
        else return 0;
    }
    
    /**
     * @param lat latitude, unit is degree.
     * @param lon longitude, unit is degree.
     * @param alt altitude in meters. Zero is the altitude of the ground with no elevation.
     * @param direction unit in degrees.
     * @param pitch unit in degrees.
     */
    Camera.prototype.setOrientation = function(lat, lon, alt, direction, pitch){
        if(this.appletRef != null){
            this.appletRef.setOrientation(lon, lat, alt, direction, pitch);
        }
    }
    
    /**
     * Returns the latitude of the camera's center position in decimal degrees.
     */
    Camera.prototype.getLatitude = function(){
        if(this.appletRef != null){
            var latitude = this.appletRef.getLatitude();
            return latitude;
        }
        else {
            return null;
        }
    }
    
    /**
     * Set the latitude position of the camera.
     * @param lat latitude in decimal degrees.
     */
    Camera.prototype.setLatitude = function(lat){
        if(this.appletRef != null){
            this.appletRef.setLatitude(lat);
        }
    }
    
    /**
     * Returns the longitude of the camera's center position in decimal degrees.
     */
    Camera.prototype.getLongitude = function(){
        if(this.appletRef != null){
            var longitude = this.appletRef.getLongitude();
            return longitude;
        }
        else {
            return null;
        }
    }
    
    /**
     * Set the longitude position of the camera.
     * @param lon longitude in decimal degrees.
     */
    Camera.prototype.setLongitude = function(lon){
        if(this.appletRef != null){
            this.appletRef.setLongitude(lon);
        }
    }
    
    /**
     * Returns the altitude in meters, zero is the altitude of the ground with no elevation.
     */
    Camera.prototype.getAltitude = function(){
        if(this.appletRef != null){
            var altitude = this.appletRef.getAltitude()
            return altitude;
        }
        else {
            return null;
        }
    }
    
    /**
     * Returns the altitude of the view in meters from the zero altitude reference.
     */
    Camera.prototype.getAltitudeMeters = function(){
        if(this.appletRef != null){
            var altitude = this.appletRef.getAltitudeMeters()
            return altitude;
        }
        else {
            return null;
        }
    }
    
    /**
     * The altitude of the view, in meters, from the zero altitude reference.
     * The zero altitude reference is the terrain with no elevation.
     */
    Camera.prototype.setAltitude = function (alt){
        if(this.appletRef != null){
            this.appletRef.setAltitude(alt);
        }
    }
    
    /**
     * Returns the direction of the camera's center position.
     */
    Camera.prototype.getDirection = function(){
        if(this.appletRef != null){
            var direction = this.appletRef.getDirection()
            return direction;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Set the direction, in degrees.
     */
    Camera.prototype.setDirection = function (dir){        
        if(this.appletRef != null){
            this.appletRef.setDirection(dir);
        }
    }
    
    /**
     * Returns the pitch of the camera's center position.
     */
    Camera.prototype.getPitch = function(){
        if(this.appletRef != null){
            var pitch = this.appletRef.getPitch()
            return pitch;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Set the pitch, in degrees
     */
    Camera.prototype.setPitch = function (pitch){        
        if(this.appletRef != null){
            this.appletRef.setPitch(pitch);
        }
    }
    
    /**
     * Set pitch in degrees, and pitch increment in degrees (optional, can be null)
     */
    Camera.prototype.setPitchDegrees = function(pitch, pitchIncrement){
        if(this.appletRef != null){
            this.appletRef.setPitchDegrees(pitch, pitchIncrement);
        }
    }

    /**
     * TODO - ???
     */
    Camera.prototype.setAccelleration = function(val){
        if(this.appletRef != null){
            this.appletRef.setAccelleration(val);
        }
    }

    /**
     * TODO - ???
     */
    Camera.prototype.setVelocity = function(val){
        if(this.appletRef != null){
            this.appletRef.setVelocity(val);
        }
    }

    /**
     * TODO - ???
     */
    Camera.prototype.setAngularSpeed = function(val){
        if(this.appletRef != null){
            this.appletRef.setAngularSpeed(val);
        }
    }

    /**
     * Enables/disables realistic flight.
     * @val a boolean value
     */
    Camera.prototype.setRealisticFlight = function(val){
        if(this.appletRef != null){
            if(val){
                this.appletRef.setRealisticFlight(1);
            }else{
                this.appletRef.setRealisticFlight(0);
            }            
        }
    }

    /**
     * Returns the status of realistic flight flag.
     */
    Camera.prototype.getRealisticFlight = function(){
        if(this.appletRef != null){
            if(this.appletRef.getRealisticFlight()==1){
                return true;
            }else{
                return false;
            }
        }
        return false;
    }
    
    /**
     * Terrain follow mode.
     */
    Camera.prototype.getTerrainFollowMode = function(){
        if(this.appletRef != null){
            return this.appletRef.getTerrainFollowMode();
        }else{
            return false;
        }
    }
    
    /**
     * Follow the terrain elevation, ie fly at a constant altitude from the terrain ("true").
     * Or, fly at a constant altitude from the zero altitude reference ("false").
     * Default value is "false".
     */
    Camera.prototype.setTerrainFollowMode = function(val){
        if(this.appletRef != null){
            if(val){
                this.appletRef.setTerrainFollowMode(true);
            }else{
                this.appletRef.setTerrainFollowMode(false);  
            }
        }
    }
}

/**
 * Terrain
 */
function Terrain(applet){

    this.appletRef = applet;
    
    /**
     * Get current terrain scale.
     */
    Terrain.prototype.getTerrainScale = function(){
        if(this.appletRef != null){
            return this.appletRef.getTerrainScale();
        }else{
            return 1;
        }
    }
    
    /**
     * Set terrain elevation scaler.
     * Default value is 1.
     */
    Terrain.prototype.setTerrainScale = function(val){
        if(this.appletRef != null){
            this.appletRef.setTerrainScale(val);
        }
    }
    
    /**
     * Get current terrain status.
     */
    Terrain.prototype.getTerrainStatus = function(){
        if(this.appletRef != null){
            return this.appletRef.getTerrainStatus();
        }else{
            return true;
        }
    }
    
    /**
     * Enable ("1") or disable ("0") landscape terrain elevation.
     */
    Terrain.prototype.setTerrainStatus = function(val){
        if(this.appletRef != null){
            if(val){
                this.appletRef.setTerrainStatus(true);
            }else{
                this.appletRef.setTerrainStatus(false);
            }  
        }
    }
    
    /**
     * Get current the horizon atmosphere altitude, in meters.
     */
    Terrain.prototype.getHorizonAlt = function(){
        if(this.appletRef != null){
            return this.appletRef.getHorizonAlt();
        }else{
            return true;
        }
    }
    
    /**
     * Set the earth atmosphere altitude, in meters.
     * The earth atmosphere will be visible when the view will be above this altitude.
     * Default value is 10000.
     */
    Terrain.prototype.setHorizonAlt = function(alt){
        if(this.appletRef != null){
            this.appletRef.setHorizonAlt(alt);
        }
    }
    
    /**
     * Get current status of the atmosphere.
     */
    Terrain.prototype.getAtmosphere = function(){
        if(this.appletRef != null){
            return this.appletRef.getAtmosphere();
        }else{
            return true;
        }
    }
    
    /**
     * Enable or disable earth atmosphere and stars.
     * @val boolean value
     */
    Terrain.prototype.setAtmosphere = function(val){
        if(this.appletRef != null){
            if(val){
                this.appletRef.setAtmosphere(true);
            }else{
                this.appletRef.setAtmosphere(false);
            }       
        }
    }

    /**
     * Change landscape rendering mode :
     * "0" Default display mode. (Landscape.DISPLAY_STANDARD)
     * "1" Landscape geometry is rendered in line mode, to visualize tile triangle geometry. (Landscape.DISPLAY_MESH)
     * "2" Landscape color is the terrain elevation. (Landscape.DISPLAY_SHADEDDEM)
     * "3" Tile color is the jp2 current resolution. (Landscape.DISPLAY_JP2RES)
     * "4" Tile color is the tile id. (Landscape.DiSPLAY_TILEID)
     */
    Terrain.prototype.setDisplayMode = function(ix){
        if(this.appletRef != null){
            this.appletRef.setDisplayMode(ix);
        }
    }
    
    /**
     * Get the current display mode.
     */
    Terrain.prototype.getDisplayMode = function(){
        if(this.appletRef != null){
            return this.appletRef.getDisplayMode();
        }else{
            return 0;
        }
    }
}

/**
 * Plugin
 */
function Plugin(applet){
    
    this.appletRef = applet;
    
    Plugin.prototype.findPlugin = function(ptype){
        if(this.appletRef != null){
            var pix = 1;
            var ret = "";
            var pstr=null,parr=null;
            while((ret != "NA") && (pix < 10000)){
                ret = "";
                pstr = this.appletRef.pluginAction(pix,"getInfo","");
                if(pstr != null) parr = pstr.split(",");
			
                if((parr != null) && (parr.length > 0)) ret = parr[0];
			
                if(ret == ptype) return pix;
                pix = pix - -1;
            }
        }
        return -1;
    }

    Plugin.prototype.pluginAction = function(pluginIndex , command , parameters){
        if(this.appletRef != null){
            return this.appletRef.pluginAction(pluginIndex , command , parameters);
        }
        else return "NA";
    }
}

/**
 * Input
 */
function Input(applet){

    this.appletRef = applet;
    
    Input.prototype.addCursor = function(server , curfile , nam){
        if(this.appletRef != null){
            this.appletRef.addCursor(server , curfile , nam);
        }
    }

    Input.prototype.setCursor = function(val){
        if(this.appletRef != null){
            this.appletRef.setCursor(val);
        }
    }

    Input.prototype.asciiKeyRelease = function(val){
        if(this.appletRef != null){
            this.appletRef.asciiKeyRelease(val);
        }
    }

    Input.prototype.asciiKeyPress = function(val){
        if(this.appletRef != null){
            this.appletRef.asciiKeyPress(val);
        }
    }
}
