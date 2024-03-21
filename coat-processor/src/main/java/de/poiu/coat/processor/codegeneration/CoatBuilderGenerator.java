/*
 * Copyright (C) 2020 - 2024 The Coat Authors
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
package de.poiu.coat.processor.codegeneration;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.poiu.coat.CoatConfigBuilder;
import de.poiu.coat.c14n.KeyC14n;
import de.poiu.coat.processor.specs.AccessorSpec;
import de.poiu.coat.processor.specs.ClassSpec;
import de.poiu.coat.processor.specs.EmbeddedTypeSpec;
import de.poiu.coat.processor.utils.JavadocHelper;
import de.poiu.coat.processor.utils.NameUtils;
import de.poiu.coat.processor.utils.SpecHelper;
import de.poiu.coat.validation.ConfigValidationException;
import de.poiu.coat.validation.ImmutableValidationFailure;
import de.poiu.coat.validation.ImmutableValidationResult;
import de.poiu.coat.validation.ValidationFailure;
import java.io.File;
import java.io.IOException;
import java.lang.System.Logger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.PRIVATE;

import de.poiu.coat.CoatParam;
import de.poiu.coat.processor.examplecontent.ExampleContentHelper;
import java.io.Writer;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;


/**
 * Helper class for generating the builder code for a CoatConfigBuilder class.
 */
public class CoatBuilderGenerator {


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final ProcessingEnvironment   pEnv;
  private final SpecHelper              specHelper;
  private final ExampleContentHelper    exampleContentHelper;

  private final ConfigImplCodeGenerator configImplGenerator;
  private final ParamImplCodeGenerator  paramImplGenerator;

