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

import de.poiu.coat.convert.CharsetConverter;
import de.poiu.coat.convert.Converter;
import de.poiu.coat.convert.DurationConverter;
import de.poiu.coat.convert.FileConverter;
import de.poiu.coat.convert.InetAddressConverter;
import de.poiu.coat.convert.LocalDateConverter;
import de.poiu.coat.convert.LocalDateTimeConverter;
import de.poiu.coat.convert.LocalTimeConverter;
import de.poiu.coat.convert.MessageDigestConverter;
import de.poiu.coat.convert.PathConverter;
import de.poiu.coat.convert.StringConverter;
import de.poiu.coat.convert.TypeConversionException;
import de.poiu.coat.convert.UncheckedTypeConversionException;
import de.poiu.coat.validation.ConfigValidationException;
import de.poiu.coat.validation.ImmutableValidationFailure;
import de.poiu.coat.validation.ImmutableValidationResult;
import de.poiu.coat.validation.ValidationFailure;
import de.poiu.coat.validation.ValidationResult;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static de.poiu.coat.validation.ValidationFailure.Type.MISSING_MANDATORY_VALUE;
import static de.poiu.coat.validation.ValidationFailure.Type.UNPARSABLE_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.WARNING;


/**
 * Base class for all generated config classes that are generated by Coat.
 * <p>
 * This class provides all the gory defails of calling different converters for the corresponding
 * types and whether to return a default value or not.
 */
public abstract class CoatConfig {

  private static final Logger LOGGER= Logger.getLogger(CoatConfig.class.getName());

  private static final Map<Class<?>, Converter<?>> converters= new ConcurrentHashMap<>();

