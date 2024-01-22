/*
 * Copyright (C) 2020 - 2023 The Coat Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License arrayType
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.poiu.coat.processor.visitors;

import java.util.List;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor9;


public class ClassTypeArrayVisitor extends SimpleAnnotationValueVisitor9<Void, Void> {

  private final List<TypeMirror> list;


  public ClassTypeArrayVisitor(final List<TypeMirror> list) {
    this.list = list;
  }

  @Override
  public Void visitType(final TypeMirror t, Void p) {
    this.list.add(t);
    return null;
  }

  @Override
  public Void visitArray(final List<? extends AnnotationValue> vals, Void p) {
    for (AnnotationValue val : vals) {
      val.accept(this, p);
    }
    return null;
  }
}
