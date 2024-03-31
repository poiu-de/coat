/*
 * Copyright (C) 2020 - 2023 The Coat Authors
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
package de.poiu.coat.casing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class KebapCaseConverterTest {


  @Test
  public void testConvert_allLowercase() {
    assertThat(KebapCaseConverter.convert("alllowercase")).isEqualTo("alllowercase");
  }


  @Test
  public void testConvert_camelCase() {
    assertThat(KebapCaseConverter.convert("camelCaseString")).isEqualTo("camel-case-string");
  }


  @Test
  public void testConvert_multipleSubsequentUppercaseChars() {
    assertThat(KebapCaseConverter.convert("inAHurry")).isEqualTo("in-a-hurry");
  }


  @Test
  public void testConvert_dontTouchUnderscores() {
    assertThat(KebapCaseConverter.convert("don_tTouchUnderscores")).isEqualTo("don_t-touch-underscores");
  }

}
