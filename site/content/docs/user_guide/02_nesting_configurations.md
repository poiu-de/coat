---
title: "Nesting Configurations"
weight: 2
---

Coat allows embedding config objects in other config objects.

It is helpful in the case if parts of a configuration are reused in
multiple other configurations without having to duplicate all the accessor
methods of the embedded config class.


### Example

As an example see the following `MqttConfig` that is embedded in the main
`AppConfig`.

```java
@Coat.Config
public interface MqttConfig {

  @Coat.Param(key = "client_id")
  public Optional<String>   clientId();

  @Coat.Param(key = "broker_address")
  public InetAddress        brokerAddress();

  @Coat.Param(key = "port", defaultValue = "1883")
  public int port();
```

```java
@Coat.Config
public interface AppConfig {

  @Coat.Param(key = "name")
  public String name();

  @Coat.Embedded(key = "mqtt")
  public MqttConfig mqtt();
```

A config file for that configuration would look like this:

```
name                = …
mqtt.client_id      = …
mqtt.broker_address = …
mqtt.port           = …
```

The config keys of the embedded config get a common prefix that is
specified on the `@Coat.Embedded` annotation of its accessor method. By
default that prefix is separated from the actual config key via a single
dot.
