/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.getDeclaredFunctions
import org.jetbrains.kotlin.ksp.isAbstract
import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSClassDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSPropertyDeclaration

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
        val interfaceProperty = ktInterface.declarations.single { it.simpleName.asString() == "interfaceProperty" } as KSPropertyDeclaration
        result.add("${interfaceProperty.simpleName.asString()}: ${interfaceProperty.isAbstract()}")
        val nonAbstractInterfaceProp = ktInterface.declarations.single { it.simpleName.asString() == "nonAbstractInterfaceProp" } as KSPropertyDeclaration
        result.add("${nonAbstractInterfaceProp.simpleName.asString()}: ${nonAbstractInterfaceProp.isAbstract()}")
        val abstractClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("B")) as KSClassDeclaration
        result.add("${abstractClass.simpleName.asString()}: ${abstractClass.isAbstract()}")
        val parameterVal = abstractClass.declarations.single { it.simpleName.asString() == "parameterVal" } as KSPropertyDeclaration
        result.add("${parameterVal.simpleName.asString()}: ${parameterVal.isAbstract()}")
        val abstractProperty = abstractClass.declarations.single { it.simpleName.asString() == "abstractProperty" } as KSPropertyDeclaration
        result.add("${abstractProperty.simpleName.asString()}: ${abstractProperty.isAbstract()}")
        val aProperty = abstractClass.declarations.single { it.simpleName.asString() == "a" } as KSPropertyDeclaration
        result.add("${aProperty.simpleName.asString()}: ${aProperty.isAbstract()}")
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