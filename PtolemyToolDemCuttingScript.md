Here is an example text script file run with the batchcut tools in order to prepare the DEM for pTolemy3D.

  1. Seattle DEM


java -classpath ./js3dbatch.jar:lib/jj2000-4.1.jar:. com.aruke.batch.tool.DemMaker -minx -122.555212 -maxx -122.032313 -miny 47.163459 -maxy 47.956169 -tw 0.8192 -cw 0.1024 -units DD -prefix 00 -out /data/js3ddata/seattledem/819200 -srcdir /home/data/seattle/lidar\_dem/

java -classpath ./js3dbatch.jar:lib/jj2000-4.1.jar:. com.aruke.batch.tool.DemMaker -minx -122.555212 -maxx -122.032313 -miny 47.163459 -maxy 47.956169 -tw 0.1024 -cw 0.0128 -units DD -prefix 00 -out /data/js3ddata/seattledem/102400 -srcdir /home/data/seattle/lidar\_dem/

java -classpath ./js3dbatch.jar:lib/jj2000-4.1.jar:. com.aruke.batch.tool.DemMaker -minx -122.555212 -maxx -122.032313 -miny 47.163459 -maxy 47.956169 -tw 0.0128 -cw 0.0016 -units DD -prefix 00 -out /data/js3ddata/seattledem/12800 -srcdir /home/data/seattle/lidar\_dem/ -divider 1000000

java -classpath ./js3dbatch.jar:lib/jj2000-4.1.jar:. com.aruke.batch.tool.DemMaker -minx -122.555212 -maxx -122.032313 -miny 47.163459 -maxy 47.956169 -tw 0.0016 -cw 0.0002 -units DD -prefix 00 -out /data/js3ddata/seattledem/1600 -srcdir /home/data/seattle/lidar\_dem/ -divider 100000



Yang san,





I already setup Mapserver and put the Java library on CCCC machine, Now all the processing step will held on CCCC machine. After finish testing, we will separate sever and decide again what server will use for FTP, Gdal process, Mapserver process and pTolemy3DJP2 data storage. Below is another batch file, It will be generated after the map file creation process. The same Cron job will check this batch file every minute and start it automatically. The bold character should be dynamic changed based on client project.





  1. JP2 batchcut for ASPservice #####


  1. Prepare the output folder for jp2 file


s\_path=/usr/local/apache/htdocs/ASP\_Service\_AUX/system/ ### path to system working folder

x\_path=/data/js3ddata/ ### This is a path to jpeg2000 output

mkdir $x\_path/Project\_Name ### Project\_Name? can be “ClientID-ProjectID-Date”

mkdir $x\_path/Project\_Name/1600

mkdir $x\_path/Project\_Name/12800

mkdir $x\_path/Project\_Name/102400

mkdir $x\_path/Project\_Name/819200

mkdir $x\_path/Project\_Name/6553600

mkdir $x\_path/Project\_Name/52428800

chmod 777 –Rf $x\_path/Project\_Name

mv $s\_path/mapfile.map /usr/local/apache/cgi-bin/mapfile.map



  1. get minx maxx miny maxy value from map extent


  1. mshost can be any (dynamic), depending on what Mapserver machine we use.


  1. mapfile is the mapfile that we generate from last process.




  1. Start MStoJP2 converter script


java -classpath /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/js3dbatch.jar: /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/jj2000-4.1.jar:. com.aruke.batch.tool.MSToJp2 -minx XXXXX -maxx XXXXX -miny XXXXX -maxy XXXXX -tw 52.428800 -units DD -prefix 00 -out /data/js3ddata/Project\_Name/52428800 -ms\_host localhost -ms\_path /cgi-bin/getTile?mapfile= mapfile.map



java -classpath /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/js3dbatch.jar: /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/jj2000-4.1.jar:. com.aruke.batch.tool.MSToJp2 -minx XXXXX -maxx XXXXX -miny XXXXX -maxy XXXXX -tw 6.553600 -units DD -prefix 00 -out /data/js3ddata/Project\_Name/6553600 -ms\_host localhost -ms\_path /cgi-bin/getTile?mapfile= mapfile.map



java -classpath /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/js3dbatch.jar: /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/jj2000-4.1.jar:. com.aruke.batch.tool.MSToJp2 -minx XXXXX -maxx XXXXX -miny XXXXX -maxy XXXXX -tw 0.819200 -units DD -prefix 00 -out /data/js3ddata/Project\_Name /819200 -ms\_host localhost -ms\_path /cgi-bin/getTile?mapfile= mapfile.map



java -classpath /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/js3dbatch.jar: /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/jj2000-4.1.jar:. com.aruke.batch.tool.MSToJp2 -minx XXXXX -maxx XXXXX -miny XXXXX -maxy XXXXX -tw 0.1024 -units DD -prefix 00 -out /data/js3ddata/Project\_Name /102400 -ms\_host localhost -ms\_path /cgi-bin/getTile?mapfile= mapfile.map







java -classpath /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/js3dbatch.jar: /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/jj2000-4.1.jar:. com.aruke.batch.tool.MSToJp2 -minx XXXXX -maxx XXXXX -miny XXXXX -maxy XXXXX -tw 0.0128 -units DD -prefix 00 -out /data/js3ddata/Project\_Name /12800 -ms\_host localhost -ms\_path /cgi-bin/getTile?mapfile= mapfile.map



java -classpath /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/js3dbatch.jar: /usr/local/apache/htdocs/ASP\_Service\_AUX/system/library/java/jj2000-4.1.jar:. com.aruke.batch.tool.MSToJp2 -minx XXXXX -maxx XXXXX -miny XXXXX -maxy XXXXX -tw 0.0016 -units DD -prefix 00 -out /data/js3ddata/Project\_Name /1600 -ms\_host localhost -ms\_path /cgi-bin/getTile?mapfile= mapfile.map





Please let me know if it has a problem.

Cheer!

-yut