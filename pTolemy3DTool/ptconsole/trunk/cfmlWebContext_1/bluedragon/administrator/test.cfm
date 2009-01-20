
<cfset kdkd = createObject("component","bluedragon.adminapi.administrator").login("admin")>
<cfset ws = createobject("component","bluedragon.adminapi.datasource")>
<cfset tt = ws.isUserLoggedIn()> 
<cfdump var=#tt#> 
<!--- 

<cfscript>
   // Login is always required. This example uses a single line of code.
   createObject("component","bluedragon.adminapi.administrator").login("admin");

   // Instantiate the data source object.
   myObj = createObject("component","bluedragon.adminapi.datasource");

   // Required arguments for a data source.
   stDSN = structNew();
   stDSN.driver = "MSSQLServer";
   stDSN.name="northwind_MSSQL";
   stDSN.host = "10.1.147.73";
   stDSN.port = "1433";
   stDSN.database = "northwind";
   stDSN.username = "sa";

   // Optional and advanced arguments.
   stDSN.login_timeout = "29";
   stDSN.timeout = "23";
   stDSN.interval = 6;
   stDSN.buffer = "64000";
   stDSN.blob_buffer = "64000";
   stDSN.setStringParameterAsUnicode = "false";
   stDSN.description = "Northwind SQL Server";
   stDSN.pooling = true;
   stDSN.maxpooledstatements = 999;
   stDSN.enableMaxConnections = "true";
   stDSN.maxConnections = "299";
   stDSN.enable_clob = true;
   stDSN.enable_blob = true;
   stDSN.disable = false;
   stDSN.storedProc = true;
   stDSN.alter = false;
   stDSN.grant = true;
   stDSN.select = true;
   stDSN.update = true;
   stDSN.create = true;
   stDSN.delete = true;
   stDSN.drop = false;
   stDSN.revoke = false;

   //Create a DSN.
   myObj.setMSSQL(argumentCollection=stDSN);
</cfscript>


<cfoutput>
<cfdump var="#stDSN#">
</cfoutput>
--->