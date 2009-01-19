<!---
 *  Copyright (C) 1996 - 2008 Mark Korver
 *
 *  This file is part of Ptolemy3D Project Jp2-From-WMS Tool.
 *  
 *  Jp2FromWMS is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  Free Software Foundation,version 3.
 *  
 *  Mail25 is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Jp2FromWMS.  If not, see http://www.gnu.org/licenses/
 *  
 *  http://www.ptolemy3d.org/
 
 This file is setup as a UDF and currently uses a command line encoder part.  This will change such that this 
 function will not have any file system reads and writes.
--->

<!--- receive png image object, convert to jp2 and return jp2 image --->
<!--- image-object, fname --->

<!--- this function needs to be improved such that it doesn't need to do any file system
writes --->

<cffunction name = "ImageToJp2" returnType = "binary" output = "no">
	<cfargument name="image" 
	   type="binary" 
	   required="Yes">
	<cfargument name="fname" 
	   type="string" 
	   required="Yes">
	<cfargument name="fname_ext" 
	   type="string" 
	   required="Yes">
	<cfargument name="image_args" 
	   type="string" 
	   required="Yes">
	 <cfargument name="timeOut" 
	   type="string" 
	   required="No"
	   default="30">
	   
<!--- base path for working dir --->
<cfset working_dir = "#ExpandPath(".")##slash#working"/>
<cfset working_in_dir = "#working_dir##slash#input" />
<cfset working_out_dir = "#working_dir##slash#output" />  


<!-- create working directories if they do not exist -->
    <cfif NOT DirectoryExists("#working_in_dir#")>
        <cfdirectory action="CREATE" directory="#working_in_dir#">
    </cfif>

    <cfif NOT DirectoryExists("#working_out_dir#")>
        <cfdirectory action="CREATE" directory="#working_out_dir#">
    </cfif>

<!-- timeOut is optional function param, but we need to set to get stdout data on cfexec tag -->
<cfparam name="timeOut" type="string" default="30">
	   
<!-- Writes the input image to working dir -->	   	
<cffile action="write" addnewline="false" output="#image#" file="#working_in_dir##slash##fname#.#fname_ext#">

<!-- java -classpath jj2000-5.1-mod.jar JJ2KEncoder -i 00x0y0.png -o test2.jp2 -tiles 1024 1024 -rate 0.5 -Wlev 3 -Aptype res -Alayers st -pph_tile on -debug
 -->	
<cfset encoder_args = " -i #working_in_dir##slash##fname#.#fname_ext# -o #working_out_dir##slash##fname#.jp2" />
<cfset encoder_args = encoder_args & " " & image_args>

<cfset bin_dir = "#ExpandPath(".")##slash#bin">
<cfset arguments = "-classpath #bin_dir##slash#jj2000-5.1-mod.jar JJ2KEncoder#encoder_args#">

	<!--- Having written the file to disk, we use encoder to convert it --->
	<cfexecute name="java" 
		variable="stdout" 
		errorvariable="errorout" 
		arguments="#arguments#" 						
		timeout="#timeOut#">
	</cfexecute>
	
	<!-- this need to be optimized using java -->
	<cffile action="APPEND" file="#working_dir##slash#error.log" 
					output="Error output: #errorout#" addnewline="Yes">

	
	<cffile action="READBINARY" file="#working_out_dir##slash##fname#.jp2" variable="jp2image"> 

	<cffile action="delete" file="#working_in_dir##slash##fname#.#fname_ext#">
	<cffile action="delete" file="#working_out_dir##slash##fname#.jp2">	
			 
	<cfreturn jp2image>
	
</cffunction>