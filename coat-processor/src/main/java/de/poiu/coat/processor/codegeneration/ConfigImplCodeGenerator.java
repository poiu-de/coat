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
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import de.poiu.coat.AbstractImmutableCoatConfig;
import de.poiu.coat.processor.specs.AccessorSpec;
import de.poiu.coat.processor.specs.ClassSpec;
import de.poiu.coat.processor.specs.EmbeddedTypeSpec;
import de.poiu.coat.processor.utils.ElementHelper;
import de.poiu.coat.processor.utils.SpecHelper;
import de.poiu.coat.processor.utils.TypeHelper;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.tools.Diagnostic;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;


/**
 * Helper class for the code generation of the implementation of a config interface.
 */
class ConfigImplCodeGenerator {


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

  public ConfigImplCodeGenerator(final ProcessingEnvironment pEnv) {
    this.pEnv                 = pEnv;
    this.specHelper           = new SpecHelper(pEnv);
    this.typeHelper           = new TypeHelper(pEnv);
    this.elementHelper        = new ElementHelper(pEnv);
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods


  /**
   * Generate the config class implementation for the given ClassSpec.
   *
   * @param classSpec the ClassSpec (the annotated interface) for which to generate the config class
   * @return the generated TypeSpec
   * @throws IOException if writing the config class to file failed
   */
  public TypeSpec generateClassCode(final ClassSpec classSpec) {
    final TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder("ConfigImpl")
      .addModifiers(PRIVATE, STATIC)
      .addSuperinterface(ClassName.get(classSpec.annotatedType()))
      .superclass(AbstractImmutableCoatConfig.class)
      ;

    this.addPrivateFields(typeSpecBuilder, classSpec);
    this.addAccessorMethods(typeSpecBuilder, classSpec);
    this.addEmbeddedAccessorMethods(typeSpecBuilder, classSpec);
    this.addPrivateConstructor(typeSpecBuilder, classSpec);

    final List<ExecutableElement> allAnnotatedMethods= this.getAllAnnotatedMethodsAsElements(classSpec);
    this.addEqualsMethod(typeSpecBuilder, allAnnotatedMethods, classSpec);
    this.addHashCodeMethod(typeSpecBuilder, allAnnotatedMethods);

    return typeSpecBuilder.build();
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


  private void addPrivateFields(final TypeSpec.Builder typeSpecBuilder, final ClassSpec classSpec) {
    // add the fields for all the accessors
    for (final AccessorSpec accessorSpec : classSpec.accessors()) {
      this.addPrivateField(typeSpecBuilder, accessorSpec);
    }
  }


  private void addAccessorMethods(final TypeSpec.Builder typeSpecBuilder, final ClassSpec classSpec) {
    for (final AccessorSpec accessorSpec : classSpec.accessors()) {
      this.addAccessorMethod(typeSpecBuilder, accessorSpec);
    }
  }


  private void addEmbeddedAccessorMethods(final TypeSpec.Builder typeSpecBuilder, final ClassSpec classSpec) {
    for (final EmbeddedTypeSpec embeddedAccessorSpec : classSpec.embeddedTypes()) {
      this.addEmbeddedAccessorMethod(typeSpecBuilder, embeddedAccessorSpec);
    }
  }


  private void addPrivateConstructor(final TypeSpec.Builder typeSpecBuilder, final ClassSpec classSpec) {
    final MethodSpec.Builder mainConstructorBuilder=
      MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE);

    // add the string representation
    mainConstructorBuilder.addParameter(String.class, "toString", FINAL);
    mainConstructorBuilder.addStatement("super(toString)");

    // add the accessors
    for (final AccessorSpec accessorSpec : classSpec.accessors()) {
      final TypeName typeName= TypeName.get(accessorSpec.accessor().getReturnType());
      mainConstructorBuilder.addParameter(typeName, accessorSpec.methodName(), FINAL);

      mainConstructorBuilder.addStatement("this.$N = $N",
                                          accessorSpec.methodName(),
                                          accessorSpec.methodName());
    }

    // add the embedded configs
    for (final EmbeddedTypeSpec embeddedSpec : classSpec.embeddedTypes()) {
      final TypeName typeName= TypeName.get(embeddedSpec.accessor().getReturnType());
      mainConstructorBuilder.addParameter(typeName, embeddedSpec.methodName(), FINAL);

      mainConstructorBuilder.addStatement("this.$N = $N",
                                          embeddedSpec.methodName(),
                                          embeddedSpec.methodName());
    }

    typeSpecBuilder.addMethod(mainConstructorBuilder.build());
  }


  private void addEmbeddedAccessorMethod(final TypeSpec.Builder typeSpecBuilder, final EmbeddedTypeSpec embeddedTypeSpec) {
    final ExecutableElement accessor  = embeddedTypeSpec.accessor();

    typeSpecBuilder.addField(
      FieldSpec.builder(
        TypeName.get(accessor.getReturnType()),
        embeddedTypeSpec.methodName()).
        addModifiers(PRIVATE, FINAL)
        .build()
    );

    final MethodSpec.Builder methodSpecBuilder= MethodSpec.overriding(accessor);
    methodSpecBuilder.addStatement("return this.$L", embeddedTypeSpec.methodName());

    final String javadoc= this.pEnv.getElementUtils().getDocComment(accessor);
    if (javadoc != null) {
      methodSpecBuilder.addJavadoc(javadoc);
    }

    typeSpecBuilder.addMethod(methodSpecBuilder.build());
  }


  private void addPrivateField(final TypeSpec.Builder typeSpecBuilder,
                               final AccessorSpec     accessorSpec) {
    final ExecutableElement accessor= accessorSpec.accessor();

    final TypeName fieldName= TypeName.get(accessor.getReturnType());

    typeSpecBuilder.addField(
      FieldSpec.builder(fieldName, accessor.getSimpleName().toString())
        .addModifiers(PRIVATE, FINAL)
        .build());
  }


  private void addAccessorMethod(final TypeSpec.Builder typeSpecBuilder,
                                 final AccessorSpec     accessorSpec) {
    final ExecutableElement accessor= accessorSpec.accessor();

    final String defaultValue= accessorSpec.defaultValue() != null && !accessorSpec.defaultValue().trim().isEmpty()
                               ? accessorSpec.defaultValue()
                               : "";

    if (!accessorSpec.mandatory() && !defaultValue.trim().isEmpty()) {
      this.pEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                               "Optional and default value don't make much sense together. The Optional will never be empty.",
                                               accessor);
    }

    final MethodSpec.Builder methodSpecBuilder= MethodSpec.overriding((ExecutableElement) accessor)
        .addStatement("return this.$L", accessorSpec.methodName());

    final String javadoc= this.pEnv.getElementUtils().getDocComment(accessor);
    if (javadoc != null) {
      methodSpecBuilder.addJavadoc(javadoc);
    }

    typeSpecBuilder.addMethod(methodSpecBuilder.build());
  }


