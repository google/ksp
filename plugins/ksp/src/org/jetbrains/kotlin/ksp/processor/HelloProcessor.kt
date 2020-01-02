/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSFunctionDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSPropertyDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSVisitorVoid

class HelloProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val visitor = HelloVisitor()

    override fun process(resolver: Resolver) {
        val symbols = resolver.getSymbolsWithAnnotation("test.Anno")
        results.add(symbols.size.toString())
        symbols.map { it.accept(visitor, Unit) }
    }

    override fun toResult(): List<String> {
        return results
    }

    inner class HelloVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(type: KSClassDeclaration, data: Unit) {
            results.add(type.qualifiedName?.asString() ?: "<error>")
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            results.add(function.qualifiedName?.asString() ?: "<error>")
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            results.add(property.qualifiedName?.asString() ?: "<error>")
        }
    }
}

