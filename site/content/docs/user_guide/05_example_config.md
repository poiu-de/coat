---
title: "Example config"
weight: 5
---

### Example config file generation

The annotation processor generates an example config file (conforming to the
specification of Java `.properties` files) for each annotated interface.
That example contains an entry for each accessor method. 

If an accessor method is optional, the entry in the example will be
commented out.

If an accessor method provides a default value for a property, the entry
in the example will be commented out and the default value is assigned.

Otherwise (mandatory property, no default value) the entry in the example
will _not_ be commented out and does not have an assigned value.

Any javadoc comments on the accessor methods will be copied as a comment
above the corresponding entry in the example file.

The generated example file will have the same name as the annotated
interface with `.properties` appended and will be placed in a directory
`examples`. It will be generated in the same path as the generated classes.
The example files can then be accessed via their resource path. For an
annotated interface `ExampleConfig` (regardless of the package it is
defined in) the corresponding example file can then be accessed in the
classpath as `/examples/ExampleConfig.properties`.

This is most useful when packaging an application to include a commented
default config file that already contains all valid properties, for example
with the `maven-assembly-plugin`.


### Generation at runtime

The generated config class provides a method
`writeExampleConfig(java.io.Writer)` that allows generating the very same
example config at runtime. This is helpful in cases when the example config
should only be generated on a users demand.

### Caveats

The block tags of the javdoc comments on the accessor methods are stripped
off before copying them into the example config.

However at the moment the the remaining javadoc is copied as is. That means
all HTML markup and all javadoc tags will be copied, too.
