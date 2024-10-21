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

// TEST PROCESSOR: ImplicitPropertyAccessorProcessor
// EXPECTED:
// privateGetterVal.getter(): Int
// privateGetterVar.getter(): String
// privateGetterVar.setter()(<set-?>: String)
// val1.getter(): Int
// var2.getter(): String
// var2.setter()(<set-?>: String)
// END
// MODULE: lib
// FILE: lib/Bar.kt
package lib

class Bar {
    val val1: Int = 0
    var var2: String = ""
}
// MODULE: main(lib)
// FILE: Foo.kt

class Foo {
    val privateGetterVal: Int
        private get

    var privateGetterVar: String
        set
        private get
}
