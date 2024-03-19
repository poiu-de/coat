/*
 * Copyright (C) 2024 The Coat Authors
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
package de.poiu.coat;


/**
 * Data holder for config entries containing the key, type and value of an entry.
 * Its use is mainly for generating a nice formatted string representation.
 *
 * This class is mutable. All fields are publically accessible.
 * Therefore this class is not thread-safe,
 */
class StringRepresentation {
  public CharSequence key;
  public CharSequence type;
  public CharSequence value;

  public StringRepresentation(final CharSequence key, final CharSequence type, final CharSequence value) {
    this.key   = key;
    this.type  = type;
    this.value = value;
  }
}
