---
title: "Planned Features"
draft: true
expiryDate: 2023-05-30
---

### Support for arrays and collection types

At the moment it is not possible to use arrays or collections with generic
types in config classes. This is quite some drawback and it is planned to
be supported in the future.


### Make @Coat.Param annotations optional

At the moment every accessor method must be annotated with `@Coat.Param` to
specify the corresponding key. However it should be possible to derive this
key from the accessor methods name. In that case the `@Coat.Param`
annotation could be omitted.


### Declarative declaration of custom converters

Custom converters can only be specified programmatically and globally at
runtime. 

It would be nice to support a declarative declaration of custom converters
in the `@Coat.Config` and/or `@Coat.Param` annotation. 

This is a low priority goal as the actual real-world benefit is unclear.
