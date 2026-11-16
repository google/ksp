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

@file:Suppress("DEPRECATION")

package com.google.devtools.ksp.processor

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSContextParameter
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.visitor.KSEmptyVisitor
import java.io.File

class ContextParameterOriginAndVisitorProcessor(
    val packageNames: List<String>,
    override val apiFeatures: ApiFeatures,
) : AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results.sorted()

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val nextVisitor = object : KSEmptyVisitor<String, String>(apiFeatures) {
            override fun defaultHandler(node: KSNode, data: String): String =
                "defaultHandler($node, $data)"

            override fun visitContextParameter(contextParameter: KSContextParameter, data: String): String =
                "visitContextParameter($contextParameter, $data)"
        }
        val legacyVisitor = LegacyKSVisitor(nextVisitor)

        val sourceDeclarations = resolver.getAllFiles().flatMap { file -> file.declarations }
        val libraryDeclarations = packageNames.flatMap(resolver::getDeclarationsFromPackage)
        (sourceDeclarations + libraryDeclarations)
            .flatMap(::contextParametersIn)
            .forEach { contextParameter ->
                val locationStr = renderLocation(contextParameter.location)
                val nextResult = contextParameter.accept(nextVisitor, "arg")
                val legacyResult = contextParameter.accept(legacyVisitor, "arg")
                results.add(
                    "${contextParameter.fqn} " +
                        "origin=${contextParameter.origin} " +
                        "location=$locationStr " +
                        "next=$nextResult " +
                        "legacy=$legacyResult"
                )
            }
        return emptyList()
    }

    private fun renderLocation(location: Location): String = when (location) {
        is FileLocation -> "FileLocation(${File(location.filePath).name}:${location.lineNumber})"
        NonExistLocation -> "NonExistLocation"
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

    @Suppress("DEPRECATION")
    private class LegacyKSVisitor(
        delegate: KSVisitor<String, String>,
    ) : KSVisitor<String, String> by delegate {
        override fun visitAnnotated(annotated: KSAnnotated, data: String): String =
            "visitAnnotated($annotated, $data)"
    }
}
