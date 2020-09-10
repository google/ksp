/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSTypeReferenceDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSTypeImpl
import org.jetbrains.kotlin.ksp.visitor.KSTopDownVisitor

open class ReferenceElementProcessor: AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val collector = ReferenceCollector()
    val references = mutableSetOf<KSTypeReference>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()

        files.forEach {
            it.accept(collector, references)
        }

        val sortedReferences = references.filter { it.element is KSClassifierReference && it.origin == Origin.KOTLIN }.sortedBy { (it.element as KSClassifierReference).referencedName() }
        for (i in sortedReferences)
            results.add("KSClassifierReferenceImpl: Qualifier of ${i.element} is ${(i.element as KSClassifierReference).qualifier}")

        val descriptorReferences = sortedReferences.map { KSTypeReferenceDescriptorImpl.getCached((it.resolve() as KSTypeImpl).kotlinType) }
        for (i in descriptorReferences) {
            results.add("KSClassifierReferenceDescriptorImpl: Qualifier of ${i.element} is ${(i.element as KSClassifierReference).qualifier}")
        }

        val javaReferences = references.filter { it.element is KSClassifierReference && it.origin == Origin.JAVA }.sortedBy { (it.element as KSClassifierReference).referencedName() }
        for (i in javaReferences) {
            results.add("KSClassifierReferenceJavaImpl: Qualifier of ${i.element} is ${(i.element as KSClassifierReference).qualifier}")
        }

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
