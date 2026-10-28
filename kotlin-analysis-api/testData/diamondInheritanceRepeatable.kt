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

// TEST PROCESSOR: AllSuperTypesRepeatableProcessor
// PROCESSOR INPUT: Leaf
// EXPECTED:
// Original:
// L04a
// L04b
// L03a
// L03b
// L02a
// L02b
// L01a
// L01b
// T00
// kotlin.Any
// Copy:
// L04a
// L04b
// L03a
// L03b
// L02a
// L02b
// L01a
// L01b
// T00
// kotlin.Any
// After Partial:
// L04a
// L04b
// L03a
// L03b
// L02a
// L02b
// L01a
// L01b
// T00
// kotlin.Any
// END

// FILE: main.kt

interface T00
interface L01a : T00;  interface L01b : T00
interface L02a : L01a, L01b;  interface L02b : L01a, L01b
interface L03a : L02a, L02b;  interface L03b : L02a, L02b
interface L04a : L03a, L03b;  interface L04b : L03a, L03b

abstract class Leaf : L04a, L04b
