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
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtArrayAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtConstantAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtEnumEntryAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtKClassAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KtUnsupportedAnnotationValue

class KSValueArgumentImpl private constructor(
    private val namedAnnotationValue: KtNamedAnnotationValue,
    override val origin: Origin
) : KSValueArgument, Deferrable {
    companion object : KSObjectCache<KtNamedAnnotationValue, KSValueArgumentImpl>() {
        fun getCached(namedAnnotationValue: KtNamedAnnotationValue, origin: Origin) =
            cache.getOrPut(namedAnnotationValue) { KSValueArgumentImpl(namedAnnotationValue, origin) }
    }

    override val name: KSName? by lazy {
        KSNameImpl.getCached(namedAnnotationValue.name.asString())
    }

    override val isSpread: Boolean = false

    override val value: Any? = namedAnnotationValue.expression.toValue()

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val location: Location by lazy {
        namedAnnotationValue.expression.sourcePsi?.toLocation() ?: NonExistLocation
    }

    override val parent: KSNode?
        get() = TODO("Not yet implemented")

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueArgument(this, data)
    }

    override fun toString(): String {
        return "${name?.asString() ?: ""}:$value"
    }

    private fun KtAnnotationValue.toValue(): Any? = when (this) {
        is KtArrayAnnotationValue -> this.values.map { it.toValue() }
        is KtAnnotationApplicationValue -> KSAnnotationImpl.getCached(this.annotationValue)
        // TODO: Enum entry should return a type, use declaration as a placeholder.
        is KtEnumEntryAnnotationValue -> this.callableId?.classId?.let {
            analyze {
                it.toKtClassSymbol()?.let {
                    it.declarations().filterIsInstance<KSClassDeclarationEnumEntryImpl>().singleOrNull {
                        it.simpleName.asString() == this@toValue.callableId?.callableName?.asString()
                    }
                }
            }
        } ?: KSErrorType
        // TODO: handle local classes.
        is KtKClassAnnotationValue -> {
            val classDeclaration =
                (this@toValue.classId?.toKtClassSymbol())?.let { KSClassDeclarationImpl.getCached(it) }
            classDeclaration?.asStarProjectedType() ?: KSErrorType
        }
        is KtConstantAnnotationValue -> this.constantValue.value
        is KtUnsupportedAnnotationValue -> null
    }

    override fun defer(): Restorable = Restorable { getCached(namedAnnotationValue, origin) }
}
