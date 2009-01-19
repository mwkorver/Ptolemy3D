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

<!--- Include custom functions --->
<cfinclude template="./udf/udf.cfm">
<cfinclude template="./udf/convert.cfm">

<cfinclude template="./head.cfm">

<script language="JavaScript">

	function processQueue(tablename) {
		document.location="<cfoutput>#CGI.SCRIPT_NAME#</cfoutput>?action=run&tableName=" + tablename;
	}

</script>

<body>
<h2 style="font-size:24px; color:#666666">pTolemy3D Create JP2 Console</h2>

<div id="header">
    <ul id="primary">
        <li><a href="index.cfm">Home</a></li>
		<li><a href="map.cfm">MAP</a></li>
		<li><a href="jobs.cfm">JOBS</a></li>
		<li><span>RUN</span> </li>
		<li><a href="../view/index.cfm">3D</a></li>
    </ul>
</div>

<div id="main">
<div id="contents">

<cfif isdefined("session.wmsstring")>


<!--- if there is no url action value give it one --->
<cfparam name="url.action" default="none" type="string"/>

<cfswitch expression="#url.action#">

	<cfcase value="none">
		
		<BODY>	
		<cfoutput>#session.wmsstring#</cfoutput><br>
		
		This page runs jobs that have been placed in a Queue.<br>  Current Queues are listed below.  
		If no jobs are listed go back to the JOBS tab to make some.<br>
		<hr width=60% align=left>
		

		<cftry>    
		<cfquery datasource="jobqueue" name="getTableNames" username="test" password="test">
			SHOW tables 
		</cfquery>
		<cfcatch tpe="" type="Database">
			Database connecton error, go to 
			http://127.0.0.1:8080/bluedragon/administrator/
			go to the Datasources menu item on left.
			Follow directions at <a href="http://trac.ptolemy3d.org/wiki/PtolemyToolPtconsole">this wiki page </a>.
			<cfabort>
		</cfcatch> 
		</cftry>
		
		<table border="0" cellspacing="1" cellpadding="1">
		<tr>
			<td align="left">Queue Name</td><td>Items</td>
		</tr>
		
		<cfoutput query="getTableNames">
			<cfif left(TABLE_NAME,3) IS "JOB"><tr><td>#TABLE_NAME#</td>
				<cfquery datasource="jobqueue" name="getJOB_" username="test" password="test">
					SELECT count(id) as totalJobs FROM #table_name#
				</cfquery>
				<cfoutput query="getJOB_">
					<td>#totalJobs#</td>
				</cfoutput>	
					<td>
					<input type="button" value="Start" onClick=processQueue("#TABLE_NAME#"); /> 
					</td></tr>
			</cfif>
		</cfoutput>
		</table>  

	        
	</cfcase>


