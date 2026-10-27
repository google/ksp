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
// TEST PROCESSOR: DeclarationSourceOrderProcessor
// PROCESSOR INPUT: lib.MyObject.MyNestedAnnotation, lib.MyAnnotation, MyObject.MyNestedAnnotation, MyAnnotation
// EXPECTED:
// lib.MyObject.MyNestedAnnotation: a, b, c, <init>
// lib.MyAnnotation: a, b, c, <init>
// MyObject.MyNestedAnnotation: a, b, c, <init>
// MyAnnotation: a, b, c, <init>
// END

// MODULE: module1
// FILE: lib/MyAnnotation.kt
package lib

annotation class MyAnnotation(
    val a: String,
    val b: String,
    val c: String,
)

object MyObject {
    annotation class MyNestedAnnotation(
        val a: String,
        val b: String,
        val c: String,
    )
}

// MODULE: main(module1)
// FILE: main.kt
@MyAnnotation(b = "b", a = "a", c = "c")
@MyObject.MyNestedAnnotation(b = "b", a = "a", c = "c")
class MyClass

annotation class MyAnnotation(
    val a: String,
    val b: String,
    val c: String,
)

object MyObject {
    annotation class MyNestedAnnotation(
        val a: String,
        val b: String,
        val c: String,
    )
}
