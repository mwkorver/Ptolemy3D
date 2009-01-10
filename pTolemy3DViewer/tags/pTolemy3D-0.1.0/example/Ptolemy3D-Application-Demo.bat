@echo off

set OPENGL_API=../lib/gluegen-rt.jar;../lib/jogl.jar
set PTOLEMY3D_API=../bin/ptolemy3d.jar;../bin/ptolemy3d-plugins.jar;../bin/ptolemy3d-viewer.jar

java -Djava.library.path=../lib/lib/ -classpath %OPENGL_API%;%PTOLEMY3D_API% org.ptolemy3d.viewer.Ptolemy3DFrame -xmlfile demo.xml -w 640 -h 480

pause