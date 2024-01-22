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
package de.poiu.coat.processor.examplecontent;

import de.poiu.coat.processor.utils.JavadocHelper;
import de.poiu.coat.processor.specs.AccessorSpec;
import de.poiu.coat.processor.specs.ClassSpec;
import de.poiu.coat.processor.utils.SpecHelper;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;


/**
 * Helper class for creating example config file content.
 */
public class ExampleContentHelper {

  private final ProcessingEnvironment pEnv;

  public ExampleContentHelper(final ProcessingEnvironment pEnv) {
    this.pEnv = pEnv;
  }


  /**
   * Create the content of an example config file for the given ClassSpec.
   * <p>
   * It will contain the entries of all accessors, direct ones as well as inherited ones and also
   * accessor of embedded configs.
   *
   * @param classSpec the classSpec for which to generate the example config
   * @return the example config to be used in a Java Properties file
   */
  public String createExampleContent(final ClassSpec classSpec) {
    final List<AccessorSpec> accessorSpecs= SpecHelper.getAccessorSpecsRecursively(classSpec);
    return this.createExampleContent(accessorSpecs);
  }


  /**
   * Write an example config file for the given ClassSpec.
   * <p>
   * It will contain the entries of all accessors, direct ones as well as inherited ones and also
   * accessor of embedded configs.
   * <p>
   * The config file will be written into the standard output folder for compiled classes in an extra
   * subfolder “examples”. The name of the generated file will be the name of the given class with
   * “.properties” appended to it.
   *
   * @param classSpec the classSpec for which to write the example config
   * @param filer     the filer to use for writing the config file
   * @throws java.io.IOException if writing the file fails
   */
  public void generateExampleFile(final ClassSpec classSpec,
                                  final Filer        filer) throws IOException {

    final String exampleFileName= classSpec.annotatedType().getSimpleName().toString() + ".properties";

    final List<AccessorSpec> accessorSpecs= SpecHelper.getAccessorSpecsRecursively(classSpec);

    final FileObject resource = filer.createResource(
      javax.tools.StandardLocation.CLASS_OUTPUT,
      "examples",
      exampleFileName,
      classSpec.annotatedType());
    try (final Writer w= resource.openWriter();) {
      w.write(this.createExampleContent(accessorSpecs));
    }
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


  /**
   * Create the content of an example config file from the given list of accessors.
   *
   * @param accessorSpecs
   * @return
   */
  private String createExampleContent(final List<? extends AccessorSpec> accessorSpecs) {
    final StringBuilder sb= new StringBuilder();

    for (final AccessorSpec accessorSpec : accessorSpecs) {
      // add javadoc as comment
      final String javadoc = this.pEnv.getElementUtils().getDocComment(accessorSpec.accessor());
      JavadocHelper.stripBlockTagsFromJavadoc(javadoc)
        .lines()
        .map(s -> "## " + s + "\n")
        .forEachOrdered(sb::append);

      // add a config keyElement
      if (!accessorSpec.mandatory() || (accessorSpec.defaultValue() != null && !accessorSpec.defaultValue().trim().isEmpty())) {
        // commented out if optional or has default value
        sb.append("# ");
      }
      sb.append(accessorSpec.key()).append(" = ");
      sb.append(accessorSpec.defaultValue() != null ? accessorSpec.defaultValue() : "");
      sb.append("\n\n");
    }

    return sb.toString();
  }
}
