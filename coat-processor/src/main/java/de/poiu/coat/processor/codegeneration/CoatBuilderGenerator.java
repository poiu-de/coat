/*
 * Copyright (C) 2020 - 2023 The Coat Authors
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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.poiu.coat.c14n.KeyC14n;
import de.poiu.coat.processor.specs.AccessorSpec;
import de.poiu.coat.processor.specs.ClassSpec;
import de.poiu.coat.processor.utils.SpecHelper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.processing.ProcessingEnvironment;

import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;


/**
 * Helper class for generating the builder code for a CoatConfig class.
 */
public class CoatBuilderGenerator {


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final ProcessingEnvironment pEnv;
  private final SpecHelper            specHelper;

  private final ClassSpec             annotatedInterface;
  private final ClassName             coatConfigClassName;
  private final ClassName             builderClassName;


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  private CoatBuilderGenerator(final ClassSpec             annotatedInterface,
                               final ClassName             fqClassName,
                               final ProcessingEnvironment processingEnv) {
    this.pEnv                = processingEnv;
    this.specHelper          = new SpecHelper(pEnv);
    this.annotatedInterface  = annotatedInterface;
    this.coatConfigClassName = fqClassName;
    this.builderClassName    = fqClassName.nestedClass("Builder");
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * Create a new CoatBuilderGenerator for the specified CoatConfig class
   * @param annotatedInterface
   * @param coatConfigClassName
   * @param processingEnv
   * @return a new CoatBuilderGenerator
   */
  public static CoatBuilderGenerator forType(final ClassSpec             annotatedInterface,
                                             final ClassName             coatConfigClassName,
                                             final ProcessingEnvironment processingEnv) {
    return new CoatBuilderGenerator(annotatedInterface, coatConfigClassName, processingEnv);
  }


  /**
   * Generates the method for creating a new Builder.
   *
   * @return
   */
  public MethodSpec generateBuilderMethod() {
    final MethodSpec.Builder methodSpecBuilder = MethodSpec.methodBuilder("builder")
      .addModifiers(PUBLIC, STATIC)
      .returns(this.builderClassName)
      .addStatement("return new Builder()")
      .addJavadoc(""
        + "Create a builder for {@link $T} instances.\n"
        + "<p>\n"
        + "Call the <code>add</code> and/or <code>addEnvVars</code> methods for specifying the config\n"
        + "sources (and the order in which they are applied), then call {@link #build()} to create the\n"
        + "$T\n"
        + "\n"
        + "@return an new $T builder", coatConfigClassName, coatConfigClassName, coatConfigClassName)
      ;

    return methodSpecBuilder.build();
  }


  /**
   * Generates the actual builder for creating a new Coat Config object.
   * @return
   */
  public TypeSpec generateBuilderClass() {
    final TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(builderClassName)
      .addModifiers(PUBLIC, STATIC)
      .addJavadoc(""
        + "Builder class for creating new {@link $T} instances.\n"
        + "<p>\n"
        + "Call the <code>add</code> and/or <code>addEnvVars</code> methods for specifying the config\n"
        + "sources (and the order in which they are applied), then call {@link #build()} to create the\n"
        + "$T", coatConfigClassName, coatConfigClassName)
      .addField(this.generateFieldProps())
      .addMethod(this.generateMethodAddMap())
      .addMethod(this.generateMethodAddFile())
      .addMethod(this.generateMethodAddProperties())
      .addMethod(this.generateMethodAddEnvVars())
      .addMethod(this.generateMethodBuild())
      ;

    return typeSpecBuilder.build();
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


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
        + "@return this Builder", this.coatConfigClassName)
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
        + "@throws java.io.IOException if reading the config file failed", this.coatConfigClassName)
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
        + "@return this Builder", this.coatConfigClassName)
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
        + "@return this Builder", this.coatConfigClassName)
      .build();
  }


  private MethodSpec generateMethodBuild() {
    return MethodSpec.methodBuilder("build")
      .addModifiers(PUBLIC)
      .returns(this.coatConfigClassName)
      .addStatement("return new $T(this.props)", this.coatConfigClassName)
      .addJavadoc(""
        + "Build a new {@link $T} with the config keys from this Builder.\n"
        + "\n"
        + "@return a new $T", this.coatConfigClassName, this.coatConfigClassName)
      .build();
  }


  /**
   * Returns a string with the names of all accessors (also inherited ones), one per line.
   * Each accessor is surrounded by double quotes and all will be separated by an comma.
   *
   * @param classSpec
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
