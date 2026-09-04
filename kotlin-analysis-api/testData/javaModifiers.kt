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
// EXPECT NEXT: C.i1.field: JAVA_STATIC JAVA_VOLATILE PROTECTED : JAVA_STATIC JAVA_VOLATILE PROTECTED
// C.i1: JAVA_STATIC JAVA_VOLATILE PROTECTED : JAVA_STATIC JAVA_VOLATILE PROTECTED
// C.intFun: JAVA_DEFAULT JAVA_SYNCHRONIZED : JAVA_DEFAULT JAVA_SYNCHRONIZED
// EXPECT NEXT: C.s1.field: FINAL JAVA_TRANSIENT : FINAL JAVA_TRANSIENT
// C.s1: FINAL JAVA_TRANSIENT : FINAL JAVA_TRANSIENT
// EXPECT NEXT: C.staticStr.field: PRIVATE : PRIVATE
// C.staticStr: PRIVATE : PRIVATE
// C: ABSTRACT PUBLIC : ABSTRACT PUBLIC
// DependencyOuterJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass.DependencyInnerJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass.DependencyInnerJavaClass: INNER OPEN PUBLIC : PUBLIC
// DependencyOuterJavaClass.DependencyNestedJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterJavaClass.DependencyNestedJavaClass: OPEN PUBLIC : PUBLIC
// EXPECT NEXT: DependencyOuterJavaClass.staticPackageProtectedField.field: FINAL JAVA_STATIC : FINAL JAVA_STATIC
// DependencyOuterJavaClass.staticPackageProtectedField: FINAL JAVA_STATIC : FINAL JAVA_STATIC
// DependencyOuterJavaClass.staticPackageProtectedMethod: FINAL JAVA_STATIC : FINAL JAVA_STATIC
// EXPECT NEXT: DependencyOuterJavaClass.staticPrivateField.field: FINAL JAVA_STATIC PRIVATE : FINAL JAVA_STATIC PRIVATE
// DependencyOuterJavaClass.staticPrivateField: FINAL JAVA_STATIC PRIVATE : FINAL JAVA_STATIC PRIVATE
// DependencyOuterJavaClass.staticPrivateMethod: FINAL JAVA_STATIC PRIVATE : FINAL JAVA_STATIC PRIVATE
// EXPECT NEXT: DependencyOuterJavaClass.staticProtectedField.field: FINAL JAVA_STATIC PROTECTED : FINAL JAVA_STATIC PROTECTED
// DependencyOuterJavaClass.staticProtectedField: FINAL JAVA_STATIC PROTECTED : FINAL JAVA_STATIC PROTECTED
// DependencyOuterJavaClass.staticProtectedMethod: FINAL JAVA_STATIC PROTECTED : FINAL JAVA_STATIC PROTECTED
// EXPECT NEXT: DependencyOuterJavaClass.staticPublicField.field: FINAL JAVA_STATIC PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterJavaClass.staticPublicField: FINAL JAVA_STATIC PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterJavaClass.staticPublicMethod: FINAL JAVA_STATIC PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterJavaClass.synchronizedFun: OPEN : JAVA_SYNCHRONIZED
// EXPECT NEXT: DependencyOuterJavaClass.transientField.field: FINAL : FINAL
// DependencyOuterJavaClass.transientField: FINAL : FINAL JAVA_TRANSIENT
// EXPECT NEXT: DependencyOuterJavaClass.volatileField.field: FINAL : FINAL
// DependencyOuterJavaClass.volatileField: FINAL : FINAL JAVA_VOLATILE
// DependencyOuterJavaClass: OPEN PUBLIC : PUBLIC
// DependencyOuterKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.Companion.<init>: FINAL PRIVATE : FINAL PRIVATE
// EXPECT NEXT: DependencyOuterKotlinClass.Companion.companionField.field: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.Companion.companionField: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.Companion.companionMethod: FINAL PUBLIC : FINAL PUBLIC
// EXPECT NEXT: DependencyOuterKotlinClass.Companion.customJvmStaticCompanionField.field: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.Companion.customJvmStaticCompanionField: FINAL PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterKotlinClass.Companion.customJvmStaticCompanionMethod: FINAL PUBLIC : FINAL JAVA_STATIC PUBLIC
// EXPECT NEXT: DependencyOuterKotlinClass.Companion.jvmStaticCompanionField.field: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.Companion.jvmStaticCompanionField: FINAL PUBLIC : FINAL JAVA_STATIC PUBLIC
// DependencyOuterKotlinClass.Companion.jvmStaticCompanionMethod: FINAL PUBLIC : FINAL JAVA_STATIC PUBLIC
// EXPECT NEXT: DependencyOuterKotlinClass.Companion.privateCompanionField.field: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.Companion.privateCompanionField: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.Companion.privateCompanionMethod: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.Companion: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyInnerKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyInnerKotlinClass: FINAL INNER PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyNestedKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// DependencyOuterKotlinClass.DependencyNestedKotlinClass: OPEN PUBLIC : PUBLIC
// DependencyOuterKotlinClass.synchronizedFun: FINAL PUBLIC : FINAL JAVA_SYNCHRONIZED PUBLIC
// EXPECT NEXT: DependencyOuterKotlinClass.transientProperty.field: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.transientProperty: FINAL PUBLIC : FINAL JAVA_TRANSIENT PUBLIC
// EXPECT NEXT: DependencyOuterKotlinClass.volatileProperty.field: FINAL PRIVATE : FINAL PRIVATE
// DependencyOuterKotlinClass.volatileProperty: FINAL PUBLIC : FINAL JAVA_VOLATILE PUBLIC
// DependencyOuterKotlinClass: OPEN PUBLIC : PUBLIC
// HasTypeAliasFuns: Modifiers: []
// HasTypeAliasFuns: Visibility: PUBLIC
// JavaInterfaceImpl.<init>: FINAL PUBLIC : FINAL PUBLIC
// JavaInterfaceImpl.otherInterfaceMethod: PUBLIC : PUBLIC
// JavaInterfaceImpl: ABSTRACT PUBLIC : ABSTRACT PUBLIC
// OuterJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass.InnerJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass.InnerJavaClass: PUBLIC : PUBLIC
// OuterJavaClass.NestedJavaClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterJavaClass.NestedJavaClass: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// EXPECT NEXT: OuterJavaClass.staticPackageProtectedField.field: JAVA_STATIC : JAVA_STATIC
// OuterJavaClass.staticPackageProtectedField: JAVA_STATIC : JAVA_STATIC
// OuterJavaClass.staticPackageProtectedMethod: JAVA_STATIC : JAVA_STATIC
// EXPECT NEXT: OuterJavaClass.staticPrivateField.field: JAVA_STATIC PRIVATE : JAVA_STATIC PRIVATE
// OuterJavaClass.staticPrivateField: JAVA_STATIC PRIVATE : JAVA_STATIC PRIVATE
// OuterJavaClass.staticPrivateMethod: JAVA_STATIC PRIVATE : JAVA_STATIC PRIVATE
// EXPECT NEXT: OuterJavaClass.staticProtectedField.field: JAVA_STATIC PROTECTED : JAVA_STATIC PROTECTED
// OuterJavaClass.staticProtectedField: JAVA_STATIC PROTECTED : JAVA_STATIC PROTECTED
// OuterJavaClass.staticProtectedMethod: JAVA_STATIC PROTECTED : JAVA_STATIC PROTECTED
// EXPECT NEXT: OuterJavaClass.staticPublicField.field: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// OuterJavaClass.staticPublicField: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// OuterJavaClass.staticPublicMethod: JAVA_STATIC PUBLIC : JAVA_STATIC PUBLIC
// OuterJavaClass: PUBLIC : PUBLIC
// OuterKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.Companion.<init>: FINAL PUBLIC : FINAL PUBLIC
// EXPECT NEXT: OuterKotlinClass.Companion.companionField.field: : FINAL PRIVATE
// OuterKotlinClass.Companion.companionField: CONST : FINAL PUBLIC
// OuterKotlinClass.Companion.companionMethod: : FINAL PUBLIC
// EXPECT NEXT: OuterKotlinClass.Companion.customJvmStaticCompanionField.field: : FINAL PRIVATE
// OuterKotlinClass.Companion.customJvmStaticCompanionField: : FINAL JAVA_STATIC PUBLIC
// OuterKotlinClass.Companion.customJvmStaticCompanionMethod: : FINAL PUBLIC
// EXPECT NEXT: OuterKotlinClass.Companion.jvmStaticCompanionField.field: : FINAL PRIVATE
// OuterKotlinClass.Companion.jvmStaticCompanionField: : FINAL JAVA_STATIC PUBLIC
// OuterKotlinClass.Companion.jvmStaticCompanionMethod: : FINAL JAVA_STATIC PUBLIC
// EXPECT NEXT: OuterKotlinClass.Companion.privateCompanionField.field: : FINAL PRIVATE
// OuterKotlinClass.Companion.privateCompanionField: PRIVATE : FINAL PRIVATE
// OuterKotlinClass.Companion.privateCompanionMethod: PRIVATE : FINAL PRIVATE
// OuterKotlinClass.Companion: : FINAL JAVA_STATIC PUBLIC
// OuterKotlinClass.InnerKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.InnerKotlinClass: INNER : FINAL PUBLIC
// OuterKotlinClass.NestedKotlinClass.<init>: FINAL PUBLIC : FINAL PUBLIC
// OuterKotlinClass.NestedKotlinClass: OPEN : PUBLIC
// OuterKotlinClass.synchronizedFun: : FINAL JAVA_SYNCHRONIZED PUBLIC
// EXPECT CURRENT: OuterKotlinClass.transientProperty: : FINAL JAVA_TRANSIENT PUBLIC
// EXPECT CURRENT: OuterKotlinClass.volatileProperty: : FINAL JAVA_VOLATILE PUBLIC
// EXPECT NEXT: OuterKotlinClass.transientProperty.field: : FINAL JAVA_TRANSIENT PRIVATE
// EXPECT NEXT: OuterKotlinClass.transientProperty: : FINAL PUBLIC
// EXPECT NEXT: OuterKotlinClass.volatileProperty.field: : FINAL JAVA_VOLATILE PRIVATE
// EXPECT NEXT: OuterKotlinClass.volatileProperty: : FINAL PUBLIC
// OuterKotlinClass: OPEN : PUBLIC
// TypeAliasInKt: Modifiers: [PRIVATE]
// TypeAliasInKt: Visibility: PRIVATE
// TypeAliasInLib: Modifiers: [FINAL, PUBLIC]
// TypeAliasInLib: Visibility: PUBLIC
// END
// MODULE: module1
// FILE: ALib.kt
fun interface ALib {
    fun test(): Boolean
}

public typealias TypeAliasInLib = Int

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

    @Synchronized
    fun synchronizedFun(): String = ""
}
// MODULE: main(module1)
// FILE: ASrc.kt
private typealias TypeAliasInKt = Int

class HasTypeAliasFuns {
    fun FunReturnTA1(): TypeAliasInKt = 0
    fun FunReturnTA2(): TypeAliasInLib = 0
}

fun interface ASrc {
    fun test(): Boolean
}
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

@Test
class JavaInterfaceDependency : JavaInterfaceImpl {

// FILE: C.java

public abstract class C {

    private String staticStr = "str"

    final transient String s1;

    protected static volatile int i1;

    default synchronized int intFun() {
        return 1;
    }

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

    @Synchronized
    fun synchronizedFun(): String = ""
}

// FILE: JavaInterface.java
public interface JavaInterface {
    void interfaceMethod();
    void otherInterfaceMethod();
}

// FILE: JavaInterfaceImpl.java
public abstract class JavaInterfaceImpl implements JavaInterface {
    @Override
    public void otherInterfaceMethod() {}
}
