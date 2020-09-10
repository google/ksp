/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.getClassDeclarationByName
import org.jetbrains.kotlin.ksp.processing.Resolver

class MapSignatureProcessor : AbstractTestProcessor() {
    private val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver) {
        val cls = resolver.getClassDeclarationByName("Cls")!!
        result.add(resolver.mapToJvmSignature(cls))
        cls.declarations.map { result.add(resolver.mapToJvmSignature(it)) }
    }
}