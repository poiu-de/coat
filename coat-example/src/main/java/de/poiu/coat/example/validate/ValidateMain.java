/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.poiu.coat.example.validate;

import static de.poiu.coat.example.validate.ValidationFailure.Type.MISSING_MANDATORY_VALUE;
import static de.poiu.coat.example.validate.ValidationFailure.Type.UNPARSABLE_VALUE;

/**
 *
 * @author mherrn
 */
public class ValidateMain {
  public static void main(String[] args) {
    final ValidationResult result= ImmutableValidationResult.builder()
      .addValidationFailure(
        ImmutableValidationFailure.builder()
          .failureType(MISSING_MANDATORY_VALUE)
          .key("derSchluessel")
          .type("String.class")
          .build())
      .addValidationFailure(
        ImmutableValidationFailure.builder()
          .failureType(UNPARSABLE_VALUE)
          .key("otherKey")
          .type("int.class")
          .value("fümpfundzwanzich")
          .build())
      .build();

    System.out.println(result);
  }
}
