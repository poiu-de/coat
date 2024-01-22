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

import de.poiu.coat.casing.SnakeCaseConverter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class SnakeCaseConverterTest {


  @Test
  public void testConvert_allLowercase() {
    assertThat(SnakeCaseConverter.convert("alllowercase")).isEqualTo("alllowercase");
  }


  @Test
  public void testConvert_alreadySnakeCase() {
    assertThat(SnakeCaseConverter.convert("already_snake_case")).isEqualTo("already_snake_case");
  }


  @Test
  public void testConvert_camelCase() {
    assertThat(SnakeCaseConverter.convert("camelCaseString")).isEqualTo("camel_case_string");
  }


  @Test
  public void testConvert_multipleSubsequentUppercaseChars() {
    assertThat(SnakeCaseConverter.convert("inAHurry")).isEqualTo("in_a_hurry");
  }

}
