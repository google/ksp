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

// WITH_RUNTIME
// TEST PROCESSOR: ContextParameterValidateProcessor
// PROCESSOR INPUT: com.example.lib
// EXPECTED:
// EXPECT ENABLE_CONTEXT_PARAMETERS = FALSE: decl ClassWithInvalidCtxFun valid=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: decl ClassWithInvalidCtxFun valid=false
// EXPECT ENABLE_CONTEXT_PARAMETERS = FALSE: decl ClassWithInvalidCtxProp valid=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: decl ClassWithInvalidCtxProp valid=false
// decl libValidFun valid=true
// decl libValidProp valid=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = FALSE: decl srcInvalidFun valid=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: decl srcInvalidFun valid=false
// EXPECT ENABLE_CONTEXT_PARAMETERS = FALSE: decl srcInvalidProp valid=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: decl srcInvalidProp valid=false
// EXPECT ENABLE_CONTEXT_PARAMETERS = FALSE: decl srcInvalidTypeArgFun valid=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: decl srcInvalidTypeArgFun valid=false
// decl srcValidFun valid=true
// decl srcValidProp valid=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param ClassWithInvalidCtxFun.memberFun.a valid=false dataIsSelf=true threwWhenDisabled=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param ClassWithInvalidCtxProp.memberProp.a valid=false dataIsSelf=true threwWhenDisabled=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param libValidFun.a valid=true dataIsSelf=true threwWhenDisabled=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param libValidProp.a valid=true dataIsSelf=true threwWhenDisabled=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param srcInvalidFun.a valid=false dataIsSelf=true threwWhenDisabled=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param srcInvalidProp.a valid=false dataIsSelf=true threwWhenDisabled=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param srcInvalidTypeArgFun.a valid=false dataIsSelf=true threwWhenDisabled=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param srcValidFun.a valid=true dataIsSelf=true threwWhenDisabled=true
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: param srcValidProp.a valid=true dataIsSelf=true threwWhenDisabled=true
// END

// MODULE: lib
// FILE: Lib.kt
package com.example.lib

interface LibCtx

context(a: LibCtx)
fun libValidFun() {}

context(a: LibCtx)
val libValidProp: Int
    get() = 1

// MODULE: main(lib)
// FILE: Main.kt

interface GoodCtx

context(a: GoodCtx)
fun srcValidFun() {}

context(a: NonExistentType)
fun srcInvalidFun() {}

context(a: List<NonExistentType>)
fun srcInvalidTypeArgFun() {}

context(a: GoodCtx)
val srcValidProp: Int
    get() = 1

context(a: NonExistentType)
val srcInvalidProp: Int
    get() = 1

class ClassWithInvalidCtxFun {
    context(a: NonExistentType)
    fun memberFun() {}
}

class ClassWithInvalidCtxProp {
    context(a: NonExistentType)
    val memberProp: Int
        get() = 1
}
