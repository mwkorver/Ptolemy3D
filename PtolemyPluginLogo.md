Logo Plugin

Description

sets a logo to one of the 4 corners of the screen.

plugin\_parameters :

usage



&lt;param name="PluginType\_1" value="logo"&gt;




&lt;param name="PluginParam\_1" value="/logostore/logo.png,4,/logostore/logo2.png,2"&gt;



parameters

(logo url relative from Server parameter),(corner)

"number of logos is determined by number of comma delimited items divided by 2."


Notes

corner parameter
  1. -  upper left corner
> 2 -  upper right corner
> 3 -  lower left corner
> 4 -  lower right corner

logo image file must be of type png.

max size for a logo is a width of 256 and height of 32. anything bigger will be cut off. transparency of logo will be set from source image.