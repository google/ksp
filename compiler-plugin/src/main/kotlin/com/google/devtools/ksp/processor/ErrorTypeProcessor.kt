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
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

class ErrorTypeProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val classC = resolver.getClassDeclarationByName(resolver.getKSNameFromString("C"))!!
        val errorAtTop = classC.declarations.single { it.simpleName.asString() == "errorAtTop" } as KSPropertyDeclaration
        val errorInComponent = classC.declarations.single { it.simpleName.asString() == "errorInComponent" } as KSPropertyDeclaration
        result.add(errorAtTop.type.resolve().print() ?: "")
        result.add(errorInComponent.type.resolve().print() ?: "")
        errorInComponent.type.resolve().arguments.map { result.add(it.type!!.resolve().print()) }
        result.add(
            "errorInComponent is assignable from errorAtTop: ${
                errorAtTop.type.resolve().isAssignableFrom(errorAtTop.type.resolve())
            }"
        )
        result.add(
            "errorInComponent is assignable from class C: ${
                errorAtTop.type.resolve().isAssignableFrom(classC.asStarProjectedType())
            }"
        )
        result.add(
            "Any is assignable from errorInComponent: ${
                ResolverImpl.instance.builtIns.anyType.isAssignableFrom(errorAtTop.type.resolve())
            }"
        )
        result.add(
            "class C is assignable from errorInComponent: ${
                classC.asStarProjectedType().isAssignableFrom(errorAtTop.type.resolve())
            }"
        )
        result.add(
            "Any is assignable from class C: ${
                ResolverImpl.instance.builtIns.anyType.isAssignableFrom(classC.asStarProjectedType())
            }"
        )
        val Cls = resolver.getClassDeclarationByName("Cls")!!
        val type = Cls.superTypes[0].resolve()
        result.add("Cls's super type is Error type: ${type.isError}")
        Cls.annotations.forEach {
            val annotation = it.annotationType.resolve()
            result.add("Cls's annotation is Error type: ${annotation.isError}")
        }
        return emptyList()
    }

    private fun KSType.print(): String {
        return if (this.isError) {
            if (this.declaration.qualifiedName == null) "ERROR TYPE" else throw IllegalStateException("Error type should resolve to KSErrorTypeClassDeclaration")
        } else this.declaration.qualifiedName!!.asString()
    }
}