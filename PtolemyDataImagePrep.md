Note: For smaller areas at higher resolution and global levels with lower resolution, such as NASA's BlueMarble data, you can use ptconsole application [available here](PtolemyToolPtconsole.md).

'''The rest of this document refers to using commandline tools to generate larger amounts of data.'''


== Preparing Image Data for Ptolemy3D == (needs updating)


Step 1. deciding on a folder structure.

A typical Image Data Folder Structure might look like this. /www/z9 /www/z9/default.jp2 /www/z9/250 /www/z9/2000 /www/z9/16000 /www/z9/128000

here the root folder is /www/z9. underneath the root folder are folders with names that specify the tile width of the data inside of them. you should start with your very smallest layer width(250) and then multiply by 8 until you get the largest layer width you think you need. for very large areas its nice to have a large layer width tile set because then when Ptolemy3D starts up it will act as a general outline for the layers beneath it.

inside the folders we have jpeg 2000 files with image names that describe the upper left corner of the image. 09x-101000y106000.jp2 is an example of a typical Ptolemy3D tile image file. 09 is a reminder that this tile is JapanGSIZone9, and then we have the x and y coordinates.

Ptolemy3D needs information by Layer width, and upper left hand corner, so to get data, it simply does the following : getData(rootdirectory + "/" + layerwidth + "/" + prefix + "x" + xcoord + "y" + ycoord + ".jp2");

default.jp2 this file can be any one of the files from the data set. it is used to read general jp2 parameters for the data set.

Set up mapserver

once you have your folders decided, set up http://mapserver.gis.umn.edu. visit the site to get all the info, as of version 4.0 we can use gdal to display the images, allowing truecolor output of our source raster files.

you need to create a mapserver uri that takes query string parameters x,y,scale,w,h. I have created one called getTile that will also take a "mapfile" query string parameter. this uri will be used to get tiled data out of mapserver.

cut and encode images using MSToJp2

this program uses your mapserver script to encode tiles into jpeg files. when setting extents for MSToJp2 I usually use ArcExplorer??. Here using the scale bar function it's easy to find extents. be sure to set your minx and maxy to values that are divisible by the layer width, meaning coordinate/layerwidth = integer.

This part has changed to a automated mechanism

subdivide large datasets using setDivider

if you have a very large amount of tiles under a given folder you may want to subdivide them into folders.