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
abstract class EmbeddedParamSpec {

  public abstract ExecutableElement   annotatedMethod();

  public abstract String              methodeName();

  public abstract String              key();

  public abstract TypeMirror          type();

  public abstract String              keySeparator();


  public static EmbeddedParamSpec from(final Element annotatedMethod) {
    final ExecutableElement executableAnnotatedMethod = (ExecutableElement) annotatedMethod;
    final Coat.Embedded     coatParamAnnotation       = assertAnnotation(executableAnnotatedMethod);

    final TypeMirror returnTypeMirror = executableAnnotatedMethod.getReturnType();

    final String methodName   = executableAnnotatedMethod.getSimpleName().toString();
    final String key          = coatParamAnnotation.key();
    final String keySeparator = coatParamAnnotation.keySeparator();
    final TypeMirror type     = returnTypeMirror;

    return ImmutableEmbeddedParamSpec.builder()
      .annotatedMethod(executableAnnotatedMethod)
      .methodeName(methodName)
      .key(key)
      .type(type)
      .keySeparator(keySeparator)
      .build();
  }


  private static Coat.Embedded assertAnnotation(final ExecutableElement annotatedMethod) {
    final Coat.Embedded[] annotationsByType = annotatedMethod.getAnnotationsByType(Coat.Embedded.class);

    // FIXME: Here we can check the validity of the annotation, don't we?

    if (annotationsByType.length == 0) {
      throw new RuntimeException("Needs to be annotated with @Coat.Param: " + annotatedMethod);
    } else if (annotationsByType.length > 1) {
      throw new RuntimeException("Only 1 @Coat.Param annotation allowed: " + annotatedMethod);
    }

    return annotationsByType[0];
  }
}
