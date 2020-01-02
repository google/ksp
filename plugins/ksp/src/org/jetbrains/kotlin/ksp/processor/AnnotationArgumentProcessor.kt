/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSValueArgument
import org.jetbrains.kotlin.ksp.symbol.KSVisitorVoid

class AnnotationArgumentProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val visitor = ArgumentVisitor()

    override fun process(resolver: Resolver) {
        val symbol = resolver.getSymbolsWithAnnotation("Bar").single()
        val annotation = (symbol as KSClassDeclaration).annotations.single()
        annotation.arguments.map { it.accept(visitor, Unit) }
    }

    override fun toResult(): List<String> {
        return results
    }

    inner class ArgumentVisitor : KSVisitorVoid() {
        override fun visitValueArgument(valueArgument: KSValueArgument, data: Unit) {
            results.add(valueArgument.value.toString())
        }
    }
}