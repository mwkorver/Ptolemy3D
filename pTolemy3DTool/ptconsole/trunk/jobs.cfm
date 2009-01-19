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

<cfprocessingdirective suppresswhitespace="Yes">
<cfset MaxMessages = "50" />
<cfset MaxQueue = "500" />

<CFINCLUDE template="head.cfm">

<body>
<h2 style="font-size:24px; color:#666666">pTolemy3D Create JP2 Console</h2>

<div id="header">
    <ul id="primary">
        <li><a href="index.cfm">Home</a></li>
		<li><a href="map.cfm">MAP</a></li>
		<li><span>JOBS</span> </li>
		<li><a href="run.cfm">RUN</a></li>
		<li><a href="../view/index.cfm">3D</a></li>
    </ul>
</div>

<div id="main">
<div id="contents">
    <strong>Populate Embedded db Job Queue With Jp2 Processing Directives</strong><BR>
	
	Default action is to show list of jp2 path/filenames as a function of BBOX and tile width.<br> 
	Change radio button to "Add Jobs" to the Job Queue.<br>
	Then go to RUN tab to process the queue.<br><br>
	
	
	<form action="<cfoutput>#CGI.SCRIPT_NAME#</cfoutput>" method="post" name="sqsForm" id="sqsForm" >
		<cfparam name="form.tilewidth" type="string" default="52.428800" />
		<table width="60%" border="0" cellspacing="1" cellpadding="1">
		<tr>
		<td align="right" valign="middle">
			Jp2 Tile Width in DD:
		</td>
		<td valign="middle">
		<select name="tilewidth" id="tilewidth">
				<option <cfif form.tilewidth EQ "52.428800">SELECTED</cfif> value="52.428800">Level 52.428800</option>
				<option <cfif form.tilewidth EQ "6.553600">SELECTED</cfif> value="6.553600">Level 6.553600</option>
				<option <cfif form.tilewidth EQ ".819200">SELECTED</cfif> value=".819200">Level .819200</option>
				<option <cfif form.tilewidth EQ ".102400">SELECTED</cfif> value=".102400">Level .102400</option>
	            <option <cfif form.tilewidth EQ ".012800">SELECTED</cfif> value=".012800">Level .012800</option>
	            <option <cfif form.tilewidth EQ ".001600">SELECTED</cfif> value=".001600">Level .001600</option> 
        </select>
		to process
		</td>
		<td></td>
		</tr>
		<cfif isdefined("session.bbox")>
			<cfif session.bbox NEQ "">
				<cfparam name="form.BBOX" type="string" default="#session.bbox#">	
			<cfelse>
				<cfparam name="form.BBOX" type="string" default="-180,-90,180,90">
			</cfif>
		<cfelse>
			<cfparam name="form.BBOX" type="string" default="-180,-90,180,90">	
		</cfif>		
		<tr>
		<td align="right" valign="top">BBOX in DD:</td>
		<td valign="middle"><input type="text" name="BBOX" value="<cfoutput>#form.BBOX#</cfoutput>" /><br>
		 BBOX values can be changed via the MAP tab
		</td>
		
		<input type="hidden" name="action" value="listJobs"/>
		<td align="right" valign="top">		
		Add Jobs to db (limit <cfoutput>#MaxQueue#</cfoutput>):<br>
		List Jobs to Page (test mode limit 100):
		</td>
		<td>
		<input type="radio" name="radiobutton" value="createJobs"onClick=get_radio_value() /> <br>
		<input type="radio" name="radiobutton" value="listJobs" onClick=get_radio_value() checked /> 
		</td>
		</tr>
		<tr>
		<td>
		&nbsp;
		</td>
		<td>
		</td>
		<td align="right" colspan="2">
		<input id="submitbutton" type="submit" value="List Files to Create" >
		</td>
		</tr>				
		
		</table>
	</form>
	
	<script type="text/javascript">
<!--

function get_radio_value()
{
for (var i=0; i < document.sqsForm.radiobutton.length; i++)
   {
   if (document.sqsForm.radiobutton[i].checked)
      {
      document.sqsForm.action.value = document.sqsForm.radiobutton[i].value;
	  if (document.sqsForm.radiobutton[i].value =="createJobs") {
	  document.sqsForm.submitbutton.value = "Add Jobs to Job Queue"; }
	  else { 
	  document.sqsForm.submitbutton.value = "List Files to Create";
	  }	  
     }
   }
}

//-->
</script>



<cfparam name="action" default="none"/>
<cfswitch expression="#action#">

	<cfcase value="createJobs,listjobs">
	
<!--- Calculate min max longitude and latitude from BBOX --->
<!--- <cfparam name="lonMin" type="numeric" default=0 />
<cfparam name="latMin" type="numeric" default=0 />
<cfparam name="lonMax" type="numeric" default=0 />
<cfparam name="latMax" type="numeric" default=0 /> --->

<cfscript>
	lonMin = listgetat(bbox, 1,",");
	if (NOT (lonMin GTE -180 AND lonMin LTE 180)) {lonMin = 0;}
	latMin = listgetat(bbox, 2,",");
	lonMax = listgetat(bbox, 3,",");
	latMax = listgetat(bbox, 4,",");
	latMax = latMax + tilewidth;
</CFSCRIPT>

<!--- Calculate where to start from the East, jp2 tile name is upper left corner --->
   
  <cfset comp = lonMin/tilewidth>
  <cfset comp = Int(comp)>
  <cfset startx = comp * tilewidth />  

<!--- Calculate where to start from the South ---> 
    
  <cfset comp = latMin/tilewidth>
  <cfset comp = #Int(comp)#>
  <cfset starty = comp * tilewidth/>  
	<cfif starty LT -90>
		<cfset starty = starty + tilewidth>
	</cfif>


	<cfoutput>
		start lon/latitude: #numberformat(startx,'9.999999')#/#numberformat(starty,'9.999999')#  (jp2 tile name is upper left corner of tile)<hr>
	</cfoutput>



<cfset tileWidthX1M=#evaluate(form.tilewidth*1000000)#>
<cftry>
<cfif action EQ "createjobs">
	<cfquery datasource="jobqueue" name="inserttest" username="test" password="test">	
		DROP TABLE IF EXISTS JOBS#tileWidthX1M#;	
		CREATE TABLE JOBS#tileWidthX1M#(ID INT PRIMARY KEY, FILENAME VARCHAR(255), PROCESSED BOOLEAN);	
	</cfquery>
</cfif>
		<cfcatch tpe="" type="Database">
			Database connecton error, go to 
			http://127.0.0.1:8080/bluedragon/administrator/
			go to the Datasources menu item on left.
			Follow directions at <a href="http://trac.ptolemy3d.org/wiki/PtolemyToolPtconsole">this wiki page </a>.
			<cfabort>
		</cfcatch> 
</cftry>

<!--- Loop through from  ---> 	
<cfset counter=0 />
<cfset messagecounter=0 />

<cfloop index="a" from="#startx#" to="#lonmax#" step="#tilewidth#">
	<cfloop index="b" from="#starty#" to="#latmax#" step="#tilewidth#">
		
		<cfset message="#tileWidthX1M#/00x#numberformat(evaluate(a*1000000),'9')#y#numberformat(evaluate(b*1000000),'9')#.jp2">
		<cfset message=replace(message,"x-0y","x0y")>

		<!--- still getting spaces?? --->
		<cfset message=replace(message," ","")>

		<!-- if createjobs is true add to jobs db -->
		<cfif action EQ "createjobs">			
			<cfquery datasource="jobqueue" name="inserttest" username="test" password="test">
				INSERT INTO JOBS#tileWidthX1M# VALUES(#counter#,'#message#',FALSE);
			</cfquery> 
        </cfif>        
           <cfif counter LTE MaxMessages>
				<cfoutput>#message#<br></cfoutput>
                <cfset messagecounter = messagecounter + 1 /> 
	       </cfif>        
		<cfset counter = counter + 1 /> 
		
		<cfif counter GT MaxQueue><cfbreak></cfif>
        
	</cfloop>
	
	<cfif counter GT MaxQueue><cfbreak></cfif>
	
</cfloop>

		<cfif action EQ "createjobs">
				<cfoutput>#messagecounter# jobs listed<br>#counter# items pushed to job queue<br></cfoutput>
		<cfelse>
				<cfoutput>#messagecounter# jobs listed (not inserted into db)<br>#counter# items total<br></cfoutput>
		</cfif>	

	</cfcase>
</cfswitch>

</div>
</div>

</body></html>
</cfprocessingdirective>