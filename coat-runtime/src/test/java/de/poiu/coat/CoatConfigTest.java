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
package de.poiu.coat;

import de.poiu.coat.convert.Converter;
import de.poiu.coat.validation.ConfigValidationException;
import de.poiu.coat.validation.ImmutableValidationFailure;
import de.poiu.coat.validation.ValidationResult;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static de.poiu.coat.validation.ValidationFailure.Type.MISSING_MANDATORY_VALUE;
import static de.poiu.coat.validation.ValidationFailure.Type.UNPARSABLE_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


/**
 *
 */
public class CoatConfigTest {

  private static class ConfigImpl extends CoatConfig {
    public ConfigImpl(final Map<String, String> map, final ConfigParam[] params) {
      super(map, params);
    }
  }

  private static class ParamImpl implements ConfigParam {
    private final String   key;
    private final Class<?> type;
    private final Class<?> collectionType;
    private final String   defaultValue;
    private final boolean  mandatory;
    private final Class<? extends Converter<?>> converter;


    public ParamImpl(String key, Class<?> type, Class<?> collectionType, String defaultValue, boolean mandatory, Class<? extends Converter<?>> converter) {
      this.key            = key;
      this.type           = type;
      this.collectionType = collectionType;
      this.defaultValue   = defaultValue;
      this.mandatory      = mandatory;
      this.converter      = converter;
    }


    @Override
    public String key() {
      return this.key;
    }


    @Override
    public Class<?> type() {
      return this.type;
    }


    @Override
    public Class<?> collectionType() {
      return this.collectionType;
    }


    @Override
    public String defaultValue() {
      return this.defaultValue;
    }


    @Override
    public boolean mandatory() {
      return this.mandatory;
    }


    @Override
    public Class<? extends Converter<?>> converter() {
      return converter;
    }
  }


  // TODO: Test all corner cases:
  //       - different parameter types
  //       - optionals, mandatory, default values
  //       - overriding through environment variables
  // TODO: Test all protected methods

  @Test
  public void testValidate_missingKey() throws Exception {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", String.class, null, null, true, null);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, null, true, null);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("key1", "val1")
      ,
      new ConfigParam[]{
        p1,
        p2,
        p3,
      });

    // - test

    final Throwable thrown= catchThrowable(() -> c.validate());

    // - verification

