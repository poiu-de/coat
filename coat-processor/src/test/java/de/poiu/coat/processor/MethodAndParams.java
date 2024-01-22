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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Stream;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;

import static java.util.stream.Collectors.toList;


/**
 * Data holder for method names and their parameter types.
 */
@Value.Immutable
public interface MethodAndParams {
  public String           methodName();
  public List<String>     params();


  public static MethodAndParams from(final Method method) {
    return ImmutableMethodAndParams.builder()
      .methodName(method.getName())
      .params(
        Stream.of(method.getParameters())
          .map(Parameter::getType)
          .map(Class::getName)
          .collect(toList()))
      .build();
  }


  public static MethodAndParams from(final ExecutableElement method) {
    return ImmutableMethodAndParams.builder()
      .methodName(method.getSimpleName().toString())
      .params(
        method.getParameters().stream()
          .map(VariableElement::asType)
          .map(TypeMirror::toString)
          .collect(toList()))
      .build();
  }


  public static MethodAndParams from(final String methodName, final Class<?>... params) {
    return ImmutableMethodAndParams.builder()
      .methodName(methodName)
      .params(Stream.of(params).map(Class::getName).collect(toList()))
      .build();
  }
}
