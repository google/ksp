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

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType

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
