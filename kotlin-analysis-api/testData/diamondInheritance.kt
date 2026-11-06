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

// TEST PROCESSOR: AllSuperTypesProcessor
// PROCESSOR INPUT: Leaf
// EXPECTED:
// L30a
// L30b
// L29a
// L29b
// L28a
// L28b
// L27a
// L27b
// L26a
// L26b
// L25a
// L25b
// L24a
// L24b
// L23a
// L23b
// L22a
// L22b
// L21a
// L21b
// L20a
// L20b
// L19a
// L19b
// L18a
// L18b
// L17a
// L17b
// L16a
// L16b
// L15a
// L15b
// L14a
// L14b
// L13a
// L13b
// L12a
// L12b
// L11a
// L11b
// L10a
// L10b
// L09a
// L09b
// L08a
// L08b
// L07a
// L07b
// L06a
// L06b
// L05a
// L05b
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
interface L05a : L04a, L04b;  interface L05b : L04a, L04b
interface L06a : L05a, L05b;  interface L06b : L05a, L05b
interface L07a : L06a, L06b;  interface L07b : L06a, L06b
interface L08a : L07a, L07b;  interface L08b : L07a, L07b
interface L09a : L08a, L08b;  interface L09b : L08a, L08b
interface L10a : L09a, L09b;  interface L10b : L09a, L09b
interface L11a : L10a, L10b;  interface L11b : L10a, L10b
interface L12a : L11a, L11b;  interface L12b : L11a, L11b
interface L13a : L12a, L12b;  interface L13b : L12a, L12b
interface L14a : L13a, L13b;  interface L14b : L13a, L13b
interface L15a : L14a, L14b;  interface L15b : L14a, L14b
interface L16a : L15a, L15b;  interface L16b : L15a, L15b
interface L17a : L16a, L16b;  interface L17b : L16a, L16b
interface L18a : L17a, L17b;  interface L18b : L17a, L17b
interface L19a : L18a, L18b;  interface L19b : L18a, L18b
interface L20a : L19a, L19b;  interface L20b : L19a, L19b
interface L21a : L20a, L20b;  interface L21b : L20a, L20b
interface L22a : L21a, L21b;  interface L22b : L21a, L21b
interface L23a : L22a, L22b;  interface L23b : L22a, L22b
interface L24a : L23a, L23b;  interface L24b : L23a, L23b
interface L25a : L24a, L24b;  interface L25b : L24a, L24b
interface L26a : L25a, L25b;  interface L26b : L25a, L25b
interface L27a : L26a, L26b;  interface L27b : L26a, L26b
interface L28a : L27a, L27b;  interface L28b : L27a, L27b
interface L29a : L28a, L28b;  interface L29b : L28a, L28b
interface L30a : L29a, L29b;  interface L30b : L29a, L29b

abstract class Leaf : L30a, L30b
