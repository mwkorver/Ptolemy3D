



<cfset amazonDS=AmazonSimpleDB("testamz", "05WPM9ZQQHNGJQKWSKG2", "oxfjeq1JpZU2rW2+aPnebBRnZgrGtH5yQ0aMBIEO")>

<!--- <cfset CreateSDBDomain("testamz", "testDomain" )> --->


<!--- <cfdump var="#qry#"> --->



<cfset domainArray = ListSDBDomains("testamz")>

<cfdump var="#domainArray#"> 


<cfquery dbtype="amazon" datasource="#amazonDS#">
  insert into testDomain (ItemName, "name", "age") values (
  'MyUniqueName', 
  <cfqueryparam value="mark">, 
  <cfqueryparam value="100">)
</cfquery>

