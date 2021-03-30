/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.poiu.coat.example.manual;


import de.poiu.coat.CoatConfig;
import de.poiu.coat.example.sub.LofoConfig;
import de.poiu.coat.example.sub.MqttConfig;
import java.io.File;
import java.io.IOException;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static de.poiu.coat.example.manual.DummyConfigParam.LOFO_NAME;


public class ImmutableDummyConfig extends CoatConfig implements LofoConfig {

  public ImmutableDummyConfig(final File file) throws IOException {
    this(toMap(file));
  }

  public ImmutableDummyConfig(final Properties jup) {
    this(toMap(jup));
  }

  public ImmutableDummyConfig(final Map<String, String> props) {
    super(props, de.poiu.coat.example.sub.LofoConfigParam.values());

    this.mqtt= new de.poiu.coat.example.sub.ImmutableMqttConfig(
      filterByAndStripPrefix(props, "mqtt."));
    super.registerEmbeddedConfig("mqtt.", this.mqtt, false);
  }

  @Override
  public String lofoName() {
    return super.getString(LOFO_NAME);
  }

  @Override
  public MqttConfig mqtt() {
    return this.mqtt;
  }


  @Override
  public int hashCode() {
    return Objects.hash(
      this.mqtt()
    );
  }


  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (this.getClass() != obj.getClass()) {
      return false;
    }

    final ImmutableDummyConfig other = (ImmutableDummyConfig) obj;

    if (!Objects.equals(this.mqtt(), other.mqtt())) {
      return false;
    }

    return true;
  }





  ////////////////////////

  private final de.poiu.coat.example.sub.ImmutableMqttConfig mqtt;

}