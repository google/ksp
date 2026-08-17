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

// WITH_RUNTIME
// TEST PROCESSOR: CrossModuleTypeAliasTestProcessor
// EXPECTED:
// A1(KOTLIN)
// A2(KOTLIN_LIB)
// M1(KOTLIN_LIB)
// END

// MODULE: module1
// FILE: M1.kt
class M1

// MODULE: module2(module1)
// FILE: M2.kt
typealias A2 = M1

// MODULE: main(module1, module2)
// FILE: main.kt
typealias A1 = M1

class TestTarget {
    val a1: A1 = TODO()
    val a2: A2 = TODO()
    val m1: M1 = TODO()
}
