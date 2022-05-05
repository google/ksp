/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue

class KSValueArgumentImpl private constructor(
    private val namedAnnotationValue: KtNamedAnnotationValue
) : KSValueArgument {
    companion object : KSObjectCache<KtNamedAnnotationValue, KSValueArgumentImpl>() {
        fun getCached(namedAnnotationValue: KtNamedAnnotationValue) =
            cache.getOrPut(namedAnnotationValue) { KSValueArgumentImpl(namedAnnotationValue) }
    }

    override val name: KSName? by lazy {
        KSNameImpl.getCached(namedAnnotationValue.name.asString())
    }

    override val isSpread: Boolean = false

    override val value: Any = namedAnnotationValue.expression

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        namedAnnotationValue.expression.sourcePsi?.toLocation() ?: NonExistLocation
    }

    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueArgument(this, data)
    }
}
