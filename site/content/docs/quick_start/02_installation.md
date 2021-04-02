---
title: "Installation"
weight: 2
---

Coat consists of two separate jars.

- The annotation processor to generate the config classes. Only needed at
  compile time.
- The runtime package containing the common base class for all generated
  config classes and the default type converters. Needed at runtime.
  
To use Coat in a maven based project use the following maven coordinates:

```xml
    <!-- Contains the annotation processor. Not needed at runtime. -->
    <dependency>
      <groupId>de.poiu.coat</groupId>
      <artifactId>coat-processor</artifactId>
      <version>0.0.1</version>
      <scope>provided</scope>
    </dependency>

    <!-- Contains the converters and base classes. Needed at runtime. -->
    <dependency>
      <groupId>de.poiu.coat</groupId>
      <artifactId>coat-runtime</artifactId>
      <version>0.0.1</version>
    </dependency>
```

