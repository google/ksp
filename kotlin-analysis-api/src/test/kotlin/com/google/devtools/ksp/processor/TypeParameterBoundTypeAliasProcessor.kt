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
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getFunctionDeclarationsByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter

/**
 * Pins that a typealias written as a type parameter bound resolves to the same [KSType] shape as the
 * same typealias written as a value parameter type.
 *
 * `KSType.declaration` prefers the type's abbreviation, but `KSType.arguments` reads the type
 * arguments of whichever type it was built from. So if the expanded type is passed instead of the
 * abbreviated one, a bound written as `StrMap<Int>` (where `typealias StrMap<V> = Map<String, V>`)
 * reports `declaration=StrMap` alongside the *expanded* arguments `[String, Int]`.
 *
 * Bounds are built in two ways, both covered here: from the declared upper bounds (source, library,
 * and class type parameters), and from substituted upper bounds via `asMemberOf`.
 */
class TypeParameterBoundTypeAliasProcessor(override val enableNewFeatures: Boolean) : AbstractTestProcessor() {

    private val results = mutableListOf<String>()

    override fun toResult(): List<String> = results

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val srcFoo =
            resolver
                .getFunctionDeclarationsByName("srcFoo", includeTopLevel = true)
                .single()

        results.add("srcFoo.p value: ${srcFoo.parameters.single().type.resolve().describe()}")
        results.add("srcFoo.T bound: ${srcFoo.typeParameters.single().describeBound()}")

        val libFoo =
            resolver
                .getFunctionDeclarationsByName("lib.libFoo", includeTopLevel = true)
                .single()

        results.add("libFoo.T bound: ${libFoo.typeParameters.single().describeBound()}")

        val srcBox = resolver.getClassDeclarationByName("SrcBox")!!

        results.add("SrcBox.T bound: ${srcBox.typeParameters.single().describeBound()}")

        // A self-referential bound: the alias argument is the type parameter itself.
        val recFoo =
            resolver
                .getFunctionDeclarationsByName("recFoo", includeTopLevel = true)
                .single()

        results.add("recFoo.T bound: ${recFoo.typeParameters.single().describeBound()}")

        val sub = resolver.getClassDeclarationByName("Sub")!!
        resolver.getClassDeclarationByName("Base")!!.getDeclaredFunctions()
            .sortedBy { it.simpleName.asString() }
            .forEach { function ->
                val asMemberOfSub = function.asMemberOf(sub.asType(emptyList()))
                results.add(
                    "Sub.${function.simpleName.asString()} asMemberOf bounds: " +
                        "${asMemberOfSub.typeParameters.map { it.describeBound() }}"
                )
            }
        return emptyList()
    }

    private fun KSTypeParameter.describeBound(): String = bounds.single().resolve().describe()

    private fun KSType.describe(): String =
        "${declaration.simpleName.asString()}${arguments.map { it.type?.resolve()?.describe() }}"
}
