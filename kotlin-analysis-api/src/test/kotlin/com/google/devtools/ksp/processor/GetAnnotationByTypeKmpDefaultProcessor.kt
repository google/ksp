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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

annotation class KmpAnnotationDefaults(
    val required: String,
    val enabled: Boolean = true,
    vararg val values: String,
)

class GetAnnotationByTypeKmpDefaultProcessor(
    override val enableNewFeatures: Boolean,
) : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().single().declarations
            .filterIsInstance<KSClassDeclaration>()
            .sortedBy { it.simpleName.asString() }
            .forEach { declaration ->
                val annotation = declaration.getAnnotationsByType(KmpAnnotationDefaults::class).single()
                val defaultNames = declaration.annotations.single().defaultArguments
                    .map { it.name!!.asString() }
                    .sorted()
                results.add(
                    "${declaration.simpleName.asString()}: " +
                        "required=${annotation.required}, enabled=${annotation.enabled}, " +
                        "values=${annotation.values.contentToString()}, defaults=$defaultNames"
                )
            }
        return emptyList()
    }
}
