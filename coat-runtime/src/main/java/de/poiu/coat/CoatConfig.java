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

import de.poiu.coat.convert.BooleanConverter;
import de.poiu.coat.convert.CharsetConverter;
import de.poiu.coat.convert.Converter;
import de.poiu.coat.convert.DoubleConverter;
import de.poiu.coat.convert.DurationConverter;
import de.poiu.coat.convert.FileConverter;
import de.poiu.coat.convert.FloatConverter;
import de.poiu.coat.convert.InetAddressConverter;
import de.poiu.coat.convert.IntegerConverter;
import de.poiu.coat.convert.ListParser;
import de.poiu.coat.convert.LocalDateConverter;
import de.poiu.coat.convert.LocalDateTimeConverter;
import de.poiu.coat.convert.LocalTimeConverter;
import de.poiu.coat.convert.LongConverter;
import de.poiu.coat.convert.MessageDigestConverter;
import de.poiu.coat.convert.PathConverter;
import de.poiu.coat.convert.StringConverter;
import de.poiu.coat.convert.TypeConversionException;
import de.poiu.coat.convert.UncheckedTypeConversionException;
import de.poiu.coat.convert.WhitespaceSeparatedListParser;
import de.poiu.coat.validation.ConfigValidationException;
import de.poiu.coat.validation.ImmutableValidationFailure;
import de.poiu.coat.validation.ImmutableValidationResult;
import de.poiu.coat.validation.ValidationFailure;
import de.poiu.coat.validation.ValidationResult;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static de.poiu.coat.validation.ValidationFailure.Type.MISSING_MANDATORY_VALUE;
import static de.poiu.coat.validation.ValidationFailure.Type.UNPARSABLE_VALUE;
import static java.lang.System.Logger.Level.WARNING;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Base class for all generated config classes that are generated by Coat.
 * <p>
 * This class provides all the gory defails of calling different converters for the corresponding
 * types and whether to return a default value or not.
 */
public abstract class CoatConfig {

  private static System.Logger LOGGER= System.getLogger(CoatConfig.class.getName());

  private static final Map<Class<?>, Converter<?>> converters= new ConcurrentHashMap<>();

  static {
    converters.put(Boolean.class,       new BooleanConverter());
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
    converters.put(Integer.class,       new IntegerConverter());
    converters.put(Long.class,          new LongConverter());
    converters.put(Float.class,         new FloatConverter());
    converters.put(Double.class,        new DoubleConverter());
  }

  private static ListParser listParser= new WhitespaceSeparatedListParser();


  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private final Map<String, String>         props=            new HashMap<>();

  private final ConfigParam[]               params;

  private final List<EmbeddedConfig>        embeddedConfigs=  new ArrayList<>();

  private final Map<Class<?>, Converter<?>> customConverters= new ConcurrentHashMap<>();


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

