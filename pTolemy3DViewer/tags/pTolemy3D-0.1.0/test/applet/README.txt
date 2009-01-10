To sign JAR file:

> keytool -genkey -keyalg rsa -alias js3d -keypass js3djogl
> keytool -export -alias js3d -file js3d.crt
> jarsigner js3d_jogl.jar js3d

