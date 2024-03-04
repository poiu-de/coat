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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Set;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;


/**
 * A very simple example app to demonstrate the usage of Coat.
 */
public class ExampleApp {

  public static void main(String[] args) throws IOException {
    // Read the properties file
    final Properties props= new Properties();
    props.load(ExampleApp.class.getResourceAsStream("/app.properties"));

    // The “ImmutableAppConfig” is the generated config class.
    // We construct it and hand it the properties from the properties file.
    final ImmutableAppConfig appConfig= ImmutableAppConfig.from(props);

    // The generated config contains a “toString()” method that prints the whole config
    // with all actually assigned values.
    System.out.println("Configuration:\n" + appConfig.toString());

    // Use Coat Validation to assert that all mandatory fields are filled and all values can be
    // converted to their real types.
    // To access the “validate()” method we need to call it on the concrete generated class.
    try {
      appConfig.validate();
    } catch (ConfigValidationException ex) {
      // In case the validation fails, print the problematic keys and their assigned values.
      System.err.println("Error in config:\n" + ex.getValidationResult().toString());
      System.exit(1);
    }

    // Use Bean Validation to assert that all values adhere to their restrictions
    final Validator validator = Validation.byDefaultProvider()
      .configure()
      .messageInterpolator(new ParameterMessageInterpolator())
      .buildValidatorFactory()
      .getValidator();
    final Set<ConstraintViolation<AppConfig>> violations = validator.validate(appConfig);
    if (!violations.isEmpty()) {
      System.err.println("Error in config:");
      for (final ConstraintViolation<AppConfig> violation : violations) {
        System.err.println(violation.getPropertyPath()+" → "+violation.getMessage());
      }
      System.exit(1);
    }

    // now use the config object to start the application (with type-safe access to all fields!).
    startApplication(appConfig);
  }


  // To access the config values we do not need the concrete generated class and can just use
  // the hand written interface.
  private static void startApplication(final AppConfig appConfig) {
    // Use the embedded MqttConfig to connect to the MQTT broker.
    startMqttClient(appConfig.mqtt());

    // Print the application name and, if specified, the description of this application.
    System.out.println("Starting server for " + appConfig.getName() + " …");
    appConfig.getDescription().ifPresent(System.out::println);

    // Start a listening Socket for this application
    try {
      final ServerSocket ss= new ServerSocket(appConfig.getListenPort(), 1, appConfig.getListenAddress());
      //final Socket s= ss.accept();
      // …
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(2);
    }

    // We are finished now
    System.out.println("\nDemonstration finished…");
  }


  private static void startMqttClient(final MqttConfig mqtt) {
    // Use the values from the MqttConfig to connect to the MQTT broker
    final DummyMqttClient mqttClient= new DummyMqttClient(mqtt.getBrokerAddresses(), mqtt.getPort());
    mqtt.getClientId().ifPresent(mqttClient::setClientId);
    mqtt.username().ifPresent(mqttClient::setUsername);
    mqtt.password().ifPresent(mqttClient::setPassword);

    mqttClient.connect();
  }
}
