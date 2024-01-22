/*
 * Copyright (C) 2020 - 2021 The Coat Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.poiu.coat.example;

import de.poiu.coat.annotation.Coat;
import de.poiu.coat.casing.CasingStrategy;
import jakarta.validation.constraints.Size;
import java.net.InetAddress;
import java.util.Optional;


/**
 * MQTT Client configuration
 */
@Coat.Config(casing = CasingStrategy.SNAKE_CASE)                    // The keys in the config file will be written in “snake_case”
public interface MqttConfig {

  /** The clientId to send to the MQTT broker. */
  public Optional<String>   getClientId();

  /** The address(es) of the MQTT broker. */
  @Size(min=1 )                                                     // Use Bean Validation to specify that at least one broker address must be given
  public InetAddress[]      getBrokerAddresses();                   // Multiple addresses can be specified (1 primary, n fallback addresses)

  /** The port to communicate with the MQTT broker. */
  @Coat.Param(defaultValue = "1883")                                // The default port is 1883
  public int                getPort();

  /** The username to connect to the MQTT broker. */
  public Optional<String>   username();                             // We leave the “get” prefix out here. Coat doesn’t need it

  /** The password to connect to the MQTT broker. */
  public Optional<String>   password();                             // We leave the “get” prefix out here. Coat doesn’t need it
}
