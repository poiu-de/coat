/*
 * Copyright (C) 2020 - 2024 The Coat Authors
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

import de.poiu.coat.annotation.Coat;
import de.poiu.coat.processor.utils.NameUtils;
import de.poiu.coat.processor.utils.ElementHelper;
import de.poiu.coat.processor.utils.SpecHelper;
import de.poiu.coat.processor.utils.TypeHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static de.poiu.coat.processor.utils.ElementHelper.Defaults.IGNORE_DEFAULT;
import static de.poiu.coat.processor.utils.ElementHelper.Defaults.LOAD_DEFAULT;
import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.toList;


/**
 * Helper class for creating (@link ClassSpec}, {@link AccessorSpec} and {@link EmbeddedTypeSpec}
 * types from the corresponding elements.
 */
public class SpecHandler {


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final Map<Name, ClassSpec> GENERATED_CLASS_SPECS= new HashMap<>();

  private final ProcessingEnvironment pEnv;
  private final Types                 typeUtils;
  private final Elements              elementUtils;
  private final TypeHelper            typeHelper;
  private final ElementHelper         elementHelper;
  private final SpecHelper            specHelper;


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  public SpecHandler(final ProcessingEnvironment pEnv) {
    this.pEnv         = pEnv;
    this.typeUtils    = pEnv.getTypeUtils();
    this.elementUtils = pEnv.getElementUtils();
    this.typeHelper   = new TypeHelper(pEnv);
    this.elementHelper= new ElementHelper(pEnv);
    this.specHelper   = new SpecHelper(pEnv);
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Main converters methods

  /**
   * Creates a class spec for a TypeElement.
   *
   * @param annotatedType
   * @return
   */
  public ClassSpec classSpecFrom(final TypeElement annotatedType) {
    if (!GENERATED_CLASS_SPECS.containsKey(annotatedType.getQualifiedName())) {
      final AnnotationMirror coatConfigAnnotation = this.elementHelper.getAnnotation(this.typeHelper.coatConfigType, annotatedType);

      if (coatConfigAnnotation == null) {
        return unsupported(annotatedType);
      }

      final List<AccessorSpec>        accessorSpecs = this.createAccessorSpecsRecursively(annotatedType);
      final List<EmbeddedTypeSpec>    embeddedSpecs = this.createEmbeddedTypeSpecs(annotatedType);
      final String                    targetPackage = this.pEnv.getElementUtils().getPackageOf(annotatedType).getQualifiedName().toString();
      final String                    builderName   = NameUtils.deriveGeneratedBuilderName(annotatedType);
      final List<TypeMirror>          converter     = this.elementHelper.getAnnotationValueAsTypeMirrorList("converters", coatConfigAnnotation, IGNORE_DEFAULT);
      final Optional<TypeMirror>      listParser    = Optional.ofNullable(this.elementHelper.getAnnotationValueAsTypeMirror("listParser", coatConfigAnnotation, IGNORE_DEFAULT));

      final ClassSpec generatedClassSpec= ImmutableClassSpec.builder()
        .annotatedType(annotatedType)
        .accessors(accessorSpecs)
        .embeddedTypes(embeddedSpecs)
        .targetPackage(targetPackage)
        .builderName(builderName)
        .converters(converter)
        .listParser(listParser)
        .build();

      GENERATED_CLASS_SPECS.put(annotatedType.getQualifiedName(), generatedClassSpec);
    }

    return GENERATED_CLASS_SPECS.get(annotatedType.getQualifiedName());
  }


  /**
   * Creates an accessor spec for an ExecutableElement.
   *
   * @param accessor
   * @return
   */
  public AccessorSpec accessorSpecFrom(final ExecutableElement accessor) {
    final AnnotationMirror coatParamAnnotation = this.elementHelper.getAnnotation(this.typeHelper.coatParamType, accessor);
    final EnclosedType     returnType          = this.typeHelper.toEnclosedType(accessor.getReturnType());

    final String               key             = this.specHelper.getOrInferKey(accessor);
    final String               defaultValue    = this.elementHelper.getAnnotationValueAsString("defaultValue", coatParamAnnotation, LOAD_DEFAULT);
    final boolean              mandatory       = !this.typeHelper.isOptional(accessor.getReturnType());
    final Optional<TypeMirror> converter       = Optional.ofNullable(this.elementHelper.getAnnotationValueAsTypeMirror("converter", coatParamAnnotation, IGNORE_DEFAULT));
    final Optional<TypeMirror> listParser      = Optional.ofNullable(this.elementHelper.getAnnotationValueAsTypeMirror("listParser", coatParamAnnotation, IGNORE_DEFAULT));
    final String               methodName      = accessor.getSimpleName().toString();
    final TypeMirror           type            = returnType.type();
    final Optional<TypeMirror> collectionType  = this.typeHelper.getCollectionType(returnType.enclosure().orElse(null));

    return ImmutableAccessorSpec.builder()
      .accessor(accessor)
      .key(key)
      .defaultValue(defaultValue)
      .mandatory(mandatory)
      .converter(converter)
      .listParser(listParser)
      .methodName(methodName)
      .type(type)
      .collectionType(collectionType)
      .build();
  }


  /**
   * Creates an embedded type spec for an ExecutableElement.
   *
   * @param accessor
   * @return
   */
  public EmbeddedTypeSpec embeddedTypeSpecFrom(final ExecutableElement accessor) {
    final AnnotationMirror coatEmbeddedAnnotation = this.elementHelper.getAnnotation(this.typeHelper.coatEmbeddedType, accessor);
    final EnclosedType     returnType             = this.typeHelper.toEnclosedType(accessor.getReturnType());

    final EnclosedType fullEmbeddedType           = this.typeHelper.toEnclosedType(accessor.getReturnType());
    final TypeElement  fullEmbeddedTypeElement    = (TypeElement) this.typeUtils.asElement(fullEmbeddedType.type());
    final ClassSpec    embeddedClassSpec          = this.classSpecFrom(fullEmbeddedTypeElement);
    final String       key                        = this.specHelper.getOrInferKey(accessor);
    final String       keySeparator               = this.elementHelper.getAnnotationValueAsString("keySeparator", coatEmbeddedAnnotation, LOAD_DEFAULT);
    final boolean      mandatory                  = !this.typeHelper.isOptional(accessor.getReturnType());
    final String       methodName                 = accessor.getSimpleName().toString();
    final TypeMirror   type                       = returnType.type();
    // FIXME: Test that Collection of Embedded types are not supported (do we need to store the collection type?)

    return ImmutableEmbeddedTypeSpec.builder()
      .accessor(accessor)
      .classSpec(embeddedClassSpec)
      .enclosure(fullEmbeddedType.enclosure())
      .key(key)
      .keySeparator(keySeparator)
      .mandatory(mandatory)
      .methodName(methodName)
      .type(type)
      .build();
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


  /**
   * Creates the accessor specs for all accessors in a TypeElement.
   *
   * @param annotatedType
   * @return
   */
  private List<AccessorSpec> createAccessorSpecs(final TypeElement annotatedType) {
    return annotatedType.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Embedded.class) == null)
      .map(ExecutableElement.class::cast)
      .map(this::accessorSpecFrom)
      .collect(toList());
  }


