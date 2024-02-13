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
    <!-- Contains the converters and base classes. Needed at runtime. -->
    <dependency>
      <groupId>de.poiu.coat</groupId>
      <artifactId>coat-runtime</artifactId>
      <version>{{< param last_stable_coat_version >}}</version>
    </dependency>

    <build>
      <plugins>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <configuration>
          <annotationProcessorPaths>
            <!-- Specifies the Coat annotation processor. Required from Java 22 onwards. -->
            <path>
              <groupId>de.poiu.coat</groupId>
              <artifactId>coat-processor</artifactId>
              <version>{{< param last_stable_coat_version >}}</version>
            </path>
          </annotationProcessorPaths>
      </plugins>
    </build>
```
