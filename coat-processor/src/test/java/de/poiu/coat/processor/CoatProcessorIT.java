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
package de.poiu.coat.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.JavaFileObjects;
import de.poiu.coat.validation.ConfigValidationException;
import de.poiu.coat.validation.ImmutableValidationFailure;
import de.poiu.coat.validation.ValidationFailure;
import de.poiu.coat.validation.ValidationResult;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Stream;
import javax.tools.JavaFileObject;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static com.google.testing.compile.Compiler.javac;
import static de.poiu.coat.validation.ValidationFailure.Type.MISSING_MANDATORY_VALUE;
import static de.poiu.coat.validation.ValidationFailure.Type.UNPARSABLE_VALUE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.assertj.core.api.InstanceOfAssertFactories.type;


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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "mandatoryString",
                       "optionalInt",
                       "charsetWithDefault");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "mandatoryString", "some value",
      "optionalInt", "25"
      // no charsetWithDefault specified → fallback to default
    ));

    this.assertResult(instance, "mandatoryString", "some value");
    this.assertResult(instance, "optionalInt", OptionalInt.of(25));
    this.assertResult(instance, "charsetWithDefault", UTF_8);
  }


  /**
   * A full test of a Coat config object with all supported basic types.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

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

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
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
  }


  /**
   * Test that the generation for a totally empty config interface is possible.
   */
  @Test
  public void testEmptyConfig() throws Exception {
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
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    final Object instance = this.createInstance(generatedBuilderClass, mapOf());
  }


  /**
   * Test the failure of the processing of a Coat config interface that has accessors with primitive arrays.
   */
  @ParameterizedTest
  @ValueSource(strings = {
    "boolean",
    "byte",
    "short",
    "int",
    "long",
    "float",
    "double",
    "char",
  })
 public void testNoPrimitiveArrays(final String primitiveType) throws Exception {
    // - preparation && execution && verification

    final Compilation compilation=
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
            "\n" + "  public " + primitiveType + "[] primitiveArray();" +
            "\n" + "}" +
            ""));

    CompilationSubject.assertThat(compilation).hadErrorContaining("Arrays of primitives are not supported. Use Lists instead.");
  }


  /**
   * Test the failure of the processing of a Coat config interface with unsupported primitives.
   */
  @ParameterizedTest
  @ValueSource(strings = {
    "byte",
    "short",
    "float",
    "char",
  })
 public void testUnsupportedPrimites(final String primitiveType) throws Exception {
    // - preparation && execution && verification

    final Compilation compilation=
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
            "\n" + "  public " + primitiveType + " primitiveType();" +
            "\n" + "}" +
            ""));

    CompilationSubject.assertThat(compilation).hadErrorContaining("Only the primitive types boolean, int, long and double are supported. Please use one of those or the corresponding object types.");
  }


  /**
   * Test the implementation of an existing mandatory string.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "mandatoryString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "mandatoryString", "some value",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "mandatoryString", "some value");
  }


  /**
   * Test the implementation of a missing mandatory string.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "mandatoryString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final InvocationTargetException itex = catchThrowableOfType(() ->
      this.createInstance(generatedBuilderClass, mapOf(
        // the mandatoryString is missing
        "irrelevant key", "irrelevant value"
      )), InvocationTargetException.class);

    assertValidationErrors(itex,
     ImmutableValidationFailure.builder()
       .failureType(MISSING_MANDATORY_VALUE)
       .key("mandatoryString")
       .build()
    );
  }


  /**
   * Test the implementation of an existing optional int.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "optionalInt");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "optionalInt", "15",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalInt", OptionalInt.of(15));
  }


  /**
   * Test the implementation of a missing optional int.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "optionalInt");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // the optionalInt is missing
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalInt", OptionalInt.empty());
  }


  /**
   * Test the implementation of an existing optional Charset.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "optionalCharset");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "optionalCharset", "UTF-8",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalCharset", Optional.of(UTF_8));
  }


  /**
   * Test the implementation of a specified charset with default value.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "charsetWithDefault");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "charsetWithDefault", "ISO-8859-1",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "charsetWithDefault", ISO_8859_1);
  }


  /**
   * Test the implementation of a missing charset with default value.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "charsetWithDefault");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no charsetWithDefault is set
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "charsetWithDefault", UTF_8);
  }


  /**
   * Test the implementation of an optional value with default value.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "optionalWithDefault");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no optionalWithDefault given
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalWithDefault", Optional.of("Hurz!"));
  }


  /**
   * Test the implementation of an existing optional float.
   */
  @Test
  public void testOptionalFloat() throws Exception {

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
            "\n" + "  public Optional<Float> optionalFloat();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "optionalFloat");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "optionalFloat", "1.5",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "optionalFloat", Optional.of(Float.valueOf(1.5f)));
  }


  /**
   * Test that the same key for multiple accessors fails.
   */
  @Test
  public void testDuplicateKey() throws Exception {
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

    CompilationSubject.assertThat(compilation).hadErrorContaining("Duplicate key");
  }


  /**
   * Test that the key can be omitted (in which case it will be assumed to be the same as the
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "omittedKey",
                       "optionalInt",
                       "omittedKeyButDefaultValue");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "omittedKey", "some value",
      "specifiedKey", "25"
      // no omittedKeyButDefaultValue specified → fallback to default
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "optionalInt", OptionalInt.of(25));
    this.assertResult(instance, "omittedKeyButDefaultValue", UTF_8);
  }


  /**
   * Test that the key on an EmbeddedConfig can be omitted.
   */
  @Test
  public void testOmittedOptionalKeyOnEmbeddedConfig() throws Exception {

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
            "\n" + "  @Coat.Param" +
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
            "\n" + "  public String someParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Embedded" +
            "\n" + "  public EmbeddedConfig embedded();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.MainConfig",
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "someParam",              "some value",
      "embedded.embeddedParam", "embedded value",
      "irrelevant key",         "irrelevant value"
    ));

    this.assertResult(instance, "someParam", "some value");
    final Object expectedEmbedded= this.createInstance(generatedEmbeddedBuilderClass, Map.of("embeddedParam", "embedded value"));
    this.assertResult(instance, "embedded", expectedEmbedded);
  }


  /**
   * Test that the @Param annotation can be omitted.
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "omittedKey",
                       "omittedAnnotation",
                       "specifiedKey");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "omittedKey", "some value",
      "omittedAnnotation", "25",
      "specified_key", "UTF-8"
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "omittedAnnotation", OptionalInt.of(25));
    this.assertResult(instance, "specifiedKey", UTF_8);
  }


  /**
   * Test that conversion failures result in a helpful validation failure.
   */
  @Test
  public void testConversionError() throws Exception {

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
            "\n" + "  public int mandatoryInt();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "mandatoryInt");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final InvocationTargetException itex = catchThrowableOfType(() ->
      this.createInstance(generatedBuilderClass, mapOf(
        "mandatoryInt", "some value",
        "irrelevant key", "irrelevant value"
      )), InvocationTargetException.class);

    assertValidationErrors(itex,
     ImmutableValidationFailure.builder()
       .failureType(UNPARSABLE_VALUE)
       .key("mandatoryInt")
       .type("int")
       .value("some value")
       .errorMsg("Error converting value to int")
       .build()
    );
  }


  /**
   * Test that accessors without return types fail.
   */
  @Test
  public void testAccessorWithoutReturnType() throws Exception {
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

    CompilationSubject.assertThat(compilation).hadErrorContaining("Accessors must have a return type");
  }


  /**
   * Test that accessors with parameters fail.
   */
  @Test
  public void testAccessorWithParameter() throws Exception {
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

    CompilationSubject.assertThat(compilation).hadErrorContaining("Accessors must not have parameters");
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
                                "com.example.BaseConfigBuilder",
                                "com.example.SubConfig",
                                "com.example.SubConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.SubConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.SubConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "inheritedParam", "additionalParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "inheritedParam", "inherited default");
    this.assertResult(instance, "additionalParam", "additional default");
  }


  /**
   * Test that inherited accessors are correctly validated.
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
                                "com.example.BaseConfigBuilder",
                                "com.example.SubConfig",
                                "com.example.SubConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.SubConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.SubConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "inheritedParam", "additionalParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final InvocationTargetException itex = catchThrowableOfType(() ->
      this.createInstance(generatedBuilderClass, mapOf(
        // no values are explicitly set
        "irrelevant key", "irrelevant value"
    )), InvocationTargetException.class);

    assertValidationErrors(itex,
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
   * Test that the same key for multiple accessors (by inheritance) fails.
   */
  @Test
  public void testInheritedConfig_DuplicateKey() throws Exception {
    // - preparation && execution && verification

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

    CompilationSubject.assertThat(compilation).hadErrorContaining("Duplicate key");
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
            "\n" + "  @Coat.Param(key = \"otherParam\", defaultValue = \"25\")" +
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
                                "com.example.BaseConfig1Builder",
                                "com.example.BaseConfig2Builder",
                                "com.example.SubConfig",
                                "com.example.SubConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.SubConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.SubConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "inheritedParam", "additionalParam", "sharedAccessor", "otherParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "inheritedParam", "inherited default");
    this.assertResult(instance, "additionalParam", "additional default");
    this.assertResult(instance, "sharedAccessor", "shared accessor");
    this.assertResult(instance, "otherParam", 25);
  }


  /**
   * Test the implementation of a Coat config interface that inherits from anotherr Coat config interfaces
   * that itself inherits from another one. The lowest one shares an accessor method.
   */
  @Test
  public void testDeeplyInheritedConfig_DuplicateAccessorMethod() throws Exception {
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
            "\n" + "public interface BaseConfig2 extends BaseConfig1 {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"otherParam\", defaultValue = \"25\")" +
            "\n" + "  public int otherParam();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.SubConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface SubConfig extends BaseConfig2 {" +
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
                                "com.example.BaseConfig1Builder",
                                "com.example.BaseConfig2Builder",
                                "com.example.SubConfig",
                                "com.example.SubConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.SubConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.SubConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "inheritedParam", "additionalParam", "sharedAccessor", "otherParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "inheritedParam", "inherited default");
    this.assertResult(instance, "additionalParam", "additional default");
    this.assertResult(instance, "sharedAccessor", "shared accessor");
    this.assertResult(instance, "otherParam", 25);
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
    // different collectivity
    "\n" + "  @Coat.Param(key = \"sharedAccessor\", defaultValue = \"shared accessor\")" +
    "\n" + "  public List<String> sharedAccessor();",
  })
 public void testInheritedConfig_ConflictingAccessorMethod(final String conflictingAccessor) throws Exception {
    // - preparation && execution && verification

    final Compilation compilation=
        javac()
          .withProcessors(new CoatProcessor())
          .compile(JavaFileObjects.forSourceString("com.example.BaseConfig1",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.util.Optional;" +
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

    CompilationSubject.assertThat(compilation).hadErrorContaining("Conflicting accessor methods");
  }


  /**
   * Test the generation of a config builder with a custom name
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
                                "com.example.BlaBla");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.BlaBla", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.BlaBla$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "mandatoryString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "mandatoryString", "some value",
      "irrelevant key", "irrelevant value"
    ));

    this.assertResult(instance, "mandatoryString", "some value");
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
                                "com.example.MainConfigBuilder",
                                "com.example.MainConfigBuilder$ParamImpl",
                                "com.example.MainConfigBuilder$ConfigImpl",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder",
                                "com.example.EmbeddedConfigBuilder$ParamImpl",
                                "com.example.EmbeddedConfigBuilder$ConfigImpl");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "someParam",              "some value",
      "embedded.embeddedParam", "embedded value",
      "irrelevant key",         "irrelevant value"
    ));

    this.assertResult(instance, "someParam", "some value");
    final Object expectedEmbedded= this.createInstance(generatedEmbeddedBuilderClass, Map.of("embeddedParam", "embedded value"));
    this.assertResult(instance, "embedded", expectedEmbedded);
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
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.DeeplyEmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedDeeplyEmbeddedBuilderClass= this.loadClass("com.example.DeeplyEmbeddedConfigBuilder", compilation);
    final Class<?> generatedDeeplyEmbeddedClass= this.loadClass("com.example.DeeplyEmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    this.assertMethods(generatedEmbeddedClass, "embeddedParam", "deeplyEmbedded");
    this.assertMethods(generatedDeeplyEmbeddedClass, "deeplyEmbeddedParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.


    // test good path
    {
      final Object instance = this.createInstance(generatedBuilderClass, mapOf(
        // no values are explicitly set
        "someParam",                                   "some value",
        "embedded.embeddedParam",                      "embedded value",
        "embedded.deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value",
        "irrelevant key",                              "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      final Object expectedEmbedded= this.createInstance(generatedEmbeddedBuilderClass, Map.of(
        "embeddedParam", "embedded value",
        "deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value"
      ));

      this.assertResult(instance, "embedded", expectedEmbedded);
    }

    // test missing embedded values
    {
      final InvocationTargetException itex = catchThrowableOfType(() ->
        this.createInstance(generatedBuilderClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        // the embedded value is missing
        "irrelevant key",         "irrelevant value"
        )), InvocationTargetException.class);

      assertValidationErrors(itex,
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
   * Test the implementation of a Coat config interface that embeds another Coat config interface
   * as an optional value.
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
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    // test existing optional
    {
      final Object instance = this.createInstance(generatedBuilderClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        "embedded.embeddedParam", "1",
        "irrelevant key",         "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      final Object expectedEmbedded= this.createInstance(generatedEmbeddedBuilderClass, Map.of("embeddedParam", "1"));
      this.assertResult(instance, "embedded", Optional.of(expectedEmbedded));
    }

    // test missing optional
    {
      final Object instance = this.createInstance(generatedBuilderClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        "irrelevant key",         "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      this.assertResult(instance, "embedded", Optional.empty());
    }


    // test invalid optional
    {
      final InvocationTargetException itex = catchThrowableOfType(() ->
        this.createInstance(generatedBuilderClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        "embedded.embeddedParam", "invalid value",
        "irrelevant key",         "irrelevant value"
      )), InvocationTargetException.class);

      assertValidationErrors(itex, ImmutableValidationFailure.builder()
        .failureType(UNPARSABLE_VALUE)
        .key("embedded.embeddedParam")
        .type("int")
        .value("invalid value")
        .errorMsg("Error converting value to int")
        .build());
    }
  }



  /**
   * Test that a missing optional embedded config is considered missing even if it contains default values.
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
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    // test missing optional
    {
      final Object instance = this.createInstance(generatedBuilderClass, mapOf(
        // no values are explicitly set
        "someParam",              "some value",
        "irrelevant key",         "irrelevant value"
      ));

      this.assertResult(instance, "someParam", "some value");
      this.assertResult(instance, "embedded", Optional.empty());
    }
  }


  /**
   * Test that the @Coat.Embedded can only be used on Types annotated with @Coat.Config
   */
  @Test
  public void testEmbeddedConfigOnWrongType() throws Exception {

    // - preparation && execution

    final Compilation compilation=
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
    CompilationSubject.assertThat(compilation).hadErrorContaining("@Coat.Embedded annotation can only be applied to types that are annotated with @Coat.Config.");
  }


  /**
   * Test that the @Coat.Embedded can only be used on Types annotated with @Coat.Config
   */
  @Test
  public void testEmbeddedConfigOnNonAnnotatedType() throws Exception {

    // - preparation && execution

    final Compilation compilation=
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
    CompilationSubject.assertThat(compilation).hadErrorContaining("@Coat.Embedded annotation can only be applied to types that are annotated with @Coat.Config.");
  }


  /**
   * Test that an interface can embed other configs without specifying any additional properties.
   */
  @Test
  public void testEmbeddedConfigWithoutAdditionalProperties() throws Exception {

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
            "\n" + "  @Coat.Embedded(key = \"embedded\", keySeparator= \".\")" +
            "\n" + "  public EmbeddedConfig embedded();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.MainConfig",
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "embedded.embeddedParam", "embedded value",
      "irrelevant key",         "irrelevant value"
    ));

    final Object expectedEmbedded= this.createInstance(generatedEmbeddedBuilderClass, Map.of("embeddedParam", "embedded value"));
    this.assertResult(instance, "embedded", expectedEmbedded);
  }


  /**
   * Test that annotation processing fails with a helpful error message if an embedded config
   * is defined as a collection.
   */
  @Test
  public void testEmbeddedConfigInCollection() throws Exception {

    // - preparation && execution

    final Compilation compilation=
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
            "\n" + "import java.util.List;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface MainConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"someParam\", defaultValue = \"some default\")" +
            "\n" + "  public String someParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Embedded(key = \"embedded\", keySeparator= \".\")" +
            "\n" + "  public List<EmbeddedConfig> embedded();" +
            "\n" + "}" +
            ""));

    CompilationSubject.assertThat(compilation).hadErrorContaining("Collection types are not supported for EmbeddedConfigs");
  }


  /**
   * Test that annotation processing fails with a helpful error message if an embedded config
   * not only has a @Coat.Embedded annotation, but also a @Coat.Param annotation.
   */
  @Test
  public void testEmbeddedConfigWithParamAnnotation() throws Exception {

    // - preparation && execution

    final Compilation compilation=
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
            "\n" + "import java.util.List;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface MainConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"someParam\", defaultValue = \"some default\")" +
            "\n" + "  public String someParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"embedded\", defaultValue = \"some default\")" +
            "\n" + "  @Coat.Embedded(key = \"embedded\", keySeparator= \".\")" +
            "\n" + "  public EmbeddedConfig embedded();" +
            "\n" + "}" +
            ""));

    CompilationSubject.assertThat(compilation).hadErrorContaining("@Param or @Embedded annotations are mutually exclusive");
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
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.DeeplyEmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedDeeplyEmbeddedBuilderClass= this.loadClass("com.example.DeeplyEmbeddedConfigBuilder", compilation);
    final Class<?> generatedDeeplyEmbeddedClass= this.loadClass("com.example.DeeplyEmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    this.assertMethods(generatedEmbeddedClass, "embeddedParam", "deeplyEmbedded");
    this.assertMethods(generatedDeeplyEmbeddedClass, "deeplyEmbeddedParam");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.


    // test equal objects
    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "someParam",                                   "some value",
      "embedded.embeddedParam",                      "embedded value",
      "embedded.deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value",
      "irrelevant key",                              "irrelevant value"
    ));

    this.assertResult(instance, "someParam", "some value");

    final Object equalObject = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "someParam",                                   "some value",
      "embedded.embeddedParam",                      "embedded value",
      "embedded.deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value",
      "irrelevant key",                              "IRRELEVANT VALUE TO BE IGNORED"
    ));

    assertThat(instance).isEqualTo(equalObject);
    assertThat(instance.hashCode()).isEqualTo(equalObject.hashCode());

    final Object unequalObject = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "someParam",                                   "some value",
      "embedded.embeddedParam",                      "embedded value",
      "embedded.deeplyEmbedded.deeplyEmbeddedParam", "DIFFERING deeply embedded value",
      "irrelevant key",                              "irrelevant value"
    ));

    assertThat(instance).isNotEqualTo(unequalObject);
    assertThat(instance.hashCode()).isNotEqualTo(unequalObject.hashCode());
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "omittedKey",
                       "omittedAnnotation",
                       "specifiedKey");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "omittedKey", "some value",
      "omittedAnnotation", "25",
      "Specified_KEY", "UTF-8"
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "omittedAnnotation", OptionalInt.of(25));
    this.assertResult(instance, "specifiedKey", UTF_8);
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
            "\n" + "import static de.poiu.coat.casing.CasingStrategy.AS_IS;" +
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "omittedKey",
                       "omittedAnnotation",
                       "specifiedKey");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "omittedKey", "some value",
      "omittedAnnotation", "25",
      "Specified_KEY", "UTF-8"
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "omittedAnnotation", OptionalInt.of(25));
    this.assertResult(instance, "specifiedKey", UTF_8);
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
            "\n" + "import static de.poiu.coat.casing.CasingStrategy.SNAKE_CASE;" +
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
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "omittedKey",
                       "omittedAnnotation",
                       "specifiedKey");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "omitted_key", "some value",
      "omitted_annotation", "25",
      "Specified_KEY", "UTF-8"
    ));

    this.assertResult(instance, "omittedKey", "some value");
    this.assertResult(instance, "omittedAnnotation", OptionalInt.of(25));
    this.assertResult(instance, "specifiedKey", UTF_8);
  }


  /**
   * Test that the casing strategy is applied to the key on an EmbeddedConfig.
   */
  @Test
  public void testCasingStrategyOnEmbeddedConfig() throws Exception {

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
            "\n" + "  @Coat.Param" +
            "\n" + "  public String embeddedParam();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.MainConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "import static de.poiu.coat.casing.CasingStrategy.SNAKE_CASE;" +
            "\n" + "" +
            "\n" + "@Coat.Config(casing = SNAKE_CASE)" +
            "\n" + "public interface MainConfig {" +
            "\n" + "" +
            "\n" + "  public String someParam();" +
            "\n" + "" +
            "\n" + "  @Coat.Embedded" +
            "\n" + "  public EmbeddedConfig embeddedConfig();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.MainConfig",
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    this.assertMethods(generatedConfigClass, "someParam", "embeddedConfig");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no values are explicitly set
      "some_param",                     "some value",
      "embedded_config.embeddedParam", "embedded value",
      "irrelevant key",                "irrelevant value"
    ));

    this.assertResult(instance, "someParam", "some value");
    final Object expectedEmbedded= this.createInstance(generatedEmbeddedBuilderClass, Map.of("embeddedParam", "embedded value"));
    this.assertResult(instance, "embeddedConfig", expectedEmbedded);
  }


  /**
   * Test that the java bean get prefix is ignored when generating keys (unless specified otherwise)
   */
  @Test
  public void testStripGetPrefix() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config()" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String normalKey();" +
            "\n" + "" +
            "\n" + "  public String getSomeString();" +
            "\n" + "" +
            "\n" + "  public String getnobean();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "normalKey",
                       "getSomeString",
                       "getnobean");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "normalKey", "normal value",
      "someString", "the prefix is stripped here",
      "getSomeString", "this should not exist",
      "getnobean", "the prefix is NOT stripped here",
      "nobean", "this should not exist"
    ));

    this.assertResult(instance, "normalKey", "normal value");
    this.assertResult(instance, "getSomeString", "the prefix is stripped here");
    this.assertResult(instance, "getnobean", "the prefix is NOT stripped here");
  }


  /**
   * Test that the java bean get prefix is retained when generating keys if specified.
   */
  @Test
  public void testDontStripGetPrefix() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config(stripGetPrefix = false)" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String normalKey();" +
            "\n" + "" +
            "\n" + "  public String getSomeString();" +
            "\n" + "" +
            "\n" + "  public String getnobean();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "normalKey",
                       "getSomeString",
                       "getnobean");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "normalKey", "normal value",
      "someString", "this should not exist",
      "getSomeString", "the prefix is NOT stripped here",
      "getnobean", "the prefix is NOT stripped here",
      "nobean", "this should not exist"
    ));

    this.assertResult(instance, "normalKey", "normal value");
    this.assertResult(instance, "getSomeString", "the prefix is NOT stripped here");
    this.assertResult(instance, "getnobean", "the prefix is NOT stripped here");
  }


  /**
   * Test that the java bean get prefix is ignored even when using a different casing strategy than AS_IS.
   */
  @Test
  public void testStripGetPrefixDifferentCasingStrategy() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "import static de.poiu.coat.casing.CasingStrategy.SNAKE_CASE;" +
            "\n" + "" +
            "\n" + "@Coat.Config(casing = SNAKE_CASE)" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String normalKey();" +
            "\n" + "" +
            "\n" + "  public String getSomeString();" +
            "\n" + "" +
            "\n" + "  public String getnobean();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "normalKey",
                       "getSomeString",
                       "getnobean");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "normal_key", "normal value",
      "some_string", "the prefix is stripped here",
      "get_some_string", "this should not exist",
      "getnobean", "the prefix is NOT stripped here",
      "nobean", "this should not exist"
    ));

    this.assertResult(instance, "normalKey", "normal value");
    this.assertResult(instance, "getSomeString", "the prefix is stripped here");
    this.assertResult(instance, "getnobean", "the prefix is NOT stripped here");
  }


  /**
   * Test that the java bean get prefix is not stripped if an explicit key definition contains it.
   */
  @Test
  public void testDontStripGetPrefixOnExplicitKey() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config()" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String getSomeString();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(key = \"getSomeOtherString\")" +
            "\n" + "  public String getSomeOtherString();" +
            "\n" + "}" +
            ""));

    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "getSomeString",
                       "getSomeOtherString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "someString", "the prefix is stripped here",
      "getSomeOtherString", "the prefix is NOT stripped here",
      "someOtherString", "this should not exist"
    ));

    this.assertResult(instance, "getSomeString", "the prefix is stripped here");
    this.assertResult(instance, "getSomeOtherString", "the prefix is NOT stripped here");
  }


  /**
   * Test that an exception is thrown on annotated types other than interfaces.
   */
  @Test
  public void testFailOnAnnotatedAbstractClass() throws Exception {
    // - preparation && execution && verification

    final Compilation compilation=
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

    CompilationSubject.assertThat(compilation).hadErrorContaining("@Coat.Config is only supported on interfaces at the moment");
  }


  /**
   * Test that multiple values can correctly be parsed into an array, a list and a set.
   */
  @Test
  public void testCollectionTypes() throws Exception {
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
            "\n" + "import java.util.List;" +
            "\n" + "import java.util.Set;" +
            "\n" + "import java.nio.file.Path;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String[] arrayOfStrings();" +
            "\n" + "" +
            "\n" + "  public List<Charset> listOfCharsets();" +
            "\n" + "" +
            "\n" + "  public Set<Path> setOfPaths();"+
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "arrayOfStrings",
                       "listOfCharsets",
                       "setOfPaths");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "arrayOfStrings", "val1 val2",
      "listOfCharsets", "UTF-8 US-ASCII",
      "setOfPaths", "/tmp /usr/share/doc /home/poiu"
    ));

    this.assertResult(instance, "arrayOfStrings", new String[]{"val1", "val2"});
    this.assertResult(instance, "listOfCharsets", List.of(UTF_8, US_ASCII));
    this.assertResult(instance, "setOfPaths", Set.of(Paths.get("/tmp"), Paths.get("/usr/share/doc"), Paths.get("/home/poiu")));
 }


  /**
   * Test that collections can take a default value.
   */
  @Test
  public void testCollectionWithDefault() throws Exception {
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
            "\n" + "import java.util.List;" +
            "\n" + "import java.util.Set;" +
            "\n" + "import java.nio.file.Path;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Coat.Param(defaultValue=\"one two\")" +
            "\n" + "  public String[] arrayOfStrings();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(defaultValue=\"UTF-8 US-ASCII\")" +
            "\n" + "  public List<Charset> listOfCharsets();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(defaultValue=\"/tmp /usr/share/doc /home/poiu\")" +
            "\n" + "  public Set<Path> setOfPaths();"+
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "arrayOfStrings",
                       "listOfCharsets",
                       "setOfPaths");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      // no values are set, therefore the defaults shoutld apply
    ));

    this.assertResult(instance, "arrayOfStrings", new String[]{"one", "two"});
    this.assertResult(instance, "listOfCharsets", List.of(UTF_8, US_ASCII));
    this.assertResult(instance, "setOfPaths", Set.of(Paths.get("/tmp"), Paths.get("/usr/share/doc"), Paths.get("/home/poiu")));
 }


  /**
   * Test that custom converters are actually used and the converters on a @Param annotation
   * take precedence over the @Config annotation;
   */
  @Test
  public void testNoConverterForType() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.math.BigInteger;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public BigInteger bigInt();" +
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "bigInt");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.


    final InvocationTargetException itex = catchThrowableOfType(() ->
      this.createInstance(generatedBuilderClass, mapOf(
        "bigInt", "200"
    )), InvocationTargetException.class);

    assertValidationErrors(itex, ImmutableValidationFailure.builder()
      .failureType(UNPARSABLE_VALUE)
      .key("bigInt")
      .type("java.math.BigInteger")
      .value("200")
      .errorMsg("No converter registered for type 'java.math.BigInteger'.")
      .build());
  }


  /**
   * Test that custom converters are actually used and the converters on a @Param annotation
   * take precedence over the @Config annotation;
   */
  @Test
  public void testCustomConverters_onParamAndConfig() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.HurzConverter",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.converters.Converter;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class HurzConverter implements Converter<String> {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String convert(final String s) throws TypeConversionException {" +
            "\n" + "    return \"Hurz!\";" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.UppercaseConverter",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.converters.Converter;" +
            "\n" + "import de.poiu.coat.convert.converters.StringConverter;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class UppercaseConverter implements Converter<String> {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String convert(final String s) throws TypeConversionException {" +
            "\n" + "    if (s == null) { return \"\"; };" +
            "\n" + "    return s.toUpperCase();" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.LowercaseConverter",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.converters.Converter;" +
            "\n" + "import de.poiu.coat.convert.converters.StringConverter;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class LowercaseConverter implements Converter<String> {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String convert(final String s) throws TypeConversionException {" +
            "\n" + "    if (s == null) { return \"\"; };" +
            "\n" + "    return s.toLowerCase();" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "" +
            "\n" + "@Coat.Config(converters={ UppercaseConverter.class })" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String normalString();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(converter=HurzConverter.class)" +
            "\n" + "  public String alwaysHurz();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(converter=LowercaseConverter.class)" +
            "\n" + "  public String alwaysLower();" +
            "\n" + "" +
            "\n" + "  public Charset unaffectedCharset();" +
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.HurzConverter",
                                "com.example.UppercaseConverter",
                                "com.example.LowercaseConverter",
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "normalString",
                       "alwaysHurz",
                       "alwaysLower",
                       "unaffectedCharset");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "normalString", "some value",
      "alwaysHurz", "25",
      "alwaysLower", "mIxEd CaSe",
      "unaffectedCharset", "UTF-8"
    ));

    this.assertResult(instance, "normalString", "SOME VALUE");
    this.assertResult(instance, "alwaysHurz", "Hurz!");
    this.assertResult(instance, "alwaysLower", "mixed case");
    this.assertResult(instance, "unaffectedCharset", UTF_8);
  }


  /**
   * Test that custom converters are correctly used for collections.
   */
  @Test
  public void testCustomConverters_inCollection() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.HurzConverter",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.converters.Converter;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class HurzConverter implements Converter<String> {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String convert(final String s) throws TypeConversionException {" +
            "\n" + "    return \"Hurz!\";" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.UppercaseConverter",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.converters.Converter;" +
            "\n" + "import de.poiu.coat.convert.converters.StringConverter;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class UppercaseConverter implements Converter<String> {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String convert(final String s) throws TypeConversionException {" +
            "\n" + "    if (s == null) { return \"\"; };" +
            "\n" + "    return s.toUpperCase();" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.List;" +
            "\n" + "" +
            "\n" + "@Coat.Config(converters={ UppercaseConverter.class })" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String[] normalStrings();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(converter=HurzConverter.class)" +
            "\n" + "  public List<String> alwaysHurz();" +
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.HurzConverter",
                                "com.example.UppercaseConverter",
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "normalStrings",
                       "alwaysHurz");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "normalStrings", "some value",
      "alwaysHurz", "one two 3"
    ));

    this.assertResult(instance, "normalStrings", new String[]{"SOME", "VALUE"});
    this.assertResult(instance, "alwaysHurz", List.of("Hurz!", "Hurz!", "Hurz!"));
  }


  /**
   * Test that custom converters that extend other converters can be used.
   */
  @Test
  public void testCustomConverters_ConverterExtendsOtherConverter() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.UppercaseConverter",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.converters.Converter;" +
            "\n" + "import de.poiu.coat.convert.converters.StringConverter;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class UppercaseConverter extends StringConverter {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String convert(final String s) throws TypeConversionException {" +
            "\n" + "    return super.convert(s).toUpperCase();" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "" +
            "\n" + "@Coat.Config(converters={ UppercaseConverter.class })" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String normalString();" +
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.UppercaseConverter",
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "normalString");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "normalString", "some value"
    ));

    this.assertResult(instance, "normalString", "SOME VALUE");
  }


  /**
   * Test that custom converters that are specified in the @Coat.Config annotation are valid for that
   * Config and not others.
   */
  @Test
  public void testCustomConverters_differentConvertersForSameType() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.HurzConverter",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.converters.Converter;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class HurzConverter implements Converter<String> {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String convert(final String s) throws TypeConversionException {" +
            "\n" + "    return \"Hurz!\";" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.UppercaseConverter",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.converters.Converter;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class UppercaseConverter implements Converter<String> {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String convert(final String s) throws TypeConversionException {" +
            "\n" + "    if (s == null) { return \"\"; };" +
            "\n" + "    return s.toUpperCase();" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String alwaysAsIs();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.TestConfigHurz",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config(converters={ HurzConverter.class })" +
            "\n" + "public interface TestConfigHurz {" +
            "\n" + "" +
            "\n" + "  public String alwaysHurz();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.TestConfigUppercase",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "" +
            "\n" + "@Coat.Config(converters={ UppercaseConverter.class })" +
            "\n" + "public interface TestConfigUppercase {" +
            "\n" + "" +
            "\n" + "  public String alwaysUppercase();" +
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.HurzConverter",
                                "com.example.UppercaseConverter",
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder",
                                "com.example.TestConfigHurz",
                                "com.example.TestConfigHurzBuilder",
                                "com.example.TestConfigUppercase",
                                "com.example.TestConfigUppercaseBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);
    this.assertMethods(generatedConfigClass,
                       "alwaysAsIs");

    final Class<?> generatedBuilderClassHurz= this.loadClass("com.example.TestConfigHurzBuilder", compilation);
    final Class<?> generatedConfigClassHurz= this.loadClass("com.example.TestConfigHurzBuilder$ConfigImpl", compilation);
    this.assertMethods(generatedConfigClassHurz,
                       "alwaysHurz");

    final Class<?> generatedBuilderClassUppercase= this.loadClass("com.example.TestConfigUppercaseBuilder", compilation);
    final Class<?> generatedConfigClassUppercase= this.loadClass("com.example.TestConfigUppercaseBuilder$ConfigImpl", compilation);
    this.assertMethods(generatedConfigClassUppercase,
                       "alwaysUppercase");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "alwaysAsIs", "one"
    ));
    final Object instanceHurz = this.createInstance(generatedBuilderClassHurz, mapOf(
      "alwaysHurz", "one"
    ));
    final Object instanceUppercase = this.createInstance(generatedBuilderClassUppercase, mapOf(
      "alwaysUppercase", "one"
    ));

    this.assertResult(instance, "alwaysAsIs", "one");
    this.assertResult(instanceHurz, "alwaysHurz", "Hurz!");
    this.assertResult(instanceUppercase, "alwaysUppercase", "ONE");
  }


  /**
   * Test that custom ListParsers are actually used and the ListParsers on a @Param annotation
   * take precedence over the @Config annotation;
   */
  @Test
  public void testCustomListParser_onParamAndConfig() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.CommaListParser",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.listparsers.ListParser;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class CommaListParser implements ListParser {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String[] convert(final String s) throws TypeConversionException {" +
            "\n" + "    return s.split(\"\\\\s*,\\\\s*\");" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.PipeListParser",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.convert.listparsers.ListParser;" +
            "\n" + "import de.poiu.coat.convert.TypeConversionException;" +
            "\n" + "" +
            "\n" + "public class PipeListParser implements ListParser {" +
            "\n" + "" +
            "\n" + "  @Override" +
            "\n" + "  public String[] convert(final String s) throws TypeConversionException {" +
            "\n" + "    return s.split(\"\\\\s*\\\\|\\\\s*\");" +
            "\n" + "  }" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.List;" +
            "\n" + "" +
            "\n" + "@Coat.Config(listParser=CommaListParser.class)" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  public String normalString();" +
            "\n" + "" +
            "\n" + "  public List<String> commaSeparated();" +
            "\n" + "" +
            "\n" + "  @Coat.Param(listParser=PipeListParser.class)" +
            "\n" + "  public List<String> pipeSeparated();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.OtherConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.nio.charset.Charset;" +
            "\n" + "import java.util.List;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface OtherConfig {" +
            "\n" + "" +
            "\n" + "  public List<String> whitespaceSeparated();" +
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.CommaListParser",
                                "com.example.PipeListParser",
                                "com.example.TestConfig",
                                "com.example.OtherConfig",
                                "com.example.TestConfigBuilder",
                                "com.example.OtherConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedOtherBuilderClass= this.loadClass("com.example.OtherConfigBuilder", compilation);
    final Class<?> generatedOtherClass= this.loadClass("com.example.OtherConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "normalString",
                       "commaSeparated",
                       "pipeSeparated");
    this.assertMethods(generatedOtherClass,
                       "whitespaceSeparated");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "normalString", "some | value",
      "commaSeparated", "first string, second string",
      "pipeSeparated", "first, string | second, string"
    ));
    final Object otherInstance = this.createInstance(generatedOtherBuilderClass, mapOf(
      "whitespaceSeparated", "first, string | second, string"
    ));

    this.assertResult(instance, "normalString", "some | value");
    this.assertResult(instance, "commaSeparated", List.of("first string", "second string"));
    this.assertResult(instance, "pipeSeparated", List.of("first, string", "second, string"));
    this.assertResult(otherInstance, "whitespaceSeparated", List.of("first,", "string", "|", "second,", "string"));
  }


  /**
   * Test that additional annotations (like for bean validation) do not disturb the
   * Coat generation.
   */
  @Test
  public void testAdditionalAnnotations() throws Exception {
    // - preparation && execution

    final Compilation compilation =
      javac()
        .withProcessors(new CoatProcessor())
        .compile(JavaFileObjects.forSourceString("com.example.annotation.Hurz",
            "" +
            "\n" + "package com.example.annotation;" +
            "\n" + "" +
            "\n" + "import java.lang.annotation.*;" +
            "\n" + "" +
            "\n" + "@Retention(RetentionPolicy.RUNTIME)" +
            "\n" + "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})" +
            "\n" + "public @interface Hurz {" +
            "\n" + "" +
            "\n" + "  public int     foo();" +
            "\n" + "  public String  bar();" +
            "\n" + "}" +
            ""),
                 JavaFileObjects.forSourceString("com.example.TestConfig",
            "" +
            "\n" + "package com.example;" +
            "\n" + "" +
            "\n" + "import com.example.annotation.Hurz;" +
            "\n" + "import de.poiu.coat.annotation.Coat;" +
            "\n" + "import java.lang.Integer;" +
            "\n" + "import java.util.List;" +
            "\n" + "import java.util.OptionalInt;" +
            "\n" + "" +
            "\n" + "@Coat.Config" +
            "\n" + "public interface TestConfig {" +
            "\n" + "" +
            "\n" + "  @Hurz(foo = 10, bar = \"frobnitz\")" +
            "\n" + "  public String mandatoryString();" +
            "\n" + "" +
            "\n" + "  @Hurz(foo = 15, bar = \"baz\")" +
            "\n" + "  public int primitiveInt();" +
            "\n" + "" +
            "\n" + "  @Hurz(foo = 20, bar = \"qux\")" +
            "\n" + "  public OptionalInt optionalInt();" +
            "\n" + "" +
            "\n" + "  @Hurz(foo = 25, bar = \"ham\")" +
            "\n" + "  public String[] array();" +
            "\n" + "" +
            "\n" + "  @Hurz(foo = 30, bar = \"eggs\")" +
            "\n" + "  public boolean bool();" +
            "\n" + "" +
            "\n" + "  public List<@Hurz(foo = 30, bar = \"spam\") Integer> listOfInts();" +
            "\n" + "}" +
            ""));


    // - verification

    CompilationSubject.assertThat(compilation).succeeded();

    this.assertGeneratedClasses(compilation,
                                "com.example.TestConfig",
                                "com.example.TestConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.TestConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.TestConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedConfigClass,
                       "mandatoryString",
                       "primitiveInt",
                       "optionalInt",
                       "array",
                       "bool",
                       "listOfInts");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance = this.createInstance(generatedBuilderClass, mapOf(
      "mandatoryString", "this is my string",
      "primitiveInt", "1",
      "optionalInt", "3",
      "array", "one two three",
      "bool", "true",
      "listOfInts", "1 2 3"
    ));

    this.assertResult(instance, "mandatoryString", "this is my string");
    this.assertResult(instance, "primitiveInt", 1);
    this.assertResult(instance, "optionalInt", OptionalInt.of(3));
    this.assertResult(instance, "array", new String[]{"one", "two", "three"});
    this.assertResult(instance, "bool", Boolean.TRUE);
    this.assertResult(instance, "listOfInts", List.of(1, 2, 3));
  }


  /**
   * Test that config values can be set via environment variables.
   */
  @Test
  @SetEnvironmentVariable(key = "some_param", value = "some env var in lowercase")
  @SetEnvironmentVariable(key = "EMBEDDED_EMBEDDED_PARAM", value = "embedded env var in uppercase")
  @SetEnvironmentVariable(key = "IRRELEVANT", value = "irrelevant")
  public void testConfigFromEnvVars() throws Exception {

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
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object instance= generatedBuilderClass.getDeclaredMethod("fromEnvVars").invoke(null);

    this.assertResult(instance, "someParam", "some env var in lowercase");
    final Object expectedEmbedded= this.createInstance(generatedEmbeddedBuilderClass, Map.of("embeddedParam", "embedded env var in uppercase"));
    this.assertResult(instance, "embedded", expectedEmbedded);
  }


  /**
   * Test that Config instances can be created via Builder and multiple sources can be merged.
   */
  @Test
  @SetEnvironmentVariable(key = "SOME_PARAM", value = "some param set via env var")
  @SetEnvironmentVariable(key = "IRRELEVANT", value = "irrelevant")
  public void testConfigFromMultipleSourcesViaBuilder() throws Exception {

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
                                "com.example.MainConfigBuilder",
                                "com.example.EmbeddedConfig",
                                "com.example.EmbeddedConfigBuilder");

    final Class<?> generatedBuilderClass= this.loadClass("com.example.MainConfigBuilder", compilation);
    final Class<?> generatedConfigClass= this.loadClass("com.example.MainConfigBuilder$ConfigImpl", compilation);
    final Class<?> generatedEmbeddedBuilderClass= this.loadClass("com.example.EmbeddedConfigBuilder", compilation);
    final Class<?> generatedEmbeddedClass= this.loadClass("com.example.EmbeddedConfigBuilder$ConfigImpl", compilation);

    this.assertMethods(generatedEmbeddedClass, "embeddedParam");
    this.assertMethods(generatedConfigClass, "someParam", "embedded");
    // FIXME: Should we check return types here? Shouldn't be necessary, as we call them later and check the result
    //        In fact we would not even need this assertion above, as we are callign each of these methods.

    final Object builderInstance= generatedBuilderClass.getDeclaredMethod("create").invoke(null);
    builderInstance.getClass().getDeclaredMethod("add", Map.class).invoke(builderInstance, mapOf(
      "someParam",              "some param set via map",
      "embedded.embeddedParam", "embedded param set via map"
    ));
    builderInstance.getClass().getDeclaredMethod("addEnvVars").invoke(builderInstance);
    final Object instance= builderInstance.getClass().getDeclaredMethod("build").invoke(builderInstance);

    this.assertResult(instance, "someParam", "some param set via env var");
    final Object expectedEmbedded= this.createInstance(generatedEmbeddedBuilderClass, Map.of("embeddedParam", "embedded param set via map"));
    this.assertResult(instance, "embedded", expectedEmbedded);
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
   * @param compilation the compiloation builderInstance from which to take the class
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
   * Create an builder instance of the given <code>clazz</code> with the given <code>props</code> as parameter
   * to the constructor.
   *
   * @param clazz the class to load
   * @param props the properties to fill the (Coat config) object
   * @return the newly created builder instance
   * @throws NoSuchMethodException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws IllegalArgumentException
   * @throws InvocationTargetException
   */
  private Object createInstance(final Class<?> clazz, final Map<String, String> props) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ConfigValidationException {
    return clazz.getDeclaredMethod("from", Map.class).invoke(null, props);
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
    // There should be only 1 interface via which we can access the accessor method
    final Object actualResult = instance.getClass().getInterfaces()[0].getMethod(methodName).invoke(instance);
    if (expectedResult != null) {
      assertThat(actualResult).isEqualTo(expectedResult);
    } else {
      assertThat(actualResult).isNull();
    }
  }


  /**
   * Assert that the <code>clazz</code> contains all methods of the given
   * <code>expectedMethods</code> plus the always generated methods and delegate methods
   * (e.g. "equals()", "hashCode(), "toString()", "validate()", etc.).
   *
   * @param clazz the class to check
   * @param expectedMethods the methods to assert
   */
  private void assertMethods(final Class<?> clazz, final MethodAndParams... expectedMethods) {
    final List<MethodAndParams> generatedMethods = Stream.of(clazz.getDeclaredMethods())
      .map(MethodAndParams::from)
      .filter(m -> !m.equals(MethodAndParams.from("equals", Object.class)))
      .filter(m -> !m.equals(MethodAndParams.from("hashCode")))
      .filter(m -> !m.equals(MethodAndParams.from("toString")))
      .filter(m -> !m.equals(MethodAndParams.from("writeExampleConfig", java.io.Writer.class)))
      .filter(m -> !m.equals(MethodAndParams.from("from", java.util.Map.class)))
      .filter(m -> !m.equals(MethodAndParams.from("from", java.io.File.class)))
      .filter(m -> !m.equals(MethodAndParams.from("from", java.util.Properties.class)))
      .filter(m -> !m.equals(MethodAndParams.from("add", java.util.Map.class)))
      .filter(m -> !m.equals(MethodAndParams.from("add", java.io.File.class)))
      .filter(m -> !m.equals(MethodAndParams.from("add", java.util.Properties.class)))
      .filter(m -> !m.equals(MethodAndParams.from("fromEnvVars")))
      .filter(m -> !m.equals(MethodAndParams.from("addEnvVars")))
      .filter(m -> !m.equals(MethodAndParams.from("builder")))
      .filter(m -> !m.equals(MethodAndParams.from("access$000", java.io.File.class))) // FIXME: This is actually CoatConfigBuilder#toMap(File) Generate it?
      .filter(m -> !m.equals(MethodAndParams.from("access$100", java.util.Properties.class))) // FIXME: This is actually CoatConfigBuilder#toMap(Properties) Generate it?
      .collect(toList());

    assertThat(
      generatedMethods)
      .containsExactlyInAnyOrder(
        expectedMethods);
  }


  /**
   * Assert that the <code>generatedConfigClass</code> contains all methods of the given
   * <code>methodNames</code> plus the always generated methods and delegate methods
   * (e.g. "equals()", "hashCode(), "toString()", "validate()", etc.).
   * <p>
   * This method expects only parameterless methods. To check for methods with parameters use
   * {@link #assertMethods(java.lang.Class, de.poiu.coat.processor.MethodAndParams...)}.
   *
   * @param generatedConfigClass the class to check
   * @param methodNames the names of the methods to assert
   */
  private void assertMethods(final Class<?> generatedConfigClass, final String... methodNames) {
    final MethodAndParams[] expected= new MethodAndParams[methodNames.length];
    for (int i= 0; i < methodNames.length; i++) {
      expected[i]= MethodAndParams.from(methodNames[i]);
    }

    this.assertMethods(generatedConfigClass, expected);
  }


  /**
   * Assert that calling the given exception <code>itex</code> that is thrown on building a Coat Config
   * object from a generated builder contains the given <code>validationFailureMessages</code>.
   * @param itex the exception that was thrown on building the config object
   * @param validationFailureMessages the expected validation failure messages
   */
  private void assertValidationErrors(final InvocationTargetException itex, final ValidationFailure... validationFailureMessages) {
    assertThat(itex)
      .hasRootCauseInstanceOf(ConfigValidationException.class)
      .rootCause()
      .extracting("validationResult", as(type(ValidationResult.class)))
      .matches(ValidationResult::hasFailures)
      .extracting("validationFailures", as(collection(ValidationFailure.class)))
      .containsExactlyInAnyOrder(validationFailureMessages);
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
