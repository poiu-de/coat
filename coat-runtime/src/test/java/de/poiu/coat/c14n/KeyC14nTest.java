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
package de.poiu.coat.c14n;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class KeyC14nTest {

  @Test
  public void testCanonicalize() {
    assertThat(KeyC14n.c14n("camelCaseString")).isEqualTo("CAMEL_CASE_STRING");
    assertThat(KeyC14n.c14n("kebab-case-string")).isEqualTo("KEBAB_CASE_STRING");
    assertThat(KeyC14n.c14n("snake_case_string")).isEqualTo("SNAKE_CASE_STRING");
    assertThat(KeyC14n.c14n("my.embeddedValue")).isEqualTo("MY_EMBEDDED_VALUE");
    assertThat(KeyC14n.c14n("totally.mixed-variantsOf_cases")).isEqualTo("TOTALLY_MIXED_VARIANTS_OF_CASES");
    assertThat(KeyC14n.c14n("inAHurry")).isEqualTo("IN_A_HURRY");
    assertThat(KeyC14n.c14n("mySAPSystem")).isEqualTo("MY_S_A_P_SYSTEM"); // be aware!
  }

}
