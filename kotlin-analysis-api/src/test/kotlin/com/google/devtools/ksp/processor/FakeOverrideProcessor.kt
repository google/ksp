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
import com.google.devtools.ksp.symbol.KSBackingField
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyAccessor
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSPropertyGetter
import com.google.devtools.ksp.symbol.KSPropertySetter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.intellij.util.containers.addIfNotNull

class FakeOverrideProcessor(val declarationNames: List<String>, override val enableNewFeatures: Boolean) :
    AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun toResult(): List<String> {
        return results.toList()
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        declarationNames.forEach { declarationName ->
            resolver
                .getClassDeclarationByName(declarationName)!!
                .also { declaration ->
                    results.add(
                        formatDeclaration(declaration)
                    )
                }
                .declarations
                .forEach { declaration ->
                    when (declaration) {
                        is KSPropertyDeclaration -> {
                            results.add(
                                formatDeclaration(declaration, declaration.type)
                            )
                            results.addIfNotNull(
                                declaration.getter?.let(::formatGetter)
                            )
                            results.addIfNotNull(
                                declaration.setter?.let(::formatAccessor)
                            )
                            results.addIfNotNull(
                                declaration.backingField?.let(::formatBackingField)
                            )
                        }

                        else -> results.add(formatDeclaration(declaration))
                    }
                }
        }
        return emptyList()
    }

    private fun formatDeclaration(decl: KSDeclaration): String =
        decl.qualifiedName?.asString() ?: decl.simpleName.asString()

    private fun formatDeclaration(decl: KSDeclaration, type: KSTypeReference): String =
        "${formatDeclaration(decl)} : ${formatType(type)}"

    private fun formatType(type: KSTypeReference): String =
        formatDeclaration(type.resolve().declaration)

    private fun formatAccessor(accessor: KSPropertyAccessor): String =
        accessor.receiver.parentDeclaration?.let { decl ->
            "${formatDeclaration(decl)}.$accessor"
        } ?: accessor.toString()

    private fun formatAccessor(accessor: KSPropertyAccessor, type: KSTypeReference): String =
        "${formatAccessor(accessor)} : ${formatType(type)}"

    private fun formatGetter(getter: KSPropertyGetter): String =
        getter.returnType?.let { returnType ->
            formatAccessor(getter, returnType)
        } ?: formatAccessor(getter)

    private fun formatBackingField(backingField: KSBackingField): String =
        formatDeclaration(backingField, backingField.type)
}
