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

import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
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
import de.poiu.coat.annotation.Coat;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;

import static java.util.stream.Collectors.joining;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toCollection;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.ElementKind.METHOD;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.ARRAY;
import static javax.lang.model.type.TypeKind.DECLARED;
import static javax.lang.model.type.TypeKind.VOID;


/**
 * The actual annotation processor of Coat.
 *
 */
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@SupportedAnnotationTypes(
  "de.poiu.coat.annotation.Coat.Config"
)
@AutoService(Processor.class)
public class CoatProcessor extends AbstractProcessor {


  private ConfigParamHandler paramSpecBuilder= null;


  @Override
  public boolean process(final Set<? extends TypeElement> annotations,
                         final RoundEnvironment           roundEnv) {

    this.paramSpecBuilder= new ConfigParamHandler(this.processingEnv);

    // for each Coat.Config annotation
    // get all the annotated types
    // and generate the corresponding classes
    annotations.stream()
      .filter(a -> a.getQualifiedName().contentEquals("de.poiu.coat.annotation.Coat.Config"))
      .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
      .map(e -> (TypeElement) e)
      .peek(this::assertIsInterface)
      .forEachOrdered(this::generateCode);

    return false;
  }


  private void generateCode(final TypeElement annotatedInterface) {
    processingEnv.getMessager().printMessage(Kind.NOTE,
                                             String.format("Generating code for %s.", annotatedInterface));

    try {
      final ClassName fqGeneratedEnumName= this.deriveGeneratedEnumName(annotatedInterface);
      this.generateEnumCode(fqGeneratedEnumName, annotatedInterface, processingEnv.getFiler());

      final ClassName fqGeneratedClassName= this.deriveGeneratedClassName(annotatedInterface);
      this.generateClassCode(fqGeneratedClassName, fqGeneratedEnumName, annotatedInterface, processingEnv.getFiler());

      final String exampleFileName= annotatedInterface.getSimpleName().toString() + ".properties";
      this.generateExampleFile(exampleFileName, annotatedInterface, processingEnv.getFiler());
    } catch (IOException ex) {
      processingEnv.getMessager().printMessage(Kind.ERROR,
                                               String.format("Error generating code for %s.", annotatedInterface));
      ex.printStackTrace();
    }
  }


  private Set<TypeElement> getInheritedInterfaces(final TypeElement annotatedInterface) {
    final Set<TypeElement> allExtendedInterfaces= new LinkedHashSet<>();

    annotatedInterface.getInterfaces().stream()
      .filter(i -> (i.getKind() == DECLARED))
      .map(i -> ((DeclaredType) i))
      .map(DeclaredType::asElement)
      .filter(e -> (e.getKind() == ElementKind.INTERFACE))
      .map(e -> ((TypeElement) e))
      .forEachOrdered(e -> {
        allExtendedInterfaces.add(e);
        allExtendedInterfaces.addAll(this.getInheritedInterfaces(e));
      })
      ;

      return allExtendedInterfaces;
  }


  private Set<ConfigParamSpec> getInheritedAnnotatedMethods(final TypeElement annotatedInterface) {
    final Set<ConfigParamSpec> annotatedMethods= new LinkedHashSet<>();

    this.getInheritedInterfaces(annotatedInterface).stream()
      .map(Element::getEnclosedElements)
      .flatMap(l -> l.stream())
      .filter(e -> (e.getKind() == ElementKind.METHOD))
      .filter(e -> e.getAnnotation(Coat.Embedded.class) == null)
      .map(paramSpecBuilder::from)
      .forEachOrdered(annotatedMethods::add);
      ;

    return annotatedMethods;
  }


