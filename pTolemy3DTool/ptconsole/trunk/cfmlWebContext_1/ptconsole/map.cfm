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

<!--- sets default session params on startup --->
<cfparam name="session.server" default="wms.jpl.nasa.gov/wms.cgi">
<cfparam name="session.map" default="">
<cfparam name="session.srs" default="EPSG:4326">
<cfparam name="session.styles" default="">
<cfparam name="session.bbox" default="-180,-90,180,90">
<cfparam name="session.layers" default="global_mosaic">
<cfparam name="session.width" default="1024">
<cfparam name="session.version" default="1.1.1">
<cfparam name="session.format" default="image/png">

<script type="text/javascript" src="./scripts/jquery.js"></script>
<script type="text/javascript" src="./scripts/map.js"></script>

<script language="JavaScript">
var origLLLON=-180.00; var origLLLAT=-90.00; var origURLON=180.00; var origURLAT=90.00;
var WMSstr="";
var BBOX="<cfoutput>#session.bbox#</cfoutput>";
var debugMode="false";
</script>

<body onload=updateMap()>

<h2 style="font-size:24px; color:#666666">pTolemy3D Create JP2 Console</h2>


<div id="header">
    <ul id="primary">
        <li><a href="index.cfm">Home</a></li>
		<li><span>MAP</span> </li>
		<li><a onclick="getBBOX();">JOBS</a></li>
		<li><a href="run.cfm">RUN</a></li>
		<li><a href="../view/index.cfm">3D</a></li>
    </ul>
</div>
<div id="main">

<div id="contents">
<form name="wmsForm" id="wmsForm">

Use your own or try one of the sample WMS sources. Please use these servers sparingly!<br> 
Your IP could (will) get restricted if you overuse them<br>

<input type="radio" id=wms_radio name=wms_radio checked onclick="wmsRadio('wms.jpl.nasa.gov/wms.cgi','','global_mosaic','')"/>&nbsp;
<a href="http://onearth.jpl.nasa.gov" target="_blank">JPL Global Mosaic, pseudo color</a>


<input type="radio" id=wms_radio name=wms_radio onclick="wmsRadio('wms.jpl.nasa.gov/wms.cgi','','BMNG')"/>&nbsp;
<a href="http://onearth.jpl.nasa.gov" target="_blank">JPL BlueMarble NextGen</a>&nbsp;



<table border='0' cellspacing='5' cellpadding='1'>
<tr>
<td align="left" valign="top" id="mapTD">
<img id='mapimage' name='mapimage' src='./img/openaerialmap.org.jpg' border='1' height='512' width='512' alt='click map to recenter' onClick=getMouseXY(event);>
</td>

<td valign="top" width=300>
<table border='0' bgcolor='' cellspacing='1' cellpadding='1'>

<tr>
	<td>SERVER&nbsp;&nbsp;http://</td>
<!--- 	<td colspan=3><input type="text" name="SERVER" value="openaerialmap.org/wms/" size="50"></td> --->	
<cfoutput>
	<td colspan=3><input type="text" name="SERVER" value="#session.server#" size="50"></td>
</tr>
<tr>
	<td>MAP</td>
	<td colspan=3>
		<input type="text" name="MAP" value="#session.map#" size="50"></td></tr>
<tr>
	<td>SRS</td>
	<td><input type="text" name="SRS" value="#session.srs#"></td>
</tr>
<tr>
	<td>FORMAT</td>
	<td colspan=3>
		<input type="radio" name="FORMAT" value="image/png" <cfif session.format EQ "image/png">checked</cfif> >png
		<input type="radio" name="FORMAT" value="image/png; mode=24bit" <cfif session.format EQ "image/png; mode=24bit">checked</cfif> >24bit png (best)
-  has to be png to work
</tr>
<tr>
	<td>LAYERS</td>
	<td><input type="text" name="LAYERS" value="#session.layers#"></td>
	<td>STYLES</td>
	<td><input type="text" name="STYLES" value="#session.styles#">
	</td>
</tr>
<tr>
	<td>WIDTH</td>
	<td><input type="text" name="WIDTH" value="#session.width#" onchange="document.getElementById('spanHEIGHT').innerHTML=this.value"/></td>
	<td>HEIGHT</td>
	<td><span id="spanHEIGHT">#session.width#</span></td>
</tr>
<tr>
	<td>VERSION</td>
	<td colspan=3><input type="text" name="VERSION" value="#session.version#"></td>
</tr>
<tr>
	<td>BBOX</td>
	<td colspan=3><input type="text" name="SBBOX" size=30 value="#session.bbox#">&nbsp;&nbsp;<input type="button" name="button1" value="Reset" onClick='resetBBOX()';></td>
</tr>
		<input type="hidden" name="LLLON" value="#listgetat(session.bbox,1,",")#">
		<input type="hidden" name="LLLAT" value="#listgetat(session.bbox,2,",")#">		
		<input type="hidden" name="URLON" value="#listgetat(session.bbox,3,",")#">
		<input type="hidden" name="URLAT" value="#listgetat(session.bbox,4,",")#">
<tr>
	<td>

	</td>
</tr>
<tr>
	<td>DD TILE WIDTH</td>
	<td colspan=3>
      <select id="DDWIDTH" name="DDWIDTH" onChange="updateBBOX(this.options[this.selectedIndex].value);">
	  <option value="select" > Select Layer 
      <option value="52428800" > Level 52.4288 (4.6km pixel)
      <option value="6553600" > Level 06.5536 (580m pixel)
      <option value="819200" > Level 00.8192 (73m pixel)
      <option value="102400" > Level 00.1024 (9m pixel)
      <option value="12800" > Level 00.0128 (1m pixel)
      <option value="1600" > Level 00.0016 (0.14m pixel)
      </select>
	  &nbsp; Ptolemy3D tile sizes
	</td>
</tr>
<tr>
	<td>Click Mode</td>
	<td colspan=3>
      <select id="ZOOM" name="ZOOM" size="1">
      <option value=".25" > Zoom in 4 times
	  <option value=".333" > Zoom in 3 times
      <option value=".5" > Zoom in 2 times
      <option value="1"  selected> Recenter Map
      <option value="2" > Zoom out 2 times
	  <option value="3" > Zoom out 3 times
      <option value="4" > Zoom out 4 times
      </select>&nbsp;
      
	  &nbsp;&nbsp;<input type="checkbox" name="showWMSrequest"> Show WMS string<br>
	 
	</td>
</tr>
<tr>
	<td> <input type="text" name="SHEIGHT" value="400" size="10"></td><td colspan=2> Show map at this height <br>(constrain image size)</td>
</tr>

<tr><td></td>
	<td colspan=2><input type="button" name="button1" value="update map" onClick="updateMap()"></td>
</tr>

</table>
</cfoutput>
</form>

<span id="logSpan"></span>
</td></tr>
</table>







