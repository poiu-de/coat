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
package de.poiu.coat.processor.utils;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import de.poiu.coat.annotation.Coat;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;


/**
 * Util classes for functionality related to naming of generated artifacts.
 *
 */
public class NameUtils {


  /**
   * Returns the given String <code>s</code> with the first char in uppercase.
   *
   * @param s the string to uppercase
   * @return the given string with the first char in uppercase
   */
  public static String upperFirstChar(final String s) {
    return String.valueOf(s.charAt(0)).toUpperCase() + s.subSequence(1, s.length());
  }


  /**
   * Returns the const-name for the given method name to be used in the generated enum.
   * <p>
   * This will uppercase all characters and insert underscores before every character that already
   * was uppercased. Therefore the given method name should adhere to the common java naming conventions.
   *
   * @param methodName the name of the method for which to return the const name
   * @return the const name for the given method name
   */
  public static String toConstName(final String methodName) {
    final StringBuilder sb= new StringBuilder();

    for (int i=0; i < methodName.length(); i++) {
      final char c= methodName.charAt(i);

      if (Character.isUpperCase(c)) {
        sb.append('_');
        sb.append(c);
      } else {
        sb.append(Character.toUpperCase(c));
      }
    }

    return sb.toString();
  }


  /**
   * Return a class name for the given type element to be used for the generated config class.
   * <p>
   * The name will be taken from the “className” property of the @Coat.Config annotation if the type,
   * if given. Otherwise the class name will be derived from the given type. In that case, if the
   * type name starts with an underscore, the underscore will just be removed. Otherwise the type name
   * will be prepended by “Immutable”.
   *
   * @param annotatedInterface the type for which to generate the config class name
   * @return the derived config class name
   */
  public static String deriveGeneratedClassName(final TypeElement annotatedInterface) {
    final String interfaceName= annotatedInterface.getSimpleName().toString();
    final Coat.Config typeAnnotation= annotatedInterface.getAnnotation(Coat.Config.class);

    final String className;
    if (typeAnnotation != null && !typeAnnotation.className().trim().isEmpty()) {
      className= typeAnnotation.className();
    } else {
      if (interfaceName.startsWith("_")) {
        className= interfaceName.substring(1);
      } else {
        className= "Immutable" + interfaceName;
      }
    }

    return className;
  }


  /**
   * Return an enum name for the given type element to be used for the generated config class.
   * <p>
   * This is done by appending “Param” to the base name of the enums name.
   * The base name will be taken from the “className” property of the @Coat.Config annotation if the type,
   * if given. Otherwise the class name will be derived from the given type. In that case, if the
   * type name starts with an underscore, the underscore will just be removed. Otherwise the base name
   * is the same as the type name.
   *
   * @param annotatedInterface the type for which to generate the enum name
   * @return the derived enum name
   */
  public static String deriveGeneratedEnumName(final TypeElement annotatedInterface) {
    final String interfaceName= annotatedInterface.getSimpleName().toString();
    final Coat.Config typeAnnotation= annotatedInterface.getAnnotation(Coat.Config.class);

    final String enumName;
    if (typeAnnotation != null && typeAnnotation.className() != null && !typeAnnotation.className().trim().isEmpty()) {
      enumName= typeAnnotation.className() + "Param";
    } else {
      if (interfaceName.startsWith("_")) {
        enumName= interfaceName.substring(1) + "Param";
      } else {
        enumName= interfaceName + "Param";
      }
    }

    return enumName;
  }
}
