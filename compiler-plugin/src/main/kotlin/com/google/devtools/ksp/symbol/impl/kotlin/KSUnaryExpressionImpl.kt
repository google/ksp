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
import com.google.devtools.ksp.symbol.KSToken
import com.google.devtools.ksp.symbol.KSUnaryExpression
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import com.google.devtools.ksp.symbol.impl.toKSToken
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtUnaryExpression

class KSUnaryExpressionImpl private constructor(ktUnaryExpression: KtUnaryExpression) : KSUnaryExpression, KSExpressionImpl(ktUnaryExpression) {
    companion object : KSObjectCache<KtUnaryExpression, KSUnaryExpressionImpl>() {
        fun getCached(ktUnaryExpression: KtUnaryExpression) = cache.getOrPut(ktUnaryExpression) { KSUnaryExpressionImpl(ktUnaryExpression) }
    }

    override val isPostfixOperation: Boolean by lazy {
        ktUnaryExpression is KtPostfixExpression
    }

    override val token: KSToken by lazy {
        ktUnaryExpression.toKSToken()
    }

    override val body: KSExpression by lazy {
        ktUnaryExpression.baseExpression.toKSExpression() ?: error("Unexpected unary expression: ${ktUnaryExpression.text}")
    }
}