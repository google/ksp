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

open class TypeParameterReferenceProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val collector = ReferenceCollector()
    val references = mutableSetOf<KSTypeReference>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val files = resolver.getNewFiles()

        files.forEach {
            it.accept(collector, references)
        }

        val sortedReferences = references.filter { it.element is KSClassifierReference && it.origin == Origin.KOTLIN }
            .sortedBy { (it.element as KSClassifierReference).referencedName() }

        for (i in sortedReferences) {
            val r = i.resolve()
            results.add("${r.declaration.qualifiedName?.asString()}: ${r.isMarkedNullable}")
        }
        val libFoo = resolver.getClassDeclarationByName("LibFoo")!!
        libFoo.declarations.filterIsInstance<KSPropertyDeclaration>().forEach { results.add(it.type.toString()) }
        val javaLib = resolver.getClassDeclarationByName("JavaLib")!!
        javaLib.declarations.filterIsInstance<KSFunctionDeclaration>().forEach { results.add(it.returnType.toString()) }
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
