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
package de.poiu.coat.processor.codegeneration;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import de.poiu.coat.CoatParam;
import de.poiu.coat.processor.CoatProcessorException;
import de.poiu.coat.processor.specs.AccessorSpec;
import de.poiu.coat.processor.specs.ClassSpec;
import de.poiu.coat.processor.utils.ElementHelper;
import de.poiu.coat.processor.utils.JavadocHelper;
import de.poiu.coat.processor.utils.NameUtils;
import de.poiu.coat.processor.utils.SpecHelper;
import de.poiu.coat.processor.utils.TypeHelper;
import java.io.IOException;
import java.util.List;
import jakarta.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;


/**
 * Helper class for the code generation of the implementation of a param enum.
 */
class ParamImplCodeGenerator {


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final ProcessingEnvironment pEnv;
  private final SpecHelper            specHelper;
  private final TypeHelper            typeHelper;
  private final ElementHelper         elementHelper;


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  public ParamImplCodeGenerator(final ProcessingEnvironment pEnv) {
    this.pEnv                 = pEnv;
    this.specHelper           = new SpecHelper(pEnv);
    this.typeHelper           = new TypeHelper(pEnv);
    this.elementHelper        = new ElementHelper(pEnv);
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * Generate the enum for the given classSpec.
   *
   * @param classSpec the classSpec (the annotated interface) for which to generate the enum
   * @throws IOException if writing the enum to file failed
   */
  @Nullable
  public TypeSpec generateEnumCode(final ClassSpec classSpec) {
    final TypeSpec.Builder typeSpecBuilder = TypeSpec.enumBuilder("ParamImpl")
      .addModifiers(PRIVATE, STATIC)
      .addSuperinterface(ClassName.get(CoatParam.class))
      .addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .addParameter(String.class,     "key",            FINAL)
        .addParameter(Class.class,      "type",           FINAL)
        .addParameter(Class.class,      "collectionType", FINAL)
        .addParameter(String.class,     "defaultValue",   FINAL)
        .addParameter(TypeName.BOOLEAN, "mandatory",      FINAL)
        .addParameter(Class.class,      "converter",      FINAL)
        .addParameter(Class.class,      "listParser",     FINAL)
        .addStatement("this.$N = $N", "key",            "key")
        .addStatement("this.$N = $N", "type",           "type")
        .addStatement("this.$N = $N", "collectionType", "collectionType")
        .addStatement("this.$N = $N", "defaultValue",   "defaultValue")
        .addStatement("this.$N = $N", "mandatory",      "mandatory")
        .addStatement("this.$N = $N", "converter",      "converter")
        .addStatement("this.$N = $N", "listParser",     "listParser")
        .build())
      ;

    this.addFieldAndAccessor(typeSpecBuilder, String.class,     "key");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "type");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "collectionType");
    this.addFieldAndAccessor(typeSpecBuilder, String.class,     "defaultValue");
    this.addFieldAndAccessor(typeSpecBuilder, boolean.class,    "mandatory");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "converter");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "listParser");

    if (classSpec.accessors().isEmpty()) {
      // not creating an enum if there is no accessor for it
      return null;
    }

    for (final AccessorSpec accessorSpec : classSpec.accessors()) {
      this.addEnumConstant(typeSpecBuilder, accessorSpec);
    }

    return typeSpecBuilder.build();
  }


  private void addFieldAndAccessor(final TypeSpec.Builder enumBuilder, final Class fieldType, final String fieldName) {
    enumBuilder
      .addField(fieldType, fieldName, PRIVATE, FINAL)
      .addMethod(MethodSpec.methodBuilder(fieldName)
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(fieldType)
        .addStatement("return this.$N", fieldName)
        .build()
      );
  }


  private void addEnumConstant(final TypeSpec.Builder typeSpecBuilder, final AccessorSpec accessorSpec) {
    final ExecutableElement accessor = accessorSpec.accessor();

    final String constName= NameUtils.toConstName(accessorSpec.key());

    final TypeSpec.Builder enumConstBuilder =
      TypeSpec.anonymousClassBuilder("$S, $L.class, $L, $S, $L, $L, $L",
                                     accessorSpec.key(),
                                     toBaseType(accessorSpec),
                                     this.specHelper.getCollectionTypeName(accessorSpec).map(c -> c+".class").orElse(null),
                                     accessorSpec.defaultValue() != null && !accessorSpec.defaultValue().trim().isEmpty()
                                       ? accessorSpec.defaultValue()
                                       : null,
                                     accessorSpec.mandatory(),
                                     accessorSpec.converter()
                                       .map(TypeMirror::toString)
                                       .map(s -> s + ".class")
                                       .orElse("null"),
                                     accessorSpec.listParser()
                                       .map(TypeMirror::toString)
                                       .map(s -> s + ".class")
                                       .orElse("null")
                                     );

    final String javadoc= this.pEnv.getElementUtils().getDocComment(accessor);
    if (javadoc != null) {
      enumConstBuilder.addJavadoc(JavadocHelper.stripBlockTagsFromJavadoc(javadoc));
    }

    typeSpecBuilder.addEnumConstant(constName, enumConstBuilder.build());
  }


  /**
   * Returns the non-optional return type of the given accessor as String.
   * <p>
   * For example the following types will result in the following Strings:
   * <table>
   * <tr><td>int</td>                     <td>⇒</td> <td>int</td></tr>
   * <tr><td>OptionalInt</td>             <td>⇒</td> <td>int</td></tr>
   * <tr><td>String</td>                  <td>⇒</td> <td>String</td></tr>
   * <tr><td>Charset</td>                 <td>⇒</td> <td>Charset</td></tr>
   * <tr><td>Optional&lt;Charset&gt;</td> <td>⇒</td> <td>Charset</td></tr>
   * <tr><td>Path[]</td>                  <td>⇒</td> <td>Path</td></tr>
   * </table>
   *
   * @param accessorSpec
   * @return
   */
  private String toBaseType(final AccessorSpec accessorSpec) {
    final TypeMirror type = accessorSpec.type();


    if (type.getKind() == TypeKind.INT || this.pEnv.getTypeUtils().isSameType(type, this.typeHelper.optionalIntType)) {
      return int.class.getName();
    }

    if (type.getKind() == TypeKind.LONG || this.pEnv.getTypeUtils().isSameType(type, this.typeHelper.optionalLongType)) {
      return long.class.getName();
    }

    if (type.getKind() == TypeKind.DOUBLE || this.pEnv.getTypeUtils().isSameType(type, this.typeHelper.optionalDoubleType)) {
      return double.class.getName();
    }

    if (type.getKind() == TypeKind.BOOLEAN) {
      return boolean.class.getName();
    }

    if (type.getKind() == DECLARED) {
      final DeclaredType declaredType= (DeclaredType) type;

      final TypeMirror erasure = this.pEnv.getTypeUtils().erasure(type);
      final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

      if (this.pEnv.getTypeUtils().isAssignable(erasure, this.typeHelper.optionalType)) {
        if (typeArguments.size() < 1) {
          throw new CoatProcessorException("Optionals without type argument are not supported.", accessorSpec.accessor());
        }
        if (typeArguments.size() > 1) {
          throw new CoatProcessorException("Optionals with multiple type arguments are not expected.", accessorSpec.accessor());
        }

        final TypeMirror typeArg = typeArguments.get(0);
        if (typeArg.getKind() == DECLARED) {
          final DeclaredType dc= (DeclaredType) typeArg;
          return dc.asElement().toString();
        } else {
          return typeArg.toString();
        }
      }

      return declaredType.asElement().toString();
    }

    if (type.getKind() == ARRAY) {
      final ArrayType arraysType= (ArrayType) type;
      final TypeMirror componentType = arraysType.getComponentType();

      return this.pEnv.getTypeUtils().asElement(componentType).toString();
    }

    return type.toString();
  }
}
