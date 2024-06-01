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
// KS.star.replace([INVARIANT Int, INVARIANT String]): KS<Int, String>
// KS.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: NotExist1>
// KS.asType([INVARIANT Int, INVARIANT String]): KS<Int, String>
// KS.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: NotExist1>
// KS.asType(emptyList()): KS<T1, T2>
// KL.star.replace([INVARIANT Int, INVARIANT String]): KL<Int, String>
// KL.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: NotExist1>
// KL.asType([INVARIANT Int, INVARIANT String]): KL<Int, String>
// KL.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: NotExist1>
// KL.asType(emptyList()): KL<T1, T2>
// JS.star.replace([INVARIANT Int, INVARIANT String]): JS<Int, String>
// JS.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: NotExist1>
// JS.asType([INVARIANT Int, INVARIANT String]): JS<Int, String>
// JS.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: NotExist1>
// JS.asType(emptyList()): JS<T1, T2>
// JL.star.replace([INVARIANT Int, INVARIANT String]): JL<Int, String>
// JL.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: NotExist1>
// JL.asType([INVARIANT Int, INVARIANT String]): JL<Int, String>
// JL.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: NotExist1>
// JL.asType(emptyList()): JL<T1, T2>
// KS1.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KS1<Int> % Unexpected extra 1 type argument(s)>
// KS1.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: <ERROR TYPE: NotExist1> % Unexpected extra 1 type argument(s)>
// KS1.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KS1<Int> % Unexpected extra 1 type argument(s)>
// KS1.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: <ERROR TYPE: NotExist1> % Unexpected extra 1 type argument(s)>
// KS1.asType(emptyList()): KS1<T>
// KL1.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KL1<Int> % Unexpected extra 1 type argument(s)>
// KL1.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: <ERROR TYPE: NotExist1> % Unexpected extra 1 type argument(s)>
// KL1.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KL1<Int> % Unexpected extra 1 type argument(s)>
// KL1.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: <ERROR TYPE: NotExist1> % Unexpected extra 1 type argument(s)>
// KL1.asType(emptyList()): KL1<T>
// JS1.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JS1<Int> % Unexpected extra 1 type argument(s)>
// JS1.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: <ERROR TYPE: NotExist1> % Unexpected extra 1 type argument(s)>
// JS1.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JS1<Int> % Unexpected extra 1 type argument(s)>
// JS1.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: <ERROR TYPE: NotExist1> % Unexpected extra 1 type argument(s)>
// JS1.asType(emptyList()): JS1<T>
// JL1.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JL1<Int> % Unexpected extra 1 type argument(s)>
// JL1.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: <ERROR TYPE: NotExist1> % Unexpected extra 1 type argument(s)>
// JL1.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JL1<Int> % Unexpected extra 1 type argument(s)>
// JL1.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: <ERROR TYPE: NotExist1> % Unexpected extra 1 type argument(s)>
// JL1.asType(emptyList()): JL1<T>
// JSE.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JSE % Unexpected extra 2 type argument(s)>
// JSE.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: JSE % Unexpected extra 2 type argument(s)>
// JSE.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JSE % Unexpected extra 2 type argument(s)>
// JSE.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: JSE % Unexpected extra 2 type argument(s)>
// JSE.asType(emptyList()): JSE
// JSE.E.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JSE.E % Unexpected extra 2 type argument(s)>
// JSE.E.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: JSE.E % Unexpected extra 2 type argument(s)>
// JSE.E.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JSE.E % Unexpected extra 2 type argument(s)>
// JSE.E.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: JSE.E % Unexpected extra 2 type argument(s)>
// JSE.E.asType(emptyList()): JSE.E
// JLE.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JLE % Unexpected extra 2 type argument(s)>
// JLE.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: JLE % Unexpected extra 2 type argument(s)>
// JLE.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JLE % Unexpected extra 2 type argument(s)>
// JLE.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: JLE % Unexpected extra 2 type argument(s)>
// JLE.asType(emptyList()): JLE
// JLE.E.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JLE.E % Unexpected extra 2 type argument(s)>
// JLE.E.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: JLE.E % Unexpected extra 2 type argument(s)>
// JLE.E.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: JLE.E % Unexpected extra 2 type argument(s)>
// JLE.E.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: JLE.E % Unexpected extra 2 type argument(s)>
// JLE.E.asType(emptyList()): JLE.E
// KSE.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KSE % Unexpected extra 2 type argument(s)>
// KSE.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: KSE % Unexpected extra 2 type argument(s)>
// KSE.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KSE % Unexpected extra 2 type argument(s)>
// KSE.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: KSE % Unexpected extra 2 type argument(s)>
// KSE.asType(emptyList()): KSE
// KSE.E.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: E % Unexpected extra 2 type argument(s)>
// KSE.E.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: E % Unexpected extra 2 type argument(s)>
// KSE.E.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: E % Unexpected extra 2 type argument(s)>
// KSE.E.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: E % Unexpected extra 2 type argument(s)>
// KSE.E.asType(emptyList()): E
// KLE.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KLE % Unexpected extra 2 type argument(s)>
// KLE.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: KLE % Unexpected extra 2 type argument(s)>
// KLE.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KLE % Unexpected extra 2 type argument(s)>
// KLE.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: KLE % Unexpected extra 2 type argument(s)>
// KLE.asType(emptyList()): KLE
// KLE.E.star.replace([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KLE.E % Unexpected extra 2 type argument(s)>
// KLE.E.star.replace([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: KLE.E % Unexpected extra 2 type argument(s)>
// KLE.E.asType([INVARIANT Int, INVARIANT String]): <ERROR TYPE: KLE.E % Unexpected extra 2 type argument(s)>
// KLE.E.asType([INVARIANT NotExist1, INVARIANT NotExist2]): <ERROR TYPE: KLE.E % Unexpected extra 2 type argument(s)>
// KLE.E.asType(emptyList()): KLE.E
// default type:A
// flexible type star:(T..T?)
// flexible type replace argument:(JS1<Int>..JS1<Int>?)
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
class JS1<T> {
    T p;
}

class JavaClass {
    JS1<?> p;
}
enum JSE {
    E
}

// FILE: KS.kt
fun <A> f(a: A) = TODO()
class KS<T1, T2>
class KS1<T>
enum class KSE {
    E
}

val x: KS<Int, String> = TODO()
val y: KS<NotExist1, NotExist2> = TODO()
val z: KL1<Int> = TODO()
