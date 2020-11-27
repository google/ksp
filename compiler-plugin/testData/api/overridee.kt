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

// WITH_RUNTIME
// TEST PROCESSOR: OverrideeProcessor
// EXPECTED:
// Subject:
// Companion.companionMethod() -> null
// Subject.notOverridingProp -> null
// Subject.overriddenBaseProp -> Base.overriddenBaseProp
// Subject.overriddenAbstractBaseProp -> Base.overriddenAbstractBaseProp
// Subject.openGrandBaseProp -> GrandBase.openGrandBaseProp
// Subject.abstractGrandBaseProp -> GrandBase.abstractGrandBaseProp
// Subject.overriddenGrandBaseProp -> Base.overriddenGrandBaseProp
// Subject.overriddenAbstractGrandBaseProp -> Base.overriddenAbstractGrandBaseProp
// Subject.openFun() -> Base.openFun()
// Subject.abstractFun() -> Base.abstractFun()
// Subject.openFunWithGenericArg(t:String) -> Base.openFunWithGenericArg(t:T)
// Subject.abstractFunWithGenericArg(t:String) -> Base.abstractFunWithGenericArg(t:T)
// Subject.nonOverridingMethod() -> null
// Subject.overriddenGrandBaseFun() -> Base.overriddenGrandBaseFun()
// Subject.overriddenAbstractGrandBaseFun() -> Base.overriddenAbstractGrandBaseFun()
// Subject.openGrandBaseFun() -> GrandBase.openGrandBaseFun()
// Subject.abstractGrandBaseFun() -> GrandBase.abstractGrandBaseFun()
// JavaSubject.Subject:
// Subject.openFun() -> Base.openFun()
// Subject.abstractFun() -> Base.abstractFun()
// Subject.openFunWithGenericArg(t:String) -> Base.openFunWithGenericArg(t:T)
// Subject.abstractFunWithGenericArg(t:String) -> Base.abstractFunWithGenericArg(t:T)
// Subject.nonOverridingMethod() -> null
// Subject.overriddenGrandBaseFun() -> Base.overriddenGrandBaseFun()
// Subject.overriddenAbstractGrandBaseFun() -> Base.overriddenAbstractGrandBaseFun()
// Subject.openGrandBaseFun() -> GrandBase.openGrandBaseFun()
// Subject.abstractGrandBaseFun() -> GrandBase.abstractGrandBaseFun()
// Subject.staticMethod() -> null
// lib.Subject:
// Companion.companionMethod() -> null
// Subject.abstractGrandBaseProp -> GrandBase.abstractGrandBaseProp
// Subject.notOverridingProp -> null
// Subject.openGrandBaseProp -> GrandBase.openGrandBaseProp
// Subject.overriddenAbstractBaseProp -> Base.overriddenAbstractBaseProp
// Subject.overriddenAbstractGrandBaseProp -> Base.overriddenAbstractGrandBaseProp
// Subject.overriddenBaseProp -> Base.overriddenBaseProp
// Subject.overriddenGrandBaseProp -> Base.overriddenGrandBaseProp
// Subject.abstractFun() -> Base.abstractFun()
// Subject.abstractFunWithGenericArg(t:String) -> Base.abstractFunWithGenericArg(t:T)
// Subject.abstractGrandBaseFun() -> GrandBase.abstractGrandBaseFun()
// Subject.nonOverridingMethod() -> null
// Subject.openFun() -> Base.openFun()
// Subject.openFunWithGenericArg(t:String) -> Base.openFunWithGenericArg(t:T)
// Subject.openGrandBaseFun() -> GrandBase.openGrandBaseFun()
// Subject.overriddenAbstractGrandBaseFun() -> Base.overriddenAbstractGrandBaseFun()
// Subject.overriddenGrandBaseFun() -> Base.overriddenGrandBaseFun()
// ConflictingSubject1:
// ConflictingSubject1.absFoo() -> MyInterface.absFoo()
// ConflictingSubject2:
// ConflictingSubject2.absFoo() -> MyAbstract.absFoo()
// ConflictingSubject3:
// ConflictingSubject3.absFoo() -> MyInterface.absFoo()
// ConflictingSubject4:
// ConflictingSubject4.absFoo() -> MyInterface2.absFoo()
// OverrideOrder1:
// OverrideOrder1.foo() -> GrandBaseInterface2.foo()
// OverrideOrder2:
// OverrideOrder2.foo() -> GrandBaseInterface1.foo()
// END
// MODULE: lib
// FILE: lib.kt
package lib;
abstract class GrandBase {
    open var openGrandBaseProp: Int = 0
    abstract var abstractGrandBaseProp: Int
    open var overriddenGrandBaseProp: Int = 0
    abstract var overriddenAbstractGrandBaseProp: Int
    open fun openGrandBaseFun() {}
    abstract fun abstractGrandBaseFun()
    open fun overriddenGrandBaseFun() {}
    abstract fun overriddenAbstractGrandBaseFun()
}
abstract class Base<T> : GrandBase() {
    open var overriddenBaseProp: Int = 0
    abstract var overriddenAbstractBaseProp: Int
    override var overriddenGrandBaseProp:Int = 0
    override var overriddenAbstractGrandBaseProp: Int = 0
    open fun openFun() {}
    abstract fun abstractFun():Unit
    open fun openFunWithGenericArg(t:T):T = TODO()
    abstract fun abstractFunWithGenericArg(t:T):T
    override open fun overriddenGrandBaseFun() {}
    override open fun overriddenAbstractGrandBaseFun() {}
}

