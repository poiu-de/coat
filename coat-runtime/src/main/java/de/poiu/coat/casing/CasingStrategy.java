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
package de.poiu.coat.casing;


/**
 * A strategy to use when inferring keys from the accessor names.
 */
public enum CasingStrategy {
  /** A casing strategy that leaves Strings unchanged. */
  AS_IS {
    @Override
    public String convert(final String accessorName) {
      return accessorName;
    }
  },

  /** A casing strategy that converts Strings to snake_case. */
  SNAKE_CASE {
    @Override
    public String convert(final String accessorName) {
      return SnakeCaseConverter.convert(accessorName);
    }
  },

  /** A casing strategy that converts Strings to kebab-case. */
  KEBAP_CASE {
    @Override
    public String convert(final String accessorName) {
      return KebapCaseConverter.convert(accessorName);
    }
  },

  ;


  /**
   * Convert the given accessor name to the the expected key name.
   * @param accessorName the name of the accessor method
   * @return the key as it is expected from the config file
   */
  public abstract String convert(final String accessorName);
}
