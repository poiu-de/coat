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

import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;


/**
 *
 */
public class PathConverter implements Converter<Path> {
  public Path convert(final String s) throws TypeConversionException {
    if (s == null || s.isBlank()) {
      return null;
    }

    try {
      return Path.of(s);
    } catch (final FileSystemNotFoundException | SecurityException | IllegalArgumentException ex) {
      throw new TypeConversionException(s, Path.class, ex);
    }
  }
}
