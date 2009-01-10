@echo off

jarsigner -keystore ptolemy -storepass ptolemy -keypass ptolemy ptolemy3d.jar ptolemy
jarsigner -keystore ptolemy -storepass ptolemy -keypass ptolemy ptolemy3d-viewer.jar ptolemy
jarsigner -keystore ptolemy -storepass ptolemy -keypass ptolemy ptolemy3d-plugins.jar ptolemy
jarsigner -keystore ptolemy -storepass ptolemy -keypass ptolemy debug/ptolemy3d.jar ptolemy

pause