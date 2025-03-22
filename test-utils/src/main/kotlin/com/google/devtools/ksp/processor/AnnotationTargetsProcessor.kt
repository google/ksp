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

class AnnotationTargetsProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val myClassName = resolver.getKSNameFromString("com.example.MyClass")
        val myClass: KSClassDeclaration = resolver.getClassDeclarationByName(myClassName)!!
        val propForLib = myClass.getAllProperties().single { it.simpleName.asString() == "propForLib" }
        val propForSrc = myClass.getAllProperties().single { it.simpleName.asString() == "propForSrc" }
        results.add("$propForLib: ${propForLib.annotations.map { it.shortName.asString() }.toList()}")
        results.add("$propForSrc: ${propForSrc.annotations.map { it.shortName.asString() }.toList()}")
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
