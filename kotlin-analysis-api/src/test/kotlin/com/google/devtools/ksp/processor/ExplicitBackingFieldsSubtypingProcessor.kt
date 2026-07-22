/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode

class ExplicitBackingFieldsSubtypingProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results.toList()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        listOf("MyClass", "lib.Other").forEach { name ->
            resolver.getClassDeclarationByName(name)!!.let { clazz ->
                clazz.getAllProperties().forEach { property ->
                    val pType = property.type.resolve()
                    property.backingField?.let { field ->
                        val fType = field.type.resolve()
                        results.add("${property.fqn}: $pType")
                        results.add("${field.fqn}: $fType")
                    }
                }
            }
        }
        return emptyList()
    }

    private val KSAnnotated.fqn: String
        get() = findAllQualifiers(this).joinToString(separator = ".")

    private fun findAllQualifiers(node: KSNode): List<KSNode> {
        val result = mutableListOf(node)
        var current = node.parent
        while (current != null && current !is KSFile) {
            result.add(current)
            current = current.parent
        }
        result.reverse()
        return result
    }
}
