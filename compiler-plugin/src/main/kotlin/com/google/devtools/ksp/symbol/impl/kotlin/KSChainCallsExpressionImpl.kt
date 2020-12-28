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

import com.google.devtools.ksp.symbol.KSChainCallsExpression
import com.google.devtools.ksp.symbol.KSExpression
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import org.jetbrains.kotlin.psi.*

class KSChainCallsExpressionImpl private constructor(ktExpression: KtExpression, override val chains: List<KSExpression>) :
    KSChainCallsExpression, KSExpressionImpl(ktExpression) {
    companion object : KSObjectCache<Pair<KtExpression, List<KSExpression>>, KSChainCallsExpressionImpl>() {
        fun getCachedOrNull(ktExpression: KtExpression): KSChainCallsExpressionImpl? {
            var base: KtExpression = ktExpression as? KtQualifiedExpression ?: return null
            val chains = ArrayDeque<KSExpression>()

            while (base is KtQualifiedExpression) {
                base.selectorExpression.toKSExpression()?.let(chains::addFirst)
                base = base.receiverExpression
            }

            base.toKSExpression()?.let(chains::addFirst)

            return when {
                chains.size <= 2 -> null
                else -> cache.getOrPut(Pair(ktExpression, chains)) { KSChainCallsExpressionImpl(ktExpression, chains) }
            }
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitChainCallsExpression(this, data)
    }
}