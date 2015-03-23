# pTolemy3D Plugins #

Much of the functionalities of pTolemy3D are implemented as, so called, ''plugins''. These way, new features or 3rd party extensions can be added implementing them as plugins, simplifying plugins the extension process.

```
#!div style="border: 2pt dashed darkgrey; margin: 5px; padding: 5px; background-color: #EEEEEE;"
See [wiki:PtolemyPluginCreate How to create a plugin] to know how you can add new features to pTolemy3D.
```

  * [wiki:PtolemyPluginVector]
  * [wiki:BuildingPlugin]
  * [wiki:GisToolPlugin]
  * [wiki:LogoPlugin]
  * [wiki:PoiPlugin]
  * [wiki:RasterPlugin]
  * [wiki:PtolemyPluginXvrml]


## Setting up plugins in the pTolemy3D applet ##

If you use pTolemy3D as an applet, you can set the initial available plugins using the applet's tag `param`.

First of all you need to specify the total number of plugins to want to load through `NumPlugins`. Later, for each plugin you must specify using `PluginType` the class that implements it:

```
#!text/html
                    ...
                    <param name="NumPlugins" value="NUMBER_OF_PLUGINS">
                    
                    <param name="PluginType_1" value="PLUGIN_CLASS_IMPLEMENTATION">
                    <param name="PluginParam_1" value="SOME_VALUE">
                    
                    ...
                    ...

                    <param name="PluginType_N" value="PLUGIN_CLASS_IMPLEMENTATION">
                    <param name="PluginParam_N" value="SOME_VALUE">
                    ...
```

As you can see, to each plugin you can pass any number of parameters using `PluginParam`.

All parameters that uses indexes must start with 1.