  private final ClassSpec               annotatedInterface;
  private final ClassName               builderClassName;


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  private CoatBuilderGenerator(final ClassSpec             annotatedInterface,
                               final ProcessingEnvironment processingEnv) {
    this.pEnv                = processingEnv;
    this.specHelper          = new SpecHelper(pEnv);
    this.exampleContentHelper= new ExampleContentHelper(pEnv);
    this.configImplGenerator = new ConfigImplCodeGenerator(pEnv);
    this.paramImplGenerator  = new ParamImplCodeGenerator(pEnv);
    this.annotatedInterface  = annotatedInterface;
    this.builderClassName    = ClassName.get(annotatedInterface.targetPackage(), annotatedInterface.builderName());
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * Create a new CoatBuilderGenerator for the specified CoatConfigBuilder class
   * @param annotatedInterface
   * @param processingEnv
   * @return a new CoatBuilderGenerator
   */
  public static CoatBuilderGenerator forType(final ClassSpec             annotatedInterface,
                                             final ProcessingEnvironment processingEnv) {
    return new CoatBuilderGenerator(annotatedInterface, processingEnv);
  }


  public void generateAndWriteToFile() throws IOException {
    final TypeSpec builderClass= this.generateBuilderClass();

    JavaFile.builder(builderClassName.packageName(), builderClass)
      .build()
      .writeTo(this.pEnv.getFiler());
  }


  /**
   * Generates the actual builder for creating a new Coat Config object.
   * @return
   */
  public TypeSpec generateBuilderClass() {
    final TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(builderClassName)
      .superclass(CoatConfigBuilder.class)
      .addModifiers(PUBLIC)
      .addJavadoc(""
        + "Builder class for creating new {@link $T} instances.\n"
        + "<p>\n"
        + "Create the builder by calling {@link #create()}. Then call the <code>add</code> and/or\n"
        + "<code>addEnvVars</code> methods for specifying the config sources (and the order in which\n"
        + "they are applied), then call {@link #build()} to create the $T.",
           this.annotatedInterface.annotatedType(), this.annotatedInterface.annotatedType())
      .addField(this.generatePrivateStaticFinalLogger())
      .addField(this.generateFieldDefaults())
      .addField(this.generateFieldProps())
      .addMethod(this.generateConstructor())
      .addMethods(this.generateStaticFactoryMethods())
      .addMethod(this.generateBuilderMethod())
      .addMethod(this.generateMethodAddMap())
      .addMethod(this.generateMethodAddFile())
      .addMethod(this.generateMethodAddProperties())
      .addMethod(this.generateMethodAddEnvVars())
      .addMethod(this.generateMethodCreateToString())
      .addMethod(this.generateWriteExampleConfigMethod())
      .addMethod(this.generateMethodBuild())
      ;

    this.generateGeneratedAnnotation()
      .ifPresent(typeSpecBuilder::addAnnotation);

    if (!this.annotatedInterface.accessors().isEmpty()) {
      // not creating an enum if there is no accessor for it
      typeSpecBuilder.addType(this.paramImplGenerator.generateEnumCode(this.annotatedInterface));
    }
    typeSpecBuilder.addType(this.configImplGenerator.generateClassCode(this.annotatedInterface));

    return typeSpecBuilder.build();
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


  private FieldSpec generatePrivateStaticFinalLogger() {
    return FieldSpec.builder(Logger.class, "LOGGER")
      .addModifiers(PRIVATE, STATIC, FINAL)
      .initializer("System.getLogger($L.class.getName())", this.builderClassName)
      .build();
  }


  /**
   * Generates the method for creating a new Builder.
   *
   * @return
   */
  private MethodSpec generateBuilderMethod() {
    final MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("create")
      .addModifiers(PUBLIC, STATIC)
      .returns(this.builderClassName)
      .addStatement("return new $T()", this.builderClassName)
      .addJavadoc(""
        + "Create a builder for {@link $T} instances.\n"
        + "<p>\n"
        + "Call the <code>add</code> and/or <code>addEnvVars</code> methods for specifying the config\n"
        + "sources (and the order in which they are applied), then call {@link #build()} to create the\n"
        + "$T\n"
        + "\n"
        + "@return a new $T", this.annotatedInterface.annotatedType(), this.annotatedInterface.annotatedType(), this.builderClassName)
      ;

    return methodSpecBuilder.build();
  }


  private List<MethodSpec> generateStaticFactoryMethods() {
    final List<MethodSpec> methodSpecs= new ArrayList<>();

    methodSpecs.add(
      MethodSpec.methodBuilder("from")
        .addModifiers(PUBLIC, STATIC)
        .returns(TypeName.get(this.annotatedInterface.annotatedType().asType()))
        .addException(ConfigValidationException.class)
        .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "props", FINAL)
        .addStatement("return new $T().add(props).build()", this.builderClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_MAP, this.annotatedInterface.annotatedType(), this.annotatedInterface.annotatedType())
        .build());

    methodSpecs.add(
      MethodSpec.methodBuilder("from")
        .addModifiers(PUBLIC, STATIC)
        .returns(TypeName.get(this.annotatedInterface.annotatedType().asType()))
        .addException(ConfigValidationException.class)
        .addParameter(File.class, "file", FINAL)
        .addStatement("return new $T().add(file).build()", this.builderClassName)
        .addException(IOException.class)
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_FILE, this.annotatedInterface.annotatedType(), this.annotatedInterface.annotatedType())
        .build());

    methodSpecs.add(
      MethodSpec.methodBuilder("from")
        .addModifiers(PUBLIC, STATIC)
        .returns(TypeName.get(this.annotatedInterface.annotatedType().asType()))
        .addException(ConfigValidationException.class)
        .addParameter(Properties.class, "jup", FINAL)
        .addStatement("return new $T().add(jup).build()", this.builderClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_PROPERTIES, this.annotatedInterface.annotatedType(), this.annotatedInterface.annotatedType())
        .build());

    methodSpecs.add(
      MethodSpec.methodBuilder("fromEnvVars")
        .addModifiers(PUBLIC, STATIC)
        .returns(TypeName.get(this.annotatedInterface.annotatedType().asType()))
        .addException(ConfigValidationException.class)
        .addStatement("return new $T().addEnvVars().build()", this.builderClassName)
        .addJavadoc(JavadocHelper.JAVADOC_ON_FROM_ENV_VARS, this.annotatedInterface.annotatedType(), this.annotatedInterface.annotatedType())
        .build());

    return methodSpecs;
  }


