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

import java.util.List;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;



/**
 * Data model for a @Coat.Config annotated interface.
 *
 */
@Value.Immutable
public abstract class ClassSpec {
  public abstract TypeElement            annotatedType();
  public abstract List<AccessorSpec>     accessors();
  public abstract List<EmbeddedTypeSpec> embeddedTypes();

  public abstract String                 targetPackage();
  public abstract String                 enumName();
  public abstract String                 className();

  public abstract List<TypeMirror>       converters();
  public abstract Optional<TypeMirror>   listParser();

  public          String                 fqEnumName()  { return targetPackage() + "." + enumName(); }
  public          String                 fqClassName() { return targetPackage() + "." + className(); }
}
