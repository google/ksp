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

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.memoized
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.processing.impl.KSTypeReferenceSyntheticImpl
import com.google.devtools.ksp.processing.impl.findAnnotationFromUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens.CROSSINLINE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.NOINLINE_KEYWORD
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

class KSValueParameterImpl private constructor(val ktParameter: KtParameter) : KSValueParameter {
    companion object : KSObjectCache<KtParameter, KSValueParameterImpl>() {
        fun getCached(ktParameter: KtParameter) = cache.getOrPut(ktParameter) { KSValueParameterImpl(ktParameter) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktParameter.toLocation()
    }

    override val parent: KSNode? by lazy {
        var parentPsi = ktParameter.parent
        while (
            parentPsi != null && parentPsi !is KtAnnotationEntry && parentPsi !is KtFunctionType &&
            parentPsi !is KtFunction && parentPsi !is KtPropertyAccessor
        ) {
            parentPsi = parentPsi.parent
        }
        when (parentPsi) {
            is KtAnnotationEntry -> KSAnnotationImpl.getCached(parentPsi)
            is KtFunctionType -> KSCallableReferenceImpl.getCached(parentPsi)
            is KtFunction -> KSFunctionDeclarationImpl.getCached(parentPsi)
            is KtPropertyAccessor -> if (parentPsi.isSetter) KSPropertySetterImpl.getCached(parentPsi) else null
            else -> null
        }
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktParameter.annotationEntries.asSequence().filter { annotation ->
            annotation.useSiteTarget?.getAnnotationUseSiteTarget()?.let {
                it != AnnotationUseSiteTarget.PROPERTY_GETTER &&
                    it != AnnotationUseSiteTarget.PROPERTY_SETTER &&
                    it != AnnotationUseSiteTarget.SETTER_PARAMETER &&
                    it != AnnotationUseSiteTarget.FIELD
            } ?: true
        }.map { KSAnnotationImpl.getCached(it) }.filterNot { valueParameterAnnotation ->
            valueParameterAnnotation.annotationType.resolve().declaration.annotations.any { metaAnnotation ->
                metaAnnotation.annotationType.resolve().declaration.qualifiedName
                    ?.asString() == "kotlin.annotation.Target" &&
                    (metaAnnotation.arguments.singleOrNull()?.value as? ArrayList<*>)?.none {
                    (it as? KSType)?.declaration?.qualifiedName
                        ?.asString() == "kotlin.annotation.AnnotationTarget.VALUE_PARAMETER"
                } ?: false
            }
        }
            .plus(this.findAnnotationFromUseSiteTarget()).memoized()
    }

    override val isCrossInline: Boolean = ktParameter.hasModifier(CROSSINLINE_KEYWORD)

    override val isNoInline: Boolean = ktParameter.hasModifier(NOINLINE_KEYWORD)

    override val isVararg: Boolean = ktParameter.isVarArg

    override val isVal = ktParameter.hasValOrVar() && !ktParameter.isMutable

    override val isVar = ktParameter.hasValOrVar() && ktParameter.isMutable

    override val name: KSName? by lazy {
        if (ktParameter.name == null) {
            null
        } else {
            KSNameImpl.getCached(ktParameter.name!!)
        }
    }

    override val type: KSTypeReference by lazy {
        ktParameter.typeReference?.let { KSTypeReferenceImpl.getCached(it) }
            ?: findPropertyForAccessor()?.type ?: KSTypeReferenceSyntheticImpl.getCached(KSErrorType, this)
    }

    override val hasDefault: Boolean = ktParameter.hasDefaultValue()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }

    override fun toString(): String {
        return name?.asString() ?: "_"
    }

    private fun findPropertyForAccessor(): KSPropertyDeclaration? {
        return (ktParameter.parent?.parent?.parent as? KtProperty)?.let { KSPropertyDeclarationImpl.getCached(it) }
    }
}