    // TODO: Differentiate between ignoring unexpected values, logging them or throwing an exception
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
        } else if (this.isCollection(param)) {
          final String[] values= this.splitArray(stringValue);
          for (final String value : values) {
            this.convertValue(value, param);
          }
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
    this.embeddedConfigs.stream()
      .filter(Objects::nonNull)
      .forEachOrdered(e -> {
        try {
          if (e.embeddedConfig().isPresent()) {
            e.embeddedConfig().get().validate();
          }
        } catch (ConfigValidationException ex) {
          for (final ValidationFailure f : ex.getValidationResult().validationFailures()) {
            resultBuilder.addValidationFailure(
              ImmutableValidationFailure.copyOf(f)
                .withKey(e.prefix() + f.key())
            );
          }
        }
      });

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


  private ListParser getListParser(final ConfigParam configParam) throws TypeConversionException {
    // First try the ListParser that was explicitly configured for this field
    final Class<? extends ListParser> paramListParserClass = configParam.listParser();
    if (paramListParserClass != null) {
      try {
        return paramListParserClass.getConstructor().newInstance();
      } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
        throw new TypeConversionException("Error instantiating “" + paramListParserClass.getCanonicalName() + "”.", ex);
      }
    }
    // Then try the ListParser registered for this CoatConfig (or the default one)
    return this.listParser;
  }

  public <T> T[] getArrayOrDefault(final ConfigParam configParam) throws UncheckedTypeConversionException {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return getArray(configParam);
    } else {
      try {
        final ListParser listParser = this.getListParser(configParam);
        final String[] stringValues = listParser.convert(configParam.defaultValue());
        return getArray(configParam, stringValues);
      } catch (TypeConversionException ex) {
        throw new UncheckedTypeConversionException("Error splitting value " + stringValue + " into a list", ex);
      }
    }
  }


  public <T> T[] getArray(final ConfigParam configParam) throws UncheckedTypeConversionException {
    final String stringValue= this.getString(configParam);
    try {
        final ListParser listParser = this.getListParser(configParam);
      final String[] stringValues = listParser.convert(stringValue);
      return getArray(configParam, stringValues);
    } catch (TypeConversionException ex) {
      throw new UncheckedTypeConversionException("Error splitting value " + stringValue + " into a list", ex);
    }
  }


  private <T> T[] getArray(final ConfigParam configParam, final String[] stringValues) throws UncheckedTypeConversionException {
    final T[] array= (T[]) Array.newInstance(configParam.type(), stringValues.length);
    for (int i= 0; i<stringValues.length; i++) {
      try {
        array[i]= this.convertValue(stringValues[i], configParam);
      } catch (TypeConversionException e) {
        throw new UncheckedTypeConversionException("Error converting value " + stringValues[i] + " to type " + configParam.type().getName(), e);
      }
    }

    return array;
  }


  private String[] splitArray(final String stringValue) throws TypeConversionException {
    return listParser.convert(stringValue);
  }


  public <T> List<T> getList(final ConfigParam configParam) throws UncheckedTypeConversionException {
    return List.of(getArray(configParam));
  }


  public <T> List<T> getListOrDefault(final ConfigParam configParam) throws UncheckedTypeConversionException {
    return List.of(getArrayOrDefault(configParam));
  }


  public <T> Set<T> getSet(final ConfigParam configParam) throws UncheckedTypeConversionException {
    return Set.of(getArray(configParam));
  }


  public <T> Set<T> getSetOrDefault(final ConfigParam configParam) throws UncheckedTypeConversionException {
    return Set.of(getArrayOrDefault(configParam));
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
    return this.parseInt(stringValue);
  }


  protected int getIntOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return this.parseInt(stringValue);
    } else {
      return this.parseInt(configParam.defaultValue());
    }
  }


  protected OptionalInt getOptionalInt(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      final int value= this.parseInt(stringValue);
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
      return OptionalInt.of(this.parseInt(stringValue));
    } else {
      return OptionalInt.of(this.parseInt(configParam.defaultValue()));
    }
  }


  protected long getLong(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    return this.parseLong(stringValue);
  }


  protected long getLongOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return this.parseLong(stringValue);
    } else {
      return this.parseLong(configParam.defaultValue());
    }
  }


  protected OptionalLong getOptionalLong(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      final long value= this.parseLong(stringValue);
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
      return OptionalLong.of(this.parseLong(stringValue));
    } else {
      return OptionalLong.of(this.parseLong(configParam.defaultValue()));
    }
  }


  protected double getDouble(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    return this.parseDouble(stringValue);
  }


  protected double getDoubleOrDefault(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      return this.parseDouble(stringValue);
    } else {
      return this.parseDouble(configParam.defaultValue());
    }
  }


  protected OptionalDouble getOptionalDouble(final ConfigParam configParam) {
    final String stringValue= this.props.get(configParam.key());
    if (stringValue != null && !stringValue.trim().isEmpty()) {
      final double value= this.parseDouble(stringValue);
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
    Converter<?> converter= null;
    // First try the converter that was explicitly configured for this field
    final Class<? extends Converter<?>> paramConverterClass = configParam.converter();
    if (paramConverterClass != null) {
      try {
        converter = paramConverterClass.getConstructor().newInstance();
      } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
        throw new TypeConversionException("Error converting “" + stringValue + "” for type “" + configParam.type() + "” via explicit converter “" + paramConverterClass.getCanonicalName() + "”.", ex);
      }
    }
    // Then try the converter registered for this CoatConfig
    if (converter == null) {
      converter= this.customConverters.get(configParam.type());
    }
    // Finally try the default converter
    if (converter == null) {
      converter= converters.get(configParam.type());
    }

    if (converter == null) {
      throw new TypeConversionException("No converter registered for type '" + configParam.type() + "'.");
    }

    return (T) converter.convert(stringValue);
  }


  private void convertPrimitive(final String stringValue, final ConfigParam param) throws TypeConversionException {
    if (param.type().equals(int.class)) {
      try {
        this.parseInt(stringValue);
      } catch (NumberFormatException ex) {
        throw new TypeConversionException("Error converting value to int", ex);
      }
    } else if (param.type().equals(long.class)) {
      try {
        this.parseLong(stringValue);
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


  private boolean isPrimitive(final ConfigParam param) {
    return !param.type().getName().contains(".");
  }


  private boolean isCollection(final ConfigParam param) {
    return param.collectionType() != null;
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
      final String defaultMarker = this.params[i].mandatory() && params[i].defaultValue() != null && value == null? " (default)" : "";
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
      sb.append(String.format("  %-"+maxKeyLength+"s %-"+maxTypeLength+"s: %s\n", (Object)paramStrings[i]));
    }

    // Include the embedded configs (and indent them a bit)
    for (final EmbeddedConfig e : this.embeddedConfigs) {
      sb.append("  ").append(e.isOptional() ? "?": "").append(e.prefix());
      if (e.embeddedConfig().isPresent()) {
        e.embeddedConfig().get().toString().lines()
          .map(l -> "  " + l + "\n")
          .collect(() -> sb, StringBuilder::append, StringBuilder::append);
      } else {
        sb.append(": null\n");
      }
    }

    sb.append("}");

    return sb.toString();
  }


  public static void registerConverter(final Converter<?> converter) {
    try {
      final Class<?> type= getConverterBaseType(converter.getClass());
      converters.put(type, converter);
    } catch (TypeConversionException ex) {
      throw new UncheckedTypeConversionException("Error trying to find base class of converter “" + converter.getClass().getCanonicalName() + "”", ex);
    }
  }


  protected void registerCustomConverter(final Converter<?> converter) {
    try {
      final Class<?> type= this.getConverterBaseType(converter.getClass());
      this.customConverters.put(type, converter);
    } catch (TypeConversionException ex) {
      throw new UncheckedTypeConversionException("Error trying to find base class of converter “" + converter.getClass().getCanonicalName() + "”", ex);
    }
  }


  private static Class<?> getConverterBaseType(final Class<? extends Converter> converterClass) throws TypeConversionException {
    final List<Type> genericInterfaces = getGenericInterfacesRecursively(converterClass);

    final ParameterizedType converterType;
    try {
      converterType = getConverterType(genericInterfaces);
      if (converterType == null) {
        throw new RuntimeException("Could not find generic converter for “" + converterClass.getCanonicalName() + "”");
      }
    } catch (ClassNotFoundException ex) {
      throw new TypeConversionException("Error loading generic converter for “" + converterClass.getCanonicalName() + "”");
    }

    final Type[] actualTypeArguments = converterType.getActualTypeArguments();
    if (actualTypeArguments.length < 1) {
      throw new RuntimeException("No type arguments for converter “" + converterType.getTypeName() + "”.");
    } else if (actualTypeArguments.length > 1) {
      throw new RuntimeException("More than 1 type argument for converter “" + converterType.getTypeName() + "”.");
    }

    final String typeName = actualTypeArguments[0].getTypeName();
    try {
      return Class.forName(typeName);
    } catch (ClassNotFoundException ex) {
      throw new TypeConversionException("Error loading class “" + typeName + "”", ex);
    }
  }


  private static List<Type> getGenericInterfacesRecursively(final Class converterClass) {
    final List<Type> genericInterfaces= new ArrayList<>();
    genericInterfaces.addAll(List.of(converterClass.getGenericInterfaces()));

    final Class<?> superclass = converterClass.getSuperclass();
    if (superclass != null) {
      genericInterfaces.addAll(getGenericInterfacesRecursively((Class)superclass));
    }

    return genericInterfaces;
  }


  private static ParameterizedType getConverterType(final List<Type> genericInterfaces) throws ClassNotFoundException {
    for (final Type interf : genericInterfaces) {
      if (interf instanceof ParameterizedType) {
        final ParameterizedType pType= (ParameterizedType) interf;
        if (pType.getRawType() != null) {
          final Class<?> rawType = Class.forName(pType.getRawType().getTypeName());
          if (rawType.isAssignableFrom(Converter.class)) {
            return pType;
          }
        }
      }
    }
    return null;
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


  public static void registerListParser(final ListParser p) {
    listParser= p;
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


  protected void registerEmbeddedConfig(final String keyPrefix, final CoatConfig embeddedConfig, final boolean optional) {
    this.embeddedConfigs.add(ImmutableEmbeddedConfig.builder()
      .prefix(keyPrefix)
      .embeddedConfig(embeddedConfig)
      .isOptional(optional)
      .build()
    );
  }


  private int parseInt(final String stringValue) {
    final String number= removeOptionalUnderscores(stringValue);
    if (number.startsWith("0x")) {
      // hex value
      return Integer.parseInt(number.substring(2), 16);
    } else if (number.startsWith("0b")) {
      // bin value
      return Integer.parseInt(number.substring(2), 2);
    } else if (number.startsWith("0")) {
      // oct value
      return Integer.parseInt(number.substring(1), 8);
    } else {
      // dec value
      return Integer.parseInt(number);
    }
  }


  private long parseLong(final String stringValue) {
    final String number= removeOptionalUnderscores(stringValue);
    if (number.startsWith("0x")) {
      // hex value
      return Long.parseLong(number.substring(2), 16);
    } else if (number.startsWith("0b")) {
      // bin value
      return Long.parseLong(number.substring(2), 2);
    } else if (number.startsWith("0")) {
      // oct value
      return Long.parseLong(number.substring(1), 8);
    } else {
      // dec value
      return Long.parseLong(number);
    }
  }


  private double parseDouble(final String stringValue) {
    final String number= removeOptionalUnderscores(stringValue);
    return Double.parseDouble(number);
  }


  private String removeOptionalUnderscores(final String number) {
    if (number.startsWith("0x")) {
      return number.replaceAll("(?<=[a-f])_+(?=[a-f])", "");
    } else {
      return number.replaceAll("(?<=\\d)_+(?=\\d)", "");
    }
  }
}
