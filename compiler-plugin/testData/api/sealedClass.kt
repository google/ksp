/*
 * Copyright 2021 Google LLC
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: SealedClassProcessor
// EXPECTED:
// from lib
// [Const: KOTLIN_LIB, NotANumber: KOTLIN_LIB, Sum: KOTLIN_LIB]
// from source
// Expr : [Const: KOTLIN, NotANumber: KOTLIN, Sum: KOTLIN]
// Const : []
// Sum : []
// NotANumber : []
// END

// MODULE: lib
// FILE: lib.kt
package lib
sealed class Expr
data class Const(val number: Double) : Expr()
data class Sum(val e1: Expr, val e2: Expr) : Expr()
object NotANumber : Expr()

// MODULE: main(lib)
// FILE: sealed.kt
sealed class Expr
data class Const(val number: Double) : Expr()
data class Sum(val e1: Expr, val e2: Expr) : Expr()
object NotANumber : Expr()
