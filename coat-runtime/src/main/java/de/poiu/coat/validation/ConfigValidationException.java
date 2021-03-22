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


/**
 * An exception that is thrown if a parsed CoatConfig cannot be validated.
 * <p>
 * Possible error cases are
 * <ul>
 *   <li>missing mandatory config values</li>
 *   <li>config values that cannot be converted to their specified type</li>
 * </ul>
 *
 * This exception will always have an instance of {@link ValidationResult} that can be accessed via
 * {@link #getValidationResult()} to get more details of the actual validation failures.
 */
public class ConfigValidationException extends Exception {

  private final ValidationResult validationResult;


  public ConfigValidationException(final ValidationResult validationResult) {
    super(validationResult.toString());
    this.validationResult= validationResult;
  }


  public ValidationResult getValidationResult() {
    return validationResult;
  }


}
