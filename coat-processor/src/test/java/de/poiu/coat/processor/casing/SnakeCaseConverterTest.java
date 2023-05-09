/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package de.poiu.coat.processor.casing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 *
 * @author mherrn
 */
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
