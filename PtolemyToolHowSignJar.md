# How to sign a JAR file #

Talking about certificates is beyond the scope of this page. Here we present a brief example using the tools `keytool` and `jarsigner` included in the Java SDK.

```
#!div style="border: 2pt dashed darkgrey; margin: 5px; padding: 5px; background-color: #EEEEEE;"
More and better information about these tools can be:
 * [http://java.sun.com/j2se/1.4.2/docs/guide/plugin/developer_guide/rsa_signing.html How to Sign Applets Using RSA-Signed Certificates].
 * [http://java.sun.com/docs/books/tutorial/security/toolsign/index.html Signing Code and Granting It Permissions].
```

Assuming both tools are in the PATH environment variable, the basic steps are:
  * Create a certificate, it can be done only once.
  * Export the certificate to a file.
  * Sign your files with the certificate.

```
> keytool -genkey -keyalg rsa -alias somename
 Enter pw : ******

> keytool -export -alias somename -file somename.crt

> jarsigner file.jar yourname
```