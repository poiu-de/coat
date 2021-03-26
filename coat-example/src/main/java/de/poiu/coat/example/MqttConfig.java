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
import java.net.InetAddress;
import java.util.Optional;


/**
 * MQTT Client configuration
 */
@Coat.Config
public interface MqttConfig {

  /** The clientId to send to the MQTT broker. */
  @Coat.Param(key = "client_id")
  public Optional<String>   clientId();

  /** The address of the MQTT broker. */
  @Coat.Param(key = "broker_address")
  public InetAddress        brokerAddress();

  /** The port to communicate with the MQTT broker. */
  @Coat.Param(key = "port", defaultValue = "1883")
  public int port();

  /** The username to connect to the MQTT broker. */
  @Coat.Param(key="username")
  public Optional<String>   username();

  /** The password to connect to the MQTT broker. */
  @Coat.Param(key="password")
  public Optional<String>   password();
}
