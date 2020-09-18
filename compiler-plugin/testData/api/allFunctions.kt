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
// TEST PROCESSOR: AllFunctionsProcessor
// EXPECTED:
// class: Foo
// bar(): kotlin.Boolean
// baz(kotlin.String,kotlin.String(hasDefault),kotlin.String(hasDefault)): kotlin.Boolean
// class: C
// class: Data
// component1(): kotlin.String
// contains(kotlin.Number): kotlin.Boolean
// containsAll(kotlin.collections.Collection): kotlin.Boolean
// copy(kotlin.String(hasDefault)): Data
// equals(kotlin.Any): kotlin.Boolean
// equals(kotlin.Any): kotlin.Boolean
// equals(kotlin.Any): kotlin.Boolean
// get(kotlin.Int): kotlin.Number
// hashCode(): kotlin.Int
// hashCode(): kotlin.Int
// hashCode(): kotlin.Int
// indexOf(kotlin.Number): kotlin.Int
// isEmpty(): kotlin.Boolean
// iterator(): kotlin.collections.Iterator
// javaListFun(): Collection
// javaListFun(): kotlin.collections.List
// javaPrivateFun(): kotlin.Unit
// javaStrFun(): kotlin.String
// javaStrFun(): kotlin.String
// lastIndexOf(kotlin.Number): kotlin.Int
// listIterator(): kotlin.collections.ListIterator
// listIterator(kotlin.Int): kotlin.collections.ListIterator
// subList(kotlin.Int,kotlin.Int): kotlin.collections.List
// toString(): kotlin.String
// toString(): kotlin.String
// toString(): kotlin.String
// END
// FILE: a.kt
abstract class Foo : C(), List<out Number> {
    override fun javaListFun(): List<Int> {
        throw java.lang.IllegalStateException()
    }

    fun bar(): Boolean {
        return false
    }

    fun baz(input: String, input2: String? = null, input3: String = ""): Boolean {
        return false
    }
}

data class Data(val a: String) {
    override fun equals(other: Any?): Boolean {
        return false
    }
}

// FILE: C.java
class C {
    private void javaPrivateFun() {

    }

    protected Collection<Int> javaListFun() {
        return Arrays.asList(1,2,3)
    }

    public String javaStrFun() {
        return "str"
    }
}
