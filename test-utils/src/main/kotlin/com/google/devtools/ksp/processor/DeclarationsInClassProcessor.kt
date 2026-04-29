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

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class DeclarationsInClassProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult() = result

    override fun process(resolver: Resolver): List<KSAnnotated> {
        fun processDeclaration(declaration: KSDeclaration) {
            val origin = declaration.origin
            when (declaration) {
                is KSClassDeclaration -> {
                    val name = declaration.qualifiedName?.asString()
                    result.add("CLASS: $name ($origin)")
                    declaration.declarations.forEach { processDeclaration(it) }
                }
                is KSFunctionDeclaration -> {
                    val name = declaration.simpleName.asString()
                    val kind = if (declaration.isConstructor()) {
                        "CONSTRUCTOR"
                    } else {
                        "FUNCTION"
                    }
                    result.add("$kind: $name ($origin)")
                }
                is KSPropertyDeclaration -> {
                    val name = declaration.simpleName.asString()
                    result.add("PROPERTY: $name ($origin)")
                    declaration.getter?.let {
                        result.add("PROPERTY GETTER: (${it.origin})")
                    }
                    declaration.setter?.let {
                        result.add("PROPERTY SETTER: (${it.origin})")
                    }
                }
            }
        }
        processDeclaration(resolver.getClassDeclarationByName("lib.CustomList")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.CustomMap")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.InterfaceWithObjectMethodOverrides")!!)
        return emptyList()
    }
}