    assertThat(thrown).isInstanceOf(ConfigValidationException.class);
    final ConfigValidationException ex= (ConfigValidationException) thrown;
    final ValidationResult result= ex.getValidationResult();
    assertThat(result.hasFailures()).isTrue();
    assertThat(result.validationFailures()).containsExactlyInAnyOrder(
      ImmutableValidationFailure.builder()
        .failureType(MISSING_MANDATORY_VALUE)
        .key("key2")
        .build()
    );
  }


  @Test
  public void testValidate_parseError() throws Exception {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", int.class,    null, null, true, null);
    final ConfigParam p2= new ParamImpl("key2", int.class,    null, null, true, null);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("key1", "55",
             "key2", "dummy")
      ,
      new ConfigParam[]{
        p1,
        p2,
        p3,
      });

    // - test

    final Throwable thrown= catchThrowable(() -> c.validate());

    // - verification

    assertThat(thrown).isInstanceOf(ConfigValidationException.class);
    final ConfigValidationException ex= (ConfigValidationException) thrown;
    final ValidationResult result= ex.getValidationResult();
    assertThat(result.hasFailures()).isTrue();
    assertThat(result.validationFailures()).containsExactlyInAnyOrder(
      ImmutableValidationFailure.builder()
        .failureType(UNPARSABLE_VALUE)
        .key("key2")
        .type("int")
        .value("dummy")
        .build()
    );
  }


  @Test
  public void testGet_String() {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", String.class, null, null, false, null);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, null, false, null);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("key1", "val1", "key2", "val2")
      ,
      new ConfigParam[]{
        p1,
        p3,
      });

    // - test and verification

    assertThat(c.get("key1")).isEqualTo("val1");
    assertThat(c.get("key2")).isEqualTo("val2");
    assertThat(c.get("key3")).isNull();
  }


  @Test
  public void testGet_Int() {
    // - preparation

    final ConfigParam dec= new ParamImpl("dec", int.class, null, null, false, null);
    final ConfigParam hex= new ParamImpl("hex", int.class, null, null, false, null);
    final ConfigParam oct= new ParamImpl("oct", int.class, null, null, false, null);
    final ConfigParam bin= new ParamImpl("bin", int.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("dec", "1234",
             "hex", "0xff",
             "oct", "0644",
             "bin", "0b01010101")
      ,
      new ConfigParam[]{
        dec,
        hex,
        oct,
        bin,
      });

    // - test and verification

    assertThat(c.getInt(dec)).isEqualTo(1234);
    assertThat(c.getInt(hex)).isEqualTo(255);
    assertThat(c.getInt(oct)).isEqualTo(420);
    assertThat(c.getInt(bin)).isEqualTo(85);
  }


  @Test
  public void testGet_IntWithUnderscores() {
    // - preparation

    final ConfigParam dec= new ParamImpl("dec", int.class, null, null, false, null);
    final ConfigParam hex= new ParamImpl("hex", int.class, null, null, false, null);
    final ConfigParam oct= new ParamImpl("oct", int.class, null, null, false, null);
    final ConfigParam bin= new ParamImpl("bin", int.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("dec", "1_2___34",
             "hex", "0xf_f",
             "oct", "0_644",
             "bin", "0b0101_0101")
      ,
      new ConfigParam[]{
        dec,
        hex,
        oct,
        bin,
      });

    // - test and verification

    assertThat(c.getInt(dec)).isEqualTo(1_2___34);
    assertThat(c.getInt(hex)).isEqualTo(0xf_f);
    assertThat(c.getInt(oct)).isEqualTo(0_644);
    assertThat(c.getInt(bin)).isEqualTo(0b0101_0101);
  }


  @Test
  public void testGet_Long() {
    // - preparation

    final ConfigParam dec= new ParamImpl("dec", long.class, null, null, false, null);
    final ConfigParam hex= new ParamImpl("hex", long.class, null, null, false, null);
    final ConfigParam oct= new ParamImpl("oct", long.class, null, null, false, null);
    final ConfigParam bin= new ParamImpl("bin", long.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("dec", "1234",
             "hex", "0xff",
             "oct", "0644",
             "bin", "0b01010101")
      ,
      new ConfigParam[]{
        dec,
        hex,
        oct,
        bin,
      });

    // - test and verification

    assertThat(c.getLong(dec)).isEqualTo(1234);
    assertThat(c.getLong(hex)).isEqualTo(255);
    assertThat(c.getLong(oct)).isEqualTo(420);
    assertThat(c.getLong(bin)).isEqualTo(85);
  }


  @Test
  public void testGet_LongWithUnderscores() {
    // - preparation

    final ConfigParam dec= new ParamImpl("dec", long.class, null, null, false, null);
    final ConfigParam hex= new ParamImpl("hex", long.class, null, null, false, null);
    final ConfigParam oct= new ParamImpl("oct", long.class, null, null, false, null);
    final ConfigParam bin= new ParamImpl("bin", long.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("dec", "1_2___34",
             "hex", "0xf_f",
             "oct", "0_644",
             "bin", "0b0101_0101")
      ,
      new ConfigParam[]{
        dec,
        hex,
        oct,
        bin,
      });

    // - test and verification

    assertThat(c.getLong(dec)).isEqualTo(1_2___34);
    assertThat(c.getLong(hex)).isEqualTo(0xf_f);
    assertThat(c.getLong(oct)).isEqualTo(0_644);
    assertThat(c.getLong(bin)).isEqualTo(0b0101_0101);
  }


  @Test
  public void testGet_Double() {
    // - preparation

    final ConfigParam dec= new ParamImpl("dec", double.class, null, null, false, null);
    final ConfigParam hex= new ParamImpl("hex", double.class, null, null, false, null);
    final ConfigParam dot= new ParamImpl("dot", double.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("dec", "1234",
             "hex", "0xffp0",
             "dot", "1234.5678"
             )
      ,
      new ConfigParam[]{
        dec,
        hex,
        dot
      });

    // - test and verification

    assertThat(c.getDouble(dec)).isEqualTo(1234d);
    assertThat(c.getDouble(hex)).isEqualTo(255d);
    assertThat(c.getDouble(dot)).isEqualTo(1234.5678d);
  }


  @Test
  public void testGet_DoubleWithExponent() {
    // - preparation

    final ConfigParam p1= new ParamImpl("int", double.class, null, null, false, null);
    final ConfigParam p2= new ParamImpl("dot", double.class, null, null, false, null);
    final ConfigParam p3= new ParamImpl("hex", double.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("int", "1234e2",
             "dot", "1234.56e2",
             "hex", "0xffp2"
             )
      ,
      new ConfigParam[]{
        p1,
        p2,
        p3,
      });

    // - test and verification

    assertThat(c.getDouble(p1)).isEqualTo(123400d);
    assertThat(c.getDouble(p2)).isEqualTo(123456d);
    assertThat(c.getDouble(p3)).isEqualTo(1020d);
  }


  @Test
  public void testGet_DoubleWithUnderscores() {
    // - preparation

    final ConfigParam dec= new ParamImpl("dec", double.class, null, null, false, null);
    final ConfigParam hex= new ParamImpl("hex", double.class, null, null, false, null);
    final ConfigParam dot= new ParamImpl("dot", double.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("dec", "1_2__34",
             "hex", "0xf__fp0",
             "dot", "1_234.5_6__7___8"
             )
      ,
      new ConfigParam[]{
        dec,
        hex,
        dot
      });

    // - test and verification

    assertThat(c.getDouble(dec)).isEqualTo(1_2__34d);
    assertThat(c.getDouble(hex)).isEqualTo(0xf_fp0d);
    assertThat(c.getDouble(dot)).isEqualTo(1_234.5_6__7___8d);
  }


  @Test
  public void testGet_ConfigParam() {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", String.class, null, null, false, null);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, null, false, null);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("key1", "val1", "key2", "val2")
      ,
      new ConfigParam[]{
        p1,
        p3,
      });

    // - test and verification

    assertThat((String)c.get(p1)).isEqualTo("val1");
    assertThat((String)c.get(p2)).isEqualTo("val2");
    assertThat((String)c.get(p3)).isNull();
  }


  @Test
  public void testGetOptional() {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", String.class, null, null, false, null);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, null, false, null);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("key1", "val1", "key2", "val2")
      ,
      new ConfigParam[]{
        p1,
        p3,
      });

    // - test and verification

    assertThat(c.getOptional(p1)).isEqualTo(Optional.of("val1"));
    assertThat(c.getOptional(p2)).isEqualTo(Optional.of("val2"));
    assertThat(c.getOptional(p3)).isEqualTo(Optional.empty());
  }


  @Test
  public void testGetOrDefault() {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", String.class, null, null,       false, null);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, "default2", false, null);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, "default3", false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("key1", "val1", "key2", "val2")
      ,
      new ConfigParam[]{
        p1,
        p2,
        p3,
      });

    // - test and verification

    assertThat((String)c.getOrDefault(p1)).isEqualTo("val1");
    assertThat((String)c.getOrDefault(p2)).isEqualTo("val2");
    assertThat((String)c.getOrDefault(p3)).isEqualTo("default3");
  }


  @Test
  public void testCollectionTypes() {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", String.class, Array.class, null, false, null);
    final ConfigParam p2= new ParamImpl("key2", String.class, List.class,  null, false, null);
    final ConfigParam p3= new ParamImpl("key3", String.class, Set.class,   null, false, null);

    final CoatConfig c= new ConfigImpl(
      Map.of("key1", "val1.1 val1.2", "key2", "val2.1 val2.2", "key3", "val3.1 val3.2")
      ,
      new ConfigParam[]{
        p1,
        p2,
        p3,
      });

    // - test and verification

    assertThat((String[])c.getArray(p1)).isEqualTo(new String[]{"val1.1", "val1.2"});
    assertThat(c.getList(p2)).isEqualTo(List.of("val2.1", "val2.2"));
    assertThat(c.getSet(p3)).isEqualTo(Set.of("val3.1", "val3.2"));
  }
}
