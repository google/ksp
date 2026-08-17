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

// TEST PROCESSOR: ExplicitBackingFieldsSubtypingProcessor
// EXPECTED:
// MyClass.prop1: List<String>
// MyClass.prop1.field: MutableList<String>
// MyClass.prop2: List<String>
// MyClass.prop2.field: List<String>
// MyClass.prop3: List<String>
// MyClass.prop3.field: MutableList<String>
// MyClass.prop4: List<Int>
// MyClass.prop4.field: MutableList<Int>
// MyClass.propA: A
// MyClass.propA.field: B
// Other.prop1: List<String>
// Other.prop1.field: List<String>
// Other.prop2: List<String>
// Other.prop2.field: List<String>
// Other.prop3: List<String>
// Other.prop3.field: List<String>
// Other.prop4: List<Int>
// Other.prop4.field: List<Int>
// Other.propA: A
// Other.propA.field: A
// END

// MODULE: lib
// FILE: lib/Other.kt

package lib

class Other {

    val prop1: List<String>
        field: MutableList<String>

    val prop2: List<String>
        field: List<String>

    val prop3: List<String>
        field = mutableListOf("")

    val prop4: List<Int>
        field: MutableList<Int> = mutableListOf(42)

    val propA: A
        field: B = B()

    val propX get() = propA.x

    init {
        prop1 = mutableListOf("")
        prop2 = mutableListOf("")
    }

    fun append(n: Int) {
        prop4.add(n)
    }

    fun add(n: Int) {
        propA.add(n)
    }
}

open class A {
    var x = 42
}

class B : A() {
    fun add(n: Int) {
        x = x + n
    }
}

// MODULE: main(lib)
// FILE: MyClass.kt

class MyClass {

    val prop1: List<String>
        field: MutableList<String>

    val prop2: List<String>
        field: List<String>

    val prop3: List<String>
        field = mutableListOf("")

    val prop4: List<Int>
        field: MutableList<Int> = mutableListOf(42)

    val propA: A
        field: B = B()

    val propX get() = propA.x

    init {
        prop1 = mutableListOf("")
        prop2 = mutableListOf("")
    }

    fun append(n: Int) {
        prop4.add(n)
    }

    fun add(n: Int) {
        propA.add(n)
    }
}

open class A {
    var x = 42
}

class B : A() {
    fun add(n: Int) {
        x = x + n
    }
}
