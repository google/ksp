/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: DeferredSymbolsProcessor
// EXPECTED:
// A
// File: K.kt
// K
// T
// f1
// p1
// p2
// v1
// v1
// v2
// v3.getter()
// v3.setter()
// END

// FILE: K.kt
@file:Defer

annotation class Defer

@Defer
typealias A = K

@Defer
class K<@Defer T>(
    @Defer val v1: Int,
    @Defer p1: String,
) {
    @Defer val v2: Int

    @Defer
    fun f1(@Defer p2: Int) = Unit

    @get:Defer
    @set:Defer
    var v3: List<Double>
        get() = TODO()
        set(v: List<Double>) = Unit
}
