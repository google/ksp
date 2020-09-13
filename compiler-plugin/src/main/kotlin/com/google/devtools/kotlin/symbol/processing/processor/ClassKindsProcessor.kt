/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.processor

import com.google.devtools.kotlin.symbol.processing.getClassDeclarationByName
import com.google.devtools.kotlin.symbol.processing.processing.Resolver
import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.visitor.KSTopDownVisitor

open class ClassKindsProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver) {
        fun KSClassDeclaration.pretty(): String = "${qualifiedName!!.asString()}: $classKind"
        val files = resolver.getAllFiles()
        files.forEach {
            it.accept(object : KSTopDownVisitor<Unit, Unit>() {
                override fun defaultHandler(node: KSNode, data: Unit) = Unit

                override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
                    results.add(classDeclaration.pretty())
                    super.visitClassDeclaration(classDeclaration, data)
                }
            }, Unit)
        }

        results.add(resolver.getClassDeclarationByName("kotlin.Any")!!.pretty())
        results.add(resolver.getClassDeclarationByName("kotlin.Annotation")!!.pretty())
        results.add(resolver.getClassDeclarationByName("kotlin.Deprecated")!!.pretty())
        results.add(resolver.getClassDeclarationByName("kotlin.Double.Companion")!!.pretty())
        results.add(resolver.getClassDeclarationByName("kotlin.DeprecationLevel")!!.pretty())
        results.add(resolver.getClassDeclarationByName("kotlin.DeprecationLevel.WARNING")!!.pretty())

        results.sort()
    }

    override fun toResult(): List<String> {
        return results
    }

}
