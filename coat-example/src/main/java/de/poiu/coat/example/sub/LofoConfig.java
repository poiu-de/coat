/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.poiu.coat.example.sub;

import de.poiu.coat.annotation.Coat;


/**
 *
 * @author mherrn
 */
@Coat.Config
public interface LofoConfig {

  @Coat.Param(key = "lofoName")
  public String lofoName();

  @Coat.Embedded(key = "mqtt", keySeparator = ".")
  public MqttConfig mqtt();
}