abstract class Subject: Base<String>() {
    var notOverridingProp: Int = 0
    override open var overriddenBaseProp: Int = 0
    override var overriddenAbstractBaseProp: Int = 0
    override open var openGrandBaseProp: Int = 0
    override var abstractGrandBaseProp: Int = 0
    override var overriddenGrandBaseProp:Int = 0
    override var overriddenAbstractGrandBaseProp: Int = 0
    override fun openFun() {}
    override fun abstractFun() {}
    override fun openFunWithGenericArg(t:String):String = TODO()
    override fun abstractFunWithGenericArg(t:String):String = TODO()
    fun nonOverridingMethod(): String =TODO()
    override fun overriddenGrandBaseFun() {}
    override fun overriddenAbstractGrandBaseFun() {}
    override fun openGrandBaseFun() {}
    override fun abstractGrandBaseFun() {}
    companion object {
        fun companionMethod(): String =TODO()
    }
}
// MODULE: main(lib)
// FILE: a.kt
abstract class GrandBase {
    open var openGrandBaseProp: Int = 0
    abstract var abstractGrandBaseProp: Int = 0
    open var overriddenGrandBaseProp: Int = 0
    abstract var overriddenAbstractGrandBaseProp: Int = 0
    open fun openGrandBaseFun() {}
    abstract fun abstractGrandBaseFun()
    open fun overriddenGrandBaseFun() {}
    abstract fun overriddenAbstractGrandBaseFun()
}
abstract class Base<T> : GrandBase() {
    open var overriddenBaseProp: Int = 0
    var overriddenAbstractBaseProp: Int = 0
    override var overriddenGrandBaseProp:Int = 0
    override var overriddenAbstractGrandBaseProp: Int = 0
    open fun openFun() {}
    abstract fun abstractFun():Unit
    open fun openFunWithGenericArg(t:T):T = TODO()
    abstract fun abstractFunWithGenericArg(t:T):T
    override open fun overriddenGrandBaseFun() {}
    override open fun overriddenAbstractGrandBaseFun() {}
}

abstract class Subject: Base<String>() {
    var notOverridingProp: Int = 0
    override open var overriddenBaseProp: Int = 0
    override var overriddenAbstractBaseProp: Int = 0
    override open var openGrandBaseProp: Int = 0
    override var abstractGrandBaseProp: Int = 0
    override var overriddenGrandBaseProp:Int = 0
    override var overriddenAbstractGrandBaseProp: Int = 0
    override fun openFun() {}
    override fun abstractFun() {}
    override fun openFunWithGenericArg(t:String):String = TODO()
    override fun abstractFunWithGenericArg(t:String):String = TODO()
    fun nonOverridingMethod(): String =TODO()
    override fun overriddenGrandBaseFun() {}
    override fun overriddenAbstractGrandBaseFun() {}
    override fun openGrandBaseFun() {}
    override fun abstractGrandBaseFun() {}
    companion object {
        fun companionMethod(): String =TODO()
    }
}

// FILE: conflictingOverrides.kt
interface MyInterface {
    fun absFoo(): Unit
}

interface MyInterface2 {
    fun absFoo(): Unit
}

abstract class MyAbstract: MyInterface {
    override fun absFoo(): Unit {val a = 1}
}

class ConflictingSubject1: MyInterface, MyAbstract() {
    override fun absFoo(): Unit = TODO()
}

class ConflictingSubject2: MyAbstract(), MyInterface {
    override fun absFoo(): Unit = TODO()
}

class ConflictingSubject3: MyInterface, MyInterface2 {
    override fun absFoo(): Unit = TODO()
}

class ConflictingSubject4: MyInterface2, MyInterface {
    override fun absFoo(): Unit = TODO()
}

// FILE: overrideOrder.kt
interface GrandBaseInterface1 {
    fun foo(): Unit
}

interface GrandBaseInterface2 {
    fun foo(): Unit
}

interface BaseInterface1 : GrandBaseInterface1 {
}

interface BaseInterface2 : GrandBaseInterface2 {
}

class OverrideOrder1 : BaseInterface1, GrandBaseInterface2 {
    override fun foo() = TODO()
}
class OverrideOrder2 : BaseInterface2, GrandBaseInterface1 {
    override fun foo() = TODO()
}

// FILE: JavaSubject.java
public class JavaSubject {
    static abstract class GrandBase {
        void openGrandBaseFun() {}
        abstract void abstractGrandBaseFun();
        void overriddenGrandBaseFun() {}
        abstract void overriddenAbstractGrandBaseFun();
    }
    static abstract class Base<T> extends GrandBase {
        void openFun() {}
        abstract void abstractFun();
        T openFunWithGenericArg(T t) {
            return null;
        }
        abstract T abstractFunWithGenericArg(T t);
        void overriddenGrandBaseFun() {}
        void overriddenAbstractGrandBaseFun() {}
    }

    static abstract class Subject extends Base<String> {
        void openFun() {}
        void abstractFun() {}
        String openFunWithGenericArg(String t) {
            return null;
        }
        String abstractFunWithGenericArg(String t) {
            return null;
        }
        String nonOverridingMethod() {
            return null;
        }
        void overriddenGrandBaseFun() {}
        void overriddenAbstractGrandBaseFun() {}
        void openGrandBaseFun() {}
        void abstractGrandBaseFun() {}
        static String staticMethod() {
            return null;
        }
    }
}