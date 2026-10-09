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
// abbreviated types are distinguishable through KSType.arguments. A type parameter bound must
// resolve to the same shape as a value parameter of the same written type.

// TEST PROCESSOR: TypeParameterBoundTypeAliasProcessor
// EXPECTED:
// srcFoo.p value: StrMap[Int[]]
// srcFoo.T bound: StrMap[Int[]]
// libFoo.T bound: StrMap[Int[]]
// SrcBox.T bound: StrMap[Int[]]
// recFoo.T bound: StrMap[T[]]
// Sub.<init> asMemberOf bounds: [Any[]]
// Sub.f asMemberOf bounds: [StrMap[Int[]]]
// END

// MODULE: lib
//FILE: lib.kt

package lib

typealias StrMap<V> = Map<String, V>

fun <T : StrMap<Int>> libFoo() {
}

// MODULE: main(lib)
//FILE: Main.kt

import lib.StrMap

fun <T : StrMap<Int>> srcFoo(p: StrMap<Int>) {
}

class SrcBox<T : StrMap<Int>>

fun <T : StrMap<T>> recFoo() {
}

open class Base<X> {
    fun <T : StrMap<X>> f() {
    }
}

class Sub : Base<Int>()
