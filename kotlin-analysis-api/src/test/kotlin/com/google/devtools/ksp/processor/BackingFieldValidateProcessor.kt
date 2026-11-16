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
import com.google.devtools.ksp.symbol.KSBackingField
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate

class BackingFieldValidateProcessor(
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
            .sortedBy { it.fqn }

        topDeclarations.forEach { decl ->
            val valid = decl.validate(apiFeatures = apiFeatures)
            results.add("decl ${decl.fqn} valid=$valid")
        }

        val invalidExplicitProp = topDeclarations
            .filterIsInstance<KSPropertyDeclaration>()
            .single { it.simpleName.asString() == "srcInvalidExplicitFieldType" }
        val skipPropertyScope = invalidExplicitProp.validate(
            predicate = { enclosing, _ -> enclosing !is KSPropertyDeclaration },
            apiFeatures = apiFeatures,
        )
        val skipBackingFieldChild = invalidExplicitProp.validate(
            predicate = { _, child -> child !is KSBackingField },
            apiFeatures = apiFeatures,
        )
        results.add(
            "predicate ${invalidExplicitProp.fqn} " +
                "skipPropertyScope=$skipPropertyScope " +
                "skipBackingFieldChild=$skipBackingFieldChild"
        )

        val allBackingFields = topDeclarations
            .flatMap(::backingFieldsIn)
            .sortedBy { it.fqn }

        allBackingFields.forEach { field ->
            val valid = field.validate(apiFeatures = apiFeatures)
            val threwWhenDisabled = try {
                field.validate(
                    apiFeatures = buildApiFeatures {
                        enableBackingFields = false
                        enableContextParameters = apiFeatures.enableContextParameters
                    }
                )
                false
            } catch (e: Exception) {
                e::class.simpleName == "InternalKSPException"
            }
            results.add("field ${field.fqn} valid=$valid threwWhenDisabled=$threwWhenDisabled")
        }

        return emptyList()
    }

    private fun backingFieldsIn(declaration: KSDeclaration): List<KSBackingField> =
        when (declaration) {
            is KSPropertyDeclaration -> listOfNotNull(declaration.backingField)
            is KSClassDeclaration -> declaration.declarations.flatMap(::backingFieldsIn).toList()
            else -> emptyList()
        }

    private val KSNode.fqn: String
        get() = generateSequence(this) { it.parent }
            .takeWhile { it !is KSFile }
            .toList()
            .asReversed()
            .joinToString(separator = ".") { node ->
                when (node) {
                    is KSDeclaration -> node.simpleName.asString()
                    else -> node.toString()
                }
            }
}
