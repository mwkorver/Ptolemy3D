# Load data in your plugins #

Once you now the [wiki:PtolemyPluginCreate basics to create a plugin], here we explain how you can load data in your plugins.

For convention and improvement, data can't be loaded in the rendering loop. This way if you load a big file the whole viewer will remain freeze until all data be read.
The `Plugin` interface has some methods that can be used to start a new thread responsible to load your data, like: `tileLoaderAction`, `reloadData` or `initGL`.

Here we present the `IconPlugin` which can draw an icon at some lat/lon position.


---

[[PageOutline(2-3,,inline)]]

---


## How to use the IconPlugin ##

The `IconPlugin` requires three parameters:
  * Path to the image file, respect the server URL you have specified in the `server` applet parameter.
  * The position specified as: `latitude, longitude`.

The next is an example suppousing it is the first plugin in your plugin list:
```
#!text/html
    ...
    <PluginType_1>org.ptolemy3d.plugin.IconPlugin</PluginType_1>
    <PluginParam_1>/some_dir/some_image.png,lat,lon</PluginParam_1>
    ...
```

## Loading the icon image ##

The code for `IconPlugin` is attached to this page, please take a look before continue reading.

The important thing here is show you how load data without freeze the application. For that purpose, we are using three flags:
```
#!java
    private boolean iconLoaded = false;
    private boolean errorLoading = false;
    private boolean initTexture = false;
```

This flags serves us to:
  * iconLoaded: true if the icon was successfully loaded,
  * errorLoading: true if any error occurs loading the icon image, and
  * initTexture: true if the texture object was initialized with the icon image.

In the `IconPlugin` we have selected the `initGL()` method to create a new thread where to load the data. Then, when the OpenGL context is initialized the plugin is notified and can create a new thread responsible to load the icon image from the server. Once the data is loaded the `iconLoaded` flag is set to true, otherwise, if any error occurs the `errorLoading` is set to true.

```
#!java
public void initGL(GL gl)
    {
        if (!iconLoaded)
        {

            Thread newThread = new Thread(new Runnable()
            {

                public void run()
                {
                    URL url;
                    try
                    {
                        String server = ptolemy.configuration.server;
                        url = new URL("http://" + server + imageName);
                        bufferedImage = ImageIO.read(url);
                    }
                    catch (IOException ex)
                    {
                        Logger.getLogger(IconPlugin.class.getName()).log(Level.SEVERE, null, ex);
                        errorLoading = true;
                    }
                    finally
                    {
                        iconLoaded = true;
                    }
                }
            });

            newThread.start();
        }
    }
```

## Drawing the icon image ##

Now, take a look at `draw()` method. This method is called inside the rendering loop. We can't make any expensive-in-time action. The load process, which is expensive, is made in the `initGL()` method. Here we:
  * check the lat/lon point are going to draw is in the visible side of the globe,
  * check the icon is loaded or no error occurs, and finally
  * check the texture object was initialized, if not, then initialize it and set the flag { {{initTexture}}} to true.

```
#!java
    public void draw(GL gl)
    {
        // Check if our point is in the visible side of the globe.
        if (!isPointInView(longitude, latitude))
        {
            return;
        }

        // If the icon image is not loaded return or there was an error loading
        // it then returns.
        if (!iconLoaded || errorLoading)
        {
            return;
        }

        // Initiallize the texture
        if (!initTexture)
        {
            initTexture = true;
            texture = TextureIO.newTexture(bufferedImage, false);
        }

        ...
        ...
    }
```

[[Image(iconplugin.jpg)]]

## Special considerations ##

Remember to be careful when implementing the `draw()` method. We encourage to make this steps for all your plugins:

  * store the current projection and modelview matrices,
  * store any OpenGL attribute you are going to change: line size, blend function, ...
  * draw your geometries, modifying the matrices or attribute,
  * restore the attributes,
  * restore the the matrices.