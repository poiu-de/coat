/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.poiu.coat.example.embedded;

import de.poiu.coat.annotation.Coat;


/**
 *
 * @author mherrn
 */
@Coat.Config
public interface MainConfig {

  @Coat.Param(key = "someParam")
  public String someParam();

  @Coat.Embedded(key = "embedded", keySeparator= ".")
  public EmbeddedConfig embedded();

  @Coat.Embedded(key = "einfachso")
  public DeeplyEmbeddedConfig einfachso();
}
