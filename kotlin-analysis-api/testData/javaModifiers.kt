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

// TEST PROCESSOR: JavaModifierProcessor
// EXPECTED:
// C.<init>: FINAL PUBLIC : FINAL PUBLIC
// C.InnerC.<init>: FINAL PUBLIC : FINAL PUBLIC
// C.InnerC: PUBLIC : PUBLIC
// C.NestedC.<init>: FINAL PUBLIC : FINAL PUBLIC
// C.NestedC: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// C.foo: ABSTRACT JAVA_STRICT : ABSTRACT JAVA_STRICT
// C.i1: JAVA_STATIC JAVA_VOLATILE PROTECTED : JAVA_STATIC JAVA_VOLATILE PROTECTED
// C.intFun: JAVA_DEFAULT JAVA_SYNCHRONIZED : JAVA_DEFAULT JAVA_SYNCHRONIZED
// C.s1: FINAL JAVA_TRANSIENT : FINAL JAVA_TRANSIENT
// C.staticStr: PRIVATE : PRIVATE
// C: ABSTRACT PUBLIC : ABSTRACT PUBLIC
// DependencyOuterJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass.DependencyInnerJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass.DependencyInnerJavaClass: INNER OPEN PUBLIC : PUBLIC
// DependencyOuterJavaClass.DependencyNestedJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass.DependencyNestedJavaClass: OPEN PUBLIC : PUBLIC
// DependencyOuterJavaClass.staticPackageProtectedField: FINAL JAVA_STATIC : FINAL JAVA_STATIC
// DependencyOuterJavaClass.staticPackageProtectedMethod: FINAL JAVA_STATIC : FINAL JAVA_STATIC
// DependencyOuterJavaClass.staticPrivateField: FINAL JAVA_STATIC PRIVATE : FINAL JAVA_STATIC PRIVATE
// DependencyOuterJavaClass.staticPrivateMethod: FINAL JAVA_STATIC PRIVATE : FINAL JAVA_STATIC PRIVATE
// DependencyOuterJavaClass.staticProtectedField: FINAL JAVA_STATIC PROTECTED : FINAL JAVA_STATIC PROTECTED
// DependencyOuterJavaClass.staticProtectedMethod: FINAL JAVA_STATIC PROTECTED : FINAL JAVA_STATIC PROTECTED
// DependencyOuterJavaClass.staticPublicField: FINAL JAVA_STATIC PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterJavaClass.staticPublicMethod: FINAL JAVA_STATIC PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterJavaClass.strictfpFun: OPEN : JAVA_STRICT
// DependencyOuterJavaClass.synchronizedFun: OPEN : JAVA_SYNCHRONIZED
// DependencyOuterJavaClass.transientField: OPEN : JAVA_TRANSIENT
// DependencyOuterJavaClass.volatileField: OPEN : JAVA_VOLATILE
// DependencyOuterJavaClass: OPEN PUBLIC : PUBLIC
// DependencyOuterKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.Companion.<init>: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.Companion.companionField: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.Companion.companionMethod: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.Companion.customJvmStaticCompanionField: FINAL PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterKotlinClass.Companion.customJvmStaticCompanionMethod: FINAL PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterKotlinClass.Companion.jvmStaticCompanionField: FINAL PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterKotlinClass.Companion.jvmStaticCompanionMethod: FINAL PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterKotlinClass.Companion.privateCompanionField: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.Companion.privateCompanionMethod: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.Companion: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyInnerKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyInnerKotlinClass: FINAL INNER PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyNestedKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyNestedKotlinClass: OPEN PUBLIC : PUBLIC
// DependencyOuterKotlinClass.strictfpFun: FINAL PUBLIC : FINAL JAVA_STRICT PUBLIC
// DependencyOuterKotlinClass.synchronizedFun: FINAL PUBLIC : FINAL JAVA_SYNCHRONIZED PUBLIC
// DependencyOuterKotlinClass.transientProperty: FINAL PUBLIC : FINAL JAVA_TRANSIENT PUBLIC
// DependencyOuterKotlinClass.volatileProperty: FINAL PUBLIC : FINAL JAVA_VOLATILE PUBLIC
// DependencyOuterKotlinClass: OPEN PUBLIC : PUBLIC
// OuterJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass.InnerJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass.InnerJavaClass: PUBLIC : PUBLIC
// OuterJavaClass.NestedJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass.NestedJavaClass: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// OuterJavaClass.staticPackageProtectedField: JAVA_STATIC : JAVA_STATIC
// OuterJavaClass.staticPackageProtectedMethod: JAVA_STATIC : JAVA_STATIC
// OuterJavaClass.staticPrivateField: JAVA_STATIC PRIVATE : JAVA_STATIC PRIVATE
// OuterJavaClass.staticPrivateMethod: JAVA_STATIC PRIVATE : JAVA_STATIC PRIVATE
// OuterJavaClass.staticProtectedField: JAVA_STATIC PROTECTED : JAVA_STATIC PROTECTED
// OuterJavaClass.staticProtectedMethod: JAVA_STATIC PROTECTED : JAVA_STATIC PROTECTED
// OuterJavaClass.staticPublicField: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// OuterJavaClass.staticPublicMethod: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// OuterJavaClass: PUBLIC : PUBLIC
// OuterKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.Companion.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.Companion.companionField: CONST : FINAL PUBLIC
// OuterKotlinClass.Companion.companionMethod: : FINAL PUBLIC
// OuterKotlinClass.Companion.customJvmStaticCompanionField: : FINAL PUBLIC
// OuterKotlinClass.Companion.customJvmStaticCompanionMethod: : FINAL PUBLIC
// OuterKotlinClass.Companion.jvmStaticCompanionField: : FINAL PUBLIC
// OuterKotlinClass.Companion.jvmStaticCompanionMethod: : FINAL PUBLIC
// OuterKotlinClass.Companion.privateCompanionField: PRIVATE : FINAL PRIVATE
// OuterKotlinClass.Companion.privateCompanionMethod: PRIVATE : FINAL PRIVATE
// OuterKotlinClass.Companion: : FINAL JAVA_STATIC PUBLIC
// OuterKotlinClass.InnerKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.InnerKotlinClass: INNER : FINAL PUBLIC
// OuterKotlinClass.NestedKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.NestedKotlinClass: OPEN : PUBLIC
// OuterKotlinClass.strictfpFun: : FINAL JAVA_STRICT PUBLIC
// OuterKotlinClass.synchronizedFun: : FINAL JAVA_SYNCHRONIZED PUBLIC
// OuterKotlinClass.transientProperty: : FINAL JAVA_TRANSIENT PUBLIC
// OuterKotlinClass.volatileProperty: : FINAL JAVA_VOLATILE PUBLIC
// OuterKotlinClass: OPEN : PUBLIC
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
