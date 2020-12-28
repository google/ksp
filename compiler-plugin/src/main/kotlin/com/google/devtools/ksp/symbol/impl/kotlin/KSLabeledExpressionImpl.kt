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
import com.google.devtools.ksp.symbol.KSLabelReferenceExpression
import com.google.devtools.ksp.symbol.KSLabeledExpression
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtLabeledExpression

open class KSLabeledExpressionImpl(ktLabeledExpression: KtLabeledExpression) : KSLabeledExpression, KSLabelReferenceExpressionImpl(ktLabeledExpression) {
    companion object : KSObjectCache<KtLabeledExpression, KSLabeledExpressionImpl>() {
        fun getCached(ktLabeledExpression: KtLabeledExpression) = cache.getOrPut(ktLabeledExpression) { KSLabeledExpressionImpl(ktLabeledExpression) }
    }

    override val name: String by lazy {
        super.name ?: error("Unable to get label name from ${ktLabeledExpression.text}")
    }

    override val body: KSExpression by lazy {
        ktLabeledExpression.baseExpression?.toKSExpression()
            ?: error("Unable to get labeled expression from ${ktLabeledExpression.text}")
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitLabeledExpression(this, data)
    }
}