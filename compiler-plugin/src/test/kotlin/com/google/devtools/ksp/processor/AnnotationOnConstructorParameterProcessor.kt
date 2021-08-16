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

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

class AnnotationOnConstructorParameterProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().first().declarations.single { it.qualifiedName!!.asString() == "Sample" }.let { clz ->
            clz as KSClassDeclaration
            val prop1 = clz.getAllProperties().single { it.simpleName.asString() == "fullName" }
            val prop2 = clz.getDeclaredProperties().single { it.simpleName.asString() == "fullName" }
            prop1.annotations.forEach { anno ->
                results.add(anno.shortName.asString())
            }
            results.add((prop1 === prop2).toString())
            val fun1 = clz.getAllFunctions().single { it.simpleName.asString() == "foo" }
            val fun2 = clz.getDeclaredFunctions().single { it.simpleName.asString() == "foo" }
            results.add((fun1 === fun2).toString())
        }
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
