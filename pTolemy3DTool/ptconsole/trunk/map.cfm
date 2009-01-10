<!--- 
* Loop on tilewidth to create a list of jp2 filenames for some bbox
*
* @param tilewidth     width and height of jp2 file. (Required)
* @param BBOX     Extents as list (llrlong,lllat, urlong, urlat. (Required)
* @return Returns List
* @author Mark Korver
* @version 1, 9/22/2008
--->

<CFINCLUDE template="head.cfm">

<script language="JavaScript">

var origLLLON=-90.00; var origLLLAT=-90.00; var origURLON=90.00; var origURLAT=90.00;

var WMSstr="";
var BBOX="";

var debugMode="false";

function getMouseXY(e) // works on IE6,FF,Moz,Opera7
{ 
  if (!e) e = window.event; // works on IE, but not NS (we rely on NS passing us the event)

  if (e)
  { 
    if (e.pageX || e.pageY)
    { // this doesn't work on IE6!! (works on FF,Moz,Opera7)
      mousex = e.pageX;
      mousey = e.pageY;
      algor = '[e.pageX]';
      if (e.clientX || e.clientY) algor += ' [e.clientX] '
    }
    else if (e.clientX || e.clientY)
    { // works on IE6,FF,Moz,Opera7
      mousex = e.clientX + document.body.scrollLeft;
      mousey = e.clientY + document.body.scrollTop;
      algor = '[e.clientX]';
      if (e.pageX || e.pageY) algor += ' [e.pageX] '
    }  
  }
  
  calcMap(mousex,mousey);  
  }
 
 
  
function calcMap(mousex,mousey) {
  
  mousex = mousex - 24 ;
  //mouse position is from top so to get y from LL of image, the 211 is the offset from page
  mousey = (document.mapimage.height - mousey) + 211;
  
	var LonDif = (document.wmsForm.LLLON.value - document.wmsForm.URLON.value);
	var LatDif = (document.wmsForm.LLLAT.value - document.wmsForm.URLAT.value);
	
	var OnePixelLonValue = Math.abs(LonDif/document.mapimage.width);
	var OnePixelLatValue = Math.abs(LatDif/document.mapimage.height);
	
	var NewCenterLon = (1*document.wmsForm.LLLON.value) + (mousex * OnePixelLonValue);
	var NewCenterLat = (1*document.wmsForm.LLLAT.value) +  (mousey * OnePixelLatValue);
	
	var zoom = document.getElementById('ZOOM').value
	
	//get corners from center as a function of zoom level
	var NLLLON = (NewCenterLon + zoom * (LonDif/2)).toFixed(6);
	var NLLLAT = (NewCenterLat + zoom * (LatDif/2)).toFixed(6);
	var NURLON = (NewCenterLon - zoom * (LonDif/2)).toFixed(6);
	var NURLAT = (NewCenterLat - zoom * (LatDif/2)).toFixed(6);
	
	BBOX = NLLLON + ',' + NLLLAT + ',' + NURLON + ',' + NURLAT;

	if(debugMode) {	
		var mousexy = 'x: ' + mousex + ', y: ' + mousey;
		var LonLatDif = 'londif: ' + LonDif + ', latdif: ' + LatDif;
		var lonlatpixvalues = 'lon: ' + OnePixelLonValue + ', lat: ' + OnePixelLatValue;
		var NewCenterLonLat = 'Center: ' + NewCenterLon + ', ' + NewCenterLat;
		
		logAppend('------------------------------------------','keep'); 
		logAppend('BBOX is ' + BBOX,'keep'); 
		logAppend('mousexy: ' + mousexy,'keep');   
		logAppend('LonLatDif: ' + LonLatDif,'keep'); 
		logAppend('NewCenterLonLat: ' + NewCenterLonLat,'keep');
		}

	document.wmsForm.LLLON.value = NLLLON;
	document.wmsForm.LLLAT.value = NLLLAT;	
	document.wmsForm.URLON.value = NURLON;
	document.wmsForm.URLAT.value = NURLAT; 
	document.wmsForm.sbbox.value = BBOX;
	
	// now that we have the newly calculated BBOX we can update the map
	updateMap(); 
     
}

function logAppend(i,mode) {
	if (mode=="keep") {
	var existingLog = document.getElementById('logSpan').innerHTML
	var i = i + '<br>' + existingLog;
	}
	document.getElementById('logSpan').innerHTML=i;
}

function updateMap() {
	BBOX = document.wmsForm.LLLON.value + ',' + document.wmsForm.LLLAT.value + ',' + document.wmsForm.URLON.value + ',' + document.wmsForm.URLAT.value;
	
	var WMSstr = document.wmsForm.SERVER.value;

	WMSstr = WMSstr + '?SRS=' + document.wmsForm.SRS.value;
	if (document.wmsForm.MAP.value !="") {			
		WMSstr = WMSstr + '&MAP=' + document.wmsForm.MAP.value;}
	WMSstr = WMSstr + '&BBOX=' + BBOX;
	WMSstr = WMSstr + '&LAYERS=' + document.wmsForm.LAYERS.value;
	WMSstr = WMSstr + '&STYLES=' + document.wmsForm.STYLES.value;
	WMSstr = WMSstr + '&WIDTH=' + document.wmsForm.WIDTH.value;
	WMSstr = WMSstr + '&HEIGHT=' + document.wmsForm.HEIGHT.value;
	//WMSstr = WMSstr + '&ZOOM=' + document.wmsForm.ZOOM.selectedValue;
	WMSstr = WMSstr + '&ZOOM=' + document.getElementById('ZOOM').value
	// WMSstr = WMSstr + '&FORMAT=' + document.wmsForm.FORMAT.value; //taken out here because it contains chars that need to be escaped
	WMSstr = WMSstr + '&VERSION=' + document.wmsForm.VERSION.value;
	WMSstr = WMSstr + '&SERVICE=WMS';
	WMSstr = WMSstr + '&REQUEST=GetMap';
<!--- 	var MapUrl = "<img id=mapimage src='http://" + WMSstr + "&FORMAT=" + document.wmsForm.FORMAT.value + "' border='0' height='" + document.wmsForm.SHEIGHT.value + "' onClick=getMouseXY(event);>"; --->
	
	var MapSrc = "http://" + WMSstr + "&FORMAT=" + document.wmsForm.FORMAT.value;
	
	//alert(MapUrl);
	if(document.wmsForm.showWMSrequest.checked) {
	logAppend(WMSstr);
	}
	
	//alert(MapSrc);
		document.getElementById('mapimage').src=MapSrc; 
	
<!--- 	document.getElementById('mapTD').innerHTML="loading";
	document.getElementById('mapTD').innerHTML=MapUrl; --->
	
}

function resetBBOX() {
	document.wmsForm.LLLON.value = origLLLON;
	document.wmsForm.LLLAT.value = origLLLAT;
	document.wmsForm.URLON.value = origURLON;
	document.wmsForm.URLAT.value = origURLAT;	
	document.wmsForm.sbbox.value = origLLLON + ',' + origLLLAT + ',' + origURLON + ',' + origURLAT;
}

// gets desired DD width from selectbox and updates BBOX
function updateBBOX(option_value) {
	var t_width = option_value/1000000;
	
	LonCenter = parseInt(document.wmsForm.LLLON.value) + ((document.wmsForm.URLON.value - document.wmsForm.LLLON.value)/2);
	LatCenter = parseInt(document.wmsForm.LLLAT.value) + ((document.wmsForm.URLAT.value - document.wmsForm.LLLAT.value)/2);
	
	//get corners from center as a function of selected
	var NLLLON = (LonCenter - t_width/2).toFixed(4);
	var NLLLAT = (LatCenter - t_width/2).toFixed(4);
	var NURLON = (LonCenter + t_width/2).toFixed(4);
	var NURLAT = (LatCenter + t_width/2).toFixed(4);
	
	// update form elements
	document.wmsForm.sbbox.value = NLLLON + ',' + NLLLAT + ',' + NURLON + ',' + NURLAT;
	document.wmsForm.LLLON.value = NLLLON;
	document.wmsForm.LLLAT.value = NLLLAT;
	document.wmsForm.URLON.value = NURLON;
	document.wmsForm.URLAT.value = NURLAT; 
}


function getBBOX() {
	alert(BBOX);
	document.location="jobs.cfm?bbox=" + BBOX;
}

</script>


<body onload=resetBBOX()>
<h2 style="font-size:24px; color:#666666">pTolemy3D Create JP2 Console</h2>


<div id="header">
    <ul id="primary">
        <li><a href="index.cfm">Home</a></li>
		<li><span>MAP</span> </li>
		<li><a onclick="getBBOX();">JOBS</a></li>
		<li><a href="run.cfm">RUN</a></li>
    </ul>
</div>
<div id="main">

<div id="contents">

<table border='1' cellspacing='0' cellpadding='0'>
<tr>
<td align="left" valign="top" id="mapTD">
<img id=mapimage src='./img/openaerialmap.org.jpg' border='1' height='256' width='256' onClick=getMouseXY(event);>
</td>

<td>
<table border='1' bgcolor='#C0C0C0' cellspacing='0' cellpadding='0'>
<form name="wmsForm" id="wmsForm">
<tr>
	<td>SERVER&nbsp;&nbsp;http://</td>
	<td colspan=3><input type="text" name="SERVER" value="openaerialmap.org/wms/" size="50"></td>
</tr>
<tr>
	<td>MAP</td>
	<td colspan=3>
		<input type="text" name="MAP" value="" size="50"></td></tr>
<tr>
	<td>SRS</td>
	<td><input type="text" name="SRS" value="EPSG:4326"></td>
	<td>FORMAT</td>
	<td><input type="text" name="FORMAT" value="image/jpeg"></td>
</tr>
<tr>
	<td>LAYERS</td>
	<td><input type="text" name="LAYERS" value="World"></td>
	<td>STYLES</td>
	<td><input type="text" name="STYLES" value="">
	</td>
</tr>
<tr>
	<td>WIDTH</td>
	<td><input type="text" name="WIDTH" value="512"></td>
	<td>HEIGHT</td>
	<td><input type="text" name="HEIGHT" value="512"></td>
</tr>
<tr>
	<td>VERSION</td>
	<td colspan=3><input type="text" name="VERSION" value="1.1.1"></td>
</tr>
<tr>
	<td>BBOX</td>
	<td colspan=3><input type="text" name="sbbox" size=30 value=""></td>
</tr>
<tr>
	<td colspan="4">
		<table border="1" bgcolor="#8080FF">
		<tr>
			<td align="right">upper right ---> </td>
			<td>
				<input type="text" name="URLON" value="90" size="10">&nbsp;
				<input type="text" name="URLAT" value="45" size="10"></td></tr>
		<tr>
			<td>
				<input type="text" name="LLLON" value="-90" size="10">&nbsp;
				<input type="text" name="LLLAT" value="-45" size="10"></td>
			<td><--- lower left</td>
		</tr>
		<tr>
			<td></td>
			<td align="right"><input type="button" name="button1" value="Reset BBOX" onClick='resetBBOX()';>
			</td></tr>
		<tr>
			<td>DD WIDTH 
		      <select id="DDWIDTH" name="DDWIDTH" onChange="updateBBOX(this.options[this.selectedIndex].value);"> 
		      <option value="52428800" > 52.4288
		      <option value="6553600" > 06.5536
		      <option value="819200" > 00.8192
		      <option value="102400" > 00.1024
		      <option value="12800" > 00.0128
		      <option value="1600" > 00.0016
		      </select>
			</td>
			<td align="right">xx</td></tr>
	</table>
</td>
</tr>
<tr>
	<td>on map click</td>
	<td colspan=3>
      <select id="ZOOM" name="ZOOM" size="1">
      <option value=".25" > Zoom in 4 times
      <option value=".5" > Zoom in 2 times
      <option value="1"  selected> Recenter Map
      <option value="2" > Zoom out 2 times
      <option value="4" > Zoom out 4 times
      </select>&nbsp;
      <input type="button" name="button1" value="update map" onClick="updateMap()">

	  &nbsp;&nbsp;	  Show WMS <input type="checkbox" name="showWMSrequest"><br>
	  show map at this height&nbsp; <input type="text" name="SHEIGHT" value="512">
	</td>
</tr>
</table>

</form>

</td></tr>

</table>
<span id="logSpan"></span>






