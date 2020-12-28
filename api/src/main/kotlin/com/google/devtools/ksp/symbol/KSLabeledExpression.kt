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
 * An expression with a label.
 *
 * [Grammar](https://kotlinlang.org/docs/reference/grammar.html#label)
 *
 * @author RinOrz
 */
interface KSLabeledExpression : KSExpression, KSLabelReferenceExpression {

    /**
     * The label name of the expression.
     *
     * For example, in `loop@ for (i in 1..100)`, the name is "loop"
     */
    override val name: String

    /**
     * The expression after the label.
     *
     * For example, in `loop@ for (i in 1..100)`, the body is "for (i in 1..100)"
     */
    val body: KSExpression
}