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
// TEST PROCESSOR: BuiltInTypesProcessor
// EXPECTED:
// Annotation: OK
// Any: OK
// Array<*>: OK
// Boolean: OK
// Byte: OK
// Char: OK
// Double: OK
// Float: OK
// Int: OK
// Iterable<*>: OK
// Long: OK
// Nothing: OK
// Number: OK
// Short: OK
// String: OK
// Unit: OK
// END

val a: Any = 0
val b: Unit = Unit
val c: Number = 0
val d: Byte = 0
val e: Short = 0
val f: Int = 0
val g: Long = 0
val h: Float = 0.0f
val i: Double = 0.0
val j: Char = '0'
val k: Boolean = false
val l: String = ""
val m: Iterable<*> = listOf<Any>()
val n: Annotation = object: Annotation {}
fun foo(): Nothing = throw Error()
val o: Array<*> = arrayOf<Any>()
