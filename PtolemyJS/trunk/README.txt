PtolemyJS
---------
With the arrive of HTML5 and its improvements and the implementation of WebGL by
most modern browsers it is clear the need for a real 3D web viewer, simply based
on standards without the need to install browser plugins.

PtolemyJS is a Virtual Globe framework implemented in JavaScript and WebGL,
so it is a 100% pure 3D web viewer.


Code
----

* Document source code with jsdoc-toolkit (http://code.google.com/p/jsdoc-toolkit/).

* All classes must be within the namespace 'Ptolemy'.

* Classes are implemented through prototype. Probably, it could be better to implement some function
  that helps in the creation of 'classes', similarly to OpenLayers.Class code.
* Classes relates to geometry (Vector4, Matrix, Quaternion) must be "immutables", that is, operations
  mustn't alter its values. Believe me, this way is much more easy when programming.
  For this purpose they are coded so operations returns a new instance with the result instead 
  modify it. For example: v1.add(v2) will return a new Vector4 instance with the result 'v1+v2' instead 
  modify the 'v1' instance.
  
* On classes related to geometry try always (if it is possible) to implement the methods: 
  equals, clone, toArray, toString.

* Static class methods and also static class constants are coded as functions. So, if you need to
  use a constant simply invoke the function, for example: Ptolemy.Vector4.UNIT_X().


