# How to create a plugin for pTolemy3D #

[[PageOutline(2-3,,inline)]]

---


Plugins are a mechanism useful to add new features to pTolemy3D. Most of the default features of pTolemy3D are implemented as plugins.
Plugins can be enabled or disabled at runtime, you can send or request information, attach only the desired plugins to the viewer (see [wiki:PtolemyPlugin#SettinguppluginsinthepTolemy3Dapplet How to setting up plugins]), etc.

## The Plugin interface ##
The main requirement to create a new plugin is to implement the Plugin interface, which extends the _Drawable interface. Below is the set of methods, and a brief explanation, you need to code when implement the Plugin interface:_

  * `public void init(Ptolemy3D ptolemy)`: This method is used to store a reference to Ptolemy3D object in a plugin attribute. Ptolemy3D is the core class with references to all other objects, like landscape, view, scene, ...
```
#!java
    /**
     * Initialize all non OpenGL datas. This is called just after the object instanciation.
     * OpenGL Context is NOT current here, initialize all OpenGL related datas in <code>initGL</code>.
     */
    public void init(Ptolemy3D ptolemy)
    {
    }
```

  * `public void setPluginIndex(int index)`: Used to store the plugin's position in the list of all configured plugins.
```
#!java
    /**
     * Notify the index of the plugin.
     */
    public void setPluginIndex(int index)
    {
    }
```

  * `public void setPluginParameters(String params)`: Parameters specified are pased to this method so that plugin can parse and store it.
```
#!java
    /**
     * Called a single time at initialisation.
     */
    public void setPluginParameters(String params)
    {
    }
```

  * `public void motionStop(GL gl)`:
```
#!java
    /**
     * Called when camera motion is stopped.
     */
    public void motionStop(GL gl)
    {
    }
```

  * `public boolean pick(double[] intersectPoint, double[][] ray)`:
```
#!java
    /**
     * Ray trace to find an intersection with the plugin geometry.
     */
    public boolean pick(double[] intersectPoint, double[][] ray)
    {
    }
```

  * `public boolean onPick(double[] intersectPoint)`:
```
#!java
    /**
     * Called when the landscape has been picked.
     * @param intersectPoint picking intersection point.
     */
    public boolean onPick(double[] intersectPoint)
    {
    }
```

  * `public void tileLoaderAction(Communicator com)`: This method can be used to start your own thread to load data.
```
#!java
    /**
     * Call by the tile loader to let the plugin request data.
     */
    public void tileLoaderAction(Communicator com) throws IOException
    {
    }
```

  * `public String pluginAction(String commandname, String command_params)`: Used to send commands to/get information from the plugin. Every plugin is responsible to parse and execute the commands.
```
#!java
    /**
     * Execute a plugin command.
     *
     * @param commandName command type
     * @param commandParams command parameters
     * @return if any value is returned, return it in a Stirng.
     */
    public String pluginAction(String commandname, String command_params)
    {
    }
```

  * `public void reloadData()`:
```
#!java
    /**
     * Called when landscape status has been changed. Plugin must reload some data due to this change.
     */
    public void reloadData()
    {
    }
```

  * `public void initGL(GL gl)`:
```
#!java
    /**
     * Initialize all OpenGL datas. OpenGL Context is current.
     */
    public void initGL(GL gl)
    {
    }
```

  * `public void draw(GL gl)`: Every plugins is responsible to store/restore PROJECTION and MODELVIEW matrices as well as OpenGL attributes that could be modified in the rendering process.
```
#!java
    /**
     * Render OpenGL geometry.
     */
    public void draw(GL gl)
    {
    }
```

  * `public void destroyGL(GL gl)`:
```
#!java
    /**
     * Destroy all OpenGL Related Datas. OpenGL Context is current.
     */
    public void destroyGL(GL gl)
    {
    }
```


## Plugin Responsibilities ##

A plugin could be coded to make what you need but there are a set of best practices you must follow to make a better plugins integration. Developers must talk a common language.

  * Every plugin is responsible to store its name. Normally it is stored in the form of '''class name in capital letters''' + '''underscore''' + '''index of the plugin'''. Supposing the AxisPlugin is the third configured plugin, its name will be '''`AXIS_PLUGIN_3`'''. The name of the plugin must be returned via `pluginAction` method using the `getLayerName` command.

  * A plugin is responsible to store its status (enable/disable) and renders or not depending on it. The status can be changed via `pluginAction` method using the `status` command.

Finally, document well all the actions/commands of your plugin in addition to all the initialization parameters it accepts.

## Example Plugin ##

[[Image(axisplugin.png)]]

This page has attached the code of a very simple plugin which shows the X,Y,Z cartesian axis into the globe.

Download and study it as a basic introduction to programming your own plugin.

As you can see, the AxisPlugin has two action/commands:
  * `status`: to enable/disable the plugin at runtime.
  * `getLayerName`: to get the plugin's name.

```
#!java
    public String pluginAction(String commandname, String command_params)
    {
        if (commandname.equalsIgnoreCase("status"))
        {
            status = (Integer.parseInt(command_params) == 1) ? true : false;
        }
        else if (commandname.equalsIgnoreCase("getLayerName"))
        {
            return NAME + index;
        }
        return null;
    }
```

In addition, we have prepared our plugin to accept one optional parameter when it is initialized.
```
#!java
    public void setPluginParameters(String params)
    {
        String p[] = params.split(",");
        if (p.length > 0)
        {
            showLabels = true;
        }
    }
```

The plugin parameter is optional and is used to show/hidde the axis names. Only if it is present and not empty (with any kind of value) the axis names will be shown.

## Adding the plugin to the viewer ##

Remember to see [wiki:PtolemySettingUpApplet How to setting up the pTolemy3D applet] and [wiki:PtolemyPlugin#SettinguppluginsinthepTolemy3Dapplet How to setting up plugins]).

Supposing you are compiled and packaged your plugin in a new JAR file you need to do two steps:

  * Add a reference to your JAR file in the applet tag.
```
#!text/html
        <applet code="com.sun.opengl.util.JOGLAppletLauncher"
                align="center" width="95%" height="65%"
                id="pTolemy3D" Name="pTolemy3D"
                codebase="./applet/"
                archive="./jogl.jar,./gluegen-rt.jar,./netscape.jar,./ptolemy3d.jar,./ptolemy3d-viewer.jar,./ptolemy3d-plugins.jar,./axisplugin.jar" MAYSCRIPT>
            <param name="subapplet.classname" VALUE="org.ptolemy3d.viewer.Ptolemy3DApplet">
            <param name="subapplet.displayname" VALUE="Ptolemy3D Applet">
            <param name="progressbar" value="true">
            <param name="noddraw.check" value="true">
            <param name="noddraw.check.silent" value="true">
            <param name="cache_archive" VALUE="jogl.jar,gluegen-rt.jar,netscape.jar,ptolemy3d.jar,ptolemy3d-viewer.jar,ptolemy3d-plugins.jar,axisplugin.jar">
            <param name="cache_archive_ex" VALUE="jogl.jar;preload,gluegen-rt.jar;preload,netscape.jar;preload,ptolemy3d.jar;preload,ptolemy3d-viewer.jar;preload,ptolemy3d-plugins.jar;preload,axisplugin.jar;preload">
        ...
        ...
        ...
        </applet>
```

  * Add a new pair of PluginType/PluginParam to your applet configuration.

Supossing it is the first plugin into your plugin list configuration, you can set it:
```
#!text/html
            ...
            ...
            <param name="NumPlugins" value="10">

            <param name="PluginType_1" value="org.ptolemy3d.plugin.AxisPlugin">
            <param name="PluginParam_1" value="1">
            ...
            ...
```

Remeber the parameter is optional in the AxisPlugin. If no prosent or no value is set then the axis labels will not be shown.


## Special considerations ##

Remember to be careful when implementing the `draw()` method. We encourage to make this steps for all your plugins:

  * store the current projection and modelview matrices,
  * store any OpenGL attribute you are going to change: line size, blend function, ...
  * draw your geometries, modifying the matrices or attribute,
  * restore the attributes,
  * restore the the matrices.