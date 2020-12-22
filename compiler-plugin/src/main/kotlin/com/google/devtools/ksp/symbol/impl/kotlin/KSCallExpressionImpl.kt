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

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSModifiers
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import java.lang.IllegalStateException

class KSCallExpressionImpl private constructor(ktExpression: KtExpression) :
    KSExpressionImpl(ktExpression), KSCallExpression {
    companion object : KSObjectCache<KtExpression, KSCallExpressionImpl>() {
        fun getCached(ktTypeReference: KtExpression) = cache.getOrPut(ktTypeReference) { KSCallExpressionImpl(ktTypeReference) }
    }

    override val receiver: KSExpression? by lazy {
        when (ktExpression) {
            is KtDotQualifiedExpression -> getCached(ktExpression.receiverExpression)
            else -> null
        }
    }

    override val name: String? by lazy {
        ktExpression.name
    }

    override val arguments: List<KSValueArgumentExpression>? by lazy {
        (ktExpression as? KtCallExpression)?.valueArguments?.mapIndexed { index, valueArgument ->
            KSValueArgumentExpressionImpl.getCached(index, valueArgument)
        }
    }

    override val returnType: KSTypeReference by lazy {
//        ktExpression.getResolvedCallWithAssert(ResolverImpl.instance.bindingTrace.bindingContext).resultingDescriptor.containingDeclaration.
//        KSTypeReferenceImpl.getCached()
//        ktExpression
        TODO()
    }


    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        TODO()
    }
}