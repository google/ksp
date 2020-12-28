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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtQualifiedExpression

class KSDslExpressionImpl private constructor(
    ktExpression: KtExpression,
    override val closures: List<KSLambdaExpression>
) : KSDslExpression, KSCallExpressionImpl(ktExpression) {
    companion object : KSObjectCache<KtExpression, KSDslExpressionImpl>() {
        fun getCachedOrNull(ktExpression: KtExpression): KSDslExpressionImpl? {
            val ktCallExpression = ktExpression as? KtCallExpression
                ?: (ktExpression as? KtQualifiedExpression)?.selectorExpression as? KtCallExpression
                ?: return null
            val closures = ktCallExpression.valueArguments.mapNotNull {
                (it as? KtLambdaArgument)?.getLambdaExpression()?.let(KSLambdaExpressionImpl::getCached)
            }
            return when {
                closures.isEmpty() -> null
                else -> cache.getOrPut(ktExpression) { KSDslExpressionImpl(ktExpression, closures) }
            }
        }
    }

    override val arguments: List<KSValueArgumentExpression> by lazy {
        super.arguments!!
    }

    override fun resolve(): KSFunctionDeclaration? {
        return super.resolve() as? KSFunctionDeclaration
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitDslExpression(this, data)
    }
}