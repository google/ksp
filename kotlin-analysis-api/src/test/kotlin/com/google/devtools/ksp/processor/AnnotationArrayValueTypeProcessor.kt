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

class AnnotationArrayValueTypeProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        logAnnotationArrayValue(resolver, "JavaAnnotated")
        logAnnotationArrayValue(resolver, "KotlinAnnotated")
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

    private fun logAnnotationArrayValue(resolver: Resolver, className: String) {
        val annotated = resolver.getClassDeclarationByName(className)!!
        val annotation = annotated.annotations.single { it.shortName.asString() == "JavaAnnotation" }
        val argument = annotation.arguments.single()
        val value = argument.value
        val argumentName = argument.name?.asString()
        results.add("$className $argumentName is Array<*>: ${value is Array<*>}")
        results.add("$className $argumentName size: ${value.sizeOrNull()}")
    }

    private fun Any?.sizeOrNull(): Int? {
        return when (this) {
            is Array<*> -> size
            is Collection<*> -> size
            else -> null
        }
    }
}
