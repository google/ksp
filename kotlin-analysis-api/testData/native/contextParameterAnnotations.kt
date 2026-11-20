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

// Native KLIB twin of contextParameterAnnotations.kt.

// TARGET_BACKEND: NATIVE
// WITH_FIXED_TARGET: linux_x64
// WITH_RUNTIME
// TEST PROCESSOR: ContextParameterAnnotationProcessor
// PROCESSOR INPUT: com.example.lib
// EXPECTED:
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: MyClass.srcProp._ annotations=[com.example.annos.CtxAnno(tag=srcUnnamed)[parentSelf=true]]
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: MyClass.srcProp.b annotations=[]
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: MyLibClass.libProp._ annotations=[com.example.annos.CtxAnno(tag=libUnnamed)[parentSelf=true]]
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: MyLibClass.libProp.b annotations=[]
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: libFun.a annotations=[com.example.annos.CtxAnno(tag=libFun)[parentSelf=true], com.example.annos.RepAnno(n=1)[parentSelf=true], com.example.annos.RepAnno(n=2)[parentSelf=true]]
// EXPECT ENABLE_CONTEXT_PARAMETERS = TRUE: srcFun.a annotations=[com.example.annos.CtxAnno(tag=srcFun)[parentSelf=true], com.example.annos.RepAnno(n=3)[parentSelf=true], com.example.annos.RepAnno(n=4)[parentSelf=true]]
// END

// MODULE: annos
// FILE: Annos.kt
package com.example.annos

annotation class CtxAnno(val tag: String)

@Repeatable
annotation class RepAnno(val n: Int)

// MODULE: lib(annos)
// FILE: Lib.kt
package com.example.lib

import com.example.annos.CtxAnno
import com.example.annos.RepAnno

interface LibCtx

context(@CtxAnno("libFun") @RepAnno(1) @RepAnno(2) a: LibCtx)
fun libFun() {}

class MyLibClass {
    context(@CtxAnno("libUnnamed") _: LibCtx, b: LibCtx)
    val libProp: Int
        get() = 42
}

// MODULE: main(lib, annos)
// FILE: Main.kt

import com.example.annos.CtxAnno
import com.example.annos.RepAnno

interface SrcCtx

context(@CtxAnno("srcFun") @RepAnno(3) @RepAnno(4) a: SrcCtx)
fun srcFun() {}

class MyClass {
    context(@CtxAnno("srcUnnamed") _: SrcCtx, b: SrcCtx)
    val srcProp: Int
        get() = 42
}
