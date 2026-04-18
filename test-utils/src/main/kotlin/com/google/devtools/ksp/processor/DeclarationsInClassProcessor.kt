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

import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class DeclarationsInClassProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> {
        return result.sorted()
    }

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
                    val parentName = when (val parentDeclaration = declaration.parentDeclaration) {
                        is KSClassDeclaration -> parentDeclaration.simpleName.asString()
                        is KSPropertyDeclaration -> parentDeclaration.parentDeclaration?.simpleName?.asString()
                        else -> error("Unexpected declaration: ${parentDeclaration?.javaClass}")
                    }
                    if (declaration.isConstructor()) {
                        result.add("CONSTRUCTOR: $parentName.$name ($origin)")
                    } else {
                        result.add("FUNCTION: $parentName.$name ($origin)")
                    }
                }
                is KSPropertyDeclaration -> {
                    val name = declaration.simpleName.asString()
                    val parentName = declaration.parentDeclaration?.simpleName?.asString()
                    result.add("PROPERTY: $parentName.$name ($origin)")
                    declaration.getter?.let {
                        result.add("GETTER: $parentName.$name (${it.origin})")
                    }
                    declaration.setter?.let {
                        result.add("SETTER: $parentName.$name (${it.origin})")
                    }
                }
            }
        }
        processDeclaration(resolver.getClassDeclarationByName("lib.CustomList")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.CustomMap")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.InterfaceWithObjectMethodOverrides")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.InterfaceWithNonObjectMethodOverrides")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.AbstractClassWithObjectMethodOverrides")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.ConcreteClassWithObjectMethodOverrides")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.BaseWithProperties")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.OverridesBaseWithProperties")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.BaseWithIsProperties")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.OverridesBaseWithIsProperties")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.BaseWithCapitalizedProperties")!!)
        // TODO(https://github.com/google/ksp/issues/2925): Enable this once we've sorted out the differences between
        //  the AA and PSI implementations. Currently, the main issue is that for the case where a property is
        //  capitalized and a Java method overrides the getter, the AA implementation includes both the original Java
        //  method and the synthetic accessor method; whereas the PSI implementation only includes the original method.
        // processDeclaration(resolver.getClassDeclarationByName("lib.OverridesBaseWithCapitalizedProperties")!!)
        processDeclaration(resolver.getClassDeclarationByName("lib.StaticVsMemberDeclarations")!!)
        return emptyList()
    }
}
