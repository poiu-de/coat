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
package de.poiu.coat.convert;


/**
 * Base class for converters that convert Strings to concrete types.
 *
 */
public interface Converter<T> {

  /**
   * Convert the given String <code>s</code> into type <code>T</code>.
   *
   * If <code>s</code> is <code>null</code> or an empty, this method should usually return null.
   * <p>
   * If the input cannot be converted, a <code>TypeConversionException</code> must be thrown.
   *
   * @param s the String to convert (<code>null</code> is allowed)
   * @return the result of the conversion
   * @throws TypeConversionException if conversion failed for some reason
   */
  public T convert(final String s) throws TypeConversionException;
}
