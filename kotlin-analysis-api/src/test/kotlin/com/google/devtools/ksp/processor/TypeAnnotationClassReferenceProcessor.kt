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
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class TypeAnnotationClassReferenceProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        for (className in listOf("LibFoo", "LibJavaFoo", "MainFoo")) {
            val classDecl = resolver.getClassDeclarationByName(className) ?: continue
            val props = classDecl.declarations.filterIsInstance<KSPropertyDeclaration>()

            props.find { it.simpleName.asString() == "bar" }?.let { prop ->
                val anno = prop.type.resolve().annotations.single()
                val arg = anno.defaultArguments.single()
                results.add("$className bar default value: ${arg.value}")
            }

            props.find { it.simpleName.asString() == "baz" }?.let { prop ->
                val anno = prop.type.resolve().annotations.single()
                val arg = anno.arguments.single()
                results.add("$className baz value: ${arg.value}")
            }

            props.find { it.simpleName.asString() == "listBar" }?.let { prop ->
                val anno = prop.type.resolve().arguments.single().type!!.resolve().annotations.single()
                val arg = anno.defaultArguments.single()
                results.add("$className listBar default value: ${arg.value}")
            }

            props.find { it.simpleName.asString() == "listBaz" }?.let { prop ->
                val anno = prop.type.resolve().arguments.single().type!!.resolve().annotations.single()
                val arg = anno.arguments.single()
                results.add("$className listBaz value: ${arg.value}")
            }

            props.find { it.simpleName.asString() == "nestedDefault" }?.let { prop ->
                val anno = prop.annotations.single()
                val innerAnno = anno.defaultArguments.single().value as KSAnnotation
                val arg = innerAnno.arguments.single()
                results.add("$className nestedDefault value: ${arg.value}")
            }

            props.find { it.simpleName.asString() == "nestedExplicit" }?.let { prop ->
                val anno = prop.annotations.single()
                val innerAnno = anno.arguments.single().value as KSAnnotation
                val arg = innerAnno.arguments.single()
                results.add("$className nestedExplicit value: ${arg.value}")
            }
        }

        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
