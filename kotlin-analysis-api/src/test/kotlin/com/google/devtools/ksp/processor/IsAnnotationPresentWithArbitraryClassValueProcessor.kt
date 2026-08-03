/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import kotlin.reflect.KClass

@KspExperimental
class IsAnnotationPresentWithArbitraryClassValueProcessor : AbstractTestProcessor() {
    private val results = mutableListOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(
            "com.google.devtools.ksp.processor.ArbitraryClassValueAnnotation"
        ).forEach {
            results.add(it.isAnnotationPresent(ArbitraryClassValueAnnotation::class).toString())
            results.add(
                it.getAnnotationsByType(OtherAnnotations.ArbitraryClassValueAnnotation::class)
                    .count()
                    .toString()
            )
            results.add(
                it.isAnnotationPresent(OtherAnnotations.ArbitraryClassValueAnnotation::class)
                    .toString()
            )
        }
        return emptyList()
    }

    override fun toResult(): List<String> {
        return results
    }
}

annotation class ArbitraryClassValueAnnotation(val value: KClass<*>)

private object OtherAnnotations {
    annotation class ArbitraryClassValueAnnotation
}
