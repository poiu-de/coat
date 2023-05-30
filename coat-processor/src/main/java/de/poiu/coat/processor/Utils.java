/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.poiu.coat.processor;

import java.util.regex.Pattern;

import static java.util.function.Predicate.not;


/**
 *
 * @author mherrn
 */
class Utils {

  private static final Pattern PATTERN_JAVADOC_BLOCK_TAG = Pattern.compile("^\\s*@.*");


  static String stripBlockTagsFromJavadoc(final String javadoc) {
    if (javadoc == null) {
      return "";
    }

    final StringBuilder sb= new StringBuilder();

    javadoc.lines()
      .takeWhile(not(PATTERN_JAVADOC_BLOCK_TAG.asMatchPredicate()))
      .map(s -> s + '\n')
      .forEachOrdered(sb::append)
      ;

    return sb.toString();
  }

}
