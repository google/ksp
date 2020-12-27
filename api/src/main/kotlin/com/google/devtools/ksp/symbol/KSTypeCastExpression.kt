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
 * An expression that represents a type cast.
 *
 * @author RinOrz
 */
interface KSTypeCastExpression : KSExpression {

    /**
     * The type to be casted in the expression.
     *
     * For example, in `a as? kotlin.String`, the type is "kotlin.String"
     */
    val type: KSTypeReference?

    /**
     * The type to be converted in the expression.
     *
     * For example, in `foo as Boolean`, the body is "foo"
     */
    val body: KSExpression

    /**
     * The token used for the type cast.
     *
     * @see KSToken.Casts `as`
     * @see KSToken.SafeCasts `as?`
     */
    val token: KSToken
}