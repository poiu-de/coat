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

import de.poiu.coat.annotation.Coat;
import de.poiu.coat.processor.specs.AccessorSpec;
import de.poiu.coat.processor.specs.EmbeddedTypeSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.type.TypeKind.VOID;


/**
 * Helper class providing asserstions for Spec elements.
 * If an assertion is not met, an error is generated.
 */
public class Assertions {


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final ProcessingEnvironment pEnv;
  private final ElementHelper         elementHelper;
  private final TypeHelper            typeHelper;


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  public Assertions(final ProcessingEnvironment pEnv) {
    this.pEnv         = pEnv;
    this.elementHelper= new ElementHelper(pEnv);
    this.typeHelper   = new TypeHelper(pEnv);
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * Assert that all accessors have a return type (void is not allowed).
   *
   * @param accessors the accessors to check
   */
  public void assertReturnType(final List<AccessorSpec> accessors) {
    // filter out all accessors without return type
    accessors.stream()
      .map(AccessorSpec::accessor)
      .filter(this::hasVoidReturnType)
      .forEachOrdered(accessor ->
        pEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
          "Accessors must have a return type",
          accessor));
  }


  /**
   * Assert that the given type is an interface (classes, even abstract ones, are not supported
   * at the moment).
   *
   * @param annotatedType the type to test
   */
  public void assertIsInterface(final TypeElement annotatedType) {
    if (annotatedType.getKind() != INTERFACE) {
      pEnv.getMessager().printMessage(
        Diagnostic.Kind.ERROR,
        "@Coat.Config is only supported on interfaces at the moment",
        annotatedType);
    }
  }


  /**
   * Asserst that the return types of all embedded type accessors are annotated with a @Coat.Config
   * annotation.
   *
   * @param embeddedTypeSpecs the embedded type accessors to check
   */
  public void assertEmbeddedTypesAreAnnotated(final List<EmbeddedTypeSpec> embeddedTypeSpecs) {
    embeddedTypeSpecs.stream()
      .forEachOrdered(this::assertEmbeddedTypeIsAnnotated);
  }


  /**
   * Asserst that the return types of the given embedded type accessor isannotated with a @Coat.Config
   * annotation.
   *
   * @param embeddedTypeSpec the embedded type accessor to check
   */
  public void assertEmbeddedTypeIsAnnotated(final EmbeddedTypeSpec embeddedTypeSpec) {
    if (this.hasEmbeddedAnnotation(embeddedTypeSpec)
      && this.hasEmbeddedTypeWithoutAnnotation(embeddedTypeSpec)) {
      pEnv.getMessager().printMessage(
        Diagnostic.Kind.ERROR,
        "@Coat.Embedded annotation can only be applied to types that are annotated with @Coat.Config.",
        embeddedTypeSpec.accessor());
    }
  }


  /**
   * Assert that all embedded type accessors return a single object, not a collection.
   *
   * @param embeddedTypeSpecs the embedded type accessors to check
   */
  public void assertEmbeddedTypesAreNotInCollection(final List<EmbeddedTypeSpec> embeddedTypeSpecs) {
    embeddedTypeSpecs.stream()
      .forEachOrdered(this::assertEmbeddedTypeIsNotInCollection);
  }


  /**
   * Assert that the given embedded type accessor retursn a single object, not a collection.
   *
   * @param embeddedTypeSpec the embedded type accessor to check
   */
  public void assertEmbeddedTypeIsNotInCollection(final EmbeddedTypeSpec embeddedTypeSpec) {
    if (this.hasEmbeddedAnnotation(embeddedTypeSpec)
      && this.hasCollectionType(embeddedTypeSpec)) {
      pEnv.getMessager().printMessage(
        Diagnostic.Kind.ERROR,
        "Collection types are not supported for EmbeddedConfigs",
        embeddedTypeSpec.accessor());
    }
  }


