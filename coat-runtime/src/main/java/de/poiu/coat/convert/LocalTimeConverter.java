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

import java.time.LocalTime;
import java.time.format.DateTimeParseException;


/**
 * Converts an input String to to a {@link LocalTime}.
 * <p>
 * The same rules for the input string apply as for {@link LocalTime#parse(java.lang.CharSequence)}.
 * Therefore it must be given in ISO-8601 format such as 18:00:30.
 *
 */
public class LocalTimeConverter implements Converter<LocalTime> {

  public LocalTime convert(final String s) throws TypeConversionException {
    if (s == null || s.isBlank()) {
      return null;
    }

    try {
      return LocalTime.parse(s);
    } catch (final DateTimeParseException ex) {
      throw new TypeConversionException(s, LocalTime.class, ex);
    }
  }
}
