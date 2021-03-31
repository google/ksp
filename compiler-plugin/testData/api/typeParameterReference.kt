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
// TEST PROCESSOR: TypeParameterReferenceProcessor
// EXPECTED:
// LibFoo: false
// kotlin.String: false
// Foo.T1: true
// Foo.bar.T2: false
// foo.T3: false
// T
// List<T>
// T
// MutableList<(T..T?)>
// END

// MODULE: lib
// FILE: lib.kt
interface LibFoo<T> {
    val v: T
    val w: List<T>
}

// FILE: JavaLib.java
import java.util.List;

interface JavaLib<T> {
    public T genericFun();
    public List<T> list();
}

// MODULE: main(lib)
// FILE: main.kt
class Foo<T1> {
    inner class Bar {
        val v: T1?
    }

    fun <T2> bar(p: T2) = 1

    val libFoo: LibFoo<String>
}

fun <T3> foo(p: T3) = 1
