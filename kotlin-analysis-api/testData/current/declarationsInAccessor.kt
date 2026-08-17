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

// TEST PROCESSOR: LocalDeclarationProcessor
// EXPECTED:
// File: a.kt
// fooAtTop
// localInfooAtTopGetter
// localInFooAtTopSetter
// Foo
// fooInBody
// localInFooInBodyGetter
// localInFooInBodySetter
// synthetic constructor for Foo
// END

// FILE: a.kt

var fooAtTop: Int
    get() {
        val localInfooAtTopGetter = 1
        return 1
    }

    set(value: Int) {
        val localInFooAtTopSetter = 1
    }

class Foo {
    var fooInBody: String
        get() {
            val localInFooInBodyGetter = 1
            return "OK"
        }
        set(value: String) {
            val localInFooInBodySetter = 1
        }
}
