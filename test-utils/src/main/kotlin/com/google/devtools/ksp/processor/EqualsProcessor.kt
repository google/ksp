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
import com.google.devtools.ksp.symbol.*

open class EqualsProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val declaration1 = resolver.getSymbolsWithAnnotation("MyAnnotation")
            .single { (it as? KSClassDeclaration)?.simpleName?.asString() == "MyClass" }
        val declaration2 = resolver.getClassDeclarationByName(resolver.getKSNameFromString("MyClass"))!!
        results.add("declaration1.equals(declaration2): ${declaration1.equals(declaration2)}")
        val declaration3 = resolver.getSymbolsWithAnnotation("MyAnnotation")
            .single { (it as? KSClassDeclaration)?.simpleName?.asString() == "MyJavaClass" }
        val declaration4 = resolver.getClassDeclarationByName(resolver.getKSNameFromString("MyJavaClass"))!!
        declaration3.equals(declaration4)
        results.add("declaration3.equals(declaration4): ${declaration3.equals(declaration4)}")
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
