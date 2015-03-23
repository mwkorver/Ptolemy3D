# pTolemy3D Refactor 0.1.0 #

The idea is to have a more simplified and consisten API whit less coupled among classes.
Currently the Ptolemy class is the heart of all the code. A lot of classes have references to it and many times they are not needed, it is a bad design problem (don't flame me it is a constructive critique).

## Diagram Class ##

[[Image(ptolemyDC.png)]]

  * Unit: Contains a set of methods to work with unit system in ptolemy (for exmaple, convert from degree to DD\_Factor units).
  * Ptolemy3D: Is the central place, it mantains the JP2 load threads and has a reference to the Scene. There is only one scene. This class has or must have the methods to start and shutdown the system (like threads or other resources).
  * Ptolemy3DGLCanvas: is the heavyweigh component that renders the scene of pTolemy instance. The canvas is rendered through the Camera, CameraMovement and InputHandler classes. Once the camera has set its positio and perspective it call the Scene.draw() method.
  * Camera: Maintains the modelview and perspective matrix. Must have all method to convert from camera to world coordinates.
  * InputHandler: Listens for mouse of keyboard changes.
  * CameraMovement: Knows how to manipulate the camera object. Queries the InputHandler class to know the keyboar/mouse status.

'''Note: The realtion among Camera, CameraMovement and InputHandler must be improved. Also we need to refactor the CameraMovement API to create a consistent set of methods.'''

'''The main idea is to have one Scene that could be rendered with different Canvas. At this moment this is not right. We can only have one Scene and one Canvas. There is a great couple issue between Jp2TileLoader and Camera class, and we are obligated to have a reference beetwen Ptolemy3D and Ptolemy3DCanvas.'''

## Changes ##

  * Use of DrawContext instance to pass parameters among objects: Scene, Landscape, Sky, Plugins, etc uses it. There is no need to maintain references to GL object on ptolemy.
  * More methods have been placed in the Camera class, like isPointInView, so all plugins can benefit of a more consistent API.

## Other tasks ##
  * Ptolemy3D wouldn't have a reference to the Canvas, it mustn't know nothing about the Canvas. For the moment it is need because the Jp2TileLoader-Camera dependency.
