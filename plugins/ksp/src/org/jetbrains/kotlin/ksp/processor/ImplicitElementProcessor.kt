/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver

class ImplicitElementProcessor : AbstractTestProcessor() {
    val result: MutableList<String> = mutableListOf()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver) {
        val ClsClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("Cls"))!!
        result.add("${ClsClass.primaryConstructor?.simpleName?.asString() ?: "<null>"}; origin: ${ClsClass.primaryConstructor?.origin}")
        val ITF = resolver.getClassDeclarationByName(resolver.getKSNameFromString("ITF"))!!
        result.add(ITF.primaryConstructor?.simpleName?.asString() ?: "<null>")
        result.add(resolver.getClassDeclarationByName(resolver.getKSNameFromString("JavaClass"))!!.primaryConstructor?.simpleName?.asString() ?: "<null>")
    }
}