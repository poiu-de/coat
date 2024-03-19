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
package de.poiu.coat.convert;

import de.poiu.coat.convert.converters.Converter;
import java.util.Objects;


/**
 * An exception thrown by a {@link Converter} if the given input String cannot be converted
 * into the corresponding type.
 * <p>
 * This is an uncecked exception than can be used to wrap instances of {@link TypeConversionException}.
 *
 */
public class UncheckedTypeConversionException extends RuntimeException {

  public UncheckedTypeConversionException(String message, TypeConversionException cause) {
    super(message, Objects.requireNonNull(cause));
  }


  public UncheckedTypeConversionException(TypeConversionException cause) {
    super(Objects.requireNonNull(cause));
  }


  /**
   * Returns the cause of this exception.
   *
   * @return  the {@code TypeConversionException} which is the cause of this exception.
   */
  @Override
  public TypeConversionException getCause() {
    return (TypeConversionException) super.getCause();
  }
}
