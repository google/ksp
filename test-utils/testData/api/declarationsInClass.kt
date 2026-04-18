/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TEST PROCESSOR: DeclarationsInClassProcessor
// EXPECTED:
// CLASS: lib.CustomList (JAVA_LIB)
// FUNCTION: get (JAVA_LIB)
// PROPERTY: size (SYNTHETIC)
// PROPERTY GETTER: (SYNTHETIC)
// FUNCTION: removeAt (JAVA_LIB)
// CONSTRUCTOR: <init> (JAVA_LIB)
// CLASS: lib.CustomMap (JAVA_LIB)
// PROPERTY: entries (SYNTHETIC)
// PROPERTY GETTER: (SYNTHETIC)
// CONSTRUCTOR: <init> (JAVA_LIB)
// CLASS: lib.InterfaceWithObjectMethodOverrides (JAVA_LIB)
// FUNCTION: someMethod (JAVA_LIB)
// END

// MODULE: lib
// FILE: lib/CustomList.java
package lib;

import java.util.AbstractList;

// Kotlin FIR intentionally hides the remove method and replaces it with `removeAt(Int)`.
// As a compiled library (Origin.JAVA_LIB), this tests that KSP correctly identifies
// the collection via FIR and routes it to the AA slow path to avoid this issue.
public class CustomList<E> extends AbstractList<E> {
    @Override
    public E remove(int index) { return null; }

    @Override
    public E get(int index) { return null; }

    @Override
    public int size() { return 0; }
}

// FILE: lib/CustomMap.java
package lib;

import java.util.AbstractMap;
import java.util.Set;

// Kotlin FIR intentionally hides the `entrySet` Java method and replaces it with
// the `entries` property. This tests that KSP correctly identifies this case via
// FIR and routes it to the AA slow path to avoid this issue.
public class CustomMap<K, V> extends AbstractMap<K, V> {
    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }
}

// FILE: lib/InterfaceWithObjectMethodOverrides.java
package lib;

// Kotlin FIR intentionally drops Object methods like `equals` from an interface's
// declarations. This tests that our PSI fast path explicitly filters them out to
// exactly mirror FIR's behavior.
public interface InterfaceWithObjectMethodOverrides {
    void someMethod();
    boolean equals(Object obj);
    int hashCode();
    String toString();
}

// MODULE: main(lib)
