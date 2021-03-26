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

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjects;
import de.poiu.coat.CoatConfig;
import de.poiu.coat.validation.ConfigValidationException;
import de.poiu.coat.validation.ImmutableValidationFailure;
import de.poiu.coat.validation.ValidationFailure;
import de.poiu.coat.validation.ValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.Compiler.javac;
import static de.poiu.coat.validation.ValidationFailure.Type.MISSING_MANDATORY_VALUE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


/**
 *
 */
public class CoatProcessorIT {

  private ByteClassLoader byteClassLoader;

  @BeforeEach
  public void setUp() {
    this.byteClassLoader= new ByteClassLoader(this.getClass().getClassLoader());
  }


  /**
   * A full test of a simple Coat config object.
   *
   * @throws Exception
   */
  @Test
  public void testBasicConfig() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.Optional;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryString\")" +
            "\n" + "  public String mandatoryString();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalInt\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"charsetWithDefault\", defaultValue = \"UTF-8\")" +
            "\n" + "  public Charset charsetWithDefault();" +
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass,
                       "mandatoryString",
                       "optionalInt",
                       "charsetWithDefault");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "mandatoryString", "some value",
      "optionalInt", "25"
      // no charsetWithDefault specified → fallback to default
    ));

    this.assertResult(instance, "mandatoryString", "some value");
    this.assertResult(instance, "optionalInt", OptionalInt.of(25));
    this.assertResult(instance, "charsetWithDefault", UTF_8);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of an existing mandatory string.
   *
   * @throws Exception
   */
  @Test
  public void testMandatoryString() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryString\")" +
            "\n" + "  public String mandatoryString();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass, "mandatoryString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "mandatoryString", "some value",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "mandatoryString", "some value");

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of a missing mandatory string.
   *
   * @throws Exception
   */
  @Test
  public void testMandatoryStringMissing() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryString\")" +
            "\n" + "  public String mandatoryString();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass, "mandatoryString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      // the mandatoryString is missing
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "mandatoryString", null);

    this.assertValidationErrors(instance,
                                ImmutableValidationFailure.builder()
                                  .failureType(MISSING_MANDATORY_VALUE)
                                  .key("mandatoryString")
                                  .build());
  }


  /**
   * Test the implementation of an existing optional int.
   *
   * @throws Exception
   */
  @Test
  public void testOptionalInt() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalInt\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass, "optionalInt");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "optionalInt", "15",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalInt", OptionalInt.of(15));

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of a missing optional int.
   *
   * @throws Exception
   */
  @Test
  public void testOptionalIntMissing() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalInt\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass, "optionalInt");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      // the optionalInt is missing
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalInt", OptionalInt.empty());

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of an existing optional Charset.
   *
   * @throws Exception
   */
  @Test
  public void testOptionalCharset() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.Optional;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalCharset\")" +
            "\n" + "  public Optional<Charset> optionalCharset();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass, "optionalCharset");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "optionalCharset", "UTF-8",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalCharset", Optional.of(UTF_8));

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of a specified charset with default value.
   *
   * @throws Exception
   */
  @Test
  public void testDefaultCharset() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"charsetWithDefault\", defaultValue = \"UTF-8\")" +
            "\n" + "  public Charset charsetWithDefault();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass, "charsetWithDefault");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "charsetWithDefault", "ISO-8859-1",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "charsetWithDefault", ISO_8859_1);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of a missing charset with default value.
   *
   * @throws Exception
   */
  @Test
  public void testDefaultCharsetMissing() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"charsetWithDefault\", defaultValue = \"UTF-8\")" +
            "\n" + "  public Charset charsetWithDefault();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass, "charsetWithDefault");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      // no charsetWithDefault is set
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "charsetWithDefault", UTF_8);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of an optional value with default value.
   *
   * @throws Exception
   */
  @Test
  public void testWarningOnOptionalWithDefault() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.util.Optional;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalWithDefault\", defaultValue = \"Hurz!\")" +
            "\n" + "  public Optional<String> optionalWithDefault();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();
    CompilationSubject.assertThat(compilation).hadWarningContaining("Optional and default value don't make much sense together. The Optional will never be empty.");

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigParam",
                                "com.example.ImmutableTestConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableTestConfig", compilation);

    this.assertMethods(generatedConfigClass, "optionalWithDefault");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      // no optionalWithDefault given
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalWithDefault", Optional.of("Hurz!"));

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of a Coat config interface that inherits from another Coat config interface.
   */
  @Test
  public void testInheritedConfig() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.BaseConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface BaseConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"inheritedParam\", defaultValue = \"inherited default\")" +
            "\n" + "  public String inheritedParam();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.SubConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface SubConfig extends BaseConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"additionalParam\", defaultValue = \"additional default\")" +
            "\n" + "  public String additionalParam();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.BaseConfig",
                                "com.example.BaseConfigParam",
                                "com.example.ImmutableBaseConfig",
                                "com.example.SubConfig",
                                "com.example.SubConfigParam",
                                "com.example.ImmutableSubConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableSubConfig", compilation);

    this.assertMethods(generatedConfigClass, "inheritedParam", "additionalParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      // no values are explicitly set
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "inheritedParam", "inherited default");
    this.assertResult(instance, "additionalParam", "additional default");

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the generation of a config with a custom name
   */
  @Test
  public void testCustomGeneratedClassName() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config(className = \"BlaBla\")" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryString\")" +
            "\n" + "  public String mandatoryString();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.BlaBlaParam",
                                "com.example.BlaBla");

    final Class<?> generatedConfigClass= this.loadClass("com.example.BlaBla", compilation);

    this.assertMethods(generatedConfigClass, "mandatoryString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "mandatoryString", "some value",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "mandatoryString", "some value");

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of a Coat config interface that embeds another Coat config interface.
   */
  @Test
  public void testEmbeddedConfig() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.EmbeddedConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface EmbeddedConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"embeddedParam\", defaultValue = \"embedded default\")" +
            "\n" + "  public String embeddedParam();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.MainConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface MainConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"someParam\", defaultValue = \"some default\")" +
            "\n" + "  public String someParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Embedded(key = \"embedded\", keySeparator= \".\")" +
            "\n" + "  public EmbeddedConfig embedded();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.MainConfig",
                                "com.example.MainConfigParam",
                                "com.example.ImmutableMainConfig",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigParam",
                                "com.example.ImmutableEmbeddedConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableMainConfig", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.ImmutableEmbeddedConfig", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      // no values are explicitly set
      "someParam",              "some value",
      "embedded.embeddedParam", "embedded value",
      "irrelevant key",         "irrelevant value"
    ));

    this.assertResult(instance, "someParam", "some value");
    final Object expectedEmbedded= this.createInstance(generatedEmbeddedClass, Map.of("embeddedParam", "embedded value"));
    this.assertResult(instance, "embedded", expectedEmbedded);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the implementation of a Coat config interface that embeds another Coat config interface
   * which itself embeds yet another Coat config interface.
   */
  @Test
  public void testDeeplyEmbeddedConfig() throws Exception {

    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.DeeplyEmbeddedConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface DeeplyEmbeddedConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"deeplyEmbeddedParam\")" +
            "\n" + "  public String deeplyEmbeddedParam();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.EmbeddedConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface EmbeddedConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"embeddedParam\")" +
            "\n" + "  public String embeddedParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Embedded(key = \"deeplyEmbedded\", keySeparator= \".\")" +
            "\n" + "  public DeeplyEmbeddedConfig deeplyEmbedded();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.MainConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface MainConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"someParam\")" +
            "\n" + "  public String someParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Embedded(key = \"embedded\", keySeparator= \".\")" +
            "\n" + "  public EmbeddedConfig embedded();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.MainConfig",
                                "com.example.MainConfigParam",
                                "com.example.ImmutableMainConfig",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigParam",
                                "com.example.DeeplyEmbeddedConfig",
                                "com.example.DeeplyEmbeddedConfigParam",
                                "com.example.ImmutableEmbeddedConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableMainConfig", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.ImmutableEmbeddedConfig", compilation);
    final Class<?> generatedDeeplyEmbeddedClass= this.loadClass("com.example.ImmutableDeeplyEmbeddedConfig", compilation);

    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    this.assertMethods(generatedEmbeddedClass, "embeddedParam", "deeplyEmbedded");
    this.assertMethods(generatedDeeplyEmbeddedClass, "deeplyEmbeddedParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.


    // test good path
    {
      final Object instance = this.createInstance(generatedConfigClass, mapOf(
        // no values are explicitly set
        "someParam",                                   "some value",
        "embedded.embeddedParam",                      "embedded value",
        "embedded.deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value",
        "irrelevant key",                              "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      final Object expectedEmbedded= this.createInstance(generatedEmbeddedClass, Map.of(
        "embeddedParam", "embedded value",
        "deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value"
      ));

      this.assertResult(instance, "embedded", expectedEmbedded);

      this.assertNoValidationErrors(instance);
    }

    // test missing embedded values
    {
      final Object instance = this.createInstance(generatedConfigClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        // the embedded value is missing
        "irrelevant key",         "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      final Object expectedEmbedded= this.createInstance(generatedEmbeddedClass, Map.of());
      this.assertResult(instance, "embedded", expectedEmbedded);

      this.assertValidationErrors(instance,
                                  ImmutableValidationFailure.builder()
                                    .failureType(MISSING_MANDATORY_VALUE)
                                    .key("embedded.embeddedParam")
                                    .build(),
                                  ImmutableValidationFailure.builder()
                                    .failureType(MISSING_MANDATORY_VALUE)
                                    .key("embedded.deeplyEmbedded.deeplyEmbeddedParam")
                                    .build()
      );
    }
  }



  ////////////////////////////////////////////////////////////////////////////////
  // Helper classes and methods
  //

  /**
   * Classloader for reading a class from a byte array. Tightly coupled to the classes
   * that are created by google-testing-compile.
   */
  private class ByteClassLoader extends ClassLoader {
    private final HashMap<String, byte[]> byteDataMap = new HashMap<>();

    public ByteClassLoader(ClassLoader parent) {
      super(parent);
    }

    public void loadDataInBytes(byte[] byteData, String resourcesName) {
      byteDataMap.put(resourcesName, byteData);
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
      if (byteDataMap.isEmpty()) {
        throw new ClassNotFoundException("byte data is empty");
      }

      final String filePath = "/" + CLASS_OUTPUT + "/" + className.replaceAll("\\.", "/").concat(".class");
      final byte[] extractedBytes = byteDataMap.get(filePath);
      if (extractedBytes == null) {
        throw new ClassNotFoundException("Cannot find " + className + " in bytes (expected filePath: "+ filePath +")");
      }

      return defineClass(className, extractedBytes, 0, extractedBytes.length);
    }
  }


  /**
   * Load a class via ByteClassLoader.
   *
   * @param fqClassName the fully qualified name of the class to load
   * @param compilation the compiloation instance from which to take the class
   * @return the Class object for the class
   * @throws ClassNotFoundException if the class cannot be found
   * @throws IOException if reading the class failed
   */
  private Class<?> loadClass(final String fqClassName, final Compilation compilation) throws ClassNotFoundException, IOException {


    for (final JavaFileObject jfo : compilation.generatedFiles()) {
      try (final InputStream stream= jfo.openInputStream();) {
        final byte[] bytes= IOUtils.toByteArray(stream);
        byteClassLoader.loadDataInBytes(bytes, jfo.getName());
      }
    }

    return Class.forName(fqClassName, true, byteClassLoader);
  }


  /**
   * Create an instance of the given <code>clazz</code> with the given <code>props</code> as parameter
   * to the constructor.
   * <p>
   * Used for creating the generated concrete implementation of a Coat config object.
   *
   * @param clazz the class to load
   * @param props the properties to fill the (Coat config) object
   * @return the newly created instance
   * @throws NoSuchMethodException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws InvocationTargetException
   */
  private Object createInstance(final Class<?> clazz, final Map<String, String> props) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    return clazz.getDeclaredConstructor(Map.class).newInstance(props);
  }


  /**
   * Assert that the given <code>compilation</code> contains a generation of all of the classes
   * specified in <code>fqClassNames</code>
   *
   * @param compilation the compilation to operate on
   * @param fqClassNames the fully qualified names of the classes to assert
   */
  private void assertGeneratedClasses(final Compilation compilation, final String... fqClassNames) {
    final String[] classFiles = Stream.of(fqClassNames)
      .map(c ->  "/" + CLASS_OUTPUT + "/" + c.replace('.', '/') + ".class")
      .toArray(String[]::new);

    assertThat(
      compilation.generatedFiles().stream()
        .map(JavaFileObject::getName))
      .contains(classFiles);
  }


  /**
   * Assert that calling the method with the given <code>methodName</code> on the given
   * <code>instance</code> object returns the expected <code>result</code>.
   * <p>
   * Used for verifying the correct result on calling the generated methods on the Coat config object.
   *
   * @param instance the instance to call the method on
   * @param methodName the method to call
   * @param expectedResult the expected result
   * @throws NoSuchMethodException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws InvocationTargetException
   */
  private void assertResult(final Object instance, final String methodName, final Object expectedResult) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    final Object actualResult = instance.getClass().getMethod(methodName).invoke(instance);
    System.out.println("--------------- exp --------------");
    System.out.println(expectedResult);
    System.out.println("--------------- act --------------");
    System.out.println(actualResult);
    System.out.println("--------------- end --------------");
    if (expectedResult != null) {
      assertThat(actualResult).isEqualTo(expectedResult);
    } else {
      assertThat(actualResult).isNull();
    }
  }


  /**
   * Assert that the <code>generatedConfigClass</code> contains all methods of the given
   * <code>methodNames</code> plus the two delegate methods "toString()" and "validate()"
   * and the generated methods "equals()" and "hashCode()".
   * @param generatedConfigClass the class to check
   * @param methodNames the names of the methods to assert
   */
  private void assertMethods(final Class<?> generatedConfigClass, final String... methodNames) {
    assertThat(
      Stream.of(generatedConfigClass.getDeclaredMethods())
        .map(Method::getName))
      .filteredOn(n -> !n.equals("equals"))
      .filteredOn(n -> !n.equals("hashCode"))
      .containsExactlyInAnyOrder(
        methodNames
      );
  }


  /**
   * Assert that calling the {@link CoatConfig#validate()} methods returns the given <code>validationFailureMessages</code>.
   * @param instance the instance to call <code>validate()</code> on
   * @param validationFailureMessages the expected validation failure messages
   */
  private void assertValidationErrors(final Object instance, final ValidationFailure... validationFailureMessages) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException {
    final Throwable thrown = catchThrowable(() ->
      instance.getClass().getMethod("validate").invoke(instance)
    );

    assertThat(thrown)
      .as("assert ValidationErrors")
      .isInstanceOf(InvocationTargetException.class)
      .hasCauseExactlyInstanceOf(ConfigValidationException.class)
      ;

    final ConfigValidationException validationException= (ConfigValidationException) thrown.getCause();
    final ValidationResult result= validationException.getValidationResult();

    assertThat(result.hasFailures()).isTrue();
    assertThat(result.validationFailures())
      .containsExactlyInAnyOrder(validationFailureMessages);
  }


  /**
   * Assert that calling the {@link CoatConfig#validate()} succeeds without failure.
   * @param instance the instance to call <code>validate()</code> on
   */
  private void assertNoValidationErrors(final Object instance) throws NoSuchMethodException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    instance.getClass().getMethod("validate").invoke(instance);
  }


  private Map<String, String> mapOf(final String... keysAndValues) {
    if (keysAndValues.length % 2 != 0) {
      throw new IllegalArgumentException("keysAndValues must be pairs of keys and values");
    }

    final Map<String, String> map= new LinkedHashMap<>();
    for (int i=0; i < keysAndValues.length; i++) {
      map.put(keysAndValues[i], keysAndValues[++i]);
    }

    return map;
  }
}
