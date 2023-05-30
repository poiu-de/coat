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
package de.poiu.coat.processor.casing;


class KebapCaseConverter {

  public static String convert(final String key) {
    final StringBuilder sb= new StringBuilder();

    for (final char c : key.toCharArray()) {
      if (Character.isUpperCase(c)) {
        sb.append("-");
        sb.append(Character.toLowerCase(c));
      } else {
        sb.append(c);
      }
    }

    return sb.toString();
  }

}
