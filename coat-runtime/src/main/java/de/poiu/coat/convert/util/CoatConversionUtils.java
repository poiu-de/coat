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
package de.poiu.coat.convert.util;

import de.poiu.coat.CoatConfigBuilder;
import de.poiu.coat.convert.BooleanConverter;
import de.poiu.coat.convert.CharsetConverter;
import de.poiu.coat.convert.Converter;
import de.poiu.coat.convert.DoubleConverter;
import de.poiu.coat.convert.DurationConverter;
import de.poiu.coat.convert.FileConverter;
import de.poiu.coat.convert.FloatConverter;
import de.poiu.coat.convert.InetAddressConverter;
import de.poiu.coat.convert.IntegerConverter;
import de.poiu.coat.listparse.ListParser;
import de.poiu.coat.convert.LocalDateConverter;
import de.poiu.coat.convert.LocalDateTimeConverter;
import de.poiu.coat.convert.LocalTimeConverter;
import de.poiu.coat.convert.LongConverter;
import de.poiu.coat.convert.PathConverter;
import de.poiu.coat.convert.StringConverter;
import de.poiu.coat.convert.TypeConversionException;
import de.poiu.coat.convert.UncheckedTypeConversionException;
import de.poiu.coat.convert.WhitespaceSeparatedListParser;
import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Helper class storing all the static converter logic. All these settings are global.
 * Class-local settings need to be set in the concrete {@link CoatConfigBuilder}.
 * <p>
 * This class is thread-safe and can be used in multi-threaded environments.
 */
public class CoatConversionUtils {

  private static final Map<Class<?>, Converter<?>> globalConverters= new ConcurrentHashMap<>();
  static {
    globalConverters.put(Boolean.class,       new BooleanConverter());
    globalConverters.put(String.class,        new StringConverter());
    globalConverters.put(Duration.class,      new DurationConverter());
    globalConverters.put(LocalDate.class,     new LocalDateConverter());
    globalConverters.put(LocalTime.class,     new LocalTimeConverter());
    globalConverters.put(LocalDateTime.class, new LocalDateTimeConverter());
    globalConverters.put(File.class,          new FileConverter());
    globalConverters.put(Path.class,          new PathConverter());
    globalConverters.put(Charset.class,       new CharsetConverter());
    globalConverters.put(InetAddress.class,   new InetAddressConverter());
    globalConverters.put(Integer.class,       new IntegerConverter());
    globalConverters.put(Long.class,          new LongConverter());
    globalConverters.put(Float.class,         new FloatConverter());
    globalConverters.put(Double.class,        new DoubleConverter());
  }

  private static final AtomicReference<ListParser> globalListParser= new AtomicReference<>(new WhitespaceSeparatedListParser());



  /**
   * Register a custom converter globally for all config classes.
   * @param converter The converter to register
   */
  public static void registerGlobalConverter(final Converter<?> converter) {
    try {
      final Class<?> type= getConverterBaseType(converter.getClass());
      globalConverters.put(type, converter);
    } catch (TypeConversionException ex) {
      throw new UncheckedTypeConversionException("Error trying to find base class of converter “" + converter.getClass().getCanonicalName() + "”", ex);
    }
  }


  /**
   * Register a custom list parser globally for all config classes.
   * @param listParser The list parser to register
   */
  public static void registerGlobalListParser(final ListParser listParser) {
    globalListParser.set(listParser);
  }


  public static Converter<?> getGlobalConverter(final Class<?> type) {
    return globalConverters.get(type);
  }


  public static ListParser getGlobalListParser() {
    return globalListParser.get();
  }


  public static Class<?> getConverterBaseType(final Class<? extends Converter> converterClass) throws TypeConversionException {
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
}
