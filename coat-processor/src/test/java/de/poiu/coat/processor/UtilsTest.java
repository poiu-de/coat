/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit4TestClass.java to edit this template
 */
package de.poiu.coat.processor;

import de.poiu.coat.processor.Utils;
import de.poiu.coat.processor.CoatProcessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;



/**
 *
 * @author mherrn
 */
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
