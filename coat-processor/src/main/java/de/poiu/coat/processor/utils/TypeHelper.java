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
package de.poiu.coat.processor.utils;

import de.poiu.coat.annotation.Coat;
import de.poiu.coat.processor.CoatProcessorException;
import de.poiu.coat.processor.specs.EnclosedType;
import java.lang.reflect.Array;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import jakarta.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.BOOLEAN;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.DOUBLE;
import static javax.lang.model.type.TypeKind.INT;
import static javax.lang.model.type.TypeKind.LONG;


/**
 *
 * @author mherrn
 */
public class TypeHelper {


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final ProcessingEnvironment pEnv;

  public final TypeMirror arrayTypeElement;
  public final TypeMirror listTypeElement;
  public final TypeMirror setTypeElement;
  public final TypeMirror optionalType;
  public final TypeMirror optionalIntType;
  public final TypeMirror optionalDoubleType;
  public final TypeMirror optionalLongType;
  public final TypeMirror intType;
  public final TypeMirror doubleType;
  public final TypeMirror longType;
  public final TypeMirror coatConfigType;
  public final TypeMirror coatParamType;
  public final TypeMirror coatEmbeddedType;

  public final EnumSet<TypeKind> supportedPrimitiveTypes= EnumSet.of(BOOLEAN, INT, LONG, DOUBLE);


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  public TypeHelper(final ProcessingEnvironment pEnv) {
    this.pEnv              = pEnv;
    this.arrayTypeElement  = this.pEnv.getElementUtils().getTypeElement(Array.class.getCanonicalName()).asType();
    this.listTypeElement   = this.pEnv.getElementUtils().getTypeElement(List.class.getCanonicalName()).asType();
    this.setTypeElement    = this.pEnv.getElementUtils().getTypeElement(Set.class.getCanonicalName()).asType();
    this.optionalType      = this.pEnv.getElementUtils().getTypeElement(Optional.class.getCanonicalName()).asType();
    this.optionalIntType   = this.pEnv.getElementUtils().getTypeElement(OptionalInt.class.getCanonicalName()).asType();
    this.optionalDoubleType= this.pEnv.getElementUtils().getTypeElement(OptionalDouble.class.getCanonicalName()).asType();
    this.optionalLongType  = this.pEnv.getElementUtils().getTypeElement(OptionalLong.class.getCanonicalName()).asType();
    this.intType           = this.pEnv.getTypeUtils().getPrimitiveType(TypeKind.INT);
    this.doubleType        = this.pEnv.getTypeUtils().getPrimitiveType(TypeKind.DOUBLE);
    this.longType          = this.pEnv.getTypeUtils().getPrimitiveType(TypeKind.LONG);
    this.coatConfigType    = this.pEnv.getElementUtils().getTypeElement(Coat.Config.class.getCanonicalName()).asType();
    this.coatParamType     = this.pEnv.getElementUtils().getTypeElement(Coat.Param.class.getCanonicalName()).asType();
    this.coatEmbeddedType  = this.pEnv.getElementUtils().getTypeElement(Coat.Embedded.class.getCanonicalName()).asType();
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * Check whether the given type is one of the supported optional types.
   * <p>
   * If the given type is null, false is returned.
   *
   * @param type the type to check
   * @return
   */
  public boolean isOptional(final @Nullable TypeMirror type) {
    if (type == null) {
      return false;
    }

    final TypeMirror erasure= this.pEnv.getTypeUtils().erasure(type);

    return this.pEnv.getTypeUtils().isAssignable(erasure, optionalType)
      || this.pEnv.getTypeUtils().isAssignable(erasure, optionalIntType)
      || this.pEnv.getTypeUtils().isAssignable(erasure, optionalLongType)
      || this.pEnv.getTypeUtils().isAssignable(erasure, optionalDoubleType)
      ;
  }


  /**
   * Checks whether the given type is one of the supported optional primitive types.
   *
   * @param type
   * @return
   */
  public boolean isOptionalPrimitive(final @Nullable TypeMirror type) {
    if (type == null) {
      return false;
    }

    return this.pEnv.getTypeUtils().isAssignable(type, this.optionalIntType)
      || this.pEnv.getTypeUtils().isAssignable(type, this.optionalLongType)
      || this.pEnv.getTypeUtils().isAssignable(type, this.optionalDoubleType);
  }


  /**
   * Checks whether the given type is one of the supported primitive types.
   *
   * @param type
   * @return
   */
  public boolean isPrimitive(final @Nullable TypeMirror type) {
    if (type == null) {
      return false;
    }

    return type.getKind() == INT
      || type.getKind() == LONG
      || type.getKind() == DOUBLE
      || type.getKind() == BOOLEAN;
  }


  /**
   * Returns the collection type according to the given type.
   * <p>
   * If the given type is null, an empty Optional is returned.
   * <p>
   * If the given type is not a supported collection type, an empty Optional is returned.
   *
   * @param type the type to check
   * @return
   * @throws CoatProcessorException if the given type is not null, but also not one of the supported collection types
   */
  public Optional<TypeMirror> getCollectionType(final @Nullable TypeMirror type) {
    if (type == null) {
      return Optional.empty();
    }

    // Optional is not a collection type and handled specially
    if (this.pEnv.getTypeUtils().isAssignable(type, optionalType)) {
      return Optional.empty();
    }

    if (this.pEnv.getTypeUtils().isAssignable(type, arrayTypeElement)) {
      return Optional.of(arrayTypeElement);
    }
    if (this.pEnv.getTypeUtils().isAssignable(type, listTypeElement)) {
      return Optional.of(listTypeElement);
    }
    if (this.pEnv.getTypeUtils().isAssignable(type, setTypeElement)) {
      return Optional.of(setTypeElement);
    }

    final StringBuilder sb= new StringBuilder("Unsupported collection type: ").append(type.toString());
    sb.append("\nOnly the following collection types are supported at the moment:");
    sb.append("\n\t").append(arrayTypeElement.toString());
    sb.append("\n\t").append(listTypeElement.toString());
    sb.append("\n\t").append(setTypeElement.toString());
    throw new CoatProcessorException(sb.toString());
  }


  /**
   * Returns an EnclosedType instance for the given type.
   * <p>
   * If the given type is an array type, the enclosure will be an ArrayType,
   * the type will be the given type.
   * <p>
   * If the given type is a generic type, the enclosure will be the erasure of the given type
   * and the type will be the type argument.
   * Only a single type argument is supported
   * <p>
   * If the given type is one of the primitive optionals {@link OptionalInt}, {@link OptionalLong}
   * or {@link OptionalDouble}, the enclosure will be {@link Optional} and the type will be the
   * corresponding (non-optional) primitive type.
   * <p>
   * Otherwise the enclosure will be empty and the type will be the given type.
   *
   * @param type
   * @return
   * @throws CoatProcessorException if the given type has more than one type argument
   */
  public EnclosedType toEnclosedType(final TypeMirror type) {
    switch (type.getKind()) {
      case ARRAY:
        final ArrayType arrayType= (ArrayType) type;
        return EnclosedType.of(this.arrayTypeElement, arrayType.getComponentType());

      case DECLARED:
        // special handling of optional primitive types
        if (this.isOptionalPrimitive(type)) {
          final TypeMirror primitiveType= this.getNonOptionalPrimitiveType(type);
          return EnclosedType.of(this.optionalType, primitiveType);
        }

        // all other types (generic or not)
        final DeclaredType declaredType= (DeclaredType) type;
        final TypeMirror erasure = this.pEnv.getTypeUtils().erasure(type);
        final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        final TypeMirror enclosedType= this.assertZeroOrOne(typeArguments);

        if (enclosedType != null) {
          return EnclosedType.of(erasure, enclosedType);
        } else {
          return EnclosedType.of(null, type);
        }
      default:
        return EnclosedType.of(null, type);
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


  /**
   * Checks that the given list contains at max a single type argument and return it.
   * <p>
   * If the list contains more than one type argument a CoatProcessorException is thrown.
   *
   * @param typeArguments
   * @return
   * @throws CoatProcessorException
   */
  @Nullable
  private TypeMirror assertZeroOrOne(List<? extends TypeMirror> typeArguments) {
    switch (typeArguments.size()) {
      case 0:
        return null;
      case 1:
        return typeArguments.get(0);
      default:
        throw new CoatProcessorException("Only single type arguments are supported");
    }
  }


  /**
   * Returns the non-optional primitive type of the given optional primitive type.
   * If the given optionalPrimitiveType is not one of the supported optional primitive types,
   * it will be returned as is.
   *
   * @param optionalPrimitiveType
   * @return
   */
  private TypeMirror getNonOptionalPrimitiveType(final TypeMirror optionalPrimitiveType) {
    if (this.pEnv.getTypeUtils().isAssignable(optionalPrimitiveType, this.optionalIntType)) {
      return this.intType;
    }

    if (this.pEnv.getTypeUtils().isAssignable(optionalPrimitiveType, this.optionalLongType)) {
      return this.longType;
    }

    if (this.pEnv.getTypeUtils().isAssignable(optionalPrimitiveType, this.optionalDoubleType)) {
      return this.doubleType;
    }

    return optionalPrimitiveType;
  }


  /**
   * Returns whether the given type is a primitive type and is one of the supported primitives.
   * @param type
   * @return
   */
  public boolean isSupportedPrimitive(final TypeMirror type) {
    return this.supportedPrimitiveTypes.contains(type.getKind());
  }
}
