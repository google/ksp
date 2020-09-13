/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.processor

import com.google.devtools.kotlin.symbol.processing.processing.Resolver
import com.google.devtools.kotlin.symbol.processing.symbol.KSClassDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSFile
import com.google.devtools.kotlin.symbol.processing.symbol.KSVisitorVoid

class ClassWithCompanionProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val visitor = CompanionVisitor()

    override fun process(resolver: Resolver) {
        resolver.getAllFiles().map { it.accept(CompanionVisitor(), Unit) }
    }

    override fun toResult(): List<String> {
        return results
    }

    inner class CompanionVisitor : KSVisitorVoid() {
        override fun visitFile(file: KSFile, data: Unit) {
            file.declarations.map { it.accept(this, Unit) }
        }

        override fun visitClassDeclaration(type: KSClassDeclaration, data: Unit) {
            results.add("${type.simpleName.asString()}:${type.isCompanionObject}")
            type.declarations.map { it.accept(this, Unit) }
        }
    }
}