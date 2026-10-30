/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.impl.recordClassReferenceLookup
import com.google.devtools.ksp.impl.symbol.java.KSValueArgumentLiteImpl
import com.google.devtools.ksp.impl.symbol.java.calcValue
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiAnnotationMethod
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaBaseNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.name.ClassId

/**
 * Contains helper functions for implementing [KSAnnotation].
 */
internal object KSAnnotationOps {

    /**
     * Finds and synthesizes an unassigned vararg argument if one was omitted at the annotation use site.
     *
     * In Kotlin, an unsupplied vararg parameter defaults to an empty array (represented as an empty list).
     * If the annotation defines an unassigned vararg parameter that was omitted from [presentArgs] and does
     * not have an explicit default value, this returns a synthetic [KSValueArgument] containing an empty list.
     *
     * @param annotationApplication The [KSAnnotation] call site / instance to which this vararg argument belongs.
     * @param classId The [ClassId] of the annotation class declaration, or `null` if unresolvable.
     * @param presentArgs The list of arguments present at the call site.
     * @return A synthetic [KSValueArgument] with an empty list if a vararg parameter was omitted, or `null` otherwise.
     */
    @JvmStatic

    fun findAbsentVararg(
        annotationApplication: KSAnnotation,
        classId: ClassId?,
        presentArgs: List<KaNamedAnnotationValue>
    ): KSValueArgument? {
        if (classId == null) {
            return null
        }

        return analyze {
            val kaClassSymbol = classId.toKtClassSymbol() ?: return@analyze null
            if (isJavaSource(kaClassSymbol)) {
                findAbsentJavaSourceVararg(annotationApplication, kaClassSymbol, presentArgs)
            } else {
                findAbsentKotlinVararg(annotationApplication, kaClassSymbol, presentArgs)
            }
        }
    }

    private const val JAVA_VARARG_METHOD_NAME = "value"

    /**
     * Finds and synthesizes an omitted array-typed `value` argument for Java source annotations.
     *
     * In Java annotations, an attribute like `String[] value();` behaves like a vararg parameter when applied
     * at a Kotlin call site (e.g., allowing `@MyAnno` with no arguments). Because Java annotations lack a
     * Kotlin primary constructor or `vararg` parameter flags, this method inspects the Java declaration's
     * annotation methods via [PsiClass]. If a method named `"value"` has an array return type, does not define
     * a default value, and was omitted at the call site, a synthetic [KSValueArgument] containing an empty list is created.
     *
     * @param annotationApplication The [KSAnnotation] call site / instance to which this vararg argument belongs.
     * @param kaClassSymbol The [KaClassSymbol] of the Java annotation declaration.
     * @param presentArgs The list of arguments present at the call site.
     * @return A synthetic [KSValueArgument] for `"value"` with an empty list, or `null` if not applicable or already present.
     */
    @JvmStatic
    private fun findAbsentJavaSourceVararg(
        annotationApplication: KSAnnotation,
        kaClassSymbol: KaClassSymbol,
        presentArgs: List<KaNamedAnnotationValue>
    ): KSValueArgument? {
        val psi = kaClassSymbol.psi as? PsiClass ?: return null
        val varargAnnotationMethod =
            psi.allMethods
                .filterIsInstance<PsiAnnotationMethod>()
                .find(::isJavaVararg) ?: return null

        val varargIsPresentAtCallSite = presentArgs.any { it.name.identifier == varargAnnotationMethod.name }
        if (varargIsPresentAtCallSite) {
            return null
        }

        return KSValueArgumentLiteImpl(
            name = KSNameImpl.getCached(varargAnnotationMethod.name),
            value = emptyList<Any?>(),
            parent = annotationApplication,
            origin = Origin.SYNTHETIC,
            location = annotationApplication.location
        )
    }

    private fun isJavaVararg(psiAnnotationMethod: PsiAnnotationMethod): Boolean =
        psiAnnotationMethod.name == JAVA_VARARG_METHOD_NAME &&
            psiAnnotationMethod.defaultValue == null &&
            psiAnnotationMethod.returnType is PsiArrayType

