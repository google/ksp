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