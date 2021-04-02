---
title: "Planned Features"
---

### Support for arrays and collection types

At the moment it is not possible to use arrays or collections with generic
types in config classes. This is quiet some drawback and it is planned to
be supported in the future.

### Generation of example config files

As the annotated interface is the specification of the whole valid configuration it would be possible and in fact desirable to let Coat generate an example config file conforming to that specification.

Optional values could be included, but commented out.

The javadoc for each accessor method could be copied as the description of
the corresponding entry in the example config file.

### Nested configuration options

At the moment only flat config files are supported.

It would be nice to nest config values to encapsulate and reuse them.

For example the following config interface

```java
@Coat.Config 
public interface MyConfig {
  @Coat.Param (key = "appName")
  public String appName();
  
  @Coat.Param (key = "mqtt.server")
  public String mqttServer();
  
  @Coat.Param (key = "mqtt.port", defaultValue = "1883")
  public String mqttPort();

  @Coat.Param (key = "mqtt.clientId")
  public Optional<String> mqttClientId();
}
```

could then be defined as

```java
@Coat.Config 
public interface MyConfig {
  @Coat.Param (key = "appName")
  public String appName();
  
  @Coat.Param (key = "mqtt")
  public MqttConfig mqttConfig();
}

public interface MqttConfig {
  @Coat.Param (key = "server")
  public String server();
  
  @Coat.Param (key = "port", defaultValue = "1883")
  public String port();

  @Coat.Param (key = "clientId")
  public Optional<String> clientId();
}
```

### Declarative declaration of custom converters

Custom converters can only be specified programmatically and globally at
runtime. 

it would be nice to support a declarative declaration of custom converters
in the `@Coat.Config` and/or `@Coat.Param` annotation. 

This is a low priority goal as the actual real-world benefit is unclear.
