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
package de.poiu.coat.validation;

import java.util.Optional;
import org.immutables.value.Value;


/**
 * ValidationFailure encapsulates a validation failure for a single config value.
 * <p>
 * The information about the failure is a human readable String that can be accessed via
 * {@link #toString()}.
 */
@Value.Immutable
@Value.Style(stagedBuilder = true)
public abstract class ValidationFailure {

  public static enum Type {
    MISSING_MANDATORY_VALUE ("Mandatory value for \"${key}\" is missing."),
    UNPARSABLE_VALUE        ("Config value for \"${key}\" cannot be converted to type \"${type}\": \"${value}\""),
    ;

    private final String formatString;

    private Type(final String formatString) {
      this.formatString= formatString;
    }
  }

  public abstract Type             failureType();
  public abstract String           key();
  public abstract Optional<String> type();
  public abstract Optional<String> value();


  @Override
  public String toString() {
    return this.formattedMessage();
  }


  public String formattedMessage() {
    return this.failureType().formatString
      .replace("${key}",   this.key())
      .replace("${value}", this.value().orElse("???"))
      .replace("${type}",  this.type().orElse("???"))
      ;
  }
}
