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

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSContextParameter
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class ContextParameterAnnotationProcessor(
    val packageNames: List<String>,
    override val apiFeatures: ApiFeatures,
) : AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results.sorted()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sourceDeclarations = resolver.getAllFiles().flatMap { file -> file.declarations }
        val libraryDeclarations = packageNames.flatMap(resolver::getDeclarationsFromPackage)
        (sourceDeclarations + libraryDeclarations)
            .flatMap(::contextParametersIn)
            .forEach { contextParameter ->
                val renderedAnnotations = contextParameter.annotations
                    .map { annotation -> renderAnnotation(annotation, contextParameter) }
                    .toList()
                results.add("${contextParameter.fqn} annotations=$renderedAnnotations")
            }
        return emptyList()
    }

    private fun renderAnnotation(annotation: KSAnnotation, expectedParent: KSContextParameter): String {
        val fqn = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
            ?: annotation.shortName.asString()
        val args = annotation.arguments.joinToString(", ") { "${it.name?.asString()}=${it.value}" }
        val parentMatches = annotation.parent === expectedParent
        return "$fqn($args)[parentSelf=$parentMatches]"
    }

    private fun contextParametersIn(declaration: KSDeclaration): List<KSContextParameter> =
        when (declaration) {
            is KSFunctionDeclaration -> declaration.contextParameters
            is KSPropertyDeclaration -> declaration.contextParameters
            is KSClassDeclaration -> declaration.declarations.flatMap(::contextParametersIn).toList()
            else -> emptyList()
        }

    private val KSNode.fqn: String
        get() = generateSequence(this) { it.parent }
            .takeWhile { it !is KSFile }
            .toList()
            .asReversed()
            .joinToString(separator = ".")
}
