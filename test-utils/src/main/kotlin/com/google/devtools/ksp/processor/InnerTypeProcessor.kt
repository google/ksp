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

import com.google.devtools.ksp.innerArguments
import com.google.devtools.ksp.outerType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

open class InnerTypeProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val typeCollector = TypeCollector()
    val types = mutableSetOf<KSType>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val files = resolver.getNewFiles()
        val ignoredNames = mutableSetOf<String>()

        files.forEach {
            it.accept(typeCollector, types)
            it.annotations.forEach {
                if (it.shortName.asString() == "Suppress") {
                    it.arguments.forEach {
                        (it.value as List<String>).forEach {
                            ignoredNames.add(it)
                        }
                    }
                }
            }
        }

        val sortedTypes = types.filterNot { it.declaration.simpleName.asString() in ignoredNames }.sortedBy {
            it.toString()
        }

        fun KSType.breakDown(): List<String> {
            var current: KSType? = this
            val brokenDown = mutableListOf<String>()
            do {
                val innerArgs = current!!.innerArguments.joinToString(", ")
                brokenDown.add("${current.declaration.qualifiedName!!.asString()}<$innerArgs>")
                current = current.outerType
            } while (current != null)
            return brokenDown
        }

        for (i in sortedTypes) {
            results.add("$i: ${i.breakDown()}")
        }
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
