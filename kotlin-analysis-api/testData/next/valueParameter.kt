/*
 * Copyright 2025 Google LLC
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: ValueParameterProcessor
// EXPECTED:
// MyClassSrc.field: isVal: false, isVar: false
// MyClassSrc.valField: isVal: true, isVar: false
// MyClassSrc.varField: isVal: false, isVar: true
// MyClassLib.field: isVal: false, isVar: false
// MyClassLib.valField: isVal: true, isVar: false
// MyClassLib.varField: isVal: false, isVar: true
// END

// MODULE: lib
// FILE: lib.kt

class MyClassLib(
    field: String,
    val valField: String,
    var varField: String,
)

// MODULE: main(lib)
// FILE: main.kt

class MyClassSrc(
    field: String,
    val valField: String,
    var varField: String,
)

