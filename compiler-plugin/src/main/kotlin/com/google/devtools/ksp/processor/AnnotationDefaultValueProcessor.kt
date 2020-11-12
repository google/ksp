/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class AnnotationDefaultValueProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val ktClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("A"))!!
        logAnnotations(ktClass)
        val javaClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("JavaAnnotated"))!!
        logAnnotations(javaClass)
        return emptyList()
    }

    private fun logAnnotations(classDeclaration: KSClassDeclaration) {
        classDeclaration.annotations.forEach { annotation ->
            result.add("${annotation.shortName.asString()} -> ${annotation.arguments.map { "${it.name?.asString()}:${it.value}" }.joinToString(",")}")
        }
    }
}
