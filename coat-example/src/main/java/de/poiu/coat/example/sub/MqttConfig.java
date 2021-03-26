/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.poiu.coat.example.sub;

import de.poiu.coat.annotation.Coat;
import java.util.Optional;


/**
 *
 * @author mherrn
 */
@Coat.Config
public interface MqttConfig {

  @Coat.Param(key = "clientId")
  public Optional<String> clientId();


  @Coat.Param(key = "host")
  public String host();
}
