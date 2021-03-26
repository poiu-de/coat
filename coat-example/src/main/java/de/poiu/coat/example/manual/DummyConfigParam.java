/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.poiu.coat.example.manual;

import de.poiu.coat.ConfigParam;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;


public enum DummyConfigParam implements ConfigParam {
  LOFO_NAME("lofoName", java.lang.String.class, null, true, false),
  ;

  private final String key;

  private final Class type;

  private final String defaultValue;

  private final boolean mandatory;

  private final boolean embedded;

  private DummyConfigParam(final String key, final Class type, final String defaultValue,
      final boolean mandatory, final boolean embedded) {
    this.key = key;
    this.type = type;
    this.defaultValue = defaultValue;
    this.mandatory = mandatory;
    this.embedded = embedded;
  }

  @Override
  public String key() {
    return this.key;
  }

  @Override
  public Class type() {
    return this.type;
  }

  @Override
  public String defaultValue() {
    return this.defaultValue;
  }

  @Override
  public boolean mandatory() {
    return this.mandatory;
  }

  @Override
  public boolean embedded() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }


}