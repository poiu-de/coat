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

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.util.SimpleTypeVisitor9;


public class TypeNameVisitor extends SimpleTypeVisitor9<String, Void> {


  @Override
  public String visitDeclared(DeclaredType t, Void p) {
    return t.asElement().toString();
  }


  @Override
  public String visitPrimitive(PrimitiveType t, Void p) {
    switch (t.getKind()) {
      case BOOLEAN:
        return "boolean";
      case INT:
      return "int";
      case LONG:
        return "long";
      case DOUBLE:
        return "double";
      default:
        return t.getClass().getSimpleName();
    }
  }
}
