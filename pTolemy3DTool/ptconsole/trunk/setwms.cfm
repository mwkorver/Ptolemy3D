<cfif isdefined("form.wms_str")>
<cfset session.wmsstring="#form.wms_str#"> 
<cfset session.server="#form.server#"> 
<cfset session.map="#form.map#">
<cfset session.bbox="#form.bbox#">  
<cfset session.layers="#form.layers#"> 
<cfset session.format="#form.format#"> 
</cfif>
<cfif isdefined("session.wmsstring")>
<cfoutput>#session.wmsstring#</cfoutput>
</cfif>



