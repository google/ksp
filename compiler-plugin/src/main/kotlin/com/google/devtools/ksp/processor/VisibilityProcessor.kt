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

import com.google.devtools.ksp.getVisibility
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

class VisibilityProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbolA = resolver.getSymbolsWithAnnotation("TestA").single() as KSClassDeclaration
        val symbolB = resolver.getSymbolsWithAnnotation("TestB").single() as KSClassDeclaration
        val symbolD = resolver.getSymbolsWithAnnotation("TestD").single() as KSClassDeclaration
        val allFunctions = (symbolA.superTypes.single().resolve()!!.declaration as KSClassDeclaration)
            .declarations.filterIsInstance<KSFunctionDeclaration>()
        allFunctions.map {
            "${it.simpleName.asString()}: ${it.getVisibility()},visible in A, B, D: " +
                    "${it.isVisibleFrom(symbolA)}, ${it.isVisibleFrom(symbolB)}, ${it.isVisibleFrom(symbolD)}"
        }.map { results.add(it) }
        return emptyList()
    }
}