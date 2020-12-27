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

import com.google.devtools.ksp.symbol.KSExpression
import com.google.devtools.ksp.symbol.KSIfExpression
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import org.jetbrains.kotlin.psi.KtIfExpression

class KSIfExpressionImpl private constructor(ktIfExpression: KtIfExpression) : KSIfExpression, KSExpressionImpl(ktIfExpression) {
    companion object : KSObjectCache<KtIfExpression, KSIfExpressionImpl>() {
        fun getCached(ktIfExpression: KtIfExpression) = cache.getOrPut(ktIfExpression) { KSIfExpressionImpl(ktIfExpression) }
    }

    override val condition: KSExpression by lazy {
        ktIfExpression.condition?.toKSExpression()
            ?: error("Failed to get the if expression condition.")
    }

    override val then: KSExpression by lazy {
        ktIfExpression.then?.toKSExpression()
            ?: error("Failed to get the true branch in the if expression.")
    }

    override val otherwise: KSExpression? by lazy {
        ktIfExpression.`else`?.toKSExpression()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitIfExpression(this, data)
    }
}