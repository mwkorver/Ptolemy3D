''' GML to Ptolemy3D\_POI'''

  * Note: there appears to be a related document [here](WfsPoiPlugin.md)

A request of the form : http://220.218.254.236/cgi-bin/mapserv?/&SERVICE=WFS&VERSION=1.0.0&REQUEST=getFeature&map=seattle_wfs.map&typename=poi

will simply return all the pois that the WFS is set up to return.


A request of the form : http://220.218.254.236/cgi-bin/mapserv?/&SERVICE=WFS&VERSION=1.0.0&REQUEST=getFeature&map=seattle_wfs.map&typename=poi&maxFeatures=20

will return a maximum of 20 pois that the WFS is set up to return.

Example of GML returned from WFS :

<wfs:FeatureCollection
> xmlns="http://www.ttt.org/myns"
> xmlns:myns="http://www.ttt.org/myns"
> xmlns:wfs="http://www.opengis.net/wfs"
> xmlns:gml="http://www.opengis.net/gml"
> xmlns:ogc="http://www.opengis.net/ogc"
> xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
> xsi:schemaLocation="http://www.opengis.net/wfs http://ogc.dmsolutions.ca/wfs/1.0.0/WFS-basic.xsd
> > http://www.ttt.org/myns http://220.218.254.236/cgi-bin/mapserv?map=seattle_wfs.map&amp;service=WFS&amp;SERVICE=WFS&amp;VERSION=1.0.0&amp;REQUEST=DescribeFeatureType&amp;TYPENAME=poi">
> > 

&lt;gml:boundedBy&gt;


> > > 

&lt;gml:Box srsName="EPSG:4326"&gt;


> > > 

&lt;gml:coordinates&gt;

-122.527880,47.184897 -121.955223,47.927340

&lt;/gml:coordinates&gt;


> > > 

&lt;/gml:Box&gt;



> > 

&lt;/gml:boundedBy&gt;


> > 

&lt;gml:featureMember&gt;


> > > 

&lt;poi&gt;


> > > > 

&lt;gml:boundedBy&gt;


> > > > > 

&lt;gml:Box srsName="EPSG:4326"&gt;


> > > > > 

&lt;gml:coordinates&gt;

-122.334200,47.584420 -122.334200,47.584420

&lt;/gml:coordinates&gt;


> > > > > 

&lt;/gml:Box&gt;



> > > > 

&lt;/gml:boundedBy&gt;


> > > > 

&lt;gml:pointProperty&gt;


> > > > > 

&lt;gml:Point srsName="EPSG:4326"&gt;


> > > > > 

&lt;gml:coordinates&gt;

-122.334200,47.584420

&lt;/gml:coordinates&gt;


> > > > > 

&lt;/gml:Point&gt;



> > > > 

&lt;/gml:pointProperty&gt;


> > > > 

&lt;oid&gt;

675717

&lt;/oid&gt;


> > > > 

&lt;id&gt;

1608

&lt;/id&gt;


> > > > 

&lt;name&gt;

Grand Central on the Park Bldg

&lt;/name&gt;


> > > > 

<group\_id>

2

</group\_id>


> > > > 

<user\_id>

1

</user\_id>



> > > 

&lt;/poi&gt;



> > 

&lt;/gml:featureMember&gt;


...


Unknown end tag for &lt;/FeatureCollection&gt;





The above GML is then transformed by an XSLT such as the following:


<?xml version='1.0' encoding='UTF-8'?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:gml="http://www.opengis.net/gml">


&lt;xsl:output indent="yes" method="xml"/&gt;




&lt;xsl:strip-space elements="\*" /&gt;





&lt;xsl:template match="text()"/&gt;





&lt;xsl:template match="/"&gt;




&lt;pois&gt;


<!--kanji: false= use texture font, true = use Graphics object-->


&lt;xsl:attribute name="kanji"&gt;

false

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="iconprefix"&gt;

/Ptolemy3DLib/icons/SeattleSymbol/icon\_1_&lt;/xsl:attribute&gt;


<!--Colours: Red:Green:Blue:Alpha If Alpha = 0, Colour is invisible-->_

&lt;xsl:attribute name="bgcolor"&gt;

100:100:250:150

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="fcolor"&gt;

255:255:255:255

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="bordercolor"&gt;

0:0:255:255

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="fontwidth"&gt;

1.3

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="fontheight"&gt;

1.8

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="position"&gt;

RM

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="maxalt"&gt;

50000

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="minalt"&gt;

0

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="fontFile"&gt;

/fonts/verdana36both.fnt

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="pinpoint"&gt;

