/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: NestedClassTypeProcessor
// EXPECTED:
// foo
// @TypeAnno1
// COVARIANT [@TypeAnno1] String
// bar
// , @TypeAnno2
// CONTRAVARIANT Int,INVARIANT [@TypeAnno2] String
// END

// FILE: a.kt
annotation class TypeAnno1
annotation class TypeAnno2

class Outer<T> {
    inner class InnerGeneric<P>
    inner class Inner
}

class G<T>

class C {
    val foo: Outer<out @TypeAnno1 String>.Inner
    val bar: Outer<@TypeAnno2 String>.InnerGeneric<in Int>
}
