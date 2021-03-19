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

import java.time.DateTimeException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;


/**
 *
 */
public class DurationConverter implements Converter<Duration> {
  public Duration convert(final String s) throws TypeConversionException {
    if (s == null || s.isBlank()) {
      return null;
    }

    try {
      if (s.endsWith("ns")) {
        final long amount= Long.valueOf(s.substring(0, s.length() - "ns".length()));
        return Duration.of(amount, ChronoUnit.NANOS);
      } else if (s.endsWith("ms")) {
        final long amount= Long.valueOf(s.substring(0, s.length() - "ms".length()));
        return Duration.of(amount, ChronoUnit.MILLIS);
      } else if (s.endsWith("s")) {
        final long amount= Long.valueOf(s.substring(0, s.length() - "s".length()));
        return Duration.of(amount, ChronoUnit.SECONDS);
      } else if (s.endsWith("m")) {
        final long amount= Long.valueOf(s.substring(0, s.length() - "m".length()));
        return Duration.of(amount, ChronoUnit.MINUTES);
      } else if (s.endsWith("h")) {
        final long amount= Long.valueOf(s.substring(0, s.length() - "h".length()));
        return Duration.of(amount, ChronoUnit.HOURS);
      } else if (s.endsWith("d")) {
        final long amount= Long.valueOf(s.substring(0, s.length() - "d".length()));
        return Duration.of(amount, ChronoUnit.DAYS);
      } else {
        final long amount= Long.valueOf(s);
        return Duration.of(amount, ChronoUnit.MILLIS);
      }
    } catch (final ArithmeticException | DateTimeException ex) {
      throw new TypeConversionException(s, Duration.class, ex);
    }
  }
}