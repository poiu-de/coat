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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.InetAddress;
import java.util.Optional;


/**
 * Main configuration for the example application.
 *
 * Since we use Bean Validation we <i>must</i> use a “get” prefix. Otherwise Bean Validation would
 * silently ignore the annotations.
 */
@Coat.Config(casing = CasingStrategy.SNAKE_CASE)                    // The keys in the config file will be written in “snake_case”
public interface AppConfig {

  /** A shorthand name for this application. */
  @Coat.Param(key = "application_name")                             // Rather than “name“ this property will be specified as “application_name” in the config file
  public String               getName();                            // The application_name is mandatory

  /** A short description of the purpose of this application. */
  public Optional<String>     getDescription();                     // The description is optional

  /** The interfaces to listen on for incoming connections. */
  @Coat.Param(defaultValue = "0.0.0.0")                             // If the listen_address is not given, default to 0.0.0.0
  public InetAddress          getListenAddress();

  /** The port to listen on for incoming connections. */
  @Coat.Param(defaultValue = "8080")                                // The default port is 8080
  @Min(1024) @Max(49151)                                            // Use Bean Validation annotations to restrict the range of allowed ports
  public int                  getListenPort();

  /** The configuration for the MQTT connection */
  @Coat.Embedded                                                    // Embed another config object
  public MqttConfig           mqtt();
}
