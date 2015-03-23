## Set Divider ##



'''Description:'''

moves jp2/dem files into subfolders, grouped by x,y coordinates. this is handy when file counts get huge.

'''Command:'''

java -classpath ./ptolemy3dbatch.jar:lib/jj2000-4.1.jar:. com.aruke.batch.tool.setDivider [to data directory](path.md) [divider](divider.md) [extension](file.md)

to reverse the file move append an 'undo' parameter to the above command

divider structure description

  * root
> > o folder scale
> > > + D(divider)
        1. x(coord)y(coord)
          * files


Real example

  * iowa

> > o 1600
> > > + D100000
        1. x1322y340
          * 00x132200000y34022400.jp2
          * 00x132200000y34028800.jp2
        1. x1322y341
          * 00x132200000y34108800.jp2


program process

takes the divider parameter and divides the x and y coord to make a folder name and move the file to that folder.


applet coordination usage

if files are put in this structure the applet tag for the divided layer must be set according to the pTolemy3DEmbed.



&lt;param name="LayerDIVIDER\_(n)" value="(divider)"&gt;



for example:



&lt;param name="!LayerWidth\_6" value="1600"&gt;

[[BR](BR.md)]


&lt;param name="LayerDEM\_6" value="9"&gt;

[[BR](BR.md)]


&lt;param name="LayerMIN\_6" value="0"&gt;

[[BR](BR.md)]


&lt;param name="LayerMAX\_6" value="2000"&gt;

[[BR](BR.md)]


&lt;param name="LayerDIVIDER\_6" value="100000"&gt;

 [[BR](BR.md)]