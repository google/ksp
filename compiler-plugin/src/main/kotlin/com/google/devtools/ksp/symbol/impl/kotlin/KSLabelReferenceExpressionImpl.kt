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

import com.google.devtools.ksp.symbol.KSLabelReferenceExpression
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel

open class KSLabelReferenceExpressionImpl(ktExpression: KtExpression) : KSLabelReferenceExpression, KSExpressionImpl(ktExpression) {
    companion object : KSObjectCache<KtExpression, KSLabelReferenceExpressionImpl>() {
        fun getCached(ktExpression: KtExpression) = cache.getOrPut(ktExpression) { KSLabelReferenceExpressionImpl(ktExpression) }
    }

    override val name: String? by lazy {
        (ktExpression as? KtExpressionWithLabel)?.getLabelName()
    }
}