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
