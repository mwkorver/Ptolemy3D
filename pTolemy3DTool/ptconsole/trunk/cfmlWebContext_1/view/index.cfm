<!--- 
* Ptolemy3D - a Java-based 3D Viewer for GeoWeb applications.
* Copyright (C) 2008 Mark W. Korver
*
* This program is free software: you can redistribute it and/or modify * it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>.
--->
<cfinclude template="../ptconsole/head.cfm">

        <script type="text/javascript" src="ptolemy3d.js"></script>

        <script type="text/javascript">
            function flyTo(lat,lon,alt){
		 // Create a Ptolemy object passing the applet identifier.
	                var ptolemy = new Ptolemy('pTolemy3D');
	                var camera = ptolemy.getCamera();
	                camera.flyTo(lat, lon, alt);
			}
        </script>
    </head>


<h2 style="font-size:24px; color:#666666">pTolemy3D Create JP2 Console</h2>

<div id="header">
    <ul id="primary">
        <li><a href="../ptconsole/index.cfm">Home</a></li>
		<li><a href="../ptconsole/map.cfm">MAP</a></li>
		<li><a href="../ptconsole/jobs.cfm">JOBS</a></li>
		<li><a href="../ptconsole/run.cfm">RUN</a></li>
		<li><span>3D</span> </li>
    </ul>
</div>
	
<div id="main" style="height: 100%;">
<div id="contents" style="height: 400px;">

If you have an empty Globe here it's because you have not created data yet.<br>

		<applet code="com.sun.opengl.util.JOGLAppletLauncher"
                align="center" width="100%" height="100%"
                id="pTolemy3D" Name="pTolemy3D"
                codebase="./applet/"
                archive="./jogl.jar,./gluegen-rt.jar,./plugin.jar,./ptolemy3d.jar,./ptolemy3d-1.0-SNAPSHOT.jar,./ptolemy3dplugin-1.0-SNAPSHOT.jar" MAYSCRIPT>
            <param name="subapplet.classname" VALUE="org.ptolemy3d.viewer.Ptolemy3DApplet">
            <param name="subapplet.displayname" VALUE="pTolemy3D Applet">
            <param name="progressbar" value="true">
            <param name="noddraw.check" value="true">
            <param name="noddraw.check.silent" value="true">
            <param name="cache_archive" VALUE="jogl.jar,gluegen-rt.jar,plugin.jar,ptolemy3d-1.0-SNAPSHOT.jar,ptolemy3dplugin-1.0-SNAPSHOT.jar">
            <param name="cache_archive_ex" VALUE="jogl.jar;preload,gluegen-rt.jar;preload,plugin.jar;preload,ptolemy3d-1.0-SNAPSHOT.jar;preload,ptolemy3dplugin-1.0-SNAPSHOT.jar;preload">

            <param name="Orientation" value="-85493900,30738580,126000000,10,0">
            <param name="CenterX" value="0">
            <param name="CenterY" value="0">
            <param name="DisplayRatio" value="1">

            <param name="Server" value="127.0.0.1:8080">
            <param name="nMapDataSvr" value="1">
            <param name="MapDataSvr1" value="127.0.0.1:8080">
           <!---  <param name="MapJp2Store1" value="/jp2/jp2base/"> --->
			
			<param name="MapJp2Store1" value="/ptconsole/working/jp2/">
			
       <!---      <param name="MapDemStore1" value="/dem/world/"> --->
            <param name="MUrlAppend1" value="&streamID=test">
            <param name="MServerKey1" value="">

            <param name="TILECOLOR" value="47,47,86">
            <param name="Area" value="00">
            <param name="HORIZON" value="10000">
            <param name="FlightFrameDelay" value="200">
            <param name="TIN" value="1">
            <param name="FOLLOWDEM" value="0">

            <param name="Units" value="DD">
            <param name="BGCOLOR" value="0,0,0">
            <param name="SubTextures" value="1">

            <param name="NumLayers" value="3">

            <param name="LayerWidth_1" value="52428800">
            <param name="LayerDEM_1" value="9">
            <param name="LayerMIN_1" value="5000">
            <param name="LayerMAX_1" value="10000000">
            <param name="LayerDIVIDER_1" value="0">

            <param name="LayerWidth_2" value="6553600">
            <param name="LayerDEM_2" value="9">
            <param name="LayerMIN_2" value="10000">
            <param name="LayerMAX_2" value="5000000">
            <param name="LayerDIVIDER_2" value="0">

            <param name="LayerWidth_3" value="819200">
            <param name="LayerDEM_3" value="9">
            <param name="LayerMIN_3" value="2000">
            <param name="LayerMAX_3" value="500000">			
		    <param name="LayerDIVIDER_3" value="0">
			
            <param name="LayerWidth_4" value="102400">
            <param name="LayerDEM_4" value="9">
            <param name="LayerMIN_4" value="0">
            <param name="LayerMAX_4" value="50000">
            <param name="LayerDIVIDER_4" value="0">

            <param name="LayerWidth_5" value="12800">
            <param name="LayerDEM_5" value="9">
            <param name="LayerMIN_5" value="0">
            <param name="LayerMAX_5" value="10000">
            <param name="LayerDIVIDER_6" value="0">
			
            <param name="LayerWidth_6" value="1600">
            <param name="LayerDEM_6" value="9">
            <param name="LayerMIN_6" value="0">
            <param name="LayerMAX_6" value="2000">
            <param name="LayerDIVIDER_6" value="0">


            <param name="NumPlugins" value="1">

            <param name="PluginType_1" value="org.ptolemy3d.plugin.HudPlugin">
            <param name="PluginParam_1" value="-hudPrefix1 Center: -hudSuffix1 dd -hudPrefix2 Speed: -hudPrefix3 Altitude: -hudSuffix3 m">

        </applet>

	  </div>
	  
	    <div id="bttmform" style="height: 100%;">
	        <form name="flyForm">
			<table border=0>		
			<tr><td width="15px" rowspan="4"></td><td>Latitude (decimal degrees):</td>
			<td><input type="text" id="lat" name="lat" value="1.1" size="6" /></td><td width="25" align="right">Quicklinks</td>
			<td align="right" valign="top" rowspan=4><input type="button" value="America" name="go" onclick="flyTo(30.73858,-85.49389,10000000);"/><br>
	            <input type="button" value="Africa" name="go" onclick="flyTo(1.1,20.66,10000000);"/>  <br>
				<input type="button" value="Europe" name="go" onclick="flyTo(48.28,0.79,4000000);"/></td>
			</tr>
			<tr><td>Longitude (decimal degrees):</td>
			<td><input type="text" id="lon" name="lon" value="20.66" size="6" /></td></tr>
			<tr><td>Altitude (meters):</td>
			<td><input type="text" id="alt" name="alt" value="10000000" size="6" /></td></tr>
			<tr><td align="right"colspan=2><input type="button" value="Go To Coordinates" name="go" onclick="flyTo(document.getElementById('lat').value,document.getElementById('lon').value,document.getElementById('alt').value);"/><br>   
			</td><td>&nbsp;&nbsp;<br></td></tr>
			</table>
	        </form>
	   </div>
	 </div>
	

    </body>
</html>
