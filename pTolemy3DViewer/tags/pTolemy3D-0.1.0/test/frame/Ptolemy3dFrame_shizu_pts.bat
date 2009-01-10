@echo off

java -classpath gluegen-rt.jar;jogl.jar;ptolemy3d.jar;ptolemy3d-plugins.jar;ptolemy3d-viewer.jar org.ptolemy3d.viewer.Ptolemy3DFrame -xmlfile shizu_pts.xml -w 640 -h 480

pause