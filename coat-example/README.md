Coat example project
====================

This example project shows the usage of Coat in a project. It is as minimal as possible while still showing the usage of as many features as possible.

It is a fully runnable application integrating Coat for loading the configuration (but without any real useful functionality).

While it is mainly intended to showcase Coats functionality an usage it can also be used as a blueprint for starting a new project utilizing Coat or just playing around with it.

The code is thoroughly commented. Look through it for a more “real life” example of Coat than can be shown in the [User Guide](https://poiu-de.github.io/coat/). The main files of interest are:

[AppConfig](src/main/java/de/poiu/coat/example/AppConfig.java)
: The annotated interface defining the type-safe accessor methods for the configuration values.

[MqttConfig](src/main/java/de/poiu/coat/example/AppConfig.java)
: Another annotated interface embedded in `AppConfig`.

[app.properties](src/main/resources/app.properties)
: The config file to load corresponding to the annotated interface.

[ExampleApp](src/main/java/de/poiu/coat/example/ExampleApp.java)
: The main class doing all the work of loading the config, validating it and using some of the config values.