  private MethodSpec generateConstructor() {
    final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
      .addModifiers(PRIVATE);

    // add the final properties to the CoatConfigBuilder
    if (this.annotatedInterface.accessors().isEmpty()) {
      // if there are no annotated methods, no enum is being generated (due to a bug in JavaPoet)
      // therefore we cannot refer to it and instaed use an empty CoatParam array
      // see https://github.com/square/javapoet/issues/739
      // and https://github.com/square/javapoet/issues/832
      constructorBuilder.addStatement("super(new $T[]{})", CoatParam.class);
    } else {
      constructorBuilder.addStatement("super(ParamImpl.values())");
    }

    constructorBuilder.addCode("\n// set default values\n");
    for (final AccessorSpec accessor : this.annotatedInterface.accessors()) {
      if (accessor.defaultValue() != null && !accessor.defaultValue().isBlank()) {
        constructorBuilder.addStatement("this.defaults.put($S, $S)", accessor.key(), accessor.defaultValue());
      }
    }

    return constructorBuilder.build();
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
    final String      generatedTypeName= NameUtils.deriveGeneratedBuilderName(returnTypeElement);

    // The code to initialize this field. Needs to be added to the typeSpecs constructors
    final CodeBlock.Builder initCodeBlockBuilder = CodeBlock.builder();
    initCodeBlockBuilder.add("\n");
    initCodeBlockBuilder.addStatement("$T $N= null",
                                      embeddedTypeSpec.type(),
                                      embeddedTypeSpec.methodName());
    initCodeBlockBuilder.beginControlFlow("try");
      if (isOptional) {
        initCodeBlockBuilder.beginControlFlow("if (hasPrefix(props, $S))",
          embeddedTypeSpec.key() + embeddedTypeSpec.keySeparator())
          .addStatement("$N= $L.from(filterByAndStripPrefix(props, $S))",
                      embeddedTypeSpec.methodName(),
                      generatedTypeName,
                      embeddedTypeSpec.key() + embeddedTypeSpec.keySeparator())
          .nextControlFlow("else")
          .addStatement("$N= null", embeddedTypeSpec.methodName())
          .endControlFlow();
      } else {
        initCodeBlockBuilder
          .addStatement("$N= $L.from(filterByAndStripPrefix(props, $S))",
                      embeddedTypeSpec.methodName(),
                      generatedTypeName,
                      embeddedTypeSpec.key() + embeddedTypeSpec.keySeparator());
      }
    initCodeBlockBuilder.nextControlFlow("catch ($T ex)", ConfigValidationException.class);
      initCodeBlockBuilder.beginControlFlow("for (final $T vf : ex.getValidationResult().validationFailures())",
                                            ValidationFailure.class);
        initCodeBlockBuilder.addStatement("validationFailures.add($T.copyOf(vf).withKey($S + vf.key()))",
                                          ImmutableValidationFailure.class,
                                          embeddedTypeSpec.key() + embeddedTypeSpec.keySeparator());
      initCodeBlockBuilder.endControlFlow();
    initCodeBlockBuilder.endControlFlow();

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
          "super.registerCustomListParser(new $T())",
          annotatedInterface.listParser().get()
        );

