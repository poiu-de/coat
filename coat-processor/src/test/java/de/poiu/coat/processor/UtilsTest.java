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
package de.poiu.coat.processor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class UtilsTest {

  @Test
  public void testStripBlockTagsFromJavadoc() {
    final String javadoc = ""
      + " bla bla dumdidum\n"
      + " lalala oo jaja {@code somecode}\n"
      + " {@link Class#method()} blubb\n"
      + "\n"
      + " sometext\n"
      + "<p>\n"
      + " moretext\n"
      + "\n"
      + " @see OtherClass\n"
      + " @param p1\n"
      + " @param p2 with some description\n"
      + " @return an intersting value\n"
      + " @throws RuntimeException if something goes wrong";

    assertThat(Utils.stripBlockTagsFromJavadoc(javadoc))
      .isEqualTo(""
      + " bla bla dumdidum\n"
      + " lalala oo jaja {@code somecode}\n"
      + " {@link Class#method()} blubb\n"
      + "\n"
      + " sometext\n"
      + "<p>\n"
      + " moretext\n"
      + "\n"
      + "");
  }

}
