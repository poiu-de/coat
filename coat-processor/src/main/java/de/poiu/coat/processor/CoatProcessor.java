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

import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import de.poiu.coat.CoatConfig;
import de.poiu.coat.ConfigParam;
import de.poiu.coat.annotation.Coat;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.lang.model.type.TypeKind.DECLARED;


/**
 * The actual annotation processor of Coat.
 *
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(
  "de.poiu.coat.annotation.Coat.Config"
)
@AutoService(Processor.class)
public class CoatProcessor extends AbstractProcessor {

  private static final Pattern PATTERN_JAVADOC_BLOCK_TAG = Pattern.compile("^\\s*@.*");


  @Override
  public boolean process(final Set<? extends TypeElement> annotations,
                         final RoundEnvironment           roundEnv) {

    // for each Coat.Config annotation
    // get all the annotated types
    // and generate the corresponding classes
    annotations.stream()
      .filter(a -> a.getQualifiedName().contentEquals("de.poiu.coat.annotation.Coat.Config"))
      .flatMap(a -> roundEnv.getElementsAnnotatedWith(a).stream())
      .map(e -> (TypeElement) e)
      .forEachOrdered(this::generateCode);

    return false;
  }


  private void generateCode(final TypeElement annotatedInterface) {
    processingEnv.getMessager().printMessage(Kind.NOTE,
                                             String.format("Generating code for %s.", annotatedInterface));

    // FIXME: Check for interface / abstract class and disallow others?

    try {
      final ClassName fqGeneratedEnumName= this.deriveGeneratedEnumName(annotatedInterface);
      this.generateEnumCode(fqGeneratedEnumName, annotatedInterface, processingEnv.getFiler());

      final ClassName fqGeneratedClassName= this.deriveGeneratedClassName(annotatedInterface);
      this.generateClassCode(fqGeneratedClassName, fqGeneratedEnumName, annotatedInterface, processingEnv.getFiler());

      final String exampleFileName= annotatedInterface.getSimpleName().toString() + ".properties";
      this.generateExampleFile(exampleFileName, annotatedInterface, processingEnv.getFiler());
    } catch (IOException ex) {
      processingEnv.getMessager().printMessage(Kind.ERROR,
                                               String.format("Error generating code for %s.", annotatedInterface));
      ex.printStackTrace();
    }
  }


  private Set<TypeElement> getInheritedInterfaces(final TypeElement annotatedInterface) {
    final Set<TypeElement> allExtendedInterfaces= new LinkedHashSet<>();

    annotatedInterface.getInterfaces().stream()
      .filter(i -> (i.getKind() == DECLARED))
      .map(i -> ((DeclaredType) i))
      .map(DeclaredType::asElement)
      .filter(e -> (e.getKind() == ElementKind.INTERFACE))
      .map(e -> ((TypeElement) e))
      .forEachOrdered(e -> {
        allExtendedInterfaces.add(e);
        allExtendedInterfaces.addAll(this.getInheritedInterfaces(e));
      })
      ;

      return allExtendedInterfaces;
  }

  private Set<Element> getInheritedAnnotatedMethods(final TypeElement annotatedInterface) {
    final Set<Element> annotatedMethods= new LinkedHashSet<>();

    this.getInheritedInterfaces(annotatedInterface).stream()
      .map(Element::getEnclosedElements)
      .flatMap(l -> l.stream())
      .filter(e -> (e.getKind() == ElementKind.METHOD))
      .filter(e -> e.getAnnotation(Coat.Param.class) != null)
      .forEachOrdered(annotatedMethods::add);
      ;

    return annotatedMethods;
  }

  private void generateEnumCode(final ClassName fqEnumName,
                                final TypeElement annotatedInterface,
                                final Filer filer) throws IOException {
    processingEnv.getMessager().printMessage(Kind.NOTE,
                                             String.format("Generating enum %s for %s.", fqEnumName, annotatedInterface));

    final TypeSpec.Builder typeSpecBuilder = TypeSpec.enumBuilder(fqEnumName)
      .addModifiers(PUBLIC)
      .addSuperinterface(ClassName.get(ConfigParam.class))
      .addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PRIVATE)
        .addParameter(String.class,     "key",          FINAL)
        .addParameter(Class.class,      "type",         FINAL)
        .addParameter(String.class,     "defaultValue", FINAL)
        .addParameter(TypeName.BOOLEAN, "mandatory",    FINAL)
        .addStatement("this.$N = $N", "key",          "key")
        .addStatement("this.$N = $N", "type",         "type")
        .addStatement("this.$N = $N", "defaultValue", "defaultValue")
        .addStatement("this.$N = $N", "mandatory",    "mandatory")
        .build())
      ;

    this.addGeneratedAnnotation(typeSpecBuilder);

    this.addFieldAndAccessor(typeSpecBuilder, String.class,     "key");
    this.addFieldAndAccessor(typeSpecBuilder, Class.class,      "type");
    this.addFieldAndAccessor(typeSpecBuilder, String.class,     "defaultValue");
    this.addFieldAndAccessor(typeSpecBuilder, TypeName.BOOLEAN, "mandatory");

    final List<Element> annotatedMethods= annotatedInterface.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Param.class) != null)
      .collect(toList());

    annotatedMethods.addAll(this.getInheritedAnnotatedMethods(annotatedInterface));

    if (annotatedMethods.isEmpty()) {
      processingEnv.getMessager().printMessage(Kind.WARNING,
                                             String.format("No annotated methods in %s.", annotatedInterface));
      throw new RuntimeException("At least one annotated method is necessary for " + annotatedInterface.toString());
    }

    for (final Element annotatedMethod : annotatedMethods) {
      this.addEnumConstant(typeSpecBuilder, annotatedMethod);
    }

    JavaFile.builder(fqEnumName.packageName(), typeSpecBuilder.build())
      .build()
      .writeTo(filer);
  }


  private void generateClassCode(final ClassName fqClassName,
                                 final ClassName fqEnumName,
                                 final TypeElement annotatedInterface,
                                 final Filer filer) throws IOException {
    processingEnv.getMessager().printMessage(Kind.NOTE,
                                             String.format("Generating config class %s for %s.", fqClassName, annotatedInterface));

    final TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(fqClassName)
      .addModifiers(PUBLIC)
      .superclass(ClassName.get(CoatConfig.class))
      .addSuperinterface(ClassName.get(annotatedInterface))
      .addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(File.class, "file", FINAL)
        .addStatement("super(file, $T.values())", fqEnumName)
        .addException(IOException.class)
        .build())
      .addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(Properties.class, "props", FINAL)
        .addStatement("super(props, $T.values())", fqEnumName)
        .build())
      .addMethod(MethodSpec.constructorBuilder()
        .addModifiers(PUBLIC)
        .addParameter(ParameterizedTypeName.get(Map.class, String.class, String.class), "props", FINAL)
        .addStatement("super(props, $T.values())", fqEnumName)
        .build())
      ;

    this.addGeneratedAnnotation(typeSpecBuilder);

    final List<Element> annotatedMethods= annotatedInterface.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Param.class) != null)
      .collect(toList());

    annotatedMethods.addAll(this.getInheritedAnnotatedMethods(annotatedInterface));

    for (final Element annotatedMethod : annotatedMethods) {
      this.addAccessorMethod(typeSpecBuilder, annotatedMethod, fqEnumName);
    }

    this.addWriteExampleConfigMethod(typeSpecBuilder, annotatedMethods);

    JavaFile.builder(fqClassName.packageName(), typeSpecBuilder.build())
      .build()
      .writeTo(filer);
  }


  private void addGeneratedAnnotation(final TypeSpec.Builder typeSpecBuilder) {
    final Class<?> generatedAnnotationClass = this.identifyGeneratedAnnotation();
    if (generatedAnnotationClass != null) {
      typeSpecBuilder.addAnnotation(AnnotationSpec.builder(generatedAnnotationClass)
        .addMember("value", "$S", this.getClass().getName())
        .addMember("date", "$S", ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .build());
    }
  }


  private void addFieldAndAccessor(final TypeSpec.Builder enumBuilder, final Class fieldType, final String fieldName) {
    enumBuilder
      .addField(fieldType, fieldName, PRIVATE, FINAL)
      .addMethod(MethodSpec.methodBuilder(fieldName)
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(fieldType)
        .addStatement("return this.$N", fieldName)
        .build()
      );
  }


  private void addFieldAndAccessor(final TypeSpec.Builder enumBuilder, final TypeName fieldType, final String fieldName) {
    enumBuilder
      .addField(fieldType, fieldName, PRIVATE, FINAL)
      .addMethod(MethodSpec.methodBuilder(fieldName)
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .returns(fieldType)
        .addStatement("return this.$N", fieldName)
        .build()
      );
  }


  private void addEnumConstant(final TypeSpec.Builder typeSpecBuilder, final Element annotatedMethod) {
    final ConfigParamSpec configParamSpec= ConfigParamSpec.from(annotatedMethod);

    final String constName= this.toConstName(configParamSpec.methodeName());

    TypeSpec.Builder enumConstBuilder =
      TypeSpec.anonymousClassBuilder("$S, $L.class, $S, $L",
                                     configParamSpec.key(),
                                     toBaseType(configParamSpec.typeName()),
                                     configParamSpec.defaultValue() != null && !configParamSpec.defaultValue().trim().isEmpty()
                                                                       ? configParamSpec.defaultValue()
                                                                       : null,
                                     configParamSpec.mandatory());



    final String javadoc= super.processingEnv.getElementUtils().getDocComment(annotatedMethod);
    if (javadoc != null) {
      enumConstBuilder.addJavadoc(stripBlockTagsFromJavadoc(javadoc));
    }

    typeSpecBuilder.addEnumConstant(constName, enumConstBuilder.build());
  }


  private void addWriteExampleConfigMethod(final TypeSpec.Builder typeSpecBuilder, final List<Element> annotatedMethod) {
    typeSpecBuilder.addMethod(
      MethodSpec.methodBuilder("writeExampleConfig")
        .addModifiers(PUBLIC, STATIC)
        .addParameter(TypeName.get(Writer.class), "writer", FINAL)
        .addStatement("writeExampleConfig(new $T(writer))", BufferedWriter.class)
        .addException(IOException.class)
        .build());

    final String exampleContent= this.createExampleContent(annotatedMethod);

    final MethodSpec.Builder methodBuilder= MethodSpec.methodBuilder("writeExampleConfig");
    methodBuilder
      .addModifiers(PUBLIC, STATIC)
      .addParameter(TypeName.get(BufferedWriter.class), "writer", FINAL)
      .addStatement("writer.append($S)", exampleContent)
      .addStatement("writer.flush()")
      .addException(IOException.class)
      ;

    typeSpecBuilder.addMethod(methodBuilder.build());
  }


  private String toBaseType(final String typeName) {
    if (typeName.equals("java.util.OptionalInt") ||
        typeName.equals("java.util.OptionalDouble") ||
        typeName.equals("java.util.OptionalLong")) {
      return typeName.substring("java.util.Optional".length(),
        typeName.length())
        .toLowerCase();
    } else if (typeName.startsWith("java.util.Optional<") &&
      typeName.endsWith(">")) {
      return typeName.substring("java.util.Optional<".length(),
        typeName.length() - 1
      );
    } else {
      return typeName;
    }
  }


  private void addAccessorMethod(final TypeSpec.Builder typeSpecBuilder, final Element annotatedMethod, final ClassName fqEnumName) {

    final ConfigParamSpec configParamSpec= ConfigParamSpec.from(annotatedMethod);

    final String constName= this.toConstName(configParamSpec.methodeName());
    final ClassName constClass= ClassName.get(fqEnumName.packageName(), fqEnumName.simpleName(), constName);

    final String defaultValue= configParamSpec.defaultValue() != null && !configParamSpec.defaultValue().trim().isEmpty()
                               ? configParamSpec.defaultValue()
                               : "";

    if (configParamSpec.typeName().startsWith("java.util.Optional") && !defaultValue.trim().isEmpty()) {
      processingEnv.getMessager().printMessage(Kind.WARNING,
                                               "Optional and default value don't make much sense together. The Optional will never be empty.",
                                               annotatedMethod);
    }

    final String getter= getSuperGetterName(configParamSpec.typeName(), !defaultValue.trim().isEmpty());

    final MethodSpec.Builder methodSpecBuilder= MethodSpec.overriding((ExecutableElement) annotatedMethod)
        .addStatement("return super.$L($T)",
                      getter,
                      constClass);

    final String javadoc= super.processingEnv.getElementUtils().getDocComment(annotatedMethod);
    if (javadoc != null) {
      methodSpecBuilder.addJavadoc(javadoc);
    }

    typeSpecBuilder.addMethod(methodSpecBuilder.build());
  }


  private static String toConstName(final String methodName) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, methodName);
  }


  private String getSuperGetterName(final String typeName, final boolean hasDefaultValue) {
    final StringBuilder sb= new StringBuilder("get");

    if (typeName.startsWith("java.util.Optional<")) {
      sb.append("Optional");
    } else if (typeName.startsWith("java.util.Optional")) {
      sb.append(typeName.substring("java.util.".length()));
    } else if (typeName.equals("java.lang.String")) {
      sb.append("String");
    } else if (!typeName.contains(".")) {
      sb.append(upperFirstChar(typeName));
    }

    if (hasDefaultValue) {
      sb.append("OrDefault");
    }

    return sb.toString();
  }


  private static String upperFirstChar(final String s) {
    return String.valueOf(s.charAt(0)).toUpperCase() + s.subSequence(1, s.length());
  }


  private static ClassName toClassName(final String fqName) {
    final int lastDot = fqName.lastIndexOf('.');
    final String packageName;
    final String simpleName;
    if (lastDot > 0) {
      packageName = fqName.substring(0, lastDot);
      simpleName= fqName.substring(lastDot + 1);
    } else {
      packageName= "";
      simpleName= fqName;
    }

    return ClassName.get(packageName, simpleName);
  }


  private static ClassName deriveGeneratedClassName(final TypeElement annotatedInterface) {
    // TODO: Write test for this method
    final ClassName fqInterfaceName= toClassName(annotatedInterface.getQualifiedName().toString());
    final Coat.Config typeAnnotation= annotatedInterface.getAnnotation(Coat.Config.class);

    final String className;
    if (typeAnnotation != null && !typeAnnotation.className().trim().isEmpty()) {
      className= typeAnnotation.className();
    } else {
      if (fqInterfaceName.simpleName().startsWith("_")) {
        className= fqInterfaceName.simpleName().substring(1);
      } else {
        className= "Immutable" + fqInterfaceName.simpleName();
      }
    }

    return ClassName.get(fqInterfaceName.packageName(), className);
  }


  private static ClassName deriveGeneratedEnumName(final TypeElement annotatedInterface) {
    // TODO: Write test for this method
    final ClassName fqInterfaceName= toClassName(annotatedInterface.getQualifiedName().toString());
    final Coat.Config typeAnnotation= annotatedInterface.getAnnotation(Coat.Config.class);

    final String enumName;
    // FIXME: typeAnnotation should never be null here
    if (typeAnnotation != null && typeAnnotation.className() != null && !typeAnnotation.className().trim().isEmpty()) {
      enumName= typeAnnotation.className() + "Param";
    } else {
      if (fqInterfaceName.simpleName().startsWith("_")) {
        enumName= fqInterfaceName.simpleName().substring(1) + "Param";
      } else {
        enumName= fqInterfaceName.simpleName() + "Param";
      }
    }

    return ClassName.get(fqInterfaceName.packageName(), enumName);
  }


  /**
   * Returns the element corresponding to the {@code @Generated} annotation present at the target
   * {@code SourceVersion}.
   * <p>
   * Returns {@code javax.annotation.processing.Generated} for JDK 9 and newer, {@code
   * javax.annotation.Generated} for earlier releases, and null if the annotation is
   * not available.
   * <p>
   * Taken from https://github.com/google/auto/blob/master/common/src/main/java/com/google/auto/common/GeneratedAnnotations.java
   *
   * @return The {@code @Generated} annotation for the target source version or {@code null} if it could not be identified
   */
  private Class<?> identifyGeneratedAnnotation() {
    try {
      final SourceVersion sourceVersion= super.processingEnv.getSourceVersion();
      return sourceVersion.compareTo(SourceVersion.RELEASE_8) > 0
           ? Class.forName("javax.annotation.processing.Generated")
           : Class.forName("javax.annotation.Generated");
    } catch (ClassNotFoundException ex) {
      return null;
    }
  }


  protected static String stripBlockTagsFromJavadoc(final String javadoc) {
    if (javadoc == null) {
      return "";
    }

    final StringBuilder sb= new StringBuilder();

    javadoc.lines()
      .takeWhile(not(PATTERN_JAVADOC_BLOCK_TAG.asMatchPredicate()))
      .map(s -> s + '\n')
      .forEachOrdered(sb::append)
      ;

    return sb.toString();
  }


  private String createExampleContent(final List<Element> annotatedMethods) {
    final StringBuilder sb= new StringBuilder();

    for (final Element annotatedMethod : annotatedMethods) {
      final ConfigParamSpec configParamSpec= ConfigParamSpec.from(annotatedMethod);

      // add javadoc as comment
      final String javadoc = super.processingEnv.getElementUtils().getDocComment(annotatedMethod);
      this.stripBlockTagsFromJavadoc(javadoc)
        .lines()
        .map(s -> "## " + s + "\n")
        .forEachOrdered(sb::append);

      // add a config key
      if (!configParamSpec.mandatory() || (configParamSpec.defaultValue() != null && !configParamSpec.defaultValue().trim().isEmpty())) {
        // commented out if optional or has default value
        sb.append("# ");
      }
      sb.append(configParamSpec.key()).append(" = ");
      sb.append(configParamSpec.defaultValue() != null ? configParamSpec.defaultValue() : "");
      sb.append("\n\n");
    }

    return sb.toString();
  }


  private void generateExampleFile(final String exampleFileName, final TypeElement annotatedInterface, final Filer filer) throws IOException {
    final List<Element> annotatedMethods= annotatedInterface.getEnclosedElements().stream()
      .filter(e -> e.getKind() == ElementKind.METHOD)
      .filter(e -> e.getAnnotation(Coat.Param.class) != null)
      .collect(toList());

    annotatedMethods.addAll(this.getInheritedAnnotatedMethods(annotatedInterface));

    final FileObject resource = filer.createResource(javax.tools.StandardLocation.CLASS_OUTPUT, "examples", exampleFileName, annotatedInterface);
    try (final Writer w= resource.openWriter();) {
      w.write(this.createExampleContent(annotatedMethods));
    }
  }
}
