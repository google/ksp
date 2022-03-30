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
import com.google.devtools.ksp.symbol.*

open class SuperTypesProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        fun KSClassDeclaration.toResult(): String {
            val supers = superTypes.map { it.resolve().declaration.qualifiedName!!.asString() }.joinToString(", ")
            return "${qualifiedName!!.asString()}: $supers"
        }

        fun show(decl: String) = results.add(resolver.getClassDeclarationByName(decl)!!.toResult())

        show("KotlinInterfaceInLib")
        show("KotlinInterfaceInLibWithSuper")
        show("AbstractKotlinClassInLib")
        show("AbstractKotlinClassInLibWithSuperClass")
        show("AbstractKotlinClassInLibWithSuperInterface")
        show("KotlinClassInLib")
        show("KotlinClassInLibWithSuperAbstract")
        show("KotlinClassInLibWithSuperClass")
        show("KotlinClassInLibWithSuperInterface")
        show("JavaInterfaceInLib")
        show("JavaInterfaceInLibWithSuper")
        show("AbstractJavaClassInLib")
        show("AbstractJavaClassInLibWithSuperInterface")
        show("AbstractJavaClassInLibWithSuperClass")
        show("JavaClassInLib")
        show("JavaClassInLibWithSuperInterface")
        show("JavaClassInLibWithSuperAbstract")
        show("JavaClassInLibWithSuperClass")

        show("KotlinInterfaceInSource")
        show("KotlinInterfaceInSourceWithSuper")
        show("AbstractKotlinClassInSource")
        show("AbstractKotlinClassInSourceWithSuperClass")
        show("AbstractKotlinClassInSourceWithSuperInterface")
        show("KotlinClassInSource")
        show("KotlinClassInSourceWithSuperAbstract")
        show("KotlinClassInSourceWithSuperClass")
        show("KotlinClassInSourceWithSuperInterface")
        show("JavaInterfaceInSource")
        show("JavaInterfaceInSourceWithSuper")
        show("AbstractJavaClassInSource")
        show("AbstractJavaClassInSourceWithSuperInterface")
        show("AbstractJavaClassInSourceWithSuperClass")
        show("JavaClassInSource")
        show("JavaClassInSourceWithSuperInterface")
        show("JavaClassInSourceWithSuperAbstract")
        show("JavaClassInSourceWithSuperClass")

        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}
