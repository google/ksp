/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.KSPropertyDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSType

class ErrorTypeProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver) {
        val classC = resolver.getClassDeclarationByName(resolver.getKSNameFromString("C"))!!
        val errorAtTop = classC.declarations.single { it.simpleName.asString() == "errorAtTop" } as KSPropertyDeclaration
        val errorInComponent = classC.declarations.single { it.simpleName.asString() == "errorInComponent" } as KSPropertyDeclaration
        result.add(errorAtTop.type?.resolve()?.print() ?: "")
        result.add(errorInComponent.type?.resolve()?.print() ?: "")
        errorInComponent.type!!.resolve()!!.arguments.map { result.add(it.type!!.resolve()!!.print()) }
        result.add(
            "errorInComponent is assignable from errorAtTop: ${
                errorAtTop.type!!.resolve()!!.isAssignableFrom(errorAtTop.type!!.resolve()!!)
            }"
        )
        result.add(
            "errorInComponent is assignable from class C: ${
                errorAtTop.type!!.resolve()!!.isAssignableFrom(classC.asStarProjectedType())
            }"
        )
        result.add(
            "Any is assignable from errorInComponent: ${
                ResolverImpl.instance.builtIns.anyType.isAssignableFrom(errorAtTop.type!!.resolve()!!)
            }"
        )
        result.add(
            "class C is assignable from errorInComponent: ${
                classC.asStarProjectedType().isAssignableFrom(errorAtTop.type!!.resolve()!!)
            }"
        )
        result.add(
            "Any is assignable from class C: ${
                ResolverImpl.instance.builtIns.anyType.isAssignableFrom(classC.asStarProjectedType())
            }"
        )
    }

    private fun KSType.print(): String {
        return if (this.isError) {
            if (this.declaration.qualifiedName == null) "ERROR TYPE" else throw IllegalStateException("Error type should resolve to KSErrorTypeClassDeclaration")
        } else this.declaration.qualifiedName!!.asString()
    }
}