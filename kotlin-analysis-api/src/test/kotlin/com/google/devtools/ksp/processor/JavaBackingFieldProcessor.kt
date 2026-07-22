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
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated

class JavaBackingFieldProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classNames = listOf(
            "lib.Fields",
            "lib.AccessorsOnly",
            "lib.ObscureFields",
            "SourceFields",
            "SourceAccessorsOnly",
            "SourceObscureFields",
        )

        classNames.forEach { className ->
            val ksClass = resolver.getClassDeclarationByName(className)!!
            results.add(className)
            ksClass.getDeclaredProperties().forEach { prop ->
                prop.type.resolve().declaration.qualifiedName!!.asString().let { tpe ->
                    results.add("- ${prop.qualifiedName!!.asString()}: $tpe - ${prop.modifiers.toList()}")
                }
                prop.backingField?.let { field ->
                    val renderedType = field.type.resolve().declaration.qualifiedName!!.asString()
                    results.add("- ${field.qualifiedName!!.asString()}: $renderedType - ${field.modifiers.toList()}")
                }
            }
        }
        return emptyList()
    }
}
