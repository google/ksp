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

import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue

class KSValueArgumentImpl private constructor(
    private val namedAnnotationValue: KaNamedAnnotationValue,
    override val parent: KSNode?,
    override val origin: Origin
) : AbstractKSValueArgumentImpl(), Deferrable {
    companion object : KSObjectCache<KaNamedAnnotationValue, KSValueArgumentImpl>() {
        fun getCached(namedAnnotationValue: KaNamedAnnotationValue, parent: KSNode?, origin: Origin) =
            cache.getOrPut(namedAnnotationValue) {
                KSValueArgumentImpl(namedAnnotationValue, parent, origin)
            }
    }

    override val name: KSName? by lazy {
        KSNameImpl.getCached(namedAnnotationValue.name.asString())
    }

    override val isSpread: Boolean = false

    override val value: Any? by lazy {
        namedAnnotationValue.expression.toValue()
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val location: Location by lazy {
        namedAnnotationValue.expression.sourcePsi?.toLocation() ?: NonExistLocation
    }

    override fun defer(): Restorable {
        val parent = if (parent is Deferrable) parent.defer() else null
        return Restorable { getCached(namedAnnotationValue, parent?.restore(), origin) }
    }
}

abstract class AbstractKSValueArgumentImpl : KSValueArgument {
    override fun hashCode(): Int {
        return name.hashCode() * 31 + value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KSValueArgument)
            return false

        return other.name == this.name && other.value == this.value
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueArgument(this, data)
    }

    override fun toString(): String {
        return "${name?.asString() ?: ""}:$value"
    }
}
