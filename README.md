# Java 9 ServletContainer JLink Integration

This projects integrates [Java 9 ServletContainer](https://github.com/ggam/java9-container) with JLink by splitting the server in two layers:

* Boot/application layer: contains the Java SE modules and the user's web application (WAR) repackaged as a JAR to fit the Java Module System.
* Server layer: server libraries are loaded from a second layer, which has the application layer as its parent. This layer can see all of the application classes.

The difference with the standard approach is that usually, it's the Application Server that runs on the base classloader, then creating a new classloader for the application classes. Classloader hierarchy is reversed to search classes on the root classloader instead of its immediate parent.

New approaches involve just the system classloader, sharing it for application and server classes. That's the simplest approach but can easily lead to conflicts.

The revised hierarchy of this project leaves user classes on the boot module layer/system class loader, while still isolating the server libraries on a child layer to avoid clashes.

This project contains two modules:

* Launcher: the main module that will be added to the module path and will take care of creating the server layer and finding the server implementation.
* Generator: creates a JLink image from a WAR, bundling the launcher and the server implementation ready to be loaded from different layers.
