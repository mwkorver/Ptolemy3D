<cfprocessingdirective suppresswhitespace="Yes">
<!--- 
*
* @param tilewidth     width and height of jp2 file. (Required)
* @param BBOX     Extents as list (llrlong,lllat, urlong, urlat. (Required)
* @return Returns List
* @author Mark Korver
* @version 1, 9/22/2008 --->

<!--- Include custom functions --->
<cfinclude template="./udf/udf.cfm">
<cfinclude template="./udf/convert.cfm">

<cfinclude template="./head.cfm">

<body>
<h2 style="font-size:24px; color:#666666">pTolemy3D Create JP2 Console</h2>

<div id="header">
    <ul id="primary">
        <li><a href="index.cfm">Home</a></li>
		<li><a href="map.cfm">MAP</a></li>
		<li><a href="jobs.cfm">JOBS</a></li>
		<li><span>RUN</span> </li>
    </ul>
</div>

<div id="main">
<div id="contents">


<!--- if there is no url action value give it one --->
<cfparam name="url.action" default="none" type="string"/>

<cfswitch expression="#url.action#">

	<cfcase value="none">
	
		<BODY>	
		This page runs jobs that have been placed in a Queue.<br>  Current Queues are listed below.  
		If no Jobs are available got back to the Jobs tab to make more.
		
		<table border="1" cellspacing="1" cellpadding="1">
		<tr>
			<td align="center">Queue Name</td><td>Items</td><td></td>
		</tr>
			    
		<cfquery datasource="jobqueue" name="getTableNames" username="test" password="test">
			SHOW tables 
		</cfquery>
		
		<cfoutput query="getTableNames">
			<cfif left(TABLE_NAME,3) IS "JOB"><tr><td>#TABLE_NAME#</td>
				<cfquery datasource="jobqueue" name="getJobs" username="test" password="test">
					SELECT count(id) as totalJobs FROM #table_name#
				</cfquery>
				<cfoutput query="getJobs">
					 <td>#totalJobs#</td>
				</cfoutput>	
					<td><A href="#CGI.SCRIPT_NAME#?action=run&tableName=#TABLE_NAME#">Process This Queue</A>
					</td></tr>
			</cfif>
		</cfoutput>
		</table>  

	        
	</cfcase>


<!--- Checks Queue to see if any jobs remaining, if so gets attributes, processes the deletes queue item from queue --->

	<cfcase value="run">
	
	<!--- Check to see if any jobs remain in this particular queue --->
	
		<cfquery datasource="jobqueue" name="getRemainingJobs" username="test" password="test">
				SELECT count(id) as totalJobs FROM #url.tableName# WHERE PROCESSED='FALSE'
		</cfquery>
			
		<cfset NumberLeft = getRemainingJobs.totalJobs />
	
		<cfoutput>Number of job items remaining:  <font color="##FF0000">[#NumberLeft#]</font><BR></cfoutput>
		
		<!--- If jobs exist, go get one --->
		<cfif NumberLeft GT 0>
	
		<!--- get just one of those remainging jobs --->
			<cfquery datasource="jobqueue" username="test" password="test" name="getOneJob" maxrows="1">
				SELECT * FROM #url.tableName# where PROCESSED='FALSE'
			</cfquery>
			
		
			<cfset job = getOneJob.FILENAME />	
			<cfset jobID = getOneJob.ID />	
		
			<cfoutput>
			<script language="Javascript1.1" type="text/javascript">
				<!--
				function rld(){
					setTimeout('document.location.reload(true)',3000);
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
			
			<cfset mapservUrl="http://openaerialmap.org/wms/?REQUEST=GetMap&SRS=EPSG:4326&LAYERS=World&STYLES=&WIDTH=512&HEIGHT=512&FORMAT=image/png&VERSION=1.1.1&SERVICE=WMS"/>
						
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
				#cfhttp.statuscode#<BR></cfoutput>
				
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

			<!--- base path for working dir --->
			<cfset working_dir = "#ExpandPath(".")##slash#working"/>
			
			<!--- setup params for png to jp2 conversion --->
			<cfset timeOut="10"/>
			<cfset image_args="-tiles 1024 1024 -rate 0.5 -Wlev 3 -Aptype res -Alayers st -pph_tile on -debug" />
			
			<cfset jp2_image = pngToJp2JJ2000(cfhttp.filecontent,fname,image_args,working_dir,timeOut)/> 
				
					
			<!-- write file here -->			
					
			<cfset froot = working_dir>
			<cfif NOT DirectoryExists("#froot##slash##fpath#")>
		    	<cfdirectory action="CREATE" directory="#froot##slash##fpath#">
		    </cfif>
			
			<cffile addnewline="false" action="write" output="#jp2_image#" file="#froot##slash##fpath##slash##fname#.jp2" />
			<cfoutput>Wrote to #froot##slash##fpath##slash##fname#.jp2<br></cfoutput> 
 
			<!--- calculates world file information --->
			<cfset jpwstring = makeWorldfileString("#fpath#/#fname#.jp2","1024")/>			  
			
	
			<cffile addnewline="false" action="write" output="#jpwstring#" file="#froot##slash##fpath##slash##fname#.jpw" />
			<cfoutput>Wrote to #froot##slash##fpath##slash##fname#.jpw<br></cfoutput> 
			
			<cfset aString = "true">
		
				<!--- if ws says success, delete job on queue --->
				<cfif aString EQ "true">
				
					<cfquery datasource="jobqueue" username="test" password="test" name="getOneJob" maxrows="1">
						DELETE FROM #url.tableName# where ID=#jobID#
					</cfquery>						
						
					<cfoutput>Job deleted from Queue<br></cfoutput>
					
				</cfif>
		
			<cfoutput>
				<a href="#CGI.SCRIPT_NAME#?action=stop&tablename=#url.tablename#">STOP Execution</a>
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
		    <a href="#CGI.SCRIPT_NAME#?action=run&tablename=#url.tablename#">Continue Processing Jobs</a><br>
	    </cfoutput>	
	</cfcase>

<!--- Halts processing when WMS requests not working --->
	<cfcase value="badWMS">
	    <cfoutput>
			Processing was stopped due to too many WMS errors</p>
		    <a href="#CGI.SCRIPT_NAME#?action=run&tablename=#url.tablename#">Try processing Jobs Again</a><br>
	    </cfoutput>	
	
	</cfcase>
	
</cfswitch>


	</div>
</div>

</body></html>

</cfprocessingdirective>