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
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.logging.Level.WARNING;


/**
 *
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


  //////////////////////////////////////////////////////////////////////////////
  //
  // Constructors

  protected CoatConfig(final File file, final ConfigParam[] params) throws IOException {
    final Properties jup= new Properties();
    jup.load(new FileReader(file, UTF_8));
    for (final Map.Entry<Object, Object> entry : jup.entrySet()) {
      if (entry.getKey() instanceof String && entry.getValue() instanceof String ) {
        this.props.put((String) entry.getKey(), (String) entry.getValue());
      } else {
        LOGGER.log(WARNING, "Ignoring non-string entry: {0} = {1}", new Object[]{entry.getKey(), entry.getValue()});
      }
    }
    this.params= params;

    // FIXME: By using java.util.Properties, the properties are unsorted. We would have to either
    //        use apron or parse them here again (which would be too much of a hassle)

    // TODO: Differntiate between ignoring unexpected values, logging them or throwing an exception
    //       Better do this only at validation time

  }

  protected CoatConfig(final Properties jup, final ConfigParam[] params) {
    for (final Map.Entry<Object, Object> entry : jup.entrySet()) {
      if (entry.getKey() instanceof String && entry.getValue() instanceof String ) {
        this.props.put((String) entry.getKey(), (String) entry.getValue());
      } else {
        LOGGER.log(WARNING, "Ignoring non-string entry: {0} = {1}", new Object[]{entry.getKey(), entry.getValue()});
      }
    }
    this.params= params;
  }


  protected CoatConfig(final Map<String, String> props, final ConfigParam[] params) {
    this.props.putAll(props);
    this.params= params;
  }


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  public void validate() throws ConfigValidationException {
    final ValidationResult result= new ValidationResult();

    for (final ConfigParam param : this.params) {
      if (param.mandatory()
        && param.defaultValue() == null
        && this.getString(param) == null) {
        final ValidationFailure f= new ValidationFailure("Mandatory value for \"" + param.key() + "\" is missing.");
        result.addValidationFailure(f);
      }
    }

//    for (final ConfigParam param : this.params) {
//      final String stringValue= this.getString(param);
//      try {
//        this.convertValue(stringValue, param);
//      } catch (TypeConversionException ex) {
//        final ValidationFailure f= new ValidationFailure("Value \"" + stringValue + "\" for \"" + param.key() + "\" is invalid for type \"" + param.type().getName() + "\".");
//        result.addValidationFailure(f);
//      }
//    }

    if (result.hasFailures()) {
      throw new ConfigValidationException(result);
    }

    // TODO: Should this method throw a ValidationException (which contains validation details)
    //       or should it return a ValidationResult object?
    //       Or should we provide both methods?

  }


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


  /**
   *
   * @param configParam
   * @return
   * @deprecated A parameter that is optional _and_ has a default value does not make sense.
   *             The optional will _never_ be null as it will at least contain the default value.
   */
  @Deprecated
  protected Optional<String> getOptionalStringOrDefault(final ConfigParam configParam) {
    return Optional.of(this.props.getOrDefault(configParam.key(), configParam.defaultValue()));
  }


  protected int getInt(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    return Integer.parseInt(stringValue);
  }

//
//  /**
//   *
//   * @param <T>
//   * @param configParam
//   * @param defaultValue
//   * @return
//   * @deprecated Warum den defaultValue mitgeben? Der ist doch im ConfigParam enthalten.
//   */
//  @Deprecated
//  public <T> T getOrDefault(final ConfigParam configParam) {
//    final String stringValue= this.getStringOrDefault(configParam, defaultValue);
//    try {
//      return this.convertValue(stringValue, configParam);
//    } catch (TypeConversionException e) {
//      throw new RuntimeException("Error converting value", e);
//    }
//  }


//  protected int getIntOrDefault(final ConfigParam configParam, final String defaultValue) {
//    final String stringValue= this.props.get(configParam.key());
//    if (stringValue != null && !stringValue.trim().isEmpty()) {
//      return Integer.parseInt(stringValue);
//    } else {
//      return Integer.parseInt(defaultValue);
//    }
//  }


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

//  protected boolean getBooleanOrDefault(final ConfigParam configParam, final String defaultValue) {
//    final String value= this.props.getOrDefault(configParam.key(), defaultValue);
//    if (value != null &&
//      (value.trim().equals(1)
//      || value.trim().equalsIgnoreCase("true")
//      || value.trim().equalsIgnoreCase("yes"))) {
//      return true;
//    } else {
//      // FIXME: Also define a set of valid "false" values?
//      //        like: 0, "", null, no, false
//      //        and throw ParsingException if anything other is contained
//      return false;
//    }
//  }


  private <T> T convertValue(final String stringValue, final ConfigParam configParam) throws TypeConversionException {
    final Converter<?> converter= converters.get(configParam.type());
    if (converter == null) {
      throw new TypeConversionException("No converter registered for type '" + configParam.type() + "'.");
    }

    return (T) converter.convert(stringValue);
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
      final String valueString = value != null ? value : params[i].defaultValue() + " (default)";
      paramStrings[i][0] = keyString;
      maxKeyLength       = Math.max(maxKeyLength, keyString.length());
      paramStrings[i][1] = typeString;
      maxTypeLength      = Math.max(maxTypeLength, typeString.length());
      paramStrings[i][2] = valueString;
    }

    final StringBuilder sb= new StringBuilder(this.getClass().getName());
    sb.append(" {\n");
    for (int i= 0; i< paramStrings.length; i++) {
      sb.append(String.format("  %-"+maxKeyLength+"s %-"+maxTypeLength+"s: %s\n", paramStrings[i]));
    }
    sb.append("}");

    return sb.toString();
  }



  public static void registerConverter(final Class<?> type, final Converter<?> converter) {
    converters.put(type, converter);
  }
}
