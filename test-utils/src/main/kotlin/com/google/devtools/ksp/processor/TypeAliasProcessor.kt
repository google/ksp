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

open class TypeAliasProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val types = mutableListOf<KSType>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val byFinalSignature = mutableMapOf<String, MutableList<KSType>>()
        resolver.getNewFiles().flatMap { file ->
            file.declarations.filterIsInstance<KSPropertyDeclaration>().map { prop ->
                buildString {
                    append(prop.simpleName.asString())
                    append(" : ")
                    val propType = prop.type.resolve()
                    val signatures = propType.typeAliasSignatures()
                    append(signatures.joinToString(" = "))
                    byFinalSignature.getOrPut(signatures.last()) {
                        mutableListOf()
                    }.add(propType)
                }
            }
        }.forEach(results::add)
        byFinalSignature.forEach { (signature, sameTypeAliases) ->
            // exclude List<T> case from the test because they lose a type argument when resolving aliases, so they
            // are not the same anymore as we traverse the declarations.
            if (signature != "List<T>") {
                for (i in sameTypeAliases) {
                    for (j in sameTypeAliases) {
                        assert(i == j) {
                            "$i and $j both map to $signature, equals should return true"
                        }
                    }
                }
                assert(sameTypeAliases.map { it.hashCode() }.distinct().size == 1) {
                    "different hashcodes for members of $signature"
                }
            }
        }
        return emptyList()
    }

    private fun KSType.typeAliasSignatures(): List<String> {
        var self: KSType? = this
        return buildList {
            while (self != null) {
                add(self!!.toSignature())
                self = (self?.declaration as? KSTypeAlias)?.type?.resolve()
            }
        }
    }

    private fun KSType.toSignature(): String = buildString {
        append(declaration.simpleName.asString())
        if (arguments.isNotEmpty()) {
            append("<")
            arguments.map {
                it.type?.resolve()?.toSignature() ?: "<error>"
            }.forEach(this::append)
            append(">")
        }
    }

    override fun toResult(): List<String> {
        return results
    }
}
