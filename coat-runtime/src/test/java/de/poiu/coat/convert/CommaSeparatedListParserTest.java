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

import de.poiu.coat.convert.listparsers.CommaSeparatedListParser;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class CommaSeparatedListParserTest {

  private CommaSeparatedListParser p= new CommaSeparatedListParser();


  /**
   *  Test strings separated by different kinds and numbers of whitespace.
   */
  @Test
  public void testConvert_SimpleStrings() throws Exception {
    assertThat(p.convert("one")).isEqualTo(new String[]{"one"});
    assertThat(p.convert("one two")).isEqualTo(new String[]{"one two"});
    assertThat(p.convert("one    two   \t\t\n  three")).isEqualTo(new String[]{"one    two   \t\t\n  three"});
  }


  /**
   *  Test strings separated by commas. Whitespace around commas is ignored.
   */
  @Test
  public void testConvert_CommaSeparated() throws Exception {
    assertThat(p.convert("one,two")).isEqualTo(new String[]{"one", "two"});
    assertThat(p.convert("one\t  \n,  \t two, three")).isEqualTo(new String[]{"one", "two", "three"});
    assertThat(p.convert("Gert Fröbe <gert@froebe.com>, Micky Mouse <micky@disney.com>")).isEqualTo(new String[]{"Gert Fröbe <gert@froebe.com>", "Micky Mouse <micky@disney.com>"});
  }


  /**
   *  Test that blank or epmty strings as well as null always return an empty array.
   */
  @Test
  public void testConvert_emptyAndNull() throws Exception {
    assertThat(p.convert("")).isEqualTo(new String[]{});
    assertThat(p.convert(null)).isEqualTo(new String[]{});
    assertThat(p.convert("    \t \n  ")).isEqualTo(new String[]{});
  }


  /**
   *  Test that commas inside Strings that are escaped by backslashes are correctly parsed.
   */
  @Test
  public void testConvert_CommasInStrings() throws Exception {
    assertThat(p.convert("first\\, string, second\\,string")).isEqualTo(new String[]{"first, string", "second,string"});
    assertThat(p.convert("first\\,\\,string\\,,second string")).isEqualTo(new String[]{"first,,string,", "second string"});
  }


  /**
   *  Test that backslashes in Strings are ignored unless escaped by another backslash.
   */
  @Test
  public void testConvert_BackslashesInStrings() throws Exception {
    assertThat(p.convert("o\\n\\e")).isEqualTo(new String[]{"one"});
    assertThat(p.convert("o\\\\n\\\\e")).isEqualTo(new String[]{"o\\n\\e"});
    assertThat(p.convert("one\\")).isEqualTo(new String[]{"one\\"});
  }


  /**
   *  Test that backslash escaped whitespace is just being ignored on finding delimiters.
   */
  @Test
  public void testConvert_BackslashEscapedWhitespace() throws Exception {
    assertThat(p.convert("one\\ ,two")).isEqualTo(new String[]{"one", "two"});
    assertThat(p.convert("one\\   ,  two")).isEqualTo(new String[]{"one", "two"});
  }


  /**
   *  Test that backslashes escape backslashes.
   */
  @Test
  public void testConvert_BackslashEscapesBackslash() throws Exception {
    assertThat(p.convert("one, two")).isEqualTo(new String[]{"one", "two"});
    assertThat(p.convert("one\\, two")).isEqualTo(new String[]{"one, two"});
    assertThat(p.convert("one\\\\, two")).isEqualTo(new String[]{"one\\", "two"});
    assertThat(p.convert("one\\\\\\, two")).isEqualTo(new String[]{"one\\, two"});
  }
}
