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
import com.google.devtools.ksp.buildApiFeatures
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSContextParameter
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.validate
import com.google.devtools.ksp.visitor.KSValidateVisitor

class ContextParameterValidateProcessor(
    val packageNames: List<String>,
    override val apiFeatures: ApiFeatures,
) : AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sourceDeclarations = resolver.getAllFiles().flatMap { file -> file.declarations }.toList()
        val libraryDeclarations = packageNames.flatMap(resolver::getDeclarationsFromPackage)
        val topDeclarations = (sourceDeclarations + libraryDeclarations)
            .filter { it.simpleName.asString() !in setOf("GoodCtx", "LibCtx") }
            .sortedBy { it.fqn }

        topDeclarations.forEach { decl ->
            val valid = decl.validate(apiFeatures = apiFeatures)
            results.add("decl ${decl.fqn} valid=$valid")
        }

        val allContextParameters = topDeclarations
            .flatMap(::contextParametersIn)
            .sortedBy { it.fqn }

        allContextParameters.forEach { ctxParam ->
            val valid = ctxParam.validate(apiFeatures = apiFeatures)
            var visitedDataIsSelf = false
            ctxParam.accept(
                object : KSValidateVisitor({ _, _ -> true }, apiFeatures) {
                    override fun visitTypeReference(typeReference: KSTypeReference, data: KSNode?): Boolean {
                        if (typeReference === ctxParam.type) {
                            visitedDataIsSelf = data === ctxParam
                        }
                        return super.visitTypeReference(typeReference, data)
                    }
                },
                null
            )
            val threwWhenDisabled = try {
                ctxParam.validate(
                    apiFeatures = buildApiFeatures {
                        enableBackingFields = apiFeatures.enableBackingFields
                        enableContextParameters = false
                    }
                )
                false
            } catch (e: Exception) {
                e::class.simpleName == "InternalKSPException"
            }
            results.add(
                "param ${ctxParam.fqn} valid=$valid dataIsSelf=$visitedDataIsSelf threwWhenDisabled=$threwWhenDisabled"
            )
        }

        return emptyList()
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
