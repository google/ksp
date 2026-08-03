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

import com.google.devtools.ksp.KSTypeNotPresentException
import com.google.devtools.ksp.KSTypesNotPresentException
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import kotlin.reflect.KClass

@KspExperimental
class AnnotationArbitraryClassValueProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(
            "com.google.devtools.ksp.processor.ClassValueAnnotation"
        )
        symbols.flatMap {
            it.getAnnotationsByType(ClassValueAnnotation::class)
        }.forEach {
            logAnnotationValues(it)
        }
        return emptyList()
    }

    private fun logAnnotationValues(classValueAnnotation: ClassValueAnnotation) {
        try {
            classValueAnnotation.classValue
        } catch (e: Exception) {
            assert(e is KSTypeNotPresentException)
            e as KSTypeNotPresentException
            result.add(e.ksType.toString())
        }

        try {
            classValueAnnotation.classValues
        } catch (e: Exception) {
            assert(e is KSTypesNotPresentException)
            e as KSTypesNotPresentException
            result.add(e.ksTypes.joinToString())
        }
    }
}

annotation class ClassValueAnnotation(
    val classValue: KClass<*>,
    val classValues: Array<KClass<*>>
)
