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
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument

/**
 * Renders all [KSAnnotation.defaultArguments] and all [KSAnnotation.arguments] of every declaration annotated
 * with one of [annotationNames].
 *
 * Unlike [MultiplatformAnnotationDefaultValueProcessor], which inspects a single argument of declarations of the
 * sources being compiled, this processor renders complete argument lists and also inspects the declarations of
 * the module's dependencies. Rendering complete argument lists makes it possible to assert that a parameter
 * without a default value is absent from [KSAnnotation.defaultArguments] while being present in
 * [KSAnnotation.arguments].
 */
class MultiplatformAnnotationArgumentsProcessor(
    val annotationNames: List<String>,
    override val enableNewFeatures: Boolean,
) : AbstractTestProcessor() {
    private val result = mutableListOf<String>()

    override fun toResult(): List<String> = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        annotationNames.forEach { annotationName ->
            val declarations =
                resolver.annotatedSourceDeclarations(annotationName) +
                    resolver.annotatedLibraryDeclarations(annotationName)

            declarations.forEach { declaration ->
                declaration.annotations
                    .filter { it.qualifiedName() == annotationName }
                    .forEach { annotation ->
                        val header = renderAnnotation(declaration, annotation)
                        result.add("$header default arguments: ${renderArguments(annotation.defaultArguments)}")
                        result.add("$header arguments: ${renderArguments(annotation.arguments)}")
                    }
            }
        }
        return emptyList()
    }

    /**
     * The declarations of the sources being compiled, i.e. the declarations whose annotations are backed by
     * `KSAnnotationImpl`.
     */
    private fun Resolver.annotatedSourceDeclarations(annotationName: String): List<KSDeclaration> =
        getSymbolsWithAnnotation(annotationName)
            .map { it as? KSDeclaration ?: error("$it is not a KSDeclaration") }
            .toList()

    /**
     * The declarations of the module's dependencies, i.e. the declarations whose annotations are backed by
     * `KSAnnotationResolvedImpl`.
     *
     * [Resolver.getSymbolsWithAnnotation] only considers the sources being compiled, so the declarations of
     * dependencies are collected from the package that declares the annotation instead. Unlike the declarations
     * of the sources being compiled, the declarations of dependencies have no containing file.
     */
    @OptIn(KspExperimental::class)
    private fun Resolver.annotatedLibraryDeclarations(annotationName: String) =
        getDeclarationsFromPackage(annotationName.substringBeforeLast('.', ""))
            .filter { declaration ->
                declaration.annotations.any { it.qualifiedName() == annotationName }
            }

    private fun KSAnnotation.qualifiedName(): String? =
        annotationType.resolve().declaration.qualifiedName?.asString()

    private fun renderAnnotation(declaration: KSDeclaration, annotation: KSAnnotation): String =
        "@${annotation.shortName.asString()} ${declaration.simpleName.asString()}"

    private fun renderArguments(arguments: List<KSValueArgument>): String =
        if (arguments.isEmpty())
            "<none>"
        else
            arguments.joinToString { "${it.name?.asString()} = ${it.value}" }
}