  /**
   * Assert that all accessors are parameterless.
   *
   * @param aceessors the accessors to check
   */
  public void assertNoParameters(final List<AccessorSpec> aceessors) {
    // filter out all accessors with parameters
    aceessors.stream()
      .map(AccessorSpec::accessor)
      .filter(this::hasParameters)
      .forEachOrdered(accessor ->
        pEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Accessors must not have parameters",
          accessor));
  }


  /**
   * Assert that the keys (either explicitly specified in the @Coat.Param annotation) are unique.
   *
   * @param accessors the accessors to check
   */
  public void assertUniqueKeys(final List<AccessorSpec> accessors) {
    final Map<String, List<AccessorSpec>> existingKeys= new HashMap<>();
    final Set<AccessorSpec> duplicateKeys= new HashSet<>();

    // collect all keys and their corresponding accessor methods
    for (final AccessorSpec accessor : accessors) {
      if (!existingKeys.containsKey(accessor.key())) {
        existingKeys.put(accessor.key(), new ArrayList<>());
      }
      existingKeys.get(accessor.key()).add(accessor);
    }

    // filter out all keys with more than 1 accessor method
    existingKeys.entrySet().stream()
      .filter(e -> e.getValue().size() > 1)
      .forEach(e -> duplicateKeys.addAll(e.getValue()));

    // and generate an error message for each of them
    duplicateKeys.stream()
      .map(AccessorSpec::accessor)
      .forEach(accessor ->
        pEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Duplicate key",
          accessor)
      );
  }


  /**
   * Assert that the method names of all accessors are unique.
   *
   * @param accessors the accessors to check
   */
  public void assertUniqueMethodNames(final List<AccessorSpec> accessors) {
    final Map<String, List<AccessorSpec>> existingMethodNames= new HashMap<>();
    final Set<AccessorSpec> duplicateMethodNames= new HashSet<>();

    // collect all keys and their corresponding accessor methods
    for (final AccessorSpec accessor : accessors) {
      if (!existingMethodNames.containsKey(accessor.methodName())) {
        existingMethodNames.put(accessor.methodName(), new ArrayList<>());
      }
      existingMethodNames.get(accessor.methodName()).add(accessor);
    }

    // filter out all keys with more than 1 accessor method
    existingMethodNames.entrySet().stream()
      .filter(e -> e.getValue().size() > 1)
      .forEach(e -> duplicateMethodNames.addAll(e.getValue()));

    // and generate an error message for each of them
    duplicateMethodNames.stream()
      .map(AccessorSpec::accessor)
      .forEach(accessor ->
        pEnv.getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "Conflicting accessor methods",
          accessor)
      );
  }


  /**
   * Assert that methods annotated as embedded types don’t contain a @Coat.Param annotation at
   * the same time.
   *
   * @param accessors the accessors to check
   */
  public void assertOnlyEitherEmbeddedOrParamAnnotation(final List<EmbeddedTypeSpec> accessors) {
    accessors.stream()
      .filter(this::hasParamAnnotation)
      .forEachOrdered(accessorSpec ->
        pEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
          "@Param or @Embedded annotations are mutually exclusive",
          accessorSpec.accessor()));
  }


  /**
   * Assert that no primitive types are used for arrays.
   *
   * @param accessors the accessors to check
   */
  public void assertNoPrimitiveArrays(final List<AccessorSpec> accessors) {
    accessors.stream()
      .filter(this::hasPrimitiveArray)
      .forEachOrdered(accessorSpec ->
        pEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
          "Arrays of primitives are not supported. Use Lists instead.",
          accessorSpec.accessor()));
  }


  /**
   * Assert that only supported primitive types are used.
   *
   * @param accessors the accessors to check
   */
  public void assertOnlySupportedPrimitives(final List<AccessorSpec> accessors) {
    accessors.stream()
      .filter(this::isUnsupportedPrimitive)
      .forEachOrdered(accessorSpec ->
        pEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
          "Only the primitive types boolean, int, long and double are supported. Please use one of those or the corresponding object types.",
          accessorSpec.accessor()));
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


  private boolean hasVoidReturnType(final ExecutableElement elm) {
    return elm.getReturnType().getKind() == VOID;
  }


  private boolean hasParameters(final ExecutableElement elm) {
    return !elm.getParameters().isEmpty();
  }


  private boolean hasEmbeddedAnnotation(final EmbeddedTypeSpec embeddedSpec) {
    final Coat.Embedded[] embeddedAnnotations = embeddedSpec.accessor().getAnnotationsByType(Coat.Embedded.class);
    return embeddedAnnotations.length > 0;
  }


  private boolean hasParamAnnotation(final EmbeddedTypeSpec embeddedSpec) {
    final Coat.Param[] embeddedAnnotations = embeddedSpec.accessor().getAnnotationsByType(Coat.Param.class);
    return embeddedAnnotations.length > 0;
  }


  private boolean hasEmbeddedTypeWithoutAnnotation(final EmbeddedTypeSpec embeddedSpec) {
    final Coat.Config[] coatConfigAnnotations= embeddedSpec.classSpec().annotatedType().getAnnotationsByType(Coat.Config.class);
    return coatConfigAnnotations.length == 0;
  }


  private boolean hasCollectionType(final EmbeddedTypeSpec embeddedTypeSpec) {
    if (embeddedTypeSpec.enclosure().isEmpty()) {
      return false;
    }

    if (this.pEnv.getTypeUtils().isAssignable(embeddedTypeSpec.enclosure().get(), this.typeHelper.optionalType)) {
      return false;
    }

    return true;
  }


  private boolean hasPrimitiveArray(final AccessorSpec accessorSpec) {
    if (accessorSpec.collectionType().isEmpty()) {
      return false;
    }

    final TypeMirror collectionType= accessorSpec.collectionType().get();
    final boolean    isArray       = this.pEnv.getTypeUtils().isAssignable(collectionType, this.typeHelper.arrayTypeElement);
    final boolean    isPrimitive   = accessorSpec.type().getKind().isPrimitive();

    return isArray && isPrimitive;
  }


  private boolean isUnsupportedPrimitive(final AccessorSpec accessorSpec) {
    final TypeKind returnTypeKind= accessorSpec.type().getKind();
    if (!returnTypeKind.isPrimitive()) {
      return false;
    }


    return !this.typeHelper.supportedTypes.contains(returnTypeKind);
  }
}
