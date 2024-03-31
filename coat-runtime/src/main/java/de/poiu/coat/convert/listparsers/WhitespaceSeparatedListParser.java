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
package de.poiu.coat.convert.listparsers;

import de.poiu.coat.convert.TypeConversionException;
import java.util.ArrayList;
import java.util.List;


/**
 * A ListParser that recognizes whitespace characters as word delimiters. Whitespace characters
 * inside words are allowed if they are escaped by a backslash. To enter a literal backslash, escape
 * it as well. All other backslashes are silently dropped.
 */
public class WhitespaceSeparatedListParser implements ListParser {

  @Override
  public String[] convert(final String s) throws TypeConversionException {
    if (s == null || s.length() == 0) {
      return new String[]{};
    }

    final List<String> result= new ArrayList<>();

    final StringBuilder sbCurrentString= new StringBuilder();

    for (int i=0; i < s.length(); i++) {
      final char c= s.charAt(i);
      if (c == '\\') {
        if (i == s.length()-1) {
          // if the backslash was the last character, it is part of the string
          sbCurrentString.append(c);
        } else {
          // otherwise the backslash is ignored
          final char nextChar= s.charAt(i+1);
          sbCurrentString.append(nextChar);
          i++;
        }
      } else if (Character.isWhitespace(c)) {
        // non-escaped whitespace is a word delimiter
        // but consecutive whitespace is ignored
        if (sbCurrentString.length() ==0) {
          continue;
        } else {
          result.add(sbCurrentString.toString());
          sbCurrentString.delete(0, sbCurrentString.length());
        }
      } else {
        // normal chars are just part of the string
        sbCurrentString.append(c);
      }
    }

    if (sbCurrentString.length() > 0) {
      result.add(sbCurrentString.toString());
    }

    return result.toArray(String[]::new);
  }
}
