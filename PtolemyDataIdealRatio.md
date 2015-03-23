'''What is the ideal DD width to use for jp2 and DEM generation?'''

the assumption here is that tiles are 1024 pixels square
at a latitude of 0 (zero) degrees, ie the equator

length of the Degree in lat is 110574.2727 meters

assuming this calculator is accurate

http://www.csgnetwork.com/degreelenllavcalc.html

to get the size of one pixel you would calc the following:

110574.2727m x .0.0016 / 1024 = 0.17277230109375 meter = 6.8 inches


DD[[BR](BR.md)]

tile w ratio(degree/pixel)[[BR](BR.md)]
0.0016 0.0000015625[[BR](BR.md)]
0.0128 0.0000125[[BR](BR.md)]
0.1024 0.0001[[BR](BR.md)]
0.8192 0.0008[[BR](BR.md)]
6.5536 0.0064[[BR](BR.md)]
52.4288 0.0512[[BR](BR.md)]



METERS
tile w ratio (meter/pixel)[[BR](BR.md)]
250 0.244140625[[BR](BR.md)]
2000 1.953125[[BR](BR.md)]
16000 15.625[[BR](BR.md)]
128000 125[[BR](BR.md)]


METER - DD[[BR](BR.md)]
LAYER ; TILE WIDTH DD ; IDEAL PIXEL/UNIT RATIO[[BR](BR.md)]

layer 1 ; 0.0016 ; 0.0000015625(about 0.142 meters)[[BR](BR.md)]
layer 2 ; 0.0128 ; 0.0000125(about 1.136 meters)[[BR](BR.md)]
layer 3 ; 0.1024 ; 0.0001(about 9.1 meters)[[BR](BR.md)]
layer 4 ; 0.8192 ; 0.0008(72.72 meters)[[BR](BR.md)]
layer 5 ; 6.5536 ; 0.0064(about 581.81 meters)[[BR](BR.md)]
layer 6 ; 52.4288; 0.0512(about 4654.54 meters) [[BR](BR.md)]
