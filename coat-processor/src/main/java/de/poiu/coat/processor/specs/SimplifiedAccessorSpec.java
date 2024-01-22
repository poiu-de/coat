/*
 * Copyright (C) 2020 - 2021 The Coat Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.poiu.coat.processor.specs;

import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;


/**
 * A class for easier detection of duplicate accessors.
 * A duplicate accessor is one where all properties are equal, but the actual accessor is different.
 * This can be the case when one Coat Config interface inherits another one and both specify the same
 * accessor properties.
 */
@Value.Immutable
public abstract class SimplifiedAccessorSpec {
  @Value.Auxiliary
  public abstract AccessorSpec    accessor();

  public abstract String          methodName();
  public abstract String          key();
  public abstract String          defaultValue();
  public abstract TypeMirror      type();
  public abstract boolean         mandatory();

  public static SimplifiedAccessorSpec from(final AccessorSpec accessorSpec) {
    return ImmutableSimplifiedAccessorSpec.builder()
      .accessor(accessorSpec)
      .methodName(accessorSpec.methodName())
      .key(accessorSpec.key())
      .defaultValue(accessorSpec.defaultValue())
      .type(accessorSpec.type())
      .mandatory(accessorSpec.mandatory())
      .build();
  }
}
