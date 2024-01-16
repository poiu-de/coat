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
package de.poiu.coat.c14n;


public class KeyC14n {
  public static String c14n(final String s) {
    final StringBuilder sb= new StringBuilder();

    for (int i=0; i < s.length(); i++) {
      final char c= s.charAt(i);

      if (c == '_' || c == '-' || c == '.') {
        sb.append('_');
      } else if (Character.isUpperCase(c)) {
        sb.append('_');
        sb.append(c);
      } else {
        sb.append(Character.toUpperCase(c));
      }
    }

    return sb.toString();
  }
}
