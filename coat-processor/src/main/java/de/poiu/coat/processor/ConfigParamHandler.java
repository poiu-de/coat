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
package de.poiu.coat.processor;

import de.poiu.coat.annotation.Coat;
import de.poiu.coat.processor.casing.CasingStrategy;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;


public class ConfigParamHandler {

  private static class SurroundingAndEnclosingTypes {
    public final TypeMirror surrounding;
    public final TypeMirror enclosed;

    public SurroundingAndEnclosingTypes(final TypeMirror surrounding, final TypeMirror enclosed) {
      this.surrounding= surrounding;
      this.enclosed= enclosed;
    }

    @Override
    public String toString() {
      return surrounding + " < " + enclosed + " >";
    }
  }


  private final ProcessingEnvironment processingEnv;
  private final TypeMirror arrayTypeElement;
  private final TypeMirror listTypeElement;
  private final TypeMirror setTypeElement;
  private final TypeMirror optionalType;
  private final TypeMirror optionalIntType;
  private final TypeMirror optionalDoubleType;
  private final TypeMirror optionalLongType;


  public ConfigParamHandler(final ProcessingEnvironment processingEnv) {
    this.processingEnv     = processingEnv;
    this.arrayTypeElement  = this.processingEnv.getElementUtils().getTypeElement(Array.class.getCanonicalName()).asType();
    this.listTypeElement   = this.processingEnv.getElementUtils().getTypeElement(List.class.getCanonicalName()).asType();
    this.setTypeElement    = this.processingEnv.getElementUtils().getTypeElement(Set.class.getCanonicalName()).asType();
    this.optionalType      = this.processingEnv.getElementUtils().getTypeElement("java.util.Optional").asType();
    this.optionalIntType   = this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalInt").asType();
    this.optionalDoubleType= this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalDouble").asType();
    this.optionalLongType  = this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalLong").asType();
  }


  public ConfigParamSpec from(final Element annotatedMethod) {
    final ExecutableElement executableAnnotatedMethod = (ExecutableElement) annotatedMethod;
    final Coat.Param        coatParamAnnotation       = assertAnnotation(executableAnnotatedMethod);

    final TypeMirror returnTypeMirror = executableAnnotatedMethod.getReturnType();

    final String methodName                        = executableAnnotatedMethod.getSimpleName().toString();
    final String key                               = getOrInferKey(executableAnnotatedMethod, coatParamAnnotation);
    final String defaultValue                      = coatParamAnnotation != null ? coatParamAnnotation.defaultValue() : "";
    final SurroundingAndEnclosingTypes wrappedType = getWrappedType(returnTypeMirror);
    final TypeMirror type                          = returnTypeMirror;
    final Optional<TypeMirror> collectionType      = getCollectionType(wrappedType.surrounding);
    final boolean isMandatory                      = !isOptional(type) && defaultValue != null;

    return ImmutableConfigParamSpec.builder()
      .annotatedMethod(executableAnnotatedMethod)
      .methodeName(methodName)
      .key(key)
      .type(type)
      .defaultValue(defaultValue)
      .mandatory(isMandatory)
      .collectionType(collectionType)
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
    return inferKey(annotatedMethod, coatParamAnnotation);
  }


  private static String inferKey(final ExecutableElement annotatedMethod, final Coat.Param coatParamAnnotation) {
    final Element enclosingElement = annotatedMethod.getEnclosingElement();
    if (enclosingElement == null) {
      throw new RuntimeException("No enclosing element for annotatedMethod ”"+annotatedMethod.toString()+"”");
    }

    if (!(enclosingElement instanceof TypeElement)) {
      throw new RuntimeException("Enclosing element of annotatedMethod “"+annotatedMethod.toString()+"” of unexpected type: "+enclosingElement);
    }

    final TypeElement type= (TypeElement) enclosingElement;
    final Coat.Config coatConfigAnnotation = type.getAnnotation(Coat.Config.class);
    if (coatConfigAnnotation == null) {
      throw new RuntimeException("Enclosing element of annotatedMethod “"+annotatedMethod.toString()+"” does not have a @Coat.Config annotation: "+enclosingElement);
    }

    final CasingStrategy casingStrategy = coatConfigAnnotation.casing();
    return casingStrategy.convert(annotatedMethod.getSimpleName().toString());
  }


  private static boolean isOptional(final String type) {
    return type.startsWith("java.util.Optional");
  }


  private  boolean isOptional(final TypeMirror type) {
    if (type == null) {
      return false;
    }

    final TypeMirror erasure= this.processingEnv.getTypeUtils().erasure(type);

    return this.processingEnv.getTypeUtils().isAssignable(erasure, optionalType)
      || this.processingEnv.getTypeUtils().isAssignable(erasure, optionalIntType)
      || this.processingEnv.getTypeUtils().isAssignable(erasure, optionalLongType)
      || this.processingEnv.getTypeUtils().isAssignable(erasure, optionalDoubleType)
      ;
  }


  private Optional<TypeMirror> getCollectionType(final TypeMirror type) {
    if (type == null) {
      return Optional.empty();
    }

    // Optional is not a collection type and handled specially
    if (this.processingEnv.getTypeUtils().isAssignable(type, optionalType)) {
      return Optional.empty();
    }

    if (this.processingEnv.getTypeUtils().isAssignable(type, arrayTypeElement)) {
      return Optional.of(arrayTypeElement);
    }
    if (this.processingEnv.getTypeUtils().isAssignable(type, listTypeElement)) {
      return Optional.of(listTypeElement);
    }
    if (this.processingEnv.getTypeUtils().isAssignable(type, setTypeElement)) {
      return Optional.of(setTypeElement);
    }

    final StringBuilder sb= new StringBuilder("Unsupported collection type: ").append(type.toString());
    sb.append("\nOnly the following collection types are supported at the moment:");
    sb.append("\n\t").append(arrayTypeElement.toString());
    sb.append("\n\t").append(listTypeElement.toString());
    sb.append("\n\t").append(setTypeElement.toString());
    throw new CoatProcessorException(sb.toString());
  }


  private SurroundingAndEnclosingTypes getWrappedType(final TypeMirror type) {
    switch (type.getKind()) {
      case ARRAY:
        final ArrayType arrayType= (ArrayType) type;
        return new SurroundingAndEnclosingTypes(arrayTypeElement, arrayType.getComponentType());

      case DECLARED:
        final DeclaredType declaredType= (DeclaredType) type;
        final TypeMirror erasure = this.processingEnv.getTypeUtils().erasure(type);
        final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        final TypeMirror enclosedType= assertZeroOrOne(typeArguments);

        if (enclosedType == null) {
          return new SurroundingAndEnclosingTypes(null, type);
        } else {
          return new SurroundingAndEnclosingTypes(erasure, enclosedType);
        }
      default:
        return new SurroundingAndEnclosingTypes(null, type);
    }
  }

  private TypeMirror assertZeroOrOne(List<? extends TypeMirror> typeArguments) {
    switch (typeArguments.size()) {
      case 0:
        return null;
      case 1:
        return typeArguments.get(0);
      default:
        throw new RuntimeException("Only single type arguments are supported");
    }
  }

}
