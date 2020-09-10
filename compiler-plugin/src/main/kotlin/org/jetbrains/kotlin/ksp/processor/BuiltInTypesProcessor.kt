/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.*

open class BuiltInTypesProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val typeCollector = TypeCollectorNoAccessor()
    val types = mutableSetOf<KSType>()

    override fun process(resolver: Resolver) {
        val files = resolver.getAllFiles()
        val ignoredNames = mutableSetOf<String>()

        files.forEach {
            it.accept(typeCollector, types)
        }

        val builtInTypes = listOf<KSType>(
            resolver.builtIns.annotationType,
            resolver.builtIns.anyType,
            resolver.builtIns.arrayType,
            resolver.builtIns.booleanType,
            resolver.builtIns.byteType,
            resolver.builtIns.charType,
            resolver.builtIns.doubleType,
            resolver.builtIns.floatType,
            resolver.builtIns.intType,
            resolver.builtIns.iterableType,
            resolver.builtIns.longType,
            resolver.builtIns.nothingType,
            resolver.builtIns.numberType,
            resolver.builtIns.shortType,
            resolver.builtIns.stringType,
            resolver.builtIns.unitType
        ).sortedBy { it.toString() }

        val collectedTypes = types.sortedBy { it.toString() }

        results.addAll(builtInTypes.zip(collectedTypes).map { (b, c) -> "$b: " + if (b == c) "OK" else "FAIL" })
    }

    override fun toResult(): List<String> {
        return results
    }

}
