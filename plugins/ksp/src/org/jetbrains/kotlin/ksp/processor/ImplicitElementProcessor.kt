/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.KSDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSPropertyDeclaration

class ImplicitElementProcessor : AbstractTestProcessor() {
    val result: MutableList<String> = mutableListOf()

    override fun toResult(): List<String> {
        return result
    }

    private fun nameAndOrigin(declaration: KSDeclaration) = "${declaration.simpleName.asString()}: ${declaration.origin}"

    override fun process(resolver: Resolver) {
        val ClsClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("Cls"))!!
        result.add("${ClsClass.primaryConstructor?.simpleName?.asString() ?: "<null>"}; origin: ${ClsClass.primaryConstructor?.origin}")
        val ITF = resolver.getClassDeclarationByName(resolver.getKSNameFromString("ITF"))!!
        result.add(ITF.primaryConstructor?.simpleName?.asString() ?: "<null>")
        result.add(resolver.getClassDeclarationByName(resolver.getKSNameFromString("JavaClass"))!!.primaryConstructor?.simpleName?.asString() ?: "<null>")
        val readOnly = ClsClass.declarations.single { it.simpleName.asString() == "readOnly" } as KSPropertyDeclaration
        readOnly.getter?.let { result.add("readOnly.get(): ${it.origin}") }
        readOnly.getter?.receiver?.let { result.add("readOnly.getter.owner: " + nameAndOrigin(it)) }
        readOnly.setter?.let { result.add("readOnly.set(): ${it.origin}") }
        readOnly.setter?.receiver?.let { result.add("readOnly.setter.owner: " + nameAndOrigin(it)) }
        val readWrite = ClsClass.declarations.single { it.simpleName.asString() == "readWrite" } as KSPropertyDeclaration
        readWrite.getter?.let { result.add("readWrite.get(): ${it.origin}") }
        readWrite.setter?.let { result.add("readWrite.set(): ${it.origin}") }
        val dataClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("Data"))!!
        val comp1 = dataClass.declarations.single { it.simpleName.asString() == "comp1" } as KSPropertyDeclaration
        comp1.getter?.let { result.add("comp1.get(): ${it.origin}") }
        comp1.setter?.let { result.add("comp1.set(): ${it.origin}") }
        val comp2 = dataClass.declarations.single { it.simpleName.asString() == "comp2" } as KSPropertyDeclaration
        comp2.getter?.let { result.add("comp2.get(): ${it.origin}") }
        comp2.setter?.let { result.add("comp2.set(): ${it.origin}") }
    }
}