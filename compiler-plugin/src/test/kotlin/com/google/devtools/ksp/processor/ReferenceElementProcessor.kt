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

import com.google.devtools.ksp.getPropertyDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.visitor.KSTopDownVisitor

open class ReferenceElementProcessor: AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val collector = ReferenceCollector()
    val references = mutableSetOf<KSTypeReference>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val files = resolver.getNewFiles()

        files.forEach {
            it.accept(collector, references)
        }

        resolver.getPropertyDeclarationByName("z", true)!!.accept(collector, references)
        resolver.getPropertyDeclarationByName("w", true)!!.accept(collector, references)

        fun refName(it: KSTypeReference) = (it.element as KSClassifierReference).referencedName()

        val sortedReferences = references.filter { it.element is KSClassifierReference && it.origin == Origin.KOTLIN }.sortedBy(::refName)
        for (i in sortedReferences)
            results.add("KSClassifierReferenceImpl: Qualifier of ${i.element} is ${(i.element as KSClassifierReference).qualifier}")

        // FIXME: References in getters and type arguments are not compared to equal.
        val descriptorReferences = references.filter { it.element is KSClassifierReference && it.origin == Origin.KOTLIN_LIB }.distinctBy(::refName).sortedBy(::refName)
        for (i in descriptorReferences) {
            results.add("KSClassifierReferenceDescriptorImpl: Qualifier of ${i.element} is ${(i.element as KSClassifierReference).qualifier}")
        }

        val javaReferences = references.filter { it.element is KSClassifierReference && it.origin == Origin.JAVA }.sortedBy(::refName)
        for (i in javaReferences) {
            results.add("KSClassifierReferenceJavaImpl: Qualifier of ${i.element} is ${(i.element as KSClassifierReference).qualifier}")
        }
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }

}

class ReferenceCollector : KSTopDownVisitor<MutableSet<KSTypeReference>, Unit>() {
    override fun defaultHandler(node: KSNode, data: MutableSet<KSTypeReference>) = Unit

    override fun visitTypeReference(typeReference: KSTypeReference, data: MutableSet<KSTypeReference>) {
        super.visitTypeReference(typeReference, data)
        data.add(typeReference)
    }
}
