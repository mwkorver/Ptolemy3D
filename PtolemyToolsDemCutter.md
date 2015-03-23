# DemCutter #

## Description ##

DemCutter is a Java command line tool that generates DEM files ready for pTolemy3D.
Given an input directory with a great amounth of data (.hdr and .dat files) it generates .bdm or .tin file for the specified region.

You can find the source code at: [source:pTolemy3DTool/tools/DemCutter].

## How to use ##

''DemCutter [dir](input.md) [startx](startx.md) [starty](starty.md) [width](tile.md) [width](cell.md) [area](area.md) [dir](output.md) [factor](mult.md) [extension](hdr.md) [extension](dat.md) [isBinary](isBinary.md) [outputType](outputType.md) [sig](inside.md) [sig](wall.md)''

  * input dir: directory where DEM and header files resides.
  * startx: minimum X in DD units (degree + 1000000). See wiki page about [wiki:PtolemyDataTileSystem pTolemy Tile System]. startx/starty defines de left-top point of the selection.
  * starty: maximum Y in DD units (degree + 1000000). See wiki page about [wiki:PtolemyDataTileSystem pTolemy Tile System]. startx/starty defines de left-top point of the selection.
  * tile width: size specified in DD units (degree + 1000000). Usually it will corresponds to a tile level. See wiki page about pTolemy Tile System.
  * cell width: size specified in DD units (degree + 1000000). Usually it will corresponds to a tile level below the 'tile width' level. See wiki page about pTolemy Tile System.
  * area: 2 char file prefix, ex. use japangsi zone (01 - 19) use 00 for DD tiles.
  * output dir: the output direcotry where to write the generated files.
  * mult factor: usually this factor will be always 1000000.
  * hdr extension: a string containing the extension for header files that contains ascii header GRIDFLOAT information.
  * dat extension: a string containing the extension for data files with the ESRI GRIDFLOAT Binary Grid information. Used named as **.dat.
  * isBinary: 1-means binary, 0-means not binary. Specifies if the input files are in binay format.
  * outputType: 1-means TIN, 0-means BDM. Specifies the output file type.
  * inside sig: optional and only applied for TIN output files. Meters value to trim data on inside of dem.
  * wall sig: optional and only applied for TIN output files. Meters value to trim data on edges of dem.**

Example usage:[[BR](BR.md)]
''-> DemCutter ./indata/ -102000000 41000000 102400 12800 00 ./outdata/ 1000000 .hdr .dat 1 0''

It is impotant to note an execution of DemCutter only generates one file based on:[[BR](BR.md)]
''filename = area + "x" + minX + "y" + maxY + outputType''.

'''IMPORTANT:''' The first time DemCutter is executed using an input directory, it scans for all header files and created an index header file named "indexheaders.idx". Next executions looks for that index file and if found it is used to get header information. '''''If you add/remove files from the input directory be sure to remove, if exists, the index header file so DemCutter created a new fresh one.'''''