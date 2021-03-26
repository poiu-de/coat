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
public interface EmbeddedConfig {

  @Coat.Param(key = "embeddedParam")
  public String embeddedParam();

  @Coat.Embedded(key = "deeplyEmbedded", keySeparator= ".")
  public DeeplyEmbeddedConfig deeplyEmbedded();

}
