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
package de.poiu.coat.example.mqtt;

import java.net.InetAddress;


/**
 * A dummy MQTT client for demonstration purposes. It only provides a minimal API and no actual functionality.
 */
public class DummyMqttClient {


  /**
   * Create a new DummyMqttClient that connects to the specified address(es) and port.
   * The given addresses must include at least one address, but can have several fallback addresses.
   * They will be tried sequentially.
   */
  public DummyMqttClient(final InetAddress[] brokerAddresses, final int port) {
    // no real functionality
  }


  /**
   * Set a custom client ID for connecting to the MQTT broker.
   */
  public void setClientId(final String clientId) {
    // no real functionality
  }


  /**
   * Set the username to connect to the MQTT broker.
   */
  public void setUsername(final String username) {
    // no real functionality
  }


  /**
   * Set the password to connect to the MQTT broker.
   */
  public void setPassword(final String password) {
    // no real functionality
  }


  /**
   * Connect to the MQTT broker. This will try to connect to each fallback broker in succession until
   * a connection could be established.
   */
  public void connect() {
    // no real functionality
  }

}
