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

import de.poiu.coat.example.embedded.ImmutableMainConfig;
import de.poiu.coat.example.manual.ImmutableDummyConfig;
import de.poiu.coat.validation.ConfigValidationException;
import java.util.HashMap;
import java.util.Map;


public class ExampleApp {

  public static void main(String[] args) {
    final Map<String, String> props= new HashMap<>();
    props.put("mandatoryString",     "someValue");
    props.put("optionalInt",         "42");
    props.put("optionalInetAddress", "127.0.0.1");
    props.put("optionalAndDefault",  "some other value");

    System.out.println("Properties: " + props);

    {
      final ExampleConfig c= new ImmutableExampleConfig(props);
      System.out.println(c);
      System.out.println(c.mandatoryString());
      System.out.println(c.optionalInt());
      System.out.println(c.charsetWithDefault());
      System.out.println(c.optionalInetAddress());
    }

    {
      final SomeSubConfig c= new TheOtherConfig(props);
      System.out.println(c);
      System.out.println(c.disabled());
      System.out.println(c.mandatoryString());
      System.out.println(c.optionalInt());
      System.out.println(c.charsetWithDefault());
      System.out.println(c.optionalInetAddress());
    }

    {
      System.out.println("\n-------\n");
      final Map<String, String> p= new HashMap<>();
      p.put("mqtt.host",     "localhost");
      p.put("lofoName",         "lofo_burps");
      final ImmutableDummyConfig c= new ImmutableDummyConfig(p);
      System.out.println(c.lofoName());
      System.out.println(c.mqtt());
      System.out.println("->");
      System.out.println(c);

      try {
        c.validate();
      } catch (ConfigValidationException ex) {
        ex.printStackTrace();
      }
    }

    {
      System.out.println("\n**************************\n");
      final Map<String, String> p= new HashMap<>();
      p.put("someParam",                                   "some value");
      p.put("embedded.embeddedParam",                      "embedded value");
      p.put("embedded.deeplyEmbedded.deeplyEmbeddedParam", "deeply embedded value");
      p.put("irrelevant key",                              "irrelevant value");
      p.put("einfachso.deeplyEmbeddedParam",               "ei guck!");
      final ImmutableMainConfig c= new ImmutableMainConfig(p);

      System.out.println("\nCCC "+ c);
      System.out.println("");
      try {
        c.validate();
      } catch (ConfigValidationException ex) {
        ex.printStackTrace();
      }
      System.out.println("\n" + c.someParam());
      System.out.println("\n" + c.einfachso().deeplyEmbeddedParam());
      System.out.println(c.embedded().embeddedParam());
      System.out.println(c.embedded().deeplyEmbedded().deeplyEmbeddedParam());
      System.out.println("________");
      System.out.println(c);
    }
  }
}
