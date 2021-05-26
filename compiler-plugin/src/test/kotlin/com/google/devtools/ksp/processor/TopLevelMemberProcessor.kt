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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.visitor.KSTopDownVisitor

@OptIn(KspExperimental::class)
open class TopLevelMemberProcessor : AbstractTestProcessor() {
    lateinit var results: List<String>

    override fun process(resolver: Resolver): List<KSAnnotated> {
        results = listOf("lib", "main").flatMap { pkg ->
            resolver.getDeclarationsFromPackage(pkg)
                .flatMap { declaration ->
                    val declarations = mutableListOf<KSDeclaration>()
                    declaration.accept(AllMembersVisitor(), declarations)
                    declarations
                }.map {
                    "$pkg : ${it.simpleName.asString()} -> ${resolver.getSyntheticJvmClass(it)}"
                }.sorted()
        }
        return emptyList()
    }

    private fun Resolver.getSyntheticJvmClass(
        declaration: KSDeclaration
    ) = when (declaration) {
        is KSPropertyDeclaration -> this.getOwnerJvmClassName(declaration)
        is KSFunctionDeclaration -> this.getOwnerJvmClassName(declaration)
        else -> error("unexpected declaration $declaration")
    }

    private class AllMembersVisitor : KSTopDownVisitor<MutableList<KSDeclaration>, Unit>() {
        override fun defaultHandler(node: KSNode, data: MutableList<KSDeclaration>) {
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: MutableList<KSDeclaration>) {
            data.add(property)
            super.visitPropertyDeclaration(property, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: MutableList<KSDeclaration>) {
            data.add(function)
            super.visitFunctionDeclaration(function, data)
        }
    }

    override fun toResult(): List<String> {
        return results
    }
}
