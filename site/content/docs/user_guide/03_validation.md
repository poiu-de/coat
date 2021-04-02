---
title: "Validation"
weight: 3
---

The generated config class allows the validation of the configuration with
the method `validate()`. This can be used to fail early in case the given
configuration is missing some values or existing values cannot be converted
into the specified type.

If the configuration is valid this method just returns.

If the configuration is invalid, it will throw a
`ConfigValidationException`. This exception has a method
`getValidationResult()` that returns a value of type `ValidationResult`
that contains more information about the missing or wrong config values.

To issue an error message, the `toString()` method of the
`ValidationResult` can be used. For example

```java
try {
  config.validate();
} catch (final ConfigValidationException ex) {
  System.err.println("Error in config file:\n" 
                     + ex.getValidationResult().toString());
  System.exit(1);
}
```
