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

<CFINCLUDE template="head.cfm">

<body>
<h2 style="font-size:24px; color:#666666">pTolemy3D Create JP2 Console</h2>

<div id="header">
    <ul id="primary">
		<li><span>HOME</span> </li>
		<li><a href="map.cfm">MAP</a></li>
		<li><a href="jobs.cfm">JOBS</a></li>
		<li><a href="run.cfm">RUN</a></li>
		<li><a href="../view/index.cfm">3D</a></li>
    </ul>
</div>
<div id="main">
<div id="contents">

<h1 style="font-size:24px; color:#666666">Note</h1>
This sample web application is meant to show how you can use WMS servers<br>
 to generate new data for pTolemy3D.  The number of WMS requests required to create a layer<br>
 is a function of layer level (image resolution) and area (BBOX).  Because this process can<br>
 make many requests to the WMS server, a limiter, MaxQueue, has been place at the top of file jobs.cfm.  <br><br>
 
The Default MaxQueue is 1568, which is the global total for pTolemy3D layer 6553600, <br>
which equates to a jp2 tile width of 65.53600DD <br>  Use the MAP tab to restrict your process Area (BBOX).
<br>

<h1 style="font-size:24px; color:#666666">Howto</h1>
1. Use the <strong>MAP</strong> tab select WMS server to use and define your BBOX. <br>
2. Create <strong>JOBS</strong> as a function of BBOX and Tile Layer. <br>
3. <strong>RUN</strong> jobs placed in queue to create jp2 tile pyramids<br>
4. Use pTolemy3D Viewer demo app to see output<br>



</div>
</div>
</body></html>