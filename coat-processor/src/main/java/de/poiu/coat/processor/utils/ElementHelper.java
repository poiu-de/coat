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

import de.poiu.coat.processor.visitors.ClassTypeArrayVisitor;
import de.poiu.coat.processor.visitors.ClassTypeVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import static de.poiu.coat.processor.utils.ElementHelper.Defaults.LOAD_DEFAULT;
import static java.util.Collections.EMPTY_LIST;


/**
 * Helper for handling {@link javax.lang.model.element.Element}s.
 */
public class ElementHelper {
  public static enum Defaults {
    LOAD_DEFAULT,
    IGNORE_DEFAULT,
    ;
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final ProcessingEnvironment pEnv;
  private final TypeHelper            typeHelper;


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  public ElementHelper(final ProcessingEnvironment pEnv) {
    this.pEnv          = pEnv;
    this.typeHelper    = new TypeHelper(pEnv);
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * Get the annotation value of the given key in the given annotation.
   * <p>
   * If the annotation is null, null is returned.
   * <p>
   * The parameter “defaults” specifies whether the default value (if existant) should be returned
   * if the annotation does not contain that key.
   *
   * @param key        the key for which to return the value
   * @param annotation the annotation to search in (may be null)
   * @param defaults   whether to return the default value if the key is not specified in the annotation
   * @return
   */
  @Nullable
  public AnnotationValue getAnnotationValueOf(final String key,
                                              final @Nullable AnnotationMirror annotation,
                                              final Defaults defaults) {
    if (annotation == null) {
      return null;
    }

    final Map<? extends ExecutableElement, ? extends AnnotationValue> annotationParams;
    if (defaults == LOAD_DEFAULT) {
      annotationParams= this.pEnv.getElementUtils().getElementValuesWithDefaults(annotation);
    } else {
      annotationParams= annotation.getElementValues();
    }
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationParams.entrySet()) {
      final ExecutableElement k = entry.getKey();
      final AnnotationValue v = entry.getValue();

      if (k.getSimpleName().contentEquals(key)) {
        return v;
      }
    }

    return null;
  }


  /**
   * Get the annotation value of the given key in the given annotation as a list of TypeMirrors.
   * <p>
   * If the annotation is null, null is returned.
   * <p>
   * The parameter “defaults” specifies whether the default value (if existant) should be returned
   * if the annotation does not contain that key.
   *
   * @param key        the key for which to return the value
   * @param annotation the annotation to search in (may be null)
   * @param defaults   whether to return the default value if the key is not specified in the annotation
   * @return
   */
  public List<TypeMirror> getAnnotationValueAsTypeMirrorList(final String key,
                                                             final @Nullable AnnotationMirror annotation,
                                                             final Defaults defaults) {
    final AnnotationValue annotationValue = this.getAnnotationValueOf(key, annotation, defaults);
    if (annotationValue == null) {
      return EMPTY_LIST;
    }

    // FIXME: Such a visitor seems complicated and hard to understand. Do we really need it.
    //        Can’t we just cast annotationValue.getValue() to List<TypeMirror>?
    final List<TypeMirror> converters= new ArrayList<>();
    final ClassTypeArrayVisitor visitor= new ClassTypeArrayVisitor(converters);
    annotationValue.accept(visitor, null);

    return converters;
  }


  /**
   * Get the annotation value of the given key in the given annotation as a single TypeMirror.
   * <p>
   * If the annotation is null, null is returned.
   * <p>
   * The parameter “defaults” specifies whether the default value (if existant) should be returned
   * if the annotation does not contain that key.
   *
   * @param key        the key for which to return the value
   * @param annotation the annotation to search in (may be null)
   * @param defaults   whether to return the default value if the key is not specified in the annotation
   * @return
   */
  @Nullable
  public TypeMirror getAnnotationValueAsTypeMirror(final String key,
                                                   final @Nullable AnnotationMirror annotation,
                                                   final Defaults defaults) {
    final AnnotationValue annotationValue = this.getAnnotationValueOf(key, annotation, defaults);
    if (annotationValue == null) {
      return null;
    }

    // FIXME: Such a visitor seems complicated and hard to understand. Do we really need it.
    //        Can’t we just cast annotationValue.getValue() to TypeMirror?
    final ClassTypeVisitor visitor= new ClassTypeVisitor();
    return annotationValue.accept(visitor, null);
  }


  /**
   * Get the annotation value of the given key in the given annotation as a String.
   * <p>
   * Unlike the similar methods in this helper class this method <i>does not return null</i>!
   * If no value was specified it returns an empty String instead.
   * <p>
   * The parameter “defaults” specifies whether the default value (if existant) should be returned
   * if the annotation does not contain that key.
   *
   * @param key        the key for which to return the value
   * @param annotation the annotation to search in (may be null)
   * @param defaults   whether to return the default value if the key is not specified in the annotation
   * @return the value for the given key or an empty String
   */
  public String getAnnotationValueAsString(final String key,
                                           final @Nullable AnnotationMirror annotation,
                                           final Defaults defaults) {
    final AnnotationValue annotationValue = this.getAnnotationValueOf(key, annotation, defaults);
    if (annotationValue == null) {
      return "";
    }

    // FIXME: Such a visitor seems complicated and hard to understand. Do we really need it.
    //        Can’t we just cast annotationValue.getValue() to TypeMirror?
    return (String) annotationValue.getValue();
  }


  /**
   * Returns the default value for the given key in the given annotation.
   * <p>
   * If the annotation is null, null is returned.
   * <p>
   *
   * @param key        the key for which to return the value
   * @param annotation the annotation to search in (may be null)
   * @return
   */
  @Nullable
  public AnnotationValue getDefaultValue(final String key, final @Nullable AnnotationMirror annotation) {
    final var annotationParams = annotation.getElementValues();
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationParams.entrySet()) {
      final ExecutableElement k = entry.getKey();

      if (k.getSimpleName().contentEquals(key)) {
        return k.getDefaultValue();
      }
    }

    return null;
  }


  /**
   * Returns the annotation of the given type from the given element.
   * <p>
   * If no annotation of the given type was found on the element, null is returned.
   * <p>
   * Multiple annotations of the same type are not supported. Only the first one will be returned.
   *
   * @param annotationType the type of the annotation to return
   * @param element        the element on which to search for the annotation
   * @return
   */
  @Nullable
  public AnnotationMirror getAnnotation(final TypeMirror annotationType, final Element element) {
    for (final AnnotationMirror annotation : element.getAnnotationMirrors()) {
      final DeclaredType declaredType = annotation.getAnnotationType();
      if (this.pEnv.getTypeUtils().isAssignable(declaredType, annotationType)) {
        return annotation;
      }
    }

    return null;
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods

  /**
   * Returns the key element of a parameter in the given annotation.
   * <p>
   * If the annotation is null, null is returned.
   *
   * @param key        the key for which to return the element
   * @param annotation the annotation on which to search for the key
   * @return
   */
  @Nullable
  private ExecutableElement getAnnotationParam(final String key, final @Nullable AnnotationMirror annotation) {
    if (annotation == null) {
      return null;
    }

    final var annotationParams = annotation.getElementValues();
    for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationParams.entrySet()) {
      final ExecutableElement k = entry.getKey();
      final AnnotationValue v = entry.getValue();

      if (k.getSimpleName().contentEquals(key)) {
        return k;
      }
    }

    return null;
  }
}