  /**
   * Creates the accessor specs for all accessors in a TypeElement and all the inherited ones.
   * <p>
   * If an accessor is defined in multiple interfaces in the inheritance hierarchy it will only
   * be returned once in the result of this method.
   *
   * @param annotatedType
   * @return
   */
  private List<AccessorSpec> createAccessorSpecsRecursively(final TypeElement annotatedType) {
    final List<AccessorSpec> accessors= new ArrayList<>();

    final List<AccessorSpec> directAccessors= annotatedType.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Embedded.class) == null)
      .map(ExecutableElement.class::cast)
      .map(this::accessorSpecFrom)
      .collect(toList());
    accessors.addAll(directAccessors);

    final List<AccessorSpec> inheritedAccessors= annotatedType.getInterfaces().stream()
      .map(this.typeUtils::asElement)
      .map(TypeElement.class::cast)
      .map(this::createAccessorSpecsRecursively)
      .flatMap(List::stream)
      .collect(toList());
    accessors.addAll(inheritedAccessors);

    this.removeDuplicteAccessors(accessors);

    return accessors;
  }


  /**
   * Creates the embedded type specs for all accordingly annotated accessors in a TypeElement.
   *
   * @param annotatedType
   * @return
   */
  private List<EmbeddedTypeSpec> createEmbeddedTypeSpecs(final TypeElement annotatedType) {
    return annotatedType.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Embedded.class) != null)
      .map(ExecutableElement.class::cast)
      .map(this::embeddedTypeSpecFrom)
      .collect(toList());
  }


  /**
   * Remove duplicate accessors from the given collection of accessors.
   * <p>
   * Such duplicates may exist if the same method is specified in different classes in an
   * inheritance hierarchy.
   *
   * @param accessors
   */
  private void removeDuplicteAccessors(final Collection<AccessorSpec> accessors) {
    final Set<SimplifiedAccessorSpec> uniqueAccessors= new HashSet<>();

    for (final Iterator<AccessorSpec> it= accessors.iterator(); it.hasNext();) {
      final AccessorSpec accessor= it.next();
      final SimplifiedAccessorSpec simplifiedAccessor = SimplifiedAccessorSpec.from(accessor);
      if (uniqueAccessors.contains(simplifiedAccessor)) {
        // ignore accessor if we already have one with the same properties
        it.remove();
      } else {
        uniqueAccessors.add(simplifiedAccessor);
      }
    }
  }


  /**
   * Creates a class spec for an unsupported TypeElement.
   * <p>
   * This is used for methods annotated with a @Coat.Embedded annotation, but with an invalid
   * return type.
   *
   * @param typeElement
   * @return
   */
  private ClassSpec unsupported(final TypeElement typeElement) {
    return ImmutableClassSpec.builder()
      .annotatedType(typeElement)
      .accessors(EMPTY_LIST)
      .embeddedTypes(EMPTY_LIST)
      .targetPackage("unsupportedpackage")
      .builderName("UnsupportedBuilder"+typeElement.getSimpleName().toString())
      .build();
  }
}
