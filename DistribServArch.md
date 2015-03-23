Architecture for pTolemy3D in a multiple server configuration



Applet parameter example



&lt;param name="nMapDataSvr" value="2"&gt;

[[BR](BR.md)]



&lt;param name="MapDataSvr1" value="192.168.0.123"&gt;

[[BR](BR.md)]


&lt;param name="MapJp2Store1" value="/bmng/,/illinois/,/chicago/"&gt;

[[BR](BR.md)]


&lt;param name="MServerKey1" value="NA"&gt;

[[BR](BR.md)]


&lt;param name="MapDemStore1" value="/dem/illinois/"&gt;

[[BR](BR.md)]



&lt;param name="MapDataSvr2" value="192.168.0.124"&gt;

[[BR](BR.md)]


&lt;param name="MapJp2Store2" value="/bmng/"&gt;

[[BR](BR.md)]


&lt;param name="MapDemStore2" value="/dembase/"&gt;

[[BR](BR.md)]

  * nMapDataSvr - an integer that defines number of servers. servers will be queried in order.
  * MapDataSvrX - host name of data server
  * MapJp2StoreX - comma delimited list of jp2 data locations on server. locations will we queried in order.
  * MapDemStoreX- comma delimited list of dem data locations on server. locations will we queried in order.
  * MServerKeyX - handshake file. used to do connection tests at initalization



Requirements for a proper server

Apache settings

  * Timeout 3000(+)[[BR](BR.md)]
  * KeepAlive On[[BR](BR.md)]
  * MaxKeepAliveRequests 0[[BR](BR.md)]
  * KeepAliveTimeout 3000+ [[BR](BR.md)]


default jpeg 2000 file
(MapJp2Store1)/default.jp2


put this file into your first location of your jp2store.
this file is used to obtain jpeg2000 parameters for this data set. to increase performance
jpeg params are parsed only once at startup.


Chain order of file aquisition
  1. filename is decided on by pTolemy3D , for example 00x137000000y35000000.jp2
> 2. server 1 locations are queried in order. if file is not found, move on to the next server until all servers and locations are checked.

Key points
  * mult. locations gives us an easy way to seperate out user files
  * server connections tested at startup. if failed server is not re-queried for the duration of usage.
  * servers that fail authentication will simply be set to null and not used throughout usage.

