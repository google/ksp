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
// class: Data
// a
// <init>(kotlin.String): Data
// component1(): kotlin.String
// copy(kotlin.String(hasDefault)): Data
// equals(kotlin.Any): kotlin.Boolean
// hashCode(): kotlin.Int
// toString(): kotlin.String
// class: Sub
// equals(kotlin.Any): kotlin.Boolean
// foo(kotlin.String ...): kotlin.Unit
// hashCode(): kotlin.Int
// toString(): kotlin.String
// class: SubAbstract
// <init>(): SubAbstract
// equals(kotlin.Any): kotlin.Boolean
// foo(kotlin.String ...): kotlin.Unit
// hashCode(): kotlin.Int
// toString(): kotlin.String
// class: Super
// equals(kotlin.Any): kotlin.Boolean
// foo(kotlin.String ...): kotlin.Unit
// hashCode(): kotlin.Int
// toString(): kotlin.String
// class: SuperAbstract
// <init>(): SuperAbstract
// equals(kotlin.Any): kotlin.Boolean
// foo(kotlin.String ...): kotlin.Unit
// hashCode(): kotlin.Int
// toString(): kotlin.String
// EXPECTED:
// END
// FILE: a.kt
data class Data(val a: String) {
    override fun equals(other: Any?): Boolean {
        return false
    }
}

interface Super {
    fun foo(vararg values: String)
}

interface Sub : Super

class SubAbstract: SuperAbstract()

abstract class SuperAbstract {
    fun foo(vararg values: String) {

    }
}

