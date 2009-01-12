function getMouseXY(e) // works on IE6,FF,Moz,Opera7
{ 
  if (!e) e = window.event; // works on IE, but not NS (we rely on NS passing us the event)

  if (e)
  { 
    if (e.pageX || e.pageY)
    { // this doesn't work on IE6 (works on FF,Moz,Opera7)
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
	document.wmsForm.SBBOX.value = BBOX;
	
	// now that we have the newly calculated BBOX we can update the map
	updateMap();      
}

function updateMap() {

	BBOX = document.wmsForm.LLLON.value + ',' + document.wmsForm.LLLAT.value + ',' + document.wmsForm.URLON.value + ',' + document.wmsForm.URLAT.value;
	
	var WMSstr = document.wmsForm.SERVER.value;

	WMSstr = WMSstr + '?SRS=' + document.wmsForm.SRS.value;
	if (document.wmsForm.MAP.value !="") {			
	WMSstr = WMSstr + '&MAP=' + document.wmsForm.MAP.value;}
	WMSstr = WMSstr + '&LAYERS=' + document.wmsForm.LAYERS.value;
	WMSstr = WMSstr + '&STYLES=' + document.wmsForm.STYLES.value;
	WMSstr = WMSstr + '&WIDTH=' + document.wmsForm.WIDTH.value;
	WMSstr = WMSstr + '&HEIGHT=' + document.wmsForm.WIDTH.value;  //always square
	//WMSstr = WMSstr + '&ZOOM=' + document.getElementById('ZOOM').value
	WMSstr = WMSstr + '&VERSION=' + document.wmsForm.VERSION.value;
	WMSstr = WMSstr + '&SERVICE=WMS';
	WMSstr = WMSstr + '&REQUEST=GetMap';
	
	
	for (var i=0; i < document.wmsForm.FORMAT.length; i++)
	   {
	   if (document.wmsForm.FORMAT[i].checked)
	      {
	      var format_val = document.wmsForm.FORMAT[i].value;
	      }
	   }
	
// this is place here because the 24 bit png value has special character in it,doesn't work in above code 
	var MapSrc = "http://" + WMSstr + "&FORMAT=" + format_val;
	
	// update session variable holding wms string without the BBOX cuz that changes per request 
	$.post("setwms.cfm", {
	wms_str: MapSrc,
	server: document.wmsForm.SERVER.value,
	map: document.wmsForm.MAP.value,
	bbox: document.wmsForm.SBBOX.value,
	layers: document.wmsForm.LAYERS.value,
	format: format_val
	 } );

	  
	MapSrc = MapSrc + '&BBOX=' + BBOX;;
	
	//alert(MapSrc);
	if(document.wmsForm.showWMSrequest.checked) {
		logAppend(MapSrc);
	}

	//alert(MapSrc);
	document.getElementById('mapimage').height=document.wmsForm.SHEIGHT.value;
	document.getElementById('mapimage').width=document.wmsForm.SHEIGHT.value;
	document.getElementById('mapimage').src=MapSrc; 
	
}

function resetBBOX() {
	document.wmsForm.LLLON.value = origLLLON;
	document.wmsForm.LLLAT.value = origLLLAT;
	document.wmsForm.URLON.value = origURLON;
	document.wmsForm.URLAT.value = origURLAT;	
	document.wmsForm.SBBOX.value = origLLLON + ',' + origLLLAT + ',' + origURLON + ',' + origURLAT;
}

// gets desired DD width from selectbox and updates BBOX
function updateBBOX(option_value) {
	if (option_value != "select") {
		var t_width = option_value/1000000;
		
		LonCenter = parseInt(document.wmsForm.LLLON.value) + ((document.wmsForm.URLON.value - document.wmsForm.LLLON.value)/2);
		LatCenter = parseInt(document.wmsForm.LLLAT.value) + ((document.wmsForm.URLAT.value - document.wmsForm.LLLAT.value)/2);
		
		//get corners from center as a function of selected
		var NLLLON = (LonCenter - t_width/2).toFixed(4);
		var NLLLAT = (LatCenter - t_width/2).toFixed(4);
		var NURLON = (LonCenter + t_width/2).toFixed(4);
		var NURLAT = (LatCenter + t_width/2).toFixed(4);
		
		// update form elements
		document.wmsForm.SBBOX.value = NLLLON + ',' + NLLLAT + ',' + NURLON + ',' + NURLAT;
		document.wmsForm.LLLON.value = NLLLON;
		document.wmsForm.LLLAT.value = NLLLAT;
		document.wmsForm.URLON.value = NURLON;
		document.wmsForm.URLAT.value = NURLAT; 
	}
}


function logAppend(i,mode) {
	if (mode=="keep") {
	var existingLog = document.getElementById('logSpan').innerHTML
	var i = i + '<br>' + existingLog;
	}
	document.getElementById('logSpan').innerHTML=i;
}

function getBBOX() {
	document.location="jobs.cfm?bbox=" + BBOX;
}

function wmsRadio(wms_str,layer) {
	document.wmsForm.SERVER.value = wms_str;
	document.wmsForm.LAYERS.value = layer;
}


