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

class AnnotationTypeArgumentsInLibraryProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        renderAnnotationIfVisible(
            resolver,
            className = "com.example.SourceRetentionTarget",
            annotationName = "SourceRetentionAnno",
        )
        renderAnnotationIfVisible(
            resolver,
            className = "com.example.BinaryRetentionTarget",
            annotationName = "BinaryRetentionAnno",
        )
        renderAnnotationIfVisible(
            resolver,
            className = "com.example.RuntimeRetentionTarget",
            annotationName = "RuntimeRetentionAnno",
        )
        return emptyList()
    }

    private fun renderAnnotationIfVisible(
        resolver: Resolver,
        className: String,
        annotationName: String,
    ) {
        val declaration = resolver.getClassDeclarationByName(className) ?: return
        val annotation = declaration.annotations.singleOrNull {
            it.shortName.asString() == annotationName
        } ?: return
        results.addAll(renderAnnotation(declaration.simpleName.asString(), annotation))
    }

    private fun renderAnnotation(label: String, annotation: KSAnnotation): List<String> {
        val typeArguments = annotation.annotationType.element!!.typeArguments
        return buildList {
            add("$label.annotationType: ${annotation.annotationType}")
            add("$label.typeArgCount: ${typeArguments.size}")
            if (typeArguments.isNotEmpty()) {
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
