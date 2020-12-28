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

import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.KSCallExpression
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSExpression
import com.google.devtools.ksp.symbol.KSValueArgumentExpression
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression

open class KSCallExpressionImpl(ktExpression: KtExpression) : KSCallExpression, KSExpressionImpl(ktExpression) {
    companion object : KSObjectCache<KtExpression, KSCallExpressionImpl>() {
        fun getCached(ktExpression: KtExpression) = cache.getOrPut(ktExpression) { KSCallExpressionImpl(ktExpression) }
    }

    override val receiver: KSExpression? by lazy {
        callNameExpression?.getReceiverExpression()?.let {
            when(it) {
                is KtQualifiedExpression -> it.selectorExpression.toKSExpression()
                else -> it.toKSExpression()
            }
        }
    }

    override val name: String by lazy {
        callNameExpression?.getReferencedName()
            ?: error("This is an incorrect call expression: ${ktExpression.text}")
    }

    override val arguments: List<KSValueArgumentExpression>? by lazy {
        callExpression?.valueArguments?.mapIndexed { index, valueArgument ->
            KSValueArgumentExpressionImpl.getCached(index, valueArgument)
        }
    }

    private val callExpression get() = ktExpression as? KtCallExpression

    private val callNameExpression by lazy {
        callExpression?.getCallNameExpression() ?: ktExpression as? KtSimpleNameExpression
    }

    override fun resolve(): KSDeclaration? = ResolverImpl.instance.resolveCallDeclaration(ktExpression)
}