Vector Plugin

Description

displays polygon and line data on the map. Vector data is currently gotten through a tomcat servlet that queries data on a postgis database. In the future data might be qureid from Mapserver, using Mapservers GML output function. (WFS Version : Vector)

plugin\_parameters :

usage



&lt;param name="PluginType\_1" value="vector"&gt;




&lt;param name="PluginParam\_1" value="P -parameter\_name parameter\_value -parameter\_name parameter\_value ..."&gt;



parameters

  * -status
> > o [1|0]


  * -jspurl (Url to access Jsp)
> > o [String](String.md)


  * -layer (if querying a servlet, table name)
> > o [String](String.md)


  * -querywidth (extents of query, meters/degrees )
> > o [int](int.md)


  * -displayradius (extents of display in viewer)
> > o [int](int.md)


  * -minalt
> > o [int](int.md)


  * -maxalt
> > o [int](int.md)


  * -vectorraise (dist in meters to set line above ground)
> > o [int](int.md)


  * -stroke (rgb)
> > o [int(0-255)]:[int(0-255)]:[int(0-255)]


  * -linew (pixel width of line)
> > o [float](float.md)


  * -interpolation (length in meters to interpolate the vector to dem/tin data)
> > o [int](int.md)


pluginAction

command	parameters	description	returns

refresh	&nbsp; 	drops current data and requeries	&nbsp;

status	0,1 	0 sets off, 1 sets on	&nbsp;

getInfo	&nbsp;	vector, $STATUS, $QueryWidth, $DisplayRadius, $DISP\_MAXZ,$DISP\_MINZ,$vectorraise, $stroke(r:g:b), $LineWidth, $interp\_len	comma delim list of parameters

setInfo	STATUS, QueryWidth?, DisplayRadius?, DISP\_MAXZ, DISP\_MINZ,vectorraise, stroke(r:g:b),LineWidth?, interp\_len	sets parameters	&nbsp;


Notes

z ht calculation

z values are calculated on the fly using vertical line intersections with the on screen dem polygons. polygons are each given a centroid, processing is ordered by the centroids distance from the camera focal position. after height data is calculated a flag is set and the vector will not get reprocessed until all other vectors within the display radius have been processed. as terrain data is streamed in vector height data will no longer match. turning the layer status off and on will reset the flags.

It might be a good idea to set the calculated flag to off to objects that get moused over...

interpolation

using the interpolation value (iv), intermediate break points are added to the vector (iv) meters apart on each separate line in the object. if (iv) is greater than the length of the line nothing is done.