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
import com.google.devtools.ksp.impl.symbol.java.KSValueArgumentLiteImpl
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
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

    private fun isJavaSource(symbol: KaClassSymbol): Boolean =
        symbol.origin == KaSymbolOrigin.JAVA_SOURCE && symbol.psi != null &&
            symbol.psi !is ClsClassImpl && symbol.psi is PsiClass
}
