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

import de.poiu.coat.example.mqtt.DummyMqttClient;
import de.poiu.coat.validation.ConfigValidationException;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;


/**
 * A very simple example app to demonstrate the usage of Coat.
 */
public class ExampleApp {

  public static void main(String[] args) throws IOException {
    final Properties props= new Properties();
    props.load(ExampleApp.class.getResourceAsStream("/app.properties"));

    final ImmutableAppConfig appConfig= new ImmutableAppConfig(props);

    System.out.println("Configuration:\n" + appConfig.toString());

    try {
      appConfig.validate();
    } catch (ConfigValidationException ex) {
      System.err.println("Error in config:\n" + ex.getValidationResult().toString());
      System.exit(1);
    }

    startMqttClient(appConfig.mqtt());

    System.out.println("Starting server for " + appConfig.name() + " …");
    appConfig.description().ifPresent(System.out::println);

    try {
      final ServerSocket ss= new ServerSocket(appConfig.listenPort(), 1, appConfig.listenAddres());
      //final Socket s= ss.accept();
      // …
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(2);
    }

    System.out.println("\nDemonstration finished…");
  }


  private static void startMqttClient(final MqttConfig mqtt) {
    final DummyMqttClient mqttClient= new DummyMqttClient(mqtt.brokerAddress(), mqtt.port());
    mqtt.clientId().ifPresent(mqttClient::setClientId);
    mqtt.username().ifPresent(mqttClient::setUsername);
    mqtt.password().ifPresent(mqttClient::setPassword);

    mqttClient.connect();
  }
}
