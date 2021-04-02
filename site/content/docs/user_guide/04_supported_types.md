---
title: "Supported Types"
weight: 4
---

### Builtin types

The following types are supported by Coat out of the box and can directly
be used as return types of the accessor methods in the annotated interface.

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
`de.poiu.coat.convert`. Refer to the corresponding Java API docs for
details about the expected format of the input string.
<!-- FIXME: Link to API doc -->

The following primitive types are also supported by default. But those are
different in that they are directly supported without any Converter.

* int
* long
* double
* boolean

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
Converter it is currently not possible to override the parsing of primitve
types with a custom converter. If different parsing of such types is
necessary the corresponding object type must be used and a Converter for
that type written (e. g. a Converter(Integer)).

### Currently unsupported types

At the moment no arrays or collection types are supported by Coat. Trying
to specify an array or a collection of a generic type will lead to
undefined behaviour.

Support for arrays and collection types is in the roadmap, but not
implemented yet.

Also at the moment the primitive types `short`, `float`, `char` and `byte`
are not supported. Therefore the next "bigger" types must be used (e. g.
`int` instead of `short`) or the corresponding class (e. g. `Short` instead
of `short`).
