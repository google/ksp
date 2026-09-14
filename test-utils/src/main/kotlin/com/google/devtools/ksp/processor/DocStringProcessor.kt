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

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSBackingField
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class DocStringProcessor(override val enableNewFeatures: Boolean) : AbstractTestProcessor() {
    private val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    private class DeclarationCollector(enableNewFeatures: Boolean) :
        KSTopDownVisitor<MutableCollection<String>, Unit>(enableNewFeatures) {
        override fun defaultHandler(node: KSNode, data: MutableCollection<String>) = Unit

        override fun visitDeclaration(declaration: KSDeclaration, data: MutableCollection<String>) {
            data.add("${declaration.simpleName.asString()}: ${declaration.renderDocString()}")
        }

        override fun visitBackingField(backingField: KSBackingField, data: MutableCollection<String>) {
            // Override this method to also render parent property's name
            data.add(
                "${backingField.property.simpleName.asString()}.${backingField.simpleName.asString()}: " +
                    "${backingField.renderDocString()}"
            )
        }

        private fun KSDeclaration.renderDocString(): String? = this.docString?.lines()?.joinToString("\\n")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val visitor = DeclarationCollector(enableNewFeatures)
        resolver.getNewFiles().forEach { it.accept(visitor, result) }
        result.sort()
        return emptyList()
    }
}
