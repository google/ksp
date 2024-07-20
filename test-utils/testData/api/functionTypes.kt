/*
 * Copyright 2021 Google LLC
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// TEST PROCESSOR: FunctionTypeProcessor
// EXPECTED:
// a: Function0 : true, false, kotlin.Function0, kotlin
// b: Function1 : true, false, kotlin.Function1, kotlin
// c: Function0 : true, false, kotlin.Function0, kotlin
// d: Function2 : true, false, kotlin.Function2, kotlin
// e: KFunction0 : true, false, kotlin.reflect.KFunction0, kotlin.reflect
// f: KSuspendFunction0 : false, true, kotlin.reflect.KSuspendFunction0, kotlin.reflect
// g: KFunction1 : true, false, kotlin.reflect.KFunction1, kotlin.reflect
// h: KSuspendFunction1 : false, true, kotlin.reflect.KSuspendFunction1, kotlin.reflect
// i: Function1 : true, false, kotlin.Function1, kotlin
// j: SuspendFunction1 : false, true, kotlin.coroutines.SuspendFunction1, kotlin.coroutines
// k: SuspendFunction0 : false, true, kotlin.coroutines.SuspendFunction0, kotlin.coroutines
// l: SuspendFunction1 : false, true, kotlin.coroutines.SuspendFunction1, kotlin.coroutines
// m: SuspendFunction0 : false, true, kotlin.coroutines.SuspendFunction0, kotlin.coroutines
// n: SuspendFunction2 : false, true, kotlin.coroutines.SuspendFunction2, kotlin.coroutines
// o: KFunction0 : true, false, kotlin.reflect.KFunction0, kotlin.reflect
// p: KSuspendFunction0 : false, true, kotlin.reflect.KSuspendFunction0, kotlin.reflect
// vbar: KSuspendFunction0 : false, true, kotlin.reflect.KSuspendFunction0, kotlin.reflect
// vfoo: KFunction0 : true, false, kotlin.reflect.KFunction0, kotlin.reflect
// END

@file:Suppress("Boolean", "Byte", "Int", "Short", "Double", "Float", "Unit", "Suppress", "C")

class C {
    fun foo(): Boolean = true
    suspend fun bar(): Int = 0
    val vfoo = ::foo
    val vbar = ::bar
}

fun foo() = Unit
suspend fun bar() = Unit

val a: () -> Unit = TODO()
val b: (Int) -> Unit = TODO()
val c: () -> Byte = TODO()
val d: (Short, Float) -> Double = TODO()

val e = C().vfoo
val f = C().vbar
val g = C::foo
val h = C::bar

val i: Int.() -> Boolean = TODO()
val j: suspend Boolean.() -> Int = TODO()

val k: suspend () -> Unit = TODO()
val l: suspend (Int) -> Unit = TODO()
val m: suspend () -> Byte = TODO()
val n: suspend (Short, Float) -> Double = TODO()

val o = ::foo
val p = ::bar
