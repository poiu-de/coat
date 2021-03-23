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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ExampleApp {

  public static void main(String[] args) throws IOException {
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
      final File propertiesFile= File.createTempFile("coat", ".properties");
      try(final FileWriter fw= new FileWriter(propertiesFile);) {
        TheOtherConfig.writeExampleConfig(fw);
        System.out.println("Example config written to: " + propertiesFile.getAbsolutePath());
      }
    }
  }
}
