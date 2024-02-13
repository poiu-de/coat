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

  /** The type of validation failure. */
  public static enum Type {
    /** A mandatory value was not found in the config file. */
    MISSING_MANDATORY_VALUE ("Mandatory value for \"${key}\" is missing."),
    /** A value could not be converted with the corresponding converter. */
    UNPARSABLE_VALUE        ("Config value for \"${key}\" cannot be converted to type \"${type}\": \"${value}\""),
    ;

    private final String formatString;

    private Type(final String formatString) {
      this.formatString= formatString;
    }
  }

  /** Returns the type of validation failure. */
  public abstract Type             failureType();
  /** Returns the name of the failed key (as specified in the config file). */
  public abstract String           key();
  /** Returns the type of the failed value. */
  public abstract Optional<String> type();
  /** Returns the failed value. */
  public abstract Optional<String> value();
  /** Returns a descriptive error message */
  public abstract Optional<String> errorMsg();


  /**
   * Returns a formatted string representation of this ValidationFailure.
   * This actually returns the same value as {@link #formattedMessage()}.
   */
  @Override
  public String toString() {
    return this.formattedMessage();
  }


  /**
   * Returns a formatted string representation of this ValidationFailure.
   */
  public String formattedMessage() {
    final String formattedMessage= this.failureType().formatString
      .replace("${key}",   this.key())
      .replace("${value}", this.value().orElse("???"))
      .replace("${type}",  this.type().orElse("???"))
      ;

    if (this.errorMsg().isEmpty() || this.errorMsg().get().isBlank()) {
      return formattedMessage;
    } else {
      return formattedMessage + ": " + this.errorMsg().get();
    }
  }
}
