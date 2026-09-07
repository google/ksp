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

package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument

class MultiplatformAnnotationDefaultValueProcessor(
    val annotationNames: List<String>,
    override val enableNewFeatures: Boolean,
) : AbstractTestProcessor() {
    private val result = mutableListOf<String>()

    override fun toResult(): List<String> = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        annotationNames.forEach { annotationName ->
            resolver.getSymbolsWithAnnotation(annotationName).forEach { annotated ->
                annotated.annotations.forEach { annotation ->
                    val defaultArg = annotation.defaultArguments.single()
                    val arg = annotation.arguments.single()
                    result.add("Default argument: ${renderAnnotation(annotated, annotation)}: ${renderArg(defaultArg)}")
                    result.add("Call-site argument: ${renderAnnotation(annotated, annotation)}: ${renderArg(arg)}")
                }
            }
        }
        return emptyList()
    }

    private fun renderAnnotation(annotated: KSAnnotated, annotation: KSAnnotation): String = when (annotated) {
        is KSDeclaration -> "@${annotation.shortName.asString()} ${annotated.simpleName.asString()}"
        else -> error("$annotated is not a KSDeclaration")
    }

    private fun renderArg(argument: KSValueArgument): String =
        "${argument.name?.asString()} = ${argument.value}"
}
