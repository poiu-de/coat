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
 * A ListParser that considers commas (including any optional surrounding whitespace) as word
 * delimiters.
 * <p>
 * For supporting commas <i>inside</i> config values they need to be preceded by a backslash.
 * To enter a literal backslash, escape it as well. All other backslashes are silently dropped.
 * <p>
 * For example the value
 *   <code>Ceasar\, Iulius Gaius &lt;emperor@rome.it&gt;, Asterix &lt;asterix@gallia.fr&gt;</code>
 * will be split into <code>Ceasar\, Iulius Gaius &lt;emperor@rome.it&gt;</code> and
 * <code>Asterix &lt;asterix@gallia.fr&gt;</code>
 */
public class CommaSeparatedListParser implements ListParser {

  private final char separatorChar= ',';

  @Override
  public String[] convert(final String s) throws TypeConversionException {
    if (s == null || s.trim().length() == 0) {
      return new String[]{};
    }

    final List<String> result= new ArrayList<>();

    final StringBuilder sbCurrentString= new StringBuilder();

    for (int i=0; i < s.length(); i++) {
      final char c= s.charAt(i);
      if (c == '\\') {
        if (i < s.length()-1) {
          final char nextChar= s.charAt(i+1);
          if (nextChar == this.separatorChar || nextChar == '\\') {
            // it the next char is whitespace or another backslash it is escaped and part of the string
            sbCurrentString.append(nextChar);
            i++;
          } else {
            // otherwise the backslash is ignored
            sbCurrentString.append(nextChar);
            i++;
          }
        } else {
          // if the backslash was the last character, it is part of the string
          sbCurrentString.append(c);
        }
      } else if (c == this.separatorChar) {
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

    return result
      .stream()
      .map(String::trim)
      .toArray(String[]::new);
  }
}
