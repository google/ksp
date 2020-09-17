/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class JavaModifierProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver) {
        val symbol = resolver.getSymbolsWithAnnotation("Test").single() as KSClassDeclaration
        symbol.superTypes.single().resolve()!!.declaration.accept(ModifierVisitor(), Unit)
    }

    inner class ModifierVisitor : KSTopDownVisitor<Unit, Unit>() {
        override fun defaultHandler(node: KSNode, data: Unit) {
        }

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            this.visitModifierListOwner(classDeclaration, data)
            classDeclaration.declarations.map { it.accept(this, data) }
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            this.visitModifierListOwner(property, data)
        }

        override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
            this.visitModifierListOwner(function, data)
        }

        override fun visitModifierListOwner(modifierListOwner: KSModifierListOwner, data: Unit) {
            results.add((modifierListOwner as KSDeclaration).simpleName.asString() + ": " +
                                modifierListOwner.modifiers.map { it.toString() }.joinToString(" "))
        }

    }
}