  static {
    converters.put(String.class,        new StringConverter());
    converters.put(Duration.class,      new DurationConverter());
    converters.put(LocalDate.class,     new LocalDateConverter());
    converters.put(LocalTime.class,     new LocalTimeConverter());
    converters.put(LocalDateTime.class, new LocalDateTimeConverter());
    converters.put(File.class,          new FileConverter());
    converters.put(Path.class,          new PathConverter());
    converters.put(Charset.class,       new CharsetConverter());
    converters.put(InetAddress.class,   new InetAddressConverter());
    converters.put(MessageDigest.class, new MessageDigestConverter());
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final Map<String, String> props= new HashMap<>();

  private final ConfigParam[] params;

  private final Map<String, CoatConfig> embeddedConfigs= new LinkedHashMap<>();


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  /**
   * Creates a new CoatConfig object by reading the given {@code file} into a {@link Properties}
   * instance that should provide the values for the given {@link params}.
   *
   * @param file the config file to read
   * @param params the defined config keys for this CoatConfig
   * @throws IOException if reading the given file failed
   */
  protected CoatConfig(final File file, final ConfigParam[] params) throws IOException {
    this(toMap(file), params);

    // FIXME: By using java.util.Properties, the properties are unsorted. We would have to either
    //        use apron or parse them here again (which would be too much of a hassle)

    // TODO: Differntiate between ignoring unexpected values, logging them or throwing an exception
    //       Better do this only at validation time

  }


  /**
   * Creates a new CoatConfig object from a {@link Properties}
   * instance that should provide the values for the given {@link params}.
   *
   * @param jup the Properties object with the config data
   * @param params the defined config keys for this CoatConfig
   */
  protected CoatConfig(final Properties jup, final ConfigParam[] params) {
    this(toMap(jup), params);
  }


  /**
   * Creates a new CoatConfig object from a Map instance that should provide the values
   * for the given {@link params}.
   *
   * @param props the map with the config data
   * @param params the defined config keys for this CoatConfig
   */
  protected CoatConfig(final Map<String, String> props, final ConfigParam[] params) {
    this.props.putAll(props);
    this.params= params;
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  /**
   * Validate this CoatConfig.
   * <p>
   * If validation succeds this method just returns.
   * <p>
   * If validation fails a {@code ConfigValidationException} is thrown that contains a
   * {@link ValidationResult} with details about the actual validation failures.
   *
   * @throws ConfigValidationException if validation fails
   */
  public void validate() throws ConfigValidationException {
    final ImmutableValidationResult.Builder resultBuilder= ImmutableValidationResult.builder();

    // verify missing mandatory parameters
    for (final ConfigParam param : this.params) {
      if (param.mandatory()
        && param.defaultValue() == null
        && this.getString(param) == null) {
        resultBuilder.addValidationFailure(
          ImmutableValidationFailure.builder()
            .failureType(MISSING_MANDATORY_VALUE)
            .key(param.key())
            .build()
        );
      }
    }

    // verify validity of specified parameters
    for (final ConfigParam param : this.params) {
      final String stringValue= this.getString(param);
      if (stringValue == null) {
        continue;
      }

      try {
        if (this.isPrimitive(param)) {
          this.convertPrimitive(stringValue, param);
        } else {
          this.convertValue(stringValue, param);
        }
      } catch (TypeConversionException ex) {
        resultBuilder.addValidationFailure(
          ImmutableValidationFailure.builder()
            .failureType(UNPARSABLE_VALUE)
            .key(param.key())
            .type(param.type().getName())
            .value(stringValue)
            .build()
        );
      }
    }

    // verify validity of embedded configs
    for (final Map.Entry<String, CoatConfig> entry : this.embeddedConfigs.entrySet()) {
      final String prefix = entry.getKey();
      final CoatConfig embeddedConfig = entry.getValue();

      try {
        embeddedConfig.validate();
      } catch (ConfigValidationException ex) {
        for (final ValidationFailure f : ex.getValidationResult().validationFailures()) {
          resultBuilder.addValidationFailure(
            ImmutableValidationFailure.copyOf(f)
              .withKey(prefix + f.key())
          );
        }
      }
    }

    final ValidationResult result= resultBuilder.build();

    if (result.hasFailures()) {
      throw new ConfigValidationException(result);
    }

    // TODO: Should this method throw a ValidationException (which contains validation details)
    //       or should it return a ValidationResult object?
    //       Or should we provide both methods?

  }


  /**
   * Get the value for the given key.
   * No special logic is applied. The value is returned exactly as contained in the configuration.
   * <p>
   * No default values will be respected. If the given key doesn't have a value, {@code null} is
   * returned.
   *
   * @param key the config key
   * @return the value of the config key as String
   */
  public String get(final String key) {
    return this.props.get(key);
  }


  public <T> T get(final ConfigParam configParam) throws UncheckedTypeConversionException {
    final String stringValue= this.getString(configParam);
    try {
      return this.convertValue(stringValue, configParam);
    } catch (TypeConversionException e) {
      throw new UncheckedTypeConversionException("Error converting value " + stringValue + " to type " + configParam.type().getName(), e);
    }
  }


  public <T> Optional<T> getOptional(final ConfigParam configParam) {
    final String stringValue= this.getString(configParam);
    if (stringValue == null || stringValue.trim().isEmpty()) {
      return Optional.empty();
    } else {
      try {
        final T t= this.convertValue(stringValue, configParam);
        return Optional.of(t);
      } catch (TypeConversionException e) {
        throw new RuntimeException("Error converting value", e);
      }
    }
  }


  public <T> T getOrDefault(final ConfigParam configParam) {
    final String stringValue= this.getStringOrDefault(configParam);
    try {
      return this.convertValue(stringValue, configParam);
    } catch (TypeConversionException e) {
      throw new RuntimeException("Error converting value", e);
    }
  }


  /**
   *
   * @param <T>
   * @param configParam
   * @return
   * @deprecated A parameter that is optional _and_ has a default value does not make sense.
   *             The optional will _never_ be null as it will at least contain the default value.
   */
  @Deprecated
  public <T> Optional<T> getOptionalOrDefault(final ConfigParam configParam) {
    final String stringValue= this.getStringOrDefault(configParam);
    try {
      return Optional.of(this.convertValue(stringValue, configParam));
    } catch (TypeConversionException e) {
      throw new RuntimeException("Error converting value", e);
    }
  }


  protected String getString(final ConfigParam configParam) {
    return this.props.get(configParam.key());
  }


  protected String getStringOrDefault(final ConfigParam configParam) {
    return this.props.getOrDefault(configParam.key(), configParam.defaultValue());
  }


//  protected String getStringOrDefault(final ConfigParam configParam, final String defaultValue) {
//    return (String) this.props.getOrDefault(configParam.key(), defaultValue);
//  }


  protected Optional<String> getOptionalString(final ConfigParam configParam) {
    final String value= this.props.get(configParam.key());
    return Optional.ofNullable(value);
  }


  protected int getInt(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    return Integer.parseInt(stringValue);
  }


  protected int getIntOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return Integer.parseInt(stringValue);
    } else {
      return Integer.parseInt(configParam.defaultValue());
    }
  }


