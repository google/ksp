/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.getDeclaredFunctions
import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSFunctionDeclaration

class CheckOverrideProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val javaList = resolver.getClassDeclarationByName(resolver.getKSNameFromString("JavaList")) as KSClassDeclaration
        val kotlinList = resolver.getClassDeclarationByName(resolver.getKSNameFromString("KotlinList")) as KSClassDeclaration
        val getFunKt = resolver.getSymbolsWithAnnotation("GetAnno").single() as KSFunctionDeclaration
        val getFunJava = javaList.getAllFunctions().single { it.simpleName.asString() == "get" }
        val fooFunJava = javaList.getDeclaredFunctions().single { it.simpleName.asString() == "foo" }
        val fooFunKt = resolver.getSymbolsWithAnnotation("FooAnno").single() as KSFunctionDeclaration
        val foooFunKt = resolver.getSymbolsWithAnnotation("BarAnno").single() as KSFunctionDeclaration
        val equalFunKt = kotlinList.getDeclaredFunctions().single { it.simpleName.asString() == "equals" }
        val equalFunJava = javaList.getAllFunctions().single { it.simpleName.asString() == "equals" }
        results.add("${getFunKt.qualifiedName?.asString()} overrides ${getFunJava.qualifiedName?.asString()}: ${getFunKt.overrides(getFunJava)}")
        results.add("${fooFunKt.qualifiedName?.asString()} overrides ${fooFunJava.qualifiedName?.asString()}: ${fooFunKt.overrides(fooFunJava)}")
        results.add("${foooFunKt.qualifiedName?.asString()} overrides ${fooFunJava.qualifiedName?.asString()}: ${foooFunKt.overrides(fooFunJava)}")
        results.add("${equalFunKt.qualifiedName?.asString()} overrides ${equalFunJava.qualifiedName?.asString()}: ${equalFunKt.overrides(equalFunJava)}")
    }
}