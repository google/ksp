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
import com.google.devtools.ksp.impl.symbol.java.KSValueArgumentLiteImpl
import com.google.devtools.ksp.impl.symbol.java.calcValue
import com.google.devtools.ksp.symbol.*
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.psi.KtAnnotationEntry

// TODO: implement a psi based version of annotation application.
class KSAnnotationImpl private constructor(
    private val ktAnnotationEntry: KtAnnotationEntry,
    override val parent: KSNode?,
    private val resolveToAnnotationApplication: () -> KaAnnotation
) : KSAnnotation {
    companion object : KSObjectCache<IdKeyPair<KtAnnotationEntry, KSNode?>, KSAnnotationImpl>() {
        fun getCached(
            ktAnnotationEntry: KtAnnotationEntry,
            parent: KSNode? = null,
            resolveToAnnotationApplication: () -> KaAnnotation
        ) =
            cache.getOrPut(IdKeyPair(ktAnnotationEntry, parent)) {
                KSAnnotationImpl(ktAnnotationEntry, parent, resolveToAnnotationApplication)
            }
    }

    private val annotationApplication by lazy {
        resolveToAnnotationApplication()
    }

    override val annotationType: KSTypeReference by lazy {
        analyze {
            ktAnnotationEntry.typeReference!!.let {
                KSTypeReferenceImpl.getCached(
                    it,
                    this@KSAnnotationImpl
                )
            }
        }
    }

    override val arguments: List<KSValueArgument> by lazy {
        val presentArgs = annotationApplication.arguments.map {
            KSValueArgumentImpl.getCached(it, this, origin)
        }
        val presentNames = presentArgs.mapNotNull { it.name?.asString() }
        val absentArgs = defaultArguments.filter {
            it.name?.asString() !in presentNames
        }
        presentArgs + absentArgs
    }

    @OptIn(KaImplementationDetail::class)
    override val defaultArguments: List<KSValueArgument> by lazy {
        analyze {
            annotationApplication.classId?.toKtClassSymbol()?.let { symbol ->
                if (symbol.origin == KaSymbolOrigin.JAVA_SOURCE && symbol.psi != null && symbol.psi !is ClsClassImpl) {
                    (symbol.psi as PsiClass).allMethods.filterIsInstance<PsiAnnotationMethod>()
                        .mapNotNull { annoMethod ->
                            annoMethod.defaultValue?.let { value ->
                                val calculatedValue: Any? = if (value is PsiArrayInitializerMemberValue) {
                                    value.initializers.map {
                                        calcValue(it)
                                    }
                                } else {
                                    calcValue(value)
                                }
                                KSValueArgumentLiteImpl.getCached(
                                    KSNameImpl.getCached(annoMethod.name),
                                    calculatedValue,
                                    this@KSAnnotationImpl,
                                    Origin.SYNTHETIC,
                                    value.toLocation()
                                )
                            }
                        }
                } else {
                    symbol.memberScope.constructors.singleOrNull()?.let {
                        it.valueParameters.mapNotNull { valueParameterSymbol ->
                            valueParameterSymbol.getDefaultValue().let { constantValue ->
                                KSValueArgumentImpl.getCached(
                                    KaBaseNamedAnnotationValue(
                                        valueParameterSymbol.name,
                                        constantValue ?: return@let null
                                    ),
                                    this@KSAnnotationImpl,
                                    Origin.SYNTHETIC
                                )
                            }
                        }
                    }
                }
            } ?: emptyList()
        }
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(ktAnnotationEntry.shortName!!.asString())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? by lazy {
        when (ktAnnotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()) {
            null -> null
            FILE -> AnnotationUseSiteTarget.FILE
            PROPERTY -> AnnotationUseSiteTarget.PROPERTY
            FIELD -> AnnotationUseSiteTarget.FIELD
            PROPERTY_GETTER -> AnnotationUseSiteTarget.GET
            PROPERTY_SETTER -> AnnotationUseSiteTarget.SET
            RECEIVER -> AnnotationUseSiteTarget.RECEIVER
            CONSTRUCTOR_PARAMETER -> AnnotationUseSiteTarget.PARAM
            SETTER_PARAMETER -> AnnotationUseSiteTarget.SETPARAM
            PROPERTY_DELEGATE_FIELD -> AnnotationUseSiteTarget.DELEGATE
            // FIXME
            ALL -> null
        }
    }

    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        annotationApplication.psi?.toLocation() ?: NonExistLocation
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}
