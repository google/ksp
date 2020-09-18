/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

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
