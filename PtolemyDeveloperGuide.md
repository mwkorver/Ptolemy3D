# pTolemy3D developer guide #

You are here because you like pTolemy3D and want (we hope) to contribute to the project by improving or adding features. This guide will show you how get the pTolemy3D source code and build the binary files yourself.

[[PageOutline(2-3,,)]]

---


## Requirements ##

Before you continue, ensure that you have installed the appropriate versions of the following software:

| Java | pTolemy3D is implemented in Java language. You need at least the JDK 1.5 or higher if you want to build the binary files from the source code. Download and install before you continue reading. You can find different downloads for Java language [here](http://java.sun.com/javase/downloads/index.jsp). |
|:-----|:------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

| Maven | pTolemy3D uses [Maven](http://maven.apache.org) to manage the project's life cycle. This requires you to download ver 2.0.9+ and install on your system. Remember maven is a Java based tool so you need to install Java first. |
|:------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

| Subversion client | Subversion is one of the best and most widely used revision control systems and so we have put the pTolemy3D code under an SVN server. To get, or send code if you are a contributor, from our SVN server you need a SVN client. You can find links to different third party clients at [Subversion](http://subversion.tigris.org) home page.|
|:------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

---



## Install JOGL JAR files on your maven's local repository ##

Before compile pTolemy3D or ptolemy3D-Plugins source code you must have installed into your maven's local repository the JAR files of the JOGL projects. To do that:

  * Download the latest stable JOGL release appropriate for your system: https://jogl.dev.java.net.
  * Unpack at some place. We refer to the new directory as `$JOGL_HOME`.
  * Go into the new directory `lib` folder: `$JOGL_HOME/lib`.
  * Execute (use the right version number):
```
mvn install:install-file -DgroupId=net.java.jogl -DartifactId=gluegen-rt.jar -Dversion=1.1.1 -Dfile=gluegen-rt.jar -Dpackaging=jar
```
> to install gluegen.jar file at maven's local repository.
  * Execute (use the right version number):
```
mvn install:install-file -DgroupId=net.java.jogl -DartifactId=jogl.jar -Dversion=1.1.1 -Dfile=jogl.jar -Dpackaging=jar
```
> to install jogl.jar file at maven's local repository.

---



## Download source code ##

Just download the source code from the pTolemy3D SVN server. Additionally, if you want to build or work with the pTolemy3D Plugins project, you need to also download the source code.
If you plan to create a new plugin as a separate project, then you may not need to download the pTolemy3D Plugins project, if you don't reference it.

For both pTolemy3D Viewer or pTolemy3D Plugins projects you can download (checkout in the SVN jargon) the current development version from the `trunk` folder or download your desired release from the `tags` one.

  * [Viewer current development](http://svn.ptolemy3d.org/pTolemy3DViewer/trunk)
  * [Viewer previous releases](http://svn.ptolemy3d.org/pTolemy3DViewer/tags)
  * [Plugins currend development](http://svn.ptolemy3d.org/pTolemy3DPlugins/trunk)
  * [Plugins previous releases](http://svn.ptolemy3d.org/pTolemy3DPlugins/tags)

For those using command line SVN client:
```
svn checkout http://svn.ptolemy3d.org/pTolemy3DViewer/trunk ptolemy3d
```

will download the current pTolemy3D development code to the local `ptolemy3d` directory.
If you want to work with the pTolemy3D-Plugins code too, then execute also:
```
svn checkout http://svn.ptolemy3d.org/pTolemy3DPlugins/trunk ptolemy3dplugins
```

---



## Before building ##

pTolemy3D is mainly designed to be executed as an applet in your web browser. Working with applets requires you to sign the JAR files that will be referenced in your HTML pages.
Don't worry about that, the projects are configured so Maven takes care to sign the pTolemy3D and pTolemy3D-Plugins JAR files for you. The only requisite is a key with alias and password 'pTolemy3D' must exists in your key store. Execute:

```
> keytool -genkey -alias pTolemy3D -keypass pTolemy3D
```

now you have a new key in your key store that can be used by Maven in the build process. This only must be done one time, or if you remove the key.
If you create a key with a different alias or password take care to modify the `pom.xml` file.

---



## Building the pTolemy3D binary file ##

As we mentioned previously, pTolemy3D uses Maven, so if you look into the downloaded project's folders you'll find a typical maven project's structure. Supposing we have executed the previous command:
```
ptolemy3d
|-- pom.xml
`-- src
    |-- main
    |   `-- java
    |       `-- ...
    `-- test
        `-- ...
```

To build the JAR file it is enough to execute:
```
> mvn clean package
```
this will create a new `target` directory which contains: the compiled classes, the pTolemy3D JAR file and a new `signed` directory with a signed version of the JAR file, ready to be used as applet in your web browser. Also, a new `target/lib` will be create with all JAR file on which pTolemy3D depends on.

---



## Building the pTolemy3D-Plugins binary file ##

As you may suppose, pTolemy3D-Plugins code depends on pTolemy3D JAR file, which contains the ''core'' classes. This mean the pTolemy3D JAR file must exists in our Maven local repository. To do so, after the previous building step and in the `ptolemy3d` directory, we must execute:

```
> mvn install
```

Go to the `ptolemy3dplugins` folder and repeat the previous process, that is:
```
> mvn clean package
```

again we could see a new `target` directory which contains: the compiled classes, the pTolemy3D-Plugins JAR file and its signed version in the `target/signed` directory ready to be used in your web browser.

---



## Setting up IDE ##

More probably you would like to work in pTolemy3D code using your favorite IDE. Hopefully, maven have is supported by almost all the existent IDE's like Eclipse, NetBeans or IDEA.

### Eclipse ###
There are two ways to start working with Eclipse:

  * Use an Eclipse plugin for Maven: http://maven.apache.org/plugins/maven-eclipse-plugin. Executing this plugin (`mvn eclipse:eclipse`) generates all required metadata files to create an Eclipse project ready to be importat to your workspace.

  * Use the Maven plugin for Eclipse: We encourage the use of http://m2eclipse.codehaus.org. Once installed `m2eclipse` can recognizes and import any Maven project.

### NetBeans ###

For NetBeans we remommend to install the Mevenide http://mevenide.codehaus.org/m2-site/index.html plugin. Once installed it allows to open any Maven project as other normal NetBeans project.

The easiest way to install it is going to the menu `Tools > Plugins` and search for `maven` string. Click on the install button and enjoy it.


---



## Working with SVN ##

There exists Subversion plugins for all IDE's, which allow you: checkout, update, commit, etc your projects and project files. NetBeans has Subversion support by default (but you need to have installed the [Subversion client](http://subversion.tigris.org/) on your system). For Eclipse, there is the [Subclipse](http://subclipse.tigris.org/) project which integrates in the Eclipse IDE very good.

---

IDE's usually generates files and/or directories with some metadata. If you have commit rights, please take care and '''''don't upload to SVN non required files others than source code and pom.xml''''' file.
For security, but it doesn't guaranties 100%, we have set the `global-ignores` property at SVN server to:

```
global-ignores = *.o *.lo *.la #*# .*.rej *.rej .*~ *~ .#* .DS_Store .* *.class *.jar
```

which ignores, among others:
  * all the hidden files and folders like: `.classpath` or `.settings` used by Eclipse.
  * all `class` files.
  * all JAR files, to avoid using unnecessary space and improve SVN downloads.

---



## References ##
  * http://maven.apache.org/index.html
  * http://books.sonatype.com/maven-book/reference/public-book.html: Online free book.
  * http://svnbook.red-bean.com/en/1.5/index.html: For people new to SVN we recommend take a loot to the chapter 2 "Basic Usage" and "Basic Work Cycle" section.