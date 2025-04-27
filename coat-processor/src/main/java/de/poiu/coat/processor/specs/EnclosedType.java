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

import java.util.Optional;
import jakarta.annotation.Nullable;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;


/**
 * Data holder for a type (the actually important return type of accessors in Coat) that may or may
 * not be the type argument of some container class. Container classes may be collection types
 * or Optional.
 * <p>
 * This data holder only supports a single type argument for the container class (the enclosure).
 */
@Value.Immutable
public abstract class EnclosedType {
  /** The container class */
  public abstract Optional<TypeMirror> enclosure();
  /** The actual type */
  public abstract TypeMirror           type();


  /**
   * Creates a new EnclosedType with the given enclosure and type.
   *
   * @param enclosure the container class (may be null)
   * @param type the actual type
   * @return
   */
  public static EnclosedType of(final @Nullable TypeMirror enclosure, final TypeMirror type) {
    return ImmutableEnclosedType.builder()
      .enclosure(Optional.ofNullable(enclosure))
      .type(type)
      .build();
  }
}
