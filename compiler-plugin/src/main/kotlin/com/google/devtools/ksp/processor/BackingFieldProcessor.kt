/*
 * Copyright 2021 Google LLC
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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.visitor.KSTopDownVisitor

@Suppress("unused") // used in tests
@OptIn(KspExperimental::class)
open class BackingFieldProcessor : AbstractTestProcessor() {
    lateinit var results: List<String>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        results = listOf("lib", "main").flatMap { pkg ->
            resolver.getDeclarationsFromPackage(pkg)
                .flatMap { declaration ->
                    val properties = mutableListOf<KSPropertyDeclaration>()
                    declaration.accept(AllMembersVisitor(), properties)
                    properties
                }.map {
                    "${it.qualifiedName?.asString()}: ${it.hasBackingField}"
                }.sorted()
        }
        return emptyList()
    }

    private class AllMembersVisitor : KSTopDownVisitor<MutableList<KSPropertyDeclaration>, Unit>() {
        override fun defaultHandler(node: KSNode, data: MutableList<KSPropertyDeclaration>) {
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: MutableList<KSPropertyDeclaration>) {
            data.add(property)
            super.visitPropertyDeclaration(property, data)
        }
    }

    override fun toResult(): List<String> {
        return results
    }
}
