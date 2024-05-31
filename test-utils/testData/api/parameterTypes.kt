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

// TEST PROCESSOR: ParameterTypeProcessor
// EXPECTED:
// a: Int
// b: <ERROR TYPE: NonExist>
// c: <ERROR TYPE>
// errorValue: <ERROR TYPE: ErrorType>
// v: String
// value: Int
// END

class Foo {
    var a: ErrorType
    set(errorValue) {
        a = errorValue
    }
    var x
        get() = "OK"
        set(v) = Unit
    var a:Int
    get() = a
    set(value) { a = value }

    fun foo(a: Int, b: NonExist, c)
}
