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


package com.google.devtools.ksp.symbol

/**
 * An expression that represents the if-else.
 *
 * [Grammar](https://kotlinlang.org/docs/reference/grammar.html#ifExpression)
 *
 * @author RinOrz
 */
interface KSIfExpression : KSExpression {

    /**
     * The condition for the if expression.
     *
     * For example, in `if (a == b)`, the condition is "a == b"
     */
    val condition: KSExpression

    /**
     * A branch of an expression that executes if the condition is `true`.
     *
     * For example, in `if (true) { ... }`, the `then` is "{ ... }"
     */
    val then: KSExpression

    /**
     * A branch of an expression that executes if the condition is `false`.
     *
     * For example, in `if (true) { ... } else { return }`, the otherwise is "{ return }"
     */
    val otherwise: KSExpression?
}