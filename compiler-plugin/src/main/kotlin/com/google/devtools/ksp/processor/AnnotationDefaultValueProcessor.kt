/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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

class AnnotationDefaultValueProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver) {
        val ktClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("A"))!!
        var ktAnno = ktClass.annotations[0]
        var javaAnno = ktClass.annotations[1]
        result.add("${ktAnno.shortName.asString()} -> ${ktAnno.arguments.map { "${it.name?.asString()}:${it.value}" }.joinToString(",")}")
        result.add("${javaAnno.shortName.asString()} -> ${javaAnno.arguments.map { "${it.name?.asString()}:${it.value}" }.joinToString(",")}")
        val javaClass = resolver.getClassDeclarationByName(resolver.getKSNameFromString("JavaAnnotated"))!!
        ktAnno = javaClass.annotations[0]
        javaAnno = javaClass.annotations[1]
        result.add("${ktAnno.shortName.asString()} -> ${ktAnno.arguments.map { "${it.name?.asString()}:${it.value}" }.joinToString(",")}")
        result.add("${javaAnno.shortName.asString()} -> ${javaAnno.arguments.map { "${it.name?.asString()}:${it.value}" }.joinToString(",")}")
    }
}