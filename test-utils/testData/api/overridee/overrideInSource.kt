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
// END

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
