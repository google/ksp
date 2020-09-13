/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.processor

import com.google.devtools.kotlin.symbol.processing.getDeclaredFunctions
import com.google.devtools.kotlin.symbol.processing.isAbstract
import com.google.devtools.kotlin.symbol.processing.processing.Resolver
import com.google.devtools.kotlin.symbol.processing.symbol.KSClassDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSPropertyDeclaration

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
        result.add("${interfaceProperty.simpleName.asString()}: isAbstract: ${interfaceProperty.isAbstract()}: isMutable: ${interfaceProperty.isMutable}")
        val interfaceVar = ktInterface.declarations.single { it.simpleName.asString() == "interfaceVar" } as KSPropertyDeclaration
        result.add("${interfaceVar.simpleName.asString()}: isAbstract: ${interfaceVar.isAbstract()}: isMutable: ${interfaceVar.isMutable}")
        val nonAbstractInterfaceProp = ktInterface.declarations.single { it.simpleName.asString() == "nonAbstractInterfaceProp" } as KSPropertyDeclaration
        result.add("${nonAbstractInterfaceProp.simpleName.asString()}: isAbstract: ${nonAbstractInterfaceProp.isAbstract()}: isMutable: ${nonAbstractInterfaceProp.isMutable}")
        val abstractClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("B")) as KSClassDeclaration
        result.add("${abstractClass.simpleName.asString()}: ${abstractClass.isAbstract()}")
        val parameterVal = abstractClass.declarations.single { it.simpleName.asString() == "parameterVal" } as KSPropertyDeclaration
        result.add("${parameterVal.simpleName.asString()}: isAbstract: ${parameterVal.isAbstract()}: isMutable: ${parameterVal.isMutable}")
        val parameterVar = abstractClass.declarations.single { it.simpleName.asString() == "parameterVar" } as KSPropertyDeclaration
        result.add("${parameterVar.simpleName.asString()}: isAbstract: ${parameterVar.isAbstract()}: isMutable: ${parameterVar.isMutable}")
        val abstractVar = abstractClass.declarations.single { it.simpleName.asString() == "abstractVar" } as KSPropertyDeclaration
        result.add("${abstractVar.simpleName.asString()}: isAbstract: ${abstractVar.isAbstract()}: isMutable: ${abstractVar.isMutable}")
        val abstractProperty = abstractClass.declarations.single { it.simpleName.asString() == "abstractProperty" } as KSPropertyDeclaration
        result.add("${abstractProperty.simpleName.asString()}: isAbstract: ${abstractProperty.isAbstract()}: isMutable: ${abstractProperty.isMutable}")
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