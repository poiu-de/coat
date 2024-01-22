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
package de.poiu.coat.processor.specs;

import java.util.Comparator;


/**
 * Comparator for sorting class specs for generation.
 * It should consider classes containing embedded classes as higher than others.
 * This has the effect that classes that do not contain any embedded configs are generated first.
 */
public class ClassSpecComparator implements Comparator<ClassSpec> {

  public static final ClassSpecComparator INSTANCE= new ClassSpecComparator();

  @Override
  public int compare(final ClassSpec o1, final ClassSpec o2) {
    if (o1 == o2) {
      return 0;
    }

    if (o1 == null) {
      return -1;
    }

    if (o2 == null) {
      return 1;
    }

    final int emb1= o1.embeddedTypes().size();
    final int emb2= o2.embeddedTypes().size();

    if (emb1 == 0 && emb2 == 0) {
      return 0;
    }

    if (emb1 == 0) {
      return -1;
    }

    if (emb2 == 0) {
      return 1;
    }

    final int embDepth1= calcEmbeddedDepth(o1);
    final int embDepth2= calcEmbeddedDepth(o2);

    if (embDepth1 != embDepth2) {
      return embDepth1 - embDepth2;
    } else {
      return o1.className().compareTo(o2.className());
    }
  }


  protected static int calcEmbeddedDepth(final ClassSpec cs) {
    int deepestSubDepth= 1;
    for (final EmbeddedTypeSpec embeddedType : cs.embeddedTypes()) {
      deepestSubDepth=
        Math.max(
          deepestSubDepth,
          calcEmbeddedDepth(embeddedType.classSpec()) + 1);
    }

    return deepestSubDepth;
  }

}
