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
// TEST PROCESSOR: MangledNamesProcessor
// EXPECTED:
// get-value -> getValue
// get-name -> getName
// get-inlineProp -> getInlineProp-uW2R4Lc
// get-internalProp -> getInternalProp$myModuleName
// get-internalInlineProp -> getInternalInlineProp-uW2R4Lc$myModuleName
// normalFun -> normalFun
// inlineReceivingFun -> inlineReceivingFun-9XlVjhY
// inlineReturningFun -> inlineReturningFun-uW2R4Lc
// END
// MODULE: lib
// FILE: input.kt
/**
 * control group
 */
package foo.bar;
inline class Inline1(val value:String)
class Foo {
    val name:String = TODO()
    val inlineProp: Inline1 = TODO()
    internal val internalProp: String = TODO()
    internal val internalInlineProp: Inline1 = TODO()
    fun normalFun() {}
    fun inlineReceivingFun(value: Inline1) {}
    fun inlineReturningFun(): Inline1 = TODO()
}
// MODULE: myModuleName
// FILE: input.kt
package foo.bar;
inline class Inline1(val value:String)
class Foo {
    val name:String = TODO()
    val inlineProp: Inline1 = TODO()
    internal val internalProp: String = TODO()
    internal val internalInlineProp: Inline1 = TODO()
    fun normalFun() {}
    fun inlineReceivingFun(value: Inline1) {}
    fun inlineReturningFun(): Inline1 = TODO()
}