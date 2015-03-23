!Apache

  * when building apache use the worker module, because of its scalability.


./configure --with-mpm=worker --prefix=/usr/local/apache --enable-module=so


here is a highly scalable example of the configuration for a worker module (this goes in the httpd.conf file)

<IfModule? worker.c>
StartServers? 20
ServerLimit? 250
MaxClients? 10000
MinSpareThreads? 1000
MaxSpareThreads? 15000
ThreadsPerChild? 50
ThreadLimit? 15000
MaxRequestsPerChild? 0


Unknown end tag for &lt;/IfModule&gt;



this will give us the ability to server up to 15000 concurrent clients from 1 machine.

  * other httpd.conf changes


Timeout 3000
MaxKeepAliveRequests? 0
KeepAliveTimeout? 3000

note: making these changes to Apache conf seems to make the difference between working and not, but we need to look at this further because something like a KeepAliveTimeout? of 3000 (secs) = 50 minutes is huge.  A lot of waiting connections!

tomcat setting, place at the bottom of the file

LoadModule? jk\_module modules/mod\_jk-2.0.43.so
JkWorkersFile? /usr/local/apache/conf/workers.properties
JkLogFile? /usr/local/apache/logs/mod\_jk.log
JkLogLevel? info
JkLogStampFormat? "[%a %b %d NaVM:%S %Y] "

Include /usr/local/tomcat/conf/auto/mod\_jk.conf

  * workers.properties file, goes in {apache root}/conf directory


workers.tomcat\_home=/usr/local/tomcat

workers.java\_home=/usr/local/java
ps=/
worker.list=ajp13
worker.ajp13.port=8009
worker.ajp13.host=192.168.0.x
worker.ajp13.type=ajp13