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

// TEST PROCESSOR: FakeOverrideProcessor
// PROCESSOR INPUT: A, B, C, D
// EXPECTED:
// A
// <init>
// B
// B.x : A
// B.x.getter() : A
// C
// C.x : A
// C.x.getter() : A
// C.y : B
// C.y.getter() : B
// C.y.setter()
// D
// D.x : A
// D.x.getter() : A
// D.y : B
// D.y.getter() : B
// D.y.setter()
// END

// FILE: Main.kt
class A

interface B {
    val x: A
}

interface C {
    val x: A
    var y: B
}

interface D : B, C
