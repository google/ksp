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

// TEST PROCESSOR: ContextParameterOriginAndVisitorProcessor
// PROCESSOR INPUT: com.example.lib
// EXPECTED:
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: MyClass.srcProp._ origin=KOTLIN location=FileLocation(Main.kt:51) next=visitContextParameter(_, arg) legacy=visitAnnotated(_, arg)
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: MyLibClass.libProp._ origin=KOTLIN_LIB location=NonExistLocation next=visitContextParameter(_, arg) legacy=visitAnnotated(_, arg)
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: libFun.a origin=KOTLIN_LIB location=NonExistLocation next=visitContextParameter(a, arg) legacy=visitAnnotated(a, arg)
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: srcFun.a origin=KOTLIN location=FileLocation(Main.kt:47) next=visitContextParameter(a, arg) legacy=visitAnnotated(a, arg)
// END

// MODULE: lib
// FILE: Lib.kt
package com.example.lib

interface LibCtx

context(a: LibCtx)
fun libFun() {}

class MyLibClass {
    context(_: LibCtx)
    val libProp: Int
        get() = 42
}

// MODULE: main(lib)
// FILE: Main.kt

interface SrcCtx

context(a: SrcCtx)
fun srcFun() {}

class MyClass {
    context(_: SrcCtx)
    val srcProp: Int
        get() = 42
}