<!--- Checks Queue to see if any JOBs remaining, if so gets attributes, processes the deletes queue item from queue --->

	<cfcase value="run">
	
	<!--- Check to see if any JOBs remain in this particular queue --->
	
		<cfquery datasource="jobqueue" name="getRemainingJobs" username="test" password="test">
				SELECT count(id) as totalJobs FROM #url.tableName# WHERE PROCESSED='FALSE'
		</cfquery>
			
		<cfset NumberLeft = getRemainingJobs.totalJobs />
	
		<cfoutput>Number of job items remaining:  <font color="##FF0000">[#NumberLeft#]</font><BR></cfoutput>
		
		<!--- If JOB_ exist, go get one --->
		<cfif NumberLeft GT 0>
	
		<!--- get just one of those remainging JOB_ --->
			<cfquery datasource="jobqueue" username="test" password="test" name="getOneJob" maxrows="1">
				SELECT * FROM #url.tableName# where PROCESSED='FALSE'
			</cfquery>
			
		
			<cfset job = getOneJob.FILENAME />	
			<cfset jobID = getOneJob.ID />	
		
			<cfoutput>
			<script language="Javascript1.1" type="text/javascript">
				<!--
				function rld(){
					setTimeout('document.location.reload(true)',500);
				}
				//-->
			</script>
				
			<BODY onLoad="rld()"> 	
			<!-- <BODY>	 -->
		
			Current Job String: 
			#job#<br></cfoutput>
		
			<cfset fname = listlast(job,"/") />
			<cfset fname = listfirst(fname,".")/>
		    <cfset fpath = listfirst(job,"/") />			
	
			
			<!--- udf makes BBOX from jp2 path/name --->
			<cfset BBstring = makeBBOXString("#job#")/>			
			
			<!--- <cfset mapservUrl="http://openaerialmap.org/wms/?REQUEST=GetMap&SRS=EPSG:4326&LAYERS=World&STYLES=&WIDTH=1024&HEIGHT=1024&FORMAT=image/png&VERSION=1.1.1&SERVICE=WMS"/>
 --->
			<cfset mapservUrl="#session.wmsstring#"/>
							
			<cfset mapservUrl = mapservUrl & "&BBOX=#BBstring#"/>
			
			<cfoutput>
				WMS url:<br> #mapservUrl#<br>
			</cfoutput>
			
			<!--- Send query to wms to retrieve png file --->
			<cfset cfhttpcntr=0>
			<cfloop condition="cfhttpcntr LT 3">
				<cfhttp url="#mapservUrl#" 
						method="GET" 
						port="80" 
						resolveurl="false" 
						throwonerror="no" 
						getasbinary="yes"/>
						
				<cfoutput>WMS Retries: [#cfhttpcntr#]<BR>
				WMS Server response code: [#cfhttp.statuscode#]<BR></cfoutput>
				
				<cfif Find("200",cfhttp.statuscode) NEQ 0>
					<cfbreak>
				</cfif>
						
				<cfset cfhttpcntr=cfhttpcntr+1/>				
			
			</cfloop>
			
			<!--- if all the retries did not work reload the process, this could be endless here --->
				<cfif Find("200",cfhttp.statuscode) EQ 0>
					<cflocation
			  			url = "#CGI.SCRIPT_NAME#?action=badWMS&tablename=#url.tablename#"
			  			addToken = "Yes">  
				</cfif>

		
			<!--- setup JJ2000 params for png to jp2 conversion --->
			 <cfscript>			
				 cmd = ArrayNew(1);
				 cmd[1] = "-pph_tile";
				 cmd[2] = "on";
				 cmd[3] = "-rate";
				 cmd[4] = "0.8";
				 cmd[5] = "-Alayers";
				 cmd[6] = "0.9";
				 cmd[7] = "+1";
				 cmd[8] = "-Qtype";
				 cmd[9] = "reversible";
				 cmd[10] = "-Wlev";
				 cmd[11] = "3";				
				 conv_args = arraytolist(cmd, " ");
			 </cfscript>		
			
		<!--- 	<cfset conv_args="-tiles 1024 1024 -rate 0.5 -Wlev 3 -Aptype res -Alayers st -pph_tile on -debug" />
		 --->
			
			<!--- <cfset conv_args="-tiles 1024 1024 -pph_tile on -rate 0.8 -Alayers 0.9 +1 -Qtype reversible -Wlev 3" /> --->
			
			<cfif session.format EQ "image/jpeg">
				<cfset fname_ext="jpg">
			<cfelse>
				<cfset fname_ext="png">
			</cfif>
			
			<cfset timeOut="10"/>
			
			<cfset jp2_image = ImageToJp2(cfhttp.filecontent,fname,fname_ext,conv_args,timeOut)/> 
			
		
			<!-- write file here -->	
			<cfset outputpath = "#ExpandPath(".")##slash#working#slash#jp2"/>	
			<cfset outputpath = "#outputpath##slash##fpath#"/>	
			
			<cfif NOT DirectoryExists("#outputpath#")>
		    	<cfdirectory action="CREATE" directory="#outputpath#">
		    </cfif>
			
			<cffile addnewline="false" action="write" output="#jp2_image#" file="#outputpath##slash##fname#.jp2" nameconflict="OVERWRITE" />
			<cfoutput>Wrote to #outputpath##slash##fname#.jp2<br></cfoutput> 
 
			<!--- calculates world file information --->
			<cfset jpwstring = makeWorldfileString("#fpath#/#fname#.jp2","1024")/>			  		
	
			<cffile addnewline="false" action="write" output="#jpwstring#" file="#outputpath##slash##fname#.j2w" nameconflict="OVERWRITE" />
			
			<cfset aString = "true">
		
				<!--- if ws says success, delete job on queue --->
				<cfif aString EQ "true">
				
					<cfquery datasource="jobqueue" username="test" password="test" name="deletejob">
						DELETE FROM #url.tableName# where ID=#jobID#
					</cfquery>						
						
					<cfoutput>Completed Job deleted from Queue<br></cfoutput>
					
				</cfif>
		
			<cfoutput>
				<input type="button" value="STOP Execution" onClick="document.location.href='http://#CGI.SERVER_NAME#:#CGI.SERVER_PORT##CGI.SCRIPT_NAME#?action=stop&tableName=#url.tablename#'"/> 
			</cfoutput>			
			<cfelse>				
				<BODY>
				No Job Items left in Queue<br>					
			</cfif>
			
	</cfcase>

<!--- Halts processing of Queue --->
	<cfcase value="stop">
	    <cfoutput>
			Execution stopped by user</p>
		    <a href="#CGI.SCRIPT_NAME#?action=run&tablename=#url.tablename#">Continue Processing</a> [Queue #url.tablename#]<br>
	    </cfoutput>	
	</cfcase>

<!--- Halts processing when WMS requests not working --->
	<cfcase value="badWMS">
	    <cfoutput>
			Processing was stopped due to too many WMS errors</p>
		    <a href="#CGI.SCRIPT_NAME#?action=run&tablename=#url.tablename#">Try processing JOB_ Again</a><br>
	    </cfoutput>	
	
	</cfcase>
	
</cfswitch>

<cfelse>
This application uses session variables to you store information needed to query a WMS server and create jp2 files.<br>
You have either not used the MAP tab to define your WMS server and area of interest, or your session has expired and you<br>
need to start over from the MAP tab.
</cfif>


	</div>
</div>

</body></html>

</cfprocessingdirective>