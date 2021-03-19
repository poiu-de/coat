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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 */
public class StringConverterTest {

  private final StringConverter c= new StringConverter();

  @Test
  public void testConvertSimpleString() throws Exception {
    assertThat(c.convert("some string")).isEqualTo("some string");
    assertThat(c.convert("some other string")).isEqualTo("some other string");
  }


  @Test
  public void testConvertNullOrBlank() throws Exception {
    assertThat(c.convert(null)).isEqualTo(null);
    assertThat(c.convert("")).isEqualTo(null);
    assertThat(c.convert("   ")).isEqualTo(null);
    assertThat(c.convert("  \t\n\r ")).isEqualTo(null);
  }

}
