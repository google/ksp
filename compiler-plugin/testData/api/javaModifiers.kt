/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: JavaModifierProcessor
// EXPECTED:
// C: PUBLIC ABSTRACT
// C.staticStr: PRIVATE
// C.s1: FINAL JAVA_TRANSIENT
// C.i1: PROTECTED JAVA_STATIC JAVA_VOLATILE
// C.NestedC: PUBLIC JAVA_STATIC
// NestedC.<init>: FINAL PUBLIC
// C.InnerC: PUBLIC
// InnerC.<init>: FINAL PUBLIC
// C.intFun: JAVA_SYNCHRONIZED JAVA_DEFAULT
// C.foo: ABSTRACT JAVA_STRICT
// C.<init>: FINAL PUBLIC
// OuterJavaClass: PUBLIC
// OuterJavaClass.staticPublicField: PUBLIC JAVA_STATIC
// OuterJavaClass.staticPackageProtectedField: JAVA_STATIC
// OuterJavaClass.staticProtectedField: PROTECTED JAVA_STATIC
// OuterJavaClass.staticPrivateField: PRIVATE JAVA_STATIC
// OuterJavaClass.InnerJavaClass: PUBLIC
// InnerJavaClass.<init>: FINAL PUBLIC
// OuterJavaClass.NestedJavaClass: PUBLIC JAVA_STATIC
// NestedJavaClass.<init>: FINAL PUBLIC
// OuterJavaClass.staticPublicMethod: PUBLIC JAVA_STATIC
// OuterJavaClass.staticPackageProtectedMethod: JAVA_STATIC
// OuterJavaClass.staticProtectedMethod: PROTECTED JAVA_STATIC
// OuterJavaClass.staticPrivateMethod: PRIVATE JAVA_STATIC
// OuterJavaClass.<init>: FINAL PUBLIC
// OuterKotlinClass: OPEN
// OuterKotlinClass.InnerKotlinClass: INNER
// InnerKotlinClass.<init>: FINAL PUBLIC
// OuterKotlinClass.NestedKotlinClass: OPEN
// NestedKotlinClass.<init>: FINAL PUBLIC
// OuterKotlinClass.Companion:
// Companion.companionMethod:
// Companion.companionField:
// Companion.privateCompanionMethod: PRIVATE
// Companion.privateCompanionField: PRIVATE
// Companion.jvmStaticCompanionMethod: JAVA_STATIC
// Companion.jvmStaticCompanionField: JAVA_STATIC
// Companion.customJvmStaticCompanionMethod: JAVA_STATIC
// Companion.customJvmStaticCompanionField: JAVA_STATIC
// Companion.<init>: FINAL PUBLIC
// OuterKotlinClass.<init>: FINAL PUBLIC
// DependencyOuterJavaClass: OPEN PUBLIC
// DependencyOuterJavaClass.DependencyNestedJavaClass: OPEN PUBLIC
// DependencyNestedJavaClass.<init>: FINAL PUBLIC
// DependencyOuterJavaClass.DependencyInnerJavaClass: OPEN PUBLIC INNER
// DependencyInnerJavaClass.<init>: FINAL PUBLIC
// DependencyOuterJavaClass.staticPublicMethod: JAVA_STATIC PUBLIC
// DependencyOuterJavaClass.staticPackageProtectedMethod: JAVA_STATIC
// DependencyOuterJavaClass.staticProtectedMethod: JAVA_STATIC PROTECTED
// DependencyOuterJavaClass.staticPrivateMethod: JAVA_STATIC PRIVATE
// DependencyOuterJavaClass.staticPublicField: JAVA_STATIC FINAL PUBLIC
// DependencyOuterJavaClass.staticPackageProtectedField: JAVA_STATIC FINAL
// DependencyOuterJavaClass.staticProtectedField: JAVA_STATIC FINAL PROTECTED
// DependencyOuterJavaClass.staticPrivateField: JAVA_STATIC FINAL PRIVATE
// DependencyOuterJavaClass.<init>: FINAL PUBLIC
// DependencyOuterKotlinClass: OPEN PUBLIC
// DependencyOuterKotlinClass.Companion: FINAL PUBLIC
// Companion.companionField: FINAL PUBLIC
// Companion.customJvmStaticCompanionField: JAVA_STATIC FINAL PUBLIC
// Companion.jvmStaticCompanionField: JAVA_STATIC FINAL PUBLIC
// Companion.privateCompanionField: FINAL PUBLIC
// Companion.companionMethod: FINAL PUBLIC
// Companion.customJvmStaticCompanionMethod: JAVA_STATIC FINAL PUBLIC
// Companion.jvmStaticCompanionMethod: JAVA_STATIC FINAL PUBLIC
// Companion.privateCompanionMethod: FINAL PRIVATE
// Companion.<init>: FINAL PRIVATE
// DependencyOuterKotlinClass.DependencyInnerKotlinClass: FINAL PUBLIC INNER
// DependencyInnerKotlinClass.<init>: FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyNestedKotlinClass: OPEN PUBLIC
// DependencyNestedKotlinClass.<init>: FINAL PUBLIC
// DependencyOuterKotlinClass.<init>: FINAL PUBLIC
// END
// MODULE: module1
// FILE: DependencyOuterJavaClass.java
public class DependencyOuterJavaClass {
    public class DependencyInnerJavaClass {}
    public static class DependencyNestedJavaClass {}
    public static void staticPublicMethod() {}
    public static String staticPublicField;
    static void staticPackageProtectedMethod() {}
    static String staticPackageProtectedField;
    protected static void staticProtectedMethod() {}
    protected static String staticProtectedField;
    private static void staticPrivateMethod() {}
    private static String staticPrivateField;
}
// FILE: DependencyOuterKotlinClass.kt
typealias DependencyCustomJvmStatic=JvmStatic
open class DependencyOuterKotlinClass {
    inner class DependencyInnerKotlinClass
    open class DependencyNestedKotlinClass
    companion object {
        fun companionMethod() {}
        val companionField:String = ""
        private fun privateCompanionMethod() {}
        val privateCompanionField:String = ""
        @JvmStatic
        fun jvmStaticCompanionMethod() {}
        @JvmStatic
        val jvmStaticCompanionField:String = ""
        @DependencyCustomJvmStatic
        fun customJvmStaticCompanionMethod() {}
        @DependencyCustomJvmStatic
        val customJvmStaticCompanionField:String = ""
    }
}
// MODULE: main(module1)
// FILE: a.kt
annotation class Test

