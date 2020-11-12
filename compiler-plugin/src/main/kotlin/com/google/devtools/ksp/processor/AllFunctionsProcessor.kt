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

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

class AllFunctionsProcessor : AbstractTestProcessor() {
    val visitor = AllFunctionsVisitor()

    override fun toResult(): List<String> {
        return visitor.toResult()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().map { it.accept(visitor, Unit) }
        return emptyList()
    }

    class AllFunctionsVisitor : KSVisitorVoid() {
        private val declarationsByClass = mutableMapOf<String, MutableList<String>>()
        fun toResult() : List<String> {
            return declarationsByClass.entries
                .sortedBy {
                    it.key
                }.flatMap {
                    listOf(it.key) + it.value
                }
        }
        fun KSFunctionDeclaration.toSignature(): String {
            return this.simpleName.asString() +
                    "(${this.parameters.map { 
                        buildString {
                            append(it.type.resolve().declaration.qualifiedName?.asString())
                            if (it.hasDefault) {
                                append("(hasDefault)")
                            }
                        }
                    }.joinToString(",")})" +
                    ": ${this.returnType?.resolve()?.declaration?.qualifiedName?.asString() ?: ""}"
        }

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val declarations = mutableListOf<String>()
            // first add properties
            declarations.addAll(
                classDeclaration.getAllProperties().map {
                    it.toString()
                }.sorted()
            )
            // then add functions
            declarations.addAll(
                classDeclaration.getAllFunctions().map {
                    it.toSignature()
                }.sorted()
            )
            declarationsByClass["class: ${classDeclaration.simpleName.asString()}"] = declarations
        }

        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.map { it.accept(this, Unit) }
        }
    }
}