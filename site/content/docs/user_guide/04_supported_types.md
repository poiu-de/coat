---
title: "Supported Types"
weight: 4
---

### Builtin types

The following types are supported by Coat out of the box and can directly
be used as return types of the accessor methods in the annotated interface.

  * java.lang.Integer
  * java.lang.Long
  * java.lang.Float
  * java.lang.Double
  * java.lang.String
  * java.time.Duration
  * java.time.LocalDate
  * java.time.LocalDateTime
  * java.time.LocalTime
  * java.io.File
  * java.nio.file.Path
  * java.nio.charset.Charset
  * java.net.InetAddress
  * java.security.MessageDigest

For each supported type a converter class exists in the package
`de.poiu.coat.convert`. Refer to the corresponding 
[Java API docs](https://javadoc.io/doc/de.poiu.coat/coat-runtime/latest/de/poiu/coat/convert/package-summary.html)
for details about the expected format of the input string.

The following primitive types are also supported by default. But those are
different in that they are directly supported without any Converter.

  * int

    Integer values can be specified in decimal (no prefix), hexadecimal
    (prefixed by `0x`), octal (prefixed by `0`) or binary (prefixed by
    `0b`) form. 

  * long

    Long values can be specified in decimal (no prefix), hexadecimal
    (prefixed by `0x`), octal (prefixed by `0`) or binary (prefixed by
    `0b`) form. 

  * double

    Double values can be specified in decimal (no prefix) or hexadecimal
    (prefixed by `0x`) form. 

    For decimal values an optional exponent can be appended, in which case it must be separated with `e` or `E`,
    like `1.0e5`. For hexadecimal values the exponent is _mandatory_ and must be separated with `p` or `P`, like `0xaaP5`.
    In either case the exponent can optionally be signed (e. g. `1.0e+5`).


  * boolean

    The strings “true" and “yes” (regardless of their case) are considered
    as `Boolean.TRUE`, all other Strings (including null) are considered to
    be `Boolean.FALSE`.

Be aware that numeric types (int, long, double) do _not_ allow the
specification of a type suffix (`l` for long, `d` for double, etc.) as
would be valid in a Java literal numeric value.  
However, underscores for separating parts of a number (like `1_000_000`) _are_
supported.


### Registering custom types

Coat allows registering custom types to be used in the annotated interface.

Each generated config class provides a static method `registerConverter()`
that can be used to register a converter for a specific type. To do this,
the interface `de.poiu.coat.convert.Converter` must be implemented for that
specific type and registered with the above mentioned method.

The `registerConverter()` method _must_ be called before an accessor method
returning that type is called and before the `validate()` method is called.

The `registerConverter()` method can additionally be used for overriding
the builtin converter for a type. For example if duration should be
specified in some other format than the default converter supports, write a
custom converter for the `java.time.Duration` type and register it via

```java
ImmutableMyConfig.registerConverter(Duration.class, new MyDurationConverter());
```

As support for primitive types is directly implemented and not via
Converter it is currently not possible to override the parsing of primitive
types with a custom Converter. If different parsing of such types is
necessary the corresponding object type must be used and a Converter for
that type written (e. g. a `Converter<Integer>`).

Additionally to the above mentioned method of registering custom converters _at runtime_, they can also be specified declaratively on the corresponding annotations. See the description of these annotation parameters on the [type]({{< ref "/docs/user_guide/01_annotations#coat-config-converters" >}}) and on the [field]({{< ref "/docs/user_guide/01_annotations#coat-param-converter" >}}) level annotations for more information.

### Currently unsupported types

At the moment the primitive types `short`, `float`, `char` and `byte`
are not supported. Therefore the next “bigger” types must be used (e. g.
`int` instead of `short`) or the corresponding class (e. g. `Short` instead
of `short`).

## Collection types

Since version 0.0.4 Coat supports Arrays and collections as return values of accessor methods.

- Arrays
- java.util.List
- java.util.Set

Arrays are only supported for object types, not primitive types. Be aware that Arrays are by nature mutable. For that reason Lists should be preferred instead.

By default the values of collection types are expected to be separated by whitespace. Whitespace _inside a single_ value can be used by prefixing each such whitespace character with a backslash. For example the value `one\ two three` would then be split into a collection with the two values `one two` and `three`.

Additionally, since version 0.1.0 a [CommaSeparatedListParser](https://javadoc.io/doc/de.poiu.coat/coat-runtime/latest/de/poiu/coat/convert/CommaSeparatedListParser.html) is provided for splitting around commas (ignoring any whitespace around the commas). If will not be used by default and must be explicitly declared like a custom ListParser.

Default values are supported for arrays and collections as well.

```java
@Coat.Config
public AppConfig {
  @Coat.Param(defaultValue = "UTF-8 US-ASCII")
  public Charset[] allowedCharsets();
}
```

## Registering custom ListParser

Coat allows for different formats than the default whitespace separated values by registering a custom ListParser.
Each generated config class provides a static method `registerListParser()` to register a custom parser for such values. Additionally such a ListParser can be declared on the [@Coat.Config]({{< ref "/docs/user_guide/01_annotations#coat-config-listparser" >}}) annotation as well as on the [@Coat.Param]({{< ref "/docs/user_guide/01_annotations#coat-param-listparser" >}}) annotation.

A custom ListParser must implement the `de.poiu.coat.convert.ListParser` interface.

## Optional values

Config entries that are optional must be encapsulated in `java.util.Optional` or the more specialized variants `OptionalInt`, `OptionalLong` or `OptionalDouble` need to be used. All other config values are considered mandatory. Missing mandatory values will throw an exception at runtime.

[Embedded types]({{< ref "/docs/user_guide/01_annotations#coatembedded" >}}) may be optional, too. They are considered present if at least one config entry with the corresponding key and separator was found. In that case all mandatory values of the embedded config must be present.

Optional collections are not supported out of the box. The generation will succeed, but no converter will be found at runtime. Most of the time an optional collection does not make much sense and an empty collection should be returned in case no value is specified. Optional collections may be supported by providing a [custom converter]({{< ref "#registering-custom-types" >}}) for that specific collection, for example:

```java
public class IntListConverter implements Converter<List<Integer>> {
  private final IntegerConverter ic= new IntegerConverter();

  @Override
  public List<Integer> convert(final String stringValue) throws TypeConversionException {
    final String[] splitString= stringValue.split("\\s+");
    final List<Integer> result= new ArrayList(splitString.length);
    for (final String s : splitString) {
      result.add(ic.convert(s));
    }

    return result;
  }
}
```

