Eclipse projects are located in :

src/Ptolemy3DCore/: .project and .classpath
src/Ptolemy3DExample/: .project and .classpath
src/Ptolemy3DPlugins/: .project and .classpath
src/Ptolemy3DViewer/: .project and .classpath

To work properly with your filesystem, you need to use the proper locations.
An utility is provided to do that automatically, just double click on MakeEclipseProject.jar.
(UnmakeProject.bat/sh will restore original configuration files).

Open eclipse and go to File->Import->'Existing Projects into Workspace'.
Open Ptolemy3D as the root directory and select all projects found.
Finally, click on 'Finish' button.