@Test
class Foo : C() {

}

@Test
class Bar : OuterJavaClass()

@Test
class Baz : OuterKotlinClass()

@Test
class JavaDependency : DependencyOuterJavaClass()

@Test
class KotlinDependency : DependencyOuterKotlinClass()

// FILE: C.java

public abstract class C {

    private String staticStr = "str"

    final transient String s1;

    protected static volatile int i1;

    default synchronized int intFun() {
        return 1;
    }

    abstract strictfp void foo() {}

    public static class NestedC {

    }

    public class InnerC {

    }
}

// FILE: OuterJavaClass.java
public class OuterJavaClass {
    public class InnerJavaClass {}
    public static class NestedJavaClass {}
    public static void staticPublicMethod() {}
    public static String staticPublicField;
    static void staticPackageProtectedMethod() {}
    static String staticPackageProtectedField;
    protected static void staticProtectedMethod() {}
    protected static String staticProtectedField;
    private static void staticPrivateMethod() {}
    private static String staticPrivateField;
}
// FILE: OuterKotlinClass.kt
typealias CustomJvmStatic=JvmStatic
open class OuterKotlinClass {
    inner class InnerKotlinClass
    open class NestedKotlinClass
    companion object {
        fun companionMethod() {}
        val companionField:String = ""
        private fun privateCompanionMethod() {}
        private val privateCompanionField:String = ""
        @JvmStatic
        fun jvmStaticCompanionMethod() {}
        @JvmStatic
        val jvmStaticCompanionField:String = ""
        @CustomJvmStatic
        fun customJvmStaticCompanionMethod() {}
        @CustomJvmStatic
        val customJvmStaticCompanionField:String = ""
    }
}
