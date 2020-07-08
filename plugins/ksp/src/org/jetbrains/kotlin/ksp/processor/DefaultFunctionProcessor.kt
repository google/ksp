/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.getDeclaredFunctions
import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration

class DefaultFunctionProcessor : AbstractTestProcessor() {

    private val result = mutableListOf<String>()

    override fun process(resolver: Resolver) {
        val ktInterface = resolver.getClassDeclarationByName(resolver.getKSNameFromString("KTInterface")) as KSClassDeclaration
        val javaInterface = resolver.getClassDeclarationByName(resolver.getKSNameFromString("C")) as KSClassDeclaration
        result.addAll(checkFunctions(ktInterface, listOf("funLiteral", "funWithBody", "emptyFun")))
        result.addAll(checkFunctions(javaInterface, listOf("foo", "bar")))
        val containsFun = ktInterface.getAllFunctions().single { it.simpleName.asString() == "iterator" }
        result.add("${containsFun.simpleName.asString()}: ${containsFun.isAbstract}")
        val equalsFun = ktInterface.getAllFunctions().single { it.simpleName.asString() == "equals" }
        result.add("${equalsFun.simpleName.asString()}: ${equalsFun.isAbstract}")
    }

    private fun checkFunctions(classDec: KSClassDeclaration, funList: List<String>): List<String> {
        return classDec.getDeclaredFunctions()
            .filter { funList.contains(it.simpleName.asString()) }
            .map { "${it.simpleName.asString()}: ${it.isAbstract}" }
    }

    override fun toResult(): List<String> {
        return result
    }
}