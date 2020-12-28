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
import com.google.devtools.ksp.symbol.impl.toKSToken
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS

class KSTypeCastExpressionImpl private constructor(val ktBinaryExpressionWithTypeRHS: KtBinaryExpressionWithTypeRHS) :
    KSTypeCastExpression, KSExpressionImpl(ktBinaryExpressionWithTypeRHS) {
    companion object : KSObjectCache<KtBinaryExpressionWithTypeRHS, KSTypeCastExpressionImpl>() {
        fun getCached(ktBinaryExpressionWithTypeRHS: KtBinaryExpressionWithTypeRHS) =
            cache.getOrPut(ktBinaryExpressionWithTypeRHS) { KSTypeCastExpressionImpl(ktBinaryExpressionWithTypeRHS) }
    }

    override val token: KSToken by lazy {
        ktBinaryExpressionWithTypeRHS.toKSToken()
    }

    override val type: KSTypeReference? by lazy {
        ktBinaryExpressionWithTypeRHS.right?.let(KSTypeReferenceImpl::getCached)
    }
    override val body: KSExpression by lazy {
        ktBinaryExpressionWithTypeRHS.left.toKSExpression()
            ?: error("Cannot get the expression to the left of '${token.symbol}'.")
    }
}