## Ptconsole App ##

### Tested on FF3 and IE6&7 on Windows, and confirmed to run using FF3 on Linux and Mac OS X ###

ptconsole is a simple web application that allows you to encode jp2 file pyramids (layer by layer and area by area) by using 2D images pulled from WMS compliant servers such as [GeoServer](http://geoserver.org/) and [Mapserver](http://mapserver.org/).  You can point the web-app at your own servers or somebody else's as long as the servers comply with certain output specifications.  You can create jp2 files with the application, and then see the result in 3D in the 3D tab, via the pTolemy3D applet.  The download package includes an http server (Jetty), so if you wished, you could make some simple configuration changes to serve this data beyond your own desktop.

pTolemy3D's tile architecture, as explained in PtolemyDataTileSystem, defines what kind of output you need from the WMS system, the most important is that the WMS can output SRS=EPSG:4326 and produce images at a WIDTH/HEIGHT of 1024 square. You can enter any server url into the MAP tab to test whether the server can satisfy the default parameters of the MAP tab.  Use the DD WIDTH select box to emulate the exact tile size of any specific jp2 pyramid layer that you are interested in creating, to see if the server can serve data at that particular scale.

Because Ptconsole will make regular machine like requests to a server, please be conservative in it's use with 3rd party WMS servers.  You could overload the WMS server and be cut off from future use of that server.


### Steps, to deploy on Windows: ###

Steps to deploy ptconsole.

1. Install Java [download page here](http://www.java.com/en/download/manual.jsp)

2. Download the ptconsole application [here](http://ptolemy3d.s3.amazonaws.com/ptconsole/ptconsole_02272009.zip)

3. Unzip ptconsole.zip

4. Open a command window and cd to ptconsole directory. example: D:\ptconsole

5. The default configuration uses port 8080, stop other services using this port before you continue.

6. You should be in the directory with the start.jar file, at the command line type java -jar start.jar

7. Open browser to http://127.0.0.1:8080/bluedragon/administrator/login.cfm

8. Login with password "admin"

9. Goto Datasources http://127.0.0.1:8080/bluedragon/administrator/datasources/index.cfm

10. Add datasource with

> |Datasource Name:| jobqueue |
|:---------------|:---------|
> |Database Type:  | H2 Embedded|

> Hit Add Datasource button

> Note: If the datasource already exists, delete it first, then add it.

10. In the Configure Datasource - H2 Embedded page next

> |Database Name:|  jobqueue|
|:-------------|:---------|
> |User Name:  |    test|
> |Password:   |    test|

> Hit the Submit button.

11. Open Browser to http://127.0.0.1:8080/ptconsole

12. Follow directions on HOME tab. If puzzled [Participate in pTolemy3D discussion and mailing list](http://groups.google.com/group/ptolemy3d). The main idea is to go from left to right wizard fashion.  If you want to see output jp2 files, to load them into desktop GIS etc. you can see output jp2 & jpw files here \ptconsole\webroot\_cfmlapps\cfmlWebContext\_1\ptconsole\working\jp2.



### Steps, to deploy on Linux: ###

Fundamentally exactly the same, just better.


### Known Issues: ###

  * If you move to 3D tab when a job is running in RUN tab, that process will end. You can go back and continue., you can get around this by running it in another browser page.

  * Many (most?) wms servers serving Global data will not work if you make a bbox request that goes outside of -180,-90,180,90.  The outer edge of the jp2 tile structure overlaps this boundary and requires this.  The not very good work around if you just output some test data is to avoid this, simply by zooming in a bit.

  * If you have downloaded a previous version of ptconsole (pre 2/20/09) and are having a Java error that has to do with a "sealing violation" please download the attached jj2000-5.1-mod.jar and replace the file here

> \ptconsole\webroot\_cfmlapps\cfmlWebContext\_1\ptconsole\lib\openbd