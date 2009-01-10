
This is the Ptolemy3D Viewer API package.

For more information about Ptolemy3D Viewer Project, please visit the Ptolemy3D wiki: 

http://www.ptolemy3d.org/

http://trac.ptolemy3d.org/wiki/


PACKAGE CONTENTS
================
bin          Ptolemy3D signed jars
doc          Documentation in the javadoc format
example      Examples
lib          Third party libraries
build        Configuration project for Ant, Netbeans and Eclipse
src          Source directory
README.TXT   This file
LICENSE.TXT  License file


bin
===
This directory contains the signed jars for Ptolemy3D Viewer API:

bin/ptolemy3d.jar          Ptolemy3D Viewer core: requiered.
bin/ptolemy3d-plugins.jar  Ptolemy3D Viewer plugins: optional visualization plugins.
bin/ptolemy3d-viewer.jar   Ptolemy3D Viewer application and applet demo (applet used for online demo).


doc
===
This directory contains the javadoc documentation of the Ptolemy3D Viewer.

The starting point of the documentation is located in :

doc/Ptolemy3D-Index.html

A small introduction guide introduce, throught a tutorial, Ptolemy3D Core API:

doc/Ptolemy3D-Programming-HowToStart.htm


lib
===
Contains third party libraries used by the viewer for OpenGL (JOGL) and applet webpage integration (netscape.jar).


src
===
Directory for source of the binaries and examples.

src/Ptolemy3DCore      Sources for Ptolemy3D core
src/Ptolemy3DPlugins   Sources for Ptolemy3D plugins
src/Ptolemy3DViewer    Sources for Ptolemy3D viewer demo
src/Ptolemy3DExample   Sources for Ptolemy3D tutorials


project
=======
Directory for project and build script.
For more information, please go to the appropriate directory :

project/netbeans/
project/eclipse/


example
=======
Ptolemy3D example 'ready to use'. An internet connection is requiered to get the aerial datas.

example/Ptolemy3D-Applet-Demo.html       Example of Ptolemy3D Viewer in an applet.
example/Ptolemy3D-Application-Demo.bat   Example of Ptolemy3D Viewer in an application.
