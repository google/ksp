/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.processor

import com.google.devtools.kotlin.symbol.processing.getDeclaredFunctions
import com.google.devtools.kotlin.symbol.processing.processing.Resolver
import com.google.devtools.kotlin.symbol.processing.symbol.KSClassDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSFunctionDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSPropertyDeclaration

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
        val bazPropKt = resolver.getSymbolsWithAnnotation("BazAnno").single() as KSPropertyDeclaration
        val baz2PropKt = resolver.getSymbolsWithAnnotation("Baz2Anno").single() as KSPropertyDeclaration
        val bazzPropKt = resolver.getSymbolsWithAnnotation("BazzAnno").single() as KSPropertyDeclaration
        val bazz2PropKt = resolver.getSymbolsWithAnnotation("Bazz2Anno").single() as KSPropertyDeclaration
        results.add("${getFunKt.qualifiedName?.asString()} overrides ${getFunJava.qualifiedName?.asString()}: ${getFunKt.overrides(getFunJava)}")
        results.add("${fooFunKt.qualifiedName?.asString()} overrides ${fooFunJava.qualifiedName?.asString()}: ${fooFunKt.overrides(fooFunJava)}")
        results.add("${foooFunKt.qualifiedName?.asString()} overrides ${fooFunJava.qualifiedName?.asString()}: ${foooFunKt.overrides(fooFunJava)}")
        results.add("${equalFunKt.qualifiedName?.asString()} overrides ${equalFunJava.qualifiedName?.asString()}: ${equalFunKt.overrides(equalFunJava)}")
        results.add("${bazPropKt.qualifiedName?.asString()} overrides ${baz2PropKt.qualifiedName?.asString()}: ${bazPropKt.overrides(baz2PropKt)}")
        results.add("${bazPropKt.qualifiedName?.asString()} overrides ${bazz2PropKt.qualifiedName?.asString()}: ${bazPropKt.overrides(bazz2PropKt)}")
        results.add("${bazzPropKt.qualifiedName?.asString()} overrides ${bazz2PropKt.qualifiedName?.asString()}: ${bazzPropKt.overrides(bazz2PropKt)}")
        results.add("${bazzPropKt.qualifiedName?.asString()} overrides ${baz2PropKt.qualifiedName?.asString()}: ${bazzPropKt.overrides(baz2PropKt)}")
    }
}