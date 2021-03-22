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

import java.util.LinkedHashSet;
import java.util.Set;


/**
 * The result of calling {@link de.poiu.coat.CoatConfig#validate()} in case validation fails.
 * <p>
 * The thrown exception will always contain an instance of this class.
 * <p>
 * A ValidationResult comprises a set of {@link ValidationFailure}s with a human readable
 * representation of the actual failures.
 *
 */
public class ValidationResult {

  /** The validation failures */
  private final Set<ValidationFailure> validationFailures= new LinkedHashSet<>();


  /**
   * Adds a validation failure to this validation result.
   *
   * @param f the failure to add
   */
  public void addValidationFailure(final ValidationFailure f) {
    this.validationFailures.add(f);
  }


  /**
   * Whether any validation failures occurred.
   *
   * @return whether any validation failures occurred
   */
  public boolean hasFailures() {
    return !this.validationFailures.isEmpty();
  }


  /**
   * Returns the validation failures.
   *
   * @return the validation failures
   */
  public Set<ValidationFailure> getValidationFailures() {
    return new LinkedHashSet<>(validationFailures);
  }


  /**
   * Prints a human readable presentation of this ValidationResult.
   * <p>
   * If validation failures occurred, these will be printed out in nicely formatted fashion,
   * one failure per line.
   *
   * @return a human readable presentation of this ValidationResult
   */
  @Override
  public String toString() {
    if (this.validationFailures.isEmpty()) {
      return "Validation Success";
    } else {
      final StringBuilder sb= new StringBuilder("Validation Failed\n");
      for (final ValidationFailure f : this.validationFailures) {
        sb.append("  - ").append(f).append("\n");
      }
      sb.delete(sb.length()-1, sb.length());
      return sb.toString();
    }
  }
}
