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

import de.poiu.coat.processor.codegeneration.CodeGenerator;
import de.poiu.coat.processor.examplecontent.ExampleContentHelper;
import de.poiu.coat.processor.specs.ClassSpec;
import de.poiu.coat.processor.specs.SpecHandler;
import de.poiu.coat.processor.specs.ClassSpecComparator;
import de.poiu.coat.processor.utils.Assertions;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;


/**
 * The actual annotation processor of Coat.
 *
 */
@SupportedAnnotationTypes(
  "de.poiu.coat.annotation.Coat.Config"
)
public class CoatProcessor extends AbstractProcessor {
  

  //////////////////////////////////////////////////////////////////////////////
  //
  // Attributes

  private SpecHandler          specHandler          = null;
  private Assertions           assertions           = null;
  private CodeGenerator        codeGenerator        = null;
  private ExampleContentHelper exampleContentHelper = null;


  //////////////////////////////////////////////////////////////////////////////
  //
  // Methods

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }


  @Override
  public boolean process(final Set<? extends TypeElement> annotations,
                         final RoundEnvironment           roundEnv) {

    this.specHandler          = new SpecHandler(processingEnv);
    this.assertions           = new Assertions(processingEnv);
    this.codeGenerator        = new CodeGenerator(processingEnv);
    this.exampleContentHelper = new ExampleContentHelper(processingEnv);

    // for each Coat.Config annotation
    // get all the annotated types
    // and generate the corresponding classes
    annotations.stream()
      .filter(a -> a.getQualifiedName().contentEquals("de.poiu.coat.annotation.Coat.Config"))
      .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
      .map(e -> (TypeElement) e)
      .map(this.specHandler::classSpecFrom)
      .sorted(ClassSpecComparator.INSTANCE)
      .forEachOrdered(this::generateCode);

    return true;
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Internal helper methods


  private void generateCode(final ClassSpec coatClassSpec) {
    processingEnv.getMessager().printMessage(Kind.NOTE,
                                             String.format("Generating code for %s.", coatClassSpec.annotatedType()));

    // Check all the assertions here
    this.assertions.assertIsInterface(coatClassSpec.annotatedType());
    this.assertions.assertReturnType(coatClassSpec.accessors());
    this.assertions.assertNoParameters(coatClassSpec.accessors());
    this.assertions.assertUniqueKeys(coatClassSpec.accessors());
    this.assertions.assertUniqueMethodNames(coatClassSpec.accessors());
    this.assertions.assertEmbeddedTypesAreAnnotated(coatClassSpec.embeddedTypes());
    this.assertions.assertEmbeddedTypesAreNotInCollection(coatClassSpec.embeddedTypes());
    this.assertions.assertOnlyEitherEmbeddedOrParamAnnotation(coatClassSpec.embeddedTypes());

    final TypeElement annotatedInterface = coatClassSpec.annotatedType();

    // Now start the code generation
    try {
      this.codeGenerator.generateEnumCode(coatClassSpec);
      this.codeGenerator.generateClassCode(coatClassSpec);
      this.exampleContentHelper.generateExampleFile(coatClassSpec, processingEnv.getFiler());
    } catch (CoatProcessorException ex) {
      processingEnv.getMessager().printMessage(Kind.ERROR,
                                               ex.getMsg(),
                                               ex.getElement() != null ? ex.getElement() : annotatedInterface);
    } catch (Exception ex) {
      processingEnv.getMessager().printMessage(Kind.ERROR,
                                               String.format("Error generating code for %s: %s", annotatedInterface, ex.getMessage()),
                                               annotatedInterface);
      ex.printStackTrace();
    }
  }
}
