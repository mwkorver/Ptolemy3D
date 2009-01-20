
<cfscript>
	
  // gets jp2 dd_level/filename.jp string, hands back a world file string 
  // fpath input should look like 12800/00x-87628800y41856000.jp2
  function makeWorldfileString(fpath,pixels) {
  	var retChar = chr(10);
  	var multiplier = 1000000;
    var path = listfirst(fpath, "/"); //12800
    var name = listlast(fpath, "/");  //00x-87628800y41856000.jp2
    var lon = listfirst(name, "y");   //00x-87628800
    lon = listlast(lon, "x");         //87628800
    var lat = listlast(name, "y");    //41856000.jp2
    lat = listfirst(lat, ".");        //41856000
    
    var pixelwidth = (path/multiplier)/pixels; //DD/pixel in horizontal x Easting direction 
        pixelwidth = numberformat(pixelwidth,'9.9999999999');
    
    lon = numberformat(lon/multiplier,'9.999999');
    lat = numberformat(lat/multiplier,'9.999999');
    
    var outputstrng = pixelwidth & retChar;
    outputstrng = outputstrng & "0.00" & retChar & "0.00" & retChar;
    outputstrng = outputstrng & "-" & pixelwidth & retChar;
    outputstrng = outputstrng & lon & retChar;
    outputstrng = outputstrng & lat & retChar;
    
    return outputstrng;
    
  }
  

  
  // gets jp2 dd_level/filename.jp2 string, hands back BBOX
  function makeBBOXString(fpath) {
  	var multiplier = 1000000;
    var ddWidth = listfirst(fpath, "/"); // 12800
    var name = listlast(fpath, "/");  // 00x-87628800y41856000.jp2
    var lllon = listfirst(name, "y"); // 00x-87628800
    lllon = listlast(lllon, "x");     // 87628800

    var urlat = listlast(name, "y");  // 41856000.jp2
    urlat = listfirst(urlat, ".");
    var lllat = urlat - ddWidth;
    var urlon = lllon + ddWidth;  
 
    lllon = numberformat(lllon/multiplier,'9.9999');
    lllat = numberformat(lllat/multiplier,'9.9999');
    urlon = numberformat(urlon/multiplier,'9.9999');
    urlat = numberformat(urlat/multiplier,'9.9999');
    
    var BBOX = lllon & "," & lllat & "," & urlon & "," & urlat;

    return BBOX;
    
  }
  
  
  
  function getFileSeparator()
{
    var fileObj = "";
    if (isDefined("application._fileSeparator"))
    {   
        return application._fileSeparator;
    }
    else
    {   
        fileObj = createObject("java", "java.io.File");
        application._fileSeparator = fileObj.separator;
        return getFileSeparator();
    }
}
  
  
  
  </cfscript>
  
  

   
  
  
  




