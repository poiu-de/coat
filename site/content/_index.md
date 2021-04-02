---
title: "Coat — Config of Annotated Types"
description: "Easy and typesafe config objects"
---

# Generate typesafe config classes

Coat is an annotation processor to generate classes for reading
configuration values into typesafe objects.

## Short Usage

{{< block "grid-3" >}}
{{< column >}}
1. For the following `config.properties` file
```
appName     = My shiny app
listenPort  = 5040
description = Only a test project







```
{{< /column >}}

{{< column >}}

2. Define a corresponding interface
```java
import de.poiu.coat.annotation.Coat;

@Coat.Config
public interface MyConfig {
  @Coat.Param(key = "appName")
  public String appName();

  @Coat.Param(key = "listenPort", defaultValue = '8080')
  public int listenPort();

  @Coat.Param(key = "desription")
  public Optional<String> description();
}
```

{{< /column >}}

{{< column >}}
3. Then use the generated class
```java
final MyConfig config=
  new ImmutableMyConfig(
    new File("/path/to/config.properties"));

final String appName    = config.appName();
final int    listenPort = config.listenPort();
config.description().ifPresent(
  …
);




```
{{< /column >}}
{{< /block >}}


## License

Coat is licensed under the terms of the [Apache license 2.0](http://www.apache.org/licenses/LICENSE-2.0).

{{< button "docs/quick_start/" "Quick Start" >}} {{< button "docs/user_guide" "User Guide" >}}