  protected OptionalInt getOptionalInt(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      final int value= Integer.parseInt(stringValue);
      return OptionalInt.of(value);
    } else {
      return OptionalInt.empty();
    }
  }


  /**
   *
   * @param configParam
   * @return
   * @deprecated A parameter that is optional _and_ has a default value does not make sense.
   *             The optional will _never_ be null as it will at least contain the default value.
   */
  @Deprecated
  protected OptionalInt getOptionalIntOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return OptionalInt.of(Integer.parseInt(stringValue));
    } else {
      return OptionalInt.of(Integer.parseInt(configParam.defaultValue()));
    }
  }


  protected long getLong(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    return Long.parseLong(stringValue);
  }


  protected long getLongOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return Long.parseLong(stringValue);
    } else {
      return Long.parseLong(configParam.defaultValue());
    }
  }


  protected OptionalLong getOptionalLong(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      final long value= Long.parseLong(stringValue);
      return OptionalLong.of(value);
    } else {
      return OptionalLong.empty();
    }
  }


  /**
   *
   * @param configParam
   * @return
   * @deprecated A parameter that is optional _and_ has a default value does not make sense.
   *             The optional will _never_ be null as it will at least contain the default value.
   */
  @Deprecated
  protected OptionalLong getOptionalLongOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return OptionalLong.of(Long.parseLong(stringValue));
    } else {
      return OptionalLong.of(Long.parseLong(configParam.defaultValue()));
    }
  }


  protected double getDouble(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    return Double.parseDouble(stringValue);
  }


  protected double getDoubleOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return Double.parseDouble(stringValue);
    } else {
      return Double.parseDouble(configParam.defaultValue());
    }
  }


  protected OptionalDouble getOptionalDouble(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      final double value= Double.parseDouble(stringValue);
      return OptionalDouble.of(value);
    } else {
      return OptionalDouble.empty();
    }
  }


  /**
   *
   * @param configParam
   * @return
   * @deprecated A parameter that is optional _and_ has a default value does not make sense.
   *             The optional will _never_ be null as it will at least contain the default value.
   */
  @Deprecated
  protected OptionalDouble getOptionalDoubleOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return OptionalDouble.of(Double.parseDouble(stringValue));
    } else {
      return OptionalDouble.of(Double.parseDouble(configParam.defaultValue()));
    }
  }


  protected boolean getBoolean(final ConfigParam configParam) {
    final String value= this.props.get(configParam.key());
    if (value != null &&
      (value.trim().equals(1)
      || value.trim().equalsIgnoreCase("true")
      || value.trim().equalsIgnoreCase("yes"))) {
      return true;
    } else {
      // FIXME: Also define a set of valid "false" values?
      //        like: 0, "", null, no, false
      //        and throw ParsingException if anything other is contained
      return false;
    }
  }


  protected boolean getBooleanOrDefault(final ConfigParam configParam) {
    final String value= this.props.getOrDefault(configParam.key(), configParam.defaultValue());
    if (value != null &&
      (value.trim().equals(1)
      || value.trim().equalsIgnoreCase("true")
      || value.trim().equalsIgnoreCase("yes"))) {
      return true;
    } else {
      // FIXME: Also define a set of valid "false" values?
      //        like: 0, "", null, no, false
      //        and throw ParsingException if anything other is contained
      return false;
    }
  }


  private <T> T convertValue(final String stringValue, final ConfigParam configParam) throws TypeConversionException {
    final Converter<?> converter= converters.get(configParam.type());
    if (converter == null) {
      throw new TypeConversionException("No converter registered for type '" + configParam.type() + "'.");
    }

    return (T) converter.convert(stringValue);
  }


  private void convertPrimitive(final String stringValue, final ConfigParam param) throws TypeConversionException {
    if (param.type().equals(int.class)) {
      try {
        Integer.parseInt(stringValue);
      } catch (NumberFormatException ex) {
        throw new TypeConversionException("Error converting value to int", ex);
      }
    } else if (param.type().equals(long.class)) {
      try {
        Long.parseLong(stringValue);
      } catch (NumberFormatException ex) {
        throw new TypeConversionException("Error converting value to long", ex);
      }
    } else if (param.type().equals(double.class)) {
      try {
        Double.parseDouble(stringValue);
      } catch (NumberFormatException ex) {
        throw new TypeConversionException("Error converting value to double", ex);
      }
    } else if (param.type().equals(boolean.class)) {
      // boolean values are always valid at the moment
    }
  }


  private boolean isPrimitive(ConfigParam param) {
    return !param.type().getName().contains(".");
  }


  @Override
  public String toString() {
    final String[][] paramStrings= new String[this.params.length][3];
    int maxKeyLength=  0;
    int maxTypeLength= 0;

    for (int i= 0; i < this.params.length; i++) {
      final String keyString   = this.params[i].key();
      final String typeString  = "[" + (this.params[i].mandatory() ? "" : "?") + this.params[i].type().getName() + "]";
      final String value       = this.props.get(params[i].key());
      final String valueString = value != null ? value : params[i].defaultValue();
      final String defaultMarker = this.params[i].mandatory() && value == null ? " (default)" : "";
      paramStrings[i][0] = keyString;
      maxKeyLength       = Math.max(maxKeyLength, keyString.length());
      paramStrings[i][1] = typeString;
      maxTypeLength      = Math.max(maxTypeLength, typeString.length());
      paramStrings[i][2] = valueString + defaultMarker;
    }

    //final StringBuilder sb= new StringBuilder(this.getClass().getName());
    final StringBuilder sb= new StringBuilder();

    sb.append("{\n");
    for (int i= 0; i< paramStrings.length; i++) {
      sb.append(String.format("  %-"+maxKeyLength+"s %-"+maxTypeLength+"s: %s\n", paramStrings[i]));
    }

    // Include the embedded configs (and indent them a bit)
    for (final Map.Entry<String, CoatConfig> entry : this.embeddedConfigs.entrySet()) {
      final String prefix = entry.getKey();
      sb.append("  ").append(prefix);
      final CoatConfig embeddedConfig = entry.getValue();
      embeddedConfig.toString().lines()
        .map(l -> "  " + l + "\n")
        .collect(() -> sb, StringBuilder::append, StringBuilder::append);
    }

    sb.append("}");

    return sb.toString();
  }



  public static void registerConverter(final Class<?> type, final Converter<?> converter) {
    converters.put(type, converter);
  }


  protected static Map<String, String> filterByAndStripPrefix(final Map<String, String> map, final String prefix) {
    final Map<String, String> filtered= new HashMap<>();

    for (final Map.Entry<String, String> e : map.entrySet()) {
      final String key = e.getKey();
      final String val = e.getValue();

      if (key.startsWith(prefix)) {
        final String strippedKey= key.substring(prefix.length());
        filtered.put(strippedKey, val);
      }
    }

    return filtered;
  }


  protected boolean hasPrefix(final Map<String, String> props, final String prefix) {
    for (final String key : props.keySet()) {
      if (key.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }


  protected static Map<String, String> toMap(final Properties jup) {
    final Map<String, String> map= new HashMap<>(jup.size());

    for (final Map.Entry<Object, Object> entry : jup.entrySet()) {
      if (entry.getKey() instanceof String && entry.getValue() instanceof String ) {
        map.put((String) entry.getKey(), (String) entry.getValue());
      } else {
        LOGGER.log(WARNING, "Ignoring non-string entry: {0} = {1}", new Object[]{entry.getKey(), entry.getValue()});
      }
    }

    return map;
  }


  protected static Map<String, String> toMap(final File file) throws IOException {
    final Properties jup= new Properties();
    jup.load(new FileReader(file, UTF_8));
    return toMap(jup);
  }


  protected void registerEmbeddedConfig(final String keyPrefix, final CoatConfig embeddedConfig) {
    this.embeddedConfigs.put(keyPrefix, embeddedConfig);
  }
}
