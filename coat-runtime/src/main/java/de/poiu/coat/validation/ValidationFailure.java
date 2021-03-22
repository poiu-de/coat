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

import java.util.Objects;


/**
 * ValidationFailure encapsulates a validation failure for a single config value.
 * <p>
 * The information about the failure is a human readable String that can be accessed via
 * {@link #toString()}.
 */
public class ValidationFailure {

  private final String details;


  public ValidationFailure(final String details) {
    this.details= details;
  }


  @Override
  public String toString() {
    return this.details;
  }


  @Override
  public int hashCode() {
    int hash = 3;
    hash = 53 * hash + Objects.hashCode(this.details);
    return hash;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ValidationFailure other = (ValidationFailure) obj;
    if (!Objects.equals(this.details, other.details)) {
      return false;
    }
    return true;
  }
}
