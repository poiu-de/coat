/*
 * Copyright (C) 2020 - 2023 The Coat Authors
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

import java.util.regex.Pattern;


/**
 * A ListParser that recognizes whitespace characters as word delimiters. Whitespace characters
 * inside words are allowed if they are escaped by a backslash. To enter a literal backslash, escape
 * it as well. All other backslashes are silently dropped.
 */
public class CommaSeparatedListParser implements ListParser {

  private static Pattern PATTERN_DELIMITER= Pattern.compile(""
    + "(?<!\\\\)"    // not preceded by a backslash
    + "\\s*,\\s*"    // comma, optionally surrounded by arbitrary whitespace
    , Pattern.UNICODE_CHARACTER_CLASS);


  @Override
  public String[] convert(final String s) throws TypeConversionException {
    if (s == null || s.trim().length() == 0) {
      return new String[]{};
    }

    final String[] result = s.trim().split(PATTERN_DELIMITER.pattern());
    // remove all backslashes before commas
    for (int i= 0; i < result.length; i++) {
      result[i]= result[i].replace("\\,", ",");
    }

    return result;
  }
}
