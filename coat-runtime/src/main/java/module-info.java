module de.poiu.coat.runtime {
  requires transitive java.compiler;
  
  requires static org.immutables.value;

  exports de.poiu.coat;
  exports de.poiu.coat.annotation;
  exports de.poiu.coat.c14n;
  exports de.poiu.coat.casing;
  exports de.poiu.coat.convert;
  exports de.poiu.coat.validation;
}
