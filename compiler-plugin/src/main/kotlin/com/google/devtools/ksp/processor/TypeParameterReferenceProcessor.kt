/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.binary.KSTypeReferenceDescriptorImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeImpl
import com.google.devtools.ksp.visitor.KSTopDownVisitor

open class TypeParameterReferenceProcessor: AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val collector = ReferenceCollector()
    val references = mutableSetOf<KSTypeReference>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()

        files.forEach {
            it.accept(collector, references)
        }

        val sortedReferences = references.filter { it.element is KSClassifierReference && it.origin == Origin.KOTLIN }.sortedBy { (it.element as KSClassifierReference).referencedName() }

        for (i in sortedReferences) {
            val r = i.resolve()
            results.add("${r?.declaration?.qualifiedName?.asString()}")
        }
    }

    override fun toResult(): List<String> {
        return results
    }

}