true

&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="numicons"&gt;

32

&lt;/xsl:attribute&gt;




&lt;xsl:apply-templates select="\*"/&gt;




&lt;/pois&gt;




&lt;/xsl:template&gt;





&lt;xsl:template match="//featureMember/poi"&gt;




&lt;poi&gt;




&lt;xsl:attribute name="id"&gt;



&lt;xsl:value-of select="group\_id/text()"/&gt;



&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="label"&gt;



&lt;xsl:value-of select="name/text()"/&gt;



&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="obj\_id"&gt;



&lt;xsl:value-of select="id/text()"/&gt;



&lt;/xsl:attribute&gt;




&lt;xsl:call-template name="coordinates"&gt;




&lt;xsl:with-param name="coords" select="./pointProperty/Point/coordinates/text()"/&gt;




&lt;/xsl:call-template&gt;




&lt;/poi&gt;




&lt;/xsl:template&gt;






&lt;xsl:template name="coordinates"&gt;




&lt;xsl:param name="coords" select="coords" /&gt;




&lt;xsl:attribute name="lon"&gt;



&lt;xsl:value-of select="substring-before($coords,',')"/&gt;



&lt;/xsl:attribute&gt;




&lt;xsl:attribute name="lat"&gt;



&lt;xsl:value-of select="substring-after($coords,',')"/&gt;



&lt;/xsl:attribute&gt;




&lt;/xsl:template&gt;






Unknown end tag for &lt;/stylesheet&gt;




which generates XML in the form that Ptolemy3D? expects:


<?xml version="1.0" encoding="UTF-8"?>
<pois xmlns:gml="http://www.opengis.net/gml" kanji="false" iconprefix="/Ptolemy3DLib/icons/SeattleSymbol/icon\_1_" bgcolor="100:100:250:150" fcolor="255:255:255:255" bordercolor="0:0:255:255" fontwidth="1.3" fontheight="1.8" position="RM" maxalt="50000" minalt="0" fontFile="/fonts/verdana36both.fnt" pinpoint="true" numicons="32">_

<poi id="6" label="Internal Revenue Service" obj\_id="1659" lon="-122.334890" lat="47.604770"/>




<poi id="2" label="Financial Center" obj\_id="1592" lon="-122.334320" lat="47.607630"/>




<poi id="16" label="Madison Renaissance Hotel" obj\_id="1857" lon="-122.330690" lat="47.607020"/>




<poi id="16" label="Pacific Plaza" obj\_id="1756" lon="-122.333560" lat="47.606800"/>




<poi id="6" label="Federal Courthouse" obj\_id="858" lon="-122.331880" lat="47.606640"/>




<poi id="2" label="College Club Bldg" obj\_id="1542" lon="-122.331730" lat="47.606580"/>




<poi id="2" label="1001 4th Ave Plaza Bldg" obj\_id="1444" lon="-122.333360" lat="47.605870"/>




<poi id="3" label="Seattle City Light" obj\_id="1824" lon="-122.330140" lat="47.604760"/>




<poi id="2" label="First Interstate Center" obj\_id="1600" lon="-122.334000" lat="47.605610"/>




<poi id="2" label="Bank of California Center" obj\_id="1495" lon="-122.332270" lat="47.605390"/>




<poi id="16" label="Pacific Hotel" obj\_id="1751" lon="-122.332260" lat="47.605370"/>




<poi id="2" label="Bank of America Tower" obj\_id="1549" lon="-122.330450" lat="47.605070"/>




<poi id="6" label="Passport Office" obj\_id="1759" lon="-122.334580" lat="47.604560"/>




<poi id="6" label="Coast Guard 13th District" obj\_id="1537" lon="-122.334580" lat="47.604560"/>




<poi id="6" label="Federal Bldg" obj\_id="1586" lon="-122.334580" lat="47.604560"/>




<poi id="2" label="905 2nd Ave Bldg" obj\_id="1463" lon="-122.334480" lat="47.604450"/>




<poi id="4" label="Metro Transit Accessible Services" obj\_id="1709" lon="-122.334430" lat="47.604400"/>




<poi id="2" label="Exchange Bldg" obj\_id="1583" lon="-122.334430" lat="47.604400"/>




<poi id="2" label="Central Bldg" obj\_id="1530" lon="-122.332790" lat="47.604280"/>




<poi id="2" label="Marion Bldg" obj\_id="1696" lon="-122.333950" lat="47.603880"/>




Unknown end tag for &lt;/pois&gt;



Ptolemy3D? parses the above XML and displays the POIs in the viewer.