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

import com.google.devtools.ksp.symbol.JumpKind
import com.google.devtools.ksp.symbol.KSExpression
import com.google.devtools.ksp.symbol.KSJumpExpression
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import org.jetbrains.kotlin.psi.*

class KSJumpExpressionImpl private constructor(ktExpression: KtExpression) : KSJumpExpression, KSLabelReferenceExpressionImpl(ktExpression) {
    companion object : KSObjectCache<KtExpression, KSJumpExpressionImpl>() {
        fun getCached(ktExpression: KtExpression) = cache.getOrPut(ktExpression) { KSJumpExpressionImpl(ktExpression) }
    }

    override val kind: JumpKind by lazy {
        when(ktExpression) {
            is KtContinueExpression -> JumpKind.Continue
            is KtBreakExpression -> JumpKind.Break
            is KtReturnExpression -> JumpKind.Return
            is KtThrowExpression -> JumpKind.Throw
            else -> error("Unknown kind of jump expression: ${ktExpression.javaClass.name}")
        }
    }

    override val body: KSExpression? by lazy {
        when(ktExpression) {
            is KtReturnExpression -> ktExpression.returnedExpression?.toKSExpression()
            is KtThrowExpression -> ktExpression.thrownExpression?.toKSExpression()
            else -> null
        }
    }
}