/*
 * Copyright (C) 2020 - 2024 The Coat Authors
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

import de.poiu.coat.convert.converters.URIConverter;
import java.net.URI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class URIConverterTest {

  final URIConverter c= new URIConverter();

  @Test
  public void testConvert() throws Exception {
    final String uriString= "a:b#c";

    final URI uri = c.convert(uriString);

    assertThat(uri.toString()).isEqualTo(uriString);
    assertThat(uri.getScheme()).isEqualTo("a");
    assertThat(uri.getSchemeSpecificPart()).isEqualTo("b");
    assertThat(uri.getFragment()).isEqualTo("c");
  }
}
