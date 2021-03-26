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
 * Main configuration for the example application.
 */
@Coat.Config
public interface AppConfig {

  /** A shorthand name for this application. */
  @Coat.Param(key = "name")
  public String name();

  /** A short description of the purpose of this application. */
  @Coat.Param(key = "description")
  public Optional<String> description();

  /** The interfaces to listen on for incoming connections. */
  @Coat.Param(key = "listen_address", defaultValue = "0.0.0.0")
  public InetAddress listenAddres();

  /** The port to listen on for incoming connections. */
  @Coat.Param(key = "listen_port", defaultValue = "8080")
  public int listenPort();

  /** The configuration for the MQTT connection */
  @Coat.Embedded(key = "mqtt")
  public MqttConfig mqtt();
}
