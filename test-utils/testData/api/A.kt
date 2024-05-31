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
// TEST PROCESSOR: AProcessor
// EXPECTED:
// END
// MODULE: lib
// FILE: Test.kt
open class BaseClass<T>(val genericProp : T) {
    fun baseMethod(input: T) {}
}
class SubClass(x : Int) : BaseClass<Int>(x) {
    val subClassProp : String = "abc"
}
// MODULE: main(lib)
// FILE: Main.kt
class Main