  private void addEqualsMethod(final TypeSpec.Builder                  typeSpecBuilder,
                               final List<? extends ExecutableElement> annotatedMethods,
                               final ClassSpec                         classSpec) {
    final MethodSpec.Builder methodSpecBuilder= MethodSpec.methodBuilder("equals")
      .addAnnotation(Override.class)
      .addModifiers(PUBLIC)
      .addParameter(Object.class, "obj", FINAL)
      .returns(boolean.class)
      .addCode("" +
        "if (this == obj) {\n" +
        "  return true;\n" +
        "}\n" +
        "\n" +
        "if (obj == null) {\n" +
        "  return false;\n" +
        "}\n" +
        "\n" +
        "if (this.getClass() != obj.getClass()) {\n" +
        "  return false;\n" +
        "}\n\n")
      .addStatement("final $T other = ($T) obj", classSpec.annotatedType(), classSpec.annotatedType())
      .addCode("\n")
      ;

      for (final Element annotatedMethod : annotatedMethods) {
        final Name methodName= annotatedMethod.getSimpleName();
        methodSpecBuilder.beginControlFlow("if (!$T.equals(this.$L(), other.$L()))", Objects.class, methodName, methodName)
          .addStatement("return false")
          .endControlFlow()
          .addCode("\n");
      }

      methodSpecBuilder.addStatement("return true");

      typeSpecBuilder
        .addMethod(methodSpecBuilder.build())
        .build();
  }


  private void addHashCodeMethod(final TypeSpec.Builder                  typeSpecBuilder,
                                 final List<? extends ExecutableElement> annotatedMethods) {
    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("hashCode")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(int.class)
        .addStatement(CodeBlock.of(
          annotatedMethods.stream()
            .map(Element::getSimpleName)
            .map(n -> n + "()")
          .collect(joining(",\n", "return java.util.Objects.hash(\n", ")"))
        ))
      .build()
    );
  }


  private List<ExecutableElement> getAllAnnotatedMethodsAsElements(final ClassSpec classSpec) {
    return Stream.concat(
      classSpec.accessors().stream()
        .map(AccessorSpec::accessor),
      classSpec.embeddedTypes().stream()
        .map(EmbeddedTypeSpec::accessor))
      .distinct()
      .collect(toList());
  }
}
