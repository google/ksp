/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// CLASS: lib.AbstractClassWithObjectMethodOverrides (JAVA_LIB)
// CLASS: lib.BaseWithCapitalizedProperties (KOTLIN_LIB)
// CLASS: lib.BaseWithIsProperties (KOTLIN_LIB)
// CLASS: lib.BaseWithProperties (KOTLIN_LIB)
// CLASS: lib.ConcreteClassWithObjectMethodOverrides (JAVA_LIB)
// CLASS: lib.CustomList (JAVA_LIB)
// CLASS: lib.CustomMap (JAVA_LIB)
// CLASS: lib.InterfaceWithNonObjectMethodOverrides (JAVA_LIB)
// CLASS: lib.InterfaceWithObjectMethodOverrides (JAVA_LIB)
// CLASS: lib.OverridesBaseWithIsProperties (JAVA_LIB)
// CLASS: lib.OverridesBaseWithProperties (JAVA_LIB)
// CLASS: lib.StaticVsMemberDeclarations (JAVA_LIB)
// CLASS: lib.StaticVsMemberDeclarations.InnerC (JAVA_LIB)
// CLASS: lib.StaticVsMemberDeclarations.NestedC (JAVA_LIB)
// CONSTRUCTOR: AbstractClassWithObjectMethodOverrides.<init> (JAVA_LIB)
// CONSTRUCTOR: ConcreteClassWithObjectMethodOverrides.<init> (JAVA_LIB)
// CONSTRUCTOR: CustomList.<init> (JAVA_LIB)
// CONSTRUCTOR: CustomMap.<init> (JAVA_LIB)
// CONSTRUCTOR: InnerC.<init> (JAVA_LIB)
// CONSTRUCTOR: NestedC.<init> (JAVA_LIB)
// CONSTRUCTOR: OverridesBaseWithIsProperties.<init> (JAVA_LIB)
// CONSTRUCTOR: OverridesBaseWithProperties.<init> (JAVA_LIB)
// CONSTRUCTOR: StaticVsMemberDeclarations.<init> (JAVA_LIB)
// FUNCTION: AbstractClassWithObjectMethodOverrides.equals (JAVA_LIB)
// FUNCTION: AbstractClassWithObjectMethodOverrides.hashCode (JAVA_LIB)
// FUNCTION: AbstractClassWithObjectMethodOverrides.toString (JAVA_LIB)
// FUNCTION: ConcreteClassWithObjectMethodOverrides.equals (JAVA_LIB)
// FUNCTION: ConcreteClassWithObjectMethodOverrides.hashCode (JAVA_LIB)
// FUNCTION: ConcreteClassWithObjectMethodOverrides.toString (JAVA_LIB)
// FUNCTION: CustomList.get (JAVA_LIB)
// FUNCTION: CustomList.removeAt (JAVA_LIB)
// FUNCTION: CustomList.size (SYNTHETIC)
// FUNCTION: CustomMap.entrySet (SYNTHETIC)
// FUNCTION: InterfaceWithNonObjectMethodOverrides.equals (JAVA_LIB)
// FUNCTION: InterfaceWithNonObjectMethodOverrides.someMethod (JAVA_LIB)
// FUNCTION: OverridesBaseWithIsProperties.getFoo (SYNTHETIC)
// FUNCTION: OverridesBaseWithIsProperties.isBar (SYNTHETIC)
// FUNCTION: OverridesBaseWithProperties.getFoo (SYNTHETIC)
// FUNCTION: OverridesBaseWithProperties.getGetBar (SYNTHETIC)
// FUNCTION: OverridesBaseWithProperties.getNonProperty (JAVA_LIB)
// GETTER: BaseWithCapitalizedProperties.Foo (KOTLIN_LIB)
// GETTER: BaseWithIsProperties.foo (KOTLIN_LIB)
// GETTER: BaseWithIsProperties.isBar (KOTLIN_LIB)
// GETTER: BaseWithProperties.foo (KOTLIN_LIB)
// GETTER: BaseWithProperties.getBar (KOTLIN_LIB)
// PROPERTY: BaseWithCapitalizedProperties.Foo (KOTLIN_LIB)
// PROPERTY: BaseWithIsProperties.foo (KOTLIN_LIB)
// PROPERTY: BaseWithIsProperties.isBar (KOTLIN_LIB)
// PROPERTY: BaseWithProperties.foo (KOTLIN_LIB)
// PROPERTY: BaseWithProperties.getBar (KOTLIN_LIB)
// PROPERTY: InnerC.staticString (JAVA_LIB)
// PROPERTY: InnerC.str (JAVA_LIB)
// PROPERTY: NestedC.staticString (JAVA_LIB)
// PROPERTY: NestedC.str (JAVA_LIB)
// PROPERTY: StaticVsMemberDeclarations.staticString (JAVA_LIB)
// PROPERTY: StaticVsMemberDeclarations.str (JAVA_LIB)
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
    boolean equals(Object obj);
    int hashCode();
    String toString();
}

// FILE: lib/InterfaceWithNonObjectMethodOverrides.java
package lib;

public interface InterfaceWithNonObjectMethodOverrides {
    void someMethod();
    boolean equals(String str);
}

// FILE: lib/AbstractClassWithObjectMethodOverrides.java
package lib;

public abstract class AbstractClassWithObjectMethodOverrides {
    public abstract boolean equals(Object obj);
    public abstract int hashCode();
    public abstract String toString();
}

// FILE: lib/ConcreteClassWithObjectMethodOverrides.java
package lib;

public class ConcreteClassWithObjectMethodOverrides {
    public boolean equals(Object obj) {
        return false;
    }
    public int hashCode() {
        return 0;
    }
    public String toString() {
        return "";
    }
}

// FILE: lib/BaseWithProperties.kt
package lib

interface BaseWithProperties {
    // Test overriding conventional property getter
    val foo: String

    // Test overriding getter-like property getter
    val getBar: String
}

// FILE: lib/OverridesBaseWithProperties.java
package lib;

public class OverridesBaseWithProperties implements BaseWithProperties {
    // Test getter that doesn't override a property
    public String getNonProperty() {
        return "";
    }

    // Overrides val foo
    @Override
    public String getFoo() {
        return "";
    }

    // Overrides val getBar
    @Override
    public String getGetBar() {
        return "";
    }
}

// FILE: lib/BaseWithIsProperties.kt
package lib

interface BaseWithIsProperties {
    val foo: String

    // Test overriding is-like property getter
    val isBar: Boolean
}

// FILE: lib/OverridesBaseWithIsProperties.java
package lib;

public class OverridesBaseWithIsProperties implements BaseWithIsProperties {
    // Overrides val foo
    @Override
    public String getFoo() {
        return "";
    }

    // Overrides val isBar
    @Override
    public boolean isBar() {
        return true;
    }
}

// FILE: lib/BaseWithCapitalizedProperties.kt
package lib

interface BaseWithCapitalizedProperties {
    // Test overriding capitalized property getter
    val Foo: String
}

// FILE: lib/OverridesBaseWithCapitalizedProperties.java
package lib;

public class OverridesBaseWithCapitalizedProperties implements BaseWithCapitalizedProperties {
    @Override
    public String getFoo() {
        return "Foo";
    }
}

// FILE: lib/StaticVsMemberDeclarations.java
package lib;

public class StaticVsMemberDeclarations {
    String str = "str";
    static String staticString = "staticString";

    public static class NestedC {
        String str = "str";
        static String staticString = "staticString";
    }

    public class InnerC {
        String str = "str";
        static String staticString = "staticString";
    }
}

// MODULE: main(lib)
