[[PageOutline](PageOutline.md)]
# I want to build a virtual globe !!! #


---


## What is a virtual globe? ##

I think a short introduction would be nice, about 3D, GIS and, of course, try to define the concept of [virtual globe](http://en.wikipedia.org/wiki/Virtual_globe).


---


## Technology Used ##

Java, OpenGL (JOGL) and mathematics.


---


## Start ##

On its basis our virtual globe must be a kind of sphere (a [geoid](http://en.wikipedia.org/wiki/Geoid)) with different elevations on its surface and images applied on it. The globe can be viewed with different [LOD](http://en.wikipedia.org/wiki/Level_of_detail), from view the whole globe to see streets on with a few meters altitude.[[BR](BR.md)]
Thinking only in the images needed to show the whole globe in all possible LODs make we understand the amount of data needed and, of course, it is impossible to store all it in memory.

There are different approaches to solve this but a common one is to use a pyramid of tiles:[[BR](BR.md)]
[[Image(http://www.geckil.com/~harvest/www6/Technical/Paper130/pyramid.jpeg,450)]]

Every level is composed by a number of tiles. Previous levels are composed of a less number of tiles than next level. Also below levels has great detail than above one.


### DEM format ###

[[Image(http://www.vterrain.org/Elevation/1-degree%20DEM.JPG)]]





