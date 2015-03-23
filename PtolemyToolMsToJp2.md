MSToJp2

description

this is a java program that can be pointed to a mapserver uri to generate jp2 files for pTolemy3D.

command

java -classpath ./js3dbatch.jar:lib/jj2000-4.1.jar:. com.aruke.batch.tool.MSToJp2 [options](options.md)

options

  * -minx [float8](float8.md)
  * -miny [float8](float8.md)
  * -maxx [float8](float8.md)
  * -maxy [float8](float8.md)


  * -tw [width float8](tile.md)
  * -units [METER|DD]


  * -prefix [char file prefix, ex. use japangsi zone (01 - 19) use 00 for DD tiles.](2.md)


  * -out [to save jp2](dir.md)


  * -ms\_host [host](mapserver.md)
  * -ms\_path [uri path (ex /cgi-bin/getTile?mapfile=the.map](mapserver.md)
  * -ms\_pxw [image pixel width (default is 1024)](mapserver.md)


  * -jp2\_rate [float8, default 0.8]
  * -jp2\_Alayers [float8, default 0.9]


  * -divider [int](int.md)

output

"-out (directory)" specifies where the jp2 files will be saved. the output is a jpeg2000 image file with a georeferenced name in the format (prefix)x(upper left x)y(upper right y).jp2

all images are 1024 pixels wide. width of the tile is defined by the directory structure. a tile 250 meters across would be in directory /path/to/jp2/base/250/(prefix)x(upper left x)y(upper right y).jp2


Tile width

The first logical step is to decide on the tile width for the jp2 tiles. this number specifies how many units across the tile will be. for example if i enter "-tw 250" and I intend to use a meter coordinate set this tile will be 250 meters across and down.

lat lon data is multiplied by 1000000 to keep the values as integers. so a tile width of 0.0016 would be in directory 1600.

Bounding Box

the minx,miny,maxx,maxy parameters define the area that will be covered. the program will automatically align the tiles so they are cut in respect to a x=0,y=0 tile. this causes the tiles to always be lined up correctly.

Mapserver

use the supplied perl script (getTile) that returns a 24bit png image for mapserver. the perl cgi program is called like :

"http://" + ms\_host + "/" + ms\_path + "&lon=" + ulx +"&lat=" + uly + "&scale=" + tw + "&w=" + ms\_pxw + "&h=" + ms\_pxw;

Jpeg 2000 options

rate and Alayers are variable. for complex images like arials, sat, data, the defaults work pretty well, and output a file of about 102k. if you are making a data set out of vector data you might want to raise the numbers though.