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
package de.poiu.coat.processor;


public class JavadocHelper {

  public static final String JAVADOC_ON_FROM_FILE= ""
    + "Create a new $T from the given config file.\n"
    + "\n"
    + "@param file the config file to read\n"
    + "@return the $T created with the entries from the given file\n"
    + "@throws IOException if reading the given file failed";


  public static final String JAVADOC_ON_FROM_MAP= ""
    + "Create a new $T from the given config entries.\n"
    + "\n"
    + "@param props the config entries\n"
    + "@return the $T created with the given entries";


  public static final String JAVADOC_ON_FROM_PROPERTIES= ""
    + "Create a new $T from the given config entries.\n"
    + "\n"
    + "@param jup the config entries\n"
    + "@return the $T created with the given entries";


  public static final String JAVADOC_ON_FROM_ENV_VARS= ""
    + "Create a new $T from the current environment variables.\n"
    + "<p>\n"
    + "Since the allowed characters for environment variables are much more restricted than Coat config keys,\n"
    + "a relaxed mapping is applied. For example the environment variable\n"
    + "<code>SERVER_MQTT_HOST</code> will match the config key <code>server.mqttHost</code>.\n"
    + "\n"
    + "@return the $T created with the entries in the current environment variables";


  public static final String JAVADOC_ON_ADD_FILE= ""
    + "Add the config entries in the given config file to this $T.\n"
    + "Already existing entries will be overwritten.\n"
    + "\n"
    + "@param file the config file to read\n"
    + "@return this $T\n"
    + "@throws IOException if reading the given file failed";


  public static final String JAVADOC_ON_ADD_MAP= ""
    + "Add the given config entries to this $T.\n"
    + "Already existing entries will be overwritten.\n"
    + "\n"
    + "@param props the config entries\n"
    + "@return this $T";


  public static final String JAVADOC_ON_ADD_PROPERTIES= ""
    + "Add the given config entries to this $T.\n"
    + "Already existing entries will be overwritten.\n"
    + "\n"
    + "@param jup the config entries\n"
    + "@return this $T";


  public static final String JAVADOC_ON_ADD_ENV_VARS= ""
    + "Add the config entries from the current environment variables to this $T.\n"
    + "Already existing entries will be overwritten.\n"
    + "<p>\n"
    + "Since the allowed characters for environment variables are much more restricted than Coat config keys,\n"
    + "a relaxed mapping is applied. For example the environment variable\n"
    + "<code>SERVER_MQTT_HOST</code> will match the config key <code>server.mqttHost</code>.\n"
    + "\n"
    + "@return this $T";
}
