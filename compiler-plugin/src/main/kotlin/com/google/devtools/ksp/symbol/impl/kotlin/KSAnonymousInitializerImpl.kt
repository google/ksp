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
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassInitializer

class KSAnonymousInitializerImpl private constructor(val ktAnonymousInitializer: KtAnonymousInitializer) : KSAnonymousInitializer,
    KSDeclarationImpl(ktAnonymousInitializer), KSExpectActual by KSExpectActualImpl(ktAnonymousInitializer) {
    companion object : KSObjectCache<KtAnonymousInitializer, KSAnonymousInitializerImpl>() {
        fun getCached(ktAnonymousInitializer: KtAnonymousInitializer) =
            cache.getOrPut(ktAnonymousInitializer) { KSAnonymousInitializerImpl(ktAnonymousInitializer) }
    }

    override val statements: List<KSExpression> by lazy {
        (ktAnonymousInitializer.body as? KtBlockExpression)?.statements?.mapNotNull {
            it.toKSExpression()
        } ?: emptyList()
    }

    override val text: String by lazy {
        ktAnonymousInitializer.text
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnonymousInitializer(this, data)
    }

    override fun toString(): String = text
}

