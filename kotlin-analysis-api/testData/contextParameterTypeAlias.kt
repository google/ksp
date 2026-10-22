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

// A typealias whose expansion has more type arguments than the alias itself, so the expanded and
// abbreviated types are distinguishable through KSType.arguments. The context parameter type must
// resolve to the same shape as the value parameter type, for source (PSI) and library (Analysis
// API) declarations alike.

// TEST PROCESSOR: ContextParameterTypeAliasProcessor
// PROCESSOR INPUT: lib
// EXPECTED:
// EXPECT NEXT: libFoo context: StrMap[Int[]]
// EXPECT NEXT: libFoo value: StrMap[Int[]]
// EXPECT NEXT: libFoo equal: true
// EXPECT NEXT: libFooRec context: T[]
// EXPECT NEXT: libFooRec value: T[]
// EXPECT NEXT: libFooRec equal: true
// EXPECT NEXT: srcFoo context: StrMap[Int[]]
// EXPECT NEXT: srcFoo value: StrMap[Int[]]
// EXPECT NEXT: srcFoo equal: true
// EXPECT NEXT: srcFooRec context: T[]
// EXPECT NEXT: srcFooRec value: T[]
// EXPECT NEXT: srcFooRec equal: true
// END

// MODULE: lib
// FILE: lib.kt

package lib

typealias StrMap<V> = Map<String, V>

context(libCtxParam: StrMap<Int>)
fun libFoo(libParam: StrMap<Int>) {
}

context(libCtxParamRec: T)
fun <T : StrMap<T>> libFooRec(libParamRec: T) {
}

// MODULE: main(lib)
// FILE: Main.kt

import lib.StrMap

context(srcCtxParam: StrMap<Int>)
fun srcFoo(srcParam: StrMap<Int>) {
}

context(srcCtxParamRec: T)
fun <T : StrMap<T>> srcFooRec(srcParamRec: T) {
}
