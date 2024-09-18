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
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

@KspExperimental
class MapSignatureProcessor : AbstractTestProcessor() {
    private val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        listOf("Cls", "JavaIntefaceWithVoid", "JavaClass", "JavaAnno", "JavaEnum")
            .map { className ->
                resolver.getClassDeclarationByName(className)!!
            }.forEach { subject ->
                result.add(resolver.mapToJvmSignature(subject)!!)
                subject.declarations.forEach {
                    if (it is KSFunctionDeclaration) {
                        val decl = it.returnType!!.resolve().declaration
                        println("before: ${it.simpleName.asString()}: ${resolver.mapToJvmSignature(decl)}")
                    }
                    result.add(it.simpleName.asString() + ": " + resolver.mapToJvmSignature(it))
                    if (it is KSFunctionDeclaration) {
                        val decl = it.returnType!!.resolve().declaration
                        println("after: ${it.simpleName.asString()}: ${resolver.mapToJvmSignature(decl)}")
                    }

                }
            }
        return emptyList()
    }
}
