## Debug version of Ptolemy3D Viewer ##

The debug version of the Ptolemy3D Viewer is made exactly the same than the usual ptolemy3d.jar,
with the few different that debug is enabled in the code:

```
org.ptolemy3d.deub.Config.DEBUG = true;
```

The debug version contains in addition :

**More textual output (console and screen).**

**More key binding for debug.**

**Debug rendering mode available.**



## Debug Screen Information ##


Press F3 to show/hide screen information.


Screen informations contains:

**Information about landscape rendering: number of tile, amount of vertex and vertex usage.**

**Information about the landscape rendering mode (same information as explained in next paragraph).**



## Debug Rendering Modes ##

'''Standard'''

Aerial view of the globe (earth).


'''Mesh'''

Globe geometry is rendered with wired polygon (quad/triangle).


'''Elevation'''

Image of the globe is replace by a shaded color meaning the altitude of the surface.


'''JP2'''

Show the jp2 wavelet level used by each tile rendered.

**Red: wavelet 0 (first jp2 level - less details)**

**Green: wavelet 1**

**Blue: wavelet 2**

**White: wavelet 3 (last jp2 level - most details)**


'''Tile Id'''

Each tile is rendered with a constant color.

We can deduce from the tile size from which layer it is.


'''Level Id (Layer Id)'''

Area with a contant color correspond to a layer.

**Full Red: layer 0**

**Full Green: layer 1**

**Full Blue: layer 2**

**Red: layer 3**

**Green: layer 4**

**Blue: layer 5**

**Light Red: layer 6**

**Light Green: layer 7**

**Light Blue: layer 8**

