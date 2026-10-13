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

// TEST PROCESSOR: ContextParameterTypeProcessor
// PROCESSOR INPUT: com.example.lib
// EXPECTED:
// EXPECT NEXT: Ctx1.y.ctxInterfaceSrcProp type=Int typeParent=self typeOrigin=KOTLIN typeLine=56 paramLine=56 ownerLine=57
// EXPECT NEXT: CtxLib.x.ctxInterfaceLibProp type=Int typeParent=self typeOrigin=KOTLIN_LIB typeLine=null paramLine=null ownerLine=null
// EXPECT NEXT: MyClass.myProp.ctxParam1 type=Ctx1 typeParent=self typeOrigin=KOTLIN typeLine=73 paramLine=73 ownerLine=74
// EXPECT NEXT: foo.ctxParam1 type=MyAlias typeParent=self typeOrigin=KOTLIN typeLine=67 paramLine=66 ownerLine=69
// EXPECT NEXT: fooLib.ctxParamLib type=CtxLib typeParent=self typeOrigin=KOTLIN_LIB typeLine=null paramLine=null ownerLine=null
// EXPECT NEXT: myLibProp.ctxParamLib type=CtxLib typeParent=self typeOrigin=KOTLIN_LIB typeLine=null paramLine=null ownerLine=null
// END

// MODULE: lib

// FILE: Other.kt

package com.example.lib

interface CtxLib {
    context(ctxInterfaceLibProp: Int)
    val x: Int
}

context(
    ctxParamLib:
    CtxLib
)
fun fooLib() {
}

context(ctxParamLib: CtxLib)
val myLibProp: Int
    get() = 42

// MODULE: main(lib)

// FILE: Main.kt

interface Ctx1 {
    context(ctxInterfaceSrcProp: Int)
    val y: Int
}

typealias MyAlias = Ctx1

// The type is deliberately on its own line, distinct from both the parameter name above it and
// the `fun` below it, so that `typeLine` identifies which branch of KSContextParameterImpl.type
// produced the KSTypeReference.
context(
    ctxParam1:
    MyAlias
)
fun foo() {
}

class MyClass {
    context(ctxParam1: Ctx1)
    val myProp: String
        get() = ""
}
