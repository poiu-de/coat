---
title: "Logging"
weight: 7
---

In some cases Coat issues logging statements. To not introduce a third-party dependency just for that case, the `java.lang.System.Logger` is used for that purpose.
By default this uses `java.util.logging` as the actual logging implementation, but can be reconfigured to use a different implementation. For example to log via log4j2 the  maven artifact `org.apache.logging.log4j:log4j-jpl` must be specified as a dependency:

```xml
    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-jpl</artifactId>
      <version>${log4j-version}</version>
      <scope>runtime</scope>
    </dependency>
```

The following events emit log messages

- When reading the configuration from a `java.util.Properties` object, but the Properties contain some non-string entry, a WARNING message is emitted that this entry will be ignored.
- When reading the configuration from environment variables an INFO message is emitted for each entry that was mapped from an environment variable to a config key.
