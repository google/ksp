/*
 * Copyright 2026 Google LLC
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

class InheritedPropertyAnnotationProcessor(
    override val enableNewFeatures: Boolean,
) : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        listOf("Control", "WithInterface").forEach { name ->
            val declaration = checkNotNull(resolver.getClassDeclarationByName(name))
            val property = declaration.getAllProperties().single { it.simpleName.asString() == "name" }
            val column = property.annotations.singleOrNull { it.shortName.asString() == "Column" }
            val value = column?.arguments?.single { it.name?.asString() == "name" }?.value
            results += "$name.name: $value"
        }
        return emptyList()
    }

    override fun toResult(): List<String> = results
}
