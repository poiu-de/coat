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
 * Data model for an embedded type (a method in a @Coat.Config annotated interface with a
 * @Coat.Embedded annotation).
 */
@Value.Immutable
public abstract class EmbeddedTypeSpec {
  public abstract ExecutableElement    accessor();
  public abstract ClassSpec            classSpec();
  public abstract Optional<TypeMirror> enclosure();

  public abstract String               key();
  public abstract String               keySeparator();
  public abstract boolean              mandatory();

  public abstract String               methodName();

  public abstract TypeMirror           type();


}
