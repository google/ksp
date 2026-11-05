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

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.symbol.*
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue

class KSValueArgumentImpl private constructor(
    private val namedAnnotationValue: KaNamedAnnotationValue,
    override val parent: KSNode?,
    override val origin: Origin
) : AbstractKSValueArgumentImpl() {
    companion object : KSObjectCache<IdKeyPair<KaNamedAnnotationValue, KSNode?>, KSValueArgumentImpl>() {
        fun getCached(namedAnnotationValue: KaNamedAnnotationValue, parent: KSNode?, origin: Origin) =
            cache.getOrPut(IdKeyPair(namedAnnotationValue, parent)) {
                KSValueArgumentImpl(namedAnnotationValue, parent, origin)
            }
    }

    override val name: KSName? by lazy {
        KSNameImpl.getCached(namedAnnotationValue.name.asString())
    }

    override val isSpread: Boolean = false

    override val value: Any? by lazy {
        namedAnnotationValue.expression.toValue(this, origin)
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val location: Location by lazy {
        namedAnnotationValue.expression.sourcePsi?.toLocation() ?: NonExistLocation
    }
}

abstract class AbstractKSValueArgumentImpl : KSValueArgument, Deferrable {
    override fun defer(): Restorable? {
        val parentAnno = parent as? KSAnnotation
        val annotationRestorer = (parent as? DeferrableAnnotation)?.deferAnnotation() ?: return null
        val argName = name?.asString()
        val argIndex = parentAnno?.arguments?.indexOfFirst { it === this } ?: -1
        val defaultArgIndex =
            if (argIndex < 0) parentAnno?.defaultArguments?.indexOfFirst { it === this } ?: -1 else -1
        val isSynthetic = origin == Origin.SYNTHETIC
        return Restorable {
            val restoredAnnotation = annotationRestorer() ?: return@Restorable null
            if (argName != null) {
                if (isSynthetic) {
                    if (argIndex >= 0) {
                        restoredAnnotation.arguments.firstOrNull {
                            it.origin == Origin.SYNTHETIC && it.name?.asString() == argName
                        }
                    } else {
                        restoredAnnotation.defaultArguments.firstOrNull { it.name?.asString() == argName }
                    }
                } else {
                    restoredAnnotation.arguments.firstOrNull { it.name?.asString() == argName }
                }
            } else {
                when {
                    argIndex >= 0 -> restoredAnnotation.arguments.getOrNull(argIndex)
                    defaultArgIndex >= 0 -> restoredAnnotation.defaultArguments.getOrNull(defaultArgIndex)
                    else -> null
                }
            }
        }
    }
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
