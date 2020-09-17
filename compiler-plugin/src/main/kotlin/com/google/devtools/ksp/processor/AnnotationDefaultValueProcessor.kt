/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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