    return Optional.of(codeBlockBuilder.build());
  }


  private FieldSpec generateFieldDefaults() {
    return FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "defaults", PRIVATE, FINAL)
      .initializer("new $T<>()", TypeName.get(HashMap.class))
      .build()
      ;
  }


  private FieldSpec generateFieldProps() {
    return FieldSpec.builder(ParameterizedTypeName.get(Map.class, String.class, String.class), "props", PRIVATE, FINAL)
      .initializer("new $T<>()", TypeName.get(HashMap.class))
      .build()
      ;
  }


  private MethodSpec generateMethodAddMap() {
    return MethodSpec.methodBuilder("add")
      .addModifiers(PUBLIC)
      .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "map", FINAL)
      .returns(this.builderClassName)
      .addStatement("this.props.putAll(map)")
      .addStatement("return this")
      .addJavadoc(""
        + "Add the config entries from the given Map to the built $T.\n"
        + "Already existing config entries with the same keys will be overwritten.\n"
        + "\n"
        + "@param map the config entries to add\n"
        + "@return this Builder", this.annotatedInterface.annotatedType())
      .build();
  }


  private MethodSpec generateMethodAddFile() {
    return MethodSpec.methodBuilder("add")
      .addModifiers(PUBLIC)
      .addParameter(TypeName.get(File.class), "file", FINAL)
      .returns(this.builderClassName)
      .addException(TypeName.get(IOException.class))
      .addStatement("this.props.putAll(toMap(file))")
      .addStatement("return this")
      .addJavadoc(""
        + "Add the config entries from the given file to the built $T.\n"
        + "Already existing config entries with the same keys will be overwritten.\n"
        + "\n"
        + "@param file the file with the config entries to add\n"
        + "@return this Builder\n"
        + "@throws java.io.IOException if reading the config file failed", this.annotatedInterface.annotatedType())
      .build();
  }


  private MethodSpec generateMethodAddProperties() {
    return MethodSpec.methodBuilder("add")
      .addModifiers(PUBLIC)
      .addParameter(TypeName.get(Properties.class), "jup", FINAL)
      .returns(this.builderClassName)
      .addStatement("this.props.putAll(toMap(jup))")
      .addStatement("return this")
      .addJavadoc(""
        + "Add the config entries from the given Properties to the built $T.\n"
        + "Already existing config entries with the same keys will be overwritten.\n"
        + "\n"
        + "@param jup the config entries to add\n"
        + "@return this Builder", this.annotatedInterface.annotatedType())
      .build();
  }


  private MethodSpec generateMethodAddEnvVars() {
    return MethodSpec.methodBuilder("addEnvVars")
      .addModifiers(PUBLIC)
      .returns(this.builderClassName)
      .addStatement("final Map<String, String> envVars= System.getenv()")
      .addStatement("final String[] configKeys= {\n" +
        "$L\n" +
        "}", this.getParamNamesString())
      .addCode("\n")
      .beginControlFlow("for (final String envVar : envVars.keySet())")
        .beginControlFlow("for (final String configKey : configKeys)")
          .beginControlFlow("if (envVar.toUpperCase().equals($T.c14n(configKey)))", KeyC14n.class)
            .addStatement("LOGGER.log($T.INFO, \"Using environment variable {0} as config key {1}\", new Object[]{envVar, configKey})", Logger.Level.class)
            .addStatement("this.props.put(configKey, envVars.get(envVar))")
          .endControlFlow()
        .endControlFlow()
      .endControlFlow()
      .addCode("\n")
      .addStatement("return this")
      .addJavadoc(""
        + "Add the config entries from the current environment variables to the built $T.\n"
        + "Already existing config entries with the same keys will be overwritten.\n"
        + "<p>\n"
        + "Since the allowed characters for environment variables are much more restricted than Coat config keys,\n"
        + "a relaxed mapping is applied.\n"
        + "<p>"
        + "Dots and hyphens are treated as underscores. Also uppercase\n"
        + "characters in config keys are preceded by an underscore (to convert camelCase to UPPER_CASE).\n"
        + "Comparison between the environment variables and the config keys is done case insensitively."
        + "<p>\n"
        + "For example the environment variable\n"
        + "<code>SERVER_MQTT_HOST</code> will match the config key <code>server.mqttHost</code>."
        + "\n"
        + "@return this Builder", this.annotatedInterface.annotatedType())
      .build();
  }


  private MethodSpec generateMethodCreateToString() {
    final MethodSpec.Builder methodBuilder=
      MethodSpec.methodBuilder("createToString")
        .addModifiers(PRIVATE)
        .returns(String.class);

    // add the expected parameters
    for (final EmbeddedTypeSpec embeddedTypeSpec : this.annotatedInterface.embeddedTypes()) {
      methodBuilder.addParameter(
        TypeName.get(embeddedTypeSpec.accessor().getReturnType()),
        embeddedTypeSpec.methodName(),
        FINAL);
    }

    methodBuilder
      .addStatement("final $T sb= new $T()", StringBuilder.class, StringBuilder.class)
      .addStatement("sb.append($S)", "{\n");

    // gather and print the direct accessors
    methodBuilder
      .addCode("\n// print the direct accessors\n")
      .addStatement("sb.append(super.getParamStrings())");

    // print embedded accessors
    methodBuilder.addCode("\n// print the embedded accessors");
    for (final EmbeddedTypeSpec embeddedTypeSpec : this.annotatedInterface.embeddedTypes()) {
      final boolean optional= !embeddedTypeSpec.mandatory();
      final String fieldName= embeddedTypeSpec.methodName();
      methodBuilder
        .addCode("\n")
        .addStatement("sb.append($S).append($S).append($S)", optional ? "  ?" : "  ", fieldName, ":");
      if (!embeddedTypeSpec.mandatory()) {
        methodBuilder.beginControlFlow("if ($L.isPresent())", fieldName);
      }
      methodBuilder
        .addStatement("final $T $LToString= $L.toString()", String.class, fieldName, optional ? fieldName+".get()" : fieldName)
        .addStatement(fieldName+"ToString.lines()\n"
          + ".map(s -> \"  \"+s+'\\n')\n"
          + ".forEachOrdered(sb::append)");
      if (!embeddedTypeSpec.mandatory()) {
        methodBuilder
          .nextControlFlow("else")
          .addStatement("sb.append($S)", "  null\n")
          .endControlFlow();
      }
    }

    methodBuilder
      .addCode("\n// finish string\n")
      .addStatement("sb.append('}')")
      .addStatement("return sb.toString()");

    return methodBuilder.build();
  }


  private MethodSpec generateWriteExampleConfigMethod() {
    final String exampleContent= this.exampleContentHelper.createExampleContent(this.annotatedInterface);

    return MethodSpec.methodBuilder("writeExampleConfig")
        .addModifiers(PUBLIC, STATIC)
        .addParameter(TypeName.get(Writer.class), "writer", FINAL)
        .addStatement("writer.append($S)", exampleContent)
        .addStatement("writer.flush()")
        .addException(IOException.class)
        .addJavadoc(JavadocHelper.JAVADOC_ON_WRITE_EXAMPLE_CONFIG)
        .build();
  }


  private MethodSpec generateMethodBuild() {
    final MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("build")
      .addModifiers(PUBLIC)
      .returns(TypeName.get(this.annotatedInterface.annotatedType().asType()))
      .addException(ConfigValidationException.class)
      .addJavadoc(""
        + "Build a new {@link $T} with the config keys from this Builder.\n"
        + "\n"
        + "@return a new $T\n"
        + "@throws ConfigValidationException", this.annotatedInterface.annotatedType(), this.annotatedInterface.annotatedType())
      .addStatement("super.add(this.defaults)")
      .addStatement("super.add(this.props)");

    // validate all the direct fields
    methodBuilder.addCode("\n");
    methodBuilder.addStatement("final $T<$T> validationFailures= super.validate()",
                               List.class, ValidationFailure.class);

    // create initializers for all embedded configs
    methodBuilder.addCode("\n");
    final List<CodeBlock> initCodeBlocks= this.prepareInitCodeBlocks(annotatedInterface);
    for (final CodeBlock initEmbeddedConfig : initCodeBlocks) {
      methodBuilder.addCode(initEmbeddedConfig);
    }

    // throw exception on validation failures
    methodBuilder.addCode("\n");
    methodBuilder.beginControlFlow("if (!validationFailures.isEmpty())");
    methodBuilder.addStatement("final $T resultBuilder= $T.builder()",
                               ImmutableValidationResult.Builder.class, ImmutableValidationResult.class);
    methodBuilder.addStatement("resultBuilder.addAllValidationFailures(validationFailures)");
    methodBuilder.addStatement("throw new $T(resultBuilder.build())", ConfigValidationException.class);
    methodBuilder.endControlFlow();

    // generate string representation
    methodBuilder.addCode("\n");
    final List<String> embeddedConfigs= this.annotatedInterface.embeddedTypes().stream()
      .map(e -> e.mandatory() ? e.methodName() : "Optional.ofNullable("+e.methodName()+")")
      .collect(toList());
    methodBuilder.addStatement("final String toString= this.createToString($L)",
                                   String.join(", ", embeddedConfigs));

    // call the constuctor of the actual config class
    methodBuilder.addCode("\n");
    final CodeBlock.Builder codeBlockBuilder= CodeBlock.builder()
      .add("return new ConfigImpl(");

    final List<String> constructorArguments= new ArrayList<>();
    constructorArguments.add("toString");

    for (int i= 0; i < this.annotatedInterface.accessors().size(); i++) {
      final AccessorSpec accessorSpec = this.annotatedInterface.accessors().get(i);
      final ExecutableElement accessor = accessorSpec.accessor();

      final String    constName = NameUtils.toConstName(accessorSpec.key());

      final String defaultValue= accessorSpec.defaultValue() != null && !accessorSpec.defaultValue().trim().isEmpty()
                                 ? accessorSpec.defaultValue()
                                 : "";

      if (!accessorSpec.mandatory() && !defaultValue.trim().isEmpty()) {
        this.pEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                           "Optional and default value don't make much sense together. The Optional will never be empty.",
                                           accessor);
      }

      final String getter= this.specHelper.getSuperGetterName(accessorSpec);

      constructorArguments.add("super." + getter + "(ParamImpl." + constName + ")");
    }

    for (final EmbeddedTypeSpec embeddedTypeSpec : this.annotatedInterface.embeddedTypes()) {
      final boolean isOptional= !embeddedTypeSpec.mandatory();
      final String  fieldName = embeddedTypeSpec.methodName();
      if (isOptional) {
        constructorArguments.add("Optional.ofNullable("+fieldName+")");
      } else {
        constructorArguments.add(fieldName);
      }
    }

    codeBlockBuilder.add("\n  ");
    codeBlockBuilder.add(String.join(",\n  ", constructorArguments));
    codeBlockBuilder.add("\n);");

    methodBuilder.addCode(codeBlockBuilder.build());

    return methodBuilder.build();
  }


  private Optional<AnnotationSpec> generateGeneratedAnnotation() {
    final Class<?> generatedAnnotationClass = this.identifyGeneratedAnnotation();
    if (generatedAnnotationClass != null) {
      return Optional.of(AnnotationSpec.builder(generatedAnnotationClass)
        .addMember("value", "$S", this.getClass().getName())
        .addMember("date", "$S", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .build());
    } else {
      return Optional.empty();
    }
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
   * Returns a string with the names of all accessors (also inherited ones), one per line.
   * Each accessor is surrounded by double quotes and all will be separated by an comma.
   *
   * @return
   */
  private String getParamNamesString() {
    return SpecHelper
      .getAccessorSpecsRecursively(this.annotatedInterface)
      .stream()
      .map(AccessorSpec::key)
      .collect(joining("\",\n\"", "\"", "\""));
  }
}
