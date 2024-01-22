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
package de.poiu.coat.annotation;


/**
 * Base annotation for all Coat annotations.
 *
 * All concrete annotations are inner classes of this one.
 *
 */
import de.poiu.coat.convert.Converter;
import de.poiu.coat.convert.ListParser;
import de.poiu.coat.casing.CasingStrategy;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static de.poiu.coat.casing.CasingStrategy.AS_IS;

/**
 * Container annotation for different concrete annotations supported by Coat.
 *
 */
public @interface Coat {

  /**
   * All config interfaces that should be processed by Coat <i>must</i> be annotated with <code>@Coat.Config</code>.
   * <p>
   * For each interface that is annotated with <code>@Coat.Config</code> two classes are generated.
   * <ul>
   *   <li>An enum that contains one constant for each accessor method annotated with {@link Param}
   *       named <code><i>&lt;ConfigClass&gt;</i>Param</code>, where <i>&lt;ConfigClass&gt;</i> is the name
   *       of the annotated interface.</li>
   *   <li>A concrete class implementing the annoated interface that derives from {@link de.poiu.coat.CoatConfig}
   *       and provides the main way to get the actual config values. By default it is named
   *       <code>Immutable<i>&lt;ConfigClass&gt;</i></code>. To let the generator generate a different name,
   *       specify that name with the option {@link #className()}.
   * </ul>
   *
   */
  @Target(ElementType.TYPE)
  public @interface Config {
    /**
     * The class name to use for the generated class.
     * It will be generated in the same package as the annotated interface.
     * Therefore it must be a simple class name and not a fully qualified class name.
     */
    public String                          className()       default "";
    /**
     * The naming strategy to use for inferred keys.
     */
    public CasingStrategy                  casing()          default AS_IS;
    /**
     * Whether to ignore a “get” prefix on accessor methods.
     */
    public boolean                         stripGetPrefix()  default true;
    /**
     * Custom converters to register for the generated class.
     */
    public Class<? extends Converter<?>>[] converters()      default VoidConverter.class;
    /**
     * Custom list parser to register for the generated class.
     */
    public Class<? extends ListParser>     listParser()      default VoidListParser.class;
  }


  /**
   * Each accessor method in a <code>@Coat.Config</code> annotated interface <i>must</i> be annotated with <code>@Coat.Param</code>.
   * <p>
   * The option {@link #key()} specifies the config key as it is used in the config file.
   * <p>
   * The option {@link #defaultValue()} may be specified to provide a default value in case no value is assigned to this parameter in the config file.
   *
   */
  @Target(ElementType.METHOD)
  public @interface Param {
    /**
     * The key to expect in the config file for this accessor method.
     */
    public String                        key()            default "";
    /**
     * The default value to use if the key is missing in the config file.
     */
    public String                        defaultValue()   default "";
    /**
     * Custom converter to register for this accessor.
     */
    public Class<? extends Converter<?>> converter()      default VoidConverter.class;
    /**
     * Custom list parser to register for this accessor.
     */
    public Class<? extends ListParser>   listParser()     default VoidListParser.class;
  }


  /**
   * An accessor method in a <code>@Coat.Config</code> annotated interface may be annotated with <code>@Coat.Embedded</code>
   * instead of <code>@Coat.Param</code> to include an instance of another <code>@Coat.Config</code> annotated interface.
   * This allows encapsulation and better reuse of config values than simple interface inheritance.
   * <p>
   * The option {@link #key()} specifies the config key as it is used in the config file.
   * <p>
   * The option {@link #keySeparator()} specifies the separator between the key of this embedded config and the actual keys
   * inside the embedded config. It defaults to a single dot.
   * <p>
   */
  @Target(ElementType.METHOD)
  public @interface Embedded {
    /**
     * The key to expect in the config file for this prefix of the embedded values.
     */
    public String  key()           default  "";
    /**
     * The separator to use between the prefix and the key of the embedded values.
     * @return
     */
    public String  keySeparator()  default  ".";
  }

}

