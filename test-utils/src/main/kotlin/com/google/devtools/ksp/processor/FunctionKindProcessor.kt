/*
 * Copyright 2025 Google LLC
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

open class FunctionKindProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        fun KSFunctionDeclaration.pretty(): String {
            val name = if (parentDeclaration != null) {
                val className = parentDeclaration?.simpleName?.asString() ?: ""
                if (className.isNotEmpty()) {
                    "$className.${simpleName.asString()}"
                } else {
                    simpleName.asString()
                }
            } else {
                qualifiedName?.asString() ?: simpleName.asString()
            }

            return "$name: $functionKind"
        }

        val files = resolver.getNewFiles()
        files.forEach {
            it.accept(
                object : KSTopDownVisitor<Unit, Unit>() {
                    override fun defaultHandler(node: KSNode, data: Unit) = Unit

                    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
                        if (!IGNORED_METHOD_NAMES.contains(function.simpleName.toString())) {
                            results.add(function.pretty())
                        }
                        super.visitFunctionDeclaration(function, data)
                    }
                },
                Unit
            )
        }

        results.sort()
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

    companion object {
        private val IGNORED_METHOD_NAMES = setOf("equals", "hashCode", "toString", "<init>")
    }
}
