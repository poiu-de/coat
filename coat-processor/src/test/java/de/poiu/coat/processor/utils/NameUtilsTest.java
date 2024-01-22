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
package de.poiu.coat.processor.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;



/**
 *
 * @author mherrn
 */
public class NameUtilsTest {



  @Test
  public void testUpperFirstChar() {
    assertThat(NameUtils.upperFirstChar("hello")).isEqualTo("Hello");
    assertThat(NameUtils.upperFirstChar("Hello")).isEqualTo("Hello");
    assertThat(NameUtils.upperFirstChar("HELLO")).isEqualTo("HELLO");
    assertThat(NameUtils.upperFirstChar("helloFriend")).isEqualTo("HelloFriend");
    assertThat(NameUtils.upperFirstChar("hello Friend")).isEqualTo("Hello Friend");
    assertThat(NameUtils.upperFirstChar("hello friend")).isEqualTo("Hello friend");
  }


  @Test
  public void testToConstName() {
    assertThat(NameUtils.toConstName("theString")).isEqualTo("THE_STRING");
    assertThat(NameUtils.toConstName("the_string")).isEqualTo("THE_STRING");
    assertThat(NameUtils.toConstName("string")).isEqualTo("STRING");
  }

}
