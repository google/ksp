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
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*

open class ReplaceWithErrorTypeArgsProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val decls = listOf(
            // Those have 2 type parameters
            resolver.getClassDeclarationByName("KS")!!,
            resolver.getClassDeclarationByName("KL")!!,
            resolver.getClassDeclarationByName("JS")!!,
            resolver.getClassDeclarationByName("JL")!!,
            // Those have 0 or 1 parameters
            resolver.getClassDeclarationByName("KS1")!!,
            resolver.getClassDeclarationByName("KL1")!!,
            resolver.getClassDeclarationByName("JS1")!!,
            resolver.getClassDeclarationByName("JL1")!!,
            resolver.getClassDeclarationByName("JSE")!!,
            resolver.getClassDeclarationByName("JSE.E")!!,
            resolver.getClassDeclarationByName("JLE")!!,
            resolver.getClassDeclarationByName("JLE.E")!!,
            resolver.getClassDeclarationByName("KSE")!!,
            resolver.getClassDeclarationByName("KSE.E")!!,
            resolver.getClassDeclarationByName("KLE")!!,
            resolver.getClassDeclarationByName("KLE.E")!!,
        )
        val x = resolver.getPropertyDeclarationByName(resolver.getKSNameFromString("x"), true)!!
        val xargs = x.type.element!!.typeArguments
        val y = resolver.getPropertyDeclarationByName(resolver.getKSNameFromString("y"), true)!!
        val yargs = y.type.element!!.typeArguments
        val z = resolver.getPropertyDeclarationByName(resolver.getKSNameFromString("z"), true)!!
        val zargs = z.type.element!!.typeArguments

        for (decl in decls) {
            val declName = decl.qualifiedName!!.asString()
            results.add("$declName.star.replace($xargs): ${decl.asStarProjectedType().replace(xargs)}")
            results.add("$declName.star.replace($yargs): ${decl.asStarProjectedType().replace(yargs)}")
            results.add("$declName.asType($xargs): ${decl.asType(xargs)}")
            results.add("$declName.asType($yargs): ${decl.asType(yargs)}")
            results.add("$declName.asType(emptyList()): ${decl.asType(emptyList())}")
        }
        val function = resolver.getFunctionDeclarationsByName(resolver.getKSNameFromString("f"), true).single()
        results.add("default type:${function.parameters.single().type.resolve().replace(emptyList())}")
        // TODO: fix flexible type creation once upstream available.
        val js1 = resolver.getClassDeclarationByName("JS1")!!
        results.add("flexible type star:${js1.getDeclaredProperties().single().type.resolve().starProjection()}")
        val javaClass = resolver.getClassDeclarationByName("JavaClass")!!
        val genericFlexibleProperty = javaClass.getDeclaredProperties().single().type.resolve()
        results.add("flexible type replace argument:${genericFlexibleProperty.replace(zargs)}")
        resolver.getClassDeclarationByName("Foo")?.let { cls ->
            cls.getDeclaredProperties().forEach { p ->
                results.add(
                    "${p.type.resolve().arguments}," +
                        " ${p.type.resolve().arguments.map { it.type!!.resolve().replace(emptyList()) } }"
                )
            }
        }
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
