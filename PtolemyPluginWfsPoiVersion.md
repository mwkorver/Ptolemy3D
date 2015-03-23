Instead of querying a database via a servlet to obtain the geographical features data we query an OGC Web Feature Service which serves the data in Geography Markup Language (GML) format. The GML is then transformed via an XSLT into an XML format that the pTolemy3D viewer understands. (GML to pTolemy3D\_POI GmlToViewerPoi)

usage



&lt;param name="PluginType\_1" value="poi"&gt;




&lt;param name="PluginParam\_1" value="P WFS -parameter\_name parameter\_value -parameter\_name parameter\_value ..."&gt;



Note the inclusion of WFS in the value attribute. This lets pTolemy3D know that it is dealing with a WFS plugin.

parameters

  * -status
> > o [1|0]


  * -stylesheet (location of the stylesheet w.r.t the applet)
> > o e.g. /xsl/poi.xsl


  * -typename (the typename to be included in the request to the WFS)
> > o e.g. poi


  * -wfsurl (the url of the wfs)
> > o e.g. http://220.218.254.236/cgi-bin/mapserv?/&SERVICE=WFS&VERSION=1.0.0&REQUEST=getFeature&map=seattle_wfs.map


  * -filter (used to filter data returned from the WFS)
> > o e.g 

&lt;PropertyIsEqualTo&gt;



&lt;PropertyName&gt;

user\_id

&lt;/PropertyName&gt;



&lt;Literal&gt;

1

&lt;/Literal&gt;



&lt;/PropertyIsEqualTo&gt;




  * -iconquerywidth (used to set up bounding box coordinates)
> > o [int](int.md)


  * -maxFeatures (limits the number of features returned by the WFS. Default is 20)
> > o [int](int.md)