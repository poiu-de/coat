/*
 * Copyright (C) 2020 - 2023 The Coat Authors
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
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.testing.compile.Compiler.javac;
import static de.poiu.coat.validation.ValidationFailure.Type.MISSING_MANDATORY_VALUE;
import static de.poiu.coat.validation.ValidationFailure.Type.UNPARSABLE_VALUE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;


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
   * A full test of a simple Coat config object.
   *
   * @throws Exception
   */
  @Test
  public void testAllBasicTypes() throws Exception {
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
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "import java.util.OptionalLong;" +
            "\n" + "import java.util.OptionalDouble;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryInt\")" +
            "\n" + "  public int mandatoryInt();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalInt\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"defaultInt\", defaultValue = \"21\")" +
            "\n" + "  public int defaultInt();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryLong\")" +
            "\n" + "  public long mandatoryLong();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalLong\")" +
            "\n" + "  public OptionalLong optionalLong();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"defaultLong\", defaultValue = \"22\")" +
            "\n" + "  public long defaultLong();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryDouble\")" +
            "\n" + "  public double mandatoryDouble();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalDouble\")" +
            "\n" + "  public OptionalDouble optionalDouble();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"defaultDouble\", defaultValue = \"23\")" +
            "\n" + "  public double defaultDouble();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryBoolean\")" +
            "\n" + "  public boolean mandatoryBoolean();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalBoolean\")" +
            "\n" + "  public Optional<Boolean> optionalBoolean();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"defaultBoolean\", defaultValue = \"true\")" +
            "\n" + "  public boolean defaultBoolean();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"mandatoryString\")" +
            "\n" + "  public String mandatoryString();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalString\")" +
            "\n" + "  public Optional<String> optionalString();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"defaultString\", defaultValue = \"default\")" +
            "\n" + "  public String defaultString();" +
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
                       "mandatoryInt",
                       "optionalInt",
                       "defaultInt",
                       "mandatoryLong",
                       "optionalLong",
                       "defaultLong",
                       "mandatoryDouble",
                       "optionalDouble",
                       "defaultDouble",
                       "mandatoryBoolean",
                       "optionalBoolean",
                       "defaultBoolean",
                       "mandatoryString",
                       "optionalString",
                       "defaultString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "mandatoryInt",     "0x7fffffff",
      "optionalInt",      "12",
      "mandatoryLong",    "0x7fffffffffffffff",
      "optionalLong",     "13",
      "mandatoryDouble",  "0x1.fffffffffffffP+1023",
      "optionalDouble",   "14",
      "mandatoryBoolean", "true",
      "optionalBoolean",  "true",
      "mandatoryString",  "mandatory",
      "optionalString",   "optional"
      // parameters with default are not explicitly set
    ));

    this.assertResult(instance, "mandatoryInt",     Integer.MAX_VALUE);
    this.assertResult(instance, "optionalInt",      OptionalInt.of(12));
    this.assertResult(instance, "defaultInt",       21);
    this.assertResult(instance, "mandatoryLong",    Long.MAX_VALUE);
    this.assertResult(instance, "optionalLong",     OptionalLong.of(13));
    this.assertResult(instance, "defaultLong",      22l);
    this.assertResult(instance, "mandatoryDouble",  Double.MAX_VALUE);
    this.assertResult(instance, "optionalDouble",   OptionalDouble.of(14));
    this.assertResult(instance, "defaultDouble",    23d);
    this.assertResult(instance, "mandatoryBoolean", true);
    this.assertResult(instance, "optionalBoolean",  Optional.of(Boolean.TRUE));
    this.assertResult(instance, "defaultBoolean",   true);
    this.assertResult(instance, "mandatoryString",  "mandatory");
    this.assertResult(instance, "optionalString",   Optional.of("optional"));
    this.assertResult(instance, "defaultString",    "default");

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
   * Test that the same key for multiple accessors fails.
   */
  @Test
  public void testDuplicateKey() throws Exception {
    // - preparation && execution && verification

    assertThatThrownBy(() -> {
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
            "\n" + "  @Coat.Param(key = \"duplicateKey\")" +
            "\n" + "  public String mandatoryString();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalInt\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"duplicateKey\", defaultValue = \"UTF-8\")" +
            "\n" + "  public Charset charsetWithDefault();" +
            "\n" + "}" +
            ""));
      })
      .cause()
      .isInstanceOf(CoatProcessorException.class)
      .hasMessageStartingWith("Duplicate keys:\n")
      .hasMessageContaining("\n  duplicateKey:\n")
      ;
  }


  /**
   * Test that the key can be omitted now (in which case it will be assumed to be the same as the
   * accessor name.
   */
  @Test
  public void testOmittedOptionalKey() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param()" +
            "\n" + "  public String omittedKey();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"specifiedKey\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(defaultValue = \"UTF-8\")" +
            "\n" + "  public Charset omittedKeyButDefaultValue();" +
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
                       "omittedKey",
                       "optionalInt",
                       "omittedKeyButDefaultValue");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "omittedKey", "some value",
      "specifiedKey", "25"
      // no omittedKeyButDefaultValue specified → fallback to default
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "optionalInt", OptionalInt.of(25));
    this.assertResult(instance, "omittedKeyButDefaultValue", UTF_8);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test that the @Param annotation can be omitted now.
   */
  @Test
  public void testOmittedOptionalParamAnnotation() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param()" +
            "\n" + "  public String omittedKey();" +
            "\n" + "" +
            "\n" + "  public OptionalInt omittedAnnotation();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"specified_key\")" +
            "\n" + "  public Charset specifiedKey();" +
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
                       "omittedKey",
                       "omittedAnnotation",
                       "specifiedKey");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "omittedKey", "some value",
      "omittedAnnotation", "25",
      "specified_key", "UTF-8"
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "omittedAnnotation", OptionalInt.of(25));
    this.assertResult(instance, "specifiedKey", UTF_8);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test that the same key for multiple accessors fails.
   */
  @Test
  public void testAccessorWithoutReturnType() throws Exception {
    // - preparation && execution && verification

    assertThatThrownBy(() -> {
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
            "\n" + "  @Coat.Param(key = \"missingReturnType\")" +
            "\n" + "  public void missingReturnType();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalInt\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "}" +
            ""));
      })
      .cause()
      .isInstanceOf(CoatProcessorException.class)
      .hasMessageStartingWith("Accessors without return type:\n")
      .hasMessageContaining("\n  missingReturnType():\n")
      ;
  }


  /**
   * Test that the same key for multiple accessors fails.
   */
  @Test
  public void testAccessorWithParameter() throws Exception {
    // - preparation && execution && verification

    assertThatThrownBy(() -> {
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
            "\n" + "  @Coat.Param(key = \"unexpectedParameter\")" +
            "\n" + "  public String unexpectedParameter(int i);" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"optionalInt\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "}" +
            ""));
      })
      .cause()
      .isInstanceOf(CoatProcessorException.class)
      .hasMessageStartingWith("Accessors with parameters:\n")
      .hasMessageContaining("\n  unexpectedParameter(int):\n")
      ;
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
   * Test the implementation of a Coat config interface that inherits from another Coat config interface.
   */
  @Test
  public void testInheritedConfig_ValidationFailures() throws Exception {

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
            "\n" + "  @Coat.Param(key = \"inheritedParam\")" +
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
            "\n" + "  @Coat.Param(key = \"additionalParam\")" +
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

    this.assertResult(instance, "inheritedParam", null);
    this.assertResult(instance, "additionalParam", null);

    this.assertValidationErrors(instance,
                                ImmutableValidationFailure.builder()
                                  .failureType(MISSING_MANDATORY_VALUE)
                                  .key("inheritedParam")
                                  .build(),
                                ImmutableValidationFailure.builder()
                                  .failureType(MISSING_MANDATORY_VALUE)
                                  .key("additionalParam")
                                  .build()
    );
  }




  /**
   * Test that the same key for multiple accessors fails.
   */
  @Test
  public void testInheritedConfig_DuplicateKey() throws Exception {
    // - preparation && execution && verification

    assertThatThrownBy(() -> {
        javac()
          .withProcessors(new CoatProcessor())
          .compile(JavaFileObjects.forSourceString("com.example.BaseConfig1",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface BaseConfig1 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"duplicateKey\", defaultValue = \"inherited default\")" +
            "\n" + "  public String inheritedParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"someAccessor\", defaultValue = \"some accessor\")" +
            "\n" + "  public String someAccessor();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.BaseConfig2",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface BaseConfig2 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"duplicateKey\", defaultValue = \"other default\")" +
            "\n" + "  public int otherParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"otherAccessor\", defaultValue = \"other accessor\")" +
            "\n" + "  public int otherAccessor();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.SubConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface SubConfig extends BaseConfig1, BaseConfig2 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"additionalParam\", defaultValue = \"additional default\")" +
            "\n" + "  public String additionalParam();" +
            "\n" + "}" +
            ""));
      })
      .cause()
      .isInstanceOf(CoatProcessorException.class)
      .hasMessageStartingWith("Duplicate keys:\n")
      .hasMessageContaining("\n  duplicateKey:\n")
      ;
  }


  /**
   * Test the implementation of a Coat config interface that inherits from two other Coat config interfaces
   * that share the same accessor method.
   */
  @Test
  public void testInheritedConfig_DuplicateAccessorMethod() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.BaseConfig1",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface BaseConfig1 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"inheritedParam\", defaultValue = \"inherited default\")" +
            "\n" + "  public String inheritedParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"sharedAccessor\", defaultValue = \"shared accessor\")" +
            "\n" + "  public String sharedAccessor();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.BaseConfig2",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface BaseConfig2 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"otherParam\", defaultValue = \"other default\")" +
            "\n" + "  public int otherParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"sharedAccessor\", defaultValue = \"shared accessor\")" +
            "\n" + "  public String sharedAccessor();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.SubConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface SubConfig extends BaseConfig1, BaseConfig2 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"additionalParam\", defaultValue = \"additional default\")" +
            "\n" + "  public String additionalParam();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.BaseConfig1",
                                "com.example.BaseConfig2",
                                "com.example.BaseConfig1Param",
                                "com.example.BaseConfig2Param",
                                "com.example.ImmutableBaseConfig1",
                                "com.example.ImmutableBaseConfig2",
                                "com.example.SubConfig",
                                "com.example.SubConfigParam",
                                "com.example.ImmutableSubConfig");

    final Class<?> generatedConfigClass= this.loadClass("com.example.ImmutableSubConfig", compilation);

    this.assertMethods(generatedConfigClass, "inheritedParam", "additionalParam", "sharedAccessor", "otherParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      // no values are explicitly set
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "inheritedParam", "inherited default");
    this.assertResult(instance, "additionalParam", "additional default");
    this.assertResult(instance, "sharedAccessor", "shared accessor");

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test the failure of the processing of a Coat config interface that inherits from two other Coat config interfaces
   * that have a conflicting accessor method (same name, but different signature).
   */
  @ParameterizedTest
  @ValueSource(strings = {
    // other return type
    "\n" + "  @Coat.Param(key = \"sharedAccessor\", defaultValue = \"shared accessor\")" +
    "\n" + "  public int sharedAccessor();",
    // different default value
    "\n" + "  @Coat.Param(key = \"sharedAccessor\", defaultValue = \"different default\")" +
    "\n" + "  public String sharedAccessor();",
    // different key
    "\n" + "  @Coat.Param(key = \"differentKey\", defaultValue = \"shared accessor\")" +
    "\n" + "  public String sharedAccessor();",
    // different mandatority
    "\n" + "  @Coat.Param(key = \"sharedAccessor\", defaultValue = \"shared accessor\")" +
    "\n" + "  public Optional<String> sharedAccessor();",
  })
 public void testInheritedConfig_ConflictingAccessorMethod(final String conflictingAccessor) throws Exception {
    // - preparation && execution && verification

    assertThatThrownBy(() -> {
        javac()
          .withProcessors(new CoatProcessor())
          .compile(JavaFileObjects.forSourceString("com.example.BaseConfig1",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface BaseConfig1 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"inheritedParam\", defaultValue = \"inherited default\")" +
            "\n" + "  public String inheritedParam();" +
            "\n" + "" +
            conflictingAccessor +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.BaseConfig2",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface BaseConfig2 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"otherParam\", defaultValue = \"other default\")" +
            "\n" + "  public int otherParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"sharedAccessor\", defaultValue = \"shared accessor\")" +
            "\n" + "  public String sharedAccessor();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.SubConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface SubConfig extends BaseConfig1, BaseConfig2 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"additionalParam\", defaultValue = \"additional default\")" +
            "\n" + "  public String additionalParam();" +
            "\n" + "}" +
            ""));
      })
      .cause()
      .isInstanceOf(CoatProcessorException.class)
      .hasMessageStartingWith("Conflicting accessor methods:\n")
      ;
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


  /**
   * Test the implementation of a Coat config interface that embeds another Coat config interface.
   */
  @Test
  public void testOptionalEmbeddedConfig() throws Exception {

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
            "\n" + "  @Coat.Param(key = \"embeddedParam\")" +
            "\n" + "  public int embeddedParam();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.MainConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.util.Optional;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface MainConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"someParam\", defaultValue = \"some default\")" +
            "\n" + "  public String someParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Embedded(key = \"embedded\")" +
            "\n" + "  public Optional<EmbeddedConfig> embedded();" +
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

    // test existing optional
    {
      final Object instance = this.createInstance(generatedConfigClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        "embedded.embeddedParam", "1",
        "irrelevant key",         "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      final Object expectedEmbedded= this.createInstance(generatedEmbeddedClass, Map.of("embeddedParam", "1"));
      this.assertResult(instance, "embedded", Optional.of(expectedEmbedded));

      this.assertNoValidationErrors(instance);
    }

    // test missing optional
    {
      final Object instance = this.createInstance(generatedConfigClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        "irrelevant key",         "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      this.assertResult(instance, "embedded", Optional.empty());

      this.assertNoValidationErrors(instance);
    }


    // test invalid optional
    {
      final Object instance = this.createInstance(generatedConfigClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        "embedded.embeddedParam", "invalid value",
        "irrelevant key",         "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      // the following is not testable as it would call the converter which would then fail
      //final Object expectedEmbedded= this.createInstance(generatedEmbeddedClass, Map.of("embeddedParam", "invalid value"));
      //this.assertResult(instance, "embedded", Optional.of(expectedEmbedded));

      this.assertValidationErrors(instance, ImmutableValidationFailure.builder()
        .failureType(UNPARSABLE_VALUE)
        .key("embedded.embeddedParam")
        .type("int")
        .value("invalid value")
        .build());
    }
  }



  /**
   * Test that a missing optional embedded config is considered missing even if it contains default values
   */
  @Test
  public void testMissingOptionalEmbeddedConfigWithDefaultValues() throws Exception {

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
            "\n" + "  @Coat.Param(key = \"embeddedParam\", defaultValue = \"42\")" +
            "\n" + "  public int embeddedParam();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.MainConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.util.Optional;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface MainConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"someParam\", defaultValue = \"some default\")" +
            "\n" + "  public String someParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Embedded(key = \"embedded\")" +
            "\n" + "  public Optional<EmbeddedConfig> embedded();" +
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

    // test missing optional
    {
      final Object instance = this.createInstance(generatedConfigClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        "irrelevant key",         "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      this.assertResult(instance, "embedded", Optional.empty());

      this.assertNoValidationErrors(instance);
    }
  }


  /**
   * Test that the @Coat.Embedded can only be used on Types annotated with @Coat.Config
   */
  @Test
  public void testEmbeddedConfigOnWrongType() throws Exception {

    // - preparation && execution

    assertThatThrownBy(() -> {
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.MainConfig",
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
            "\n" + "  public String embedded();" +
            "\n" + "}" +
            ""));
        })
      .cause()
      .isInstanceOf(CoatProcessorException.class)
      .hasMessageStartingWith("@Coat.Embedded annotation can only be applied to types that are annotated with @Coat.Config.")
      .hasMessageContaining("embedded()")
      ;
  }


  /**
   * Test that the @Coat.Embedded can only be used on Types annotated with @Coat.Config
   */
  @Test
  public void testEmbeddedConfigOnNonAnnotatedType() throws Exception {

    // - preparation && execution

    assertThatThrownBy(() -> {
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.EmbeddedConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
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
        })
      .cause()
      .isInstanceOf(CoatProcessorException.class)
      .hasMessageStartingWith("@Coat.Embedded annotation can only be applied to types that are annotated with @Coat.Config.")
      .hasMessageContaining("embedded()")
      ;
  }


  /**
   * Test the generated equals() and hashCode() methods with embedded configs.
   */
  @Test
  public void testEqualsAndHashCode() throws Exception {

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


    // test equal objects
    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      // no values are explicitly set
      "someParam",                                   "some value",
      "embedded.embeddedParam",                      "embedded value",
      "embedded.deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value",
      "irrelevant key",                              "irrelevant value"
    ));

    this.assertResult(instance, "someParam", "some value");

    final Object equalObject = this.createInstance(generatedConfigClass, mapOf(
      // no values are explicitly set
      "someParam",                                   "some value",
      "embedded.embeddedParam",                      "embedded value",
      "embedded.deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value",
      "irrelevant key",                              "IRRELEVANT VALUE TO BE IGNORED"
    ));

    assertThat(instance).isEqualTo(equalObject);
    assertThat(instance.hashCode()).isEqualTo(equalObject.hashCode());

    final Object unequalObject = this.createInstance(generatedConfigClass, mapOf(
      // no values are explicitly set
      "someParam",                                   "some value",
      "embedded.embeddedParam",                      "embedded value",
      "embedded.deeplyEmbedded.deeplyEmbeddedParam", "DIFFERING deeply embedded value",
      "irrelevant key",                              "irrelevant value"
    ));

    assertThat(instance).isNotEqualTo(unequalObject);
    assertThat(instance.hashCode()).isNotEqualTo(unequalObject.hashCode());
  }


  @Test
  public void testStripBlockTagsFromJavadoc() {
    final String javadoc = ""
      + " bla bla dumdidum\n"
      + " lalala oo jaja {@code somecode}\n"
      + " {@link Class#method()} blubb\n"
      + "\n"
      + " sometext\n"
      + "<p>\n"
      + " moretext\n"
      + "\n"
      + " @see OtherClass\n"
      + " @param p1\n"
      + " @param p2 with some description\n"
      + " @return an intersting value\n"
      + " @throws RuntimeException if something goes wrong";

    assertThat(CoatProcessor.stripBlockTagsFromJavadoc(javadoc))
      .isEqualTo(""
      + " bla bla dumdidum\n"
      + " lalala oo jaja {@code somecode}\n"
      + " {@link Class#method()} blubb\n"
      + "\n"
      + " sometext\n"
      + "<p>\n"
      + " moretext\n"
      + "\n"
      + "");
  }


  /**
   * Test that the casing of inferred keys can be specified and defaults to AS_IS.
   */
  @Test
  public void testCasingStrategyDefault() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param()" +
            "\n" + "  public String omittedKey();" +
            "\n" + "" +
            "\n" + "  public OptionalInt omittedAnnotation();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"Specified_KEY\")" +
            "\n" + "  public Charset specifiedKey();" +
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
                       "omittedKey",
                       "omittedAnnotation",
                       "specifiedKey");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "omittedKey", "some value",
      "omittedAnnotation", "25",
      "Specified_KEY", "UTF-8"
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "omittedAnnotation", OptionalInt.of(25));
    this.assertResult(instance, "specifiedKey", UTF_8);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test that the casing of inferred keys can be specified and test AS_IS (which is also the default).
   */
  @Test
  public void testCasingStrategyAsIs() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "import static de.poiu.coat.processor.casing.CasingStrategy.AS_IS;" +
            "\n" + "" +
            "\n" + "@Coat.Config(casing = AS_IS)" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param()" +
            "\n" + "  public String omittedKey();" +
            "\n" + "" +
            "\n" + "  public OptionalInt omittedAnnotation();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"Specified_KEY\")" +
            "\n" + "  public Charset specifiedKey();" +
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
                       "omittedKey",
                       "omittedAnnotation",
                       "specifiedKey");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "omittedKey", "some value",
      "omittedAnnotation", "25",
      "Specified_KEY", "UTF-8"
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "omittedAnnotation", OptionalInt.of(25));
    this.assertResult(instance, "specifiedKey", UTF_8);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test that the casing of inferred keys can be specified and test SNAKE_CASE.
   */
  @Test
  public void testCasingStrategySnakeCase() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "import static de.poiu.coat.processor.casing.CasingStrategy.SNAKE_CASE;" +
            "\n" + "" +
            "\n" + "@Coat.Config(casing = SNAKE_CASE)" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param()" +
            "\n" + "  public String omittedKey();" +
            "\n" + "" +
            "\n" + "  public OptionalInt omittedAnnotation();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"Specified_KEY\")" +
            "\n" + "  public Charset specifiedKey();" +
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
                       "omittedKey",
                       "omittedAnnotation",
                       "specifiedKey");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedConfigClass, mapOf(
      "omitted_key", "some value",
      "omitted_annotation", "25",
      "Specified_KEY", "UTF-8"
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "omittedAnnotation", OptionalInt.of(25));
    this.assertResult(instance, "specifiedKey", UTF_8);

    this.assertNoValidationErrors(instance);
  }


  /**
   * Test that an exception is thrown on annotated types other than interfaces.
   */
  @Test
  public void testFailOnAnnotatedAbstractClass() throws Exception {
    // - preparation && execution && verification

    assertThatThrownBy(() -> {
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public abstract class TestConfig {" +
            "\n" + "" +
            "\n" + "  public abstract String mandatoryString();" +
            "\n" + "}" +
            ""));
      })
      .cause()
      .isInstanceOf(CoatProcessorException.class)
      .hasMessageStartingWith("@Coat.Config is only supported on interfaced at the moment:\n")
      .hasMessageContaining("  Non-interface type: com.example.TestConfig")
      ;
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
      .filteredOn(n -> !n.equals("writeExampleConfig"))
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
