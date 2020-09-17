/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.processor

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class VisibilityProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val symbolA = resolver.getSymbolsWithAnnotation("TestA").single() as KSClassDeclaration
        val symbolB = resolver.getSymbolsWithAnnotation("TestB").single() as KSClassDeclaration
        val symbolD = resolver.getSymbolsWithAnnotation("TestD").single() as KSClassDeclaration
        val allFunctions = (symbolA.superTypes.single().resolve()!!.declaration as KSClassDeclaration)
            .declarations.filterIsInstance<KSFunctionDeclaration>()
        allFunctions.map {
            "${it.simpleName.asString()}: ${it.getVisibility()},visible in A, B, D: " +
                    "${it.isVisibleFrom(symbolA)}, ${it.isVisibleFrom(symbolB)}, ${it.isVisibleFrom(symbolD)}"
        }.map { results.add(it) }
    }
}