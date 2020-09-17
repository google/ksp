/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o.
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
// TEST PROCESSOR: TypeAliasProcessor
// EXPECTED:
// A = String
// A = String
// B = String
// C = A = String
// String
// String
// String
// END

typealias A = String
typealias B = String
typealias C = A
val a: A = ""
val b: B = ""
val c: C = ""
val d: String = ""
