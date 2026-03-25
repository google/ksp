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

class AnnotationTypeArgumentsProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("MapConvert").forEach { annotated ->
            val name = (annotated as KSDeclaration).simpleName.asString()
            val annotation = annotated.annotations.single { it.shortName.asString() == "MapConvert" }
            results.addAll(renderAnnotation(name, annotation))
        }

        resolver.getSymbolsWithAnnotation("Container").forEach { annotated ->
            val name = (annotated as KSDeclaration).simpleName.asString()
            val containerAnnotation = annotated.annotations.single { it.shortName.asString() == "Container" }
            val nestedAnnotation = containerAnnotation.arguments.single().value as KSAnnotation
            results.addAll(renderAnnotation("$name.nested", nestedAnnotation))
        }

        resolver.getClassDeclarationByName("com.example.LibClass")?.let { annotated ->
            val name = (annotated as KSDeclaration).simpleName.asString()
            val annotation = annotated.annotations.single { it.shortName.asString() == "LibAnno" }
            results.addAll(renderAnnotation(name, annotation))
        }

        return emptyList()
    }

    private fun renderAnnotation(label: String, annotation: KSAnnotation): List<String> {
        val typeArguments = annotation.annotationType.element!!.typeArguments
        return buildList {
            add("$label.annotationType: ${annotation.annotationType}")
            add("$label.typeArgCount: ${typeArguments.size}")
            if(typeArguments.isNotEmpty()) {
                add("$label.typeArgs: ${
                    typeArguments.joinToString { argument ->
                        argument.type!!.resolve().declaration.qualifiedName!!.asString()
                    }
                }")
            }
        }
    }

    override fun toResult(): List<String> = results.sorted()
}
