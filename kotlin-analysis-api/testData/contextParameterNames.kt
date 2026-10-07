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

// An unnamed context parameter, `context(_: T)`, must be reported with a null name (and `_` as its
// toString()), never with the compiler-internal special name the Analysis API uses for it
// (`SpecialNames.UNDERSCORE_FOR_UNUSED_VAR`, i.e. `<unused var>`). Covered for both source and
// library (binary) symbols, since the Analysis API derives the name from PSI for the former and
// from FIR/metadata for the latter.

// TEST PROCESSOR: ContextParameterNameProcessor
// PROCESSOR INPUT: lib
// EXPECTED:
// EXPECT NEXT: libNamed[0] name=libNamed toString=libNamed
// EXPECT NEXT: libUnnamed[0] name=null toString=_
// EXPECT NEXT: mixedProp[0] name=null toString=_
// EXPECT NEXT: mixedProp[1] name=b toString=b
// EXPECT NEXT: namedFun[0] name=named toString=named
// EXPECT NEXT: unnamedFun[0] name=null toString=_
// END

// MODULE: lib
// FILE: lib.kt

package lib

interface LibCtx

context(libNamed: LibCtx)
fun libNamed() {
}

context(_: LibCtx)
fun libUnnamed() {
}

// MODULE: main(lib)
// FILE: Main.kt

interface Ctx1
interface Ctx2

context(named: Ctx1)
fun namedFun() {
}

context(_: Ctx1)
fun unnamedFun() {
}

class MyClass {
    context(_: Ctx1, b: Ctx2)
    val mixedProp: String
        get() = ""
}
