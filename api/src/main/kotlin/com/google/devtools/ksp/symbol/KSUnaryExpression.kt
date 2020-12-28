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
 * An expression for a unary operation.
 *
 * @author RinOrz
 */
interface KSUnaryExpression : KSExpression {

    /**
     * The body expression using the [token].
     *
     * For example, in `-1`, the body is "1"
     */
    val body: KSExpression

    /**
     * The token of the prefix or postfix in this expression.
     *
     * For example, in `1++`, the token is "++"
     */
    val token: KSToken

    /**
     * Returns true if the [token] is to the right of the [body].
     *
     * For example, in `--a`, the isPostfixOperation is false
     */
    val isPostfixOperation: Boolean
}