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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

@KspExperimental
open class JavaToKotlinMapProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val typeCollector = TypeCollectorNoAccessor()
    val types = mutableSetOf<KSType>()

    val javaClasses = listOf(
        "java.lang.String",
        "java.lang.Integer",
        "java.util.List",
        "java.util.Map.Entry",
        "java.lang.Void",
    )

    val kotlinClasses = listOf(
        "kotlin.Throwable",
        "kotlin.Int",
        "kotlin.Nothing",
        "kotlin.IntArray",
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        javaClasses.forEach {
            val k = resolver.mapJavaNameToKotlin(
                resolver.getKSNameFromString(it)
            )?.asString()
            results.add("$it -> $k")
        }

        kotlinClasses.forEach {
            val j = resolver.mapKotlinNameToJava(
                resolver.getKSNameFromString(it)
            )?.asString()
            results.add("$it -> $j")
        }

        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

}
