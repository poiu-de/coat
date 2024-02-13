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
package de.poiu.coat.processor.codegeneration;

import de.poiu.coat.processor.utils.NameUtils;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.poiu.coat.CoatConfig;
import de.poiu.coat.ConfigParam;
import de.poiu.coat.processor.CoatProcessorException;
import de.poiu.coat.processor.utils.JavadocHelper;
import de.poiu.coat.processor.examplecontent.ExampleContentHelper;
import de.poiu.coat.processor.specs.AccessorSpec;
import de.poiu.coat.processor.specs.ClassSpec;
import de.poiu.coat.processor.specs.EmbeddedTypeSpec;
import de.poiu.coat.processor.utils.ElementHelper;
import de.poiu.coat.processor.utils.SpecHelper;
import de.poiu.coat.processor.utils.TypeHelper;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.System.Logger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;


/**
 * Helper class for the actual code generation.
 */
public class CodeGenerator {


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final ProcessingEnvironment pEnv;
  private final SpecHelper            specHelper;
  private final TypeHelper            typeHelper;
  private final ElementHelper         elementHelper;
  private final ExampleContentHelper  exampleContentHelper;


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  public CodeGenerator(final ProcessingEnvironment pEnv) {
    this.pEnv                 = pEnv;
    this.specHelper           = new SpecHelper(pEnv);
    this.typeHelper           = new TypeHelper(pEnv);
    this.elementHelper        = new ElementHelper(pEnv);
    this.exampleContentHelper = new ExampleContentHelper(pEnv);
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
  public void generateEnumCode(final ClassSpec classSpec) throws IOException {
    pEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                    String.format("Generating enum %s for %s.", classSpec.fqEnumName(), classSpec.annotatedType()));

    final ClassName enumName= ClassName.get(classSpec.targetPackage(), classSpec.enumName());
    final TypeSpec.Builder typeSpecBuilder = TypeSpec.enumBuilder(enumName)
      .addModifiers(PUBLIC)
      .addSuperinterface(ClassName.get(ConfigParam.class))
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

    this.addGeneratedAnnotation(typeSpecBuilder);

    this.addFieldAndAccessor(typeSpecBuilder, String.class,     "key");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "type");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "collectionType");
    this.addFieldAndAccessor(typeSpecBuilder, String.class,     "defaultValue");
    this.addFieldAndAccessor(typeSpecBuilder, TypeName.BOOLEAN, "mandatory");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "converter");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "listParser");

    if (classSpec.accessors().isEmpty()) {
      // not creating an enum if there is no accessor for it
      return;
    }

    for (final AccessorSpec accessorSpec : classSpec.accessors()) {
      this.addEnumConstant(typeSpecBuilder, accessorSpec);
    }

    JavaFile.builder(enumName.packageName(), typeSpecBuilder.build())
      .build()
      .writeTo(this.pEnv.getFiler());
  }


  /**
   * Generate the config class for the given classSpec.
   *
   * @param classSpec the classSpec (the annotated interface) for which to generate the config class
   * @throws IOException if writing the config class to file failed
   */
  public void generateClassCode(final ClassSpec classSpec) throws IOException {
    pEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                             String.format("Generating config class %s for %s.", classSpec.fqEnumName(), classSpec.annotatedType()));

    final ClassName fqEnumName = ClassName.get(classSpec.targetPackage(), classSpec.enumName());
    final ClassName fqClassName= ClassName.get(classSpec.targetPackage(), classSpec.className());

    final TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(fqClassName)
      .addModifiers(PUBLIC)
      .superclass(ClassName.get(CoatConfig.class))
      .addSuperinterface(ClassName.get(classSpec.annotatedType()))
      ;

    this.addGeneratedAnnotation(typeSpecBuilder);

    this.addPrivateStaticFinalLogger(typeSpecBuilder, fqClassName);
    this.addStaticFactoryMethods(typeSpecBuilder, fqClassName);
    this.addAccessorMethods(typeSpecBuilder, classSpec, fqEnumName);
    this.addEmbeddedAccessorMethods(typeSpecBuilder, classSpec, fqEnumName);
    this.addPrivateConstructor(typeSpecBuilder, classSpec, fqEnumName);

    final List<ExecutableElement> allAnnotatedMethods= this.getAllAnnotatedMethodsAsElements(classSpec);
    this.addEqualsMethod(typeSpecBuilder, allAnnotatedMethods, fqClassName);
    this.addHashCodeMethod(typeSpecBuilder, allAnnotatedMethods);

    this.addWriteExampleConfigMethod(typeSpecBuilder, classSpec);

    this.addBuilder(typeSpecBuilder, classSpec, fqClassName);

    JavaFile.builder(fqClassName.packageName(), typeSpecBuilder.build())
      .build()
      .writeTo(this.pEnv.getFiler());
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


  private void addAccessorMethods(final TypeSpec.Builder typeSpecBuilder, final ClassSpec classSpec, final ClassName fqEnumName) {
    for (final AccessorSpec accessorSpec : classSpec.accessors()) {
      this.addAccessorMethod(typeSpecBuilder, accessorSpec, fqEnumName);
    }
  }


  private void addEmbeddedAccessorMethods(final TypeSpec.Builder typeSpecBuilder, final ClassSpec classSpec, final ClassName fqEnumName) {
    for (final EmbeddedTypeSpec embeddedAccessorSpec : classSpec.embeddedTypes()) {
      this.addEmbeddedAccessorMethod(typeSpecBuilder, embeddedAccessorSpec, fqEnumName);
    }
  }


  private void addPrivateConstructor(final TypeSpec.Builder typeSpecBuilder, final ClassSpec classSpec, final ClassName fqEnumName) {
    final MethodSpec.Builder mainConstructorBuilder= MethodSpec.constructorBuilder()
      .addModifiers(PRIVATE)
      .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "props", FINAL);

    if (classSpec.accessors().isEmpty()) {
      // if there are no annotated methods, no enum is being generated (due to a bug in JavaPoet)
      // therefore we cannot refer to it and instaed use an empty ConfigParam array
      // see https://github.com/square/javapoet/issues/739
      // and https://github.com/square/javapoet/issues/832
      mainConstructorBuilder.addStatement("super(new $T[]{})", ConfigParam.class);
    } else {
      mainConstructorBuilder.addStatement("super($T.values())", fqEnumName);
    }

    final List<CodeBlock> initCodeBlocks= this.prepareInitCodeBlocks(classSpec);
    for (final CodeBlock initEmbeddedConfig : initCodeBlocks) {
      mainConstructorBuilder.addCode(initEmbeddedConfig);
    }

    mainConstructorBuilder.addCode("\n");
    mainConstructorBuilder.addStatement("this.add(props)");
    typeSpecBuilder.addMethod(mainConstructorBuilder.build());
  }


  private void addEmbeddedAccessorMethod(final TypeSpec.Builder typeSpecBuilder, final EmbeddedTypeSpec embeddedTypeSpec, final ClassName fqEnumName) {
    final ExecutableElement accessor  = embeddedTypeSpec.accessor();
    final boolean           isOptional= !embeddedTypeSpec.mandatory();

    final TypeElement returnTypeElement= (TypeElement) this.pEnv.getTypeUtils().asElement(embeddedTypeSpec.type());
    final String generatedTypeName     = NameUtils.deriveGeneratedClassName(returnTypeElement);

    typeSpecBuilder.addField(FieldSpec.builder(ClassName.get(fqEnumName.packageName(), generatedTypeName),
        embeddedTypeSpec.key(),
        PRIVATE,
        FINAL)
        .build()
    );

    final MethodSpec.Builder methodSpecBuilder= MethodSpec.overriding(accessor);

    if (isOptional) {
      methodSpecBuilder.addStatement("return $T.ofNullable(this.$L)",
                                     Optional.class,
                                     embeddedTypeSpec.key()
                                     );
    } else {
        methodSpecBuilder.addStatement("return this.$L",
                      embeddedTypeSpec.key());
    }

    final String javadoc= this.pEnv.getElementUtils().getDocComment(accessor);
    if (javadoc != null) {
      methodSpecBuilder.addJavadoc(javadoc);
    }

    typeSpecBuilder.addMethod(methodSpecBuilder.build());
  }


  private void addWriteExampleConfigMethod(final TypeSpec.Builder typeSpecBuilder,
                                           final ClassSpec        classSpec) {
    final String exampleContent= this.exampleContentHelper.createExampleContent(classSpec);

    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("writeExampleConfig")
        .addModifiers(PUBLIC, STATIC)
        .addParameter(TypeName.get(Writer.class), "writer", FINAL)
        .addStatement("writer.append($S)", exampleContent)
        .addStatement("writer.flush()")
        .addException(IOException.class)
        .addJavadoc(JavadocHelper.JAVADOC_ON_WRITE_EXAMPLE_CONFIG)
        .build());
  }


  private void addBuilder(final TypeSpec.Builder typeSpecBuilder, final ClassSpec classSpec, final ClassName fqClassName) {
    final CoatBuilderGenerator builderGenerator= CoatBuilderGenerator.forType(classSpec, fqClassName, this.pEnv);
    typeSpecBuilder.addMethod(builderGenerator.generateBuilderMethod());
    typeSpecBuilder.addType(builderGenerator.generateBuilderClass());
  }


  private void addPrivateStaticFinalLogger(final TypeSpec.Builder typeSpecBuilder, final ClassName fqClassName) {
    typeSpecBuilder.addField(
      FieldSpec.builder(Logger.class, "LOGGER")
        .addModifiers(PRIVATE, STATIC, FINAL)
        .initializer("System.getLogger($L.class.getName())", fqClassName)
        .build());

  }


  private void addStaticFactoryMethods(final TypeSpec.Builder typeSpecBuilder, final ClassName fqClassName) {
    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("from")
        .addModifiers(PUBLIC, STATIC)
        .returns(fqClassName)
        .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "props", FINAL)
        .addStatement("return new $T(props)", fqClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_MAP, fqClassName, fqClassName)
        .build());

    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("from")
        .addModifiers(PUBLIC, STATIC)
        .returns(fqClassName)
        .addParameter(File.class, "file", FINAL)
        .addStatement("return new $T(toMap(file))", fqClassName)
        .addException(IOException.class)
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_FILE, fqClassName, fqClassName)
        .build());

    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("from")
        .addModifiers(PUBLIC, STATIC)
        .returns(fqClassName)
        .addParameter(Properties.class, "jup", FINAL)
        .addStatement("return new $T(toMap(jup))", fqClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_PROPERTIES, fqClassName, fqClassName)
        .build());

    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("fromEnvVars")
        .addModifiers(PUBLIC, STATIC)
        .returns(fqClassName)
        .addStatement("return builder().addEnvVars().build()")
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_ENV_VARS, fqClassName, fqClassName)
        .build());
  }



  private void addGeneratedAnnotation(final TypeSpec.Builder typeSpecBuilder) {
    final Class<?> generatedAnnotationClass = this.identifyGeneratedAnnotation();
    if (generatedAnnotationClass != null) {
      typeSpecBuilder.addAnnotation(AnnotationSpec.builder(generatedAnnotationClass)
        .addMember("value", "$S", this.getClass().getName())
        .addMember("date", "$S", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .build());
    }
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


  private void addFieldAndAccessor(final TypeSpec.Builder enumBuilder, final TypeName fieldType, final String fieldName) {
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

    final String constName= NameUtils.toConstName(accessorSpec.methodName());

    TypeSpec.Builder enumConstBuilder =
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


  private void addAccessorMethod(final TypeSpec.Builder typeSpecBuilder,
                                 final AccessorSpec     accessorSpec,
                                 final ClassName        fqEnumName) {
    final ExecutableElement accessor= accessorSpec.accessor();

    final String    constName = NameUtils.toConstName(accessorSpec.methodName());
    final ClassName constClass= ClassName.get(fqEnumName.packageName(), fqEnumName.simpleName(), constName);

    final String defaultValue= accessorSpec.defaultValue() != null && !accessorSpec.defaultValue().trim().isEmpty()
                               ? accessorSpec.defaultValue()
                               : "";

    if (!accessorSpec.mandatory() && !defaultValue.trim().isEmpty()) {
      this.pEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                               "Optional and default value don't make much sense together. The Optional will never be empty.",
                                               accessor);
    }

    final String getter= this.specHelper.getSuperGetterName(accessorSpec);

    final MethodSpec.Builder methodSpecBuilder= MethodSpec.overriding((ExecutableElement) accessor)
        .addStatement("return super.$L($T)",
                      getter,
                      constClass);

    final String javadoc= this.pEnv.getElementUtils().getDocComment(accessor);
    if (javadoc != null) {
      methodSpecBuilder.addJavadoc(javadoc);
    }

    typeSpecBuilder.addMethod(methodSpecBuilder.build());
  }


  private void addEqualsMethod(final TypeSpec.Builder typeSpecBuilder,
                               final List<? extends ExecutableElement> annotatedMethods,
                               final ClassName className) {
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
      .addStatement("final $T other = ($T) obj", className, className)
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


  private void addHashCodeMethod(final TypeSpec.Builder typeSpecBuilder,
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


  private List<CodeBlock> prepareInitCodeBlocks(final ClassSpec classSpec) {
    final List<CodeBlock> initCodeBlocks= new ArrayList<>();

    // init Embedded config
    for (final EmbeddedTypeSpec embeddedSpec : classSpec.embeddedTypes()) {
      final CodeBlock initEmbeddedCodeBlock = this.createEmbeddedTypeInitializationCode(embeddedSpec);
      initCodeBlocks.add(initEmbeddedCodeBlock);
    }

    // register custom converters and list parser
    final Optional<CodeBlock> registerCustomConverters= this.createRegisterCustomConverters(classSpec);
    registerCustomConverters.ifPresent(initCodeBlocks::add);
    final Optional<CodeBlock> registerCustomListParser= this.createRegisterCustomListParser(classSpec);
    registerCustomListParser.ifPresent(initCodeBlocks::add);

    return initCodeBlocks;
  }


  private CodeBlock createEmbeddedTypeInitializationCode(final EmbeddedTypeSpec embeddedTypeSpec) {
    final boolean     isOptional       = !embeddedTypeSpec.mandatory();
    final TypeElement returnTypeElement= (TypeElement) this.pEnv.getTypeUtils().asElement(embeddedTypeSpec.type());
    final String      generatedTypeName= NameUtils.deriveGeneratedClassName(returnTypeElement);

    // The code to initialize this field. Needs to be added to the typeSpecs constructors
    final CodeBlock.Builder initCodeBlockBuilder = CodeBlock.builder();
    initCodeBlockBuilder.add("\n");
    if (isOptional) {
      initCodeBlockBuilder.beginControlFlow("if (hasPrefix(props, $S))",
        embeddedTypeSpec.key() + embeddedTypeSpec.keySeparator()
      );
    }
    initCodeBlockBuilder
      .addStatement("this.$N= $L.from(\n"
        + "filterByAndStripPrefix(props, $S))",
                    embeddedTypeSpec.key(),
                    generatedTypeName,
                    embeddedTypeSpec.key() + embeddedTypeSpec.keySeparator());
    if (isOptional) {
      initCodeBlockBuilder
        .nextControlFlow("else")
        .addStatement("this.$N= null", embeddedTypeSpec.key())
        .endControlFlow();
    }
    initCodeBlockBuilder.addStatement("super.registerEmbeddedConfig($S, this.$N, $L)",
      embeddedTypeSpec.key() + embeddedTypeSpec.keySeparator(),
      embeddedTypeSpec.key(),
      isOptional);

    return initCodeBlockBuilder.build();
  }


  private Optional<CodeBlock> createRegisterCustomConverters(final ClassSpec annotatedInterface) {
    if (annotatedInterface.converters().isEmpty()) {
      return Optional.empty();
    }

    // The code  to initialize this field. Needs to be added to the typeSpecs constructors
    final CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
    codeBlockBuilder.add("\n");

    annotatedInterface.converters().
      forEach((c) -> {
        codeBlockBuilder.addStatement(
          "super.registerCustomConverter(new $L())",
          c.toString()
        );
      });

    return Optional.of(codeBlockBuilder.build());
  }


  private Optional<CodeBlock> createRegisterCustomListParser(final ClassSpec annotatedInterface) {
    if (annotatedInterface.listParser().isEmpty()) {
      return Optional.empty();
    }

    // The code  to initialize this field. Needs to be added to the typeSpecs constructors
    final CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
    codeBlockBuilder.add("\n");
    codeBlockBuilder.addStatement(
          "super.registerListParser(new $T())",
          annotatedInterface.listParser().get()
        );

    return Optional.of(codeBlockBuilder.build());
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



  /**
   * Returns the typeElement corresponding to the {@code @Generated} annotation present at the target
   * {@code SourceVersion}.
   * <p>
   * Returns {@code javax.annotation.processing.Generated} for JDK 9 and newer, {@code
   * javax.annotation.Generated} for earlier releases, and null if the annotation is
   * not available.
   * <p>
   * Taken from https://github.com/google/auto/blob/master/common/src/main/java/com/google/auto/common/GeneratedAnnotations.java
   *
   * @return The {@code @Generated} annotation for the target source version or {@code null} if it could not be identified
   */
  @Nullable
  private Class<?> identifyGeneratedAnnotation() {
    try {
      final SourceVersion sourceVersion= this.pEnv.getSourceVersion();
      return sourceVersion.compareTo(SourceVersion.RELEASE_8) > 0
           ? Class.forName("javax.annotation.processing.Generated")
           : Class.forName("javax.annotation.Generated");
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }


  /**
   * Returns the non-optinal return type of the given accessor as String.
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
