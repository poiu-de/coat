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

  private static class Either {
    private final Coat.Param coatParam;
    private final Coat.Embedded coatEmbedded;

    private Either(final Coat.Param coatParam, final Coat.Embedded coatEmbedded) {
      if ((coatParam == null && coatEmbedded == null)
        || (coatParam != null && coatEmbedded != null)){
        throw new IllegalArgumentException("Exactly one of coatParam or coatEmbedded must be non-null");
      }
      this.coatParam= coatParam;
      this.coatEmbedded= coatEmbedded;
    }

    public static Either of(final Coat.Param param) {
      return new Either(param, null);
    }
    public static Either of(final Coat.Embedded embedded) {
      return new Either(null, embedded);
    }

    public Coat.Param param() {
      return this.coatParam;
    }

    public Coat.Embedded embedded() {
      return this.coatEmbedded;
    }
  }


  public abstract ExecutableElement   annotatedMethod();

  public abstract String              methodeName();

  public abstract String              key();

  public abstract String              typeName();

  public abstract String              defaultValue();

  public abstract boolean             mandatory();

  public abstract boolean             embedded();


  public static ConfigParamSpec from(final Element annotatedMethod) {
    final ExecutableElement executableAnnotatedMethod = (ExecutableElement) annotatedMethod;
    final Either            coatAnnotation            = assertAnnotation(executableAnnotatedMethod);

    final TypeMirror returnTypeMirror = executableAnnotatedMethod.getReturnType();

    final String methodName   = executableAnnotatedMethod.getSimpleName().toString();
    final String key          = getKeyFrom(coatAnnotation);
    final String defaultValue = getDefaultValueFrom(coatAnnotation);
    final String typeName     = returnTypeMirror.toString(); // FIXME: Should be TypeMirror?
    final boolean isMandatory = !isOptional(typeName) && defaultValue != null;
    final boolean isEmbedded  = coatAnnotation.coatEmbedded != null;

    return ImmutableConfigParamSpec.builder()
      .annotatedMethod(executableAnnotatedMethod)
      .methodeName(methodName)
      .key(key)
      .typeName(typeName)
      .defaultValue(defaultValue)
      .mandatory(isMandatory)
      .embedded(isEmbedded)
      .build();
  }


  private static Either assertAnnotation(final ExecutableElement annotatedMethod) {
    final Coat.Param[] paramAnnotations = annotatedMethod.getAnnotationsByType(Coat.Param.class);
    final Coat.Embedded[] embeddedAnnotations = annotatedMethod.getAnnotationsByType(Coat.Embedded.class);

    if (paramAnnotations.length == 0 && embeddedAnnotations.length == 0) {
      throw new RuntimeException("Needs to be annotated with either @Coat.Param or @Coat.Embedded: " + annotatedMethod);
    } else if (paramAnnotations.length > 1) {
      throw new RuntimeException("Only 1 @Coat.Param annotation allowed: " + annotatedMethod);
    } else if (embeddedAnnotations.length > 1) {
      throw new RuntimeException("Only 1 @Coat.Embedded annotation allowed: " + annotatedMethod);
    } else if (paramAnnotations.length > 0 && embeddedAnnotations.length > 0) {
      throw new RuntimeException("Only one of @Coat.Param or @Coat.Embedded annotation allowed: " + annotatedMethod);
    }

    return new Either(
      paramAnnotations.length > 0 ? paramAnnotations[0] : null,
      embeddedAnnotations.length > 0 ? embeddedAnnotations[0] : null);
  }


  private static boolean isOptional(final String type) {
    return type.startsWith("java.util.Optional");
  }


  private static String getKeyFrom(final Either either) {
    if (either.coatParam != null) {
      return either.coatParam.key();
    } else {
      return either.coatEmbedded.key();
    }
  }

  private static String getDefaultValueFrom(final Either either) {
    if (either.coatParam != null) {
      return either.coatParam.defaultValue();
    } else {
      return "";
    }
  }
}
