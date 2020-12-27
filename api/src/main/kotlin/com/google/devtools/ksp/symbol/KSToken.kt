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
 *
 */

package com.google.devtools.ksp.symbol


/**
 * The Kotlin keywords and operators.
 *
 * [Grammar](https://kotlinlang.org/docs/reference/keyword-reference.html)
 *
 * @author RinOrz
 */
sealed class KSToken(val symbol: String) {
    object Casts : KSToken("as")
    object SafeCasts : KSToken("as?")

    object Semicolon : KSToken(";")

    object Unknown : KSToken("???")

    sealed class Operator(symbol: String) : KSToken(symbol) {
        object Elvis : Operator("?:")

        object In : Operator("in")
        object NotIn : Operator("!in")

        object Is : Operator("is")
        object NotIs : Operator("!is")

        object Plus : Operator("+")
        object Minus : Operator("-")
        object Times : Operator("*")
        object Div : Operator("/")
        object Rem : Operator("%")
        object Range : Operator("..")

        object PlusAssign : Operator("+=")
        object MinusAssign : Operator("-=")
        object TimesAssign : Operator("*=")
        object DivAssign : Operator("/=")
        object RemAssign : Operator("%=")

        object Equal : Operator("=")

        object Equality : Operator("==")
        object NotEquality : Operator("!=")
        object ReferenceEquality : Operator("===")
        object ReferenceNotEquality : Operator("!==")

        object Increments : Operator("++")
        object Decrements : Operator("--")

        object Disjunction : Operator("||")
        object Conjunction : Operator("&&")
        object Negation : Operator("!")

        object LessThan : Operator("<")
        object GreaterThan : Operator(">")
        object LessThanOrEqual : Operator("<=")
        object GreaterThanOrEqual : Operator(">=")
    }
}