    /**
     * Finds and synthesizes an unassigned vararg argument for Kotlin-defined annotations.
     *
     * Inspects the annotation's primary constructor value parameters via [KaClassSymbol.memberScope].
     * If a parameter is marked with `vararg` and does not have an explicit default value, and was omitted
     * from [presentArgs], a synthetic [KSValueArgument] containing an empty list is created.
     *
     * @param annotationApplication The [KSAnnotation] call site / instance to which this vararg argument belongs.
     * @param kaClassSymbol The [KaClassSymbol] of the Kotlin annotation declaration.
     * @param presentArgs The list of arguments present at the call site.
     * @return A synthetic [KSValueArgument] with an empty list if a vararg parameter was omitted, or `null` otherwise.
     */
    @JvmStatic
    private fun KaSession.findAbsentKotlinVararg(
        annotationApplication: KSAnnotation,
        kaClassSymbol: KaClassSymbol,
        presentArgs: List<KaNamedAnnotationValue>
    ): KSValueArgument? {
        val constructor = kaClassSymbol.memberScope.constructors.singleOrNull() ?: return null
        val vararg = constructor.valueParameters.find { it.isVararg && !it.hasDefaultValue } ?: return null

        val varargIsPresentAtCallSite = presentArgs.any { it.name == vararg.name }
        if (varargIsPresentAtCallSite) {
            return null
        }

        return KSValueArgumentLiteImpl(
            name = KSNameImpl.getCached(vararg.name.asString()),
            value = emptyList<Any?>(),
            parent = annotationApplication,
            origin = Origin.SYNTHETIC,
            location = annotationApplication.location
        )
    }

    private fun isJavaSource(symbol: KaClassSymbol): Boolean =
        symbol.origin == KaSymbolOrigin.JAVA_SOURCE && symbol.psi != null &&
            symbol.psi !is ClsClassImpl && symbol.psi is PsiClass

    /**
     * Returns any default arguments defined by the annotation that were not explicitly supplied at the call site.
     *
     * @param ksAnnotation The [KSAnnotation] call site / instance whose default arguments are being retrieved.
     * @param presentArgs The list of arguments explicitly provided at the call site.
     * @return A list of [KSValueArgument]s representing default values for all parameters omitted from [presentArgs].
     */
    @JvmStatic
    fun findAbsentDefaultArguments(
        ksAnnotation: KSAnnotation,
        presentArgs: List<KSValueArgument>
    ): List<KSValueArgument> {
        val presentNames = presentArgs.mapNotNull { it.name?.asString() }
        return ksAnnotation.defaultArguments.filter { arg ->
            arg.name?.asString() !in presentNames
        }
    }

    /**
     * Resolves and returns all default argument values defined on an annotation declaration.
     *
     * For Java source annotations ([PsiClass]), default values are extracted from [PsiAnnotationMethod.defaultValue].
     * For Kotlin annotations, default values are retrieved from the primary constructor value parameters,
     * and class reference lookups are recorded for incremental processing when default values contain class literals.
     *
     * @param ksAnnotation The [KSAnnotation] call site / instance to which the resolved default arguments will belong.
     * @param classId The [ClassId] of the annotation class declaration, or `null`.
     * @return A list of [KSValueArgument]s representing all default arguments defined by the annotation.
     */
    @OptIn(KaImplementationDetail::class)
    @JvmStatic
    fun findDefaultArguments(ksAnnotation: KSAnnotation, classId: ClassId?): List<KSValueArgument> {
        if (classId == null) return emptyList()
        return analyze {
            classId.toKtClassSymbol()?.let { symbol ->
                if (isJavaSource(symbol)) {
                    (symbol.psi as PsiClass).allMethods.filterIsInstance<PsiAnnotationMethod>()
                        .mapNotNull { annoMethod ->
                            annoMethod.defaultValue?.let { value ->
                                KSValueArgumentLiteImpl(
                                    KSNameImpl.getCached(annoMethod.name),
                                    calcValue(value, ksAnnotation),
                                    ksAnnotation,
                                    Origin.SYNTHETIC,
                                    value.toLocation()
                                )
                            }
                        }
                } else {
                    symbol.memberScope.constructors.singleOrNull()?.let {
                        it.valueParameters.mapNotNull { valueParameterSymbol ->
                            if (!valueParameterSymbol.hasDefaultValue) return@mapNotNull null
                            val constantValue = valueParameterSymbol.getDefaultValue() ?: return@mapNotNull null
                            if (constantValue is KaAnnotationValue.ClassLiteralValue) {
                                ksAnnotation.findParentDeclaration()?.let { decl ->
                                    recordClassReferenceLookup(constantValue.type, decl)
                                }
                            }
                            KSValueArgumentImpl.getCached(
                                KaBaseNamedAnnotationValue(
                                    valueParameterSymbol.name,
                                    constantValue
                                ),
                                ksAnnotation,
                                Origin.SYNTHETIC
                            )
                        }
                    }
                }
            } ?: emptyList()
        }
    }
}
