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

import com.google.devtools.ksp.symbol.KSBlockExpression
import com.google.devtools.ksp.symbol.KSExpression
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import com.intellij.psi.PsiCodeBlock
import org.jetbrains.kotlin.psi.KtBlockExpression

class KSBlockExpressionImpl private constructor(ktBlockExpression: KtBlockExpression) : KSBlockExpression, KSExpressionImpl(ktBlockExpression) {
    companion object : KSObjectCache<KtBlockExpression, KSBlockExpressionImpl>() {
        fun getCached(ktBlockExpression: KtBlockExpression) = cache.getOrPut(ktBlockExpression) { KSBlockExpressionImpl(ktBlockExpression) }
    }

    override val statements: List<KSExpression> by lazy {
        ktBlockExpression.statements.mapNotNull { it.toKSExpression() }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitBlockExpression(this, data)
    }
}