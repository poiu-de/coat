---
title: "Annotations"
weight: 1
---

Coat provides three annotations, a type-level annotation `@Coat.Config` and
two method-level annotations `@Coat.Param` and `@Coat.Embedded`.

The type-level annotation is mandatory. It is the indicator for the annotation processor which interfaces need to be proceessed.

`@Coat.Param` and `@Coat.Embedded` are mutually exclusive.


### @Coat.Config

Each interface that should be processed by the annotation processor must be
annotated with `@Coat.Config`.

The generated class will always be generated in the same package as the
annotated interface.

The name of the generated class is by the default the interface name with
`Immutable` prepended to it. One exception is if the interface name starts with
an underscore. In that case the generated class name is the same as the
interface, but with the leading underscore removed. Therefore for the interface
`_MyConfig` the generated class would be `MyConfig`.

`@Coat.Config` [(Javadoc)](https://javadoc.io/doc/de.poiu.coat/coat-processor/latest/de/poiu/coat/annotation/Coat.Config.html) supports the following attributes:

#### *className*
The `className` can be specified to generate a class with a different name than
when applying the above mentioned naming rules. When `className` is specified
it will be used as the generated class name. It will still be generated in the
same package as the annotated interface.

```java
@Coat.Config(className = "MyAppConfig")
public interface AppConfig {
  …
}
```

#### *casing*

By default (if not explicitly specified by [@Coat.Param#key]({{< ref "#coat-param-key" >}})) Coat expects the key in the config file exactly as the accessor methods name (respecting upper/lower case). To allow for different formats of keys without having to explicitly declare each key, Coat provides a `CasingStrategy`.

`AS_IS`
: The default. The key is expected in the same case as the accessor methods name.

`SNAKE_CASE`
: The typical snakeCase of Java method names is converted to _snake_case_. For example the method name “listenPort” would then be specified as “listen_port” in the config file.

`KEBAB_CASE`
: Similar to SNAKE_CASE, but instead of underscores an hyphen is used. “listenPort” would be expected as “listen-port”.

All the above strategies expect the accessor method names in camelCase (conforming to the Java code formatting conventions). Deviating from that conventions may lead to unexpected results.

```java
@Coat.Confg(casing = SNAKE_CASE)
public interface AppConfig {
  …
}
```

#### *stripGetPrefix*

Coat is agnostic to the naming conventions of the accessor methods. Using the Java Bean Convention of prefixing the methods with “get” leads to strange config keys (that also include the “get” prefix). Therefore Coat strips these prefixes before inferring the key. So the accessor method “getListenPort” would be specified as “listenPort” in the config file,

If the “get” prefix should not be stripped for some reason, the `stripGetPrefix` can be set to false to prohibit that behavior.

```java
@Coat.Confg(stripGetPrefix = false)
public interface AppConfig {
  public boolean getMeABeer();
  …
}
```

#### *converters* {#coat-config-converters}

Coat supports registering [custom converters]({{< ref "/docs/user_guide/04_supported_types.md#registering-custom-types" >}}) via a static method on the `CoatConfig` class.

Converters can also be registered declaratively with the `converters` attribute. It expects an array of Converter classes to register.

```java
@Coat.Confg(converters = {
  UuidConverter.class,
  CurrencyConverter.class,
})
public interface AppConfig {
  public UUID uuid();
  public Currency currency();
  …
}
```

#### *listParser* {#coat-config-listparser}

Since version 0.0.4 Coat supports [Arrays and other collection types]({{< ref "/docs/user_guide/04_supported_types#collection-types" >}}). By default it splits the values given in the config file on whitespace. To define a different format to use for Arrays and collections a custom ListParser can be registered.

Since version 0.1.0 Coat provides such an optional ListParser for splitting config values on commas, ignoring any whitespace around the commas.
Of course, other custom ListParsers can be implemented and specified in the same way.

```java
@Coat.Confg(listParser = CommaSeparatedListParser.class)
public interface AppConfig {
  public InetAddress[] remoteAdresses();
  …
}
```

### @Coat.Param

Since version 0.0.4 the `@Coat.Param` annotation on accessor methods is optional.

`@Coat.Param` [(Javadoc)](https://javadoc.io/doc/de.poiu.coat/coat-processor/latest/de/poiu/coat/annotation/Coat.Param.html) supports the following attributes:

#### *key* {#coat-param-key}

The parameter `key` specifies the name of the key
as it must be specified in the config file (or the `Properties` or `Map`
object with which the config class is instantiated).

If this `key` is not specified, it will be automatically inferred from the accessor methods name, respecting the [casing]({{< ref "#casing" >}}) and [stripGetPrefix]({{< ref "#stripgetprefix" >}}) parameters of the corresponding `@Coat.Config` annotation. By default the `key` will be exactly the same as the accessor methods name.

```java
@Coat.Param(key = "listen-port")
public int port();
```

#### *defaultValue*

A parameter `defaultValue` can be specified to define a default
value that will be used if the config key is missing in the config file.
The default value must be specified as a String in the same form it would
be specified in the config file. For example:

```java
@Coat.Param(defaultValue = "8080")
public int port();
```

The generated config would return the value that was specified in the
config file or `8080` if no port was specified.

#### *converter*  {#coat-param-converter}

Additionally to the [corresponding parameter on the `@CoatConfig` annotation]({{< ref "#coat-config-converters" >}}) a converter can be specified on the accessor method itself. This will override the default converter specified on the interface level.

```java
@Coat.Param(converter = MyDateConverter.class)
public LocalDate startTime();
```

Use this attribute sparingly and prefer the corresponding attribute on the interface-level annotation `@Coat.Config` as different formats for the same data types in the same config can be very surprising.

#### *listParser* {#coat-param-listparser}

Additionally to the [corresponding parameter on the `@CoatConfig` annotation]({{< ref "#coat-config-listparser" >}}) a ListParser can be specified on the accessor method itself. This will override the default ListParser specified on the interface level.

```java
@Coat.Param(listParser = MyListParser.class)
public String[] preferredRoles();
```

Use this attribute sparingly and prefer the corresponding attribute on the interface-level annotation `@Coat.Config` as different formats for specifying lists in the same config can be very surprising.


### @Coat.Embedded

When using [nested configurations]({{< ref "/docs/user_guide/02_nesting_configurations" >}}) the
annotation `@Coat.Embedded` must be used instead of `@Coat.Param` on the
corresponding accessor method.

When using `@Coat.Embedded` the return type of the accessor method _must_
be a `@Coat.Config` annotated interface.

`@Coat.Embedded` [(Javadoc)](https://javadoc.io/doc/de.poiu.coat/coat-processor/latest/de/poiu/coat/annotation/Coat.Embedded.html) supports the following attributes:

#### *key*

The parameter `key` specifies the prefix for the config parameters of the
embedded config. See [@Coat.Param#key]({{< relref "#coat-param-key" >}}) for
more details about this attribute.

```java
@Coat.Embedded(key = "broker")
public MqttConfig mqtt();
```

#### *keySeparator*

The parameter `keySeparator` can be specified to define the
separator between the prefix and the actual config key of the embedded
config. It defaults to a single dot.

```java
@Coat.Embedded(keySeparator = "-")
public MqttConfig mqtt();
```
