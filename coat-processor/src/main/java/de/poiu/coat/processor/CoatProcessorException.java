/*
 * Copyright (C) 2020 - 2024 The Coat Authors
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

import javax.lang.model.element.Element;


/**
 * An exeption that is thrown if processing the Coat annotation fails.
 */
public class CoatProcessorException extends RuntimeException {

  private final String msg;
  private final Element element;

  public CoatProcessorException(final String message) {
    this.msg= message;
    this.element= null;
  }

  public CoatProcessorException(final String message, final Element element) {
    this.msg= message;
    this.element= element;
  }


  public CoatProcessorException(final String message, final Throwable cause) {
    super(cause);
    this.msg= message;
    this.element= null;
  }


  public CoatProcessorException(final String message, final Element element, final Throwable cause) {
    super(cause);
    this.msg= message;
    this.element= element;
  }


  public String getMsg() {
    return msg;
  }


  public Element getElement() {
    return element;
  }

}
