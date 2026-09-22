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
package com.google.devtools.ksp.impl.symbol.kotlin.resolved

import com.google.devtools.ksp.common.IdKeyPair
import com.google.devtools.ksp.common.KSObjectCache
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.impl.recordClassReferenceLookup
import com.google.devtools.ksp.impl.symbol.java.KSValueArgumentLiteImpl
import com.google.devtools.ksp.impl.symbol.java.calcValue
import com.google.devtools.ksp.impl.symbol.kotlin.KSValueArgumentImpl
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.findParentDeclaration
import com.google.devtools.ksp.impl.symbol.kotlin.getDefaultValue
import com.google.devtools.ksp.impl.symbol.kotlin.toKtClassSymbol
import com.google.devtools.ksp.impl.symbol.kotlin.toLocation
import com.google.devtools.ksp.symbol.AnnotationUseSiteTarget
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.ALL
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.FIELD
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.FILE
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY_GETTER
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY_SETTER
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.RECEIVER
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.SETTER_PARAMETER
import org.jetbrains.kotlin.psi.KtFile

/**
 * A resolved [KSAnnotation] backed directly by an Analysis API [KaAnnotation],
 * rather than a source PSI node ([KtAnnotationEntry][org.jetbrains.kotlin.psi.KtAnnotationEntry] or
 * [PsiAnnotation][com.intellij.psi.PsiAnnotation]).
 *
 * This implementation is used when annotations are resolved semantically or lack direct source PSI,
 * such as:
 * - Annotations loaded from compiled bytecode/libraries (binary symbols).
 * - Annotations on resolved types or type references.
 * - Nested annotations passed as arguments inside another annotation.
 * - Synthesized annotations (such as `@ExtensionFunctionType`).
 * - Annotations mapped via use-site targets on accessors or backing fields.
 *
 * Because there is no direct source PSI annotation entry, [location] is always [NonExistLocation].
 *
 * @param annotationApplication The underlying resolved Analysis API [KaAnnotation] representation.
 * @param parent The parent AST node on which this annotation is applied (e.g., declaration, type reference, or value argument).
 * @param origin The source or binary origin of this annotation (e.g., [Origin.KOTLIN], [Origin.JAVA], [Origin.KOTLIN_LIB], [Origin.JAVA_LIB], or [Origin.SYNTHETIC]).
 */
class KSAnnotationResolvedImpl private constructor(
    private val annotationApplication: KaAnnotation,
    override val parent: KSNode?,
    override val origin: Origin,
) : KSAnnotation {

    companion object :
        KSObjectCache<IdKeyPair<KaAnnotation, KSNode?>, KSAnnotationResolvedImpl>() {

        /**
         * Returns a cached [KSAnnotationResolvedImpl] for the given [KaAnnotation].
         *
         * @param annotationApplication The underlying resolved Analysis API [KaAnnotation] representation.
         * @param parent The parent AST node on which this annotation is applied. Defaults to `null`.
         * @param origin The origin of this annotation. If not specified, defaults to `parent?.origin` or [Origin.SYNTHETIC].
         */
        fun getCached(annotationApplication: KaAnnotation, parent: KSNode? = null, origin: Origin? = parent?.origin) =
            cache.getOrPut(IdKeyPair(annotationApplication, parent)) {
                KSAnnotationResolvedImpl(annotationApplication, parent, origin ?: Origin.SYNTHETIC)
            }
    }

    override val annotationType: KSTypeReference by lazy {
        analyze {
            KSTypeReferenceResolvedImpl.getCached(
                buildClassType(annotationApplication.classId!!),
                parent = this@KSAnnotationResolvedImpl
            )
        }
    }
    override val arguments: List<KSValueArgument> by lazy {
        val presentArgs = annotationApplication.arguments.map { arg ->
            val argOrigin = if (origin == Origin.SYNTHETIC) {
                arg.expression.sourcePsi?.containingFile?.let { containingFile ->
                    if (containingFile is KtFile) Origin.KOTLIN else Origin.JAVA
                } ?: Origin.JAVA_LIB // FIXME: how to tell from KOTLIN_LIB?
            } else origin
            KSValueArgumentImpl.getCached(arg, this, argOrigin)
        }
        val presentNames = presentArgs.mapNotNull { it.name?.asString() }
        val absentArgs = analyze {
            val annotationClass = annotationApplication.classId?.toKtClassSymbol()
            val annotationConstructor = annotationClass?.memberScope?.constructors?.singleOrNull()
            val params = annotationConstructor?.valueParameters
            defaultArguments.filter { arg ->
                val name = arg.name?.asString() ?: return@filter false
                if (name in presentNames)
                    return@filter false
                params?.any { it.name.asString() == name && it.hasDefaultValue } == true
            }
        }
        presentArgs + absentArgs
    }

    @OptIn(KaImplementationDetail::class)
    override val defaultArguments: List<KSValueArgument> by lazy {
        analyze {
            annotationApplication.classId?.toKtClassSymbol()?.let { symbol ->
                if (
                    symbol.origin == KaSymbolOrigin.JAVA_SOURCE && symbol.psi != null &&
                    symbol.psi !is ClsClassImpl && symbol.psi is PsiClass
                ) {
                    (symbol.psi as PsiClass).allMethods.filterIsInstance<PsiAnnotationMethod>()
                        .mapNotNull { annoMethod ->
                            annoMethod.defaultValue?.let { value ->
                                val calculatedValue: Any? = if (value is PsiArrayInitializerMemberValue) {
                                    value.initializers.map {
                                        calcValue(it, this@KSAnnotationResolvedImpl)
                                    }
                                } else {
                                    calcValue(value, this@KSAnnotationResolvedImpl)
                                }
                                KSValueArgumentLiteImpl(
                                    KSNameImpl.getCached(annoMethod.name),
                                    calculatedValue,
                                    this@KSAnnotationResolvedImpl,
                                    Origin.SYNTHETIC,
                                    value.toLocation()
                                )
                            }
                        }
                } else {
                    symbol.memberScope.constructors.singleOrNull()?.let {
                        it.valueParameters.mapNotNull { valueParameterSymbol ->
                            val constantValue = valueParameterSymbol.getDefaultValue() ?: return@mapNotNull null
                            if (constantValue is KaAnnotationValue.ClassLiteralValue) {
                                this@KSAnnotationResolvedImpl.findParentDeclaration()?.let { decl ->
                                    recordClassReferenceLookup(constantValue.type, decl)
                                }
                            }
                            KSValueArgumentImpl.getCached(
                                KaBaseNamedAnnotationValue(
                                    valueParameterSymbol.name,
                                    constantValue
                                ),
                                this@KSAnnotationResolvedImpl,
                                Origin.SYNTHETIC
                            )
                        }
                    }
                }
            } ?: emptyList()
        }
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(annotationApplication.classId!!.shortClassName.asString())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? by lazy {
        // Do not use compiler hard-coded use-site target.
        // FIXME: use origin after it is fixed.
        if (parent?.origin == Origin.KOTLIN_LIB || parent?.origin == Origin.JAVA_LIB)
            return@lazy null

        when (annotationApplication.useSiteTarget) {
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
            ALL -> AnnotationUseSiteTarget.ALL
        }
    }

    override val location: Location by lazy {
        NonExistLocation
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}