  private void generateEnumCode(final ClassName fqEnumName,
                                final TypeElement annotatedInterface,
                                final Filer filer) throws IOException {
    processingEnv.getMessager().printMessage(Kind.NOTE,
                                             String.format("Generating enum %s for %s.", fqEnumName, annotatedInterface));

    final TypeSpec.Builder typeSpecBuilder = TypeSpec.enumBuilder(fqEnumName)
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

    final List<ConfigParamSpec> annotatedMethods= annotatedInterface.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Embedded.class) == null)
      .map(paramSpecBuilder::from)
      .collect(toCollection(ArrayList::new));

    annotatedMethods.addAll(this.getInheritedAnnotatedMethods(annotatedInterface));

    if (annotatedMethods.isEmpty()) {
      processingEnv.getMessager().printMessage(Kind.WARNING,
                                             String.format("No annotated methods in %s.", annotatedInterface));
      throw new RuntimeException("At least one annotated method is necessary for " + annotatedInterface.toString());
    }

    this.assertReturnType(annotatedMethods);
    this.assertNoParameters(annotatedMethods);
    this.reduceDuplicateAccessors(annotatedMethods);
    this.assertUniqueKeys(annotatedMethods);

    for (final ConfigParamSpec annotatedMethod : annotatedMethods) {
      this.addEnumConstant(typeSpecBuilder, annotatedMethod);
    }

    JavaFile.builder(fqEnumName.packageName(), typeSpecBuilder.build())
      .build()
      .writeTo(filer);
  }


  private void generateClassCode(final ClassName fqClassName,
                                 final ClassName fqEnumName,
                                 final TypeElement annotatedInterface,
                                 final Filer filer) throws IOException {
    processingEnv.getMessager().printMessage(Kind.NOTE,
                                             String.format("Generating config class %s for %s.", fqClassName, annotatedInterface));

    final TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(fqClassName)
      .addModifiers(PUBLIC)
      .superclass(ClassName.get(CoatConfig.class))
      .addSuperinterface(ClassName.get(annotatedInterface))
      ;

    this.addGeneratedAnnotation(typeSpecBuilder);

    // add static factory methods
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
        .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "props", FINAL)
        .addStatement("return new $T(props)", fqClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_MAP, fqClassName, fqClassName)
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
        .addStatement("final $T c= new $T(java.util.Collections.EMPTY_MAP)", fqClassName, fqClassName)
        .addStatement("return c.addEnvVars()")
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_ENV_VARS, fqClassName, fqClassName)
        .build());

    // add “add” methods for merging multiple config sources into the same config object
    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("add")
        .addModifiers(PUBLIC)
        .returns(fqClassName)
        .addParameter(File.class, "file", FINAL)
        .addStatement("return ($T) super.add(file)", fqClassName)
        .addException(IOException.class)
        .addJavadoc(JavadocHelper.JAVADOC_ON_ADD_FILE, fqClassName, fqClassName)
        .build());
    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("add")
      .addModifiers(PUBLIC)
        .returns(fqClassName)
      .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "props", FINAL)
      .addStatement("return ($T) super.add(props)", fqClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_ADD_MAP, fqClassName, fqClassName)
      .build());
    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("add")
        .addModifiers(PUBLIC)
        .returns(fqClassName)
        .addParameter(Properties.class, "jup", FINAL)
        .addStatement("return ($T) super.add(jup)", fqClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_ADD_PROPERTIES, fqClassName, fqClassName)
        .build());
    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("addEnvVars")
        .addModifiers(PUBLIC)
        .returns(fqClassName)
        .addStatement("return ($T) super.addEnvVars()", fqClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_ADD_ENV_VARS, fqClassName, fqClassName)
        .build());

    final List<ConfigParamSpec> annotatedMethods= new ArrayList<>();

    // add accessor for direct parameters
    annotatedInterface.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Embedded.class) == null)
      .map(paramSpecBuilder::from)
      .forEachOrdered(annotatedMethods::add);

    annotatedMethods.addAll(this.getInheritedAnnotatedMethods(annotatedInterface));

    this.assertReturnType(annotatedMethods);
    this.assertNoParameters(annotatedMethods);
    this.reduceDuplicateAccessors(annotatedMethods);
    this.assertUniqueKeys(annotatedMethods);

    for (final ConfigParamSpec annotatedMethod : annotatedMethods) {
      this.addAccessorMethod(typeSpecBuilder, annotatedMethod, fqEnumName);
    }

    // add accessor for embedded configs
    // TODO: Check that not both, @Embedded and @Param are specified
    final List<EmbeddedParamSpec> embeddedAnnotatedMethods= annotatedInterface.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Embedded.class) != null)
      .map(paramSpecBuilder::embeddedFrom)
      .collect(toList());

    this.assertEmbeddedTypeIsAnnotated(embeddedAnnotatedMethods);

    final List<CodeBlock> initCodeBlocks= new ArrayList<>();
    for (final EmbeddedParamSpec annotatedMethod : embeddedAnnotatedMethods) {
      final CodeBlock initEmbeddedCodeBlock = this.addEmbeddedAccessorMethod(typeSpecBuilder, annotatedMethod, fqEnumName);
      initCodeBlocks.add(initEmbeddedCodeBlock);
    }

    final Optional<CodeBlock> registerCustomConverters= this.addRegisterCustomConverters(annotatedInterface);
    registerCustomConverters.ifPresent(initCodeBlocks::add);

    final Optional<CodeBlock> registerCustomListParser= this.addRegisterCustomListParser(annotatedInterface);
    registerCustomListParser.ifPresent(initCodeBlocks::add);

    // add (private) constructor
    final MethodSpec.Builder mainConstructorBuilder= MethodSpec.constructorBuilder()
      .addModifiers(PRIVATE)
      .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "props", FINAL)
      .addStatement("super(props, $T.values())", fqEnumName);
    for (final CodeBlock initEmbeddedConfig : initCodeBlocks) {
      mainConstructorBuilder.addCode(initEmbeddedConfig);
    }
    typeSpecBuilder.addMethod(mainConstructorBuilder.build());

    final List<Element> allAnnotatedMethods=
      Stream.concat(
        annotatedMethods.stream()
          .map(ConfigParamSpec::annotatedMethod),
        embeddedAnnotatedMethods.stream()
          .map(EmbeddedParamSpec::annotatedMethod)
      )
      .distinct()
      .collect(toList());
    this.addEqualsMethod(typeSpecBuilder, allAnnotatedMethods, fqClassName);
    this.addHashCodeMethod(typeSpecBuilder, allAnnotatedMethods);

    final List<ConfigParamSpec> allAnnotatedParams= this.getAnnotatedParamsRecursively(annotatedInterface);
    this.addWriteExampleConfigMethod(typeSpecBuilder, allAnnotatedParams);

    JavaFile.builder(fqClassName.packageName(), typeSpecBuilder.build())
      .build()
      .writeTo(filer);
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


  private void addEnumConstant(final TypeSpec.Builder typeSpecBuilder, final ConfigParamSpec configParamSpec) {
    final ExecutableElement annotatedMethod = configParamSpec.annotatedMethod();

    final String constName= this.toConstName(configParamSpec.methodeName());

    TypeSpec.Builder enumConstBuilder =
      TypeSpec.anonymousClassBuilder("$S, $L.class, $L, $S, $L, $L, $L",
                                     configParamSpec.key(),
                                     toBaseType(configParamSpec),
                                     getCollectionTypeName(configParamSpec).map(c -> c+".class").orElse(null),
                                     configParamSpec.defaultValue() != null && !configParamSpec.defaultValue().trim().isEmpty()
                                       ? configParamSpec.defaultValue()
                                       : null,
                                     configParamSpec.mandatory(),
                                     configParamSpec.converter()
                                       .map(TypeMirror::toString)
                                       .map(s -> s + ".class")
                                       .orElse("null"),
                                     configParamSpec.listParser()
                                       .map(TypeMirror::toString)
                                       .map(s -> s + ".class")
                                       .orElse("null")
                                     );

    final String javadoc= super.processingEnv.getElementUtils().getDocComment(annotatedMethod);
    if (javadoc != null) {
      enumConstBuilder.addJavadoc(Utils.stripBlockTagsFromJavadoc(javadoc));
    }

    typeSpecBuilder.addEnumConstant(constName, enumConstBuilder.build());
  }


  private void addWriteExampleConfigMethod(final TypeSpec.Builder typeSpecBuilder, final List<ConfigParamSpec> configParamSpecs) {
    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("writeExampleConfig")
        .addModifiers(PUBLIC, STATIC)
        .addParameter(TypeName.get(Writer.class), "writer", FINAL)
        .addStatement("writeExampleConfig(new $T(writer))", BufferedWriter.class)
        .addException(IOException.class)
        .build());

    final String exampleContent= this.createExampleContent(configParamSpecs);

    final MethodSpec.Builder methodBuilder= MethodSpec.methodBuilder("writeExampleConfig");
    methodBuilder
      .addModifiers(PUBLIC, STATIC)
      .addParameter(TypeName.get(BufferedWriter.class), "writer", FINAL)
      .addStatement("writer.append($S)", exampleContent)
      .addStatement("writer.flush()")
      .addException(IOException.class)
      ;

    typeSpecBuilder.addMethod(methodBuilder.build());
  }


  private String toBaseType(final String typeName) {
    if (typeName.equals("java.util.OptionalInt") ||
        typeName.equals("java.util.OptionalDouble") ||
        typeName.equals("java.util.OptionalLong")) {
      return typeName.substring("java.util.Optional".length(),
        typeName.length())
        .toLowerCase();
    } else if (typeName.startsWith("java.util.Optional<") &&
      typeName.endsWith(">")) {
      return typeName.substring("java.util.Optional<".length(),
        typeName.length() - 1
      );
    } else {
      return typeName;
    }
  }


  private String toBaseType(final ConfigParamSpec paramSpec) {
    final TypeMirror type = paramSpec.type();

    final TypeMirror optionalType = this.processingEnv.getElementUtils().getTypeElement("java.util.Optional").asType();
    final TypeMirror optionalIntType = this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalInt").asType();
    final TypeMirror optionalLongType = this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalLong").asType();
    final TypeMirror optionalDoubleType = this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalDouble").asType();
    final TypeMirror optErasure = this.processingEnv.getTypeUtils().erasure(optionalType);

    if (type.getKind() == TypeKind.INT || this.processingEnv.getTypeUtils().isSameType(type, optionalIntType)) {
      return int.class.getName();
    }

    if (type.getKind() == TypeKind.LONG || this.processingEnv.getTypeUtils().isSameType(type, optionalLongType)) {
      return long.class.getName();
    }

    if (type.getKind() == TypeKind.DOUBLE || this.processingEnv.getTypeUtils().isSameType(type, optionalDoubleType)) {
      return double.class.getName();
    }

    if (type.getKind() == TypeKind.BOOLEAN) {
      return boolean.class.getName();
    }

    if (type.getKind() == DECLARED) {
      final DeclaredType declaredType= (DeclaredType) type;

      final TypeMirror erasure = this.processingEnv.getTypeUtils().erasure(type);
      final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

      if (this.processingEnv.getTypeUtils().isAssignable(erasure, optionalType)
        || paramSpec.collectionType().isPresent()) {
        if (typeArguments.size() < 1) {
          throw new CoatProcessorException("Optionals without type argument are not supported.");
        }
        if (typeArguments.size() > 1) {
          throw new RuntimeException("Optionals with multiple type arguments are not expected.");
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

      return this.processingEnv.getTypeUtils().asElement(componentType).toString();
    }

    return type.toString();
  }


  private boolean isOptional(final TypeMirror type) {
    final TypeMirror optionalType = this.processingEnv.getElementUtils().getTypeElement("java.util.Optional").asType();
    final TypeMirror optionalIntType = this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalInt").asType();
    final TypeMirror optionalLongType = this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalLong").asType();
    final TypeMirror optionalDoubleType = this.processingEnv.getElementUtils().getTypeElement("java.util.OptionalDouble").asType();

    final TypeMirror erasure = this.processingEnv.getTypeUtils().erasure(type);

    if (this.processingEnv.getTypeUtils().isAssignable(erasure, optionalType)
      || this.processingEnv.getTypeUtils().isAssignable(erasure, optionalIntType)
      || this.processingEnv.getTypeUtils().isAssignable(erasure, optionalLongType)
      || this.processingEnv.getTypeUtils().isAssignable(erasure, optionalDoubleType)) {
      return true;
    }

    return false;
  }


  private Optional<String> getCollectionTypeName(final ConfigParamSpec configParamSpec) {
    if (configParamSpec.collectionType().isEmpty()) {
      return Optional.empty();
    }

    final TypeMirror collectionType = configParamSpec.collectionType().get();
    if (collectionType.getKind() == DECLARED) {
      final DeclaredType declaredType= (DeclaredType) collectionType;
      return Optional.of(this.processingEnv.getTypeUtils().erasure(collectionType).toString());
    }

    return Optional.of(collectionType.toString());
  }


  private void addAccessorMethod(final TypeSpec.Builder typeSpecBuilder, final ConfigParamSpec configParamSpec, final ClassName fqEnumName) {
    final ExecutableElement annotatedMethod= configParamSpec.annotatedMethod();

    final String constName= this.toConstName(configParamSpec.methodeName());
    final ClassName constClass= ClassName.get(fqEnumName.packageName(), fqEnumName.simpleName(), constName);

    final String defaultValue= configParamSpec.defaultValue() != null && !configParamSpec.defaultValue().trim().isEmpty()
                               ? configParamSpec.defaultValue()
                               : "";

    if (!configParamSpec.mandatory() && !defaultValue.trim().isEmpty()) {
      processingEnv.getMessager().printMessage(Kind.WARNING,
                                               "Optional and default value don't make much sense together. The Optional will never be empty.",
                                               annotatedMethod);
    }

    final String getter= getSuperGetterName(configParamSpec.type(), !defaultValue.trim().isEmpty(), configParamSpec.collectionType());

    final MethodSpec.Builder methodSpecBuilder= MethodSpec.overriding((ExecutableElement) annotatedMethod)
        .addStatement("return super.$L($T)",
                      getter,
                      constClass);

    final String javadoc= super.processingEnv.getElementUtils().getDocComment(annotatedMethod);
    if (javadoc != null) {
      methodSpecBuilder.addJavadoc(javadoc);
    }

    typeSpecBuilder.addMethod(methodSpecBuilder.build());
  }


  private CodeBlock addEmbeddedAccessorMethod(final TypeSpec.Builder typeSpecBuilder, final EmbeddedParamSpec embeddedParamSpec, final ClassName fqEnumName) {
    final ExecutableElement annotatedMethod= embeddedParamSpec.annotatedMethod();

    boolean isOptional= false;
    DeclaredType generatedType= (DeclaredType) embeddedParamSpec.annotatedMethod().getReturnType();
    if (generatedType.asElement().toString().equals("java.util.Optional")) {
      // FIXME: Handle the (strange) case when less or more than 1 type argument exist
      generatedType = (DeclaredType) generatedType.getTypeArguments().get(0);
      isOptional= true;
    }
    final ClassName generatedTypeName= this.deriveGeneratedClassName(
      (TypeElement) processingEnv.getTypeUtils().asElement(generatedType));

    typeSpecBuilder.addField(FieldSpec.builder(generatedTypeName, embeddedParamSpec.key(), PRIVATE, FINAL)
      .build()
    );

    // The code  to initialize this field. Needs to be added to the typeSpecs constructors
    final CodeBlock.Builder initCodeBlockBuilder = CodeBlock.builder();
    initCodeBlockBuilder.add("\n");
    if (isOptional) {
      initCodeBlockBuilder.beginControlFlow(
        "if (hasPrefix(props, $S))",
        embeddedParamSpec.key() + embeddedParamSpec.keySeparator()
      );
    }
    initCodeBlockBuilder
      .addStatement("this.$N= $T.from(\n"
        + "filterByAndStripPrefix(props, $S))",
                    embeddedParamSpec.key(),
                    generatedTypeName,
                    embeddedParamSpec.key() + embeddedParamSpec.keySeparator());
    if (isOptional) {
      initCodeBlockBuilder
        .nextControlFlow("else")
        .addStatement("this.$N= null", embeddedParamSpec.key())
        .endControlFlow();
    }
    initCodeBlockBuilder.addStatement(
      "super.registerEmbeddedConfig($S, this.$N, $L)",
      embeddedParamSpec.key() + embeddedParamSpec.keySeparator(),
      embeddedParamSpec.key(),
      isOptional);


    final MethodSpec.Builder methodSpecBuilder= MethodSpec.overriding(annotatedMethod);

    if (isOptional) {
      methodSpecBuilder.addStatement("return $T.ofNullable(this.$L)",
                                     Optional.class,
                                     embeddedParamSpec.key()
                                     );
    } else {
        methodSpecBuilder.addStatement("return this.$L",
                      embeddedParamSpec.key());
    }

    final String javadoc= super.processingEnv.getElementUtils().getDocComment(annotatedMethod);
    if (javadoc != null) {
      methodSpecBuilder.addJavadoc(javadoc);
    }

    typeSpecBuilder.addMethod(methodSpecBuilder.build());

    return initCodeBlockBuilder.build();
  }


  private static String toConstName(final String methodName) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, methodName);
  }


  private String getSuperGetterName(final TypeMirror type, final boolean hasDefaultValue, final Optional<TypeMirror> collectionType) {
    final StringBuilder sb= new StringBuilder("get");

    final TypeNameVisitor visitor= new TypeNameVisitor();
    final String typeName= type.accept(visitor, null);

    // FIXME: These types are used more often. They should be defined in a central place.
    final TypeMirror listType = this.processingEnv.getElementUtils().getTypeElement(List.class.getCanonicalName()).asType();
    final TypeMirror setType = this.processingEnv.getElementUtils().getTypeElement(Set.class.getCanonicalName()).asType();


    if (collectionType.isPresent()) {
      if (collectionType.get().toString().equals(Array.class.getCanonicalName())) {
        sb.append("Array");
      } else if (this.processingEnv.getTypeUtils().isAssignable(collectionType.get(), listType)) {
        sb.append("List");
      } else if (this.processingEnv.getTypeUtils().isAssignable(collectionType.get(), setType)) {
        sb.append("Set");
      }
    } else if (typeName.startsWith("java.util.Optional<")) {
      sb.append("Optional");
    } else if (typeName.startsWith("java.util.Optional")) {
      sb.append(typeName.substring("java.util.".length()));
    } else if (!typeName.contains(".")) {
      sb.append(upperFirstChar(typeName));
    }

    if (hasDefaultValue) {
      sb.append("OrDefault");
    }

    return sb.toString();
  }


  private static String upperFirstChar(final String s) {
    return String.valueOf(s.charAt(0)).toUpperCase() + s.subSequence(1, s.length());
  }


  private static ClassName toClassName(final String fqName) {
    final int lastDot = fqName.lastIndexOf('.');
    final String packageName;
    final String simpleName;
    if (lastDot > 0) {
      packageName = fqName.substring(0, lastDot);
      simpleName= fqName.substring(lastDot + 1);
    } else {
      packageName= "";
      simpleName= fqName;
    }

    return ClassName.get(packageName, simpleName);
  }


  private static ClassName deriveGeneratedClassName(final TypeElement annotatedInterface) {
    // TODO: Write test for this method
    final ClassName fqInterfaceName= toClassName(annotatedInterface.getQualifiedName().toString());
    final Coat.Config typeAnnotation= annotatedInterface.getAnnotation(Coat.Config.class);

    final String className;
    if (typeAnnotation != null && !typeAnnotation.className().trim().isEmpty()) {
      className= typeAnnotation.className();
    } else {
      if (fqInterfaceName.simpleName().startsWith("_")) {
        className= fqInterfaceName.simpleName().substring(1);
      } else {
        className= "Immutable" + fqInterfaceName.simpleName();
      }
    }

    return ClassName.get(fqInterfaceName.packageName(), className);
  }


  private static ClassName deriveGeneratedEnumName(final TypeElement annotatedInterface) {
    // TODO: Write test for this method
    final ClassName fqInterfaceName= toClassName(annotatedInterface.getQualifiedName().toString());
    final Coat.Config typeAnnotation= annotatedInterface.getAnnotation(Coat.Config.class);

    final String enumName;
    // FIXME: typeAnnotation should never be null here
    if (typeAnnotation != null && typeAnnotation.className() != null && !typeAnnotation.className().trim().isEmpty()) {
      enumName= typeAnnotation.className() + "Param";
    } else {
      if (fqInterfaceName.simpleName().startsWith("_")) {
        enumName= fqInterfaceName.simpleName().substring(1) + "Param";
      } else {
        enumName= fqInterfaceName.simpleName() + "Param";
      }
    }

    return ClassName.get(fqInterfaceName.packageName(), enumName);
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
  private Class<?> identifyGeneratedAnnotation() {
    try {
      final SourceVersion sourceVersion= super.processingEnv.getSourceVersion();
      return sourceVersion.compareTo(SourceVersion.RELEASE_8) > 0
           ? Class.forName("javax.annotation.processing.Generated")
           : Class.forName("javax.annotation.Generated");
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }


  private void addEqualsMethod(final TypeSpec.Builder typeSpecBuilder, final List<Element> annotatedMethods, final ClassName className) {
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


  private void addHashCodeMethod(final TypeSpec.Builder typeSpecBuilder, final List<Element> annotatedMethods) {
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


  private String createExampleContent(final List<ConfigParamSpec> configParamSpecs) {
    final StringBuilder sb= new StringBuilder();

    for (final ConfigParamSpec configParamSpec : configParamSpecs) {
      // add javadoc as comment
      final String javadoc = super.processingEnv.getElementUtils().getDocComment(configParamSpec.annotatedMethod());
      Utils.stripBlockTagsFromJavadoc(javadoc)
        .lines()
        .map(s -> "## " + s + "\n")
        .forEachOrdered(sb::append);

      // add a config key
      if (!configParamSpec.mandatory() || (configParamSpec.defaultValue() != null && !configParamSpec.defaultValue().trim().isEmpty())) {
        // commented out if optional or has default value
        sb.append("# ");
      }
      sb.append(configParamSpec.key()).append(" = ");
      sb.append(configParamSpec.defaultValue() != null ? configParamSpec.defaultValue() : "");
      sb.append("\n\n");
    }

    return sb.toString();
  }


  private void generateExampleFile(final String exampleFileName, final TypeElement annotatedInterface, final Filer filer) throws IOException {
    final List<ConfigParamSpec> configParamSpecs= this.getAnnotatedParamsRecursively(annotatedInterface);

    final FileObject resource = filer.createResource(javax.tools.StandardLocation.CLASS_OUTPUT, "examples", exampleFileName, annotatedInterface);
    try (final Writer w= resource.openWriter();) {
      w.write(this.createExampleContent(configParamSpecs));
    }
  }


  private List<ConfigParamSpec> getAnnotatedParamsRecursively(final TypeElement annotatedInterface) {
    final List<ConfigParamSpec> result= new ArrayList<>();

    // collect all accessor methods of this interface
    annotatedInterface.getEnclosedElements().stream()
      .filter(e -> e.getKind() == METHOD)
      .filter(e -> e.getAnnotation(Coat.Embedded.class) == null)
      .map(paramSpecBuilder::from)
      .forEachOrdered(result::add);

    // collect the accessor methods of all embedded configs
    final List<EmbeddedParamSpec> embeddingAccessors = annotatedInterface.getEnclosedElements().stream()
      .filter(e -> e.getKind() == METHOD)
      .filter(e -> e.getAnnotation(Coat.Embedded.class) != null)
      .map(paramSpecBuilder::embeddedFrom)
      .collect(toList());

    for (final EmbeddedParamSpec eps : embeddingAccessors) {
      final TypeElement embeddedConfig= this.getEmbeddedType(eps);

      final List<ConfigParamSpec> embeddedAccessors= getAnnotatedParamsRecursively(embeddedConfig);
      for (final ConfigParamSpec cps : embeddedAccessors) {
        final ConfigParamSpec embeddedCps= ImmutableConfigParamSpec.copyOf(cps)
          .withKey(eps.key() + eps.keySeparator() + cps.key());
        result.add(embeddedCps);
      }
    }

    return result;
  }


  private TypeElement getEmbeddedType(final EmbeddedParamSpec eps) throws RuntimeException {
    final TypeMirror returnType = eps.annotatedMethod().getReturnType();
    if (returnType.getKind() != DECLARED) {
      throw new RuntimeException("All embedded configs are expected to be of DECLARED kind. “"+eps.annotatedMethod()+"”s return type “"+returnType+"”is of kind “"+returnType.getKind());
    }

    final DeclaredType declaredReturnType= (DeclaredType) returnType;
    final TypeMirror baseType;
    final TypeMirror erasure = this.processingEnv.getTypeUtils().erasure(declaredReturnType);

    // TODO: Check erasure for collection types
    if (declaredReturnType.getTypeArguments().isEmpty()) {
      baseType= erasure;
    } else if (declaredReturnType.getTypeArguments().size() == 1) {
      baseType= declaredReturnType.getTypeArguments().get(0);
    } else {
      throw new RuntimeException("Multiple type parameters are not supported at the moment. Found “"+declaredReturnType.getTypeArguments()+"” on “"+eps.annotatedMethod()+"”.");
    }

    return (TypeElement) processingEnv.getTypeUtils().asElement(baseType);
  }


  private void assertReturnType(final List<ConfigParamSpec> annotatedMethods) throws CoatProcessorException {
    final List<ConfigParamSpec> missingReturnTypes= new ArrayList<>();

    // filter out all accessors without return type
    annotatedMethods.stream()
      .filter(this::hasVoidReturnType)
      .forEachOrdered(missingReturnTypes::add);

    if (!missingReturnTypes.isEmpty()) {
      final StringBuilder sb= new StringBuilder("Accessors without return type:\n");
      missingReturnTypes.forEach(accessor -> {
        sb.append("  ").append(accessor.annotatedMethod()).append(":\n");
          sb.append("    ")
            .append(toHumanReadableString(accessor))
            .append("\n\n");
      });

      processingEnv.getMessager().printMessage(Kind.ERROR, sb.toString());
      throw new CoatProcessorException(sb.toString());
    }
  }


  private void assertIsInterface(final TypeElement annotatedType) {
    if (annotatedType.getKind() != INTERFACE) {
      final String errorMsg= "@Coat.Config is only supported on interfaced at the moment:\n"
        + "  Non-interface type: "+annotatedType.toString();
      processingEnv.getMessager().printMessage(Kind.ERROR, errorMsg);
      throw new CoatProcessorException(errorMsg);
    }
  }


  private void assertEmbeddedTypeIsAnnotated(final List<EmbeddedParamSpec> annotatedMethods) throws CoatProcessorException {
    final List<EmbeddedParamSpec> unannotatedEmbeddedTypes= new ArrayList<>();

    // filter out all accessors without return type
    annotatedMethods.stream()
      .filter(this::hasEmbeddedAnnotation)
      .filter(not(this::hasEmbeddedTypeWithoutAnnotation))
      .forEachOrdered(unannotatedEmbeddedTypes::add);

    if (!unannotatedEmbeddedTypes.isEmpty()) {
      final StringBuilder sb= new StringBuilder("@Coat.Embedded annotation can only be applied to types that are annotated with @Coat.Config.\n");
      sb.append("Accessors with unannotated types:\n");
      unannotatedEmbeddedTypes.forEach(accessor -> {
        sb.append("  ").append(accessor.annotatedMethod()).append(":\n");
          sb.append("    ")
            .append(toHumanReadableString(accessor))
            .append("\n\n");
      });

      processingEnv.getMessager().printMessage(Kind.ERROR, sb.toString());
      throw new CoatProcessorException(sb.toString());
    }
  }


  private boolean hasVoidReturnType(final ConfigParamSpec accessor) {
    return this.hasVoidReturnType(accessor.annotatedMethod());
  }


  private boolean hasVoidReturnType(final ExecutableElement elm) {
    return elm.getReturnType().getKind() == VOID;
  }


  private void assertNoParameters(final List<ConfigParamSpec> annotatedMethods) throws CoatProcessorException {
    final List<ConfigParamSpec> withParameters= new ArrayList<>();

    // filter out all accessors without return type
    annotatedMethods.stream()
      .filter(this::hasParameters)
      .forEachOrdered(withParameters::add);

    if (!withParameters.isEmpty()) {
      final StringBuilder sb= new StringBuilder("Accessors with parameters:\n");
      withParameters.forEach(accessor -> {
        sb.append("  ").append(accessor.annotatedMethod()).append(":\n");
          sb.append("    ")
            .append(toHumanReadableString(accessor))
            .append("\n\n");
      });

      processingEnv.getMessager().printMessage(Kind.ERROR, sb.toString());
      throw new CoatProcessorException(sb.toString());
    }
  }


  private boolean hasParameters(final ConfigParamSpec accessor) {
    return this.hasParameters(accessor.annotatedMethod());
  }


  private boolean hasParameters(final ExecutableElement elm) {
    return !elm.getParameters().isEmpty();
  }


  private void reduceDuplicateAccessors(final List<ConfigParamSpec> annotatedMethods) throws CoatProcessorException {
    final FastListMultimap<String, ConfigParamSpec> duplicateAccessors  = FastListMultimap.newMultimap();
    final FastListMultimap<String, ConfigParamSpec> conflictingAccessors= FastListMultimap.newMultimap();

    // gather all duplicate and conflicting accessors
    for (final ConfigParamSpec annotatedMethod : annotatedMethods) {
      for (final ConfigParamSpec otherMethod : annotatedMethods) {
        // don't compare an accessor with itself
        if (annotatedMethod == otherMethod) {
          continue;
        }

        // accessors with different names don't conflict
        if (!annotatedMethod.methodeName().equals(otherMethod.methodeName())) {
          continue;
        }

        // Equally named ccessors with the same return type, key, default value, mandatoriness
        // are duplicates. One of them can be removed.
        // If at least one of these attribute differs, the accessor conflict with each other.
        if (annotatedMethod.key()         .equals(otherMethod.key())
         && annotatedMethod.type()        .equals(otherMethod.type())
         && annotatedMethod.defaultValue().equals(otherMethod.defaultValue())
         && annotatedMethod.mandatory()        == otherMethod.mandatory()) {
          if (!duplicateAccessors.containsValue(annotatedMethod)) {
            duplicateAccessors.put(annotatedMethod.methodeName(), annotatedMethod);
          }
          if (!duplicateAccessors.containsValue(otherMethod)) {
            duplicateAccessors.put(annotatedMethod.methodeName(), otherMethod);
          }
        } else {
          if (!conflictingAccessors.containsValue(annotatedMethod)) {
            conflictingAccessors.put(annotatedMethod.methodeName(), annotatedMethod);
          }
          if (!conflictingAccessors.containsValue(otherMethod)) {
            conflictingAccessors.put(annotatedMethod.methodeName(), otherMethod);
          }
        }
      }
    }

    // reduce duplicate accessors to only the first occurrence
    duplicateAccessors.forEachKey(accessor -> {
      duplicateAccessors.get(accessor)
        .forEachWithIndex((duplicate, idx) -> {
          if (idx > 0) {
            annotatedMethods.remove(duplicate);
          }
        });
    });

    // error out on conflicting accessors
    if (!conflictingAccessors.isEmpty()) {
      final StringBuilder sb= new StringBuilder("Conflicting accessor methods:\n");
      conflictingAccessors.forEachKeyMultiValues((accessor, conflicting) -> {
        sb.append("  ").append(accessor).append("():\n");
        conflicting.forEach(a -> {
          sb.append("    ")
            .append(toHumanReadableString(a))
            .append("\n\n");
        });
      });

      processingEnv.getMessager().printMessage(Kind.ERROR, sb.toString());
      throw new CoatProcessorException(sb.toString());
    }
  }


  private void assertUniqueKeys(final List<ConfigParamSpec> annotatedMethods) throws CoatProcessorException {
    final Map<String, List<ConfigParamSpec>> existingKeys   = new HashMap<>();
    final Map<String, List<ConfigParamSpec>> duplicateKeys  = new HashMap<>();

    // collect all keys and their corresponding accessor methods
    for (final ConfigParamSpec accessor : annotatedMethods) {
      if (!existingKeys.containsKey(accessor.key())) {
        existingKeys.put(accessor.key(), new ArrayList<>());
      }
      existingKeys.get(accessor.key()).add(accessor);
    }

    // filter out all keys with more than 1 accessor method
    existingKeys.entrySet().stream()
      .filter(e -> e.getValue().size() > 1)
      .forEach(e -> duplicateKeys.put(e.getKey(), e.getValue()));

    if (!duplicateKeys.isEmpty()) {
      final StringBuilder sb= new StringBuilder("Duplicate keys:\n");
      duplicateKeys.forEach((accessor, duplicates) -> {
        sb.append("  ").append(accessor).append(":\n");
        duplicates.forEach(a -> {
          sb.append("    ")
            .append(toHumanReadableString(a))
            .append("\n\n");
        });
      });

      processingEnv.getMessager().printMessage(Kind.ERROR, sb.toString());
      throw new CoatProcessorException(sb.toString());
    }
  }


  private String toHumanReadableString(final ConfigParamSpec configParamSpec) {
    final StringBuilder sb= new StringBuilder();

    sb.append("@Coat.Param(");
    sb.append("key = \"").append(configParamSpec.key()).append("\"");
    if (configParamSpec.defaultValue() != null && !configParamSpec.defaultValue().isBlank()) {
      sb.append(", defaultValue = \"").append(configParamSpec.defaultValue()).append("\"");
    }
    sb.append(")\n    ");
    sb.append(configParamSpec.annotatedMethod().getEnclosingElement());
    sb.append("#");
    sb.append(configParamSpec.annotatedMethod());
    sb.append(" : ");
    sb.append(configParamSpec.annotatedMethod().getReturnType());

    return sb.toString();
  }


  private String toHumanReadableString(final EmbeddedParamSpec configParamSpec) {
    final StringBuilder sb= new StringBuilder();

    sb.append("@Coat.Embedded(");
    sb.append("key = \"").append(configParamSpec.key()).append("\"");
    if (configParamSpec.keySeparator() != null && !configParamSpec.keySeparator().isBlank()) {
      sb.append(", keySeparator = \"").append(configParamSpec.keySeparator()).append("\"");
    }
    sb.append(")\n    ");
    sb.append(configParamSpec.annotatedMethod().getEnclosingElement());
    sb.append("#");
    sb.append(configParamSpec.annotatedMethod());
    sb.append(" : ");
    sb.append(configParamSpec.annotatedMethod().getReturnType());

    return sb.toString();
  }


  private boolean hasEmbeddedAnnotation(final EmbeddedParamSpec configParamSpec) {
    final Coat.Embedded[] embeddedAnnotations = configParamSpec.annotatedMethod().getAnnotationsByType(Coat.Embedded.class);
    return embeddedAnnotations.length > 0;
  }


  private boolean hasEmbeddedTypeWithoutAnnotation(final EmbeddedParamSpec configParamSpec) {
    final TypeElement embeddedType = this.getEmbeddedType(configParamSpec);
    final Coat.Config[] configAnnotations = embeddedType.getAnnotationsByType(Coat.Config.class);
    return configAnnotations.length > 0;
  }


  private Optional<CodeBlock> addRegisterCustomConverters(final TypeElement annotatedInterface) {
    // add custom converters
    final List<TypeMirror> customConverters= this.getAnnotationValue(annotatedInterface, "converters");

    if (customConverters.isEmpty()) {
      return Optional.empty();
    }

    // The code  to initialize this field. Needs to be added to the typeSpecs constructors
    final CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
    codeBlockBuilder.add("\n");

    customConverters.
      forEach((c) -> {
        codeBlockBuilder.addStatement(
          "super.registerCustomConverter(new $L())",
          c.toString()
        );
      });

    return Optional.of(codeBlockBuilder.build());
  }


  private Optional<CodeBlock> addRegisterCustomListParser(final TypeElement annotatedInterface) {
    // add custom converters
    final List<TypeMirror> customConverters= this.getAnnotationValue(annotatedInterface, "listParser");

    if (customConverters.isEmpty()) {
      return Optional.empty();
    }

    // The code  to initialize this field. Needs to be added to the typeSpecs constructors
    final CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
    codeBlockBuilder.add("\n");

    customConverters.
      forEach((c) -> {
        codeBlockBuilder.addStatement(
          "super.registerListParser(new $L())",
          c.toString()
        );
      });

    return Optional.of(codeBlockBuilder.build());
  }


  private List<TypeMirror> getAnnotationValue(final TypeElement annotatedInterface, final String valueName) {
    final List<? extends AnnotationMirror> annotationMirrors = annotatedInterface.getAnnotationMirrors();

    final TypeMirror coatType= this.processingEnv.getElementUtils().getTypeElement(Coat.Config.class.getCanonicalName()).asType();

    final List<TypeMirror> converters= new ArrayList<>();
    final ClassTypeArrayVisitor visitor= new ClassTypeArrayVisitor(converters);

    for (final AnnotationMirror annotationMirror : annotationMirrors) {
      // only process @Coat.Config annotations
      if (!this.processingEnv.getTypeUtils().isAssignable(annotationMirror.getAnnotationType(), coatType)) {
        continue;
      }

      // search for the given value
      final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
      for (final Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
        final ExecutableElement key = entry.getKey();
        if (!key.getSimpleName().toString().equals(valueName)) {
          continue;
        }

        final AnnotationValue val = entry.getValue();
        val.accept(visitor, null);
      }
    }

    return converters;
  }
}
