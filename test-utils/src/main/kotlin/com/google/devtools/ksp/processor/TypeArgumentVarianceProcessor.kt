/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

class TypeArgumentVarianceProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()
    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val myClass = resolver.getClassDeclarationByName("MyClass")!!
        val fieldDeclaration = myClass.getDeclaredProperties().single { it.simpleName.asString() == "field" }
        result.add(fieldDeclaration.type.toString())
        result.add(fieldDeclaration.type.element?.typeArguments?.single().toString())
        result.add(fieldDeclaration.type.resolve().toString())
        result.add(fieldDeclaration.type.resolve().arguments.single().toString())
        return emptyList()
    }
}
