<!--- 3 hour timeout --->
<cfapplication name="ptconsole" sessionTimeout=#CreateTimeSpan(0, 3, 0, 0)# sessionManagement="Yes">
        
<cfif server.os.name contains "Windows">
	<cfset slash = "\">
<cfelse>
	<cfset slash = "/">
</cfif>

