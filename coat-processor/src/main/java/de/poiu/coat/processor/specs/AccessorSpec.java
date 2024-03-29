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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;


/**
 * Data model for an accessor (a method in a @Coat.Config annotated interface).
 * 
 */
@Value.Immutable
public abstract class AccessorSpec {
  public abstract ExecutableElement    accessor();

  public abstract String               key();
  public abstract String               defaultValue();
  public abstract boolean              mandatory();

  public abstract Optional<TypeMirror> converter();
  public abstract Optional<TypeMirror> listParser();

  public abstract String               methodName();

  public abstract TypeMirror           type();
  public abstract Optional<TypeMirror> collectionType();
}
