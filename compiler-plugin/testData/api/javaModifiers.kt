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
// C: ABSTRACT PUBLIC : ABSTRACT PUBLIC
// C.staticStr: PRIVATE : PRIVATE
// C.s1: FINAL JAVA_TRANSIENT : FINAL JAVA_TRANSIENT
// C.i1: JAVA_STATIC JAVA_VOLATILE PROTECTED : JAVA_STATIC JAVA_VOLATILE PROTECTED
// C.NestedC: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// NestedC.<init>: FINAL PUBLIC : FINAL PUBLIC
// C.InnerC: PUBLIC : PUBLIC
// InnerC.<init>: FINAL PUBLIC : FINAL PUBLIC
// C.intFun: JAVA_DEFAULT JAVA_SYNCHRONIZED : JAVA_DEFAULT JAVA_SYNCHRONIZED
// C.foo: ABSTRACT JAVA_STRICT : ABSTRACT JAVA_STRICT
// C.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass: PUBLIC : PUBLIC
// OuterJavaClass.staticPublicField: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// OuterJavaClass.staticPackageProtectedField: JAVA_STATIC : JAVA_STATIC
// OuterJavaClass.staticProtectedField: JAVA_STATIC PROTECTED : JAVA_STATIC PROTECTED
// OuterJavaClass.staticPrivateField: JAVA_STATIC PRIVATE : JAVA_STATIC PRIVATE
// OuterJavaClass.InnerJavaClass: PUBLIC : PUBLIC
// InnerJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass.NestedJavaClass: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// NestedJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass.staticPublicMethod: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// OuterJavaClass.staticPackageProtectedMethod: JAVA_STATIC : JAVA_STATIC
// OuterJavaClass.staticProtectedMethod: JAVA_STATIC PROTECTED : JAVA_STATIC PROTECTED
// OuterJavaClass.staticPrivateMethod: JAVA_STATIC PRIVATE : JAVA_STATIC PRIVATE
// OuterJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass: OPEN : PUBLIC
// OuterKotlinClass.InnerKotlinClass: INNER : FINAL PUBLIC
// InnerKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.NestedKotlinClass: OPEN : PUBLIC
// NestedKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.Companion: : FINAL JAVA_STATIC PUBLIC
// Companion.companionMethod: : FINAL PUBLIC
// Companion.companionField: CONST : FINAL PUBLIC
// Companion.privateCompanionMethod: PRIVATE : FINAL PRIVATE
// Companion.privateCompanionField: PRIVATE : FINAL PRIVATE
// Companion.jvmStaticCompanionMethod: : FINAL JAVA_STATIC PUBLIC
// Companion.jvmStaticCompanionField: : FINAL JAVA_STATIC PUBLIC
// Companion.customJvmStaticCompanionMethod: : FINAL PUBLIC
// Companion.customJvmStaticCompanionField: : FINAL PUBLIC
// Companion.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.transientProperty: : FINAL JAVA_TRANSIENT PUBLIC
// OuterKotlinClass.volatileProperty: : FINAL JAVA_VOLATILE PUBLIC
// OuterKotlinClass.strictfpFun: : FINAL JAVA_STRICT PUBLIC
// OuterKotlinClass.synchronizedFun: : FINAL JAVA_SYNCHRONIZED PUBLIC
// OuterKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass: OPEN PUBLIC : PUBLIC
// DependencyOuterJavaClass.DependencyNestedJavaClass: OPEN PUBLIC : PUBLIC
// DependencyNestedJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass.DependencyInnerJavaClass: INNER OPEN PUBLIC : PUBLIC
// DependencyInnerJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass.synchronizedFun: JAVA_SYNCHRONIZED OPEN : JAVA_SYNCHRONIZED
// DependencyOuterJavaClass.strictfpFun: JAVA_STRICT OPEN : JAVA_STRICT
// DependencyOuterJavaClass.transientField: FINAL JAVA_TRANSIENT : FINAL JAVA_TRANSIENT
// DependencyOuterJavaClass.volatileField: FINAL JAVA_VOLATILE : FINAL JAVA_VOLATILE
// DependencyOuterJavaClass.staticPublicMethod: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// DependencyOuterJavaClass.staticPackageProtectedMethod: JAVA_STATIC : JAVA_STATIC
// DependencyOuterJavaClass.staticProtectedMethod: JAVA_STATIC PROTECTED : JAVA_STATIC PROTECTED
// DependencyOuterJavaClass.staticPrivateMethod: JAVA_STATIC PRIVATE : JAVA_STATIC PRIVATE
// DependencyOuterJavaClass.staticPublicField: FINAL JAVA_STATIC PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterJavaClass.staticPackageProtectedField: FINAL JAVA_STATIC : FINAL JAVA_STATIC
// DependencyOuterJavaClass.staticProtectedField: FINAL JAVA_STATIC PROTECTED : FINAL JAVA_STATIC PROTECTED
// DependencyOuterJavaClass.staticPrivateField: FINAL JAVA_STATIC PRIVATE : FINAL JAVA_STATIC PRIVATE
// DependencyOuterJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass: OPEN PUBLIC : PUBLIC
// DependencyOuterKotlinClass.transientProperty: FINAL PUBLIC : FINAL JAVA_TRANSIENT PUBLIC
// DependencyOuterKotlinClass.volatileProperty: FINAL PUBLIC : FINAL JAVA_VOLATILE PUBLIC
// DependencyOuterKotlinClass.strictfpFun: FINAL PUBLIC : FINAL JAVA_STRICT PUBLIC
// DependencyOuterKotlinClass.synchronizedFun: FINAL PUBLIC : FINAL JAVA_SYNCHRONIZED PUBLIC
// DependencyOuterKotlinClass.Companion: FINAL PUBLIC : FINAL PUBLIC
// Companion.companionField: FINAL PUBLIC : FINAL PUBLIC
// Companion.customJvmStaticCompanionField: FINAL PUBLIC : FINAL PUBLIC
// Companion.jvmStaticCompanionField: FINAL PUBLIC : FINAL PUBLIC
// Companion.privateCompanionField: FINAL PUBLIC : FINAL PUBLIC
// Companion.companionMethod: FINAL PUBLIC : FINAL PUBLIC
// Companion.customJvmStaticCompanionMethod: FINAL PUBLIC : FINAL PUBLIC
// Companion.jvmStaticCompanionMethod: FINAL PUBLIC : FINAL PUBLIC
// Companion.privateCompanionMethod: FINAL PRIVATE : FINAL PRIVATE
// Companion.<init>: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.DependencyInnerKotlinClass: FINAL INNER PUBLIC : FINAL PUBLIC
// DependencyInnerKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyNestedKotlinClass: OPEN PUBLIC : PUBLIC
// DependencyNestedKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
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
    transient String transientField = "";
    volatile String volatileField = "";
    synchronized String synchronizedFun() { return ""; }
    strictfp String strictfpFun() { return ""; }
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

    @Transient
    val transientProperty: String = ""

    @Volatile
    var volatileProperty: String = ""

    @Strictfp
    fun strictfpFun(): String = ""

    @Synchronized
    fun synchronizedFun(): String = ""
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
        const val companionField:String = ""
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

    @Transient
    val transientProperty: String = ""

    @Volatile
    var volatileProperty: String = ""

    @Strictfp
    fun strictfpFun(): String = ""

    @Synchronized
    fun synchronizedFun(): String = ""
}
