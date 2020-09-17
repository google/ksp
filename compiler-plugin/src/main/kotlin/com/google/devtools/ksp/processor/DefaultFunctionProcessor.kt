/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

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