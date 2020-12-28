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


package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

class KSLambdaExpressionImpl private constructor(ktLambdaExpression: KtLambdaExpression) : KSLambdaExpression,
    KSExpressionImpl(ktLambdaExpression), KSBlockExpression {
    companion object : KSObjectCache<KtLambdaExpression, KSLambdaExpressionImpl>() {
        fun getCached(ktLambdaExpression: KtLambdaExpression) =
            cache.getOrPut(ktLambdaExpression) { KSLambdaExpressionImpl(ktLambdaExpression) }
    }

    override val parameters: List<KSValueParameter> by lazy {
        ktLambdaExpression.valueParameters.map {
            KSValueParameterImpl.getCached(it)
        }
    }

    override val statements: List<KSExpression> by lazy {
        ktLambdaExpression.bodyExpression?.statements?.mapNotNull { it.toKSExpression() }
            ?: error("Unable to get code block from lambda expression, this is an incorrect expression: \n${ktLambdaExpression.text}")
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitBlockExpression(this, data)
    }
}