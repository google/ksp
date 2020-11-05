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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration

class AnnotationArrayValueProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver) {
        val ktClass = resolver.getClassDeclarationByName("KotlinAnnotated")!!
        logAnnotations(ktClass)
        val javaClass = resolver.getClassDeclarationByName("JavaAnnotated")!!
        logAnnotations(javaClass)
    }

    private fun logAnnotations(classDeclaration: KSClassDeclaration) {
        result.add(classDeclaration.qualifiedName!!.asString())
        classDeclaration.annotations.forEach { annotation ->
            result.add("${annotation.shortName.asString()} ->")
            annotation.arguments.forEach {
                val value = it.value
                // log type of value as well. see issue:#135
                val key = "${it.name?.asString()}: ${value!!::class.simpleName}"
                if (value is Array<*>) {
                    result.add("$key = [${value.joinToString(", ")}]")
                } else {
                    result.add("$key = ${it.value}")
                }
            }
        }
    }
}