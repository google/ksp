/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.processor

import com.google.devtools.kotlin.symbol.processing.processing.Resolver
import com.google.devtools.kotlin.symbol.processing.symbol.KSClassDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSFile
import com.google.devtools.kotlin.symbol.processing.symbol.KSFunctionDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSVisitorVoid

class AllFunctionsProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val visitor = AllFunctionsVisitor()

    override fun toResult(): List<String> {
        val finalResult = mutableListOf(results[0])
        finalResult.addAll(results.subList(1, results.size).sorted())
        return finalResult
    }

    override fun process(resolver: Resolver) {
        resolver.getAllFiles().map { it.accept(visitor, Unit) }
    }

    inner class AllFunctionsVisitor : KSVisitorVoid() {
        fun KSFunctionDeclaration.toSignature(): String {
            return "${this.simpleName.asString()}" +
                    "(${this.parameters.map { 
                        buildString {
                            append(it.type?.resolve()?.declaration?.qualifiedName?.asString())
                            if (it.hasDefault) {
                                append("(hasDefault)")
                            }
                        }
                    }.joinToString(",")})" +
                    ": ${this.returnType?.resolve()?.declaration?.qualifiedName?.asString() ?: ""}"
        }

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            results.add("class: ${classDeclaration.simpleName.asString()}")
            classDeclaration.getAllFunctions().map { it.accept(this, Unit) }
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            results.add(function.toSignature())
        }

        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.map { it.accept(this, Unit) }
        }
    }
}