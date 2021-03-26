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
public interface DeeplyEmbeddedConfig {

  @Coat.Param(key = "deeplyEmbeddedParam")
  public String deeplyEmbeddedParam();

}
