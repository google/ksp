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
// TEST PROCESSOR: ReplaceWithErrorTypeArgsProcessor
// EXPECTED:
// KS<Int, String>
// <ERROR TYPE>
// KS<Int, String>
// <ERROR TYPE>
// KL<Int, String>
// <ERROR TYPE>
// KL<Int, String>
// <ERROR TYPE>
// JS<Int, String>
// <ERROR TYPE>
// JS<Int, String>
// <ERROR TYPE>
// JL<Int, String>
// <ERROR TYPE>
// JL<Int, String>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// <ERROR TYPE>
// END

// MODULE: lib
// FILE: JL.java
class JL<T1, T2> {}
class JL1<T> {}
enum JLE {
    E
}

// FILE: KL.kt
class KL<T1, T2>
class KL1<T>
enum class KLE {
    E
}

// MODULE: main(lib)
// FILE: JS.java
class JS<T1, T2> {}
class JS1<T> {}
enum JSE {
    E
}

// FILE: KS.kt
class KS<T1, T2>
class KS1<T>
enum class KSE {
    E
}

val x: KS<Int, String> = TODO()
val y: KS<NotExist1, NotExist2> = TODO()
