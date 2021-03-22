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

import de.poiu.coat.validation.ConfigValidationException;
import de.poiu.coat.validation.ValidationFailure;
import de.poiu.coat.validation.ValidationResult;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
    private final String key;
    private final Class<?> type;
    private final String defaultValue;
    private final boolean mandatory;


    public ParamImpl(String key, Class<?> type, String defaultValue, boolean mandatory) {
      this.key = key;
      this.type = type;
      this.defaultValue = defaultValue;
      this.mandatory = mandatory;
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
    public String defaultValue() {
      return this.defaultValue;
    }


    @Override
    public boolean mandatory() {
      return this.mandatory;
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

    final ConfigParam p1= new ParamImpl("key1", String.class, null, true);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, true);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, false);

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
    assertThat(result.getValidationFailures()).containsExactlyInAnyOrder(
      new ValidationFailure("Mandatory value for \"key2\" is missing.")
    );
  }


  @Test
  public void testValidate_parseError() throws Exception {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", int.class,    null, true);
    final ConfigParam p2= new ParamImpl("key2", int.class,    null, true);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, false);

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
    assertThat(result.getValidationFailures()).containsExactlyInAnyOrder(
      new ValidationFailure("Config value for \"key2\" cannot be convert to type \"int\": dummy")
    );
  }


  @Test
  public void testGet_String() {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", String.class, null, false);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, false);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, false);

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
  public void testGet_ConfigParam() {
    // - preparation

    final ConfigParam p1= new ParamImpl("key1", String.class, null, false);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, false);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, false);

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

    final ConfigParam p1= new ParamImpl("key1", String.class, null, false);
    final ConfigParam p2= new ParamImpl("key2", String.class, null, false);
    final ConfigParam p3= new ParamImpl("key3", String.class, null, false);

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

    final ConfigParam p1= new ParamImpl("key1", String.class, null, false);
    final ConfigParam p2= new ParamImpl("key2", String.class, "default2", false);
    final ConfigParam p3= new ParamImpl("key3", String.class, "default3", false);

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
}
