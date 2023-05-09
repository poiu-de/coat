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
package de.poiu.coat.processor;

import de.poiu.coat.annotation.Coat;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;




@Value.Style(stagedBuilder = true)
@Value.Immutable
abstract class ConfigParamSpec {

  public abstract ExecutableElement   annotatedMethod();

  public abstract String              methodeName();

  public abstract String              key();

  public abstract String              typeName();

  public abstract String              defaultValue();

  public abstract boolean             mandatory();


  public static ConfigParamSpec from(final Element annotatedMethod) {
    final ExecutableElement executableAnnotatedMethod = (ExecutableElement) annotatedMethod;
    final Coat.Param        coatParamAnnotation       = assertAnnotation(executableAnnotatedMethod);

    final TypeMirror returnTypeMirror = executableAnnotatedMethod.getReturnType();

    final String methodName   = executableAnnotatedMethod.getSimpleName().toString();
    final String key          = getOrInferKey(executableAnnotatedMethod, coatParamAnnotation);
    final String defaultValue = coatParamAnnotation != null ? coatParamAnnotation.defaultValue() : "";
    final String typeName     = returnTypeMirror.toString(); // FIXME: Should be TypeMirror?
    final boolean isMandatory = !isOptional(typeName) && defaultValue != null;

    return ImmutableConfigParamSpec.builder()
      .annotatedMethod(executableAnnotatedMethod)
      .methodeName(methodName)
      .key(key)
      .typeName(typeName)
      .defaultValue(defaultValue)
      .mandatory(isMandatory)
      .build();
  }


  private static Coat.Param assertAnnotation(final ExecutableElement annotatedMethod) {
    final Coat.Param[] annotationsByType = annotatedMethod.getAnnotationsByType(Coat.Param.class);

    if (annotationsByType.length == 0) {
      return null;
    }

    if (annotationsByType.length > 1) {
      throw new RuntimeException("Only 1 @Coat.Param annotation allowed: " + annotatedMethod);
    }

    return annotationsByType[0];
  }


  private static String getOrInferKey(final ExecutableElement annotatedMethod, final Coat.Param coatParamAnnotation) {
    final String specifiedKey= coatParamAnnotation != null ? coatParamAnnotation.key() : "";
    if (!specifiedKey.isEmpty()) {
      return specifiedKey;
    }

    // if no key was explicity specified, infer it from the methods name
    return annotatedMethod.getSimpleName().toString();
  }


  private static boolean isOptional(final String type) {
    return type.startsWith("java.util.Optional");
  }
}
