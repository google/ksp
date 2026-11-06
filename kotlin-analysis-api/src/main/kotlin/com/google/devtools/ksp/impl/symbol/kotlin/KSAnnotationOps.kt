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
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
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
            val ctor = kaClassSymbol.memberScope.constructors.singleOrNull() ?: return@analyze null
            val vararg = ctor.valueParameters.find { it.isVararg && !it.hasDefaultValue } ?: return@analyze null

            val varargIsPresentAtCallSite = presentArgs.any { it.name == vararg.name }
            if (varargIsPresentAtCallSite) {
                return@analyze null
            }

            KSValueArgumentLiteImpl(
                name = KSNameImpl.getCached(vararg.name.asString()),
                value = emptyList<Any?>(),
                parent = annotationApplication,
                origin = Origin.SYNTHETIC,
                location = annotationApplication.location
            )
        }
    }

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
        val presentNames = presentArgs.mapNotNullTo(hashSetOf()) { it.name?.asString() }
        return ksAnnotation.defaultArguments.filter { arg ->
            arg.name?.asString() !in presentNames
        }
    }

    /**
     * Resolves and returns all default argument values defined on an annotation declaration.
     *
     * Records a class-reference lookup on the annotation declaration for incremental processing so
     * changes to the annotation's default values invalidate consumers. Default values themselves are
     * evaluated lazily when [KSValueArgument.value] is accessed, so overridden defaults do not pollute
     * the lookup graph with unreferenced class literals or nested annotations.
     *
     * @param ksAnnotation The [KSAnnotation] call site / instance to which the resolved default arguments will belong.
     * @param classId The [ClassId] of the annotation class declaration, or `null`.
     * @return A list of [KSValueArgument]s representing all default arguments defined by the annotation.
     */
    @OptIn(KaImplementationDetail::class)
    @JvmStatic
    fun findDefaultArguments(ksAnnotation: KSAnnotation, classId: ClassId?): List<KSValueArgument> {
        if (classId == null) return emptyList()
        val fqName = classId.asFqNameString()
        if (fqName.startsWith("__KSP_unresolved_") || "<error>" in fqName) return emptyList()
        return analyze {
            classId.toKtClassSymbol()?.let { symbol ->
                recordClassReferenceLookup(fqName, ksAnnotation)
                if (isJavaSource(symbol)) {
                    (symbol.psi as PsiClass).allMethods.filterIsInstance<PsiAnnotationMethod>()
                        .mapNotNull { annoMethod ->
                            annoMethod.defaultValue?.let { value ->
                                KSValueArgumentLiteImpl(
                                    KSNameImpl.getCached(annoMethod.name),
                                    { arg -> calcValue(value, arg, Origin.SYNTHETIC) },
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
                            KSValueArgumentImpl.getCached(
                                KaBaseNamedAnnotationValue(
                                    valueParameterSymbol.name,
                                    constantValue
                                ),
                                ksAnnotation,
                                Origin.SYNTHETIC
                            )
                        }
                    } ?: emptyList()
                }
            } ?: emptyList()
        }
    }

    private fun isJavaSource(symbol: KaClassSymbol): Boolean =
        symbol.origin == KaSymbolOrigin.JAVA_SOURCE && symbol.psi != null &&
            symbol.psi !is ClsClassImpl && symbol.psi is PsiClass
}
