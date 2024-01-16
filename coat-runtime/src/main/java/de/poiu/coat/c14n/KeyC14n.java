/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.poiu.coat.c14n;


/**
 *
 * @author mherrn
 */
public class KeyC14n {
  public static String c14n(final String s) {
    final StringBuilder sb= new StringBuilder();

    for (int i=0; i < s.length(); i++) {
      final char c= s.charAt(i);

      if (c == '_' || c == '-' || c == '.') {
        sb.append('_');
      } else if (Character.isUpperCase(c)) {
        sb.append('_');
        sb.append(c);
      } else {
        sb.append(Character.toUpperCase(c));
      }
    }

    return sb.toString();
  }
}
