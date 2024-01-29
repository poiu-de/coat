---
title: "Usage"
weight: 3
---

### Create config interface

Write an interface with accessor methods for each config entry your
application supports. The accessor methods can return the concrete types you
want your config entry to be. There is a number of types that are
[supported by default]({{< ref "/docs/user_guide/04_supported_types.md" >}}),
but custom types can be registered to support additional types.

Config values that are _optional_, must be of type `java.util.Optional` or
the more specialized variants `OptionalInt`, `OptionalLong` or
`OptionalDouble`. All other config values are considered _mandatory_.
Missing mandatory values will throw exceptions at runtime.

The interface _must_ be annotated with the `@Coat.Config` annotation for
the annotation processor to recognize it.

Each accessor _may_ be annotated with the `@Coat.Param` annotation
to tell the processor the corresponding key in the config file (if it
should be different than what Coat would infer otherwise) or a default
value in case the key is missing in the config file.

Both annotations have some possible attributes that can be set which are described in [Annotations]({{< ref "/docs/user_guide/01_annotations.md" >}}).

For example:

```java
package com.example;

import de.poiu.coat.annotation.Coat;

@Coat.Config
public interface AppConfig {
  public String      appName();

  public InetAddress remoteIP();

  @Coat.Param(default = "5044")
  public int         remotePort();

  @Coat.Param(key = "long_description")
  public Optional<String> description();
}
```

### Generate concrete config class

When compiling the project the annotation processor will produce a
concrete implementation of the interface in the same package and (by
default) the same name with `Immutable` prepended to it. Therefore the
above example interface would produce a `com.example.ImmutableAppConfig`
class.

### Use the generated config class

The generated config class can be instantiated from a number of different sources.
Since version 0.1.0 Coat also allows instantiation from multiple sources at the same time.

#### Config Sources

The main source of config entries for Coat are `java.util.Map<String,
String>`s. For common config sources some shortcut methods exist that provide
direct support, but in all other cases everything that can be represented as
a map of String keys to String values can be used as a config source.

##### java.util.Map<String, String>

A map with key-value mappings can directly be given to Coat to instantiate
a Config class with these entries.

##### java.io.File

The traditional approach of specifying config entries in Java is via
Property-Files. Coat explicitly supports this use case by allowing a `File`
object as config source which in then read via Javas loading mechanism for
Property-Files.

##### java.util.Properties

Instead of specifying a file, a `Properties` object can be directly fed into
Coat as config source. This allows, for example, the usage of Java System
Properties as config source by reading them via `System#getProperties()`.

While `java.util.Properties` can theoretically contain non-String keys or
values, Coat does not allow these and will drop such entries (generating
a warning message).

##### Environment variables

Special support is provided for reading config entries from environment
variables. This is a common approach for applications running in containers.

Unfortunately the allowed character set for environment variable keys is much
stricter than the character set in Coat config (and therefore Java Properties)
files. For that reason a relaxed mapping is applied to match environment variables to Coat config keys.

- All dots and hyphens are treated as underscores.
- All uppercase characters in Coat config keys are preceded by an underscore (to convert camelCase to UPPER_CASE).
- The comparison between the environment variables and the Coat config keys is done case insensitively.

For example the environment variable `SERVER_MQTT_HOST` will match the config key `server.mqttHost`.

#### Instantiation via static factory methods

Static factory methods are provided for reading config values from a single config source:

- `from(java.util.Map)`
- `from(java.util.Properties)`
- `from(java.io.File)`
- `fromEnvVars()`

Example:

```java
final MyConfig config= ImmutableMyConfig.from(System.getProperties());
```

#### Instantiation via builder

A builder is provided to create config objects and define the config sources.
While it is possible to use the builder for a single config source, it is the
only way to read multiple config sources.

The order in which the config sources are read is defined by the order in which
they are added to the builder. Later config sources overwrite values with the
same keys of earlier config sources.

For example for reading the basic config from a config file, but allow
overriding some entries via environment variables:

```java
final MyConfig config= ImmutableMyConfig.builder()
  .add(new File("myConfig.properties"))
  .addEnvVars()
  .build();
```

#### Validation of config objets

The instantiated config object can be
[validated]({{< ref "/docs/user_guide/03_validation.md" >}})
to fail early in case mandatory config values are missing or existing
values cannot be converted to the expected type.

```java
public class MyApp {
  public static void main(String[] args) {
    final ImmutableAppConfig config= ImmutableAppConfig.from(
      new File("/path/to/config.properties"));

    try {
      config.validate();
    } catch (final ConfigValidationException ex) {
      System.err.println("Error in config file:\n"
                         + ex.getValidationResult().toString());
      System.exit(1);
    }

    System.out.println("Starting " + config.appName());
    config.description.ifPresent(System.out::println);

    final Socket s= new Socket(config.remoteIP, config.remotePort);

    …
  }
}
```
