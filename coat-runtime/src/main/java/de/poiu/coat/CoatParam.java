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
package de.poiu.coat;

import de.poiu.coat.convert.Converter;
import de.poiu.coat.convert.ListParser;


/**
 * Specification of a single accessor of a config class.
 */
public interface CoatParam {

  /** The key to expect in the config file. */
  public String  key();
  /** The type of this config value. */
  public Class<?>  type();
  /** The collection type of this value. May be null if this is not a collection. */
  public Class<?> collectionType();
  /** The default value to use if the corresponding value is not given in the config file. */
  public String  defaultValue();
  /** Whether this config value is mandatory. */
  public boolean mandatory();
  /** The converter to use for this config value. May be null to use the class-wide or global one. */
  public Class<? extends Converter<?>> converter();
  /** The list parser to use for this config value (if it is a collection). May be null to use the class-wide or global one. */
  public Class<? extends ListParser> listParser();
}
