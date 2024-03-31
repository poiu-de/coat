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
package de.poiu.coat.convert.converters;

import de.poiu.coat.convert.TypeConversionException;


/**
 * Converts an input String into a String.
 * <p>
 * The only actual conversion is returning <code>null</code> for a non-null, but blank String.
 * <p>
 * This converter will never throw a {@code TypeConversionException} as converting a String into a
 * String will always succeed.
 *
 */
public class StringConverter implements Converter<String> {

  @Override
  public String convert(final String s) throws TypeConversionException {
    if (s == null || s.isBlank()) {
      return null;
    }

    return s;
  }
}
