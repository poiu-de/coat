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
package de.poiu.coat.convert;

import java.util.Properties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;


public class WhitspaceSeparatedListParserTest {

  private WhitespaceSeparatedListParser p= new WhitespaceSeparatedListParser();


  /**
   *  Test strings separated by different kinds and numbers of whitespace.
   */
  @Test
  public void testConvert_SimpleStrings() throws Exception {
    assertThat(p.convert("one")).isEqualTo(new String[]{"one"});
    assertThat(p.convert("one two")).isEqualTo(new String[]{"one", "two"});
    assertThat(p.convert("one    two   \t\t\n  three")).isEqualTo(new String[]{"one", "two", "three"});
  }


  /**
   *  Test that blank or empty strings as well as null always return an empty array.
   */
  @Test
  public void testConvert_emptyAndNull() throws Exception {
    assertThat(p.convert("")).isEqualTo(new String[]{});
    assertThat(p.convert(null)).isEqualTo(new String[]{});
    assertThat(p.convert("    \t \n  ")).isEqualTo(new String[]{});
  }


  /**
   *  Test that spaces inside Strings that are escaped by backslashes are correctly parsed.
   */
  @Test
  public void testConvert_SpacesInStrings() throws Exception {
    assertThat(p.convert("first\\ string second\\ string")).isEqualTo(new String[]{"first string", "second string"});
    assertThat(p.convert("first\\ \\ \\\t\\ \\\nstring second\\\nstring")).isEqualTo(new String[]{"first  \t \nstring", "second\nstring"});
  }


  /**
   *  Test that backslashed in Strings are ignored unless escaped by another backslash.
   */
  @Test
  public void testConvert_BackslashesInStrings() throws Exception {
    assertThat(p.convert("o\\n\\e")).isEqualTo(new String[]{"one"});
    assertThat(p.convert("o\\\\n\\\\e")).isEqualTo(new String[]{"o\\n\\e"});
    assertThat(p.convert("one\\")).isEqualTo(new String[]{"one\\"});
  }
}
