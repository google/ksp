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


@file:Suppress("RemoveRedundantQualifierName")

package com.google.devtools.ksp.symbol


/**
 * An expression that represents the if-else.
 *
 * [Grammar](https://kotlinlang.org/docs/reference/grammar.html#ifExpression)
 *
 * @author RinOrz
 */
interface KSWhenExpression : KSExpression {

    /**
     * All branches in the `when` expression.
     */
    val branches: List<KSWhenExpression.Branch>

    /**
     * The subject for the when expression.
     *
     * For example, in `when (a)`, the subject is "a"
     *
     * This is also a property declaration when the expression is `when (val a = ...)`
     */
    val subject: KSExpression?


    /**
     * A node that represents a branch in the `when` expression.
     *
     * [Grammar](https://kotlinlang.org/docs/reference/grammar.html#whenEntry)
     */
    interface Branch : KSNode {

        /**
         * The test conditions for this branch.
         *
         * E.g:
         * ```
         * when(count) {
         *     a, b -> {}  // conditions is "a" and "b"
         *     in 1..2 -> {}  // conditions is "in 1..2"
         *     is String -> {}  // conditions is "String"
         * }
         * ```
         *
         * @see KSWhenExpression.Branch.RangeCondition
         * @see KSWhenExpression.Branch.TypeCondition
         */
        val conditions: Array<KSExpression>

        /**
         * The body behavior of this branch.
         *
         * E.g:
         * ```
         * when {
         *     a -> invoke()  // behavior is "invoke()"
         *     else -> {}  // behavior is "{}"
         * }
         * ```
         */
        val body: KSExpression

        /**
         * Whether this is an `else` branch.
         */
        val isElse: Boolean


        /**
         * An expression representing a range condition.
         *
         * [Grammar](https://kotlinlang.org/docs/reference/grammar.html#rangeTest)
         */
        interface RangeCondition : KSExpression {

            /**
             * An operator used to determine if the condition is "in range"
             *
             * @see KSToken.Operator.In
             * @see KSToken.Operator.NotIn
             */
            val operator: KSToken.Operator

            /**
             * The range expression for this condition.
             *
             * For example, in `in 1..10`, the range is "1..10"
             *
             * [Grammar](https://kotlinlang.org/docs/reference/grammar.html#rangeExpression)
             */
            val range: KSBinaryExpression
        }

        /**
         * An expression that expresses the type judgment condition.
         *
         * [Grammar](https://kotlinlang.org/docs/reference/grammar.html#typeTest)
         */
        interface TypeCondition : KSExpression {

            /**
             * An operator used to determine if the condition is "has a certain type"
             *
             * @see KSToken.Operator.Is
             * @see KSToken.Operator.NotIs
             */
            val operator: KSToken.Operator

            /**
             * The type that satisfies this condition.
             *
             * For example, in `is String`, the type is "String"
             */
            val type: KSTypeReference
        }
    }
}