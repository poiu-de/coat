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
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.OptionalInt;


/**
 *
 *
 */
@Coat.Config
public interface ExampleConfig {
  /**
   * This is a mandatory string that must be specified in the config file.
   * <p>
   * The application will not start without it.
   *
   * @return the config value for mandatorString
   */
  @Coat.Param(key = "mandatoryString")
  public String mandatoryString();

  /**
   * An optional int that <em>may</em> be specified, but may also be left off.
   * <p>
   * The application can run without it.3
   *
   * Und hier ist das @mittendrin enthalten.
   *
   * @return the config value for optionalInt
   */
  @Coat.Param(key = "optionalInt")
  public OptionalInt optionalInt();

  /**
   * The charset to use. If not specified it defaults to UTF-8.
   *
   * @return the config value for charsetWithDefault
   */
  @Coat.Param(key = "charsetWithDefault", defaultValue = "UTF-8")
  public Charset charsetWithDefault();

  /**
   * An InetAddress that <em>may</em> be specified.
   * <p>
   * The value from the config file must conform to the same rules as the InetAddress constructor.
   * Basically that means that IP addresses or hostnames can be specified.
   *
   * @return the config value for optionalInetAddress
   */
  @Coat.Param(key = "optionalInetAddress")
  public Optional<InetAddress> optionalInetAddress();
}
