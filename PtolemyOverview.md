# pTolemy3D package description #

## pTolemy3D API package contains ##
  * bin: pTolemy3D signed jars.
  * doc: Documentation in the javadoc format.
  * example:  examples
  * lib: Third party libraries (JOGL ...)
  * src: pTolemy3D source code
  * README.TXT: This file
  * LICENSE.TXT: License file


---


### bin directory ###
This directory contains the signed jars of pTolemy3D API:
  * bin/ptolemy3d.jar: pTolemy3D Core signed jar.
  * bin/ptolemy3d-plugins.jar: pTolemy3D Plugins signed jar. This file is optional and contains some plugins (used by online demo).
  * bin/ptolemy3d-viewer.jar: pTolemy3D Viewer signed jar, including an application and applet version of the viewer (used for online demo).

### doc directory ###
This directory contains the javadoc documentation of the pTolemy3D API.

The starting point of the documentation is located in :
  * doc/Ptolemy3D-Index.html
  * http://www.ptolemy3d.org/javadocs/Ptolemy3D-Index.html

A small guide introduce, throught a tutorial, pTolemy3D Core API initialization:
  * doc/Ptolemy3D-Programming-HowToStart.htm
  * http://www.ptolemy3d.org/javadocs/Ptolemy3D-Programming-HowToStart.htm

### lib directory ###
Contains third party libraries used by the viewer for OpenGL (JOGL) and applet webpage integration (netscape.jar).

### src directory ###
Directory containing the java source for sources of the project.
  * src/Ptolemy3DCore: Sources for pTolemy3D core
  * src/Ptolemy3DPlugins: Sources for pTolemy3D plugins
  * src/Ptolemy3DViewer: Sources for pTolemy3D viewer demo
  * src/Ptolemy3DExample: Sources for pTolemy3D tutorials and PtolemyHowToStart page

### example directory ###
pTolemy3D examples 'ready to run'. An internet connection is required to get the aerial datas.
  * example/Ptolemy3D-Applet-Demo.html: Example of pTolemy3D Viewer in an applet.
  * example/Ptolemy3D-Application-Demo.bat: Example of pTolemy3D Viewer in an application.


---


## pTolemy3D Core ##
pTolemy3D engine contains all the technical code of the viewer.
It's aim is to download, manage and render geospatial data automatically.

## pTolemy3D Viewer ##
The pTolemy3D Viewer available in applet or application form.
The viewer initialize and start pTolemy3D Core to render geospatial data in its own components.
The viewer will used as example to show how easy it is to use and integrate pTolemy3D in every java application or applet.

## pTolemy3D Plugins ##
Plugins will be introduced in [wiki:PtolemyPlugin].