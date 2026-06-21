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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class JvmNameRecordProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    override fun toResult(): List<String> {
        return results
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val sourceRecords = resolver.getSymbolsWithAnnotation("kotlin.jvm.JvmRecord")
            .filterIsInstance<KSClassDeclaration>()
            .toSet()
        // LibRecord: library classes are not returned by getSymbolsWithAnnotation.
        // TypeAliased: PSI does not resolve typealias annotations in getSymbolsWithAnnotation,
        //   so it is looked up directly. Deduplication handles the AA case where it appears in both.
        val extraRecords = listOfNotNull(
            resolver.getClassDeclarationByName("LibRecord"),
            resolver.getClassDeclarationByName("TypeAliased"),
        ).filter { it !in sourceRecords }
        (sourceRecords + extraRecords)
            .flatMap { cls ->
                cls.getAllProperties().map { property ->
                    val accessorNames = listOfNotNull(
                        property.getter?.let { resolver.getJvmName(it) },
                        property.setter?.let { setter ->
                            val setterName = resolver.getJvmName(setter)
                            val parameterName = setter.parameter.name?.asString()
                            if (parameterName != null) "$setterName($parameterName)" else setterName
                        },
                    )
                    "${cls.simpleName.asString()}.${property.simpleName.asString()}: ${accessorNames.joinToString()}"
                }
            }
            .sorted()
            .let { results.addAll(it) }
        return emptyList()
    }
}
