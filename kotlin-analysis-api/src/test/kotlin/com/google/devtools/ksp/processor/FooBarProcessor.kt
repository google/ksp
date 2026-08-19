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

import com.google.devtools.ksp.impl.symbol.kotlin.KSErrorType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.intellij.diagnostic.PluginException

class FooBarProcessor(val annotationNames: List<String>) : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> = results.toList()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        annotationNames.forEach { annotationName ->
            resolver.getSymbolsWithAnnotation(annotationName).forEach { symbol ->
                results.add("$annotationName: $symbol")
                if (symbol is KSClassDeclaration) {
                    symbol.declarations.filterIsInstance<KSFunctionDeclaration>().forEach { func ->
                        func.parameters.forEach { param ->
                            param.annotations.forEach { anno ->
                                anno.arguments.forEach { arg ->
                                    val valueStr = when (val v = arg.value) {
                                        is KSType -> v.toString()
                                        KSErrorType.Companion -> "<ERROR TYPE>"
                                        else -> v?.toString()
                                    }
                                    results.add("Arg: ${arg.name?.asString()} = $valueStr")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verify PluginProblemReporter and PluginException creation (reproducing b/545532844)
        val exception = PluginException.createByClass("Test error", null, FooBarProcessor::class.java)
        results.add("PluginProblemReporter: registered, created ${exception.javaClass.simpleName}")

        return emptyList()
    }
}
