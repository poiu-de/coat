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

Also, each accessor _must_ be annotated with the `@Coat.Param` annotation
to tell the processor the corresponding key in the config file.

For example:

```java
package com.example; 

import de.poiu.coat.annotation.Coat;

@Coat.Config
public interface AppConfig {
  @Coat.Param(key = "appName")
  public String      appName();

  @Coat.Param(key = "remoteIP")
  public InetAddress remoteIP();

  @Coat.Param(key = "remotePort")
  public int         remotePort();

  @Coat.Param(key = "desription")
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

At runtime the generated config class can be instantiated with either a
`java.io.File` object referencing the actual config file, a
`java.util.Properties` object or, if the config data is read from some
other source, with a `java.util.Map<String, String>`.

The instantiated config object can be 
[validated]({{< ref "/docs/user_guide/03_validation.md" >}})
to fail early in case mandatory config values are missing or existing
values cannot be converted to the expected type.

```java
public class MyApp {
  public static void main(String[] args) {
    final ImmutableAppConfig config= new ImmutableAppConfig(
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
