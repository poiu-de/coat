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
import de.poiu.coat.casing.CasingStrategy;
import de.poiu.coat.processor.specs.AccessorSpec;
import de.poiu.coat.processor.specs.ClassSpec;
import de.poiu.coat.processor.specs.EmbeddedTypeSpec;
import de.poiu.coat.processor.specs.ImmutableAccessorSpec;
import de.poiu.coat.processor.visitors.TypeNameVisitor;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static de.poiu.coat.processor.utils.ElementHelper.Defaults.LOAD_DEFAULT;
import static javax.lang.model.type.TypeKind.DECLARED;


/**
 * Helper class for the {@link de.poiu.coat.processor.specs.SpecHandler}.
 */
public class SpecHelper {


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final ProcessingEnvironment pEnv;
  private final ElementHelper         elementHelper;
  private final TypeHelper            typeHelper;


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  public SpecHelper(final ProcessingEnvironment pEnv) {
    this.pEnv          = pEnv;
    this.elementHelper = new ElementHelper(pEnv);
    this.typeHelper    = new TypeHelper(pEnv);
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * Returns a list of all accessors found in the given ClassSpec and all of its embedded configs.
   *
   * @param classSpec
   * @return
   */
  public static List<AccessorSpec> getAccessorSpecsRecursively(final ClassSpec classSpec) {
    final List<AccessorSpec> result= new ArrayList<>();

    // collect all accessor methods of this interface
    result.addAll(classSpec.accessors());

    // collect the accessor methods of all embedded configs
    for (final EmbeddedTypeSpec eps : classSpec.embeddedTypes()) {
      final ClassSpec embeddedClassSpec = eps.classSpec();

      final List<AccessorSpec> embeddedAccessors= getAccessorSpecsRecursively(embeddedClassSpec);
      embeddedAccessors.stream()
        .map(ImmutableAccessorSpec::copyOf)
        .map(s -> s.withKey(eps.key() + eps.keySeparator() + s.key()))
        .forEachOrdered(result::add);
    }

    return result;
  }


  /**
   * Returns the config key for the given annotated method.
   * <p>
   * If the config key was given explicitly in a @Coat.Param or @Coat.Embedded annotation,
   * that one is returned.
   * Otherwise the config key is inferred from the method name.
   * <p>
   * For inferring the key a specified {@link Coat.Config#casing CasingStrategy} is applied and the
   * value of {@link Coat.Config#stripGetPrefix} is taken into account.
   *
   * @param annotatedMethod
   * @return
   */
  public String getOrInferKey(final ExecutableElement annotatedMethod) {
    final AnnotationMirror coatParamAnnotationMirror    = this.elementHelper.getAnnotation(this.typeHelper.coatParamType, annotatedMethod);
    final AnnotationMirror coatEmbeddedAnnotationMirror = this.elementHelper.getAnnotation(this.typeHelper.coatEmbeddedType, annotatedMethod);

    final String specifiedKey= this.getKeyFromAnnotation(coatParamAnnotationMirror, coatEmbeddedAnnotationMirror);
    if (specifiedKey != null && !specifiedKey.isEmpty()) {
      return specifiedKey;
}

    // if no key was explicity specified, infer it from the methods name
    return inferKey(annotatedMethod);
  }


  /**
   * Retruns the name of the collection type of the return type of the given accessor.
   * <p>
   * If the the return type of the accessor is not a supported collection type an empty Optional
   * will be returned.
   *
   * @param accessorSpec
   * @return
   */
  public Optional<String> getCollectionTypeName(final AccessorSpec accessorSpec) {
    if (accessorSpec.collectionType().isEmpty()) {
      return Optional.empty();
    }

    final TypeMirror collectionType = accessorSpec.collectionType().get();
    if (collectionType.getKind() == DECLARED) {
      return Optional.of(this.pEnv.getTypeUtils().erasure(collectionType).toString());
    }

    return Optional.of(collectionType.toString());
  }


  /**
   * Returns the name of the getter in the {@link de.poiu.coat.CoatConfig} class.
   * <p>
   * The actual name of the getter depends on the return type of the accessor (e.g. Collections
   * or Optional) and whether a default value exists.
   *
   * @param accessorSpec
   * @return
   */
  public String getSuperGetterName(final AccessorSpec accessorSpec) {
    final StringBuilder sb= new StringBuilder("get");

    final TypeMirror type= accessorSpec.type();
    final TypeNameVisitor visitor= new TypeNameVisitor();
    final String typeName= type.accept(visitor, null);

    final Optional<TypeMirror> collectionType= accessorSpec.collectionType();
    if (collectionType.isPresent()) {
      if (collectionType.get().toString().equals(Array.class.getCanonicalName())) {
        sb.append("Array");
      } else if (this.pEnv.getTypeUtils().isAssignable(collectionType.get(), this.typeHelper.listTypeElement)) {
        sb.append("List");
      } else if (this.pEnv.getTypeUtils().isAssignable(collectionType.get(), this.typeHelper.setTypeElement)) {
        sb.append("Set");
      }
    } else if (!accessorSpec.mandatory()) {
      sb.append("Optional");
    } else {
      // no else necessary
    }

    // primitive types have special getter methods with the type name included, e.g. getInt()
    if (this.typeHelper.isPrimitive(type)) {
      final String simpleTypeName= typeName.substring(typeName.lastIndexOf(".")+1);
      sb.append(NameUtils.upperFirstChar(simpleTypeName));
    }

    final boolean hasDefaultValue= !accessorSpec.defaultValue().isBlank();
    if (hasDefaultValue) {
      sb.append("OrDefault");
    }

    return sb.toString();
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods

  /**
   * Infer the key from the method name of the given method.
   * <p>
   * For inferring the key a specified {@link Coat.Config#casing CasingStrategy} is applied and the
   * value of {@link Coat.Config#stripGetPrefix} is taken into account.
   *
   * @param annotatedMethod
   * @return
   */
  private static String inferKey(final ExecutableElement annotatedMethod) {
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

    final boolean stripGetPrefix= coatConfigAnnotation.stripGetPrefix();
    final CasingStrategy casingStrategy = coatConfigAnnotation.casing();
    String accessorName= annotatedMethod.getSimpleName().toString();
    if (stripGetPrefix) {
      accessorName= stripGetPrefix(accessorName);
    }

    return casingStrategy.convert(accessorName);
  }


  /**
   * Strips a “get” prefix from the given accessor name.
   *
   * @param accessorName
   * @return
   */
  private static String stripGetPrefix(final String accessorName) {
    final Pattern PATTERN_ACCESSOR_NAME= Pattern.compile(""
      + "^(get)"      // the “get” prefix
      + "(\\p{Lu})"   // followed by an uppercase letter
      + "(.*)");      // and the rest of the name
    final Matcher matcher = PATTERN_ACCESSOR_NAME.matcher(accessorName);
    if (matcher.matches()) {
      return matcher.group(2).toLowerCase()
        + matcher.group(3);
    }

    return accessorName;
  }


  /**
   * Returns the “key” argument from the given annotations.
   * <p>
   * If the <code>coatParamAnnotation</code> is not null, the key (or its default value will be
   * taken from it.
   * Otherwise if <code>coatEmbeddedAnnotation</code> is not null, the key (or its default value
   * will be taken from it.
   * If both are null, null is returned.
   *
   * @param coatParamAnnotation
   * @param coatEmbeddedAnnotation
   * @return
   */
  @Nullable
  private String getKeyFromAnnotation(final AnnotationMirror coatParamAnnotation, final AnnotationMirror coatEmbeddedAnnotation) {
    if (coatParamAnnotation == null && coatEmbeddedAnnotation == null) {
      return null;
    }

    if (coatParamAnnotation != null) {
      return this.elementHelper.getAnnotationValueAsString("key", coatParamAnnotation, LOAD_DEFAULT);
    }

    if (coatEmbeddedAnnotation != null) {
      return this.elementHelper.getAnnotationValueAsString("key", coatEmbeddedAnnotation, LOAD_DEFAULT);
    }

    return null;
  }
}
